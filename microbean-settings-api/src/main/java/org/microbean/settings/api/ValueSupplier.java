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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;

import java.util.function.Function;
import java.util.function.Supplier;

import org.microbean.development.annotation.Convenience;

public interface ValueSupplier {


  /*
   * Abstract methods.
   */


  public boolean respondsFor(final QualifiedPath qualifiedPath);

  public Value<?> get(final QualifiedPath qualifiedPath,
                      final Function<? super QualifiedPath, ? extends Collection<ValueSupplier>> valueSuppliers);


  /*
   * Static methods.
   */


  public static Value<?> resolve(final Collection<? extends ValueSupplier> valueSuppliers,
                                 final QualifiedPath qualifiedPath,
                                 final Function<? super QualifiedPath, ? extends Collection<ValueSupplier>> valueSuppliersFunction) {
    final Value<?> returnValue;
    if (valueSuppliers == null || valueSuppliers.isEmpty()) {
      returnValue = null;
    } else {
      Collection<Value<?>> bad = null;
      Value<?> candidate = null;
      for (final ValueSupplier valueSupplier : valueSuppliers) {
        if (valueSupplier != null && valueSupplier.respondsFor(qualifiedPath)) {
          final Value<?> v = valueSupplier.get(qualifiedPath, valueSuppliersFunction);
          if (v != null) {
            final Map<?, ?> qualifiers = v.qualifiers();
            if (Qualifiers.isAssignable(qualifiedPath.applicationQualifiers(), qualifiers)) {
              if (candidate == null || qualifiers.size() > candidate.qualifiers().size()) {
                candidate = v;
              }
            } else {
              if (bad == null) {
                bad = new ArrayList<>(5);
                if (candidate != null) {
                  bad.add(candidate);
                }
              }
              bad.add(v);
            }
          }
        }
      }
      if (bad != null) {
        throw new IllegalStateException("bad or conflicting ValueSuppliers: " + bad);
      }
      returnValue = candidate;
    }
    return returnValue;
  }

  public static List<ValueSupplier> loadedValueSuppliers() {
    return Value.loadedValueSuppliers.get(ValueSupplier.class);
  }

  public static List<ValueSupplier> loadedValueSuppliers(final QualifiedPath qp) {
    return loadedValueSuppliers().stream()
      .filter(vs -> vs.respondsFor(qp))
      .toList();
  }

  public static void clearLoadedValueSuppliers() {
    Value.loadedValueSuppliers.remove(ValueSupplier.class);
  }


  /*
   * Inner and nested classes.
   */


  public static final record Value<T>(T value, Path path, Map<?, ?> qualifiers) implements Supplier<T> {

    // Ideally this would live in ValueSupplier directly but
    // interfaces cannot have private static members.
    private static final ClassValue<List<ValueSupplier>> loadedValueSuppliers = new ClassValue<>() {
        @Override
        protected final List<ValueSupplier> computeValue(final Class<?> c) {
          return
          ValueSupplier.class == c ? 
          ServiceLoader.load(ValueSupplier.class, ValueSupplier.class.getClassLoader())
          .stream()
          .map(ServiceLoader.Provider::get)
          .sorted(Prioritized.COMPARATOR_DESCENDING)
          .toList() :
          null;
        }
      };
    
    public Value {
      path = Objects.requireNonNull(path, "path");
      qualifiers = qualifiers == null ? Map.of() : Map.copyOf(qualifiers);
    }

    @Override // Supplier
    public final T get() {
      return this.value();
    }

  }

}
