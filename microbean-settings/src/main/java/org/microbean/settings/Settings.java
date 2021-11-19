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
import java.util.Optional;
import java.util.ServiceLoader;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.microbean.development.annotation.Experimental;
import org.microbean.development.annotation.SubordinateTo;

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

import org.microbean.type.Types;

/**
 * A subclassable default {@link Configured} implementation
 * that delegates its work to {@link Provider}s.
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

  private final Configured<?> parent;

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
   * @deprecated This constructor should be invoked by subclasses and
   * {@link ServiceLoader java.util.ServiceLoader} instances only.
   *
   * @see Configured#of()
   */
  @Deprecated // intended for use by ServiceLoader only
  public Settings() {
    this(new ConcurrentHashMap<Qualified<Path<?>>, Settings<?>>(),
         null, // providers
         null, // qualifiers
         null, // parent,
         null, // absolutePath
         null, // supplier
         null);
  }

  private Settings(final ConcurrentMap<Qualified<Path<?>>, Settings<?>> settingsCache,
                   final Collection<? extends Provider> providers,
                   final Qualifiers qualifiers,
                   final Configured<?> parent, // if null, will end up being "this" if absolutePath is Path.root()
                   final Path<T> absolutePath,
                   final Supplier<T> supplier, // if null, will end up being () -> this if absolutePath is Path.root()
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

  @Override // Configured
  public final Qualifiers qualifiers() {
    // NOTE: This null check is critical.  We check for null here
    // because during bootstrapping the qualifiers will not have been
    // loaded yet, and yet the bootstrapping mechanism may still end
    // up calling this.qualifiers().  The alternative would be to make
    // the qualifiers field non-final and I don't want to do that.
    final Qualifiers qualifiers = this.qualifiers;
    return qualifiers == null ? Qualifiers.of() : qualifiers;
  }

  @Override // Configured
  @SuppressWarnings("unchecked")
  public final <P> Configured<P> parent() {
    return (Configured<P>)this.parent;
  }

  @Override // Configured
  public final Path<T> absolutePath() {
    return this.absolutePath;
  }

  @Override // Configured
  public final T get() {
    return this.supplier.get();
  }

  @Override // Configured
  public final <U> Settings<U> of(Path<U> path) {
    path = this.normalize(path);
    assert path.isAbsolute() : "!path.isAbsolute(): " + path;
    if (path.isRoot()) {
      throw new IllegalArgumentException("path.isRoot(): " + path);
    }

    final Configured<?> requestor = this.configuredFor(path);

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
    final Qualified<Path<?>> qp = new Qualified<>(requestor.qualifiers(), path);
    Settings<?> settings = this.settingsCache.get(qp);
    if (settings == null) {
      settings = this.settingsCache.putIfAbsent(qp, this.computeSettings(requestor, path));
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

  @Experimental
  @Override
  public final <U> Path<U> transliterate(final Path<U> path) {
    if (path.isTransliterated()) {
      return path;
    }
    final TypeToken<Path<U>> typeToken = new TypeToken<Path<U>>() {};
    if (path.type() == typeToken.type()) {
      final Element<U> a = path.last();
      if (a != null && a.name().equals("transliterate")) {
        final List<Class<?>> parameters = a.parameters().orElse(null);
        if (parameters.size() == 1 && parameters.get(0) == Path.class) {
          // Are we in the middle of a transliteration request? Avoid
          // the infinite loop.
          return path;
        }
      }
    }
    final Configured<Path<U>> configured =
      this.of(Element.<Path<U>>of("transliterate", // name
                                  typeToken,
                                  Path.class, // parameter
                                  path.toString())); // sole argument
    return configured.orElse(path);
  }

  private final <U> Settings<U> computeSettings(final Configured<?> requestor, final Path<U> absolutePath) {
    assert absolutePath.isAbsolute() : "absolutePath: " + absolutePath;
    final Value<U> value = this.value(requestor, absolutePath);
    final Supplier<U> supplier;
    if (value == null) {
      supplier = Settings::throwNoSuchElementException;
    } else {
      supplier = value;
    }
    return
      new Settings<>(this.settingsCache,
                     this.providers(),
                     requestor.qualifiers(),
                     requestor,
                     absolutePath,
                     supplier,
                     this.ambiguityHandler());
  }

  private final <U> Value<U> value(final Configured<?> requestor, final Path<U> absolutePath) {
    assert absolutePath.isAbsolute() : "absolutePath: " + absolutePath;

    final Collection<? extends Provider> providers = this.providers();
    if (providers.isEmpty()) {
      return null;
    }

    final AmbiguityHandler ambiguityHandler = this.ambiguityHandler();

    if (providers.size() == 1) {
      final Provider provider = providers instanceof List<? extends Provider> list ? list.get(0) : providers.iterator().next();
      if (provider == null) {
        return null;
      } else if (!this.isSelectable(provider, requestor, absolutePath)) {
        ambiguityHandler.providerRejected(requestor, absolutePath, provider);
        return null;
      } else {
        final Value<U> value = provider.get(requestor, absolutePath);
        if (value == null) {
          ambiguityHandler.providerRejected(requestor, absolutePath, provider);
        } else if (!isSelectable(requestor.qualifiers(), absolutePath, value.qualifiers(), value.path())) {
          ambiguityHandler.valueRejected(requestor, absolutePath, provider, value);
        }
        return value;
      }
    }

    final Qualifiers qualifiers = requestor.qualifiers();

    Value<U> candidate = null;
    Provider candidateProvider = null;

    int candidateQualifiersScore = Integer.MIN_VALUE;
    int candidatePathScore = Integer.MIN_VALUE;

    PROVIDER_LOOP:
    for (final Provider provider : providers) {

      if (provider == null || !this.isSelectable(provider, requestor, absolutePath)) {
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
          candidateQualifiersScore = this.score(qualifiers, candidate.qualifiers());
          candidatePathScore = this.score(absolutePath, candidate.path());
          break VALUE_EVALUATION_LOOP;
        }

        // Let's do qualifiers first.  This is an arbitrary decision.
        final int valueQualifiersScore = this.score(qualifiers, value.qualifiers());
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

        // Same qualifiers score; let's now do paths.
        final int valuePathScore = this.score(absolutePath, value.path());

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
          // Couldn't disambiguate.
          //
          // TODO: I'm not sure whether to null the candidate bits and
          // potentially grab another less suitable one, keep the
          // existing one even though it's ambiguous, or, if we keep
          // it, to break or continue.  For now I'm going to keep it
          // and continue; the caller can examine whatever ended up in
          // the ambiguous values consumer and figure out what it
          // wants to do.
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
    return candidate;
  }

  protected final boolean isSelectable(final Provider provider,
                                       final Configured<?> supplier,
                                       final Path<?> absolutePath) {
    if (!absolutePath.isAbsolute()) {
      throw new IllegalArgumentException("absolutePath: " + absolutePath);
    }
    return
      AssignableType.of(provider.upperBound()).isAssignable(absolutePath.type()) &&
      provider.isSelectable(supplier, absolutePath);
  }

  @SubordinateTo("#of(Configured, Path, Supplier)")
  protected int score(final Qualifiers referenceQualifiers, final Qualifiers valueQualifiers) {
    final int intersectionSize = referenceQualifiers.intersectionSize(valueQualifiers);
    if (intersectionSize > 0) {
      return
        intersectionSize == valueQualifiers.size() ?
        intersectionSize :
        intersectionSize - referenceQualifiers.symmetricDifferenceSize(valueQualifiers);
    } else {
      return -(referenceQualifiers.size() + valueQualifiers.size());
    }
  }

  /**
   * Returns a score indicating the relative specificity of {@code
   * valuePath} with respect to {@code absoluteReferencePath}.
   *
   * <p>This is <em>not</em> a comparison method.</p>
   *
   * <p>The following preconditions must hold or undefined behavior
   * will result:</p>
   *
   * <ul>
   *
   * <li>Neither parameter's value may be {@code null}.</li>
   *
   * <li>{@code absoluteReferencePath} must be {@linkplain
   * Path#isAbsolute() absolute}
   *
   * <li>{@code valuePath} must be selectable with respect to {@code
   * absoluteReferencePath}, where the definition of selectability is
   * described below</li>
   *
   * </ul>
   *
   * <p>For {@code valuePath} to "be selectable" with respect to
   * {@code absoluteReferencePath} for the purposes of this method and
   * for no other purpose, a hypothetical invocation of {@link
   * #isSelectable(Path, Path)} must return {@code true} when supplied
   * with {@code absoluteReferencePath} and {@code valuePath}
   * respectively.  Note that such an invocation is <em>not</em> made
   * by this method, but logically precedes it when this method is
   * called in the natural course of events by the {@link
   * #of(Path)} method.</p>
   *
   * @param absoluteReferencePath the {@link Path} against which to
   * score the supplied {@code valuePath}; must not be {@code null};
   * must adhere to the preconditions above
   *
   * @param valuePath the {@link Path} to score against the supplied
   * {@code absoluteReferencePath}; must not be {@code null}; must
   * adhere to the preconditions above
   *
   * @return a relative score for {@code valuePath} with respect to
   * {@code absoluteReferencePath}; meaningless on its own
   *
   * @exception NullPointerException if either parameter is {@code
   * null}
   *
   * @exception IllegalArgumentException if certain preconditions have
   * been violated
   *
   * @see #of(Path)
   *
   * @see #isSelectable(Path, Path)
   *
   * @threadsafety This method is and its overrides must be safe for
   * concurrent use by multiple threads.
   *
   * @idempotency This method is idempotent and deterministic.
   * Specifically, the same score is returned whenever this method is
   * invoked with the same paths.  Overrides must preserve this
   * property.
   */
  @SubordinateTo("#of(Configured, Path, Supplier)")
  protected int score(final Path<?> absoluteReferencePath, final Path<?> valuePath) {
    if (!absoluteReferencePath.isAbsolute()) {
      throw new IllegalArgumentException("absoluteReferencePath: " + absoluteReferencePath);
    }

    final int lastValuePathIndex = absoluteReferencePath.lastIndexOf(valuePath, ElementsMatchBiPredicate.INSTANCE);
    assert lastValuePathIndex >= 0 : "absoluteReferencePath: " + absoluteReferencePath + "; valuePath: " + valuePath;
    assert lastValuePathIndex + valuePath.size() == absoluteReferencePath.size() : "absoluteReferencePath: " + absoluteReferencePath + "; valuePath: " + valuePath;

    int score = valuePath.size();
    for (int valuePathIndex = 0; valuePathIndex < valuePath.size(); valuePathIndex++) {
      final int referencePathIndex = lastValuePathIndex + valuePathIndex;

      final Element<?> referenceElement = absoluteReferencePath.get(referencePathIndex);
      final Element<?> valueElement = valuePath.get(valuePathIndex);
      if (!referenceElement.name().equals(valueElement.name())) {
        return Integer.MIN_VALUE;
      }

      final Type referenceType = referenceElement.type().orElse(null);
      final Type valueType = valueElement.type().orElse(null);
      if (referenceType == null) {
        if (valueType != null) {
          return Integer.MIN_VALUE;
        }
      } else if (valueType == null) {
        return Integer.MIN_VALUE;
      } else if (referenceType.equals(valueType)) {
        ++score;
      } else if (!AssignableType.of(referenceType).isAssignable(valueType)) {
        return Integer.MIN_VALUE;
      }

      final List<Class<?>> referenceParameters = referenceElement.parameters().orElse(null);
      final List<Class<?>> valueParameters = valueElement.parameters().orElse(null);
      if (referenceParameters == null) {
        if (valueParameters != null) {
          return Integer.MIN_VALUE;
        }
      } else if (valueParameters == null || referenceParameters.size() != valueParameters.size()) {
        return Integer.MIN_VALUE;
      }

      final List<String> referenceArguments = referenceElement.arguments().orElse(null);
      final List<String> valueArguments = valueElement.arguments().orElse(null);
      if (referenceArguments == null) {
        if (valueArguments != null) {
          return Integer.MIN_VALUE;
        }
      } else if (valueArguments == null) {
        // The value is indifferent with respect to arguments. It
        // *could* be suitable but not *as* suitable as one that
        // matched.  Don't adjust the score.
      } else {
        final int referenceArgsSize = referenceArguments.size();
        final int valueArgsSize = valueArguments.size();
        if (referenceArgsSize < valueArgsSize) {
          // The value path is unsuitable because it provided too
          // many arguments.
          return Integer.MIN_VALUE;
        } else if (referenceArguments.equals(valueArguments)) {
          score += referenceArgsSize;
        } else if (referenceArgsSize == valueArgsSize) {
          // Same sizes, but different arguments.  The value is not suitable.
          return Integer.MIN_VALUE;
        } else if (valueArgsSize == 0) {
          // The value is indifferent with respect to arguments. It
          // *could* be suitable but not *as* suitable as one that
          // matched.  Don't adjust the score.
        } else {
          // The reference element had, say, two arguments, and the
          // value had, say, one.  We treat this as a mismatch.
          return Integer.MIN_VALUE;
        }
      }
    }
    return score;
  }

  @SuppressWarnings("unchecked")
  private final <X> X returnThis() {
    return (X)this;
  }

  /*
   * Static methods.
   */


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

  protected static final boolean isSelectable(final Qualifiers referenceQualifiers,
                                              final Qualifiers valueQualifiers) {
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
  protected static final boolean isSelectable(final Path<?> absoluteReferencePath,
                                              final Path<?> valuePath) {
    if (!absoluteReferencePath.isAbsolute()) {
      throw new IllegalArgumentException("absoluteReferencePath: " + absoluteReferencePath);
    }
    return absoluteReferencePath.endsWith(valuePath, ElementsMatchBiPredicate.INSTANCE);
  }

  private static final <T> T throwNoSuchElementException() {
    throw new NoSuchElementException();
  }

  private static final void sink(final Object ignored) {

  }

  private static final Consumer<Object> generateSink() {
    return Settings::sink;
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
