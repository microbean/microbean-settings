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

import org.microbean.settings.api.Configured;
import org.microbean.settings.api.Path;

public interface AmbiguityHandler {

  public default void providerRejected(final Configured<?> rejector, final Path<?> absolutePath, final Provider provider) {

  }

  public default void valueRejected(final Configured<?> rejector, final Path<?> absolutePath, final Provider provider, final Value<?> value) {

  }

  /**
   * Given two {@link Value}s and some contextual objects, chooses one
   * over the other and returns it, or synthesizes a new {@link Value}
   * and returns that, or indicates that disambiguation is impossible
   * by returning {@code null}.
   *
   * @param <U> the type of objects the {@link Value}s in question can
   * supply
   *
   * @param requestor the {@link Configured} currently seeking a
   * {@link Value}; must not be {@code null}
   *
   * @param absolutePath an {@linkplain Path#isAbsolute() absolute
   * <code>Path</code>} for which a value is being sought; must not be
   * {@code null}
   *
   * @param p0 the {@link Provider} that supplied the first {@link
   * Value}; must not be {@code null}
   *
   * @param v0 the first {@link Value}; must not be {@code null}
   *
   * @param p1 the {@link Provider} that supplied the second {@link
   * Value}; must not be {@code null}
   *
   * @param v1 the second {@link Value}; must not be {@code null}
   *
   * @return the {@link Value} to use instead; ordinarily one of the
   * two supplied {@link Value}s but may be {@code null} to indicate
   * that disambiguation was impossible, or an entirely different
   * {@link Value} altogether
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @nullability The default implementation of this method and its
   * overrides may return {@code null}.
   *
   * @threadsafety The default implementation of this method is, and
   * its overrides must be, safe for concurrent use by multiple
   * threads.
   *
   * @idempotency The default implementation of this method is, and
   * its overrides must be, idempotent. The default implementation of
   * this method is deterministic, but its overrides need not be.
   */
  public default <U> Value<U> disambiguate(final Configured<?> requestor,
                                           final Path<?> absolutePath,
                                           final Provider p0,
                                           final Value<U> v0,
                                           final Provider p1,
                                           final Value<U> v1) {
    return null;
  }

}
