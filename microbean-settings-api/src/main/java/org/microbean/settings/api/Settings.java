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

import java.util.concurrent.atomic.AtomicReference;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.microbean.settings.api.Provider.Value;

// Could not be any more experimental.  The <T> is because I'm
// currently making Settings usurp Context<T>.  It could be <Void> to
// start.
public class Settings<T> implements SupplierBroker {


  /*
   * Static fields.
   */


  private static final AtomicReference<Settings<?>> instance = new AtomicReference<>();


  /*
   * Instance fields.
   */


  private final BiFunction<? super Qualified.Record<Path>, Function<? super Qualified.Record<Path>, ? extends Supplier<?>>, ? extends Supplier<?>> supplierCache;

  private final List<Provider> providers;

  // I'm in the process of making Settings usurp Context; hence the
  // three following currently useless fields.
  private final Qualifiers qualifiers;

  private final Supplier<?> parentSupplier;

  private final Path path;


  /*
   * Constructors.
   */


  public Settings() {
    this(loadedProviders());
  }

  public Settings(final Collection<? extends Provider> providers) {
    this(new ConcurrentHashMap<Qualified.Record<Path>, Supplier<?>>()::computeIfAbsent, providers);
  }

  public Settings(final BiFunction<? super Qualified.Record<Path>, Function<? super Qualified.Record<Path>, ? extends Supplier<?>>, ? extends Supplier<?>> supplierCache,
                  final Collection<? extends Provider> providers) {
    super();
    this.supplierCache = Objects.requireNonNull(supplierCache, "supplierCache");
    this.providers = List.copyOf(providers);
    this.parentSupplier = Settings::returnNull;
    this.path = Path.of();
    this.qualifiers =
      this.supplier(this,
                    Qualifiers.of(),
                    this.parentSupplier,
                    this.path().plus(Accessor.of("supplier"), Qualifiers.class),
                    Settings::sink,
                    Settings::sink,
                    Settings::sink,
                    Qualifiers::of).get();
  }

  public Settings(final BiFunction<? super Qualified.Record<Path>, Function<? super Qualified.Record<Path>, ? extends Supplier<?>>, ? extends Supplier<?>> supplierCache,
                  final Collection<? extends Provider> providers,
                  final Qualifiers qualifiers) {
    this(supplierCache, providers, qualifiers, Settings::returnNull);
  }

  public Settings(final BiFunction<? super Qualified.Record<Path>, Function<? super Qualified.Record<Path>, ? extends Supplier<?>>, ? extends Supplier<?>> supplierCache,
                  final Collection<? extends Provider> providers,
                  final Qualifiers qualifiers,
                  final Supplier<?> parentSupplier) {
    this(supplierCache, providers, qualifiers, Settings::returnNull, null);
  }

  public Settings(final BiFunction<? super Qualified.Record<Path>, Function<? super Qualified.Record<Path>, ? extends Supplier<?>>, ? extends Supplier<?>> supplierCache,
                  final Collection<? extends Provider> providers,
                  final Qualifiers qualifiers,
                  final Supplier<?> parentSupplier,
                  final Path path) {
    super();
    this.supplierCache = Objects.requireNonNull(supplierCache, "supplierCache");
    this.providers = List.copyOf(providers);
    this.qualifiers = Objects.requireNonNull(qualifiers, "qualifiers");
    this.parentSupplier = parentSupplier == null ? Settings::returnNull : parentSupplier;
    this.path = path;
  }


  /*
   * Instance methods.
   */


  public final Collection<Provider> providers() {
    return this.providers;
  }

  public final Qualifiers qualifiers() {
    return this.qualifiers;
  }

  public final Supplier<?> parentSupplier() {
    return this.parentSupplier;
  }

  public final Path path() {
    return this.path;
  }

  public final T get(final Supplier<T> defaultSupplier) {
    return
      this.supplier(this,
                    this.qualifiers(),
                    this.parentSupplier(),
                    this.path(),
                    Settings::sink,
                    Settings::sink,
                    Settings::sink,
                    defaultSupplier)
      .get();
  }

  @Override // SupplierBroker
  @SuppressWarnings("unchecked")
  public final <T> Supplier<T> supplier(final SupplierBroker broker,
                                        final Qualifiers qualifiers,
                                        final Supplier<?> parentSupplier,
                                        final Path path,
                                        final Consumer<? super Provider> rejectedProviders,
                                        final Consumer<? super Value<?>> rejectedValues,
                                        final Consumer<? super Value<?>> ambiguousValues,
                                        final Supplier<T> defaultSupplier) {
    return
      (Supplier<T>)this.supplierCache.apply(Qualified.Record.of(qualifiers, path),
                                            qp -> this.computeSupplier(broker,
                                                                       qp.qualifiers(),
                                                                       parentSupplier,
                                                                       qp.qualified(),
                                                                       rejectedProviders,
                                                                       rejectedValues,
                                                                       ambiguousValues,
                                                                       defaultSupplier));
  }

