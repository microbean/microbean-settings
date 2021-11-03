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

import org.microbean.development.annotation.Convenience;

public interface ConfiguredSupplier<T> extends Supplier<T> {


  /*
   * Abstract instance methods.
   */


  public Qualifiers qualifiers();

  public <P> Optional<ConfiguredSupplier<P>> parent();

  public Path path();

  public <U> ConfiguredSupplier<U> of(final ConfiguredSupplier<?> parent,
                                      final Path path,
                                      final Supplier<U> defaultSupplier);


  /*
   * Default instance methods.
   */


  public default ClassLoader classLoader() {
    return this.path().classLoader();
  }
  
  public default <U> ConfiguredSupplier<U> plus(final Type type) {
    return
      this.plus(type,
                ConfiguredSupplier::fail);
  }

  @Convenience
  public default <U> ConfiguredSupplier<U> plus(final String accessor,
                                                final Type type) {
    return
      this.plus(accessor,
                type,
                ConfiguredSupplier::fail);
  }

  @Convenience
  public default <U> ConfiguredSupplier<U> plus(final String accessor,
                                                final Type type,
                                                final Supplier<U> defaultSupplier) {
    return
      this.plus(Path.of(Accessor.of(accessor), type),
                defaultSupplier);
  }

  @Convenience
  public default <U> ConfiguredSupplier<U> plus(final String accessor,
                                                final Type type,
                                                final U defaultValue) {
    return
      this.plus(Path.of(Accessor.of(accessor), type),
                defaultValue);
  }

  public default <U> ConfiguredSupplier<U> plus(final Type type,
                                                final Supplier<U> defaultSupplier) {
    return
      this.plus(Path.of(Accessor.of(), type),
                defaultSupplier);
  }

  @Convenience
  public default <U> ConfiguredSupplier<U> plus(final Type type,
                                                final U defaultValue) {
    return
      this.plus(Path.of(Accessor.of(), type),
                () -> defaultValue);
  }

  public default <U> ConfiguredSupplier<U> plus(final Path path) {
    return
      this.plus(path,
                ConfiguredSupplier::fail);
  }

  public default <U> ConfiguredSupplier<U> plus(final Path path,
                                                final Supplier<U> defaultSupplier) {
    return
      this.of(this,
              this.path().plus(path),
              defaultSupplier);
  }

  @Convenience
  public default <U> ConfiguredSupplier<U> plus(final Path path,
                                                final U defaultValue) {
    return
      this.of(this,
              this.path().plus(path),
              () -> defaultValue);
  }

  public default <U> ConfiguredSupplier<U> of(final ConfiguredSupplier<?> parent,
                                              final Path path) {
    return
      this.of(parent,
              path,
              ConfiguredSupplier::fail);
  }

  @Convenience
  public default <U> ConfiguredSupplier<U> of(final ConfiguredSupplier<?> parent,
                                              final String accessor,
                                              final Type type) {
    return
      this.of(parent,
              accessor,
              type,
              ConfiguredSupplier::fail);
  }

  @Convenience
  public default <U> ConfiguredSupplier<U> of(final ConfiguredSupplier<?> parent,
                                              final String accessor,
                                              final Type type,
                                              final Supplier<U> defaultSupplier) {
    return
      this.of(parent,
              accessor,
              type,
              defaultSupplier);
  }

  @Convenience
  public default <U> ConfiguredSupplier<U> of(final ConfiguredSupplier<?> parent,
                                              final String accessor,
                                              final Type type,
                                              final U defaultValue) {
    return
      this.of(parent,
              accessor,
              type,
              defaultValue);
  }

  @Convenience
  public default <U> ConfiguredSupplier<U> of(final ConfiguredSupplier<?> parent,
                                              final Path path,
                                              final U defaultValue) {
    return
      this.of(parent,
              path,
              () -> defaultValue);
  }

  public default <U> ConfiguredSupplier<U> of(final Path path) {
    return
      this.of(path,
              ConfiguredSupplier::fail);
  }

  public default <U> ConfiguredSupplier<U> of(final Path path,
                                              final Supplier<U> defaultSupplier) {
    return
      this.of(this.root(),
              path,
              defaultSupplier);
  }

  @Convenience
  public default <U> ConfiguredSupplier<U> of(final Path path,
                                              final U defaultValue) {
    return
      this.of(this.root(),
              path,
              () -> defaultValue);
  }

  @Convenience
  public default <U> ConfiguredSupplier<U> of(final Type type) {
    return
      this.of(Path.of(Accessor.of(), type),
              ConfiguredSupplier::fail);
  }

  @Convenience
  public default <U> ConfiguredSupplier<U> of(final Type type,
                                              final Supplier<U> defaultSupplier) {
    return
      this.of(Path.of(Accessor.of(), type),
              defaultSupplier);
  }

  @Convenience
  public default <U> ConfiguredSupplier<U> of(final Type type,
                                              final U defaultValue) {
    return
      this.of(Path.of(Accessor.of(), type),
              () -> defaultValue);
  }
  
  @Convenience
  public default <U> ConfiguredSupplier<U> of(final String accessor,
                                              final Type type) {
    return
      this.of(accessor,
              type,
              ConfiguredSupplier::fail);
  }

  @Convenience
  public default <U> ConfiguredSupplier<U> of(final String accessor,
                                              final Type type,
                                              final U defaultValue) {
    return
      this.of(accessor,
              type,
              () -> defaultValue);
  }

  @Convenience
  public default <U> ConfiguredSupplier<U> of(final String accessor,
                                              final Type type,
                                              final Supplier<U> defaultSupplier) {
    return
      this.of(Path.of(Accessor.of(accessor), type),
              defaultSupplier);
  }

  public default ConfiguredSupplier<?> root() {
    ConfiguredSupplier<?> root = this;
    Optional<ConfiguredSupplier<Object>> parent = root.parent();
    while (parent.isPresent() && parent.orElseThrow() != root) {
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
        assert bootstrapConfiguredSupplier.parent().orElseThrow() == bootstrapConfiguredSupplier;
        assert bootstrapConfiguredSupplier.get() == bootstrapConfiguredSupplier;
        instance = bootstrapConfiguredSupplier.of(ConfiguredSupplier.class, () -> bootstrapConfiguredSupplier).get();
        assert instance.get() instanceof ConfiguredSupplier;
        assert instance.parent().orElseThrow() == bootstrapConfiguredSupplier;
      }
    }.instance;
  }

  private static <U> U fail() {
    throw new UnsupportedOperationException();
  }

}
