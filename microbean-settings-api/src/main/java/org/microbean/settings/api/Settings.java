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
import java.util.Objects;
import java.util.ServiceLoader;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.microbean.settings.api.Provider.Value;

public class Settings<P, T> implements ConfiguredSupplier<T> {


  /*
   * Instance fields.
   */


  private final BiFunction<? super Qualified.Record<Path>, Function<? super Qualified.Record<Path>, ? extends Settings<?, ?>>, ? extends Settings<?, ?>> settingsCache;

  private final List<Provider> providers;

  private final Qualifiers qualifiers;

  private final Supplier<P> parentSupplier;

  private final Supplier<T> supplier;

  private final Path path;

  private final Supplier<? extends Consumer<? super Provider>> rejectedProvidersConsumerSupplier;

  private final Supplier<? extends Consumer<? super Value<?>>> rejectedValuesConsumerSupplier;

  private final Supplier<? extends Consumer<? super Value<?>>> ambiguousValuesConsumerSupplier;


  /*
   * Constructors.
   */


  public Settings() {
    this(loadedProviders());
  }

  public Settings(final Collection<? extends Provider> providers) {
    this(new ConcurrentHashMap<Qualified.Record<Path>, Settings<?, ?>>()::computeIfAbsent, providers);
  }

  public Settings(final BiFunction<? super Qualified.Record<Path>, Function<? super Qualified.Record<Path>, ? extends Settings<?, ?>>, ? extends Settings<?, ?>> settingsCache,
                  final Collection<? extends Provider> providers) {
    super();
    this.settingsCache = Objects.requireNonNull(settingsCache, "settingsCache");
    this.providers = List.copyOf(providers);
    this.parentSupplier = Settings::returnNull;
    this.path = Path.of();
    this.supplier = Settings::returnNull;
    this.rejectedValuesConsumerSupplier = Settings::generateSink;
    this.rejectedProvidersConsumerSupplier = Settings::generateSink;
    this.ambiguousValuesConsumerSupplier = Settings::generateSink;
    this.qualifiers =
      this.of(Qualifiers.of(),
              this.parentSupplier(),
              this.path().plus(Accessor.of(), Qualifiers.class),
              Qualifiers::of)
      .get();
  }

  public Settings(final Qualifiers qualifiers) {
    this(loadedProviders(), qualifiers);
  }

  public Settings(final Collection<? extends Provider> providers, final Qualifiers qualifiers) {
    this(providers, qualifiers, Settings::returnNull);
  }

  public Settings(final Collection<? extends Provider> providers, final Qualifiers qualifiers, final Supplier<P> parentSupplier) {
    this(new ConcurrentHashMap<Qualified.Record<Path>, Settings<?, ?>>()::computeIfAbsent, providers, qualifiers, parentSupplier);
  }

  public Settings(final Collection<? extends Provider> providers,
                  final Qualifiers qualifiers,
                  final Supplier<P> parentSupplier,
                  final Path path) {
    this(new ConcurrentHashMap<Qualified.Record<Path>, Settings<?, ?>>()::computeIfAbsent, providers, qualifiers, parentSupplier, path);
  }

  public Settings(final Collection<? extends Provider> providers,
                  final Qualifiers qualifiers,
                  final Supplier<P> parentSupplier,
                  final Path path,
                  final Supplier<T> supplier) {
    this(new ConcurrentHashMap<Qualified.Record<Path>, Settings<?, ?>>()::computeIfAbsent, providers, qualifiers, parentSupplier, path, supplier);
  }

  public Settings(final BiFunction<? super Qualified.Record<Path>, Function<? super Qualified.Record<Path>, ? extends Settings<?, ?>>, ? extends Settings<?, ?>> settingsCache,
                  final Collection<? extends Provider> providers,
                  final Qualifiers qualifiers) {
    this(settingsCache, providers, qualifiers, Settings::returnNull);
  }

