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
package org.microbean.settings.provider;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.microbean.settings.provider.Provider;

/**
 * A skeletal {@link Provider} implementation.
 *
 * @param <T> the upper bound of the types of objects produced by the
 * {@link #get(Configured, Path)} method

 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see #upperBound()
 *
 * @see Provider
 */
public abstract class AbstractProvider<T> implements Provider {


  /*
   * Static fields.
   */


  private static final ClassValue<Type> type = new ClassValue<>() {
      @Override
      protected final Type computeValue(final Class<?> c) {
        if (c != AbstractProvider.class && AbstractProvider.class.isAssignableFrom(c)) {
          return ((ParameterizedType)c.getGenericSuperclass()).getActualTypeArguments()[0];
        } else {
          return null;
        }
      }
    };


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link AbstractProvider}.
   */
  protected AbstractProvider() {
    super();
  }

  /**
   * Returns a {@link Type} representing the upper bound of all
   * possible {@linkplain Value values} {@linkplain #get(Configured,
   * Path) supplied} by this {@link AbstractProvider}.
   *
   * <p>The value returned is harvested from the sole type parameter
   * of {@link AbstractProvider}.</p>
   *
   * @return the value of the sole type parameter of the {@link
   * AbstractProvider} class; never {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  @Override // Provider
  public final Type upperBound() {
    return type.get(this.getClass());
  }

}
