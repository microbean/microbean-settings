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

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.UndeclaredThrowableException;
import java.lang.reflect.WildcardType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.Spliterator;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import java.util.stream.BaseStream;
import java.util.stream.Stream;

import org.microbean.development.annotation.Experimental;
import org.microbean.development.annotation.Incomplete;

import org.microbean.settings.api.ValueSupplier.Value;

@Experimental
@Incomplete
public final class Configured<T> implements QualifiedPath, Supplier<T> {


  /*
   * Static fields.
   */

  
  private static final ClassValue<List<ValueSupplier>> loadedValueSuppliers = new ClassValue<>() {
      @Override
      protected final List<ValueSupplier> computeValue(final Class<?> c) {
        if (ValueSupplier.class.equals(c)) {
          final List<ValueSupplier> list = ServiceLoader.load(ValueSupplier.class, ValueSupplier.class.getClassLoader())
          .stream()
          .map(ServiceLoader.Provider::get)
          .toList();
          Collections.sort(list, Prioritized.COMPARATOR_DESCENDING);
          // TODO: add last ditch ListValueSupplier, MapValueSupplier, etc.          
          return list;
        } else {
          return null;
        }
      }
    };


  /*
   * Instance fields.
   */


  private final ConcurrentMap<Path, Object> proxies;

  private final Path path;

  private final Map<?, ?> applicationQualifiers;

  private final Supplier<? extends T> defaultTargetSupplier;

  private final Function<? super QualifiedPath, ? extends Collection<ValueSupplier>> valueSuppliers;

  private final BiFunction<? super String, ? super Boolean, ? extends String> pathComponentFunction;

  private final Function<? super Type, ? extends Boolean> isProxiableFunction;

  private final ClassLoader cl;


  /*
   * Constructors.
   */


  public Configured(final Class<T> rootType,
                    final Supplier<? extends T> defaultTargetSupplier,
                    final Function<? super QualifiedPath, ? extends Collection<ValueSupplier>> valueSuppliers) {
    this(new Path(rootType),
         Map.of(),
         defaultTargetSupplier,
         valueSuppliers,
         Configured::methodName,
         Configured::isProxiable);
  }

  public Configured(final Class<T> rootType,
                    final Supplier<? extends T> defaultTargetSupplier,
                    final Function<? super QualifiedPath, ? extends Collection<ValueSupplier>> valueSuppliers,
                    final BiFunction<? super String, ? super Boolean, ? extends String> pathComponentFunction) {
    this(new Path(rootType),
         Map.of(),
         defaultTargetSupplier,
         valueSuppliers,
         pathComponentFunction,
         Configured::isProxiable);
  }

  public Configured(final Class<T> rootType,
                    final Map<?, ?> applicationQualifiers,
                    final Supplier<? extends T> defaultTargetSupplier,
                    final Function<? super QualifiedPath, ? extends Collection<ValueSupplier>> valueSuppliers) {
    this(new Path(rootType),
         applicationQualifiers,
         defaultTargetSupplier,
         valueSuppliers,
         Configured::methodName,
         Configured::isProxiable);
  }

  public Configured(final Class<T> rootType,
                    final Map<?, ?> applicationQualifiers,
                    final Supplier<? extends T> defaultTargetSupplier,
                    final Function<? super QualifiedPath, ? extends Collection<ValueSupplier>> valueSuppliers,
                    final BiFunction<? super String, ? super Boolean, ? extends String> pathComponentFunction) {
    this(new Path(rootType),
         applicationQualifiers,
         defaultTargetSupplier,
         valueSuppliers,
         pathComponentFunction,
         Configured::isProxiable);
  }

