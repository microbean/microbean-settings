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
package org.microbean.settings;

import java.lang.reflect.Type;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.ServiceLoader;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.function.BiPredicate;
import java.util.function.Supplier;

import org.microbean.development.annotation.Experimental;

import org.microbean.settings.api.Configured;
import org.microbean.settings.api.Path;
import org.microbean.settings.api.Path.Element;
import org.microbean.settings.api.Qualified;
import org.microbean.settings.api.Qualifiers;
import org.microbean.settings.api.TypeToken;

import org.microbean.settings.provider.AmbiguityHandler;
import org.microbean.settings.provider.AssignableType;
import org.microbean.settings.provider.Provider;
import org.microbean.settings.provider.Value;

/**
 * A subclassable default {@link Configured} implementation that
 * delegates its work to {@link Provider}s and an {@link
 * #ambiguityHandler() AmbiguityHandler}.
 *
 * @param <T> the type of configured objects this {@link Settings}
 * supplies
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see Configured
 *
 * @see Provider
 */
public class Settings<T> implements AutoCloseable, Configured<T> {


  /*
   * Instance fields.
   */


  // Package-private for testing only.
  final ConcurrentMap<Qualified<Path<?>>, Settings<?>> settingsCache;

  private final Path<T> absolutePath;

  private final Settings<?> parent;

  private final Supplier<T> supplier;

  private final Collection<Provider> providers;

  private final Qualifiers qualifiers;

