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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.microbean.development.annotation.Convenience;
import org.microbean.development.annotation.Experimental;
import org.microbean.development.annotation.Incomplete;

import org.microbean.settings.api.ValueSupplier.Value;

import org.microbean.type.CovariantTypeSemantics;
import org.microbean.type.TypeSemantics;
import org.microbean.type.Types;

@Experimental
@Incomplete
public class ProxyingValueSupplier implements ValueSupplier {


  private static final TypeSemantics typeSemantics = new CovariantTypeSemantics(false);
  

  /*
   * Instance fields.
   */


  /**
   * A thread-safe cache of {@link Object}s returned by various
   * invocations of the {@link Proxy#newProxyInstance(ClassLoader,
   * Class[], InvocationHandler)} method indexed by {@link
   * QualifiedPath.Record}s describing, primarily, the {@link Path} to
   * which they apply and incidentally the application qualifiers in
   * effect (which never change throughout the life of the
   * application).
   *
   * <p>Among other things, this means that the keys in this {@link
   * ConcurrentMap} will vary only in terms of the {@link Path}s they
   * logically contain.</p>
   *
   * <p>This field is never {@code null}.</p>
   *
   * <p>The fact that {@link QualifiedPath.Record} is used as a
   * key&mdash;rather than {@link Path}&mdash;is mainly for
   * convenience and is subject to change.</p>
   *
   * @nullability This field is never {@code null}.
   *
   * @threadsafety This field is safe for concurrent use by multiple
   * threads.
   */
  // Do NOT get clever and try to change this to ConcurrentMap<Path, Object>.
  private final ConcurrentMap<QualifiedPath.Record, Object> proxies;

  private final int basePriority;

  private Map<Path, Supplier<? extends Map<?, ?>>> qualifiersSupplierMap;

  private final Predicate<? super QualifiedPath> mayHandlePredicate;

  // This Function accepts a Path on purpose: the Supplier is for
  // defaults, i.e. independent of qualifiers, so there's no need for
  // a QualifiedPath here.
  private final Function<? super Path, ? extends Supplier<?>> defaultTargetSupplierFunction;

  private final Function<? super QualifiedPath, ? extends Collection<ValueSupplier>> valueSuppliers;

  private final BiFunction<? super String, ? super Boolean, ? extends String> pathComponentFunction;

  private final Function<? super Type, ? extends Boolean> isProxiableFunction;


  /*
   * Constructors.
   */


  public ProxyingValueSupplier() {
    this(-1000, null, null, null, null, null, null);
  }

  public ProxyingValueSupplier(final Supplier<?> defaultTargetSupplier) {
    this(-1000, null, null, p -> defaultTargetSupplier, null, null, null);
  }

  public ProxyingValueSupplier(final int basePriority,
                               final Map<? extends Path, ? extends Supplier<? extends Map<?, ?>>> qualifiersSupplierMap,
                               final Predicate<? super QualifiedPath> mayHandlePredicate,
                               final Function<? super Path, ? extends Supplier<?>> defaultTargetSupplierFunction,
                               final Function<? super QualifiedPath, ? extends Collection<ValueSupplier>> valueSuppliers,
                               final BiFunction<? super String, ? super Boolean, ? extends String> pathComponentFunction,
                               final Function<? super Type, ? extends Boolean> isProxiableFunction) {
    super();    
    this.proxies = new ConcurrentHashMap<>();
    this.basePriority = basePriority;
    this.qualifiersSupplierMap = qualifiersSupplierMap == null ? Map.of() : Map.copyOf(qualifiersSupplierMap);
    this.mayHandlePredicate = mayHandlePredicate == null ? qp -> true : mayHandlePredicate;
    this.defaultTargetSupplierFunction =
      defaultTargetSupplierFunction == null ? ProxyingValueSupplier::noDefaultTargetSupplier : defaultTargetSupplierFunction;
    this.valueSuppliers = valueSuppliers == null ? ValueSupplier::loadedValueSuppliers : valueSuppliers;
    this.pathComponentFunction = pathComponentFunction == null ? ProxyingValueSupplier::identity : pathComponentFunction;
    this.isProxiableFunction = isProxiableFunction == null ? ProxyingValueSupplier::isProxiable : isProxiableFunction;
  }


  /*
   * Instance methods.
   */


  @Override // Prioritized
  public int priority() {
    return this.basePriority;
  }

  @Override // ValueSupplier
  public int priority(final QualifiedPath qp) {
    if (this.mayHandle(qp)) {
      final Map<?, ?> qualifiers = this.qualifiersSupplierMap.get(qp.path()).get();
      return qualifiers == null ? 0 : qualifiers.size();
    } else {
      return Integer.MIN_VALUE;
    }
  }

