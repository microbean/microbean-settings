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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

import java.util.Arrays;

final class Types {

  private Types() {
    super();
  }

  /**
   * Returns an array of {@link Class}es representing upper bounds
   * each element of which is a {@link Class} of which an object
   * bearing the supplied {@link Type} {@link Class#isInstance(Object)
   * is an instance}, or {@code null} if the calculation of these
   * bounds is impossible or would yield an ambiguous result.
   *
   * <p>This method may return {@code null} to indicate that
   * calculation of these bounds is impossible or would yield an
   * ambiguous result.</p>
   *
   * @param type a {@link Type}; may be {@code null} in which case
   * {@code null} will be returned
   *
   * @return an array of {@link Class}es representing upper bounds
   * each element of which is a {@link Class} of which an object
   * bearing the supplied {@link Type} {@link Class#isInstance(Object)
   * is an instance}, or {@code null} if the calculation of these
   * bounds is impossible or would yield an ambiguous result
   *
   * @nullability This method may return {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads, provided that operations on normally JDK-supplied {@link
   * Type} instances are also safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   */
  public static final Class<?>[] rawClasses(final Type type) {
    if (type == null) {
      return null;
    } else if (type instanceof TypeVariable<?> tv) {
      return rawClasses(tv);
    } else {
      final Class<?> c = rawClass(type);
      return c == null ? null : new Class<?>[] { c };
    }
  }

  public static final Class<?>[] rawClasses(final TypeVariable<?> tv) {
    if (tv == null) {
      return null;
    } else {
      final Type[] bounds = tv.getBounds();
      if (bounds.length == 1) {
        return rawClasses(bounds[0]);
      } else {
        assert bounds.length > 0;
        final Class<?>[] classes = new Class<?>[bounds.length];
        for (int i = 0; i < bounds.length; i++) {
          // If a TypeVariable has more than one bound, then all bounds
          // are guaranteed not to be TypeVariables. See
          // https://docs.oracle.com/javase/specs/jls/se17/html/jls-4.html#jls-TypeBound.
          assert !(bounds[i] instanceof TypeVariable);
          classes[i] = rawClass(bounds[i]);
        }
        return classes;
      }
    }
  }

  public static final Class<?> rawClass(final Type type) {
    if (type instanceof Class<?> c) {
      return c;
    } else if (type instanceof ParameterizedType ptype) {
      return rawClass(ptype);
    } else if (type instanceof TypeVariable<?> tv) {
      return rawClass(tv);
    } else if (type instanceof GenericArrayType gat) {
      return rawClass(gat);
    } else if (type instanceof WildcardType wt) {
      return rawClass(wt);
    } else {
      return null;
    }
  }

  public static final Class<?> rawClass(final ParameterizedType type) {
    return type == null ? null : rawClass(type.getRawType());
  }

  public static final Class<?> rawClass(final TypeVariable<?> typeVariable) {
    if (typeVariable != null) {
      final Type[] bounds = typeVariable.getBounds();
      assert bounds.length > 0;
      if (bounds.length == 1) {
        final Type soleBound = bounds[0];
        if (soleBound instanceof TypeVariable<?> soleTypeVariableBound) {
          // The sole bound is a TypeVariable itself, which may have
          // multiple bounds.
          final Class<?>[] rawClasses = rawClasses(soleTypeVariableBound);
          if (rawClasses != null && rawClasses.length == 1) {
            return rawClasses[0];
          }
        } else {
          // The Java language guarantees that a TypeVariable with
          // multiple bounds will not have a TypeVariable among its
          // bounds. See
          // https://docs.oracle.com/javase/specs/jls/se17/html/jls-4.html#jls-TypeBound.
          assert !(soleBound instanceof TypeVariable);
          return rawClass(soleBound);
        }
      }
    }
    return null;
  }

  public static final Class<?> rawClass(final GenericArrayType genericArrayType) {
    if (genericArrayType == null) {
      return null;
    } else {
      final Type componentType = genericArrayType.getGenericComponentType();
      if (componentType instanceof TypeVariable<?> tv) {
        final Class<?>[] rawClasses = rawClasses(tv);
        if (rawClasses == null || rawClasses.length > 1) {
          return null;
        }
      }
      final Class<?> c = rawClass(componentType);
      return c == null ? null : Array.newInstance(c, 0).getClass();
    }
  }

  public static final Class<?> rawClass(final WildcardType wildcardType) {
    if (wildcardType != null) {
      // Despite the presence of array-typed bounds accessors for both
      // upper and lower bounds, the Java language specification as of
      // version 17 states that a WildcardType may have a maximum of
      // one upper bound and one lower bound, and, additionally, when
      // a lower bound (super) is specified, then the upper bound will
      // be Object.  See also
      // https://docs.oracle.com/javase/specs/jls/se17/html/jls-4.html#jls-WildcardBounds
      // and https://stackoverflow.com/a/6645454/208288 and
      // https://github.com/openjdk/jdk/blob/7c9868c0b3c9bd3d305e71f91596190813cdccce/src/java.base/share/classes/java/lang/reflect/WildcardType.java#L77-L79
      // and
      // https://github.com/AdoptOpenJDK/openjdk-jdk8u-backup-06-sep-2018/blob/90e5951446532bbb7ca07ba524267fb028c63abe/jdk/src/share/classes/java/lang/reflect/WildcardType.java#L80-L82
      final Type[] upperBounds = wildcardType.getUpperBounds();
      switch (upperBounds.length) {
      case 1:
        final Type[] lowerBounds = wildcardType.getLowerBounds();
        if (lowerBounds.length == 0 || Arrays.equals(upperBounds, lowerBounds)) {
          final Class<?>[] rawClasses = rawClasses(upperBounds[0]);
          if (rawClasses != null && rawClasses.length == 1) {
            return rawClasses[0];
          }
        }
        break;
      case 0:
        throw new AssertionError();
      default:
        throw new IllegalArgumentException("WildcardType instances with multiple upper bounds are not supported");
      }
    }
    return null;
  }
  
}
