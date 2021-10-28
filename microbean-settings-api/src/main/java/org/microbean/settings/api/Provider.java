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

public interface Provider {

  public default Type upperBound() {
    return Object.class;
  }

  public boolean isSelectable(final Qualifiers contextQualifiers, final Context<?> context);

  public Value<?> get(final Qualifiers contextQualifiers, final Context<?> context);


  /*
   * Inner and nested classes.
   */


  public record Value<T>(Qualifiers qualifiers, Path path, T value) implements Supplier<T> {

    public Value(final Type type, final T value) {
      this(Qualifiers.of(), Path.of(type), value);
    }
    
    public Value(final Path path, final T value) {
      this(Qualifiers.of(), path, value);
    }

    public Value {
      Objects.requireNonNull(path, "path");
      if (qualifiers == null) {
        qualifiers = Qualifiers.of();
      }
      if (value != null && !AssignableType.of(path.type()).isAssignable(value.getClass())) {
        throw new IllegalArgumentException("value: " + value);
      }
    }

    @Override // Supplier<T>
    public final T get() {
      return this.value();
    }

    public final Type type() {
      return this.path().type();
    }

  }

}
