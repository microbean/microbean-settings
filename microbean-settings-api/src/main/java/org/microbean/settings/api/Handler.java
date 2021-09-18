/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2021 microBean™.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.microbean.settings.api;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.microbean.development.annotation.Experimental;
import org.microbean.development.annotation.Incomplete;

import org.microbean.settings.api.ValueSupplier.Value;

@Experimental
@Incomplete
public final class Handler<T> implements Supplier<T> {


  /*
   * Static fields.
   */

  
  private static final ConcurrentMap<Path, Object> proxies = new ConcurrentHashMap<>();


  /*
   * Instance fields.
   */

  
  private final Path path;

  private final Map<?, ?> applicationQualifiers;
  
  private final Supplier<? extends T> defaultTargetSupplier;

  private final BiFunction<? super Path, ? super Map<?, ?>, ? extends Collection<ValueSupplier<?>>> valueSuppliers;
  
  private final ClassLoader cl;


  /*
   * Constructors.
   */


  public Handler(final Class<T> rootType,
                 final Supplier<? extends T> defaultTargetSupplier,
                 final BiFunction<? super Path, ? super Map<?, ?>, ? extends Collection<ValueSupplier<?>>> valueSuppliers) {
    this(new Path(rootType),
         Map.of(),
         defaultTargetSupplier,
         valueSuppliers,
         rootType.getClassLoader());
  }
  
  public Handler(final Class<T> rootType,
                 final Map<?, ?> applicationQualifiers,
                 final Supplier<? extends T> defaultTargetSupplier,
                 final BiFunction<? super Path, ? super Map<?, ?>, ? extends Collection<ValueSupplier<?>>> valueSuppliers) {
    this(new Path(rootType),
         applicationQualifiers,
         defaultTargetSupplier,
         valueSuppliers,
         rootType.getClassLoader());
  }
  
  private Handler(final Path path,
                  final Map<?, ?> applicationQualifiers,
                  final Supplier<? extends T> defaultTargetSupplier,
                  final BiFunction<? super Path, ? super Map<?, ?>, ? extends Collection<ValueSupplier<?>>> valueSuppliers,
                  ClassLoader cl)
  {
    super();
    this.path = Objects.requireNonNull(path, "path");
    this.applicationQualifiers = applicationQualifiers == null ? Map.of() : Map.copyOf(applicationQualifiers);
    if (defaultTargetSupplier == null) {
      this.defaultTargetSupplier = Handler::returnNull;
    } else {
      this.defaultTargetSupplier = defaultTargetSupplier;
    }
    if (valueSuppliers == null) {
      this.valueSuppliers = (p, q) -> List.of();
    } else {
      this.valueSuppliers = valueSuppliers;
    }
    this.cl = cl;
  }

  @SuppressWarnings("unchecked")
  public final T get() {
    return
      (T)proxies.computeIfAbsent(this.path,
                                 p -> Proxy.newProxyInstance(p.targetClass().getClassLoader(),
                                                             new Class<?>[] { p.targetClass() },
                                                             this::invoke));
  }

  private final Object computeProxy(final Path p) {
    return this.computeProxy(p, this::invoke);
  }
  
  private final Object invoke(final Object proxy, final Method method, final Object[] args) throws ReflectiveOperationException {
    if (method.getDeclaringClass() == Object.class) {
      return
        switch (method.getName()) {
        case "hashCode" -> System.identityHashCode(proxy);
        case "equals" -> proxy == args[0];
        case "toString" -> proxy.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(proxy));
        default -> throw new AssertionError();
        };
    } else {
      final Path path = this.path(method);
      final Supplier<?> value = this.value(path);
      if (value == null) {
        final Class<?> returnType = method.getReturnType();
        if (isProxyable(returnType)) {
          assert path.targetClass().equals(method.getReturnType());
          return proxies.computeIfAbsent(path, this::computeSubProxy);
        } else {
          final T defaultTarget = this.defaultTargetSupplier.get();
          if (defaultTarget == null) {
            throw new UnsupportedOperationException(method.getName());
          } else {
            return method.invoke(defaultTarget, args);
          }
        }
      } else {
        return value.get();
      }
    }
  }

  private final Object computeSubProxy(final Path p) {
    return this.computeProxy(p, this.butWith(p)::invoke);
  }

  private final Object computeProxy(final Path p, final InvocationHandler invocationHandler) {
    final Class<?> targetClass = p.targetClass();
    final ClassLoader cl = targetClass.getClassLoader();
    return Proxy.newProxyInstance(cl, new Class<?>[] { targetClass }, invocationHandler);
  }

  private final Handler<T> butWith(final Path path) {
    return new Handler<>(path, this.applicationQualifiers, this.defaultTargetSupplier, this.valueSuppliers, path.targetClass().getClassLoader());
  }
  
  private final Path path(final Method m) {
    return this.path.plus(m.getName(), m.getGenericReturnType());
  }

  private final Value<?> value(final Path path) {
    return this.value(path, this.valueSuppliers.apply(path, this.applicationQualifiers));
  }

  private final Value<?> value(final Path path, final Collection<? extends ValueSupplier<?>> valueSuppliers) {
    final Value<?> returnValue;
    if (valueSuppliers == null || valueSuppliers.isEmpty()) {
      returnValue = null;
    } else {
      Collection<Value<?>> bad = null;
      Value<?> candidate = null;
      for (final ValueSupplier<?> valueSupplier : valueSuppliers) {
        final Value<?> v = valueSupplier == null ? null : valueSupplier.get(path, this.applicationQualifiers);
        if (v != null) {
          final Map<?, ?> qualifiers = v.qualifiers();
          if (viable(qualifiers)) {
            if (candidate == null || qualifiers.size() > candidate.qualifiers().size()) {
              candidate = v;
            } else {
              // take no action
            }
          } else {
            if (bad == null) {
              bad = new ArrayList<>(5);
              if (candidate != null) {
                bad.add(candidate);
              }
            }
            bad.add(v);
          }
        }
      }
      if (bad != null) {
        throw new IllegalStateException("bad or conflicting ValueSuppliers: " + bad);
      }
      returnValue = candidate;
    }
    return returnValue;
  }

  private final boolean viable(final Map<?, ?> vsQualifiers) {
    if (this.applicationQualifiers.isEmpty()) {
      return vsQualifiers == null || vsQualifiers.isEmpty();
    } else if (vsQualifiers == null || vsQualifiers.isEmpty()) {
      return false;
    } else {
      return applicationQualifiers.entrySet().containsAll(vsQualifiers.entrySet());
    }
  }

  /*
   * Static methods.
   */


  private static final boolean isGetter(final Method m) {
    if (m != null && m.getDeclaringClass() != Object.class && m.getParameterCount() == 0) {
      final Object returnType = m.getReturnType();
      return returnType != void.class && returnType != Void.class;
    } else {
      return false;
    }
  }

  private static final boolean isProxyable(final Class<?> c) {
    if (c != null && c.isInterface()) {
      for (final Method m : c.getMethods()) {
        if (isGetter(m)) {
          return true;
        }
      }
      return false;
    } else {
      return false;
    }
  }

  private static final Class<?> rawClass(final Type type) {
    if (type instanceof Class<?> c) {
      return c;
    } else if (type instanceof ParameterizedType ptype) {
      return rawClass(ptype);
    } else {
      throw new IllegalArgumentException("type: " + type);
    }
  }

  private static final Class<?> rawClass(final ParameterizedType type) {
    return rawClass(type.getRawType());
  }

  private static final <T> T returnNull() {
    return null;
  }

}
