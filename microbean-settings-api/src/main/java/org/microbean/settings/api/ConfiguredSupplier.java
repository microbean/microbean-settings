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

import java.util.ServiceLoader;

import java.util.function.Supplier;

public interface ConfiguredSupplier<T> extends Supplier<T> {

  public Qualifiers qualifiers();

  public Path path();

  public default <U> ConfiguredSupplier<U> plus(final Type type) {
    return this.plus(Path.of(Accessor.of(), type), ConfiguredSupplier::fail);
  }

  public default <U> ConfiguredSupplier<U> plus(final Type type, final Supplier<U> defaultSupplier) {
    return this.plus(Path.of(Accessor.of(), type), defaultSupplier);
  }

  public default <U> ConfiguredSupplier<U> plus(final Path path) {
    return this.plus(path, ConfiguredSupplier::fail);
  }

  public <U> ConfiguredSupplier<U> plus(final Path path, final Supplier<U> defaultSupplier);

  public default <P, U> ConfiguredSupplier<U> of(final Qualifiers qualifiers,
                                                 final Supplier<P> parentSupplier,
                                                 final Path path) {
    return this.of(qualifiers, parentSupplier, path, ConfiguredSupplier::fail);
  }

  public <P, U> ConfiguredSupplier<U> of(final Qualifiers qualifiers,
                                         final Supplier<P> parentSupplier,
                                         final Path path,
                                         final Supplier<U> defaultSupplier);

  @SuppressWarnings("static")
  public static ConfiguredSupplier<?> of() {
    return new Object() {
      private static final ConfiguredSupplier<?> instance;
      static {
        final ConfiguredSupplier<?> bootstrapConfiguredSupplier =
          ServiceLoader.load(ConfiguredSupplier.class, ConfiguredSupplier.class.getClassLoader()).findFirst().orElseThrow();
        instance = bootstrapConfiguredSupplier.plus(ConfiguredSupplier.class, () -> bootstrapConfiguredSupplier);
      }
    }.instance;
  }

  public static <U> ConfiguredSupplier<U> of(final Type type) {
    return of().plus(type);
  }

  private static <U> U fail() {
    throw new UnsupportedOperationException();
  }

}
