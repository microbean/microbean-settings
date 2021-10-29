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

import java.lang.reflect.Type;

import java.util.Objects;

import java.util.function.Supplier;

import org.microbean.development.annotation.Convenience;

public interface Provider {


  /*
   * Instance methods.
   */


  /**
   * Returns a {@link Type} representing the upper bound of all
   * possible {@link Value}s {@linkplain #get(Qualifiers, Context)
   * supplied} by this {@link Provider}.
   *
   * <p>Implementations of this method must not return {@code
   * null}.</p>
   *
   * <p>The default implementation of this method returns {@link
   * Object Object.class}.</p>
   *
   * @return a {@link Type} representing the upper bound of all
   * possible {@link Value}s {@linkplain #get(Qualifiers, Context)
   * supplied} by this {@link Provider}; never {@code null}
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

  /**
   * Returns {@code true} if this {@link Provider} should be
   * considered for potentially satisfying the demand represented by
   * the supplied {@link Qualifiers} and {@link Context}.
   *
   * <p>This method will be called immediately before any invocation
   * of the {@link #get(Qualifiers, Context)} method, and if it
   * returns {@code false} no {@link #get(Qualifiers, Context)}
   * invocation will occur.</p>
   *
   * @param contextQualifiers the {@link Qualifiers} qualifying the
   * supplied {@link Context}; must not be {@code null}
   *
   * @param context the {@link Context} representing demand; must not
   * be {@code null}
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
  public boolean isSelectable(final Qualifiers contextQualifiers, final Context<?> context);

  /**
   * Returns a {@link Value} suitable for the supplied {@link
   * Qualifiers} and {@link Context}, or {@code null} if the
   * represented request is inappropriate or cannot be satisfied.
   *
   * <p>Implementations of this method may, and often do, return
   * {@code null}.</p>
   *
   * <p>If an implementation of this method returns a non-{@code null}
   * {@link Value}, there is a tacit contract that the {@link
   * Provider} has fulfilled its responsibilities and may be
   * jettisoned.  That is, the returned {@link Value} may be cached by
   * the caller (for example) and can be regarded as the source for
   * (little-v) values going forward.  (The {@link Value} so returned
   * may, of course, return {@code null} from its {@link Value#get()}
   * method at any point for any reason.)</p>
   *
   * @param contextQualifiers the {@link Qualifiers} qualifying the
   * supplied {@link Context}; must not be {@code null}
   *
   * @param context the {@link Context} representing demand; must not
   * be {@code null}
   *
   * @return a {@link Value} suitable for the supplied {@link
   * Qualifiers} and {@link Context}, or {@code null}
   *
   * @nullability Implementations of this method may return {@code
   * null}.
   *
   * @idempotency Implementations of this method need not be
   * idempotent or deterministic.
   *
   * @threadsafety Implementations of this method must be safe for
   * concurrent use by multiple threads.
   */
  public Value<?> get(final Qualifiers contextQualifiers, final Context<?> context);


  /*
   * Inner and nested classes.
   */


  /**
   * A {@link Supplier} of a value returned by a {@link Provider}.
   *
   * <p>A {@link Value}, once returned by a {@link Provider}, is no
   * longer linked to that {@link Provider} and can be regarded as an
   * authoritative source for values going forward.  Notably, it can
   * be cached.  Its {@link Provider} can be discarded.</p>
   *
   * @author <a href="https://about.me/lairdnelson"
   * target="_parent">Laird Nelson</a>
   *
   * @param qualifiers the {@link Qualifiers} this {@link Value} is
   * suitable for; must not be {@code null}
   *
   * @param path the possibly vague {@link Path} (or paths that match
   * it) this {@link Value} is suitable for; must not be {@code null}
   *
   * @param supplier the {@link Supplier} underlying this {@link
   * Value}; must not be {@code null}
   */
  public record Value<T>(Qualifiers qualifiers, Path path, Supplier<T> supplier) implements Supplier<T> {


    /*
     * Constructors.
     */


    public Value(final Type type, final T value) {
      this(Qualifiers.of(), Path.of(type), () -> value);
    }

    public Value(final Type type, final Supplier<T> supplier) {
      this(Qualifiers.of(), Path.of(type), supplier);
    }

    public Value(final Qualifiers qualifiers, final Type type, final T value) {
      this(qualifiers, Path.of(type), () -> value);
    }

    public Value(final Qualifiers qualifiers, final Type type, final Supplier<T> supplier) {
      this(qualifiers, Path.of(type), supplier);
    }

    public Value(final Class<T> type, final T value) {
      this(Qualifiers.of(), Path.of(type), () -> value);
    }

    public Value(final Class<T> type, final Supplier<T> supplier) {
      this(Qualifiers.of(), Path.of(type), supplier);
    }

    public Value(final Qualifiers qualifiers, final Class<T> type, final T value) {
      this(qualifiers, Path.of(type), () -> value);
    }

    public Value(final Qualifiers qualifiers, final Class<T> type, final Supplier<T> supplier) {
      this(qualifiers, Path.of(type), supplier);
    }

    public Value(final Path path, final T value) {
      this(Qualifiers.of(), path, () -> value);
    }

    public Value(final Path path, final Supplier<T> supplier) {
      this(Qualifiers.of(), path, supplier);
    }

    public Value(final Qualifiers qualifiers, final Path path, final T value) {
      this(qualifiers, path, () -> value);
    }

    public Value {
      Objects.requireNonNull(qualifiers, "qualifiers");
      Objects.requireNonNull(path, "path");
      Objects.requireNonNull(supplier, "supplier");
    }


    /*
     * Instance methods.
     */


    @Override // Supplier<T>
    public final T get() {
      return this.supplier().get();
    }

    /**
     * Returns the result of invoking {@link Path#type()} on the
     * return value of this {@link Value}'s {@link #path()} method.
     *
     * <p>This method never returns {@code null}.</p>
     *
     * @return the result of invoking {@link Path#type()} on the
     * return value of this {@link Value}'s {@link #path()} method;
     * never {@code null}
     *
     * @nullability This method never returns {@code null}.
     *
     * @idempotency This method is idempotent and and deterministic.
     *
     * @threadsafety This method is safe for concurrent use by
     * multiple threads.
     *
     * @see #path()
     *
     * @see Path#type()
     */
    @Convenience
    public final Type type() {
      return this.path().type();
    }

  }

}