  @Override // ValueSupplier
  public final boolean mayHandle(final QualifiedPath qualifiedPath) {
    final boolean returnValue;
    if (this.isProxiableFunction.apply(qualifiedPath.type())) {
      final Type qpType = qualifiedPath.type();
      boolean temp = false;
      for (final Entry<Path, Supplier<? extends Map<?, ?>>> entry : this.qualifiersSupplierMap.entrySet()) {
        final Path pathFragment = entry.getKey();
        if (typeSemantics.isAssignable(qpType, pathFragment.targetType()) && qualifiedPath.isAssignable(entry.getValue().get())) {
          temp = true;
          break;
        }
      }
      returnValue = temp;
    } else {
      returnValue = false;
    }
    return returnValue;
  }

  @Override // ValueSupplier
  public Value<?> get(final QualifiedPath qp,
                      final Function<? super QualifiedPath, ? extends Collection<ValueSupplier>> valueSuppliers) {
    if (this.mayHandle(qp)) {
      final Path path = qp.path();
      // Ensure the incoming QualifiedPath implementation is
      // immutable.
      final QualifiedPath.Record qpr =
        qp instanceof QualifiedPath.Record r ? r : new QualifiedPath.Record(path, qp.applicationQualifiers());
      final Map<?, ?> qualifiers = this.qualifiersSupplierMap.get(path).get();      
      return
        new Value<>(this.proxies.computeIfAbsent(qpr, k -> this.computeProxy(k, valueSuppliers)),
                    qpr.path(),
                    qualifiers,
                    this.priority(qpr));
    } else {
      return null;
    }
  }

  private final Object computeProxy(final QualifiedPath.Record qpr,
                                    final Function<? super QualifiedPath, ? extends Collection<ValueSupplier>> valueSuppliers) {
    final Path path = qpr.path();
    final Class<?> targetClass = path.targetClass();
    return
      Proxy.newProxyInstance(targetClass.getClassLoader(),
                             new Class<?>[] { targetClass },
                             new Handler(qpr,
                                         this.defaultTargetSupplierFunction.apply(path),
                                         this.pathComponentFunction,
                                         valueSuppliers));
  }


  /*
   * Static methods.
   */


  @Convenience
  public static final String identity(final CharSequence cs, final boolean ignored) {
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

  private static final boolean isProxiable(final QualifiedPath qp) {
    return isProxiable(qp.type());
  }
  
  private static final boolean isProxiable(final Path path) {
    return isProxiable(path.targetType());
  }
  
  private static final boolean isProxiable(final Type genericReturnType) {
    final Class<?> c = Types.erase(genericReturnType);
    if (c != null && c.isInterface()) {
      for (final Method m : c.getMethods()) {
        if (accessor(m)) {
          return true;
        }
      }
    }
    return false;
  }

  private static final boolean accessor(final Method m) {
    if (m != null && m.getDeclaringClass() != Object.class && m.getParameterCount() == 0) {
      final Object returnType = m.getReturnType();
      return returnType != void.class && returnType != Void.class;
    } else {
      return false;
    }
  }

  private static final Supplier<?> noDefaultTargetSupplier(final Path ignored) {
    return ProxyingValueSupplier::returnNull;
  }

  private static final <T> T returnNull() {
    return null;
  }


  /*
   * Inner and nested classes.
   */


  private static final class Handler implements InvocationHandler {

    private final QualifiedPath qp;

    private final Supplier<?> defaultTargetSupplier;

    private final BiFunction<? super String, ? super Boolean, ? extends String> pathComponentFunction;

    private final Function<? super QualifiedPath, ? extends Collection<ValueSupplier>> vsf;

    private Handler(final QualifiedPath qp,
                    final Supplier<?> defaultTargetSupplier,
                    final BiFunction<? super String, ? super Boolean, ? extends String> pathComponentFunction,
                    final Function<? super QualifiedPath, ? extends Collection<ValueSupplier>> vsf) {
      super();
      this.qp = Objects.requireNonNull(qp, "qp");
      this.defaultTargetSupplier = defaultTargetSupplier == null ? ProxyingValueSupplier::returnNull : defaultTargetSupplier;
      this.pathComponentFunction = pathComponentFunction == null ? ProxyingValueSupplier::identity : pathComponentFunction;
      this.vsf = vsf == null ? ValueSupplier::loadedValueSuppliers : vsf;
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
        final QualifiedPath newQualifiedPath =
          new QualifiedPath.Record(this.qp.path().plus(this.pathComponentFunction.apply(m.getName(),
                                                                                        boolean.class == m.getReturnType()),
                                                                  m.getGenericReturnType()),
                                   this.qp.applicationQualifiers());
        final Value<?> value = ValueSupplier.resolve(this.vsf.apply(newQualifiedPath), newQualifiedPath, this.vsf);
        if (value != null) {
          // If there's an explicit ValueSupplier for this method, use
          // it.  The ValueSupplier might of course be us.  The return
          // value of value.get() might very well be null and that's
          // fine.
          returnValue = value.get();
        } else {
          final Object defaultTarget = this.defaultTargetSupplier.get();
          if (defaultTarget != null) {
            // If the default target supplier supplies a default target,
            // then invoke the method on it.
            returnValue = m.invoke(defaultTarget, args);
          } else if (m.isDefault()) {
            try {
              // If the current method is a default method of the
              // proxied interface, invoke it.
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
        }
      }
      return returnValue;
    }

  }

}
