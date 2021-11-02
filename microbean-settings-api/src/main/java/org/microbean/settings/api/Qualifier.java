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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

public final class Qualifier {


  /*
   * Static fields.
   */


  private static final String VALUE = "value";


  /*
   * Instance fields.
   */


  private final String name;

  private final String value;


  /*
   * Constructors.
   */


  public Qualifier(final CharSequence value) {
    this(VALUE, value);
  }

  public Qualifier(final CharSequence name, final CharSequence value) {
    super();
    this.name = Objects.requireNonNull(name, "name").toString();
    this.value = Objects.requireNonNull(value, "value").toString();
  }


  /*
   * Instance methods.
   */


  public final String name() {
    return this.name;
  }

  public final String value() {
    return this.value;
  }

  @Override
  public final int hashCode() {
    int hashCode = 17;
    Object v = this.name();
    int c = v == null ? 0 : v.hashCode();
    hashCode = 37 * hashCode + c;
    v = this.value();
    c = v == null ? 0 : v.hashCode();
    return 37 * hashCode + c;
  }

  @Override
  public final boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other == null || this.getClass() != other.getClass()) {
      return false;
    } else {
      final Qualifier her = (Qualifier)other;
      return
        Objects.equals(this.name(), her.name()) &&
        Objects.equals(this.value(), her.value());
    }
  }

  public final Entry<String, String> toEntry() {
    return Map.entry(this.name(), this.value());
  }

  @Override // Object
  public final String toString() {
    return toString(this.name(), this.value());
  }


  /*
   * Static methods.
   */


  public static final String toString(final CharSequence name, final CharSequence value) {
    return String.valueOf(name) + "=" + String.valueOf(value);
  }

  public static final Qualifier of(final Entry<? extends CharSequence, ? extends CharSequence> e) {
    return of(e.getKey(), e.getValue());
  }

  public static final Qualifier of(final CharSequence value) {
    return of(VALUE, value);
  }

  public static final Qualifier of(final CharSequence name, final CharSequence value) {
    return new Qualifier(name, value);
  }

}
