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

public class Settings<T> implements ConfiguredSupplier<T> {


  /*
   * Instance fields.
   */


  private final BiFunction<? super Qualified.Record<Path>, Function<? super Qualified.Record<Path>, ? extends Settings<?>>, ? extends Settings<?>> settingsCache;

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


  @Deprecated // intended for use by ServiceLoader only
  public Settings() {
    this(new ConcurrentHashMap<Qualified.Record<Path>, Settings<?>>()::computeIfAbsent,
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
  private Settings(final BiFunction<? super Qualified.Record<Path>, Function<? super Qualified.Record<Path>, ? extends Settings<?>>, ? extends Settings<?>> settingsCache,
                   final Collection<? extends Provider> providers,
                   final Qualifiers qualifiers,
                   final ConfiguredSupplier<?> parent,
                   final Path path,
                   final Supplier<T> supplier,
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
      if (path == null || path.equals(Path.of())) {
        this.path = Path.of();
        this.supplier = supplier == null ? () -> (T)this : supplier;
        this.parent = this;
      } else {
        throw new IllegalArgumentException("path: " + path);
      }
    } else if (path == null) {
      this.path = Path.of();
      this.supplier = Objects.requireNonNull(supplier, "supplier");
      this.parent = parent;
    } else {
      this.path = path;
      this.supplier = Objects.requireNonNull(supplier, "supplier");
      this.parent = parent;
    }
    // While the following call is in effect, our qualifiers
    // instance field will be null.  Note that the qualifiers()
    // method accounts for this and will return Qualifiers.of()
    // instead.
    this.qualifiers = qualifiers == null ? this.plus(Qualifiers.class, Qualifiers::of).get() : qualifiers;
  }


  /*
   * Instance methods.
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
                                  final Path path,
                                  final Supplier<U> defaultSupplier) {
    return
      this.of(parent,
              path, // NOTE: no plus()
              defaultSupplier,
              this.rejectedProvidersConsumerSupplier.get(),
              this.rejectedValuesConsumerSupplier.get(),
              this.ambiguousValuesConsumerSupplier.get());
  }

  @SuppressWarnings("unchecked")
  private final <U> Settings<U> of(final ConfiguredSupplier<?> parent,
                                   final Path path,
                                   final Supplier<U> defaultSupplier,
                                   final Consumer<? super Provider> rejectedProviders,
                                   final Consumer<? super Value<?>> rejectedValues,
                                   final Consumer<? super Value<?>> ambiguousValues) {
    Objects.requireNonNull(parent, "parent");
    if (Path.of().equals(path)) {
      throw new IllegalArgumentException("path: " + path);
    }
    return
      (Settings<U>)this.settingsCache.apply(Qualified.Record.of(parent.qualifiers(), path),
                                            qp -> this.computeSettings(parent,
                                                                       qp.qualified(),
                                                                       defaultSupplier,
                                                                       rejectedProviders,
                                                                       rejectedValues,
                                                                       ambiguousValues));
  }

  private final <U> Settings<U> computeSettings(final ConfiguredSupplier<?> parent,
                                                final Path path,
                                                final Supplier<U> defaultSupplier,
                                                final Consumer<? super Provider> rejectedProviders,
                                                final Consumer<? super Value<?>> rejectedValues,
                                                final Consumer<? super Value<?>> ambiguousValues) {
    final Value<U> value = this.value(parent, path, rejectedProviders, rejectedValues, ambiguousValues);
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
      } else if (!this.isSelectable(parent, path, provider)) {
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
      Value<?> candidate = null;
      Provider candidateProvider = null;
      int candidateQualifiersScore = Integer.MIN_VALUE;
      int candidatePathScore = Integer.MIN_VALUE;
      for (final Provider provider : providers) {
        if (provider != null && this.isSelectable(parent, path, provider)) {
          Value<?> value = provider.get(parent, path);
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

  protected final boolean isSelectable(final ConfiguredSupplier<?> parent,
                                       final Path path,
                                       final Provider provider) {
    return
      AssignableType.of(provider.upperBound()).isAssignable(path.type()) &&
      provider.isSelectable(parent, path);
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

  protected Value<?> disambiguate(final ConfiguredSupplier<?> parent,
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
