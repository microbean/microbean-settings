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
import java.util.Map;
import java.util.ServiceLoader;

import java.util.function.Supplier;

import org.microbean.settings.api.Provider.Value;

@Deprecated(forRemoval = true)
public final class Configured {


  /*
   * Constructors.
   */


  private Configured() {
    super();
  }


  /*
   * Static methods.
   */


  public static final List<Provider> loadedProviders() {
    return LoadedProviders.loadedProviders;
  }

  public static final List<Provider> loadedProviders(final Context context) {
    return loadedProviders()
      .stream()
      .filter(p -> isSelectable(p, context))
      .toList();
  }

  private static final boolean isSelectable(final Provider provider, final Context context) {
    return context.type().isAssignable(provider.upperBound()) && provider.isSelectable(context);
  }

  public static final <T> T of(final Qualified<Type> target) {
    return of(target, null);
  }
  
  public static final <T> T of(final Qualified<Type> target, final Supplier<? extends T> defaultTargetSupplier) {
    return of(Qualifiers.of(), target, defaultTargetSupplier);
  }

  public static final <T> T of(final Qualifiers rootQualifiers, final Qualified<Type> target) {
    return of(rootQualifiers, target, null);
  }
  
  public static final <T> T of(final Qualifiers rootQualifiers, final Qualified<Type> target, final Supplier<? extends T> defaultTargetSupplier) {
    return of(Path.root(rootQualifiers), target, defaultTargetSupplier);
  }

  public static final <T> T of(final Path parentPath, final Qualified<Type> target, final Supplier<? extends T> defaultTargetSupplier) {
    final T returnValue;
    final Context context = new Context(parentPath, target);
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
      final T rv = value == null ? null : (T)value.get();
      returnValue = rv;
    }
    return returnValue;
  }

  public static final Value<?> resolve(final Collection<? extends Provider> providers, final Context context) {
    switch (providers.size()) {
    case 0:
      break;
    case 1:
      final Provider provider = providers instanceof List<? extends Provider> list ? list.get(0) : providers.iterator().next();
      return provider.isSelectable(context) ? provider.get(context) : null;
    default:
      Collection<Value<?>> bad = null;
      Value<?> candidate = null;
      final Path contextPath = context.path();
      for (final Provider p : providers) {
        if (p.isSelectable(context)) {
          final Value<?> v = p.get(context);
          if (v != null) {
            final Path valuePath = v.path();
            if (contextPath.isAssignable(valuePath.type())) {
              if (candidate == null) {
                candidate = v;
              } else {
                switch (Integer.signum(Path.SpecificityComparator.INSTANCE.compare(valuePath, candidate.path()))) {
                case -1:
                  // The valuePath is less specific than the candidate path.
                  break;
                case 0:
                  // The two paths are equal.
                  break;
                case 1:
                  // The valuePath is more specific than the candidate path.
                  break;
                default:
                  throw new AssertionError();
                }
              }
            }
          }
        }
      }
    }
    throw new UnsupportedOperationException("TODO: Not yet fully implemented");
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
