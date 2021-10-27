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

import java.util.Objects;

import java.util.function.Function;
import java.util.function.BiPredicate;

public interface Qualified<T> extends Assignable<T> {


  /*
   * Instance methods.
   */


  @Override // Assignable<T>
  public default T assignable() {
    return this.qualified();
  }

  @Override // Assignable<T>
  public default boolean isAssignable(final T payload) {
    return this.assignable().equals(payload);
  }

  public T qualified();

  public Qualifiers qualifiers();


  /*
   * Static methods.
   */


  public static <T> String toString(final Qualified<T> q) {
    return toString(q, t -> t.toString());
  }

  public static <T> String toString(final Qualified<? extends T> q, final Function<? super T, ? extends String> f) {
    if (q == null) {
      return "";
    } else {
      final StringBuilder sb = new StringBuilder(String.valueOf(f.apply(q.qualified())));
      final Qualifiers qualifiers = q.qualifiers();
      if (!qualifiers.isEmpty()) {
        sb.append(":").append(qualifiers.toString());
      }
      return sb.toString();
    }
  }


  /*
   * Inner and nested classes.
   */


  public record Record<T>(Qualifiers qualifiers, T qualified, BiPredicate<T, T> assignabilityPredicate) implements Qualified<T> {


    /*
     * Constructors.
     */


    public Record(final Qualifiers qualifiers, final T qualified) {
      this(qualifiers, qualified, Objects::equals);
    }

    public Record {
      if (assignabilityPredicate == null) {
        assignabilityPredicate = Objects::equals;
      }
    }


    /*
     * Instance methods.
     */


    @Override
    public final boolean isAssignable(final T payload) {
      return this.assignabilityPredicate().test(this.assignable(), payload);
    }

    @Override
    public final int hashCode() {
      int hashCode = 17;
      Object v = this.qualifiers();
      int c = v == null ? 0 : v.hashCode();
      hashCode = 37 * hashCode + c;
      v = this.qualified();
      c = v == null ? 0 : v.hashCode();
      hashCode = 37 * hashCode + c;
      return hashCode;
    }

    @Override
    public final boolean equals(final Object other) {
      if (other == this) {
        return true;
      } else if (other == null || this.getClass() != other.getClass()) {
        return false;
      } else {
        final Record<?> her = (Record<?>)other;
        return
          Objects.equals(this.qualifiers(), her.qualifiers()) &&
          Objects.equals(this.qualified(), her.qualified());
      }
    }

    @Override
    public final String toString() {
      return Qualified.toString(this);
    }

    public final String toString(final Function<? super T, ? extends String> f) {
      return Qualified.toString(this, f);
    }


    /*
     * Static methods.
     */


    public static final <T> Record<T> of(final Qualified<T> q) {
      return q instanceof Record<T> qr ? qr : new Record<>(q.qualifiers(), q.qualified());
    }

    public static final <T> Record<T> of(final T qualified) {
      return of(Qualifiers.of(), qualified);
    }

    public static final <T> Record<T> of(final Qualifiers qualifiers, final T qualified) {
      return new Record<>(qualifiers, qualified);
    }

  }

}
