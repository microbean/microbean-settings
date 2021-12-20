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

import java.util.stream.Stream;

import org.microbean.settings.api.Loader;
import org.microbean.settings.api.Path;

/**
 * A service provider of {@link Value}s that might be suitable for a
 * {@link Loader} implementation to return.
 *
 * <p>{@link Provider} instances are subordinate to {@link
 * org.microbean.settings.Settings}.</p>
 *
 * <p>Any {@link Provider} implementation must have a {@code public}
 * constructor that has no arguments.</p>
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see #get(Loader, Path)
 *
 * @see AbstractProvider
 *
 * @see org.microbean.settings.Settings
 */
@FunctionalInterface
public interface Provider {


  /*
   * Instance methods.
   */


  /**
   * Returns a {@link Type} representing the upper bound of all
   * possible {@linkplain Value values} {@linkplain #get(Loader,
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
   * possible {@linkplain Value values} {@linkplain #get(Loader,
   * Path) supplied} by this {@link Provider}; never {@code null}
   *
   * @nullability This method does not, and overrides of this method
   * must not, return {@code null}.
   *
   * @idempotency This method is, and overrides of this method must
   * be, idempotent and deterministic.
   *
   * @threadsafety This method is, and overrides of this method must
   * be, safe for concurrent use by multiple threads.
   */
  public default Type upperBound() {
    return Object.class;
  }

  public default Stream<Path<?>> paths() {
    return Stream.empty();
  }

  /**
   * Returns a {@link Value} suitable for the supplied {@link
   * Loader} and {@link Path}, or {@code null} if there is no such
   * {@link Value} now <strong>and if there never will be such a
   * {@link Value}</strong> for the supplied arguments.
   *
   * <p>The following assertions will be true when this method is
   * called in the normal course of events:</p>
   *
   * <ul>
   *
   * <li>{@code assert absolutePath.isAbsolute();}</li>
   *
   * <li>{@code assert
   * absolutePath.startsWith(requestor.absolutePath());}</li>
   *
   * <li>{@code assert
   * !absolutePath.equals(requestor.absolutePath());}</li>
   *
   * </ul>
   *
   * @param <T> the type the supplied {@link Path} is typed with
   *
   * @param requestor the {@link Loader} seeking a {@link Value};
   * must not be {@code null}
   *
   * @param absolutePath an {@linkplain Path#isAbsolute() absolute
   * <code>Path</code>} for which the supplied {@link Loader} is
   * seeking a value; must not be {@code null}
   *
   * @return a {@link Value} more or less suitable for the combination
   * of the supplied {@link Loader} and {@link Path}, or {@code
   * null} if there is no such {@link Value} now <strong>and if there
   * never will be such a {@link Value}</strong> for the supplied
   * arguments
   *
   * @exception NullPointerException if either {@code requestor} or
   * {@code absolutePath} is {@code null}
   *
   * @exception IllegalArgumentException if {@code absolutePath}
   * {@linkplain Path#isAbsolute() is not absolute}
   *
   * @nullability Implementations of this method may return {@code
   * null}.
   *
   * @threadsafety Implementations of this method must be safe for
   * concurrent use by multiple threads.
   *
   * @idempotency Implementations of this method must be idempotent
   * but are not assumed to be deterministic.
   */
  public <T> Value<T> get(final Loader<?> requestor, final Path<T> absolutePath);

}
