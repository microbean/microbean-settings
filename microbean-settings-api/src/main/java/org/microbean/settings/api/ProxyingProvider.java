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
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.microbean.development.annotation.Convenience;

import org.microbean.type.Types;

public class ProxyingProvider extends AbstractProvider<Object> {


  /*
   * Instance fields.
   */


  private final ConcurrentMap<Path, Object> proxies;


  /*
   * Constructors.
   */


  public ProxyingProvider() {
    super();
    this.proxies = new ConcurrentHashMap<>();
  }


  /*
   * Instance methods.
   */


  @Override // Provider
  public final boolean isSelectable(final SupplierBroker<?> broker,
                                    final Qualifiers qualifiers,
                                    final Supplier<?> parentSupplier,
                                    final Path path) {
    return
      super.isSelectable(broker, qualifiers, parentSupplier, path) &&
      this.isProxiable(broker, qualifiers, parentSupplier, path);
  }

  protected boolean isProxiable(final SupplierBroker<?> broker,
                                final Qualifiers qualifiers,
                                final Supplier<?> parentSupplier,
                                final Path path) {
    return path.type() instanceof Class<?> c && c.isInterface();
  }

  @Override // Provider
  public final Value<?> get(final SupplierBroker<?> broker,
                            final Qualifiers qualifiers,
                            final Supplier<?> parentSupplier,
                            final Path path) {
    return
      new Value<>(this.qualifiers(broker, qualifiers, parentSupplier, path),
                  this.path(broker, qualifiers, parentSupplier, path),
                  this.proxies.computeIfAbsent(broker.path().plus(path),
                                               p -> this.newProxyInstance(broker,
                                                                          qualifiers,
                                                                          parentSupplier,
                                                                          path,
                                                                          Types.erase(path.type()))));
  }

  protected Qualifiers qualifiers(final SupplierBroker<?> broker,
                                  final Qualifiers qualifiers,
                                  final Supplier<?> parentSupplier,
                                  final Path path) {
    return Qualifiers.of();
  }

  protected Path path(final SupplierBroker<?> broker,
                      final Qualifiers qualifiers,
                      final Supplier<?> parentSupplier,
                      final Path path) {
    return Path.of(path.type());
  }

  protected Proxy newProxyInstance(final SupplierBroker<?> broker,
                                   final Qualifiers qualifiers,
                                   final Supplier<?> parentSupplier,
                                   final Path path,
                                   final Class<?> iface) {
    return
      (Proxy)Proxy.newProxyInstance(iface.getClassLoader(),
                                    new Class<?>[] { iface },
                                    new Handler(broker,
                                                (m, args) -> Path.of(Accessor.of(propertyName(m.getName(),
                                                                                              boolean.class == m.getReturnType()),
                                                                                 Arrays.asList(m.getParameterTypes()),
                                                                                 stringArgs(args)),
                                                                     m.getGenericReturnType())));
  }


  /*
   * Static methods.
   */


  private static final List<String> stringArgs(final Object[] args) {
    if (args == null || args.length <= 0) {
      return List.of();
    } else {
      final List<String> list = new ArrayList<>(args.length);
      for (final Object arg : args) {
        list.add(String.valueOf(arg));
      }
      return Collections.unmodifiableList(list);
    }
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
   * name, according to the rules of the Java Beans specification, or
   * {@code null} (only if {@code cs} is {@code null})
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
  @Convenience
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


  /*
   * Inner and nested classes.
   */


  private static final class Handler implements InvocationHandler {

    private final SupplierBroker<?> broker;

    private final BiFunction<? super Method, ? super Object[], ? extends Path> pathFunction;

    private Handler(final SupplierBroker<?> broker,
                    final BiFunction<? super Method, ? super Object[], ? extends Path> pathFunction) {
      super();
      this.broker = Objects.requireNonNull(broker, "broker");
      this.pathFunction = Objects.requireNonNull(pathFunction, "pathFunction");
    }

    @Override // InvocationHandler
    public final Object invoke(final Object proxy, final Method m, final Object[] args) throws ReflectiveOperationException {
      final Object returnValue;
      if (m.getDeclaringClass() == Object.class) {
        returnValue =
          switch (m.getName()) {
          case "hashCode" -> System.identityHashCode(proxy);
          case "equals" -> proxy == args[0];
          case "toString" -> proxy.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(proxy));
          default -> throw new AssertionError("method: " + m);
          };
      } else {
        final SupplierBroker<Object> broker = this.broker.plus(this.pathFunction.apply(m, args), () -> defaultValue(proxy, m, args));
        returnValue = broker.get();
      }
      return returnValue;
    }

    private static final Object defaultValue(final Object proxy, final Method m, final Object[] args) {
      final Object returnValue;
      if (m.isDefault()) {
        try {
          // If the current method is a default method of the proxied
          // interface, invoke it.
          returnValue = InvocationHandler.invokeDefault(proxy, m, args);
        } catch (final UnsupportedOperationException | Error e) {
          throw e;
        } catch (final Exception e) {
          throw new UnsupportedOperationException(m.getName(), e);
        } catch (final Throwable e) {
          throw new AssertionError(e.getMessage(), e);
        }
      } else {
        // We have no recourse.
        throw new UnsupportedOperationException(m.getName());
      }
      return returnValue;
    }

  }
}
