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

import org.microbean.settings.api.Loader;
import org.microbean.settings.api.Path;
import org.microbean.settings.api.Qualifiers;
import org.microbean.settings.api.TypeToken.ActualTypeArgumentExtractor;

/**
 * A skeletal {@link Provider} implementation.
 *
 * @param <T> the upper bound of the types of objects produced by the
 * {@link #get(Loader, Path)} method

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


  private static final ActualTypeArgumentExtractor actualTypeArgumentExtractor = new ActualTypeArgumentExtractor(AbstractProvider.class, 0);
  

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
   * possible {@linkplain Value values} {@linkplain #get(Loader,
   * Path) supplied} by this {@link AbstractProvider}.
   *
   * <p>The value returned is harvested from the sole type argument
   * supplied to {@link AbstractProvider} by a concrete subclass.</p>
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
   *
   * @see ActualTypeArgumentExtractor
   */
  @Override // Provider
  public final Type upperBound() {
    return actualTypeArgumentExtractor.get(this.getClass());
  }

}
