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
 * A service provider of {@link Value}s that might be suitable for a
 * {@link Configured} implementation to return.
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
 * @see #isSelectable(Configured, Path)
 *
 * @see #get(Configured, Path)
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

  /**
   * Returns {@code false} if this {@link Provider} implementation
   * <strong>absolutely will not</strong> provide values suitable for
   * the supplied {@link Configured} and {@link Path}.
   *
   * <p>Returning {@code true} from an implementation of this method
   * does <em>not</em> mean that a suitable value <em>will</em> be
   * supplied by the {@link #get(Configured, Path)} method, but it
   * means that one <em>might</em> be supplied.  Returning {@code
   * false} from this method will normally prevent the {@link
   * #get(Configured, Path)} method from being called at all.</p>
   *
   * <p>The default implementation of this method returns {@code true}
   * if and only if this {@link Provider}'s {@linkplain #upperBound()
   * upper bound} {@linkplain AssignableType#isAssignable(Type) is
   * assignable from} the supplied {@link Path}'s {@linkplain
   * Path#type() type}.  Overrides are strongly encouraged, but not
   * required, to call {@code Provider.super.isSelectable(supplier,
   * absolutePath)} from their implementation and to proceed only if
   * the call returns {@code true}.</p>
   *
   * @param supplier the {@link Configured} that may request a value
   * if this method returns {@code true}; must not be {@code null}
   *
   * @param absolutePath an {@linkplain Path#isAbsolute() absolute
   * <code>Path</code>} for which the supplied {@link Configured} is
   * seeking a value; must not be {@code null}
   *
   * @return {@code false} if this {@link Provider} implementation
   * absolutely cannot provide values suitable for the supplied {@link
   * Configured} and {@link Path}; {@code true} otherwise
   *
   * @exception NullPointerException if either {@code supplier} or
   * {@code absolutePath} is {@code null}
   *
   * @exception IllegalArgumentException if {@code absolutePath}
   * {@linkplain Path#isAbsolute() is not absolute}
   *
   * @see #get(Configured, Path)
   *
   * @threadsafety This method is, and overrides of this method must
   * be, safe for concurrent use by multiple threads.
   *
   * @idempotency This method is, and overrides of this method must
   * be, idempotent and deterministic.
   */
  public default boolean isSelectable(final Configured<?> supplier, final Path<?> absolutePath) {
    if (!absolutePath.isAbsolute()) {
      throw new IllegalArgumentException("absolutePath: " + absolutePath);
    }
    return AssignableType.of(this.upperBound()).isAssignable(absolutePath.type());
  }

  /**
   * Returns a {@link Value} suitable for the supplied {@link
   * Configured} and {@link Path}, or {@code null} if there is no such
   * {@link Value}.
   *
   * <p>Callers may assume that a call to {@link
   * #isSelectable(Configured, Path)} immediately preceded any
   * invocation of this method, and that it returned {@code true}.</p>
   *
   * <p>If an implementation of this method returns {@code null} once,
   * it does not follow that it must return {@code null} forever
   * after, even when supplied with the same arguments.</p>
   *
   * @param requestor the {@link Configured} seeking a {@link Value};
   * must not be {@code null}
   *
   * @param absolutePath an {@linkplain Path#isAbsolute() absolute
   * <code>Path</code>} for which the supplied {@link Configured} is
   * seeking a value; must not be {@code null}
   *
   * @return a {@link Value} more or less suitable for the combination
   * of the supplied {@link Configured} and {@link Path}, or {@code
   * null} if there is no such {@link Value}
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
  public <T> Value<T> get(final Configured<?> requestor, final Path<T> absolutePath);

}
