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
package org.microbean.settings;

import java.lang.reflect.Type;

import javax.enterprise.util.TypeLiteral;

@FunctionalInterface
public interface ConverterProvider {

  /**
   * Returns a {@link Converter} capable of {@linkplain
   * Converter#convert(Value) converting} {@link Value}s into objects
   * of the supplied {@code type}.
   *
   * @param <T> the conversion type
   *
   * @param type a {@link Class} describing the conversion type; must
   * not be {@code null}
   *
   * @return a non-{@code null} {@link Converter} capable of
   * {@linkplain Converter#convert(Value) converting} {@link Value}s
   * into objects of the proper type
   *
   * @exception NullPointerException if {@code type} is {@code null}
   *
   * @exception IllegalArgumentException if no {@link Converter} is
   * available for the supplied {@code type}
   *
   * @idempotency Implementations of this method may return different
   * {@link Converter} instances over time given the same {@code
   * type}.
   *
   * @nullability Implementations of this method must never return
   * {@code null}.
   *
   * @threadsafety Implementations of this method must be safe for
   * concurrent use by multiple threads.
   *
   * @see #getConverter(Type)
   */
  default public <T> Converter<? extends T> getConverter(final Class<T> type) {
    @SuppressWarnings("unchecked")
    final Converter<? extends T> returnValue = (Converter<? extends T>)this.getConverter((Type)type);
    return returnValue;
  }

  /**
   * Returns a {@link Converter} capable of {@linkplain
   * Converter#convert(Value) converting} {@link Value}s into objects
   * of the supplied {@code type}.
   *
   * @param <T> the conversion type
   *
   * @param type a {@link TypeLiteral} describing the conversion type;
   * must not be {@code null}
   *
   * @return a non-{@code null} {@link Converter} capable of
   * {@linkplain Converter#convert(Value) converting} {@link Value}s
   * into objects of the proper type
   *
   * @exception NullPointerException if {@code type} is {@code null}
   *
   * @exception IllegalArgumentException if no {@link Converter} is
   * available for the supplied {@code type}
   *
   * @idempotency Implementations of this method may return different
   * {@link Converter} instances over time given the same {@code
   * type}.
   *
   * @nullability Implementations of this method must never return
   * {@code null}.
   *
   * @threadsafety Implementations of this method must be safe for
   * concurrent use by multiple threads.
   *
   * @see #getConverter(Type)
   */
  default public <T> Converter<? extends T> getConverter(final TypeLiteral<T> type) {
    @SuppressWarnings("unchecked")
    final Converter<? extends T> returnValue = (Converter<? extends T>)this.getConverter(type.getType());
    return returnValue;
  }

  /**
   * Returns a {@link Converter} capable of {@linkplain
   * Converter#convert(Value) converting} {@link Value}s into objects
   * of the supplied {@code type}.
   *
   * <p>Implementations of this method must not call either {@link
   * #getConverter(Class)} or {@link #getConverter(TypeLiteral)} or
   * undefined behavior may result.</p>
   *
   * @param type a {@link Type} describing the conversion type; must
   * not be {@code null}
   *
   * @return a non-{@code null} {@link Converter} capable of
   * {@linkplain Converter#convert(Value) converting} {@link Value}s
   * into objects of the proper type
   *
   * @exception NullPointerException if {@code type} is {@code null}
   *
   * @exception IllegalArgumentException if no {@link Converter} is
   * available for the supplied {@code type}
   *
   * @idempotency Implementations of this method may return different
   * {@link Converter} instances over time given the same {@code
   * type}.
   *
   * @nullability Implementations of this method must never return
   * {@code null}.
   *
   * @threadsafety Implementations of this method must be safe for
   * concurrent use by multiple threads.
   */
  public Converter<?> getConverter(final Type type);
  
}
