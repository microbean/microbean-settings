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
import org.microbean.development.annotation.OverridingDiscouraged;
import org.microbean.development.annotation.OverridingEncouraged;

public interface ValueSupplier extends Prioritized {


  /*
   * Abstract methods.
   */


  @OverridingEncouraged
  public default int priority(final QualifiedPath qualifiedPath) {
    // If your ValueSupplier is "for" a particular set of qualifiers,
    // then their specificity, as well as the qualifiedPath's
    // specificity, should also be taken into account. This
    // implementation is deliberately simplistic and just moves the
    // inappropriates to the back of the line.
    return this.mayHandle(qualifiedPath) ? qualifiedPath.priority() : Integer.MIN_VALUE;
  }

  @OverridingEncouraged
  public default boolean mayHandle(final QualifiedPath qualifiedPath) {
    // This just says the ValueSupplier *may* handle the supplied
    // QualifiedPath. It is under no obligation to actually do so from
    // within the get(QualifiedPath, Function) method.  If an
    // implementation of this method returns false, then the
    // get(QualifiedPath, Function) method's behavior will be
    // undefined.
    return true;
  }

  public Value<?> get(final QualifiedPath qualifiedPath,
                      final Function<? super QualifiedPath, ? extends Collection<ValueSupplier>> valueSuppliersFunction);

  
  /*
   * Static methods.
   */


  public static Value<?> resolve(final Collection<? extends ValueSupplier> valueSuppliers,
                                 final QualifiedPath qualifiedPath,
                                 final Function<? super QualifiedPath, ? extends Collection<ValueSupplier>> vsf) {
    final Value<?> returnValue;
    if (valueSuppliers == null || valueSuppliers.isEmpty()) {
      returnValue = null;
    } else {
      Collection<Value<?>> bad = null;
      Value<?> candidate = null;
      final Comparator cmp = new Comparator(qualifiedPath);
      for (final ValueSupplier valueSupplier : valueSuppliers) {
        if (valueSupplier != null && valueSupplier.mayHandle(qualifiedPath)) {
          final Value<?> v = valueSupplier.get(qualifiedPath, vsf);
          if (v != null) {
            final Map<?, ?> qualifiers = v.qualifiers();
            if (qualifiedPath.isAssignable(qualifiers)) {
              if (candidate == null) {
                candidate = v;
              } else {
                assert qualifiers.size() <= candidate.qualifiers().size() :
                "candidate qualifiers: " + candidate.qualifiers() + "; assignable value qualifiers: " + qualifiers;

                final int comparison = Prioritized.COMPARATOR_DESCENDING.compare(candidate, v);
                if (comparison >= 0) {
                  // candidate is "greater than" or "equal to" v; do nothing
                } else {
                  // candidate is "less than" v; replace it with v
                  candidate = v;
                }
                
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
    final Comparator c = new Comparator(qp);
    return loadedValueSuppliers().stream()
      .filter(vs -> vs.mayHandle(qp)) // weed out the inappropriates
      .sorted(c::compareValueSuppliers) // sort the remainder further by path specificity
      .toList();
  }

  public static void clearLoadedValueSuppliers() {
    Value.loadedValueSuppliers.remove(ValueSupplier.class);
  }


  /*
   * Inner and nested classes.
   */


  public static final record Value<T>(T value, Path path, Map<?, ?> qualifiers, int priority) implements Prioritized, Supplier<T> {

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
          .sorted(Prioritized.COMPARATOR_DESCENDING) // default sort by general priority, not path-specific obviously
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

  static final class Comparator {

    private final QualifiedPath qp;
    
    private Comparator(final QualifiedPath qp) {
      super();
      this.qp = Objects.requireNonNull(qp, "qp");
    }

    // Inconsistent with equals
    private final int compareValueSuppliers(final ValueSupplier vs0, final ValueSupplier vs1) {
      if (vs0 == null) {
        return vs1 == null ? 0 : -1; // nulls right
      } else if (vs1 == null) {
        return 1; // nulls right
      } else {
        // Note descending order
        return Integer.signum(vs1.priority(this.qp) - vs0.priority(this.qp));
      }
    }
    
  }

}
