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
import org.microbean.development.annotation.Experimental;
import org.microbean.development.annotation.OverridingDiscouraged;
import org.microbean.development.annotation.OverridingEncouraged;
import org.microbean.development.annotation.SubordinateTo;

/**
 * An {@link OptionalSupplier} of configured objects.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see #of()
 */
public interface ConfiguredSupplier<T> extends OptionalSupplier<T> {


  /*
   * Abstract instance methods.
   */


  public Qualifiers qualifiers();

  // Note that the root will have itself as its parent.
  public <P> ConfiguredSupplier<P> parent();

  public Path path();

  public <U> ConfiguredSupplier<U> of(final ConfiguredSupplier<?> parent,
                                      final Path absolutePath);


  /*
   * Default instance methods.
   */


  /**
   * <em>Transliterates</em> the supplied {@linkplain
   * Path#isAbsolute() absolute <code>Path</code>} into some other
   * {@link Path} whose meaning is the same but whose representation
   * may be different that will be used instead.
   *
   * <p>The {@link Path} that is returned may be the {@link Path} that
   * was supplied.  This may happen, for example, if {@linkplain
   * Path#isTransliterated() the path has already been
   * transliterated}, or if the path identifies a transliteration
   * request.</p>
   *
   * <p>Path transliteration is needed because the {@link
   * Accessor#name() name()} components of {@link Accessor}s may
   * unintentionally clash when two components are combined into a
   * single application.</p>
   *
   * <p>Path transliteration must occur during the execution of any
   * implementation of the {@link #of(ConfiguredSupplier, Path)}
   * method, such that the {@link Path} supplied to that method, once
   * it has been verified to be {@linkplain Path#isAbsolute()
   * absolute}, is supplied to an implementation of this method. The
   * {@link Path} returned by an implementation of this method must
   * then be used during the rest of the invocation of the {@link
   * #of(ConfiguredSupplier, Path)} method, as if it had been supplied
   * in the first place.</p>
   *
   * <p>Behavior resulting from any other usage of an implementation
   * of this method is undefined.</p>
   *
   * <p>The default implementation of this method simply returns the
   * supplied {@link Path}.  Implementations of the {@link
   * ConfiguredSupplier} interface are strongly encouraged to actually
   * perform path transliteration.</p>
   *
   * <p>A class that implements {@link ConfiguredSupplier} may find
   * {@link StackWalker} particularly useful in implementing this method.</p>
   *
   * @param path an {@linkplain Path#isAbsolute() absolute
   * <code>Path</code>}; must not be null; must return {@code true}
   * from its {@link Path#isAbsolute() isAbsolute()} method
   *
   * @return the transliterated {@link Path}; never {@code null}
   *
   * @exception NullPointerException if {@code path} is {@code null}
   *
   * @exception IllegalArgumentException if {@code path} returns
   * {@code false} from its {@link Path#isAbsolute() isAbsolute()}
   * method
   *
   * @nullability Implementations of this method must not return
   * {@code null}.
   *
   * @threadsafety Implementations of this method must be safe for
   * concurrent use by multiple threads.
   *
   * @idempotency Implementations of this method must be idempotent
   * and deterministic.
   *
   * @see Path#isAbsolute()
   *
   * @see Path#transliterate(BiFunction)
   *
   * @see #of(ConfiguredSupplier, Path)
   */
  @Experimental
  @OverridingEncouraged
  @SubordinateTo("#of(ConfiguredSupplier, Path, Supplier")
  public default Path transliterate(final Path path) {
    if (!path.isAbsolute()) {
      throw new IllegalArgumentException("!path.isAbsolute(): " + path);
    }
    return path;
  }

  @Convenience
  @OverridingDiscouraged
  public default ClassLoader classLoader() {
    return this.path().classLoader();
  }

  @OverridingDiscouraged
  public default <U> ConfiguredSupplier<U> plus(final Class<? extends U> type) {
    return
      this.plus(Accessor.of(),
                type);
  }

