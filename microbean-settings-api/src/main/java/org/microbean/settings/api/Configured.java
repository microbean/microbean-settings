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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.microbean.settings.api.Provider.Value;

public final class Configured {
  
  private Configured() {
    super();
  }

  public static final Qualifiers qualifiers() {
    return qualifiers(Resolver.DEFAULT);
  }
  
  public static final Qualifiers qualifiers(final Resolver resolver) {
    return get(Qualifiers.of(), Context.of(Qualifiers.class), resolver, Qualifiers::of);
  }

  public static final <T> T get(final Type type,
                                final Supplier<? extends T> defaultSupplier) {
    return
      get(Context.of(type),
          defaultSupplier);
  }    

  public static final <T> T get(final Context<?> context,
                                final Supplier<? extends T> defaultSupplier) {
    return
      get(qualifiers(Resolver.DEFAULT),
          context,
          Resolver.DEFAULT,
          defaultSupplier);
  }

  public static final <T> T get(final Context<?> context,
                                final Resolver resolver,
                                final Supplier<? extends T> defaultSupplier) {
    return
      get(qualifiers(resolver),
          context,
          resolver,
          defaultSupplier);
  }

  public static final <T> T get(final Qualifiers contextQualifiers,
                                final Context<?> context,
                                final Supplier<? extends T> defaultSupplier) {
    return
      get(contextQualifiers,
          context,
          Resolver.DEFAULT,
          defaultSupplier);
  }
  
  public static final <T> T get(final Qualifiers contextQualifiers,
                                final Context<?> context,
                                final Resolver resolver,
                                final Supplier<? extends T> defaultSupplier) {
    final Collection<Provider> rejectedProviders = new ArrayList<>();
    final Collection<Value<?>> rejectedValues = new ArrayList<>();
    final Collection<Value<?>> ambiguousValues = new ArrayList<>();
    final T object =
      get(loadedProviders(),
          contextQualifiers,
          context,
          resolver,
          rejectedProviders::add,
          rejectedValues::add,
          ambiguousValues::add,
          Disambiguator.DEFAULT,
          defaultSupplier);
    if (ambiguousValues.isEmpty()) {
      return object;
    } else {
      throw new AmbiguousConfigurationException(Collections.unmodifiableCollection(ambiguousValues));
    }
  }
  
  public static final <T> T get(final Qualifiers contextQualifiers,
                                final Context<?> context,
                                final Resolver resolver,
                                final Consumer<? super Provider> rejectedProviders,
                                final Consumer<? super Value<?>> rejectedValues,
                                final Consumer<? super Value<?>> ambiguousValues,
                                final Disambiguator disambiguator,
                                final Supplier<? extends T> defaultSupplier) {
    return
      get(loadedProviders(),
          contextQualifiers,
          context,
          resolver,
          rejectedProviders,
          rejectedValues,
          ambiguousValues,
          disambiguator,
          defaultSupplier);
  }
  
  public static final <T> T get(final Collection<? extends Provider> providers,
                                final Qualifiers contextQualifiers,
                                final Context<?> context,
                                final Resolver resolver,
                                final Consumer<? super Provider> rejectedProviders,
                                final Consumer<? super Value<?>> rejectedValues,
                                final Consumer<? super Value<?>> ambiguousValues,
                                final Disambiguator disambiguator,
                                final Supplier<? extends T> defaultSupplier) {
    final T returnValue;
    final Value<T> value =
      (resolver == null ? Resolver.DEFAULT : resolver).resolve(providers,
                                                               contextQualifiers,
                                                               context,
                                                               rejectedProviders,
                                                               rejectedValues,
                                                               disambiguator,
                                                               ambiguousValues);
    if (value == null) {
      if (defaultSupplier == null) {
        throw new UnsupportedOperationException();
      } else {
        returnValue = defaultSupplier.get();
      }
    } else {
      returnValue = value.value();
    }
    return returnValue;
  }

  public static final List<Provider> loadedProviders() {
    return LoadedProviders.loadedProviders;
  }
  
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
