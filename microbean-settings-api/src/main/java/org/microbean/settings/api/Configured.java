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
import java.lang.reflect.UndeclaredThrowableException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import java.util.stream.Stream;

import org.microbean.development.annotation.Experimental;
import org.microbean.development.annotation.Incomplete;

import org.microbean.settings.api.ValueSupplier.Value;

@Experimental
@Incomplete
public final class Configured<T> implements Supplier<T> {


  /*
   * Static fields.
   */


  private static final ClassValue<List<ValueSupplier>> loadedValueSuppliers =
    new ClassValue<>() {
      @Override
      protected final List<ValueSupplier> computeValue(final Class<?> c) {
        if (ValueSupplier.class.equals(c)) {
          return
          ServiceLoader.load(ValueSupplier.class, ValueSupplier.class.getClassLoader())
          .stream()
          .map(ServiceLoader.Provider::get)
          .toList();
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

  private final BiFunction<? super Path, ? super Map<?, ?>, ? extends Collection<ValueSupplier>> valueSuppliers;

  private final BiFunction<? super String, ? super Boolean, ? extends String> pathComponentFunction;

  private final ClassLoader cl;


  /*
   * Constructors.
   */


  public Configured(final Class<T> rootType,
                    final Supplier<? extends T> defaultTargetSupplier,
                    final BiFunction<? super Path, ? super Map<?, ?>, ? extends Collection<ValueSupplier>> valueSuppliers) {
    this(new Path(rootType),
         Map.of(),
         defaultTargetSupplier,
         valueSuppliers,
         Configured::methodName);
  }

  public Configured(final Class<T> rootType,
                    final Supplier<? extends T> defaultTargetSupplier,
                    final BiFunction<? super Path, ? super Map<?, ?>, ? extends Collection<ValueSupplier>> valueSuppliers,
                    final BiFunction<? super String, ? super Boolean, ? extends String> pathComponentFunction) {
    this(new Path(rootType),
         Map.of(),
         defaultTargetSupplier,
         valueSuppliers,
         pathComponentFunction);
  }

  public Configured(final Class<T> rootType,
                    final Map<?, ?> applicationQualifiers,
                    final Supplier<? extends T> defaultTargetSupplier,
                    final BiFunction<? super Path, ? super Map<?, ?>, ? extends Collection<ValueSupplier>> valueSuppliers) {
    this(new Path(rootType),
         applicationQualifiers,
         defaultTargetSupplier,
         valueSuppliers,
         Configured::methodName);
  }

  public Configured(final Class<T> rootType,
                    final Map<?, ?> applicationQualifiers,
                    final Supplier<? extends T> defaultTargetSupplier,
                    final BiFunction<? super Path, ? super Map<?, ?>, ? extends Collection<ValueSupplier>> valueSuppliers,
                    final BiFunction<? super String, ? super Boolean, ? extends String> pathComponentFunction) {
    this(new Path(rootType),
         applicationQualifiers,
         defaultTargetSupplier,
         valueSuppliers,
         pathComponentFunction);
  }

  private Configured(final Path path,
                     final Map<?, ?> applicationQualifiers,
                     final Supplier<? extends T> defaultTargetSupplier,
                     final BiFunction<? super Path, ? super Map<?, ?>, ? extends Collection<ValueSupplier>> valueSuppliers,
                     final BiFunction<? super String, ? super Boolean, ? extends String> pathComponentFunction)
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
      this.valueSuppliers = (p, q) -> List.of();
    } else {
      this.valueSuppliers = valueSuppliers;
    }
    if (pathComponentFunction == null) {
      this.pathComponentFunction = Configured::methodName;
    } else {
      this.pathComponentFunction = pathComponentFunction;
    }
    this.cl = path.classLoader();
  }

  @SuppressWarnings("unchecked")
  public final T get() {
    return (T)this.proxies.computeIfAbsent(this.path, this::computeProxy);
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
          return
            this.proxies.computeIfAbsent(path,
                                         p -> {
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
                                           return this.computeSubProxy(newDefaultTargetSupplier, p);
                                         });
        } else {
          final T defaultTarget = this.defaultTargetSupplier.get();
          if (defaultTarget == null) {
            if (method.isDefault()) {
              try {
                return InvocationHandler.invokeDefault(proxy, method, args);
              } catch (final UnsupportedOperationException | Error e) {
                throw e;
              } catch (final Exception e) {
                throw new UnsupportedOperationException(method.getName(), e);
              } catch (final Throwable e) {
                throw new AssertionError(e.getMessage(), e);
              }
            } else {
              throw new UnsupportedOperationException(method.getName());
            }
          } else {
            return method.invoke(defaultTarget, args);
          }
        }
      } else {
        return value.get();
      }
    }
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
                       this.pathComponentFunction);
  }

  private final Path path(final Method m) {
    return
      this.path.plus(m.getGenericReturnType(),
                     this.pathComponentFunction.apply(m.getName(),
                                                      boolean.class == m.getReturnType()));
  }

  private final Value<?> value(final Path path) {
    return this.value(path, this.valueSuppliers.apply(path, this.applicationQualifiers));
  }

  private final Value<?> value(final Path path, final Collection<? extends ValueSupplier> valueSuppliers) {
    final Value<?> returnValue;
    if (valueSuppliers == null || valueSuppliers.isEmpty()) {
      returnValue = null;
    } else {
      Collection<Value<?>> bad = null;
      Value<?> candidate = null;
      for (final ValueSupplier valueSupplier : valueSuppliers) {
        final Value<?> v = valueSupplier == null ? null : valueSupplier.get(path, this.applicationQualifiers);
        if (v != null) {
          final Map<?, ?> qualifiers = v.qualifiers();
          if (viable(qualifiers)) {
            if (candidate == null || qualifiers.size() > candidate.qualifiers().size()) {
              candidate = v;
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
    final boolean returnValue;
    if (this.applicationQualifiers.isEmpty()) {
      returnValue = vsQualifiers == null || vsQualifiers.isEmpty();
    } else if (vsQualifiers == null || vsQualifiers.isEmpty()) {
      returnValue = false;
    } else {
      returnValue = this.applicationQualifiers.entrySet().containsAll(vsQualifiers.entrySet());
    }
    return returnValue;
  }


  /*
   * Static methods.
   */


  private static final String methodName(final CharSequence cs, final boolean ignored) {
    return cs.toString();
  }

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
          } else {
            return decapitalize(cs);
          }
        case 'g':
          if (length > 3 && cs.charAt(1) == 'e' && cs.charAt(2) == 't') {
            return decapitalize(cs.subSequence(3, length));
          } else {
            return decapitalize(cs);
          }
        default:
          return decapitalize(cs);
        }
      } else if (length > 3) {
        switch (cs.charAt(0)) {
        case 'g':
          if (cs.charAt(1) == 'e' && cs.charAt(2) == 't') {
            return decapitalize(cs.subSequence(3, length));
          } else {
            return decapitalize(cs);
          }
        default:
          return decapitalize(cs);
        }
      } else {
        return decapitalize(cs);
      }
    }
  }

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

  public static final List<ValueSupplier> loadValueSuppliers(final Path path, final Map<?, ?> applicationQualifiers) {
    return loadedValueSuppliers.get(ValueSupplier.class)
      .stream()
      .filter(vs -> vs.respondsFor(path, applicationQualifiers))
      .toList();
  }

  public static final void clearLoadedValueSuppliers() {
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