  private Configured(final Path path,
                     final Map<?, ?> applicationQualifiers,
                     final Supplier<? extends T> defaultTargetSupplier,
                     final Function<? super QualifiedPath, ? extends Collection<ValueSupplier>> valueSuppliers,
                     final BiFunction<? super String, ? super Boolean, ? extends String> pathComponentFunction,
                     final Function<? super Type, ? extends Boolean> isProxiableFunction)
  {
    super();
    this.proxies = new ConcurrentHashMap<>();
    this.path = Objects.requireNonNull(path, "path");
    this.applicationQualifiers = applicationQualifiers == null ? Map.of() : Map.copyOf(applicationQualifiers);
    if (defaultTargetSupplier == null) {
      this.defaultTargetSupplier = Configured::returnNull;
    } else {
      this.defaultTargetSupplier = defaultTargetSupplier;
    }
    if (valueSuppliers == null) {
      this.valueSuppliers = qp -> List.of();
    } else {
      this.valueSuppliers = valueSuppliers;
    }
    if (pathComponentFunction == null) {
      this.pathComponentFunction = Configured::methodName;
    } else {
      this.pathComponentFunction = pathComponentFunction;
    }
    if (isProxiableFunction == null) {
      this.isProxiableFunction = Configured::isProxiable;
    } else {
      this.isProxiableFunction = isProxiableFunction;
    }
    this.cl = path.classLoader();
  }

  @SuppressWarnings("unchecked")
  public final T get() {
    return (T)this.proxies.computeIfAbsent(this.path, this::computeProxy);
  }

  @Override // QualifiedPath
  public final Path path() {
    return this.path;
  }

