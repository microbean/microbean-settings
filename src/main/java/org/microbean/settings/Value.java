/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2019–2020 microBean™.
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
package org.microbean.settings;

import java.lang.annotation.Annotation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import java.util.function.Supplier;

/**
 * A {@link Supplier} of {@link String}s and hence an abstraction of
 * a {@link String}-typed configuration value, additionally
 * encompassing the configuration setting for which it is applicable
 * and the {@link Source} whence it originated.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @threadsafety Instances of this class are safe for concurrent use
 * by multiple threads.
 *
 * @see Settings
 *
 * @see Source
 *
 * @see Source#getValue(String, Set)
 */
public class Value implements Supplier<String> {


  /*
   * Instance fields.
   */


  private final Source source;

  private final String name;

  private final Set<Annotation> qualifiers;

  private final boolean authoritative;

  private final Supplier<? extends String> valueSupplier;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link Value}.
   *
   * @param other the {@link Value} to copy; must not be {@code null}
   *
   * @exception NullPointerException if {@code other} is {@code null}
   */
  public Value(final Value other) {
    this(other.getSource(),
         other.getName(),
         other.getQualifiers(),
         other.isAuthoritative(),
         other::get);
  }

  /**
   * Creates a new {@link Value}.
   *
   * @param other the {@link Value} to copy; must not be {@code null}
   *
   * @param value the {@link String} to be returned by the {@link
   * #get()} method; may be {@code null}
   *
   * @exception NullPointerException if {@code other} is {@code null}
   */
  public Value(final Value other,
               final String value) {
    this(other.getSource(),
         other.getName(),
         other.getQualifiers(),
         other.isAuthoritative(),
         () -> value);
  }

  /**
   * Creates a new {@link Value}.
   *
   * @param other the {@link Value} to copy; must not be {@code null}
   *
   * @param valueSupplier a {@link Supplier} of {@link String}s whose
   * {@link Supplier#get()} method will be called by this {@link
   * Value}'s {@link #get()} method; may be {@code null}; if
   * non-{@code null}, must be safe for concurrent access by multiple
   * threads
   *
   * @exception NullPointerException if {@code other} is {@code null}
   */
  public Value(final Value other,
               final Supplier<? extends String> valueSupplier) {
    this(other.getSource(),
         other.getName(),
         other.getQualifiers(),
         other.isAuthoritative(),
         valueSupplier);
  }

  /**
   * Creates a new {@link Value}.
   *
   * @param source the {@link Source} that created this {@link Value};
   * may be {@code null} to indicate that the {@link Value} was
   * synthesized
   *
   * @param name the name of the setting for which this is a {@link
   * Value}; must not be {@code null}
   *
   * @param qualifiers a {@link Set} of {@link Annotation}s qualifying
   * this {@link Value}; may be {@code null}; will be iterated over by
   * this constructor without any extra synchronization; will be
   * copied shallowly by this constructor if it is non-{@code null}
   *
   * @param value the {@link String} to be returned by the {@link
   * #get()} method; may be {@code null}
   *
   * @exception NullPointerException if {@code name} is {@code null}
   */
  public Value(final Source source,
               final String name,
               final Set<Annotation> qualifiers,
               final String value) {
    this(source, name, qualifiers, false, () -> value);
  }

  /**
   * Creates a new {@link Value}.
   *
   * @param source the {@link Source} that created this {@link Value};
   * may be {@code null} to indicate that the {@link Value} was
   * synthesized
   *
   * @param name the name of the setting for which this is a {@link
   * Value}; must not be {@code null}
   *
   * @param qualifiers a {@link Set} of {@link Annotation}s qualifying
   * this {@link Value}; may be {@code null}; will be iterated over by
   * this constructor without any extra synchronization; will be
   * copied shallowly by this constructor if it is non-{@code null}
   *
   * @param valueSupplier a {@link Supplier} of {@link String}s whose
   * {@link Supplier#get()} method will be called by this {@link
   * Value}'s {@link #get()} method; may be {@code null}; if
   * non-{@code null}, must be safe for concurrent access by multiple
   * threads
   *
   * @exception NullPointerException if {@code name} is {@code null}
   */
  public Value(final Source source,
               final String name,
               final Set<Annotation> qualifiers,
               final Supplier<? extends String> valueSupplier) {
    this(source, name, qualifiers, false, valueSupplier);
  }

  /**
   * Creates a new {@link Value}.
   *
   * @param source the {@link Source} that created this {@link Value};
   * may be {@code null} to indicate that the {@link Value} was
   * synthesized
   *
   * @param name the name of the setting for which this is a {@link
   * Value}; must not be {@code null}
   *
   * @param qualifiers a {@link Set} of {@link Annotation}s qualifying
   * this {@link Value}; may be {@code null}; will be iterated over by
   * this constructor without any extra synchronization; will be
   * copied shallowly by this constructor if it is non-{@code null}
   *
   * @param authoritative whether this {@link Value} is to be treated
   * as coming from an authoritative source
   *
   * @param value the {@link String} to be returned by the {@link
   * #get()} method; may be {@code null}
   *
   * @exception NullPointerException if {@code name} is {@code null}
   */
  public Value(final Source source,
               final String name,
               final Set<Annotation> qualifiers,
               final boolean authoritative,
               final String value) {
    this(source, name, qualifiers, authoritative, () -> value);
  }

  /**
   * Creates a new {@link Value}.
   *
   * @param source the {@link Source} that created this {@link Value};
   * may be {@code null} to indicate that the {@link Value} was
   * synthesized
   *
   * @param name the name of the setting for which this is a {@link
   * Value}; must not be {@code null}
   *
   * @param qualifiers a {@link Set} of {@link Annotation}s qualifying
   * this {@link Value}; may be {@code null}; will be iterated over by
   * this constructor without any extra synchronization; will be
   * copied shallowly by this constructor if it is non-{@code null}
   *
   * @param authoritative whether this {@link Value} is to be treated
   * as coming from an authoritative source
   *
   * @param valueSupplier a {@link Supplier} of {@link String}s whose
   * {@link Supplier#get()} method will be called by this {@link
   * Value}'s {@link #get()} method; may be {@code null}; if
   * non-{@code null}, must be safe for concurrent access by multiple
   * threads
   *
   * @exception NullPointerException if {@code name} is {@code null}
   */
  public Value(final Source source,
               final String name,
               final Set<Annotation> qualifiers,
               final boolean authoritative,
               final Supplier<? extends String> valueSupplier) {
    super();
    this.source = source;
    if (qualifiers == null || qualifiers.isEmpty()) {
      this.qualifiers = Collections.emptySet();
    } else if (qualifiers.size() == 1) {
      this.qualifiers = Collections.singleton(qualifiers.iterator().next());
    } else {
      this.qualifiers = Collections.unmodifiableSet(new HashSet<>(qualifiers));
    }
    this.name = Objects.requireNonNull(name);
    this.authoritative = authoritative;
    if (valueSupplier == null) {
      this.valueSupplier = Value::returnNull;
    } else {
      this.valueSupplier = valueSupplier;
    }
  }


  /*
   * Instance methods.
   */


  /**
   * Returns the {@link String} representation of this {@link Value}.
   *
   * @return the {@link String} representation of this {@link Value},
   * which may be {@code null}
   *
   * @nullability This method may return {@code null}.
   *
   * @idempotency No guarantees are made with respect to the
   * idempotency of this method.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  @Override
  public final String get() {
    return this.valueSupplier.get();
  }

  /**
   * Returns {@code true} if this {@link Value} is to be treated as
   * coming from an authoritative source.
   *
   * @return {@code true} if this {@link Value} is to be treated as
   * coming from an authoritative source; {@code false} otherwise
   *
   * @idempotency This method is idempotent.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public final boolean isAuthoritative() {
    return this.authoritative;
  }

  /**
   * Returns the {@link Source} that created this {@link Value}.
   *
   * @return the {@link Source} that created this {@link Value}, which
   * may be {@code null}
   *
   * @nullability This method may return {@code null}.
   *
   * @idempotency This method is idempotent.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public final Source getSource() {
    return this.source;
  }

  /**
   * Returns the name of the setting for which this is a {@link Value}.
   *
   * @return the name of the setting for which this is a {@link
   * Value}; never {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public final String getName() {
    return this.name;
  }

  /**
   * Returns a non-{@code null}, {@linkplain
   * Collections#unmodifiableSet(Set) unmodifiable <code>Set</code>}
   * of {@link Annotation}s qualifying this {@link Value}.
   *
   * @return a non-{@code null}, {@linkplain
   * Collections#unmodifiableSet(Set) unmodifiable <code>Set</code>}
   * of {@link Annotation}s qualifying this {@link Value}
   *
   * @nullability This method never returns {@code null}.
   *
   * @idempotency This method is idempotent.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public final Set<Annotation> getQualifiers() {
    return this.qualifiers;
  }

  /**
   * Returns a hashcode for this {@link Value} based off the return
   * value of the {@link #get()} method and nothing else.
   *
   * <p>Note that because the return value of invocations of a {@link
   * Value}'s {@link #get()} method can change over time, in general
   * {@link Value} hashcodes should be treated with great care.</p>
   *
   * @return a hashcode for this {@link Value}
   *
   * @idempotency No guarantees are made with respect to the
   * idempotency of this method.
   *
   * @threadsafety This method is and overrides must be safe for
   * concurrent use by multiple threads.
   *
   * @see #equals(Object)
   */
  @Override
  public int hashCode() {
    final Object value = this.get();
    return value == null ? 0 : value.hashCode();
  }

  /**
   * Returns {@code true} if the supplied {@link Object} is a {@link
   * Value} and has a {@link #get()} method implementation that
   * returns a value equal to the return value of an invocation of
   * this {@link Value}'s {@link #get()} method.
   *
   * <p>Note that because the return value of invocations of a {@link
   * Value}'s {@link #get()} method can change over time, in general
   * {@link Value} equality comparisons should be treated with great
   * care.</p>
   *
   * @param other the {@link Object} to compare; may be {@code null}
   *
   * @return {@code true} if the supplied {@link Object} is a {@link
   * Value} and has a {@link #get()} method implementation that
   * returns a value equal to the return value of an invocation of
   * this {@link Value}'s {@link #get()} method; {@code false}
   * otherwise
   *
   * @idempotency No guarantees are made with respect to the
   * idempotency of this method.
   *
   * @threadsafety This method is and overrides must be safe for
   * concurrent use by multiple threads.
   *
   * @see #hashCode()
   */
  @Override
  public boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other instanceof Value) {
      final Value her = (Value)other;

      final Object value = this.get();
      if (value == null) {
        if (her.get() != null) {
          return false;
        }
      } else if (!value.equals(her.get())) {
        return false;
      }

      return true;

    } else {
      return false;
    }
  }

  /**
   * Returns a {@link String} representation of this {@link Value} as
   * of the current moment by returning the result of invoking the
   * {@link #get()} method.
   *
   * <p>This method may return {@code null}.</p>
   *
   * @return a {@link String} representation of this {@link Value} as
   * of the current moment, or {@code null}
   *
   * @nullability This method and its overrides may return {@code
   * null}.
   *
   * @idempotency No guarantees are made with respect to the
   * idempotency of this method.
   *
   * @threadsafety This method is and its overrides must be safe for
   * concurrent use by multiple threads.
   *
   * @see #get()
   */
  @Override
  public String toString() {
    return this.get();
  }


  /*
   * Static methods.
   */
  

  /**
   * A static method that exists only to be referred to internally by
   * a method reference.
   *
   * @return {@code null} when invoked
   */
  private static final String returnNull() {
    return null;
  }

}
