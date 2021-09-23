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


  /*
   * Static methods.
   */


  public static final Map<?, ?> application() {
    return loadedQualifiers.get(Qualifiers.class).get();
  }

  public static boolean viable(final Map<?, ?> applicationQualifiers, final Map<?, ?> qualifiers) {
    final boolean returnValue;
    if (applicationQualifiers.isEmpty()) {
      returnValue = qualifiers == null || qualifiers.isEmpty();
    } else if (qualifiers == null || qualifiers.isEmpty()) {
      returnValue = false;
    } else {
      returnValue = applicationQualifiers.entrySet().containsAll(qualifiers.entrySet());
    }
    return returnValue;
  }



}
