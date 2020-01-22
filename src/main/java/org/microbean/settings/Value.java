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

public final class Value implements Supplier<String> {

  private final Source source;

  private final String name;

  private final Set<Annotation> qualifiers;

  private final boolean authoritative;

  private final String value;

  public Value(final Value other) {
    this(other.getSource(),
         other.getName(),
         other.getQualifiers(),
         other.isAuthoritative(),
         other.get());
  }
  
  public Value(final Value other,
               final String value) {
    this(other.getSource(),
         other.getName(),
         other.getQualifiers(),
         other.isAuthoritative(),
         value);
  }
  
  public Value(final Source source,
               final String name,
               final Set<Annotation> matchedQualifiers,
               final String value) {
    this(source, name, matchedQualifiers, false, value);
  }
  
  public Value(final Source source,
               final String name,
               final Set<Annotation> matchedQualifiers,
               final boolean authoritative,
               final String value) {
    super();
    this.source = source;
    if (matchedQualifiers == null || matchedQualifiers.isEmpty()) {
      this.qualifiers = Collections.emptySet();
    } else if (matchedQualifiers.size() == 1) {
      this.qualifiers = Collections.singleton(matchedQualifiers.iterator().next());
    } else {
      this.qualifiers = Collections.unmodifiableSet(new HashSet<>(matchedQualifiers));
    }
    this.name = Objects.requireNonNull(name);
    this.authoritative = authoritative;
    this.value = value;
  }

  @Override
  public final String get() {
    return this.value;
  }
  
  public final boolean isAuthoritative() {
    return this.authoritative;
  }
  
  public final Source getSource() {
    return this.source;
  }

  public final String getName() {
    return this.name;
  }

  public final Set<Annotation> getQualifiers() {
    return this.qualifiers;
  }

  /**
   * Returns a hashcode for this {@link Value} based off the return
   * value of the {@link #get()} method and nothing else.
   *
   * @return a hashcode for this {@link Value}
   *
   * @see #equals(Object)
   */
  @Override
  public final int hashCode() {
    final Object value = this.get();
    return value == null ? 0 : value.hashCode();
  }

  /**
   * Returns {@code true} if the supplied {@link Object} is a {@link
   * Value} and has a {@link #get()} method implementation that
   * returns a value equal to the return value of an invocation of
   * this {@link Value}'s {@link #get()} method.
   *
   * @param other the {@link Object} to compare; may be {@code null}
   *
   * @return {@code true} if the supplied {@link Object} is a {@link
   * Value} and has a {@link #get()} method implementation that
   * returns a value equal to the return value of an invocation of
   * this {@link Value}'s {@link #get()} method; {@code false}
   * otherwise
   *
   * @see #hashCode()
   */
  @Override
  public final boolean equals(final Object other) {
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
   * of the current moment.
   *
   * <p>This method may return {@code null}.</p>
   *
   * @return a {@link String} representation of this {@link Value} as
   * of the current moment, or {@code null}
   *
   * @see #get()
   */
  @Override
  public final String toString() {
    return this.get();
  }
  
}
