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
import java.util.Objects;

import java.util.function.Supplier;

import org.microbean.development.annotation.Convenience;

public interface ValueSupplier {

  public default boolean respondsFor(final Path path, final Map<?, ?> applicationQualifiers) {
    return true;
  }
  
  public <T> Value<T> get(final Path path, final Map<?, ?> applicationQualifiers);

  public static final record Value<T>(T value, Path path, Map<?, ?> qualifiers) implements Supplier<T> {

    public Value {
      path = Objects.requireNonNull(path, "path");
      qualifiers = qualifiers == null ? Map.of() : Map.copyOf(qualifiers);
    }

    @Override // Supplier
    public final T get() {
      return this.value();
    }

    @Convenience
    @SuppressWarnings("unchecked")
    public final <X> Value<X> cast() {
      return (Value<X>)this;
    }

  }
  
}