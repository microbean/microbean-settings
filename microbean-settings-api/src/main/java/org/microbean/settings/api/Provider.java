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

import java.util.function.Supplier;

public interface Provider {

  public default Type upperBound() {
    return Object.class;
  }

  public boolean isSelectable(final Qualified<? extends Context> context);

  public Value<?> get(final Qualified<? extends Context> context);


  /*
   * Inner and nested classes.
   */


  public record Value<T>(Qualified<? extends Path> qualifiedPath, T value) implements Supplier<T> {

    public Value() {
      this(null, null);
    }

    public Value(final Qualified<? extends Path> qualifiedPath) {
      this(qualifiedPath, null);
    }
    
    public Value(final Path path) {
      this(Qualified.Record.of(path), null);
    }

    public Value(final T value) {
      this(null, value);
    }

    public Value {
      if (qualifiedPath != null && value != null && !AssignableType.of(qualifiedPath.qualified().type()).isAssignable(value.getClass())) {
        throw new IllegalArgumentException("value: " + value);
      }
    }

    @Override
    public final T get() {
      return this.value();
    }

    public final Qualifiers qualifiers() {
      final Qualified<? extends Path> qualifiedPath = this.qualifiedPath();
      return qualifiedPath == null ? Qualifiers.of() : qualifiedPath.qualifiers();
    }
    
    public final Path path() {
      final Qualified<? extends Path> qualifiedPath = this.qualifiedPath();
      return qualifiedPath == null ? null : qualifiedPath.qualified();
    }
    
    public final Type type() {
      final Qualified<? extends Path> qualifiedPath = this.qualifiedPath();
      return qualifiedPath == null ? null : qualifiedPath.qualified().type();
    }

  }

}
