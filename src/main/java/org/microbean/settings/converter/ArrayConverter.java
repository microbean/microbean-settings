/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2020 microBean™.
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
package org.microbean.settings.converter;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.util.Collection;
import java.util.Objects;

import java.util.function.Function;

import java.util.regex.Pattern;

import javax.enterprise.inject.Vetoed;

import org.microbean.settings.Converter;
import org.microbean.settings.Value;

/**
 * An abstract class that can serve as the base class for
 * implementations of, but which deliberately does not itself
 * implement, the {@link Converter} interface.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see Converter
 *
 * @see #convert(Value)
 */
@Vetoed
public abstract class ArrayConverter<T> {


  /*
   * Static fields.
   */

  
  private static final long serialVersionUID = 1L;


  /*
   * Instance fields.
   */

  
  private final Class<?> arrayComponentType;
  
  private final Converter<? extends Collection<? extends T>> collectionConverter;


  /*
   * Constructors.
   */

  
  /**
   * Creates a new {@link ArrayConverter}.
   *
   * @param collectionConverter a {@link Converter} that returns
   * {@link Collection}s whose elements are of the appropriate type
   * from its {@link Converter#convert(Value)} method; must not be
   * {@code null}
   *
   * @param arrayComponentType a {@link Class} identifying the
   * {@linkplain Class#getComponentType() component type} of the array
   * type in question; must not be {@code null}
   *
   * @exception NullPointerException if either {@code
   * collectionConverter} or {@code arrayComponentType} is {@code
   * null}
   *
   * @see #convert(Value)
   */
  protected ArrayConverter(final Converter<? extends Collection<? extends T>> collectionConverter) {
    super();
    Type genericSuperclass = this.getClass();
    while (genericSuperclass instanceof Class) {
      genericSuperclass = ((Class<?>)genericSuperclass).getGenericSuperclass();
    }
    assert genericSuperclass instanceof ParameterizedType : "Unexpected genericSuperclass: " + genericSuperclass;
    final ParameterizedType genericSupertype = (ParameterizedType)genericSuperclass;
    final Type[] actualTypeArguments = genericSupertype.getActualTypeArguments();
    assert actualTypeArguments != null;
    assert actualTypeArguments.length == 1;
    final Type firstTypeArgument = actualTypeArguments[0];
    assert firstTypeArgument instanceof Class;
    this.arrayComponentType = (Class<?>)firstTypeArgument;
    this.collectionConverter = Objects.requireNonNull(collectionConverter);
  }


  /*
   * Instance methods.
   */
  

  /**
   * Converts the supplied {@link Value} into an appropriately-typed
   * array and returns the result.
   *
   * @param value the {@link Value} to convert; may be {@code null}
   *
   * @return an appropriately-typed array, or {@code null}
   *
   * @nullability This method does and its overrides are permitted to
   * return {@code null}.
   *
   * @idempotency No guarantees are made about the idempotency of this
   * method or its overrides.
   *
   * @threadsafety This method is and its overrides must be safe for
   * concurrent use by multiple threads.
   *
   * @see Converter#convert(Value)
   */
  public T[] convert(final Value value) {
    final Collection<? extends T> collection = this.collectionConverter.convert(value);
    final T[] returnValue;
    if (collection == null) {
      returnValue = null;
    } else {
      @SuppressWarnings("unchecked")
      final T[] targetArray = (T[])Array.newInstance(this.arrayComponentType, collection.size());
      returnValue = collection.toArray(targetArray);
    }
    return returnValue;
  }

}
