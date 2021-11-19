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

import java.io.Serializable;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

import java.util.Objects;
import java.util.Optional;

public abstract class TypeToken<T> implements AutoCloseable, Serializable {


  /*
   * Static fields.
   */


  private static final long serialVersionUID = 1L;


  /*
   * Constructors.
   */


  protected TypeToken() {
    super();
  }


  /*
   * Instance methods.
   */


  public final Type type() {
    return ActualTypeArgumentExtractor.INSTANCE.get(this.getClass());
  }

  @Override // AutoCloseable
  public final void close() {
    ActualTypeArgumentExtractor.INSTANCE.remove(this.getClass());
  }

  public final Class<?> erase() {
    return erase(this.type());
  }

  public String getTypeName() {
    final Type type = this.type();
    return type == null ? null : type.getTypeName();
  }

  @Override // Object
  public int hashCode() {
    final Type type = this.type();
    return type == null ? 0 : type.hashCode();
  }

  @Override // Object
  public boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other instanceof TypeToken<?> tt) {
      return Objects.equals(this.type(), tt.type());
    } else {
      return false;
    }
  }

  @Override // Object
  public String toString() {
    final Type type = this.type();
    return type == null ? "null" : type.toString();
  }


  /*
   * Static methods.
   */


  private static final Class<?> erase(final Type type) {
    if (type instanceof Class<?> c) {
      return c;
    } else if (type instanceof ParameterizedType pt) {
      return erase(pt.getRawType());
    } else if (type instanceof GenericArrayType gat) {
      return Array.newInstance(erase(gat.getGenericComponentType()), 0).getClass();
    } else if (type instanceof TypeVariable<?> tv) {
      return erase(tv.getBounds()[0]);
    } else if (type instanceof WildcardType wt) {
      return erase(wt.getUpperBounds()[0]);
    } else {
      return null;
    }
  }


  /*
   * Inner and nested classes.
   */


  private static final class ActualTypeArgumentExtractor extends ClassValue<Type> {

    private static final ActualTypeArgumentExtractor INSTANCE = new ActualTypeArgumentExtractor();

    private final Class<?> stopClass;

    private final int index;

    private ActualTypeArgumentExtractor() {
      this(ActualTypeArgumentExtractor.class.getDeclaringClass(), 0);
    }

    private ActualTypeArgumentExtractor(final Class<?> stopClass, final int index) {
      super();
      if (index < 0 || index >= stopClass.getTypeParameters().length) {
        throw new IndexOutOfBoundsException(index);
      } else if (!Modifier.isAbstract(stopClass.getModifiers()) && !stopClass.isInterface() || stopClass.isArray()) {
        throw new IllegalArgumentException("stopClass: " + stopClass.getName());
      }
      this.stopClass = stopClass;
      this.index = index;
    }

    @Override
    protected final Type computeValue(final Class<?> c) {
      final Class<?> lssc = this.leastSpecificProperSubclass(c);
      return lssc == null ? null : ((ParameterizedType)lssc.getGenericSuperclass()).getActualTypeArguments()[this.index];
    }

    private final Class<?> leastSpecificProperSubclass(final Class<?> c) {
      if (c == null || c == this.stopClass || c == Object.class) {
        return null;
      } else {
        final Class<?> sc = c.getSuperclass();
        return sc == this.stopClass ? c : this.leastSpecificProperSubclass(sc);
      }
    }

  }

}