  public Settings(final BiFunction<? super Qualified.Record<Path>, Function<? super Qualified.Record<Path>, ? extends Settings<?, ?>>, ? extends Settings<?, ?>> settingsCache,
                  final Collection<? extends Provider> providers,
                  final Qualifiers qualifiers,
                  final Supplier<P> parentSupplier) {
    this(settingsCache, providers, qualifiers, parentSupplier, Path.of());
  }

  public Settings(final BiFunction<? super Qualified.Record<Path>, Function<? super Qualified.Record<Path>, ? extends Settings<?, ?>>, ? extends Settings<?, ?>> settingsCache,
                  final Collection<? extends Provider> providers,
                  final Qualifiers qualifiers,
                  final Supplier<P> parentSupplier,
                  final Path path) {
    this(settingsCache, providers, qualifiers, parentSupplier, path, Settings::returnNull);
  }

  public Settings(final BiFunction<? super Qualified.Record<Path>, Function<? super Qualified.Record<Path>, ? extends Settings<?, ?>>, ? extends Settings<?, ?>> settingsCache,
                  final Collection<? extends Provider> providers,
                  final Qualifiers qualifiers,
                  final Supplier<P> parentSupplier,
                  final Path path,
                  final Supplier<T> supplier) {
    this(settingsCache, providers, qualifiers, parentSupplier, path, supplier, Settings::generateSink, Settings::generateSink, Settings::generateSink);
  }

  public Settings(final BiFunction<? super Qualified.Record<Path>, Function<? super Qualified.Record<Path>, ? extends Settings<?, ?>>, ? extends Settings<?, ?>> settingsCache,
                  final Collection<? extends Provider> providers,
                  final Qualifiers qualifiers,
                  final Supplier<P> parentSupplier,
                  final Path path,
                  final Supplier<T> supplier,
                  final Supplier<? extends Consumer<? super Provider>> rejectedProvidersConsumerSupplier,
                  final Supplier<? extends Consumer<? super Value<?>>> rejectedValuesConsumerSupplier,
                  final Supplier<? extends Consumer<? super Value<?>>> ambiguousValuesConsumerSupplier) {
    super();
    this.settingsCache = Objects.requireNonNull(settingsCache, "settingsCache");
    this.providers = List.copyOf(providers);
    this.qualifiers = Objects.requireNonNull(qualifiers, "qualifiers");
    this.parentSupplier = Objects.requireNonNull(parentSupplier, "parentSupplier");
    this.path = Objects.requireNonNull(path, "path");
    this.supplier = supplier == null ? Settings::fail : supplier;
    this.rejectedProvidersConsumerSupplier = Objects.requireNonNull(rejectedProvidersConsumerSupplier, "rejectedProvidersConsumerSupplier");
    this.rejectedValuesConsumerSupplier = Objects.requireNonNull(rejectedValuesConsumerSupplier, "rejectedValuesConsumerSupplier");
    this.ambiguousValuesConsumerSupplier = Objects.requireNonNull(ambiguousValuesConsumerSupplier, "ambiguousValuesConsumerSupplier");
  }


  /*
   * Instance methods.
   */


  public final Collection<Provider> providers() {
    return this.providers;
  }

  @Override // ConfiguredSupplier
  public final Qualifiers qualifiers() {
    return this.qualifiers;
  }

