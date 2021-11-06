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

public interface OptionalSupplier<T> extends Supplier<T> {

  public default Optional<T> filter(final Predicate<? super T> predicate) {
    final T value = this.get();
    return value != null && predicate.test(value) ? Optional.of(value) : Optional.empty();
  }

  @SuppressWarnings("unchecked")
  public default <U> Optional<U> flatMap(final Function<? super T, ? extends Optional<? extends U>> mapper) {
    final T value = this.get();
    return value == null ? Optional.empty() : Objects.requireNonNull((Optional<U>)mapper.apply(value));
  }

  public default void ifPresent(final Consumer<? super T> action) {
    final T value = this.get();
    if (value == null) {
      action.accept(value);
    }
  }

  public default void ifPresentOrElse(final Consumer<? super T> action, final Runnable emptyAction) {
    final T value = this.get();
    if (value == null) {
      emptyAction.run();
    } else {
      action.accept(value);
    }
  }

  @Deprecated
  public default boolean isEmpty() {
    return this.get() == null;
  }

  @Deprecated
  public default boolean isPresent() {
    return this.get() != null;
  }

  public default <U> Optional<U> map(final Function <? super T, ? extends U> mapper) {
    final T value = this.get();
    return value == null ? Optional.empty() : Optional.ofNullable(mapper.apply(value));
  }

  @SuppressWarnings("unchecked")
  public default Optional<T> or(final Supplier<? extends Optional<? extends T>> supplier) {
    final T value = this.get();
    return value == null ? Objects.requireNonNull((Optional<T>)supplier.get()) : Optional.of(value);
  }

  public default T orElse(final T other) {
    final T value = this.get();
    return value == null ? other : value;
  }

  public default T orElseGet(final Supplier<? extends T> supplier) {
    final T value = this.get();
    return value == null ? supplier.get() : value;
  }

  public default T orElseThrow() {
    final T value = this.get();
    if (value == null) {
      throw new NoSuchElementException();
    } else {
      return value;
    }
  }

  public default <X extends Throwable> T orElseThrow(final Supplier<? extends X> exceptionSupplier) throws X {
    final T value = this.get();
    if (value == null) {
      throw exceptionSupplier.get();
    } else {
      return value;
    }
  }

  public default Stream<T> stream() {
    final T value = this.get();
    return value == null ? Stream.empty() : Stream.of(value);
  }



}
