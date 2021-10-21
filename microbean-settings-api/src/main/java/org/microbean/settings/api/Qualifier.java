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

import java.util.Map.Entry;
import java.util.Objects;

public record Qualifier<T>(String name, T value) implements Assignable<Qualifier<T>>, Comparable<Qualifier<?>> {

  public static final String VALUE = "value";

  public Qualifier(final CharSequence name, final T value) {
    this(name.toString(), value);
  }
  
  public Qualifier {
    name = Objects.requireNonNull(name, "name");
    value = Objects.requireNonNull(value, "value"); // value better be immutable
  }

  @Override // Assignable<Qualifier>
  public final Qualifier<T> assignable() {
    return this;
  }

  @Override
  public final int compareTo(final Qualifier<?> other) {
    if (other == null) {
      return -1; // nulls go to the right
    } else if (this.equals(other)) {
      return 0;
    } else {
      return this.toString().compareTo(other.toString());
    }
  }

  @Override // Object
  public final String toString() {
    return toString(this.name(), this.value());
  }

  public static final String toString(final CharSequence name, final Object value) {
    return String.valueOf(name) + "=" + String.valueOf(value);
  }
  
  public static final <T> Qualifier<T> of(final Entry<? extends CharSequence, ? extends T> e) {
    return of(e.getKey(), e.getValue());
  }

  public static final <T> Qualifier<T> of(final T value) {
    return of(VALUE, value);
  }

  public static final <T> Qualifier<T> of(final CharSequence name, final T value) {
    return new Qualifier<>(name, value);
  }
  
}
