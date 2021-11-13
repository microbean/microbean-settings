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

import java.lang.reflect.Type;

import org.microbean.settings.api.Configured;
import org.microbean.settings.api.Path;

/**
 * A provider of {@link Value}s that might be suitable for a {@link
 * Configured} implementation to return.
 *
 * <p>{@link Provider} instances are subordinate to {@link
 * org.microbean.settings.Settings}.</p>
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see #isSelectable(Configured, Path)
 *
 * @see #get(Configured, Path)
 *
 * @see org.microbean.settings.Settings
 */
@FunctionalInterface
public interface Provider extends Prioritized {


  /*
   * Instance methods.
   */


  /**
   * Returns a {@link Type} representing the upper bound of all
   * possible {@linkplain Value values} {@linkplain #get(Configured,
   * Path) supplied} by this {@link Provider}.
   *
   * <p>Often the value returned by implementations of this method is
   * no more specific than simply {@link Object Object.class}.</p>
   *
   * <p>Implementations of this method must not return {@code
   * null}.</p>
   *
   * <p>The default implementation of this method returns {@link
   * Object Object.class}.</p>
   *
   * @return a {@link Type} representing the upper bound of all
   * possible {@linkplain Value values} {@linkplain #get(Configured,
   * Path) supplied} by this {@link Provider}; never {@code null}
   *
   * @nullability Implementations of this method must not return
   * {@code null}.
   *
   * @idempotency Implementations of this method must be idempotent
   * and deterministic.
   *
   * @threadsafety Implementations of this method must be safe for
   * concurrent use by multiple threads.
   */
  public default Type upperBound() {
    return Object.class;
  }

  public default boolean isSelectable(final Configured<?> supplier, final Path absolutePath) {
    if (!absolutePath.isAbsolute()) {
      throw new IllegalArgumentException("absolutePath: " + absolutePath);
    }
    return AssignableType.of(this.upperBound()).isAssignable(absolutePath.type());
  }

  public Value<?> get(final Configured<?> supplier, final Path absolutePath);

}