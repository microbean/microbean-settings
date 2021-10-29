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

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.microbean.settings.api.Provider.Value;

// Could not be any more experimental.
public class Settings {


  /*
   * Static fields.
   */


  private static final AtomicReference<Settings> instance = new AtomicReference<>();


  /*
   * Instance fields.
   */


  private final ConcurrentMap<Qualified<Context<?>>, Supplier<?>> supplierMap;

  private final List<Provider> providers;

  private Qualifiers qualifiers;


  /*
   * Constructors.
   */


  public Settings() {
    this(loadedProviders());
  }

  public Settings(final Collection<? extends Provider> providers) {
    super();
    this.supplierMap = new ConcurrentHashMap<>();
    this.providers = List.copyOf(providers);
    this.qualifiers = Qualifiers.of();
    try {
      this.qualifiers = this.get(Context.of(Qualifiers.class), this::qualifiers);
    } finally {
      this.supplierMap.clear();
    }
  }

  public Settings(final Qualifiers qualifiers) {
    this(loadedProviders(), qualifiers);
  }

  public Settings(final Collection<? extends Provider> providers, final Qualifiers qualifiers) {
    super();
    this.supplierMap = new ConcurrentHashMap<>();
    this.providers = List.copyOf(providers);
    this.qualifiers = Objects.requireNonNull(qualifiers, "qualifiers");
  }


  /*
   * Instance methods.
   */


  public final Qualifiers qualifiers() {
    return this.qualifiers;
  }

  public final Collection<Provider> providers() {
    return this.providers;
  }

  public final <T> T get(final Class<T> cls,
                         final Supplier<? extends T> defaultSupplier) {
    return this.get(Context.of(cls), defaultSupplier);
  }

  public final <T> T get(final Type type,
                         final Supplier<? extends T> defaultSupplier) {
    return this.get(Context.of(type), defaultSupplier);
  }

  public final <T> T get(final Path path,
                         final Supplier<? extends T> defaultSupplier) {
    return this.get(Context.of(path), defaultSupplier);
  }

  public final <T> T get(final Context<?> context,
                         final Supplier<? extends T> defaultSupplier) {
    return this.get(this.qualifiers, context, Settings::sink, Settings::sink, Settings::sink, defaultSupplier);
  }

  public final <T> T get(final Context<?> context,
                         final Consumer<? super Provider> rejectedProviders,
                         final Consumer<? super Value<?>> rejectedValues,
                         final Consumer<? super Value<?>> ambiguousValues,
                         final Supplier<? extends T> defaultSupplier) {
    return this.get(this.qualifiers, context, rejectedProviders, rejectedValues, ambiguousValues, defaultSupplier);
  }

  @SuppressWarnings("unchecked")
  public final <T> T get(final Qualifiers contextQualifiers,
                         final Context<?> context,
                         final Consumer<? super Provider> rejectedProviders,
                         final Consumer<? super Value<?>> rejectedValues,
                         final Consumer<? super Value<?>> ambiguousValues,
                         final Supplier<? extends T> defaultSupplier) {
    return
      (T)this.supplierMap.computeIfAbsent(Qualified.Record.of(contextQualifiers, context),
                                          qc -> this.computeSupplier(qc,
                                                                     rejectedProviders,
                                                                     rejectedValues,
                                                                     ambiguousValues,
                                                                     defaultSupplier))
      .get();
  }

  private final <T> Supplier<? extends T> computeSupplier(final Qualified<Context<?>> qualifiedContext,
                                                          final Consumer<? super Provider> rejectedProviders,
                                                          final Consumer<? super Value<?>> rejectedValues,
                                                          final Consumer<? super Value<?>> ambiguousValues,
                                                          final Supplier<? extends T> defaultSupplier) {
    final Supplier<? extends T> supplier =
      this.resolve(qualifiedContext.qualifiers(),
                   qualifiedContext.qualified(),
                   rejectedProviders,
                   rejectedValues,
                   ambiguousValues);
    if (supplier == null) {
      return defaultSupplier == null ? Settings::throwUnsupported : defaultSupplier;
    } else {
      return supplier;
    }
  }

