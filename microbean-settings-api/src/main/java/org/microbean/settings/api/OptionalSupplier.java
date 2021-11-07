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

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import java.util.stream.Stream;

import org.microbean.development.annotation.OverridingDiscouraged;

/**
 * A {@link Supplier} that is {@link Optional}-like.
 *
 * <p>Unlike {@link Optional}, any implementation of this interface is
 * not a <a
 * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/doc-files/ValueBased.html">value-based
 * class</a>.  This is also why there are no {@code isPresent()} or
 * {@code isEmpty()} methods.</p>
 *
 * @param <T> the type of value implementations of this interface
 * {@linkplain #get() supply}
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see #get()
 *
 * @see #optional()
 */
@FunctionalInterface
public interface OptionalSupplier<T> extends Supplier<T> {

  /**
   * Returns a value, which may be {@code null}, indicating (possibly
   * transitory) <em>emptiness</em>, or non-{@code null}, indicating
   * (possibly transitory) <em>presence</em>.
   *
   * <p>This method's contract extends {@link Supplier#get()}'s
   * contract with the following additional requirements:</p>
   *
   * <ul>
   *
   * <li>An implementation of this method need not be deterministic.</li>
   *
   * <li>An implementation of this method may indicate (possibly
   * transitory) emptiness by any of the following means:
   *
   * <ul>
   *
   * <li>Returning {@code null}.</li>
   *
   * <li>Throwing a {@link NoSuchElementException}.</li>
   *
   * <li>Throwing an {@link UnsupportedOperationException}.</li>
   *
   * </ul></li>
   *
   * <li>The returning of a non-{@code null} value indicates (possibly
   * transitory) presence.</li>
   *
   * </ul>
   *
   * @return a value, or {@code null} to indicate emptiness
   *
   * @exception NoSuchElementException to indicate emptiness
   *
   * @exception UnsupportedOperationException to indicate emptiness
   *
   * @nullability Implementations of this method may and often will
   * return {@code null}.
   *
   * @threadsafety Implementations of this method must be safe for
   * concurrent use by multiple threads.
   *
   * @idempotency Implementations of this method must be idempotent
   * but need not be deterministic.
   */
  @Override
  public T get();

  /**
   * Returns a non-{@code null} but possibly {@linkplain
   * Optional#isEmpty() empty} {@link Optional} representing this
   * {@link OptionalSupplier}'s {@linkplain #get() value}.
   *
   * <p>The default implementation of this method does not and its
   * overrides must not return {@code null}.</p>
   *
   * @return a non-{@code null} but possibly {@linkplain
   * Optional#isEmpty() empty} {@link Optional} representing this
   * {@link OptionalSupplier}'s {@linkplain #get() value}
   *
   * @nullability The default implementation of this method does not
   * and overrides must not return {@code null}.
   *
   * @threadsafety The default implementation of this method is and
   * overrides must be safe for concurrent use by multiple threads.
   *
   * @idempotency The default implementation and overrides of this
   * method may not be idempotent or deterministic.
   */
  @OverridingDiscouraged
  public default Optional<T> optional() {
    try {
      return Optional.ofNullable(this.get());
    } catch (final NoSuchElementException | UnsupportedOperationException e) {
      // TODO: log
      return Optional.empty();
    }
  }

  @OverridingDiscouraged
  public default Optional<T> filter(final Predicate<? super T> predicate) {
    return this.optional().filter(predicate);
  }

  @OverridingDiscouraged
  public default <U> Optional<U> flatMap(final Function<? super T, ? extends Optional<? extends U>> mapper) {
    return this.optional().flatMap(mapper);
  }

  @OverridingDiscouraged
  public default void ifPresent(final Consumer<? super T> action) {
    this.optional().ifPresent(action);
  }

  @OverridingDiscouraged
  public default void ifPresentOrElse(final Consumer<? super T> action, final Runnable emptyAction) {
    this.optional().ifPresentOrElse(action, emptyAction);
  }

  @OverridingDiscouraged
  public default <U> Optional<U> map(final Function <? super T, ? extends U> mapper) {
    return this.optional().map(mapper);
  }

  @OverridingDiscouraged
  public default Optional<T> or(final Supplier<? extends Optional<? extends T>> supplier) {
    return this.optional().or(supplier);
  }

  @OverridingDiscouraged
  public default T orElse(final T other) {
    return this.optional().orElse(other);
  }

  @OverridingDiscouraged
  public default T orElseGet(final Supplier<? extends T> supplier) {
    return this.optional().orElseGet(supplier);
  }

  @OverridingDiscouraged
  public default T orElseThrow() {
    return this.orElseThrow(NoSuchElementException::new);
  }

  @OverridingDiscouraged
  public default <X extends Throwable> T orElseThrow(final Supplier<? extends X> exceptionSupplier) throws X {
    return this.optional().orElseThrow(exceptionSupplier);
  }

  @OverridingDiscouraged
  public default Stream<T> stream() {
    return this.optional().stream();
  }

}
