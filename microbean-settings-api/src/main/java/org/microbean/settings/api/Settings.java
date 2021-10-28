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

import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.microbean.settings.api.Provider.Value;

// Could not be any more experimental.
public class Settings {

  private final ConcurrentMap<Qualified<Context<?>>, Supplier<?>> supplierMap;

  private final List<Provider> providers;

  private final Resolver resolver;

  private final Qualifiers qualifiers;

  private final Disambiguator disambiguator;

  public Settings(final Resolver resolver,
                  final Qualifiers qualifiers,
                  final Disambiguator disambiguator) {
    this(LoadedProviders.loadedProviders,
         resolver,
         qualifiers,
         disambiguator);
  }
  
  public Settings(final Collection<? extends Provider> providers,
                  final Resolver resolver,
                  final Qualifiers qualifiers,
                  final Disambiguator disambiguator) {
    super();
    this.supplierMap = new ConcurrentHashMap<>();
    this.providers = List.copyOf(providers);
    this.resolver = resolver == null ? Resolver.DEFAULT : resolver;
    this.qualifiers = qualifiers == null ? Qualifiers.of() : qualifiers;
    this.disambiguator = disambiguator == null ? Disambiguator.DEFAULT : disambiguator;
  }

  @SuppressWarnings("unchecked")
  public final <T> T get(final Context<?> context,
                         final Consumer<? super Provider> rejectedProviders,
                         final Consumer<? super Value<?>> rejectedValues,
                         final Consumer<? super Value<?>> ambiguousValues,
                         final Supplier<? extends T> defaultSupplier) {
    return
      (T)this.supplierMap.computeIfAbsent(Qualified.Record.of(this.qualifiers,
                                                              context),
                                          qc -> this.supplier(qc,
                                                              rejectedProviders,
                                                              rejectedValues,
                                                              ambiguousValues,
                                                              defaultSupplier))
      .get();
  }

  private final <T> Supplier<? extends T> supplier(final Qualified<Context<?>> qc,
                                                   final Consumer<? super Provider> rejectedProviders,
                                                   final Consumer<? super Value<?>> rejectedValues,
                                                   final Consumer<? super Value<?>> ambiguousValues,
                                                   final Supplier<? extends T> defaultSupplier) {
    Supplier<? extends T> supplier =
      this.resolver.resolve(this.providers,
                            qc.qualifiers(),
                            qc.qualified(),
                            rejectedProviders,
                            rejectedValues,
                            this.disambiguator,
                            ambiguousValues);
    if (supplier == null) {
      if (defaultSupplier == null) {
        supplier = () -> { throw new UnsupportedOperationException(); };
      } else {
        supplier = defaultSupplier;
      }
    }
    return supplier;
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
