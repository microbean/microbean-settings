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

import org.microbean.settings.api.Path.Element;

/**
 * An {@link OptionalSupplier} of configured objects.
 *
 * <p><strong>Note:</strong> {@link Configured} implementations are
 * expected to be immutable with respect to the methods exposed by
 * this interface.  All methods in this interface that have a {@link
 * Configured}-typed return type require their implementations to
 * return a <em>new</em> {@link Configured}.</p>
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see #of()
 */
public interface Configured<T> extends OptionalSupplier<T> {


  /*
   * Abstract instance methods.
   */


  /**
   * Returns the {@link Qualifiers} with which this {@link Configured}
   * is associated.
   *
   * <p>Implementations of this method must not return {@code
   * null}.</p>
   *
   * @return the non-{@code null} {@link Qualifiers} with which this
   * {@link Configured} is associated
   *
   * @nullability Implementations of this method must not return
   * {@code null}.
   *
   * @threadsafety Implementations of this method must be safe for
   * concurrent use by multiple threads.
   *
   * @idempotency Implementations of this method must be idempotent
   * and deterministic.
   */
  public Qualifiers qualifiers();

  /**
   * Returns the {@link Configured} serving as the parent of this
   * {@link Configured}.
   *
   * <p>The "root" {@link Configured} must return itself from its {@link
   * #parent()} implementation.</p>
   *
   * <p>Implementations of this method must not return {@code
   * null}.</p>
   *
   * @param <P> the type of object that the parent can return
   *
   * @return the non-{@code null} {@link Configured} serving as the
   * parent of this {@link Configured}; may be this {@link Configured}
   * itself
   *
   * @nullability Implementations of this method must not return
   * {@code null}.
   *
   * @threadsafety Implementations of this method must be safe for
   * concurrent use by multiple threads.
   *
   * @idempotency Implementations of this method must be idempotent
   * and deterministic.
   */
  // Note that the root will have itself as its parent.
  public <P> Configured<P> parent();

  /**
   * Returns the {@linkplain Path#isAbsolute() absolute} {@link Path}
   * with which this {@link Configured} is associated.
   *
   * <p>Implementations of this method must not return {@code
   * null}.</p>
   *
   * @return the non-{@code null} {@linkplain Path#isAbsolute()
   * absolute} {@link Path} with which this {@link Configured} is
   * associated
   *
   * @nullability Implementations of this method must not return
   * {@code null}.
   *
   * @threadsafety Implementations of this method must be safe for
   * concurrent use by multiple threads.
   *
   * @idempotency Implementations of this method must be idempotent
   * and deterministic.
   */
  public Path absolutePath();

