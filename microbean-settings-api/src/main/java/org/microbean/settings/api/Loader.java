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

import java.util.List;
import java.util.ServiceLoader;

import java.util.function.Supplier;

import org.microbean.development.annotation.Convenience;
import org.microbean.development.annotation.EntryPoint;
import org.microbean.development.annotation.Experimental;
import org.microbean.development.annotation.OverridingDiscouraged;
import org.microbean.development.annotation.OverridingEncouraged;

import org.microbean.settings.api.Path.Element;

/**
 * An {@link OptionalSupplier} of configured objects.
 *
 * <p><strong>Note:</strong> {@link Loader} implementations are
 * expected to be immutable with respect to the methods exposed by
 * this interface.  All methods in this interface that have a {@link
 * Loader}-typed return type require their implementations to
 * return a <em>new</em> {@link Loader}.</p>
 *
 * @param <T> the type of configured objects this {@link Loader}
 * supplies
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see #loader()
 *
 * @see OptionalSupplier#get()
 *
 * @see #load(Path)
 */
public interface Loader<T> extends OptionalSupplier<T> {


  /*
   * Abstract instance methods.
   */


  /**
   * Returns the {@link Qualifiers} with which this {@link Loader}
   * is associated.
   *
   * <p>Implementations of this method must not return {@code
   * null}.</p>
   *
   * @return the non-{@code null} {@link Qualifiers} with which this
   * {@link Loader} is associated
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
   * Returns the {@link Loader} serving as the parent of this
   * {@link Loader}.
   *
   * <p>The "root" {@link Loader} must return itself from its {@link
   * #parent()} implementation.</p>
   *
   * <p>Implementations of this method must not return {@code
   * null}.</p>
   *
   * @return the non-{@code null} {@link Loader} serving as the
   * parent of this {@link Loader}; may be this {@link Loader}
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
  public Loader<?> parent();

  /**
   * Returns the {@linkplain Path#isAbsolute() absolute} {@link Path}
   * with which this {@link Loader} is associated.
   *
   * <p>Implementations of this method must not return {@code
   * null}.</p>
   *
   * @return the non-{@code null} {@linkplain Path#isAbsolute()
   * absolute} {@link Path} with which this {@link Loader} is
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
  public Path<T> absolutePath();

  /**
   * Returns a {@link Loader} that can supply configured objects
   * that are suitable for the supplied {@code path}.
   *
   * <p>After {@linkplain #loader() bootstrapping}, this method serves as
   * the main entry point into this framework.</p>
   *
   * <p>Implementations of this method must obtain a normalized {@link
   * Path} as if by passing the supplied {@code path} to the default
   * implementation of the {@link #normalize(Path)} method and using
   * its return value.</p>
   *
   * <p>Implementations of this method that may invoke other
   * mechanisms expecting a {@link Loader} must supply them with a
   * {@link Loader} as if returned by the {@link
   * #loaderFor(Path)} method when supplied with the supplied
   * {@code path}.</p>
   *
   * <p>Although implementations of this method must not return {@code
   * null}, the {@link Loader} that is returned may return {@code
   * null} from its {@link Loader#get() get()} method.
   * Additionally, the {@link Loader} that is returned may throw
   * {@link java.util.NoSuchElementException} or {@link
   * UnsupportedOperationException} from its {@link Loader#get()
   * get()} method.</p>
   *
   * @param <U> the type of the supplied {@link Path} and the type of
   * the returned {@link Loader}
   *
   * @param path the {@link Path} for which a {@link Loader}
   * should be returned; must not be {@code null}
   *
   * @return a {@link Loader} capable of supplying configured
   * objects suitable for the supplied {@code path}; never {@code
   * null}
   *
   * @exception NullPointerException if {@code path} is {@code null}
   *
   * @exception IllegalArgumentException if the {@code path}, after
   * {@linkplain #normalize(Path) normalization}, {@linkplain
   * Path#isRoot() is the root <code>Path</code>}
   *
   * @nullability Implementations of this method must not return
   * {@code null}.
   *
   * @threadsafety Implementations of this method must be safe for
   * concurrent use by multiple threads.
   *
   * @threadsafety Implementations of this method must be idempotent
   * and deterministic.
   */
  @EntryPoint
  public <U> Loader<U> load(final Path<U> path);


  /*
   * Default instance methods.
   */