  @Override // QualifiedPath
  public final Map<?, ?> applicationQualifiers() {
    return this.applicationQualifiers;
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
      if (value != null) {
        // If there's an explicit ValueSupplier for this method, use it.
        return value.get();
      } else if (this.isProxiableFunction.apply(method.getGenericReturnType())) {
        // If we can synthesize what amounts to a ValueSupplier for
        // this method, then do so.
        return this.proxies.computeIfAbsent(path, p -> this.computeSubProxy(this.newDefaultTargetSupplier(method, args), p));
      } else {
        final T defaultTarget = this.defaultTargetSupplier.get();
        if (defaultTarget != null) {
          // If the default target supplier supplies a default target,
          // then invoke the method on it.
          return method.invoke(defaultTarget, args);
        } else if (method.isDefault()) {
          try {
            // If the current method is a default method of the
            // proxied interface, invoke it.
            return InvocationHandler.invokeDefault(proxy, method, args);
          } catch (final UnsupportedOperationException | Error e) {
            throw e;
          } catch (final Exception e) {
            throw new UnsupportedOperationException(method.getName(), e);
          } catch (final Throwable e) {
            throw new AssertionError(e.getMessage(), e);
          }
        } else {
          // We have no recourse.
          throw new UnsupportedOperationException(method.getName());
        }
      }
    }
  }

  private final Supplier<?> newDefaultTargetSupplier(final Method method, final Object[] args) {
    final Supplier<?> newDefaultTargetSupplier;
    final Object defaultTarget = this.defaultTargetSupplier.get();
    if (defaultTarget == null) {
      newDefaultTargetSupplier = Configured::returnNull;
    } else {
      newDefaultTargetSupplier = () -> {
        try {
          return method.invoke(defaultTarget, args);
        } catch (final ReflectiveOperationException roe) {
          throw new UndeclaredThrowableException(roe, roe.getMessage()); // TODO: improve exception
        }
      };
    }
    return newDefaultTargetSupplier;
  }

  private final Object computeSubProxy(final Supplier<?> defaultTargetSupplier, final Path p) {
    return this.computeProxy(p, this.butWith(p, defaultTargetSupplier)::invoke);
  }

  private final Object computeProxy(final Path p, final InvocationHandler invocationHandler) {
    return Proxy.newProxyInstance(p.classLoader(), new Class<?>[] { p.targetClass() }, invocationHandler);
  }

  private final <U> Configured<U> butWith(final Path path, final Supplier<? extends U> defaultTargetSupplier) {
    return
      new Configured<>(path,
                       this.applicationQualifiers,
                       defaultTargetSupplier,
                       this.valueSuppliers,
                       this.pathComponentFunction,
                       this.isProxiableFunction);
  }

  private final MethodHandler methodHandler(final Method m) {
    return null; // TODO implement
  }
  
  private final Path path(final Method m) {
    return
      this.path.plus(m.getGenericReturnType(),
                     this.pathComponentFunction.apply(m.getName(),
                                                      boolean.class == m.getReturnType()));
  }

  private final Value<?> value(final Path path) {
    return this.value(path, this.valueSuppliers.apply(new QualifiedPath.Record(path, this.applicationQualifiers)));
  }

  private final Value<?> value(final Path path, final Collection<? extends ValueSupplier> valueSuppliers) {
    return ValueSupplier.resolve(valueSuppliers, new QualifiedPath.Record(path, this.applicationQualifiers), this.valueSuppliers);
  }


  /*
   * Static methods.
   */


  private static final String methodName(final CharSequence cs, final boolean ignored) {
    return cs.toString();
  }

  /**
   * Given a {@link CharSequence} normally representing the name of a
   * "getter" method, and a {@code boolean} indicating whether the
   * method in question returns a {@code boolean}, applies the rules
   * declared by the Java Beans specification to the name and yields
   * the result.
   *
   * @param cs a {@link CharSequence} naming a "getter" method; may be
   * {@code null} in which case {@code null} will be returned
   *
   * @param methodReturnsBoolean {@code true} if the method named by
   * the supplied {@link CharSequence} has {@code boolean} as its
   * return type
   *
   * @return the property name corresponding to the supplied method
   * name, according to the rules of the Java Beans specification
   *
   * @nullability This method may return {@code null} but only when
   * {@code cs} is {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @see #decapitalize(CharSequence)
   */
  @SuppressWarnings("fallthrough")
  public static final String propertyName(final CharSequence cs, final boolean methodReturnsBoolean) {
    if (cs == null) {
      return null;
    } else {
      final int length = cs.length();
      if (length <= 2) {
        return decapitalize(cs);
      } else if (methodReturnsBoolean) {
        switch (cs.charAt(0)) {
        case 'i':
          if (cs.charAt(1) == 's') {
            return decapitalize(cs.subSequence(2, length));
          }
        case 'g':
          if (length > 3 && cs.charAt(1) == 'e' && cs.charAt(2) == 't') {
            return decapitalize(cs.subSequence(3, length));
          }
        default:
          return decapitalize(cs);
        }
      } else if (length > 3) {
        switch (cs.charAt(0)) {
        case 'g':
          if (cs.charAt(1) == 'e' && cs.charAt(2) == 't') {
            return decapitalize(cs.subSequence(3, length));
          }
        default:
          return decapitalize(cs);
        }
      } else {
        return decapitalize(cs);
      }
    }
  }

  /**
   * Decapitalizes the supplied {@link CharSequence} according to the
   * rules of the Java Beans specification.
   *
   * @param cs the {@link CharSequence} to decapitalize; may be {@code
   * null} in which case {@code null} will be returned
   *
   * @return the decapitalized {@link String} or {@code null}
   *
   * @nullability This method may return {@code null} but only when
   * {@code cs} is {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   */
  public static final String decapitalize(final CharSequence cs) {
    if (cs == null) {
      return null;
    } else if (cs.isEmpty() || Character.isLowerCase(cs.charAt(0))) {
      return cs.toString();
    } else if (cs.length() == 1) {
      return cs.toString().toLowerCase();
    } else if (Character.isUpperCase(cs.charAt(1))) {
      return cs.toString();
    } else {
      final char[] chars = cs.toString().toCharArray();
      chars[0] = Character.toLowerCase(chars[0]);
      return String.valueOf(chars);
    }
  }

  public static final List<ValueSupplier> valueSupplierServices(final QualifiedPath qualifiedPath) {
    return loadedValueSuppliers.get(ValueSupplier.class)
      .stream()
      .filter(vs -> vs.respondsFor(qualifiedPath))
      .toList();
  }

  public static final void clearValueSupplierServices() {
    loadedValueSuppliers.remove(ValueSupplier.class);
  }

  private static final boolean isGetter(final Method m) {
    if (m != null && m.getDeclaringClass() != Object.class && m.getParameterCount() == 0) {
      final Object returnType = m.getReturnType();
      return returnType != void.class && returnType != Void.class;
    } else {
      return false;
    }
  }

  private static final boolean isProxiable(final Type genericReturnType) {
    final Class<?> c = Types.rawClass(genericReturnType);
    if (c != null && c.isInterface()) {
      for (final Method m : c.getMethods()) {
        if (isGetter(m)) {
          return true;
        }
      }
    }
    return false;
  }

  private static final <T> T returnNull() {
    return null;
  }

}