  public <U> Configured<U> of(final Configured<?> requestor, final Path absolutePath);


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
   * Element#name() name()} components of {@link Element}s may
   * unintentionally clash when two components are combined into a
   * single application.</p>
   *
   * <p>Path transliteration must occur during the execution of any
   * implementation of the {@link #of(Configured, Path)} method, such
   * that the {@link Path} supplied to that method, once it has been
   * verified to be {@linkplain Path#isAbsolute() absolute}, is
   * supplied to an implementation of this method. The {@link Path}
   * returned by an implementation of this method must then be used
   * during the rest of the invocation of the {@link #of(Configured,
   * Path)} method, as if it had been supplied in the first place.</p>
   *
   * <p>Behavior resulting from any other usage of an implementation
   * of this method is undefined.</p>
   *
   * <p>The default implementation of this method simply returns the
   * supplied {@link Path}.  Implementations of the {@link Configured}
   * interface are strongly encouraged to actually perform path
   * transliteration.</p>
   *
   * <p>A class that implements {@link Configured} may find {@link
   * StackWalker} particularly useful in implementing this method.</p>
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
   * @see #of(Configured, Path)
   */
  @Experimental
  @OverridingEncouraged
  @SubordinateTo("#of(Configured, Path, Supplier")
  public default Path transliterate(final Path path) {
    if (!path.isAbsolute()) {
      throw new IllegalArgumentException("!path.isAbsolute(): " + path);
    }
    return path;
  }

  @Convenience
  @OverridingDiscouraged
  public default ClassLoader classLoader() {
    return this.absolutePath().classLoader();
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> Configured<U> plus(final Class<? extends U> type) {
    return this.plus(Element.of(type));
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> Configured<U> plus(final Type type) {
    return this.plus(Element.of(type));
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> Configured<U> plus(final String element,
                                        final Class<? extends U> type) {
    return this.plus(element.isEmpty() ? Element.of(type) : Element.of(element, type));
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> Configured<U> plus(final String element,
                                        final Type type) {
    return this.plus(element.isEmpty() ? Element.of(type) : Element.of(element, type));
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> Configured<U> plus(final Element nonRootElement) {
    if (nonRootElement.isRoot()) {
      throw new IllegalArgumentException("nonRootElement.isRoot(): " + nonRootElement);
    }
    return this.plus(Path.relative(nonRootElement));
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> Configured<U> plus(final Path relativePath) {
    if (!relativePath.isRelative()) {
      throw new IllegalArgumentException("relativePath: " + relativePath);
    }
    return this.of(this.root(), this.absolutePath().plus(relativePath)); // NOTE
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> Configured<U> of(final Path absolutePath) {
    if (!absolutePath.isAbsolute()) {
      throw new IllegalArgumentException("absolutePath: " + absolutePath);
    }
    return this.of(this.root(), absolutePath);
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> Configured<U> of(final Class<? extends U> type) {
    return this.of(Element.of(type));
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> Configured<U> of(final Type type) {
    return this.of(Element.of(type));
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> Configured<U> of(final String element,
                                      final Class<? extends U> type) {
    return this.of(element.isEmpty() ? Element.of(type) : Element.of(element, type));
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> Configured<U> of(final String element,
                                      final Type type) {
    return this.of(element.isEmpty() ? Element.of(type) : Element.of(element, type));
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> Configured<U> of(final Element nonRootElement) {
    if (nonRootElement.isRoot()) {
      throw new IllegalArgumentException("nonRootElement.isRoot(): " + nonRootElement);
    }
    return this.of(Path.root().plus(nonRootElement)); // root().plus() is critical here
  }

  /**
   * Returns {@code true} if {@code this} is the instance returned by
   * the {@link #parent()} method.
   *
   * @return {@code true} if {@code this} is the instance returned by
   * the {@link #parent()} method
   *
   * @threadsafety This method is, and (discouraged) overrides of this
   * method must be, safe for concurrent use by multiple threads.
   *
   * @idempotency This method is, and (discouraged) overrides of this
   * method must be, both idempotent and deterministic.
   *
   * @see #parent()
   *
   * @see #root()
   */
  @OverridingDiscouraged
  public default boolean isRoot() {
    return this.parent() == this;
  }

  /**
   * Returns the non-{@code null} {@link Configured} that is at the
   * root of a chain of {@linkplain #parent() parented} {@link
   * Configured} implementations.
   *
   * <p>The returned {@link Configured} must return itself from its
   * {@link #parent()} method or undefined behavior will result.</p>
   *
   * @return the non-{@code null} {@link Configured} that is at the
   * root of a chain of {@linkplain #parent() parented} {@link
   * Configured} implementations
   *
   * @nullability This method does not, and (discouraged) overrides of
   * this method must not, return {@code null}.
   *
   * @threadsafety This method is, and (discouraged) overrides of this
   * method must be, safe for concurrent use by multiple threads.
   *
   * @idempotency This method is, and (discouraged) overrides of this
   * method must be, both idempotent and deterministic.
   */
  @OverridingDiscouraged
  public default Configured<?> root() {
    Configured<?> root = this;
    Configured<?> parent = root.parent();
    while (parent != null && parent != root) {
      // (Strictly speaking, Configured::parent should NEVER be null.)
      root = parent;
      parent = root.parent();
    }
    assert root.absolutePath().equals(Path.root());
    assert this != root ? !this.absolutePath().equals(Path.root()) : true;
    assert this.qualifiers().equals(root.qualifiers());
    return root;
  }


  /*
   * Static methods.
   */


  /**
   * Returns a non-{@code null} {@linkplain #root() root} {@link
   * Configured} that can be used to acquire configured objects.
   *
   * <p>The {@linkplain #root() root} {@link Configured} is located
   * using the {@link ServiceLoader}.  The first of all discovered
   * {@link Configured} instances is used and all others are ignored.</p>
   *
   * <p>The {@link Configured} that is loaded via this mechanism is
   * subject to the following restrictions:</p>
   *
   * <ul>
   *
   * <li>It must return a {@link Path} from its {@link
   * #absolutePath()} implementation that is equal to {@link
   * Path#root() Path.root()}.</li>
   *
   * <li>It must return itself from its {@link #parent()}
   * implementation.</li>
   *
   * <li>It must return itself from its {@link #get()} method.</li>
   *
   * <li>It must return {@code true} from its {@link #isRoot()}
   * implementation.</li>
   *
   * <li>It must return itself from its {@link #root()} method.</li>
   *
   * </ul>
   *
   * <p>That <em>bootstrap</em> instance is then used to find the
   * "real" {@link Configured} implementation, which in most cases is
   * simply itself.</p>
   *
   * <p>The {@link Configured} that is supplied by the bootstrap
   * instance is subject to the following restrictions (which are
   * compatible with the instance's being the bootstrap instance
   * itself):</p>
   *
   * <ul>
   *
   * <li>It must return a {@link Path} from its {@link
   * #absolutePath()} implementation that is equal to {@link
   * Path#root() Path.root()}.</li>
   *
   * <li>It must return the bootstrap instance from its {@link
   * #parent()} implementation.</li>
   *
   * <li>It must return a {@link Configured} implementation, normally
   * itself, from its {@link #get()} method.</li>
   *
   * </ul>
   *
   * <p>This is the entry point for end users of this framework.</p>
   *
   * @return a non-{@code null} {@linkplain #root() root} {@link
   * Configured} that can be used to acquire configured objects
   *
   * @exception IllegalStateException if any of the restrictions above
   * is violated
   */
  @EntryPoint
  @SuppressWarnings("static")
  public static Configured<?> of() {
    return new Object() {
      private static final Configured<?> instance;
      static {

        final Configured<?> bootstrapConfigured =
          ServiceLoader.load(Configured.class, Configured.class.getClassLoader()).findFirst().orElseThrow();

        if (!Path.root().equals(bootstrapConfigured.absolutePath())) {
          throw new IllegalStateException("bootstrapConfigured.absolutePath(): " + bootstrapConfigured.absolutePath());
        } else if (bootstrapConfigured.parent() != bootstrapConfigured) {
          throw new IllegalStateException("bootstrapConfigured.parent(): " + bootstrapConfigured.parent());
        } else if (bootstrapConfigured.get() != bootstrapConfigured) {
          throw new IllegalStateException("bootstrapConfigured.get(): " + bootstrapConfigured.get());
        } else if (!bootstrapConfigured.isRoot()) {
          throw new IllegalStateException("!bootstrapConfigured.isRoot()");
        } else if (bootstrapConfigured.root() != bootstrapConfigured) {
          throw new IllegalStateException("bootstrapConfigured.root(): " + bootstrapConfigured.root());
        }

        instance = bootstrapConfigured
          .<Configured<?>>of(new TypeToken<Configured<?>>() {}.type())
          .orElse(bootstrapConfigured);

        if (!Path.root().equals(bootstrapConfigured.absolutePath())) {
          throw new IllegalStateException("instance.absolutePath(): " + instance.absolutePath());
        } else if (instance.parent() != bootstrapConfigured) {
          throw new IllegalStateException("instance.parent(): " + instance.parent());
        } else if (!(instance.get() instanceof Configured)) {
          throw new IllegalStateException("instance.get(): " + instance.get());
        }
      }
    }.instance;
  }

}