  private final <T> Supplier<T> computeSupplier(final SupplierBroker broker,
                                                final Qualifiers qualifiers,
                                                final Supplier<?> parentSupplier,
                                                final Path path,
                                                final Consumer<? super Provider> rejectedProviders,
                                                final Consumer<? super Value<?>> rejectedValues,
                                                final Consumer<? super Value<?>> ambiguousValues,
                                                final Supplier<T> defaultSupplier) {
    final Value<T> value = this.value(broker, qualifiers, parentSupplier, path, rejectedProviders, rejectedValues, ambiguousValues);
    if (value == null) {
      return defaultSupplier == null ? Settings::throwUnsupported : defaultSupplier;
    } else {
      return value;
    }
  }

  private final <T> Value<T> value(final SupplierBroker broker,
                                   final Qualifiers qualifiers,
                                   final Supplier<?> parentSupplier,
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
      } else if (!isSelectable(broker, qualifiers, parentSupplier, path, provider)) {
        rejectedProviders.accept(provider);
        return null;
      } else {
        final Value<?> value = provider.get(broker, qualifiers, parentSupplier, path);
        if (value == null) {
          return null;
        } else if (!isSelectable(qualifiers, path, value.qualifiers(), value.path())) {
          rejectedValues.accept(value);
          return null;
        } else {
          @SuppressWarnings("unchecked")
          final Value<T> v = (Value<T>)value;
          return v;
        }
      }
    } else {
      Value<?> candidate = null;
      Provider candidateProvider = null;
      int candidateQualifiersScore = Integer.MIN_VALUE;
      for (final Provider provider : providers) {
        if (provider != null && isSelectable(broker, qualifiers, parentSupplier, path, provider)) {
          Value<?> value = provider.get(broker, qualifiers, parentSupplier, path);
          VALUE_EVALUATION_LOOP:
          while (value != null) { // NOTE INFINITE LOOP POSSIBILITY; read carefully
            if (isSelectable(qualifiers, path, value.qualifiers(), value.path())) {
              if (candidate == null) {
                candidate = value;
                candidateProvider = provider;
                candidateQualifiersScore = this.score(qualifiers, candidate.qualifiers());
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
                  final int valuePathSize = valuePath.size();
                  if (valuePathSize < candidate.path().size()) {
                    rejectedValues.accept(value);
                    value = null; // critical
                  } else if (valuePathSize == candidate.path().size()) {
                    final Value<?> disambiguatedValue =
                      this.disambiguate(broker, qualifiers, parentSupplier, path, candidateProvider, candidate, provider, value);
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
                    value = null; // critical
                  }
                } else {
                  rejectedValues.accept(candidate);
                  candidate = value;
                  candidateProvider = provider;
                  candidateQualifiersScore = valueQualifiersScore;
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
      final Value<T> c = (Value<T>)candidate;
      return c;
    }
  }

  protected int score(final Qualifiers contextQualifiers, final Qualifiers valueQualifiers) {
    final int intersectionSize = contextQualifiers.intersectionSize(valueQualifiers);
    if (intersectionSize > 0) {
      if (intersectionSize == valueQualifiers.size()) {
        assert contextQualifiers.equals(valueQualifiers);
        return intersectionSize;
      } else {
        return intersectionSize - contextQualifiers.symmetricDifferenceSize(valueQualifiers);
      }
    } else {
      assert intersectionSize == 0;
      return -(contextQualifiers.size() + valueQualifiers.size());
    }
  }

  protected Value<?> disambiguate(final SupplierBroker broker,
                                  final Qualifiers qualifiers,
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

  public static final Settings<?> instance() {
    Settings<?> returnValue = instance.get();
    if (returnValue == null) {
      final Settings<?> bootstrapSettings = new Settings<>();
      instance.compareAndSet(null,
                             bootstrapSettings.supplier(bootstrapSettings,
                                                        bootstrapSettings.qualifiers(),
                                                        bootstrapSettings.parentSupplier(),
                                                        bootstrapSettings.path().plus(Accessor.of("supplier"), Qualifiers.class),
                                                        Settings::sink,
                                                        Settings::sink,
                                                        Settings::sink,
                                                        () -> bootstrapSettings)
                             .get());
      returnValue = instance.get();
      assert returnValue != null;
    }
    return returnValue;
  }

  protected static final boolean isSelectable(final SupplierBroker broker,
                                              final Qualifiers qualifiers,
                                              final Supplier<?> parentSupplier,
                                              final Path path,
                                              final Provider provider) {
    return path.isAssignable(provider.upperBound()) && provider.isSelectable(broker, qualifiers, parentSupplier, path);
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

  private static final <T> T throwUnsupported() {
    throw new UnsupportedOperationException();
  }

  private static final <T> T returnNull() {
    return null;
  }

  private static final void sink(final Object ignored) {

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
