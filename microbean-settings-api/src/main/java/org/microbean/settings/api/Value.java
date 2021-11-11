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

import java.util.NoSuchElementException;
import java.util.Objects;

import java.util.function.Supplier;

import org.microbean.development.annotation.Convenience;

/**
 * An {@link OptionalSupplier} of a value that is additionally
 * qualified by a {@link Qualifiers} and a {@link Path} partially
 * identifying the kinds of {@link Qualifiers} and {@link Path}s for
 * which it might be suitable.
 *
 * <p>The {@link Value} interface is not used by the {@link
 * ConfiguredSupplier} interface directly but is often quite useful in
 * implementing it.</p>
 *
 * <p>A {@link Value} once received retains no reference to whatever
 * produced it and can be regarded as an authoritative source for
 * (possibly ever-changing) values going forward.  Notably, it can be
 * cached.</p>
 *
 * @param <T> the type of value this {@link Value} returns
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see Provider
 */
public final class Value<T> implements OptionalSupplier<T> {

  private final Qualifiers qualifiers;

  private final Path path;

  private final Supplier<? extends T> supplier;

  private final boolean deterministic;

  public Value(final Qualifiers qualifiers, final Path path, final T value) {
    this(null, qualifiers, path, () -> value, true);
  }

  public Value(final Qualifiers qualifiers, final Path path, final Supplier<? extends T> supplier) {
    this(null, qualifiers, path, supplier, false);
  }

  public Value(final Qualifiers qualifiers, final Path path, final Supplier<? extends T> supplier, final boolean deterministic) {
    this(null, qualifiers, path, supplier, deterministic);
  }

  public Value(final Supplier<? extends T> defaults,
               final Qualifiers qualifiers,
               final Path path,
               final Supplier<? extends T> supplier) {
    this(defaults, qualifiers, path, supplier, false);
  }

  public Value(final Supplier<? extends T> defaults, final Value<? extends T> source) {
    this(defaults, source.qualifiers(), source.path(), source, source.deterministic());
  }

  public Value(final Value<? extends T> source) {
    this(null, source.qualifiers(), source.path(), source, source.deterministic());
  }

  public Value(final Supplier<? extends T> defaults,
               final Qualifiers qualifiers,
               final Path path,
               final Supplier<? extends T> supplier,
               final boolean deterministic) {
    super();
    this.qualifiers = Objects.requireNonNull(qualifiers, "qualifiers");
    this.path = Objects.requireNonNull(path, "path");
    Objects.requireNonNull(supplier, "supplier");
    if (defaults == null) {
      this.supplier = supplier;
    } else {
      this.supplier = new Supplier<>() {
          private volatile Supplier<? extends T> s = supplier;
          @Override
          public final T get() {
            final Supplier<? extends T> s = this.s;
            try {
              return s.get();
            } catch (final NoSuchElementException | UnsupportedOperationException e) {
              if (s == defaults) {
                throw e;
              } else {
                this.s = defaults;
                return defaults.get();
              }
            }
          }
        };
    }
    this.deterministic = deterministic;
  }

  public final Qualifiers qualifiers() {
    return this.qualifiers;
  }

  public final Path path() {
    return this.path;
  }

  @Override // OptionalSupplier<T>
  public final T get() {
    return this.supplier.get();
  }

  public final boolean deterministic() {
    return this.deterministic;
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

  @Override
  public final int hashCode() {
    int hashCode = 17;
    Object v = this.qualifiers();
    int c = v == null ? 0 : v.hashCode();
    hashCode = 37 * hashCode + c;

    v = this.path();
    c = v == null ? 0 : v.hashCode();
    hashCode = 37 * hashCode + c;

    c = this.deterministic() ? 1 : 0;
    hashCode = 37 * hashCode + c;

    return hashCode;
  }

  @Override
  public final boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other != null && this.getClass() == other.getClass()) {
      final Value<?> her = (Value<?>)other;
      return
        Objects.equals(this.qualifiers(), her.qualifiers()) &&
        Objects.equals(this.path(), her.path()) &&
        this.deterministic() && her.deterministic();
    } else {
      return false;
    }
  }

}
