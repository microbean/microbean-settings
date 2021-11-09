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

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import java.util.stream.Stream;

import org.microbean.development.annotation.Convenience;
import org.microbean.development.annotation.OverridingDiscouraged;

/**
 * A {@link Supplier} that is {@link Optional}-like, and a convenient
 * {@linkplain #optional() bridge to <code>Optional</code> objects}.
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

  @OverridingDiscouraged
  public default T exceptionally(final Function<? super RuntimeException, ? extends T> handler) {
    try {
      return this.get();
    } catch (final RuntimeException e) {
      return handler.apply(e);
    }
  }

  @Convenience
  @OverridingDiscouraged
  public default Optional<T> filter(final Predicate<? super T> predicate) {
    return this.optional().filter(predicate);
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> Optional<U> flatMap(final Function<? super T, ? extends Optional<? extends U>> mapper) {
    return this.optional().flatMap(mapper);
  }

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
   * <ul><li>Returning {@code null}.  The emptiness is
   * <em>transitory</em>, i.e. a subsequent invocation of this method
   * may return a non-{@code null} result.</li>
   *
   * <li>Throwing a {@link NoSuchElementException}.  The emptiness is
   * <em>permanent</em>, i.e. all subsequent invocations of this
   * method will (must) also throw an {@link
   * NoSuchElementException}.</li>
   *
   * <li>Throwing an {@link UnsupportedOperationException}.  The
   * emptiness is <em>permanent</em>, i.e. all subsequent invocations
   * of this method will (must) also throw an {@link
   * UnsupportedOperationException}.</li>
   *
   * </ul></li>
   *
   * <li>The returning of a non-{@code null} value indicates (only)
   * <em>transitory presence</em>, i.e. a subsequent invocation of
   * this method may return {@code null} or throw either a {@link
   * NoSuchElementException} or an {@link
   * UnsupportedOperationException}.</li>
   *
   * </ul>
   *
   * @return a value, or {@code null} to indicate transitory emptiness
   *
   * @exception NoSuchElementException to indicate permanent emptiness
   *
   * @exception UnsupportedOperationException to indicate permanent
   * emptiness
   *
   * @nullability Implementations of this method may and often will
   * return {@code null}, indicating transitory emptiness.
   *
   * @threadsafety Implementations of this method must be safe for
   * concurrent use by multiple threads.
   *
   * @idempotency Implementations of this method must be idempotent
   * but need not be deterministic.  However, once an implementation
   * of this method throws either a {@link NoSuchElementException} or
   * an {@link UnsupportedOperationException}, it must also do so for
   * every subsequent invocation.
   */
  @Override
  public T get();

  @OverridingDiscouraged
  public default <U> U handle(final BiFunction<? super T, ? super RuntimeException, ? extends U> handler) {
    try {
      return handler.apply(this.get(), null);
    } catch (final RuntimeException e) {
      return handler.apply(null, e);
    }
  }

  @Convenience
  @OverridingDiscouraged
  public default void ifPresent(final Consumer<? super T> action) {
    this.optional().ifPresent(action);
  }

  @Convenience
  @OverridingDiscouraged
  public default void ifPresentOrElse(final Consumer<? super T> action, final Runnable emptyAction) {
    this.optional().ifPresentOrElse(action, emptyAction);
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> Optional<U> map(final Function <? super T, ? extends U> mapper) {
    return this.optional().map(mapper);
  }

  /**
   * Returns a non-{@code null} but possibly {@linkplain
   * Optional#isEmpty() empty} {@link Optional} representing this
   * {@link OptionalSupplier}'s {@linkplain #get() value}.
   *
   * <p>The default implementation of this method does not and its
   * overrides must not return {@code null}.</p>
   *
   * <p>The default implementation of this method catches all {@link
   * NoSuchElementException}s and {@link
   * UnsupportedOperationException}s and returns an empty {@link
   * Optional} in these cases.  To detect permanent versus transitory
   * emptiness, potential callers should use the {@link #get()} method
   * directly or either the {@link #optional(Function)} or {@link
   * #optional(BiFunction)} methods.</p>
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
   *
   * @see #optional(Function)
   *
   * @see #optional(BiFunction)
   *
   * @see #get()
   */
  @Convenience
  @OverridingDiscouraged
  public default Optional<T> optional() {
    return this.optional(OptionalSupplier::returnNull);
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> Optional<U> optional(final BiFunction<? super T, ? super RuntimeException, ? extends U> handler) {
    return Optional.ofNullable(this.handle(handler));
  }

  @Convenience
  @OverridingDiscouraged
  public default Optional<T> optional(final Function<? super RuntimeException, ? extends T> handler) {
    return Optional.ofNullable(this.exceptionally(handler));
  }

  @Convenience
  @OverridingDiscouraged
  public default Optional<T> or(final Supplier<? extends Optional<? extends T>> supplier) {
    return this.optional().or(supplier);
  }

  @Convenience
  @OverridingDiscouraged
  public default T orElse(final T other) {
    return this.optional().orElse(other);
  }

  @Convenience
  @OverridingDiscouraged
  public default T orElseGet(final Supplier<? extends T> supplier) {
    return this.optional().orElseGet(supplier);
  }

  @Convenience
  @OverridingDiscouraged
  public default T orElseThrow() {
    return this.orElseThrow(NoSuchElementException::new);
  }

  @Convenience
  @OverridingDiscouraged
  public default <X extends Throwable> T orElseThrow(final Supplier<? extends X> exceptionSupplier) throws X {
    return this.optional().orElseThrow(exceptionSupplier);
  }

  @Convenience
  @OverridingDiscouraged
  public default Stream<T> stream() {
    return this.optional().stream();
  }

  private static <T> T returnNull(final RuntimeException e) {
    if (e instanceof NoSuchElementException || e instanceof UnsupportedOperationException) {
      return null;
    } else {
      throw e;
    }
  }

}
