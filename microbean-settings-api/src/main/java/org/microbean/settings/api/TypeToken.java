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

import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

import java.util.Objects;
import java.util.Optional;

public abstract class TypeToken<T> implements AutoCloseable, Constable, Serializable, Type {


  /*
   * Static fields.
   */


  private static final long serialVersionUID = 1L;

  private static final ActualTypeArgumentExtractor extractor = new ActualTypeArgumentExtractor(TypeToken.class, 0);


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
    return extractor.get(this.getClass());
  }

  @Override
  public final void close() {
    extractor.remove(this.getClass());
  }

  public final Class<?> erase() {
    return erase(this.type());
  }

  @Override // Constable
  public final Optional<? extends ConstantDesc> describeConstable() {
    return Optional.ofNullable(this.erase()).flatMap(Class::describeConstable);
  }

  @Override // Type
  public String getTypeName() {
    return this.type().getTypeName();
  }

  @Override // Object
  public int hashCode() {
    return this.type().hashCode();
  }

  @Override // Object
  public boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other instanceof TypeToken<?> tt) {
      return Objects.equals(this.type(), tt.type());
    } else if (other instanceof Type t) {
      return this.type().equals(t);
    } else {
      return false;
    }
  }

  @Override // Object
  public String toString() {
    return this.type().getTypeName();
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
    } else if (type instanceof TypeToken<?> tt) {
      return erase(tt.type());
    } else {
      return null;
    }
  }


  /*
   * Inner and nested classes.
   */


  private static final class ActualTypeArgumentExtractor extends ClassValue<Type> {

    private final Class<?> stopClass;

    private final int index;

    private ActualTypeArgumentExtractor(final Class<?> stopClass, final int index) {
      super();
      if (stopClass.isInterface() || !Modifier.isAbstract(stopClass.getModifiers())) {
        throw new IllegalArgumentException("stopClass: " + stopClass.getName());
      } else if (index < 0 || index >= stopClass.getTypeParameters().length) {
        throw new IndexOutOfBoundsException(index);
      }
      this.stopClass = stopClass;
      this.index = index;
    }

    @Override
    protected final Type computeValue(final Class<?> c) {
      final Class<?> lssc = this.leastSpecificSubclass(c);
      if (lssc == null) {
        return null;
      } else {
        return ((ParameterizedType)lssc.getGenericSuperclass()).getActualTypeArguments()[this.index];
      }
    }

    private final Class<?> leastSpecificSubclass(final Class<?> c) {
      if (c == null || c == this.stopClass || c == Object.class) {
        return null;
      } else {
        final Class<?> sc = c.getSuperclass();
        return sc == this.stopClass ? c : this.leastSpecificSubclass(sc);
      }
    }

  }

}