  private final AmbiguityHandler ambiguityHandler;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link Settings}.
   *
   * @see Configured#of()
   *
   * @deprecated This constructor should be invoked by subclasses and
   * {@link ServiceLoader java.util.ServiceLoader} instances only.
   */
  @Deprecated // intended for use by subclasses and java.util.ServiceLoader only
  public Settings() {
    this(new ConcurrentHashMap<Qualified<Path<?>>, Settings<?>>(),
         null, // providers
         null, // Qualifiers
         null, // parent,
         null, // absolutePath
         null, // Supplier
         null); // AmbiguityHandler
  }

  private Settings(final ConcurrentMap<Qualified<Path<?>>, Settings<?>> settingsCache,
                   final Collection<? extends Provider> providers,
                   final Qualifiers qualifiers,
                   final Settings<?> parent, // if null, will end up being "this" if absolutePath is null or Path.root()
                   final Path<T> absolutePath,
                   final Supplier<T> supplier, // if null, will end up being () -> this if absolutePath is null or Path.root()
                   final AmbiguityHandler ambiguityHandler) {
    super();
    this.settingsCache = Objects.requireNonNull(settingsCache, "settingsCache");
    if (parent == null) {
      // Bootstrap case, i.e. the zero-argument constructor called us.
      // Pay attention.
      if (absolutePath == null || absolutePath.equals(Path.root())) {
        @SuppressWarnings("unchecked")
        final Path<T> p = (Path<T>)Path.root();
        this.absolutePath = p;
        this.parent = this; // NOTE
        this.supplier = supplier == null ? this::returnThis : supplier; // NOTE
        this.providers = List.copyOf(providers == null ? loadedProviders() : providers);
        final Qualified<Path<?>> qp = new Qualified<>(Qualifiers.of(), Path.root());
        this.settingsCache.put(qp, this); // NOTE
        // While the following call is in effect, our
        // final-but-as-yet-uninitialized qualifiers field and our
        // final-but-as-yet-uninitialized ambiguityHandler field will
        // both be null.  Note that the qualifiers() instance method
        // accounts for this and will return Qualifiers.of() instead,
        // and the ambiguityHandler() instance method does as well.
        try {
          this.qualifiers = this.of(Qualifiers.class).orElseGet(Qualifiers::of);
          this.ambiguityHandler = this.of(AmbiguityHandler.class).orElseGet(Settings::loadedAmbiguityHandler);
        } finally {
          this.settingsCache.remove(qp);
        }
      } else {
        throw new IllegalArgumentException("!absolutePath.equals(Path.root()): " + absolutePath);
      }
    } else if (absolutePath.equals(Path.root())) {
      throw new IllegalArgumentException("absolutePath.equals(Path.root()): " + absolutePath);
    } else if (!absolutePath.isAbsolute()) {
      throw new IllegalArgumentException("!absolutePath.isAbsolute(): " + absolutePath);
    } else if (!parent.absolutePath().isAbsolute()) {
      throw new IllegalArgumentException("!parent.absolutePath().isAbsolute(): " + parent.absolutePath());
    } else {
      this.absolutePath = absolutePath;
      this.parent = parent;
      this.supplier = Objects.requireNonNull(supplier, "supplier");
      this.providers = List.copyOf(providers);
      this.qualifiers = Objects.requireNonNull(qualifiers, "qualifiers");
      this.ambiguityHandler = Objects.requireNonNull(ambiguityHandler, "ambiguityHandler");
    }
  }


  /*
   * Instance methods.
   */


  /**
   * Clears any caches used by this {@link Settings}.
   *
   * <p>This {@link Settings} remains valid to use.</p>
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is deterministic but not idempotent
   * unless the caches are already cleared.
   */
  @Experimental
  @Override
  public final void close() {
    this.settingsCache.clear();
  }

  /**
   * Returns an {@linkplain
   * java.util.Collections#unmodifiableCollection(Collection)
   * unmodifiable} {@link Collection} of {@link Provider}s that this
   * {@link Settings} will use to supply objects.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @return an {@linkplain
   * java.util.Collections#unmodifiableCollection(Collection)
   * unmodifiable} {@link Collection} of {@link Provider}s that this
   * {@link Settings} will use to supply objects; never {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   */
  public final Collection<Provider> providers() {
    return this.providers;
  }

  /**
   * Returns the {@link AmbiguityHandler} associated with this {@link
   * Settings}.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @return the {@link AmbiguityHandler} associated with this {@link
   * Settings}; never {@code null}
   *
   * @nullability This method never returns {@code null}
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   *
   * @see AmbiguityHandler
   */
  public final AmbiguityHandler ambiguityHandler() {
    // NOTE: This null check is critical.  We check for null here
    // because during bootstrapping the AmbiguityHandler will not have
    // been loaded yet, and yet the bootstrapping mechanism may still
    // end up calling this.ambiguityHandler().  The alternative would
    // be to make the ambiguityHandler field non-final and I don't
    // want to do that.
    final AmbiguityHandler ambiguityHandler = this.ambiguityHandler;
    return ambiguityHandler == null ? NoOpAmbiguityHandler.INSTANCE : ambiguityHandler;
  }

  /**
   * Returns the {@link Qualifiers} with which this {@link Settings}
   * is associated.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @return the {@link Qualifiers} with which this {@link Settings}
   * is associated
   *
   * @nullability This method never returns {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   */
  @Override // Configured<T>
  public final Qualifiers qualifiers() {
    // NOTE: This null check is critical.  We check for null here
    // because during bootstrapping the qualifiers will not have been
    // loaded yet, and yet the bootstrapping mechanism may still end
    // up calling this.qualifiers().  The alternative would be to make
    // the qualifiers field non-final and I don't want to do that.
    final Qualifiers qualifiers = this.qualifiers;
    return qualifiers == null ? Qualifiers.of() : qualifiers;
  }

  /**
   * Returns the {@link Settings} serving as the parent of this
   * {@link Settings}.
   *
   * <p>The "root" {@link Settings} returns itself from its {@link
   * #parent()} implementation.</p>
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @return the non-{@code null} {@link Settings} serving as the
   * parent of this {@link Settings}; may be this {@link Settings}
   * itself
   *
   * @nullability This method never returns {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   */
  // Note that the root will have itself as its parent.
  @Override // Configured<T>
  public final Settings<?> parent() {
    return this.parent;
  }

  @Override // Configured<T>
  public final Path<T> absolutePath() {
    return this.absolutePath;
  }

  @Override // Configured<T>
  public final T get() {
    return this.supplier.get();
  }

  @Override // Configured<T>
  public final Settings<?> configuredFor(Path<?> path) {
    return (Settings<?>)Configured.super.configuredFor(path);
  }

  @Override // Configured<T>
  public final <U> Settings<U> of(final Path<U> path) {
    final Path<U> absolutePath = this.normalize(path);
    assert absolutePath.isAbsolute() : "!normalize(path).isAbsolute(): " + absolutePath;
    if (absolutePath.isRoot()) {
      throw new IllegalArgumentException("normalize(path).isRoot(): " + absolutePath);
    }

    final Settings<?> requestor = this.configuredFor(absolutePath);

    // We deliberately do not use computeIfAbsent() because of()
    // operations can kick off other of() operations, and then you'd
    // have a cache mutating operation occuring within a cache
    // mutating operation, which is forbidden.  Sometimes you get an
    // IllegalStateException as you are supposed to; other times you
    // do not, which is a JDK bug.  See
    // https://blog.jooq.org/avoid-recursion-in-concurrenthashmap-computeifabsent/.
    //
    // This obviously can result in unnecessary work, but most
    // configuration use cases will cause this work to happen anyway.
    final Qualified<Path<?>> qp = new Qualified<>(requestor.qualifiers(), absolutePath);
    Settings<?> settings = this.settingsCache.get(qp);
    if (settings == null) {
      settings = this.settingsCache.putIfAbsent(qp, this.computeSettings(requestor, absolutePath));
      if (settings == null) {
        settings = this.settingsCache.get(qp);
      }
    }
    assert settings != null;
    assert settings == this.settingsCache.get(qp);
    @SuppressWarnings("unchecked")
    final Settings<U> returnValue = (Settings<U>)settings;
    return returnValue;
  }

  private final <U> Settings<U> computeSettings(final Settings<?> requestor, final Path<U> absolutePath) {
    assert absolutePath.isAbsolute() : "absolutePath: " + absolutePath;
    final Qualifiers qualifiers = requestor.qualifiers();
    final AmbiguityHandler ambiguityHandler = requestor.ambiguityHandler();
    Value<U> candidate = null;
    final Collection<? extends Provider> providers = this.providers();
    if (!providers.isEmpty()) {
      Provider candidateProvider = null;
      if (providers.size() == 1) {

        candidateProvider = providers instanceof List<? extends Provider> list ? list.get(0) : providers.iterator().next();
        if (candidateProvider == null || !isSelectable(candidateProvider, absolutePath)) {
          ambiguityHandler.providerRejected(requestor, absolutePath, candidateProvider);
        } else {
          candidate = candidateProvider.get(requestor, absolutePath);
          if (candidate == null) {
            ambiguityHandler.providerRejected(requestor, absolutePath, candidateProvider);
          } else if (!isSelectable(qualifiers, absolutePath, candidate.qualifiers(), candidate.path())) {
            ambiguityHandler.valueRejected(requestor, absolutePath, candidateProvider, candidate);
          }
        }

      } else {
        int candidateQualifiersScore = Integer.MIN_VALUE;
        int candidatePathScore = Integer.MIN_VALUE;

        PROVIDER_LOOP:
        for (final Provider provider : providers) {

          if (provider == null || !isSelectable(provider, absolutePath)) {
            ambiguityHandler.providerRejected(requestor, absolutePath, provider);
            continue PROVIDER_LOOP;
          }

          Value<U> value = provider.get(requestor, absolutePath);

          if (value == null) {
            ambiguityHandler.providerRejected(requestor, absolutePath, provider);
            continue PROVIDER_LOOP;
          }

          // NOTE: INFINITE LOOP POSSIBILITY; read carefully!
          VALUE_EVALUATION_LOOP:
          while (true) {

            if (!isSelectable(qualifiers, absolutePath, value.qualifiers(), value.path())) {
              ambiguityHandler.valueRejected(requestor, absolutePath, provider, value);
              break VALUE_EVALUATION_LOOP;
            }

            if (candidate == null) {
              candidate = value;
              candidateProvider = provider;
              candidateQualifiersScore = ambiguityHandler.score(qualifiers, candidate.qualifiers());
              candidatePathScore = ambiguityHandler.score(absolutePath, candidate.path());
              break VALUE_EVALUATION_LOOP;
            }

            // Let's score Qualifiers first, not paths.  This is an
            // arbitrary decision.
            final int valueQualifiersScore = ambiguityHandler.score(qualifiers, value.qualifiers());
            if (valueQualifiersScore < candidateQualifiersScore) {
              candidate = new Value<>(value, candidate);
              break VALUE_EVALUATION_LOOP;
            }

            if (valueQualifiersScore > candidateQualifiersScore) {
              candidate = new Value<>(candidate, value);
              candidateProvider = provider;
              candidateQualifiersScore = valueQualifiersScore;
              // (No need to update candidatePathScore.)
              break VALUE_EVALUATION_LOOP;
            }

            // The Qualifiers scores were equal.  Let's do paths.
            final int valuePathScore = ambiguityHandler.score(absolutePath, value.path());

            if (valuePathScore < candidatePathScore) {
              candidate = new Value<>(value, candidate);
              break VALUE_EVALUATION_LOOP;
            }

            if (valuePathScore > candidatePathScore) {
              candidate = new Value<>(candidate, value);
              candidateProvider = provider;
              candidateQualifiersScore = valueQualifiersScore;
              candidatePathScore = valuePathScore;
              break VALUE_EVALUATION_LOOP;
            }

            final Value<U> disambiguatedValue =
              ambiguityHandler.disambiguate(requestor, absolutePath, candidateProvider, candidate, provider, value);

            if (disambiguatedValue == null) {
              // Couldn't disambiguate.  Drop both values.
              break VALUE_EVALUATION_LOOP;
            }

            if (disambiguatedValue.equals(candidate)) {
              candidate = new Value<>(value, candidate);
              break VALUE_EVALUATION_LOOP;
            }

            if (disambiguatedValue.equals(value)) {
              candidate = new Value<>(candidate, disambiguatedValue);
              candidateProvider = provider;
              candidateQualifiersScore = valueQualifiersScore;
              candidatePathScore = valuePathScore;
              break VALUE_EVALUATION_LOOP;
            }

            // Disambiguation came up with an entirely different value, so
            // run it back through the while loop.
            value = disambiguatedValue;
            continue VALUE_EVALUATION_LOOP;

          }
        }
      }
    }
    return
      new Settings<>(this.settingsCache,
                     providers,
                     qualifiers,
                     requestor, // parent
                     absolutePath,
                     candidate == null ? Settings::throwNoSuchElementException : candidate,
                     ambiguityHandler);
  }

  @SuppressWarnings("unchecked")
  private final <X> X returnThis() {
    return (X)this;
  }


  /*
   * Static methods.
   */

  
  private static final boolean isSelectable(final Provider provider, final Path<?> absolutePath) {
    if (!absolutePath.isAbsolute()) {
      throw new IllegalArgumentException("absolutePath: " + absolutePath);
    }
    return
      AssignableType.of(provider.upperBound()).isAssignable(absolutePath.type());
  }

  static final Collection<Provider> loadedProviders() {
    return Loaded.providers;
  }

  private static final AmbiguityHandler loadedAmbiguityHandler() {
    return Loaded.ambiguityHandler;
  }

  private static final boolean isSelectable(final Qualifiers referenceQualifiers,
                                            final Path<?> absoluteReferencePath,
                                            final Qualifiers valueQualifiers,
                                            final Path<?> valuePath) {
    return isSelectable(referenceQualifiers, valueQualifiers) && isSelectable(absoluteReferencePath, valuePath);
  }

  private static final boolean isSelectable(final Qualifiers referenceQualifiers, final Qualifiers valueQualifiers) {
    return referenceQualifiers.isEmpty() || valueQualifiers.isEmpty() || referenceQualifiers.intersectionSize(valueQualifiers) > 0;
  }

  /**
   * Returns {@code true} if the supplied {@code valuePath} is
   * <em>selectable</em> (for further consideration and scoring) with
   * respect to the supplied {@code absoluteReferencePath}.
   *
   * <p>This method calls {@link Path#endsWith(Path, BiPredicate)} on
   * the supplied {@code absoluteReferencePath} with {@code valuePath}
   * as its {@link Path}-typed first argument, and a {@link
   * BiPredicate} that returns {@code true} if and only if all of the
   * following conditions are true:</p>
   *
   * <ul>
   *
   * <li>Each {@link Element} has a {@linkplain Element#name()
   * name} that is either {@linkplain String#isEmpty() empty} or equal
   * to the other's.</li>
   *
   * <li>Either {@link Element} has a {@link Element#type() Type}
   * that is {@code null}, or the first {@link Element}'s {@link
   * Element#type() Type} {@link AssignableType#of(Type) is
   * assignable from} the second's.</li>
   *
   * <li>Either {@link Element} has {@code null} {@linkplain
   * Element#parameters() parameters} or each of the first {@link
   * Element}'s {@linkplain Element#parameters() parameters}
   * {@linkplain Class#isAssignableFrom(Class) is assignable from} the
   * second's corresponding parameter.</li>
   *
   * </ul>
   *
   * <p>In all other cases this method returns {@code false} or throws
   * an exception.</p>
   *
   * @param absoluteReferencePath the reference path; must not be
   * {@code null}; must be {@linkplain Path#isAbsolute() absolute}
   *
   * @param valuePath the {@link Path} to test; must not be {@code
   * null}
   *
   * @return {@code true} if {@code valuePath} is selectable (for
   * further consideration and scoring) with respect to {@code
   * absoluteReferencePath}; {@code false} in all other cases
   *
   * @exception NullPointerException if either parameter is {@code
   * null}
   *
   * @exception IllegalArgumentException if {@code
   * absoluteReferencePath} {@linkplain Path#isAbsolute() is not
   * absolute}
   */
  private static final boolean isSelectable(final Path<?> absoluteReferencePath, final Path<?> valuePath) {
    if (!absoluteReferencePath.isAbsolute()) {
      throw new IllegalArgumentException("absoluteReferencePath: " + absoluteReferencePath);
    }
    return absoluteReferencePath.endsWith(valuePath, ElementsMatchBiPredicate.INSTANCE);
  }

  private static final <T> T throwNoSuchElementException() {
    throw new NoSuchElementException();
  }


  /*
   * Inner and nested classes.
   */


  private static final class Loaded {

    private static final List<Provider> providers =
      ServiceLoader.load(Provider.class, Provider.class.getClassLoader())
      .stream()
      .map(ServiceLoader.Provider::get)
      .toList();

    private static final AmbiguityHandler ambiguityHandler =
      ServiceLoader.load(AmbiguityHandler.class, AmbiguityHandler.class.getClassLoader())
      .stream()
      .map(ServiceLoader.Provider::get)
      .findFirst()
      .orElse(NoOpAmbiguityHandler.INSTANCE);

  }

  private static final class NoOpAmbiguityHandler implements AmbiguityHandler {

    private static final NoOpAmbiguityHandler INSTANCE = new NoOpAmbiguityHandler();

    private NoOpAmbiguityHandler() {
      super();
    }

  }

  // Matches element names (equality), parameter types
  // (isAssignableFrom) and Types (AssignableType.isAssignable()).
  // Argument values themselves are deliberately ignored.
  private static final class ElementsMatchBiPredicate implements BiPredicate<Element<?>, Element<?>> {

    private static final ElementsMatchBiPredicate INSTANCE = new ElementsMatchBiPredicate();

    private ElementsMatchBiPredicate() {
      super();
    }

    @Override // BiPredicate<Element<?>, Element<?>>
    public final boolean test(final Element<?> e1, final Element<?> e2) {
      final String name1 = e1.name();
      final String name2 = e2.name();
      if (!name1.isEmpty() && !name2.isEmpty() && !name1.equals(name2)) {
        // Empty names have special significance in that they "match"
        // any other name.
        return false;
      }

      final Type t1 = e1.type().orElse(null);
      final Type t2 = e2.type().orElse(null);
      if (t1 != null && t2 != null && !AssignableType.of(t1).isAssignable(t2)) {
        return false;
      }

      final List<Class<?>> p1 = e1.parameters().orElse(null);
      final List<Class<?>> p2 = e2.parameters().orElse(null);
      if (p1 != null && p2 != null) {
        if (p1.size() != p2.size()) {
          return false;
        } else {
          for (int i = 0; i < p1.size(); i++) {
            if (!p1.get(i).isAssignableFrom(p2.get(i))) {
              return false;
            }
          }
        }
      }

      return true;
    }

  }

}