  public final Supplier<P> parentSupplier() {
    return this.parentSupplier;
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
  public final <U> Settings<T, U> plus(final Type type) {
    return this.plus(type, Settings::fail);
  }

  @Override // ConfiguredSupplier
  public final <U> Settings<T, U> plus(final Type type, final Supplier<U> defaultSupplier) {
    return this.plus(Path.of(Accessor.of(), type), defaultSupplier);
  }

  @Override // ConfiguredSupplier
  public final <U> Settings<T, U> plus(final Path path) {
    return this.plus(path, Settings::fail);
  }

  @Override // ConfiguredSupplier
  public final <U> Settings<T, U> plus(final Path path, final Supplier<U> defaultSupplier) {
    return
      this.plus(path,
                defaultSupplier,
                this.rejectedProvidersConsumerSupplier.get(),
                this.rejectedValuesConsumerSupplier.get(),
                this.ambiguousValuesConsumerSupplier.get());
  }

  public final <U> Settings<T, U> plus(final Path path,
                                       final Supplier<U> defaultSupplier,
                                       final Consumer<? super Provider> rejectedProviders,
                                       final Consumer<? super Value<?>> rejectedValues,
                                       final Consumer<? super Value<?>> ambiguousValues) {
    return this.of(this, path, defaultSupplier, rejectedProviders, rejectedValues, ambiguousValues);
  }

  public final <Q, U> Settings<Q, U> of(final ConfiguredSupplier<Q> parent,
                                        final Path path,
                                        final Supplier<U> defaultSupplier) {
    return this.of(parent.qualifiers(), parent, parent.path().plus(path), defaultSupplier);
  }

  public final <Q, U> Settings<Q, U> of(final ConfiguredSupplier<Q> parent,
                                        final Path path,
                                        final Supplier<U> defaultSupplier,
                                        final Consumer<? super Provider> rejectedProviders,
                                        final Consumer<? super Value<?>> rejectedValues,
                                        final Consumer<? super Value<?>> ambiguousValues) {
    return
      this.of(parent.qualifiers(),
              parent,
              parent.path().plus(path),
              defaultSupplier,
              rejectedProviders,
              rejectedValues,
              ambiguousValues);
  }

  @Override // ConfiguredSupplier
  public final <Q, U> Settings<Q, U> of(final Qualifiers qualifiers,
                                        final Supplier<Q> parentSupplier,
                                        final Path path,
                                        final Supplier<U> defaultSupplier) {
    return
      this.of(qualifiers,
              parentSupplier,
              path,
              defaultSupplier,
              this.rejectedProvidersConsumerSupplier.get(),
              this.rejectedValuesConsumerSupplier.get(),
              this.ambiguousValuesConsumerSupplier.get());
  }

  @SuppressWarnings("unchecked")
  public final <Q, U> Settings<Q, U> of(final Qualifiers qualifiers,
                                        final Supplier<Q> parentSupplier,
                                        final Path path,
                                        final Supplier<U> defaultSupplier,
                                        final Consumer<? super Provider> rejectedProviders,
                                        final Consumer<? super Value<?>> rejectedValues,
                                        final Consumer<? super Value<?>> ambiguousValues) {
    return (Settings<Q, U>)this.settingsCache.apply(Qualified.Record.of(qualifiers, path),
                                                    qp -> this.computeSettings(qp.qualifiers(),
                                                                               parentSupplier,
                                                                               qp.qualified(),
                                                                               defaultSupplier,
                                                                               rejectedProviders,
                                                                               rejectedValues,
                                                                               ambiguousValues));
  }

  private final <Q, U> Settings<Q, U> computeSettings(final Qualifiers qualifiers,
                                                      final Supplier<Q> parentSupplier,
                                                      final Path path,
                                                      final Supplier<U> defaultSupplier,
                                                      final Consumer<? super Provider> rejectedProviders,
                                                      final Consumer<? super Value<?>> rejectedValues,
                                                      final Consumer<? super Value<?>> ambiguousValues) {
    final Value<U> value = this.value(qualifiers, parentSupplier, path, rejectedProviders, rejectedValues, ambiguousValues);
    final Supplier<U> supplier;
    if (value == null) {
      if (defaultSupplier == null) {
        supplier = Settings::fail;
      } else {
        supplier = defaultSupplier;
      }
    } else {
      supplier = value;
    }
    return new Settings<>(this.settingsCache, this.providers, qualifiers, parentSupplier, path, supplier);
  }

  private final <Q, U> Value<U> value(final Qualifiers qualifiers,
                                      final Supplier<Q> parentSupplier,
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
      } else if (!this.isSelectable(qualifiers, parentSupplier, path, provider)) {
        rejectedProviders.accept(provider);
        return null;
      } else {
        final Value<?> value = provider.get(this, qualifiers, parentSupplier, path);
        if (value == null) {
          return null;
        } else if (!isSelectable(qualifiers, path, value.qualifiers(), value.path())) {
          rejectedValues.accept(value);
          return null;
        } else {
          @SuppressWarnings("unchecked")
          final Value<U> v = (Value<U>)value;
          return v;
        }
      }
    } else {
      Value<?> candidate = null;
      Provider candidateProvider = null;
      int candidateQualifiersScore = Integer.MIN_VALUE;
      int candidatePathScore = Integer.MIN_VALUE;
      for (final Provider provider : providers) {
        if (provider != null && this.isSelectable(qualifiers, parentSupplier, path, provider)) {
          Value<?> value = provider.get(this, qualifiers, parentSupplier, path);
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
                final Path valuePath = value.path();
                // Let's do qualifiers first.  This is an arbitrary decision.
                final int valueQualifiersScore = this.score(qualifiers, value.qualifiers());
                if (valueQualifiersScore < candidateQualifiersScore) {
                  rejectedValues.accept(value);
                  value = null; // critical
                } else if (valueQualifiersScore == candidateQualifiersScore) {
                  // Same qualifiers score; let's now do paths.
                  final int valuePathScore = this.score(path, valuePath);
                  if (valuePathScore > 0) {
                    rejectedValues.accept(value);
                    value = null; // critical
                  } else if (valuePathScore == 0) {
                    final Value<?> disambiguatedValue =
                      this.disambiguate(qualifiers, parentSupplier, path, candidateProvider, candidate, provider, value);
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
          } // end while(value != null)
          assert value == null;
        } else {
          rejectedProviders.accept(provider);
        }
      }
      @SuppressWarnings("unchecked")
      final Value<U> c = (Value<U>)candidate;
      return c;
    }
  }

