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

import java.util.function.Supplier;

import org.microbean.settings.api.Provider.Value;

public final class Configured<T> {

  private Configured() {
    super();
  }

  public static final List<Provider> loadedProviders() {
    return LoadedProviders.loadedProviders;
  }

  public static final List<Provider> loadedProviders(final Qualified<? extends Context> context) {
    return loadedProviders()
      .stream()
      .filter(p -> isSelectable(p, context))
      .toList();
  }
  
  public static final <T> T of(final Qualifiers qualifiers, final Path path, final Supplier<? extends T> defaultTargetSupplier) {
    final T returnValue;
    final Qualified<? extends Context> context = Qualified.Record.of(qualifiers, new Context(path));
    final Collection<? extends Provider> providers = loadedProviders(context);
    if (providers.isEmpty()) {
      if (defaultTargetSupplier == null) {
        throw new UnsupportedOperationException();
      } else {
        returnValue = defaultTargetSupplier.get();
      }
    } else {
      final Value<?> value = resolve(providers, context);
      @SuppressWarnings("unchecked")
      final T rv = value == null ? null : (T)value.value();
      returnValue = rv;
    }
    return returnValue;
  }

  public static final Value<?> resolve(final Collection<? extends Provider> providers, final Qualified<? extends Context> context) {
    switch (providers.size()) {
    case 0:
      return null;
    case 1:
      final Provider provider = providers instanceof List<? extends Provider> list ? list.get(0) : providers.iterator().next();
      return provider.isSelectable(context) ? provider.get(context) : null;
    default:
    }
    throw new UnsupportedOperationException("TODO: Not yet fully implemented");
  }
  
  private static final boolean isSelectable(final Provider provider, final Qualified<? extends Context> context) {
    return AssignableType.of(context.qualified().path().type()).isAssignable(provider.upperBound()) && provider.isSelectable(context);
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
