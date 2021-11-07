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

import org.microbean.settings.api.Provider.Value;

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
public class Settings<T> implements ConfiguredSupplier<T> {


  /*
   * Instance fields.
   */


  // Package-private for testing only.
  final ConcurrentMap<Qualified.Record<Path>, Settings<?>> settingsCache;

  private final List<Provider> providers;

  private final Qualifiers qualifiers;

  private final ConfiguredSupplier<?> parent;

  private final Supplier<T> supplier;

  private final Path path;

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
    this(new ConcurrentHashMap<Qualified.Record<Path>, Settings<?>>(),
         loadedProviders(),
         null, // qualifiers
         null, // parent,
         Path.of(),
         null, // supplier
         Settings::generateSink,
         Settings::generateSink,
         Settings::generateSink);
  }

  @SuppressWarnings("unchecked")
  private Settings(final ConcurrentMap<Qualified.Record<Path>, Settings<?>> settingsCache,
                   final Collection<? extends Provider> providers,
                   final Qualifiers qualifiers,
                   final ConfiguredSupplier<?> parent, // if null, will end up being "this" if path is Path.of()
                   final Path path,
                   final Supplier<T> supplier, // if null, will end up being () -> this if path is Path.of()
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
      if (path.equals(Path.of())) {
        this.path = Path.of();
        this.supplier = supplier == null ? () -> (T)this : supplier; // NOTE
        this.parent = this; // NOTE
        final Qualified.Record<Path> qp = Qualified.Record.of(Qualifiers.of(), Path.of());
        this.settingsCache.put(qp, this); // NOTE
        // While the following call is in effect, our
        // final-but-as-yet-uninitialized qualifiers instance field will
        // be null.  Note that the qualifiers() accessor method accounts
        // for this and will return Qualifiers.of() instead.
        // this.qualifiers = this.plus(Qualifiers.class, Qualifiers::of).get();
        this.qualifiers = this.<Qualifiers>plus(Qualifiers.class).orElseGet(Qualifiers::of);
        this.settingsCache.remove(qp);
      } else {
        throw new IllegalArgumentException("path: " + path);
      }
    } else if (path.equals(Path.of())) {
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
  public final Path path() {
    return this.path;
  }

  @Override // ConfiguredSupplier
  public final T get() {
    return this.supplier.get();
  }

  @Override // ConfiguredSupplier
  public final <U> Settings<U> of(final ConfiguredSupplier<?> parent,
                                  final Path path) {
    return
      this.of(parent,
              path, // NOTE: no plus()
              this.rejectedProvidersConsumerSupplier.get(),
              this.rejectedValuesConsumerSupplier.get(),
              this.ambiguousValuesConsumerSupplier.get());
  }

  private final <U> Settings<U> of(final ConfiguredSupplier<?> parent,
                                   Path path,
                                   final Consumer<? super Provider> rejectedProviders,
                                   final Consumer<? super Value<?>> rejectedValues,
                                   final Consumer<? super Value<?>> ambiguousValues) {
    if (path.isAbsolute()) {
      if (path.size() == 1 && parent != this) {
        throw new IllegalArgumentException("path.isRoot(): " + path);
      }
    } else {
      throw new IllegalArgumentException("!path.isAbsolute(): " + path);
    }
    path = this.transliterate(path);
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
    final Qualified.Record<Path> qp = Qualified.Record.of(parent.qualifiers(), path);
    Settings<?> settings = this.settingsCache.get(qp);
    if (settings == null) {
      settings =
        this.settingsCache.putIfAbsent(qp,
                                       this.computeSettings(parent,
                                                            path,
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
  public final Path transliterate(final Path path) {
    if (!path.isAbsolute() || path.isTransliterated() || path.type() == Path.class) {
      final Accessor a = path.accessor(1);
      if (a.name().equals("transliterate") && a.parameterCount() == 1 && a.parameter(0) == Path.class) {
        return path;
      }
    }
    return
      this.of(Accessor.of("transliterate", Path.class),
              Path.class)
      .orElse(path);
  }

  private final <U> Settings<U> computeSettings(final ConfiguredSupplier<?> parent,
                                                final Path path,
                                                final Consumer<? super Provider> rejectedProviders,
                                                final Consumer<? super Value<?>> rejectedValues,
                                                final Consumer<? super Value<?>> ambiguousValues) {
    final Value<U> value = this.value(parent, path, rejectedProviders, rejectedValues, ambiguousValues);
    final Supplier<U> supplier;
    if (value == null) {
      supplier = Settings::returnNull;
    } else {
      supplier = value;
    }
    return
      new Settings<>(this.settingsCache,
                     this.providers,
                     parent.qualifiers(),
                     parent,
                     path,
                     supplier,
                     this.rejectedProvidersConsumerSupplier,
                     this.rejectedValuesConsumerSupplier,
                     this.ambiguousValuesConsumerSupplier);
  }

  private final <U> Value<U> value(final ConfiguredSupplier<?> parent,
                                   final Path path,
                                   final Consumer<? super Provider> rejectedProviders,
                                   final Consumer<? super Value<?>> rejectedValues,
                                   final Consumer<? super Value<?>> ambiguousValues) {
    final Collection<? extends Provider> providers = this.providers();
    if (providers.isEmpty()) {
      return null;
    } else if (providers.size() == 1) {
      final Provider provider = providers instanceof List<? extends Provider> list ? list.get(0) : providers.iterator().next();
      if (provider == null) {
        return null;
      } else if (!this.isSelectable(provider, parent, path)) {
        rejectedProviders.accept(provider);
        return null;
      } else {
        final Value<?> value = provider.get(parent, path);
        if (value == null) {
          return null;
        } else if (!isSelectable(parent.qualifiers(), path, value.qualifiers(), value.path())) {
          rejectedValues.accept(value);
          return null;
        } else {
          @SuppressWarnings("unchecked")
          final Value<U> v = (Value<U>)value;
          return v;
        }
      }
    } else {
      final Qualifiers qualifiers = parent.qualifiers();
      Value<U> candidate = null;
      Provider candidateProvider = null;
      int candidateQualifiersScore = Integer.MIN_VALUE;
      int candidatePathScore = Integer.MIN_VALUE;
      for (final Provider provider : providers) {
        if (provider != null && this.isSelectable(provider, parent, path)) {
          @SuppressWarnings("unchecked")
          final Value<U> v = (Value<U>)provider.get(parent, path);
          Value<U> value = v;
          VALUE_EVALUATION_LOOP:
          while (value != null) { // NOTE INFINITE LOOP POSSIBILITY; read carefully
            if (isSelectable(qualifiers, path, value.qualifiers(), value.path())) {
              if (candidate == null) {
                candidate = value;
                candidateProvider = provider;
                candidateQualifiersScore = this.score(qualifiers, candidate.qualifiers());
                candidatePathScore = this.score(path, candidate.path());
                value = null; // critical
              } else {
                // Let's do qualifiers first.  This is an arbitrary decision.
                final int valueQualifiersScore = this.score(qualifiers, value.qualifiers());
                if (valueQualifiersScore < candidateQualifiersScore) {
                  rejectedValues.accept(value);
                  value = null; // critical
                } else if (valueQualifiersScore == candidateQualifiersScore) {
                  // Same qualifiers score; let's now do paths.
                  final int valuePathScore = this.score(path, value.path());
                  if (valuePathScore < candidatePathScore) {
                    rejectedValues.accept(value);
                    value = null; // critical
                  } else if (valuePathScore == candidatePathScore) {
                    final Value<U> disambiguatedValue =
                      this.disambiguate(parent, path, candidateProvider, candidate, provider, value);
                    if (disambiguatedValue == null) {
                      ambiguousValues.accept(candidate);
                      ambiguousValues.accept(value);
                      value = null; // critical
                      // TODO: I'm not sure whether to null the
                      // candidate bits and potentially grab another
                      // less suitable one, keep the existing one even
                      // though it's ambiguous, or, if we keep it, to
                      // break or continue.  For now I'm going to keep
                      // it and continue; the caller can examine
                      // whatever ended up in the ambiguous values
                      // consumer and figure out what it wants to do.
                    } else if (disambiguatedValue.equals(candidate)) {
                      rejectedValues.accept(value);
                      value = null; // critical
                    } else if (disambiguatedValue.equals(value)) {
                      rejectedValues.accept(candidate);
                      candidate = disambiguatedValue;
                      candidateProvider = provider;
                      candidateQualifiersScore = valueQualifiersScore;
                      candidatePathScore = valuePathScore;
                      value = null; // critical
                    } else {
                      // Disambiguation came up with an entirely
                      // different value, so run it through the
                      // machine.
                      value = disambiguatedValue;
                      assert value != null;
                      continue VALUE_EVALUATION_LOOP; // NOTE
                    }
                  } else {
                    rejectedValues.accept(candidate);
                    candidate = value;
                    candidateProvider = provider;
                    candidateQualifiersScore = valueQualifiersScore;
                    candidatePathScore = valuePathScore;
                    value = null; // critical
                  }
                } else {
                  rejectedValues.accept(candidate);
                  candidate = value;
                  candidateProvider = provider;
                  candidateQualifiersScore = valueQualifiersScore;
                  // (No need to update candidatePathScore.)
                  value = null; // critical
                }
              }
            } else {
              rejectedValues.accept(value);
              value = null; // critical
            }
            assert value == null;
          } // end while(value != null)
          assert value == null;
        } else {
          rejectedProviders.accept(provider);
        }
      }
      return candidate;
    }
  }

  protected final boolean isSelectable(final Provider provider,
                                       final ConfiguredSupplier<?> supplier,
                                       final Path path) {
    return
      AssignableType.of(provider.upperBound()).isAssignable(path.type()) &&
      provider.isSelectable(supplier, path);
  }

  @SubordinateTo("#of(ConfiguredSupplier, Path, Supplier)")
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
   * valuePath} with respect to {@code referencePath}.
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
   * <li>{@code referencePath} must have {@code void.class} as its
   * first {@linkplain Path#type(int) element}</li>
   *
   * <li>{@code valuePath} must be selectable with respect to {@code
   * referencePath}, where the definition of selectability is
   * described below</li>
   *
   * </ul>
   *
   * <p>For {@code valuePath} to "be selectable" with respect to
   * {@code referencePath} for the purposes of this method and for no
   * other purpose, a hypothetical invocation of {@link
   * #isSelectable(Path, Path)} must return {@code true} when supplied
   * with {@code referencePath} and {@code valuePath} respectively.
   * Note that such an invocation is <em>not</em> made by this method,
   * but logically precedes it when this method is called in the
   * natural course of events by the {@link #of(ConfiguredSupplier,
   * Path)} method.</p>
   *
   * @param referencePath the {@link Path} against which to score the
   * supplied {@code valuePath}; must not be {@code null}; must adhere
   * to the preconditions above
   *
   * @param valuePath the {@link Path} to score against the supplied
   * {@code referencePath}; must not be {@code null}; must adhere to
   * the preconditions above
   *
   * @return a relative score for {@code valuePath} with respect to
   * {@code referencePath}; meaningless on its own
   *
   * @exception NullPointerException if either parameter is {@code
   * null}
   *
   * @exception IllegalArgumentException if certain preconditions have
   * been violated
   *
   * @see #of(ConfiguredSupplier, Path)
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
  @SubordinateTo("#of(ConfiguredSupplier, Path, Supplier)")
  protected int score(final Path referencePath, final Path valuePath) {
    assert referencePath.isAbsolute() : "referencePath: " + referencePath;

    final int lastValuePathIndex = referencePath.lastIndexOf(valuePath, AccessorsMatchBiPredicate.INSTANCE);
    assert lastValuePathIndex >= 0 : "referencePath: " + referencePath + "; valuePath: " + valuePath;
    assert lastValuePathIndex + valuePath.size() == referencePath.size() : "referencePath: " + referencePath + "; valuePath: " + valuePath;

    int score = valuePath.size();
    for (int valuePathIndex = 0; valuePathIndex < valuePath.size(); valuePathIndex++) {
      final int referencePathIndex = lastValuePathIndex + valuePathIndex;
      if (referencePath.isAccessor(referencePathIndex)) {
        if (valuePath.isAccessor(valuePathIndex)) {
          final Accessor referenceAccessor = referencePath.accessor(referencePathIndex);
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
      } else if (referencePath.isType(referencePathIndex)) {
        if (valuePath.isType(valuePathIndex)) {
          final Type referenceType = referencePath.type(referencePathIndex);
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
        throw new IllegalArgumentException("referencePath: " + referencePath);
      }
    }
    return score;
  }

  protected <U> Value<U> disambiguate(final ConfiguredSupplier<?> parent,
                                      final Path path,
                                      final Provider p0,
                                      final Value<U> v0,
                                      final Provider p1,
                                      final Value<U> v1) {
    return null;
  }


  /*
   * Static methods.
   */


  public static final Collection<Provider> loadedProviders() {
    return LoadedProviders.loadedProviders;
  }

  private static final boolean isSelectable(final Qualifiers referenceQualifiers,
                                              final Path referencePath,
                                              final Qualifiers valueQualifiers,
                                              final Path valuePath) {
    return isSelectable(referenceQualifiers, valueQualifiers) && isSelectable(referencePath, valuePath);
  }

  protected static final boolean isSelectable(final Qualifiers referenceQualifiers,
                                              final Qualifiers valueQualifiers) {
    return referenceQualifiers.isEmpty() || valueQualifiers.isEmpty() || referenceQualifiers.intersectionSize(valueQualifiers) > 0;
  }

  /**
   * Returns {@code true} if the supplied {@code valuePath} is
   * <em>selectable</em> (for further consideration and scoring) with
   * respect to the supplied {@code referencePath}.
   *
   * <p>This method calls {@link Path#endsWith(Path, BiPredicate)} on
   * the supplied {@code referencePath} with {@code valuePath} as its
   * {@link Path}-typed first argument, and a {@link BiPredicate} that
   * returns {@code true} if and only if any of the following
   * conditions is true:</p>
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
   * parameter found in {@code referencePath}</li>
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
   * @param referencePath the reference path; must not be {@code
   * null}; must have {@code void.class} as its first {@linkplain
   * Path#type(int) element}
   *
   * @param valuePath the {@link Path} to test; must not be {@code
   * null}
   *
   * @return {@code true} if {@code valuePath} is selectable (for
   * further consideration and scoring) with respect to {@code
   * referencePath}; {@code false} in all other cases
   *
   * @exception NullPointerException if either parameter is {@code
   * null}
   *
   * @exception IllegalArgumentException if {@code referencePath}'s
   * first {@linkplain Path#type(int) element} is not {@code
   * void.class}
   */
  protected static final boolean isSelectable(final Path referencePath,
                                              final Path valuePath) {
    if (referencePath.isAbsolute()) {
      return referencePath.endsWith(valuePath, AccessorsMatchBiPredicate.INSTANCE);
    } else {
      throw new IllegalArgumentException("referencePath: " + referencePath);
    }
  }

  private static final <T> T returnNull() {
    return null;
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
  private static final class AccessorsMatchBiPredicate implements BiPredicate<Object, Object> {

    private static final AccessorsMatchBiPredicate INSTANCE = new AccessorsMatchBiPredicate();

    private AccessorsMatchBiPredicate() {
      super();
    }

    @Override // BiPredicate
    public final boolean test(final Object o1, final Object o2) {
      if (o1 instanceof Accessor a1) {
        if (o2 instanceof Accessor a2 && a1.name().equals(a2.name()) && a1.arguments().size() == a2.arguments().size()) {
          final List<Class<?>> p1 = a1.parameters();
          final List<Class<?>> p2 = a2.parameters();
          if (p1.size() == p2.size()) {
            for (int i = 0; i < p1.size(); i++) {
              if (!p1.get(i).isAssignableFrom(p2.get(i))) {
                return false;
              }
            }
            return true;
          }
        } else if (!(o2 instanceof Type)) {
          throw new IllegalArgumentException("o2: " + o2);
        }
      } else if (o1 instanceof Type t1) {
        if (o2 instanceof Type t2) {
          return AssignableType.of(t1).isAssignable(t2);
        } else if (!(o2 instanceof Accessor)) {
          throw new IllegalArgumentException("o2: " + o2);
        }
      } else {
        throw new IllegalArgumentException("o1: " + o1);
      }
      return false;
    }

  }

}
