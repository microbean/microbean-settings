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

import java.util.Optional;

import java.util.concurrent.atomic.AtomicReference;

import java.util.function.Supplier;

/**
 * A {@link Supplier} that computes the value it will return from its
 * {@link #get()} when that method is first invoked, and that returns
 * that computed value for all subsequent invocations of that method.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @param <T> The type of object returned by the {@link #get()}
 * method.
 *
 * @see #get()
 */
public final class CachingSupplier<T> implements Supplier<T> {


  /*
   * Instance fields.
   */


  private final Supplier<? extends T> delegate;

  private final AtomicReference<Optional<T>> ref;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link CachingSupplier}.
   *
   * @see #CachingSupplier(Supplier)
   *
   * @see #get()
   *
   * @see #set(Object)
   *
   * @see
   * AtomicReference#updateAndGet(java.util.function.UnaryOperator)
   */
  public CachingSupplier() {
    this(CachingSupplier::returnNull);
  }

  /**
   * Creates a new {@link CachingSupplier}.
   *
   * @param value the value that will be returned by all invocations
   * of the {@link #get()} method; may be {@code null} in which case
   * the {@link #get()} method will return {@code null} until such
   * time as the {@link #set(Object)} method is called, whereupon it
   * will return that value instead
   *
   * @see #get()
   *
   * @see #set(Object)
   */
  public CachingSupplier(final T value) {
    super();
    this.ref = new AtomicReference<>(Optional.ofNullable(value));
    this.delegate = CachingSupplier::returnNull;
  }

  /**
   * Creates a new {@link CachingSupplier}.
   *
   * @param supplier the {@link Supplier} that will be used to supply
   * the value that will be returned by all invocations of the {@link
   * #get()} method; may be {@code null} in which case the {@link
   * #get()} method will forever return {@code null} (unless the
   * {@link #set(Object)} method is called); <strong>must be safe for
   * concurrent use by multiple threads and must be side-effect
   * free</strong>
   *
   * @see #get()
   *
   * @see
   * AtomicReference#updateAndGet(java.util.function.UnaryOperator)
   */
  public CachingSupplier(final Supplier<? extends T> supplier) {
    super();
    this.ref = new AtomicReference<>();
    this.delegate = supplier == null ? CachingSupplier::returnNull : supplier;
  }


  /*
   * Instance methods.
   */


  /**
   * Returns the value this {@link CachingSupplier} will forever
   * supply, computing it with the first invocation by using the
   * {@link Supplier} supplied at {@linkplain
   * #CachingSupplier(Supplier) construction time}.
   *
   * <p>If the {@link Supplier} supplied at {@linkplain
   * #CachingSupplier(Supplier) construction time} returns {@code
   * null} from its {@link Supplier#get()} method, then its {@link
   * Supplier#get()} method may be invoked multiple times.  Otherwise,
   * the return value of its {@link Supplier#get()} method will be
   * cached forever and returned with every subsequent invocation of
   * this method.</p>
   *
   * @return the value, which may very well be {@code null}
   *
   * @nullability This method may return {@code null}.
   *
   * @idempotency This method's idempotency and determinism are
   * determined by the idempotency and determinisim of the {@link
   * Supplier} supplied at {@linkplain #CachingSupplier(Supplier)
   * construction time}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #CachingSupplier(Supplier)
   */
  @Override // Supplier
  public final T get() {
    Optional<T> optional = this.ref.get();
    if (optional == null) {
      optional = Optional.ofNullable(this.delegate.get());
      if (!this.ref.compareAndSet(null, optional)) {
        optional = this.ref.get();
      }
    }
    return optional.orElse(null);
  }

  /**
   * Sets the value that will be returned forever afterwards by the
   * {@link #get()} method and returns {@code true} if and only if the
   * value was previously unset.
   *
   * <p>The use of this method effectively renders the {@link
   * Supplier} {@linkplain #CachingSupplier(Supplier) supplied at
   * construction time} superfluous.</p>
   *
   * @param newValue the new value that will be returned by the {@link
   * #get()} method forever afterwards; may be {@code null}
   *
   * @return {@code true} if and only if this assignment was permitted;
   * {@code false} otherwise
   *
   * @see #get()
   *
   * @see #CachingSupplier(Supplier)
   *
   * @see AtomicReference#compareAndSet(Object, Object)
   */
  public final boolean set(final T newValue) {
    return this.ref.compareAndSet(null, Optional.ofNullable(newValue));
  }


  /*
   * Static methods.
   */


  /**
   * Returns {@code null} when invoked.
   *
   * <p>This method is referred to via a method reference in the
   * {@link #CachingSupplier(Supplier)} constructor and is used for no
   * other purpose.</p>
   *
   * @param <T> the type of object to return; irrelevant because
   * {@code null} is always returned
   *
   * @return {@code null} always
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @nullability This method always returns {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #CachingSupplier(Supplier)
   */
  private static final <T> T returnNull() {
    return null;
  }

}