  protected <T> Value<T> resolve(final Qualifiers contextQualifiers,
                                 final Context<?> context,
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
      } else if (!isSelectable(contextQualifiers, context, provider)) {
        rejectedProviders.accept(provider);
        return null;
      } else {
        final Value<?> value = provider.get(contextQualifiers, context);
        if (value == null) {
          return null;
        } else if (!isSelectable(contextQualifiers, context, value)) {
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
      int candidatePathSize = Integer.MIN_VALUE;
      int candidateQualifiersScore = Integer.MIN_VALUE;
      for (final Provider provider : providers) {
        if (provider != null && isSelectable(contextQualifiers, context, provider)) {
          Value<?> value = provider.get(contextQualifiers, context);
          while (value != null) { // NOTE
            if (isSelectable(contextQualifiers, context.path(), value)) {
              if (candidate == null) {
                candidate = value;
                candidateProvider = provider;
                candidatePathSize = candidate.path().size();
                candidateQualifiersScore = this.score(contextQualifiers, candidate.qualifiers());
                value = null;
              } else {
                final Path valuePath = value.path();
                // Let's do qualifiers first.  This is an arbitrary decision.
                final int valueQualifiersScore = this.score(contextQualifiers, value.qualifiers());
                if (valueQualifiersScore < candidateQualifiersScore) {
                  rejectedValues.accept(value);
                  value = null;
                } else if (valueQualifiersScore == candidateQualifiersScore) {
                  final int valuePathSize = valuePath.size();
                  if (valuePathSize < candidatePathSize) {
                    rejectedValues.accept(value);
                    value = null;
                  } else if (valuePathSize == candidatePathSize) {
                    final Value<?> disambiguatedValue =
                      this.disambiguate(contextQualifiers, context, candidateProvider, candidate, provider, value);
                    if (disambiguatedValue == null) {
                      ambiguousValues.accept(candidate);
                      ambiguousValues.accept(value);
                      value = null;
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
                      value = null;
                    } else if (disambiguatedValue.equals(value)) {
                      rejectedValues.accept(candidate);
                      candidate = disambiguatedValue;
                      candidateProvider = provider;
                      candidatePathSize = valuePathSize;
                      candidateQualifiersScore = valueQualifiersScore;
                      value = null;
                    } else {
                      value = disambiguatedValue; // NOTE; this will run through the while loop again
                    }
                  } else {
                    rejectedValues.accept(candidate);
                    candidate = value;
                    candidateProvider = provider;
                    candidatePathSize = valuePathSize;
                    candidateQualifiersScore = valueQualifiersScore;
                    value = null;
                  }
                } else {
                  rejectedValues.accept(candidate);
                  candidate = value;
                  candidateProvider = provider;
                  candidatePathSize = valuePath.size();
                  candidateQualifiersScore = valueQualifiersScore;
                  value = null;
                }
              }
            } else {
              rejectedValues.accept(value);
              value = null;
            }
          }
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
    return contextQualifiers.relativeScore(valueQualifiers);
  }

  protected Value<?> disambiguate(final Qualifiers contextQualifiers,
                                  final Context<?> context,
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
  
  public static final Settings get() {
    Settings returnValue = instance.get();
    if (returnValue == null) {
      final Settings bootstrapSettings = new Settings();
      instance.compareAndSet(null, bootstrapSettings.get(Settings.class, () -> bootstrapSettings));
      returnValue = instance.get();
      assert returnValue != null;
    }
    return returnValue;
  }

  protected static final boolean isSelectable(final Qualifiers contextQualifiers,
                                              final Context<?> context,
                                              final Provider provider) {
    return
      context.isAssignable(provider.upperBound()) &&
      provider.isSelectable(contextQualifiers, context);
  }

  protected static final boolean isSelectable(final Qualifiers contextQualifiers,
                                              final Context<?> context,
                                              final Value<?> value) {
    return
      isSelectable(contextQualifiers,
                   context.path(),
                   value.qualifiers(),
                   value.path());
  }

  protected static final boolean isSelectable(final Qualifiers contextQualifiers,
                                              final Path contextPath,
                                              final Value<?> value) {
    return
      isSelectable(contextQualifiers,
                   contextPath,
                   value.qualifiers(),
                   value.path());
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
