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
import java.util.Map;
import java.util.Objects;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.microbean.development.annotation.Convenience;

public interface ValueSupplier {

  public default boolean respondsFor(final Path path, final Map<?, ?> applicationQualifiers) {
    return true;
  }
  
  public <T> Value<T> get(final Path path,
                          final Map<?, ?> applicationQualifiers,
                          final BiFunction<? super Path, ? super Map<?, ?>, ? extends Collection<ValueSupplier>> valueSuppliers);

  public static Value<?> resolve(final Collection<? extends ValueSupplier> valueSuppliers,
                                 final Path path,
                                 final Map<?, ?> applicationQualifiers,
                                 final BiFunction<? super Path, ? super Map<?, ?>, ? extends Collection<ValueSupplier>> valueSuppliersFunction) {
    final Value<?> returnValue;
    if (valueSuppliers == null || valueSuppliers.isEmpty()) {
      returnValue = null;
    } else {
      Collection<Value<?>> bad = null;
      Value<?> candidate = null;
      for (final ValueSupplier valueSupplier : valueSuppliers) {
        if (valueSupplier != null && valueSupplier.respondsFor(path, applicationQualifiers)) {
          final Value<?> v = valueSupplier.get(path, applicationQualifiers, valueSuppliersFunction);
          if (v != null) {
            final Map<?, ?> qualifiers = v.qualifiers();
            if (viable(applicationQualifiers, qualifiers)) {
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

  private static boolean viable(final Map<?, ?> applicationQualifiers, final Map<?, ?> vsQualifiers) {
    final boolean returnValue;
    if (applicationQualifiers.isEmpty()) {
      returnValue = vsQualifiers == null || vsQualifiers.isEmpty();
    } else if (vsQualifiers == null || vsQualifiers.isEmpty()) {
      returnValue = false;
    } else {
      returnValue = applicationQualifiers.entrySet().containsAll(vsQualifiers.entrySet());
    }
    return returnValue;
  }

  public static final record Value<T>(T value, Path path, Map<?, ?> qualifiers) implements Supplier<T> {

    public Value {
      path = Objects.requireNonNull(path, "path");
      qualifiers = qualifiers == null ? Map.of() : Map.copyOf(qualifiers);
    }

    @Override // Supplier
    public final T get() {
      return this.value();
    }

    @Convenience
    @SuppressWarnings("unchecked")
    public final <X> Value<X> cast() {
      return (Value<X>)this;
    }

  }
  
}
