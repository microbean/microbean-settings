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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.ServiceLoader;

import java.util.function.Supplier;

public class Qualifiers implements Supplier<Map<?, ?>> {


  /*
   * Static fields.
   */


  private static final ClassValue<Qualifiers> loadedQualifiers = new ClassValue<>() {
      @Override // ClassValue<Qualifiers>
      protected final Qualifiers computeValue(final Class<?> c) {
        return Qualifiers.class == c ?
        ServiceLoader.load(Qualifiers.class, Qualifiers.class.getClassLoader())
        .stream()
        .map(ServiceLoader.Provider::get)
        .sorted(Prioritized.COMPARATOR_DESCENDING)
        .findFirst()
        .orElse(new Qualifiers()) :
        null;
      }
    };


  /*
   * Instance fields.
   */


  private final Map<?, ?> qualifiers;


  /*
   * Constructors.
   */


  public Qualifiers() {
    this(Map.of());
  }

  public Qualifiers(final Map<?, ?> qualifiers) {
    super();
    this.qualifiers = qualifiers == null ? Map.of() : Map.copyOf(qualifiers);
  }


  /*
   * Instance methods.
   */


  @Override // Supplier<Map<?, ?>
  public final Map<?, ?> get() {
    return this.qualifiers;
  }

  @Override // Object
  public int hashCode() {
    return this.qualifiers.hashCode();
  }

  @Override
  public boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other != null && this.getClass().equals(other.getClass())) {
      final Qualifiers her = (Qualifiers)other;
      return Objects.equals(this.get(), her.get());
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return this.get().toString();
  }


  /*
   * Static methods.
   */


  public static final Map<?, ?> application() {
    return loadedQualifiers.get(Qualifiers.class).get();
  }

  // TODO: this is almost certainly wrong
  public static boolean isAssignable(final Map<?, ?> lhs, final Map<?, ?> rhs) {
    /*
      Let's treat this like CDI.

      In CDI, if the lhs is empty, then it is @Default.

      If there is exactly one rhs, then it too has @Default,
      explicitly or implicitly.

      If the lhs has @Something, then a rhs with @Default won't match.

      So suppose lhs.isEmpty() means @Default.

      And suppose rhs.isEmpty() means @Default.

      Now suppose we want to add: on the rhs, if there is a non-empty
      Map (i.e. NOT @Default), then it is assignable if and only if
      the lhs is equal to it.

      So if the lhs says, only, env=test, then the rhs, to be
      assignable, must also have, only, env=test.  If the rhs also
      has, say, datacenter=east, that's "too specific" and will not
      assign.  (It is exactly as if the lhs had implicitly specified
      datacenter=default.)

      We also want to say that if the rhs is empty, then it is
      assignable to any lhs.  Maybe in the ValueSuppliers.resolve()
      method, we want to say "take the most-specific rhs you can, but
      otherwise go ahead and use the empty one".
     */
    final boolean returnValue;
    if (lhs == null) {
      returnValue = rhs == null || rhs.isEmpty();
    } else if (rhs == null) {
      returnValue = true;
    } else {
      final int lhsSize = lhs.size();
      final int rhsSize = rhs.size();
      if (lhsSize == 0) {
        returnValue = rhsSize == 0;
      } else if (rhsSize == 0) {
        // An empty rhs is assignable to any lhs.
        returnValue = true;
      } else if (lhsSize < rhsSize) {
        returnValue = false;
      } else {
        boolean temp = true;
        for (final Entry<?, ?> lhsEntry : lhs.entrySet()) {
          final Object lhsKey = lhsEntry.getKey();
          final Object lhsValue = lhsEntry.getValue();
          for (final Entry<?, ?> rhsEntry : rhs.entrySet()) {
            if (lhsKey.equals(rhsEntry.getKey())) {
              // The right-hand-side contains the key, so it has a
              // particular opinion.
              if (!lhsValue.equals(rhsEntry.getValue())) {
                // The right-hand-side's value for that key didn't
                // match.
                temp = false;
                break;
              }
            }
          }
        }
        returnValue = temp;
      }
    }
    return returnValue;
  }

}
