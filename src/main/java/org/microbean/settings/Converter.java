/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2019–2020 microBean™.
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

import java.io.Serializable;

import java.util.function.Function;

/**
 * A {@link Function} that can convert a {@link Value} into some other
 * kind of object.
 *
 * @param <T> the conversion type
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see #convert(Value)
 */
@FunctionalInterface
public interface Converter<T> extends Function<Value, T>, Serializable {


  /*
   * Static fields.
   */


  /**
   * The version of this interface for {@link Serializable
   * serialization purposes}.
   */
  static final long serialVersionUID = 1L;


  /*
   * Default methods.
   */


  /**
   * Calls the {@link #convert(Value)} method and returns its result.
   *
   * @param value the {@link Value} to convert; may be {@code null}
   *
   * @return the result of the conversion (possibly {@code null})
   *
   * @nullability This method and its overrides may return {@code
   * null}.
   *
   * @idempotency No guarantees with respect to idempotency are made
   * about this method or its overrides.
   *
   * @threadsafety This method is and its overrides must be safe for
   * concurrent use by multiple threads.
   */
  default public T apply(final Value value) {
    return this.convert(value);
  }

   /**
   * Converts the supplied {@link Value} into the appropriate kind of
   * object and returns the result.
   *
   * <p>Implementations of this method must not call the {@link
   * #apply(Value)} method.</p>
   *
   * @param value the {@link Value} to convert; may be {@code null}
   *
   * @return the result of the conversion (possibly {@code null})
   *
   * @nullability This method's implementations may return {@code
   * null}.
   *
   * @idempotency No guarantees with respect to idempotency are made
   * about this method's implementations.
   *
   * @threadsafety This method is and its overrides must be safe for
   * concurrent use by multiple threads.
   */
  public T convert(final Value value);
  
}
