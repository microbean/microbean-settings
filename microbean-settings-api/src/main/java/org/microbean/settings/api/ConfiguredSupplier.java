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

import org.microbean.development.annotation.Convenience;
import org.microbean.development.annotation.EntryPoint;
import org.microbean.development.annotation.OverridingDiscouraged;

/**
 * A {@link Supplier} of configured objects.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see #of()
 */
public interface ConfiguredSupplier<T> extends Supplier<T> {


  /*
   * Abstract instance methods.
   */


  public Qualifiers qualifiers();

  // Note that the root will have itself as its parent.
  public <P> ConfiguredSupplier<P> parent();

  public Path path();

  public <U> ConfiguredSupplier<U> of(final ConfiguredSupplier<?> parent,
                                      final Path path,
                                      final Supplier<U> defaultSupplier);


  /*
   * Default instance methods.
   */


  @Convenience
  @OverridingDiscouraged
  public default ClassLoader classLoader() {
    return this.path().classLoader();
  }

  @OverridingDiscouraged
  public default <U> ConfiguredSupplier<U> plus(final Type type) {
    return
      this.plus(type,
                ConfiguredSupplier::fail);
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> ConfiguredSupplier<U> plus(final String accessor,
                                                final Type type) {
    return
      this.plus(accessor,
                type,
                ConfiguredSupplier::fail);
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> ConfiguredSupplier<U> plus(final String accessor,
                                                final Type type,
                                                final Supplier<U> defaultSupplier) {
    return
      this.plus(Path.of(Accessor.of(accessor), type),
                defaultSupplier);
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> ConfiguredSupplier<U> plus(final String accessor,
                                                final Type type,
                                                final U defaultValue) {
    return
      this.plus(Path.of(Accessor.of(accessor), type),
                defaultValue);
  }

  @OverridingDiscouraged
  public default <U> ConfiguredSupplier<U> plus(final Type type,
                                                final Supplier<U> defaultSupplier) {
    return
      this.plus(Path.of(Accessor.of(), type),
                defaultSupplier);
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> ConfiguredSupplier<U> plus(final Type type,
                                                final U defaultValue) {
    return
      this.plus(Path.of(Accessor.of(), type),
                () -> defaultValue);
  }

  @OverridingDiscouraged
  public default <U> ConfiguredSupplier<U> plus(final Path path) {
    return
      this.plus(path,
                ConfiguredSupplier::fail);
  }

  @OverridingDiscouraged
  public default <U> ConfiguredSupplier<U> plus(final Path path,
                                                final Supplier<U> defaultSupplier) {
    return
      this.of(this,
              this.path().plus(path),
              defaultSupplier);
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> ConfiguredSupplier<U> plus(final Path path,
                                                final U defaultValue) {
    return
      this.of(this,
              this.path().plus(path),
              () -> defaultValue);
  }

  @OverridingDiscouraged
  public default <U> ConfiguredSupplier<U> of(final ConfiguredSupplier<?> parent,
                                              final Path path) {
    return
      this.of(parent,
              path,
              ConfiguredSupplier::fail);
  }

  @Convenience
  @OverridingDiscouraged
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
  @OverridingDiscouraged
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
  @OverridingDiscouraged
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
  @OverridingDiscouraged
  public default <U> ConfiguredSupplier<U> of(final ConfiguredSupplier<?> parent,
                                              final Path path,
                                              final U defaultValue) {
    return
      this.of(parent,
              path,
              () -> defaultValue);
  }

  @OverridingDiscouraged
  public default <U> ConfiguredSupplier<U> of(final Path path) {
    return
      this.of(path,
              ConfiguredSupplier::fail);
  }

  @OverridingDiscouraged
  public default <U> ConfiguredSupplier<U> of(final Path path,
                                              final Supplier<U> defaultSupplier) {
    return
      this.of(this.root(),
              path,
              defaultSupplier);
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> ConfiguredSupplier<U> of(final Path path,
                                              final U defaultValue) {
    return
      this.of(this.root(),
              path,
              () -> defaultValue);
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> ConfiguredSupplier<U> of(final Type type) {
    return
      this.of(Path.of(Accessor.of(), type),
              ConfiguredSupplier::fail);
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> ConfiguredSupplier<U> of(final Type type,
                                              final Supplier<U> defaultSupplier) {
    return
      this.of(Path.of(Accessor.of(), type),
              defaultSupplier);
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> ConfiguredSupplier<U> of(final Type type,
                                              final U defaultValue) {
    return
      this.of(Path.of(Accessor.of(), type),
              () -> defaultValue);
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> ConfiguredSupplier<U> of(final String accessor,
                                              final Type type) {
    return
      this.of(accessor,
              type,
              ConfiguredSupplier::fail);
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> ConfiguredSupplier<U> of(final String accessor,
                                              final Type type,
                                              final U defaultValue) {
    return
      this.of(accessor,
              type,
              () -> defaultValue);
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> ConfiguredSupplier<U> of(final String accessor,
                                              final Type type,
                                              final Supplier<U> defaultSupplier) {
    return
      this.of(Path.of(Accessor.of(accessor), type),
              defaultSupplier);
  }

  @OverridingDiscouraged
  public default boolean isRoot() {
    return this.parent() == this;
  }

  @OverridingDiscouraged
  public default ConfiguredSupplier<?> root() {
    ConfiguredSupplier<?> root = this;
    ConfiguredSupplier<?> parent = root.parent();
    while (parent != null && parent != root) {
      root = parent;
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


  @EntryPoint
  @SuppressWarnings("static")
  public static ConfiguredSupplier<?> of() {
    return new Object() {
      private static final ConfiguredSupplier<?> instance;
      static {
        final ConfiguredSupplier<?> bootstrapConfiguredSupplier =
          ServiceLoader.load(ConfiguredSupplier.class, ConfiguredSupplier.class.getClassLoader()).findFirst().orElseThrow();
        if (!Path.of().equals(bootstrapConfiguredSupplier.path())) {
          throw new IllegalStateException("path(): " + bootstrapConfiguredSupplier.path());
        } else if (bootstrapConfiguredSupplier.parent() != bootstrapConfiguredSupplier) {
          throw new IllegalStateException("parent(): " + bootstrapConfiguredSupplier.parent());
        } else if (bootstrapConfiguredSupplier.get() != bootstrapConfiguredSupplier) {
          throw new IllegalStateException("bootstrapConfiguredSupplier.get(): " + bootstrapConfiguredSupplier.get());
        } else if (!bootstrapConfiguredSupplier.isRoot()) {
          throw new IllegalStateException("!bootstrapConfiguredSupplier.isRoot()");
        } else if (bootstrapConfiguredSupplier.root() != bootstrapConfiguredSupplier) {
          throw new IllegalStateException("root(): " + bootstrapConfiguredSupplier.root());
        }
        instance = bootstrapConfiguredSupplier.of(ConfiguredSupplier.class, () -> bootstrapConfiguredSupplier).get();
        if (instance.parent() != bootstrapConfiguredSupplier) {
          throw new IllegalStateException("instance.parent(): " + instance.parent());
        } else if (!(instance.get() instanceof ConfiguredSupplier)) {
          throw new IllegalStateException("instance.get(): " + instance.get());
        }
      }
    }.instance;
  }

  private static <U> U fail() {
    throw new UnsupportedOperationException();
  }

}