  protected final boolean isSelectable(final Qualifiers qualifiers,
                                       final Supplier<?> parentSupplier,
                                       final Path path,
                                       final Provider provider) {
    return
      AssignableType.of(provider.upperBound()).isAssignable(path.type()) &&
      provider.isSelectable(this, qualifiers, parentSupplier, path);
  }

  protected int score(final Qualifiers contextQualifiers, final Qualifiers valueQualifiers) {
    final int intersectionSize = contextQualifiers.intersectionSize(valueQualifiers);
    if (intersectionSize > 0) {
      return
        intersectionSize == valueQualifiers.size() ?
        intersectionSize :
        intersectionSize - contextQualifiers.symmetricDifferenceSize(valueQualifiers);
    } else {
      return -(contextQualifiers.size() + valueQualifiers.size());
    }
  }

  protected int score(final Path contextPath, final Path valuePath) {
    return contextPath.size() - valuePath.size();
  }

  protected Value<?> disambiguate(final Qualifiers qualifiers,
                                  final Supplier<?> parentSupplier,
                                  final Path path,
                                  final Provider p0,
                                  final Value<?> v0,
                                  final Provider p1,
                                  final Value<?> v1) {
    return null;
  }


  /*
   * Static methods.
   */


  public static final Collection<Provider> loadedProviders() {
    return LoadedProviders.loadedProviders;
  }

  protected static final boolean isSelectable(final Qualifiers contextQualifiers,
                                              final Path contextPath,
                                              final Qualifiers valuePathQualifiers,
                                              final Path valuePath) {
    if (AssignableType.of(contextPath.type()).isAssignable(valuePath.type())) {
      if (contextQualifiers.isEmpty() ||
          valuePathQualifiers.isEmpty() ||
          contextQualifiers.intersectionSize(valuePathQualifiers) > 0) {
        // TODO: this is just endsWith()
        final int lastIndex = contextPath.lastIndexOf(valuePath);
        return lastIndex >= 0 && lastIndex + valuePath.size() == contextPath.size();
      }
    }
    return false;
  }

  private static final <T> T fail() {
    throw new UnsupportedOperationException();
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

}