  @OverridingDiscouraged
  @SuppressWarnings("unchecked")
  public default <U> ConfiguredSupplier<U> plus(final Type type) {
    if (type instanceof Class<?>) {
      return
        this.plus((Class<? extends U>)type);
    } else {
      return
        this.plus(Accessor.of(),
                  type);
    }
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> ConfiguredSupplier<U> plus(final String accessor,
                                                final Class<? extends U> type) {
    return
      this.plus(accessor.isEmpty() ? Accessor.of() : Accessor.of(accessor),
                type);
  }


  @Convenience
  @OverridingDiscouraged
  @SuppressWarnings("unchecked")
  public default <U> ConfiguredSupplier<U> plus(final String accessor,
                                                final Type type) {
    if (type instanceof Class) {
      return
        this.plus(accessor,
                  (Class<? extends U>)type);
    } else {
      return
        this.plus(accessor.isEmpty() ? Accessor.of() : Accessor.of(accessor),
                  type);
    }
  }

  public default <U> ConfiguredSupplier<U> plus(final Accessor accessor,
                                                final Class<? extends U> type) {
    return
      this.plus(Path.of(accessor, type));
  }

  @Convenience
  @OverridingDiscouraged
  @SuppressWarnings("unchecked")
  public default <U> ConfiguredSupplier<U> plus(final Accessor accessor,
                                                final Type type) {
    if (type instanceof Class) {
      return
        this.plus(accessor,
                  (Class<? extends U>)type);
    } else {
      return
        this.plus(Path.of(accessor, type));
    }
  }

  @OverridingDiscouraged
  public default <U> ConfiguredSupplier<U> plus(final Path path) {
    return
      this.of(this,
              this.path().plus(path)); // NOTE
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> ConfiguredSupplier<U> of(final ConfiguredSupplier<?> parent,
                                              final Class<? extends U> type) {
    return
      this.of(parent,
              Accessor.of(),
              type);
  }

  @Convenience
  @OverridingDiscouraged
  @SuppressWarnings("unchecked")
  public default <U> ConfiguredSupplier<U> of(final ConfiguredSupplier<?> parent,
                                              final Type type) {
    if (type instanceof Class) {
      return
        this.of(parent,
                (Class<? extends U>)type);
    } else {
      return
        this.of(parent,
                Accessor.of(),
                type);
    }
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> ConfiguredSupplier<U> of(final ConfiguredSupplier<?> parent,
                                              final String accessor,
                                              final Class<? extends U> type) {
    return
      this.of(parent,
              accessor.isEmpty() ? Accessor.of() : Accessor.of(accessor),
              type);
  }


  @Convenience
  @OverridingDiscouraged
  @SuppressWarnings("unchecked")
  public default <U> ConfiguredSupplier<U> of(final ConfiguredSupplier<?> parent,
                                              final String accessor,
                                              final Type type) {
    if (type instanceof Class) {
      return
        this.of(parent,
                accessor,
                (Class<? extends U>)type);
    } else {
      return
        this.of(parent,
                accessor.isEmpty() ? Accessor.of() : Accessor.of(accessor),
                type);
    }
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> ConfiguredSupplier<U> of(final ConfiguredSupplier<?> parent,
                                              final Accessor accessor,
                                              final Class<? extends U> type) {
    return
      this.of(parent,
              Path.of().plus(accessor, type)); // of().plus() is critical here
  }


  @Convenience
  @OverridingDiscouraged
  @SuppressWarnings("unchecked")
  public default <U> ConfiguredSupplier<U> of(final ConfiguredSupplier<?> parent,
                                              final Accessor accessor,
                                              final Type type) {
    if (type instanceof Class) {
      return
        this.of(parent,
                accessor,
                (Class<? extends U>)type);
    } else {
      return
        this.of(parent,
                Path.of().plus(accessor, type)); // of().plus() is critical here
    }
  }

  @OverridingDiscouraged
  public default <U> ConfiguredSupplier<U> of(final Path absolutePath) {
    return
      this.of(this.root(),
              absolutePath);
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> ConfiguredSupplier<U> of(final Class<? extends U> type) {
    return
      this.of(Accessor.of(),
              type);
  }


  @Convenience
  @OverridingDiscouraged
  public default <U> ConfiguredSupplier<U> of(final Type type) {
    return
      this.of(Accessor.of(),
              type);
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> ConfiguredSupplier<U> of(final String accessor,
                                              final Class<? extends U> type) {
    return
      this.of(accessor.isEmpty() ? Accessor.of() : Accessor.of(accessor),
              type);
  }

  @Convenience
  @OverridingDiscouraged
  @SuppressWarnings("unchecked")
  public default <U> ConfiguredSupplier<U> of(final String accessor,
                                              final Type type) {
    if (type instanceof Class) {
      return
        this.of(accessor,
                (Class<? extends U>)type);
    } else {
      return
        this.of(accessor.isEmpty() ? Accessor.of() : Accessor.of(accessor),
                type);
    }
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> ConfiguredSupplier<U> of(final Accessor accessor,
                                              final Class<? extends U> type) {
    return
      this.of(Path.of().plus(accessor, type)); // of().plus() is critical here
  }

  @Convenience
  @OverridingDiscouraged
  @SuppressWarnings("unchecked")
  public default <U> ConfiguredSupplier<U> of(final Accessor accessor,
                                              final Type type) {
    if (type instanceof Class) {
      return
        this.of(accessor,
                (Class<? extends U>)type);
    } else {
      return
        this.of(Path.of().plus(accessor, type)); // of().plus() is critical here
    }
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

        instance = bootstrapConfiguredSupplier
          .<ConfiguredSupplier<?>>of(new TypeToken<ConfiguredSupplier<?>>() {}.type())
          .orElse(bootstrapConfiguredSupplier);

        if (instance.parent() != bootstrapConfiguredSupplier) {
          throw new IllegalStateException("instance.parent(): " + instance.parent());
        } else if (!(instance.get() instanceof ConfiguredSupplier)) {
          throw new IllegalStateException("instance.get(): " + instance.get());
        }
      }
    }.instance;
  }

}
