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

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.microbean.development.annotation.Experimental;
import org.microbean.development.annotation.SubordinateTo;

import org.microbean.type.Types;

/**
 * A subclassable default {@link ConfiguredSupplier} implementation
 * that delegates its work to {@link Provider}s.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see ConfiguredSupplier
 *
 * @see Provider
 */
public class Settings<T> implements AutoCloseable, ConfiguredSupplier<T> {


  /*
   * Instance fields.
   */


  // Package-private for testing only.
  final ConcurrentMap<Qualified.Record<Path2>, Settings<?>> settingsCache;

  private final List<Provider> providers;

  private final Qualifiers qualifiers;

  private final ConfiguredSupplier<?> parent;

  private final Supplier<T> supplier;

  private final Path2 path;

  private final Supplier<? extends Consumer<? super Provider>> rejectedProvidersConsumerSupplier;

  private final Supplier<? extends Consumer<? super Value<?>>> rejectedValuesConsumerSupplier;

  private final Supplier<? extends Consumer<? super Value<?>>> ambiguousValuesConsumerSupplier;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link Settings}.
   *
   * @deprecated This constructor should be invoked by subclasses and
   * {@link ServiceLoader java.util.ServiceLoader} instances only.
   *
   * @see ConfiguredSupplier#of()
   */
  @Deprecated // intended for use by ServiceLoader only
  public Settings() {
    this(new ConcurrentHashMap<Qualified.Record<Path2>, Settings<?>>(),
         loadedProviders(),
         null, // qualifiers
         null, // parent,
         Path2.root(),
         null, // supplier
         Settings::generateSink,
         Settings::generateSink,
         Settings::generateSink);
  }

  @SuppressWarnings("unchecked")
  private Settings(final ConcurrentMap<Qualified.Record<Path2>, Settings<?>> settingsCache,
                   final Collection<? extends Provider> providers,
                   final Qualifiers qualifiers,
                   final ConfiguredSupplier<?> parent, // if null, will end up being "this" if path is Path2.root()
                   final Path2 path,
                   final Supplier<T> supplier, // if null, will end up being () -> this if path is Path2.root()
                   final Supplier<? extends Consumer<? super Provider>> rejectedProvidersConsumerSupplier,
                   final Supplier<? extends Consumer<? super Value<?>>> rejectedValuesConsumerSupplier,
                   final Supplier<? extends Consumer<? super Value<?>>> ambiguousValuesConsumerSupplier) {
    super();
    this.settingsCache = Objects.requireNonNull(settingsCache, "settingsCache");
    this.rejectedProvidersConsumerSupplier = Objects.requireNonNull(rejectedProvidersConsumerSupplier, "rejectedProvidersConsumerSupplier");
    this.rejectedValuesConsumerSupplier = Objects.requireNonNull(rejectedValuesConsumerSupplier, "rejectedValuesConsumerSupplier");
    this.ambiguousValuesConsumerSupplier = Objects.requireNonNull(ambiguousValuesConsumerSupplier, "ambiguousValuesConsumerSupplier");
    this.providers = List.copyOf(providers);
    if (parent == null) {
      // Bootstrap case, i.e. the zero-argument constructor called us.
      // Pay attention.
      if (path.equals(Path2.root())) {
        this.path = Path2.root();
        this.supplier = supplier == null ? () -> (T)this : supplier; // NOTE
        this.parent = this; // NOTE
        final Qualified.Record<Path2> qp = Qualified.Record.of(Qualifiers.of(), Path2.root());
        this.settingsCache.put(qp, this); // NOTE
        // While the following call is in effect, our
        // final-but-as-yet-uninitialized qualifiers instance field will
        // be null.  Note that the qualifiers() accessor method accounts
        // for this and will return Qualifiers.of() instead.
        try {
          this.qualifiers = this.of(Qualifiers.class).orElseGet(Qualifiers::of);
        } finally {
          this.settingsCache.remove(qp);
        }
      } else {
        throw new IllegalArgumentException("path: " + path);
      }
    } else if (path.equals(Path2.root())) {
      throw new IllegalArgumentException("path: " + path);
    } else {
      this.path = path;
      this.supplier = Objects.requireNonNull(supplier, "supplier");
      this.parent = parent;
      this.qualifiers = Objects.requireNonNull(qualifiers, "qualifiers");
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

  @Override // ConfiguredSupplier
  public final Qualifiers qualifiers() {
    // NOTE: This null check is critical.  We check for null here
    // because during bootstrapping the qualifiers will not have been
    // loaded yet, and yet the bootstrapping mechanism may still end
    // up calling this.qualifiers().  The alternative would be to make
    // the qualifiers field non-final and I don't want to do that.
    final Qualifiers qualifiers = this.qualifiers;
    return qualifiers == null ? Qualifiers.of() : qualifiers;
  }

  @Override // ConfiguredSupplier
  @SuppressWarnings("unchecked")
  public final <P> ConfiguredSupplier<P> parent() {
    return (ConfiguredSupplier<P>)this.parent;
  }

  @Override // ConfiguredSupplier
  public final Path2 path() {
    return this.path;
  }

  @Override // ConfiguredSupplier
  public final T get() {
    return this.supplier.get();
  }

  @Override // ConfiguredSupplier
  public final <U> Settings<U> of(final ConfiguredSupplier<?> parent,
                                  final Path2 absolutePath) {
    return
      this.of(parent,
              absolutePath,
              this.rejectedProvidersConsumerSupplier.get(),
              this.rejectedValuesConsumerSupplier.get(),
              this.ambiguousValuesConsumerSupplier.get());
  }

  private final <U> Settings<U> of(final ConfiguredSupplier<?> parent,
                                   Path2 absolutePath,
                                   final Consumer<? super Provider> rejectedProviders,
                                   final Consumer<? super Value<?>> rejectedValues,
                                   final Consumer<? super Value<?>> ambiguousValues) {
    // TODO: temporary assertions
    assert parent.isRoot();
    assert parent.path().isRoot();

    if (absolutePath.isAbsolute()) {
      if (absolutePath.size() == 1 && parent != this) {
        throw new IllegalArgumentException("absolutePath.isRoot(): " + absolutePath);
      }
    } else {
      throw new IllegalArgumentException("!absolutePath.isAbsolute(): " + absolutePath);
    }
    absolutePath = this.transliterate(absolutePath);
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
    final Qualified.Record<Path2> qp = Qualified.Record.of(parent.qualifiers(), absolutePath);
    Settings<?> settings = this.settingsCache.get(qp);
    if (settings == null) {
      settings =
        this.settingsCache.putIfAbsent(qp,
                                       this.computeSettings(parent,
                                                            absolutePath,
                                                            rejectedProviders,
                                                            rejectedValues,
                                                            ambiguousValues));
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
  public final Path2 transliterate(final Path2 path) {
    if (path.isTransliterated()) {
      return path;
    } else if (path.type() == Path2.class) {
      final Accessor2 a = path.get(path.size() - 1);
      if (a != null && a.name().equals("transliterate")) {
        final List<Class<?>> parameters = a.parameters().orElse(null);
        if (parameters.size() == 1 && parameters.get(0) == Path2.class) {
          // Are we in the middle of a transliteration request? Avoid
          // the infinite loop.
          return path;
        }
      }
    }
    return
      this.<Path2>of(Accessor2.of("transliterate", // name
                                  Path2.class, // type
                                  Path2.class, // parameter
                                  path.toString())) // sole argument
      .orElse(path);
  }

  private final <U> Settings<U> computeSettings(final ConfiguredSupplier<?> parent,
                                                final Path2 absolutePath,
                                                final Consumer<? super Provider> rejectedProviders,
                                                final Consumer<? super Value<?>> rejectedValues,
                                                final Consumer<? super Value<?>> ambiguousValues) {
    assert absolutePath.isAbsolute() : "absolutePath: " + absolutePath;
    final Value<U> value = this.value(parent, absolutePath, rejectedProviders, rejectedValues, ambiguousValues);
    final Supplier<U> supplier;
    if (value == null) {
      supplier = Settings::throwNoSuchElementException;
    } else {
      supplier = value;
    }
    return
      new Settings<>(this.settingsCache,
                     this.providers,
                     parent.qualifiers(),
                     parent,
                     absolutePath,
                     supplier,
                     this.rejectedProvidersConsumerSupplier,
                     this.rejectedValuesConsumerSupplier,
                     this.ambiguousValuesConsumerSupplier);
  }

  private final <U> Value<U> value(final ConfiguredSupplier<?> parent,
                                   final Path2 absolutePath,
                                   final Consumer<? super Provider> rejectedProviders,
                                   final Consumer<? super Value<?>> rejectedValues,
                                   final Consumer<? super Value<?>> ambiguousValues) {
    assert absolutePath.isAbsolute() : "absolutePath: " + absolutePath;
    final Collection<? extends Provider> providers = this.providers();

    if (providers.isEmpty()) {
      return null;
    }

    if (providers.size() == 1) {
      final Provider provider = providers instanceof List<? extends Provider> list ? list.get(0) : providers.iterator().next();
      if (provider == null) {
        return null;
      } else if (!this.isSelectable(provider, parent, absolutePath)) {
        rejectedProviders.accept(provider);
        return null;
      } else {
        final Value<?> value = provider.get(parent, absolutePath);
        if (value == null) {
          return null;
        } else if (!isSelectable(parent.qualifiers(), absolutePath, value.qualifiers(), value.path())) {
          rejectedValues.accept(value);
          return null;
        }
        @SuppressWarnings("unchecked")
        final Value<U> v = (Value<U>)value;
        return v;
      }
    }

    final Qualifiers qualifiers = parent.qualifiers();

    Value<U> candidate = null;
    Provider candidateProvider = null;

    int candidateQualifiersScore = Integer.MIN_VALUE;
    int candidatePathScore = Integer.MIN_VALUE;

    PROVIDER_LOOP:
    for (final Provider provider : providers) {

      if (provider == null || !this.isSelectable(provider, parent, absolutePath)) {
        rejectedProviders.accept(provider);
        continue PROVIDER_LOOP;
      }

      @SuppressWarnings("unchecked")
      final Value<U> v = (Value<U>)provider.get(parent, absolutePath);
      Value<U> value = v;

      // NOTE: INFINITE LOOP POSSIBILITY; read carefully!
      VALUE_EVALUATION_LOOP:
      while (true) {

        if (!isSelectable(qualifiers, absolutePath, value.qualifiers(), value.path())) {
          rejectedValues.accept(value);
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

        final Value<U> disambiguatedValue = this.disambiguate(parent, absolutePath, candidateProvider, candidate, provider, value);

        if (disambiguatedValue == null) {
          // Couldn't disambiguate.
          ambiguousValues.accept(candidate);
          ambiguousValues.accept(value);
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
                                       final ConfiguredSupplier<?> supplier,
                                       final Path2 absolutePath) {
    if (!absolutePath.isAbsolute()) {
      throw new IllegalArgumentException("absolutePath: " + absolutePath);
    }
    return
      AssignableType.of(provider.upperBound()).isAssignable(absolutePath.type()) &&
      provider.isSelectable(supplier, absolutePath);
  }

  @SubordinateTo("#of(ConfiguredSupplier, Path2, Supplier)")
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
   * Path2#isAbsolute() absolute}
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
   * #isSelectable(Path2, Path2)} must return {@code true} when supplied
   * with {@code absoluteReferencePath} and {@code valuePath}
   * respectively.  Note that such an invocation is <em>not</em> made
   * by this method, but logically precedes it when this method is
   * called in the natural course of events by the {@link
   * #of(ConfiguredSupplier, Path2)} method.</p>
   *
   * @param absoluteReferencePath the {@link Path2} against which to
   * score the supplied {@code valuePath}; must not be {@code null};
   * must adhere to the preconditions above
   *
   * @param valuePath the {@link Path2} to score against the supplied
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
   * @see #of(ConfiguredSupplier, Path2)
   *
   * @see #isSelectable(Path2, Path2)
   *
   * @threadsafety This method is and its overrides must be safe for
   * concurrent use by multiple threads.
   *
   * @idempotency This method is idempotent and deterministic.
   * Specifically, the same score is returned whenever this method is
   * invoked with the same paths.  Overrides must preserve this
   * property.
   */
  @SubordinateTo("#of(ConfiguredSupplier, Path2, Supplier)")
  protected int score(final Path2 absoluteReferencePath, final Path2 valuePath) {
    if (!absoluteReferencePath.isAbsolute()) {
      throw new IllegalArgumentException("absoluteReferencePath: " + absoluteReferencePath);
    }

    final int lastValuePathIndex = absoluteReferencePath.lastIndexOf(valuePath, AccessorsMatchBiPredicate.INSTANCE);
    assert lastValuePathIndex >= 0 : "absoluteReferencePath: " + absoluteReferencePath + "; valuePath: " + valuePath;
    assert lastValuePathIndex + valuePath.size() == absoluteReferencePath.size() : "absoluteReferencePath: " + absoluteReferencePath + "; valuePath: " + valuePath;

    int score = valuePath.size();
    for (int valuePathIndex = 0; valuePathIndex < valuePath.size(); valuePathIndex++) {
      final int referencePathIndex = lastValuePathIndex + valuePathIndex;

      final Accessor2 referenceAccessor = absoluteReferencePath.get(referencePathIndex);
      final Accessor2 valueAccessor = valuePath.get(valuePathIndex);
      if (!referenceAccessor.name().equals(valueAccessor.name())) {
        return Integer.MIN_VALUE;
      }

      final Type referenceType = referenceAccessor.type().orElse(null);
      final Type valueType = valueAccessor.type().orElse(null);
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

      final List<Class<?>> referenceParameters = referenceAccessor.parameters().orElse(null);
      final List<Class<?>> valueParameters = valueAccessor.parameters().orElse(null);
      if (referenceParameters == null) {
        if (valueParameters != null) {
          return Integer.MIN_VALUE;
        }
      } else if (valueParameters == null || referenceParameters.size() != valueParameters.size()) {
        return Integer.MIN_VALUE;
      }

      final List<String> referenceArguments = referenceAccessor.arguments().orElse(null);
      final List<String> valueArguments = valueAccessor.arguments().orElse(null);
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
          // The reference accessor had, say, two arguments, and the
          // value had, say, one.  We treat this as a mismatch.
          return Integer.MIN_VALUE;
        }
      }
    }
    return score;
  }
  /*
      if (absoluteReferencePath.isAccessor(referencePathIndex)) {
        if (valuePath.isAccessor(valuePathIndex)) {
          final Accessor referenceAccessor = absoluteReferencePath.accessor(referencePathIndex);
          final Accessor valueAccessor = valuePath.accessor(valuePathIndex);

          assert referenceAccessor.name().equals(valueAccessor.name()) : "referenceAccessor: " + referenceAccessor + "; valueAccessor: " + valueAccessor;
          assert referenceAccessor.parameters().size() == valueAccessor.parameters().size() : "referenceAccessor: " + referenceAccessor + "; valueAccessor: " + valueAccessor;

          final List<String> referenceArgs = referenceAccessor.arguments();
          final int referenceArgsSize = referenceArgs.size();
          final List<String> valueArgs = valueAccessor.arguments();
          final int valueArgsSize = valueArgs.size();
          if (referenceArgsSize < valueArgsSize) {
            // The value path is unsuitable because it provided too
            // many arguments.
            return Integer.MIN_VALUE;
          } else if (referenceArgs.equals(valueArgs)) {
            score += referenceArgsSize;
          } else if (referenceArgsSize == valueArgsSize) {
            // Same sizes, but different arguments.  The value is not suitable.
            return Integer.MIN_VALUE;
          } else if (valueArgsSize == 0) {
            // The value is indifferent with respect to arguments. It
            // *could* be suitable but not *as* suitable as one that
            // matched.  Don't adjust the score.
          } else {
            // The reference accessor had, say, two arguments, and the
            // value had, say, one.  We treat this as a mismatch.
            return Integer.MIN_VALUE;
          }
        } else {
          throw new IllegalArgumentException("valuePath: " + valuePath);
        }
      } else if (absoluteReferencePath.isType(referencePathIndex)) {
        if (valuePath.isType(valuePathIndex)) {
          final Type referenceType = absoluteReferencePath.type(referencePathIndex);
          final Type valueType = valuePath.type(valuePathIndex);
          if (Types.equals(referenceType, valueType)) {
            score++;
          } else {
            // valueType is a subtype of referenceType. Don't adjust
            // the score.
            assert AssignableType.of(referenceType).isAssignable(valueType) : "referenceType: " + referenceType + "; valueType: " + valueType;
          }
        } else {
          throw new IllegalArgumentException("valuePath: " + valuePath);
        }
      } else {
        throw new IllegalArgumentException("absoluteReferencePath: " + absoluteReferencePath);
      }
    }
    return score;
  }
  */

  protected <U> Value<U> disambiguate(final ConfiguredSupplier<?> supplier,
                                      final Path2 absolutePath,
                                      final Provider p0,
                                      final Value<U> v0,
                                      final Provider p1,
                                      final Value<U> v1) {
    if (!absolutePath.isAbsolute()) {
      throw new IllegalArgumentException("absolutePath: " + absolutePath);
    }
    final Disambiguator d = supplier.of(Disambiguator.class).orElse(null);
    return d == null ? null : d.disambiguate(supplier, absolutePath, p0, v0, p1, v1);
  }


  /*
   * Static methods.
   */


  public static final Collection<Provider> loadedProviders() {
    return LoadedProviders.loadedProviders;
  }

  private static final boolean isSelectable(final Qualifiers referenceQualifiers,
                                            final Path2 absoluteReferencePath,
                                            final Qualifiers valueQualifiers,
                                            final Path2 valuePath) {
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
   * <p>This method calls {@link Path2#endsWith(Path2, BiPredicate)} on
   * the supplied {@code absoluteReferencePath} with {@code valuePath}
   * as its {@link Path2}-typed first argument, and a {@link
   * BiPredicate} that returns {@code true} if and only if any of the
   * following conditions is true:</p>
   *
   * <ul>
   *
   * <li>The first argument is an {@link Accessor}, the second
   * argument is also an {@link Accessor}, both have equal {@linkplain
   * Accessor#name() names}, both have equal numbers of {@linkplain
   * Accessor#parameters() parameters}, both have equal numbers of
   * {@linkplain Accessor#arguments() arguments}, and each parameter
   * found in {@code valuePath} {@linkplain
   * Class#isAssignableFrom(Class) is assignable to} the corresponding
   * parameter found in {@code absoluteReferencePath}</li>
   *
   * <li>The first argument is a {@link Type}, the second argument is
   * also a {@link Type} and the second {@link Type} {@linkplain
   * Assignable#isAssignable(Object) is assignable to} the first</li>
   *
   * </ul>
   *
   * <p>In all other cases this method returns {@code false} or throws
   * an exception.</p>
   *
   * @param absoluteReferencePath the reference path; must not be
   * {@code null}; must be {@linkplain Path2#isAbsolute() absolute}
   *
   * @param valuePath the {@link Path2} to test; must not be {@code
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
   * absoluteReferencePath} {@linkplain Path2#isAbsolute() is not
   * absolute}
   */
  protected static final boolean isSelectable(final Path2 absoluteReferencePath,
                                              final Path2 valuePath) {
    if (!absoluteReferencePath.isAbsolute()) {
      throw new IllegalArgumentException("absoluteReferencePath: " + absoluteReferencePath);
    }
    return absoluteReferencePath.endsWith(valuePath, AccessorsMatchBiPredicate.INSTANCE);
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


  private static final class LoadedProviders {

    // We hide this static field inside a private nested class so it
    // isn't initialized unless it's actually used.
    private static final List<Provider> loadedProviders =
      ServiceLoader.load(Provider.class, Provider.class.getClassLoader())
      .stream()
      .map(ServiceLoader.Provider::get)
      .sorted(Prioritized.COMPARATOR_DESCENDING)
      .toList();

  }

  // Matches accessor names (equality), parameter types
  // (isAssignableFrom) and Types (AssignableType.isAssignable()).
  // Argument values themselves are deliberately ignored.
  private static final class AccessorsMatchBiPredicate implements BiPredicate<Accessor2, Accessor2> {

    private static final AccessorsMatchBiPredicate INSTANCE = new AccessorsMatchBiPredicate();

    private AccessorsMatchBiPredicate() {
      super();
    }

    @Override // BiPredicate<Accessor2, Accessor2>
    public final boolean test(final Accessor2 a1, final Accessor2 a2) {
      final String name1 = a1.name();
      final String name2 = a2.name();
      if (!name1.isEmpty() && !name2.isEmpty() && !name1.equals(name2)) {
        // Empty names have special significance in that they "match"
        // any other name.
        return false;
      }

      final Type t1 = a1.type().orElse(null);
      final Type t2 = a2.type().orElse(null);
      if (t1 != null && t2 != null && !AssignableType.of(t1).isAssignable(t2)) {
        return false;
      }

      final List<Class<?>> p1 = a1.parameters().orElse(null);
      final List<Class<?>> p2 = a2.parameters().orElse(null);
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
