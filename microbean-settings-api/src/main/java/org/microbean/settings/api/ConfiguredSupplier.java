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

import java.util.Optional;
import java.util.ServiceLoader;

import java.util.function.Supplier;

public interface ConfiguredSupplier<T> extends Supplier<T> {


  /*
   * Abstract instance methods.
   */


  public Qualifiers qualifiers();

  public <P> Optional<ConfiguredSupplier<P>> parent();

  public Path path();

  // Effectively a constructor.
  public <U> ConfiguredSupplier<U> of(final Qualifiers qualifiers,
                                      final ConfiguredSupplier<?> parent,
                                      final Path path,
                                      final Supplier<U> defaultSupplier);


  /*
   * Default instance methods.
   */


  public default <U> ConfiguredSupplier<U> plus(final Type type) {
    return
      this.plus(type,
                ConfiguredSupplier::fail);
  }

  public default <U> ConfiguredSupplier<U> plus(final Type type,
                                                final Supplier<U> defaultSupplier) {
    return
      this.plus(Path.of(Accessor.of(), type),
                defaultSupplier);
  }

  public default <U> ConfiguredSupplier<U> plus(final Path path) {
    return
      this.plus(path,
                ConfiguredSupplier::fail);
  }

  public default <U> ConfiguredSupplier<U> plus(final Path path,
                                                final Supplier<U> defaultSupplier) {
    return
      this.of(this.qualifiers(),
              this,
              this.path().plus(path),
              defaultSupplier);
  }

  public default <U> ConfiguredSupplier<U> of(final Qualifiers qualifiers,
                                              final Path path) {
    return
      this.of(qualifiers,
              path,
              ConfiguredSupplier::fail);
  }

  public default <U> ConfiguredSupplier<U> of(final Qualifiers qualifiers,
                                              final Path path,
                                              final Supplier<U> defaultSupplier) {
    return
      this.of(qualifiers,
              this.root(),
              path,
              defaultSupplier);
  }

  public default <U> ConfiguredSupplier<U> of(final Qualifiers qualifiers,
                                              final ConfiguredSupplier<?> parent,
                                              final Path path) {
    return
      this.of(qualifiers,
              parent,
              path,
              ConfiguredSupplier::fail);
  }

  public default <U> ConfiguredSupplier<U> of(final ConfiguredSupplier<?> parent,
                                              final Path path) {
    return
      this.of(parent,
              path,
              ConfiguredSupplier::fail);
  }

  public default <U> ConfiguredSupplier<U> of(final ConfiguredSupplier<?> parent,
                                              final Path path,
                                              final Supplier<U> defaultSupplier) {
    return
      this.of(parent.qualifiers(),
              parent,
              path,
              defaultSupplier);
  }

  public default <U> ConfiguredSupplier<U> of(final Path path) {
    return
      this.of(path,
              ConfiguredSupplier::fail);
  }
  
  public default <U> ConfiguredSupplier<U> of(final Path path,
                                              final Supplier<U> defaultSupplier) {
    final ConfiguredSupplier<?> root = this.root();
    return
      this.of(root.qualifiers(),
              root,
              path,
              defaultSupplier);
  }

  public default ConfiguredSupplier<?> root() {
    ConfiguredSupplier<?> root = this;
    Optional<ConfiguredSupplier<Object>> parent = this.parent();
    while (parent.isPresent()) {
      root = parent.orElseThrow();
      parent = root.parent();
    }
    assert root.path().equals(Path.of());
    assert this != root ? !this.path().equals(Path.of()) : true;
    assert this.qualifiers().equals(root.qualifiers());
    return root;
  }


  /*
   * Static methods.
   */


  @SuppressWarnings("static")
  public static ConfiguredSupplier<?> of() {
    return new Object() {
      private static final ConfiguredSupplier<?> instance;
      static {
        final ConfiguredSupplier<?> bootstrapConfiguredSupplier =
          ServiceLoader.load(ConfiguredSupplier.class, ConfiguredSupplier.class.getClassLoader()).findFirst().orElseThrow();
        if (!Path.of().equals(bootstrapConfiguredSupplier.path())) {
          throw new IllegalStateException("path(): " + bootstrapConfiguredSupplier.path());
        }
        final ConfiguredSupplier<ConfiguredSupplier<?>> configuredSupplierSupplier =
          bootstrapConfiguredSupplier.plus(ConfiguredSupplier.class, () -> bootstrapConfiguredSupplier);
        instance = configuredSupplierSupplier.get();
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
