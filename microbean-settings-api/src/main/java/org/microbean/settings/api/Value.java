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

  private final Value<? extends T> defaults;
  
  private final Qualifiers qualifiers;

  private final Path path;

  private final OptionalSupplier<T> optionalSupplier;

  public Value(final Qualifiers qualifiers, final Path path, final T value) {
    this(null, qualifiers, path, () -> value);
  }

  public Value(final Qualifiers qualifiers, final Path path, final OptionalSupplier<? extends T> optionalSupplier) {
    this(null, qualifiers, path, optionalSupplier);
  }
  
  public Value(final Value<? extends T> defaults, final Qualifiers qualifiers, final Path path, final OptionalSupplier<? extends T> optionalSupplier) {
    super();
    this.defaults = defaults;
    this.qualifiers = Objects.requireNonNull(qualifiers, "qualifiers");
    this.path = Objects.requireNonNull(path, "path");
    @SuppressWarnings("unchecked")
    final OptionalSupplier<T> os = (OptionalSupplier<T>)Objects.requireNonNull(optionalSupplier, "optionalSupplier");
    this.optionalSupplier = os;
  }

  public final Qualifiers qualifiers() {
    return this.qualifiers;
  }

  public final Path path() {
    return this.path;
  }

  public final OptionalSupplier<T> optionalSupplier() {
    return this.optionalSupplier;
  }

  @Override // OptionalSupplier<T>
  public final T get() {
    final T value = this.optionalSupplier().orElse(null);
    return value == null ? this.defaults == null ? null : this.defaults.get() : value;
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