  /**
   * <em>Transliterates</em> the supplied {@linkplain
   * Path#isAbsolute() absolute <code>Path</code>} into some other
   * {@link Path}, whose meaning is the same, but whose representation
   * may be different, that will be used instead.
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
   * implementation of the {@link #load(Path)} method, such that the
   * {@link Path} supplied to that method, once it has been verified
   * to be {@linkplain Path#isAbsolute() absolute}, is supplied to an
   * implementation of this method. The {@link Path} returned by an
   * implementation of this method must then be used during the rest
   * of the invocation of the {@link #load(Path)} method, as if it had
   * been supplied in the first place.</p>
   *
   * <p>Behavior resulting from any other usage of an implementation
   * of this method is undefined.</p>
   *
   * <p>The default implementation of this method uses the
   * configuration system itself to find a transliterated {@link Path}
   * corresponding to the supplied {@link Path}, returning the
   * supplied {@code path} itself if no transliteration can take
   * place.</p>
   *
   * <p>Overrides of this method <strong>must not call {@link
   * #normalize(Path)}</strong> or an infinite loop may result.</p>
   *
   * <p>Overrides of this method <strong>must not call {@link
   * #loaderFor(Path)}</strong> or an infinite loop may
   * result.</p>
   *
   * <p>Overrides of this method <strong>must not call {@link
   * #load(Path)} with the supplied {@code path}</strong> or an infinite
   * loop may result.</p>
   *
   * <p>An implementation of {@link Loader} will find {@link
   * Path#transliterate(java.util.function.BiFunction)} particularly
   * useful in implementing this method.</p>
   *
   * @param <U> the type of the supplied and returned {@link Path}s
   *
   * @param path an {@linkplain Path#isAbsolute() absolute
   * <code>Path</code>}; must not be null
   *
   * @return the transliterated {@link Path}; never {@code null};
   * possibly the supplied {@code path} itself
   *
   * @exception NullPointerException if {@code path} is {@code null}
   *
   * @exception IllegalArgumentException if {@code path} returns
   * {@code false} from its {@link Path#isAbsolute() isAbsolute()}
   * method
   *
   * @nullability The default implementation of this method does not,
   * and its overrides must not, return {@code null}.
   *
   * @threadsafety The default implementation of this method is, and
   * its overrides must be, safe for concurrent use by multiple
   * threads.
   *
   * @idempotency The default implementation of this method is, and
   * its overrides must be, idempotent and deterministic.
   *
   * @see Path#isAbsolute()
   *
   * @see Path#transliterate(java.util.function.BiFunction)
   *
   * @see #normalize(Path)
   *
   * @see #loaderFor(Path)
   *
   * @see #load(Path)
   */
  @OverridingDiscouraged
  public default <U> Path<U> transliterate(final Path<U> path) {
    if (path.isTransliterated()) {
      return path;
    }
    final Element<U> last = path.last();
    if (last.name().equals("transliterate")) {
      final List<Class<?>> parameters = last.parameters().orElseThrow();
      if (parameters.size() == 1 && parameters.get(0) == Path.class) {
        // Are we in the middle of a transliteration request? Avoid
        // the infinite loop.
        return path;
      }
    }
    return
      this.load(this.absolutePath().plus(Path.of(Element.of("transliterate", // name
                                                          new TypeToken<Path<U>>() {}, // type
                                                          Path.class, // parameter
                                                          path.toString())))) // sole argument
      .orElse(path);
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> Loader<U> load(final Class<U> type) {
    return this.load(Element.of(type));
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> Loader<U> load(final TypeToken<U> type) {
    return this.load(Element.of(type));
  }

  @Convenience
  @OverridingDiscouraged
  public default Loader<?> load(final Type type) {
    return this.load(Element.of(type));
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> Loader<U> load(final String element, final Class<U> type) {
    return this.load(Element.of(element, type));
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> Loader<U> load(final String element, final TypeToken<U> type) {
    return this.load(Element.of(element, type));
  }

  @Convenience
  @OverridingDiscouraged
  public default Loader<?> load(final String element, final Type type) {
    return this.load(Element.of(element, type));
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> Loader<U> load(final List<? extends String> names, final Class<U> type) {
    return this.load(Path.of(names, type));
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> Loader<U> load(final List<? extends String> names, final TypeToken<U> type) {
    return this.load(Path.of(names, type));
  }

  @Convenience
  @OverridingDiscouraged
  public default Loader<?> load(final List<? extends String> names, final Type type) {
    return this.load(Path.of(names, type));
  }

  @Convenience
  @OverridingDiscouraged
  public default <U> Loader<U> load(final Element<U> nonRootElement) {
    if (nonRootElement.isRoot()) {
      throw new IllegalArgumentException("nonRootElement.isRoot(): " + nonRootElement);
    }
    return this.load(Path.of(nonRootElement));
  }

  /**
   * Returns a normalized version of the supplied {@link Path}.
   *
   * <p>The returned {@link Path} must not be {@code null}, must be
   * {@linkplain Path#isAbsolute() absolute} and must have been
   * {@linkplain #transliterate(Path) transliterated}.  An
   * implementation that deviates from these requirements will cause
   * undefined behavior.</p>
   *
   * <p>The default implementation of this method returns the
   * equivalent of {@code this.transliterate(path.isAbsolute() ? path
   * : this.absolutePath().plus(path))}.</p>
   *
   * <p>Implementations of this method must not call {@link
   * #loaderFor(Path)} or an infinite loop may result.</p>
   *
   * @param <U> the type of the supplied and returned {@link Path}s
   *
   * @param path the {@link Path} to normalize; must not be {@code
   * null}
   *
   * @return a normalized, {@linkplain #transliterate(Path)
   * transliterated} {@link Path}
   *
   * @exception NullPointerException if {@code path} is {@code null}
   *
   * @nullability The default implementation of this method does not,
   * and its (discouraged) overrides must not, return {@code null}.
   *
   * @threadsafety The default implementation of this method is, and
   * its (discouraged) overrides must be, safe for concurrent use by
   * multiple threads.
   *
   * @idempotency The default implementation of this method is, and
   * its (discouraged) overrides must be, idempotent and
   * deterministic.
   *
   * @see #transliterate(Path)
   *
   * @see #loaderFor(Path)
   */
  @OverridingDiscouraged
  public default <U> Path<U> normalize(final Path<U> path) {
    if (path.isAbsolute()) {
      if (path.isTransliterated() || path.size() == 1) {
        return path;
      } else {
        return this.transliterate(path);
      }
    } else {
      return this.transliterate(this.absolutePath().plus(path));
    }
  }

  /**
   * Returns a {@link Loader}, derived from this {@link Loader}, that
   * is suitable for a {@linkplain #normalize(Path) normalized
   * version} of the supplied {@code path}, particularly for cases
   * where, during the execution of the {@link #load(Path)} method, a
   * {@link Loader} must be supplied to some other class.
   *
   * <p>The returned {@link Loader} must be one whose {@link
   * #absolutePath()} method returns the {@linkplain Path#size()
   * longest} {@link Path} that is a parent of the ({@linkplain
   * #normalize(Path) normalized}) supplied {@code path}.  In many
   * cases {@code this} will be returned.</p>
   *
   * <p>Typically only classes implementing this interface will need
   * to call this method.</p>
   *
   * @param path the {@link Path} in question; must not be {@code
   * null}
   *
   * @return a {@link Loader}, derived from this {@link Loader}, that
   * is suitable for a {@linkplain #normalize(Path) normalized
   * version} of the supplied {@code path}, particularly for cases
   * where, during the execution of the {@link #load(Path)} method, a
   * {@link Loader} must be supplied to some other class; never {@code
   * null}
   *
   * @exception NullPointerException if {@code path} is {@code null}
   *
   * @nullability The default implementation of this method does not,
   * and its (discouraged) overrides must not, return {@code null}.
   *
   * @threadsafety The default implementation of this method is, and
   * its (discouraged) overrides must be, safe for concurrent use by
   * multiple threads.
   *
   * @idempotency The default implementation of this method is, and
   * its (discouraged) overrides must be, idempotent and
   * deterministic.
   *
   * @see #normalize(Path)
   *
   * @see #transliterate(Path)
   *
   * @see #root()
   */
  @OverridingDiscouraged
  public default Loader<?> loaderFor(Path<?> path) {
    path = this.normalize(path);
    Loader<?> requestor = this;
    final Path<?> requestorPath = requestor.absolutePath();
    assert requestorPath.isAbsolute() : "!requestorPath.isAbsolute(): " + requestorPath;
    if (!requestorPath.equals(path)) {
      if (requestorPath.startsWith(path)) {
        final int requestorPathSize = requestorPath.size();
        final int pathSize = path.size();
        assert requestorPathSize != pathSize;
        if (requestorPathSize > pathSize) {
          for (int i = 0; i < requestorPathSize - pathSize; i++) {
            requestor = requestor.parent();
          }
          assert requestor.absolutePath().equals(path) : "!requestor.absolutePath().equals(path); requestor.absolutePath(): " + requestor.absolutePath() + "; path: " + path;
        } else {
          throw new AssertionError("requestorPath.size() < path.size(); requestorPath: " + requestorPath + "; path: " + path);
        }
      } else {
        requestor = requestor.root();
      }
    }
    return requestor;
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
   * Returns the non-{@code null} {@link Loader} that is at the
   * root of a chain of {@linkplain #parent() parented} {@link
   * Loader} implementations.
   *
   * <p>The returned {@link Loader} must return itself from its
   * {@link #parent()} method or undefined behavior will result.</p>
   *
   * @return the non-{@code null} {@link Loader} that is at the
   * root of a chain of {@linkplain #parent() parented} {@link
   * Loader} implementations
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
  public default Loader<?> root() {
    Loader<?> root = this;
    Loader<?> parent = root.parent();
    while (parent != null && parent != root) {
      // (Strictly speaking, Loader::parent should NEVER be null.)
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
   * Loader} that can be used to acquire configured objects.
   *
   * <p>The {@linkplain #root() root} {@link Loader} is located
   * using the {@link ServiceLoader}.  The first of all discovered
   * {@link Loader} instances is used and all others are ignored.</p>
   *
   * <p>The {@link Loader} that is loaded via this mechanism is
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
   * <li>It must return itself from its {@link #get() get()}
   * method.</li>
   *
   * <li>It must return {@code true} from its {@link #isRoot()}
   * implementation.</li>
   *
   * <li>It must return itself from its {@link #root()} method.</li>
   *
   * </ul>
   *
   * <p>That <em>bootstrap</em> instance is then used to find the
   * "real" {@link Loader} implementation, which in most cases is
   * simply itself.</p>
   *
   * <p>The {@link Loader} that is supplied by the bootstrap
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
   * <li>It must return a {@link Loader} implementation, normally
   * itself, from its {@link #get() get()} method.</li>
   *
   * </ul>
   *
   * <p>This is the entry point for end users of this framework.</p>
   *
   * @return a non-{@code null} {@linkplain #root() root} {@link
   * Loader} that can be used to acquire configured objects
   *
   * @exception IllegalStateException if any of the restrictions above
   * is violated
   */
  @EntryPoint
  @SuppressWarnings("static")
  public static Loader<?> loader() {
    final class RootLoader {
      private static final Loader<?> INSTANCE;
      static {
        final Loader<?> bootstrapLoader =
          ServiceLoader.load(Loader.class, Loader.class.getClassLoader()).findFirst().orElseThrow();
        if (!Path.root().equals(bootstrapLoader.absolutePath())) {
          throw new IllegalStateException("bootstrapLoader.absolutePath(): " + bootstrapLoader.absolutePath());
        } else if (bootstrapLoader.parent() != bootstrapLoader) {
          throw new IllegalStateException("bootstrapLoader.parent(): " + bootstrapLoader.parent());
        } else if (bootstrapLoader.get() != bootstrapLoader) {
          throw new IllegalStateException("bootstrapLoader.get(): " + bootstrapLoader.get());
        } else if (!bootstrapLoader.isRoot()) {
          throw new IllegalStateException("!bootstrapLoader.isRoot()");
        } else if (bootstrapLoader.root() != bootstrapLoader) {
          throw new IllegalStateException("bootstrapLoader.root(): " + bootstrapLoader.root());
        }
        INSTANCE = bootstrapLoader.load(new TypeToken<Loader<?>>() {}).orElse(bootstrapLoader);
        if (!Path.root().equals(INSTANCE.absolutePath())) {
          throw new IllegalStateException("INSTANCE.absolutePath(): " + INSTANCE.absolutePath());
        } else if (INSTANCE.parent() != bootstrapLoader) {
          throw new IllegalStateException("INSTANCE.parent(): " + INSTANCE.parent());
        } else if (!(INSTANCE.get() instanceof Loader)) {
          throw new IllegalStateException("INSTANCE.get(): " + INSTANCE.get());
        }
      }
    };
    return RootLoader.INSTANCE;
  }

}
