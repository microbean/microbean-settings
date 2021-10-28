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

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.microbean.settings.api.Provider.Value;

public final class Configured {
  
  private Configured() {
    super();
  }

  public static final List<Provider> loadedProviders() {
    return LoadedProviders.loadedProviders;
  }

  public static final <T> T of(final Collection<? extends Provider> providers,
                               final Qualifiers contextQualifiers,
                               final Context<?> context,
                               Resolver resolver,
                               final Consumer<? super Provider> rejectedProviders,
                               final Consumer<? super Value<?>> rejectedValues,
                               final Consumer<? super Value<?>> ambiguousValues,
                               final Disambiguator disambiguator,
                               final Supplier<? extends T> defaultTargetSupplier) {
    final T returnValue;
    if (resolver == null) {
      resolver = Resolver.DEFAULT;
    }
    final Value<T> value = resolver.resolve(providers, contextQualifiers, context, rejectedProviders, rejectedValues, disambiguator, ambiguousValues);
    if (value == null) {
      if (defaultTargetSupplier == null) {
        throw new UnsupportedOperationException();
      } else {
        returnValue = defaultTargetSupplier.get();
      }
    } else {
      returnValue = value.value();
    }
    return returnValue;
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
