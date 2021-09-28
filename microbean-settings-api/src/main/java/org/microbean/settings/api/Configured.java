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
import java.util.Map;

import java.util.function.Supplier;

import org.microbean.settings.api.ValueSupplier.Value;

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


  public static final <T> T of(final Type type) {
    return of(new QualifiedPath.Record(new Path(type), Qualifiers.application()), null);
  }

  public static final <T> T of(final Type type, final Supplier<? extends T> defaultTargetSupplier) {
    return of(new QualifiedPath.Record(new Path(type), Qualifiers.application()), defaultTargetSupplier);
  }

  public static final <T> T of(final Path path) {
    return of(new QualifiedPath.Record(path, Qualifiers.application()), null);
  }

  public static final <T> T of(final Path path, final Supplier<? extends T> defaultTargetSupplier) {
    return of(new QualifiedPath.Record(path, Qualifiers.application()), defaultTargetSupplier);
  }

  public static final <T> T of(final QualifiedPath qp, final Supplier<? extends T> defaultTargetSupplier) {
    final T returnValue;
    final Collection<ValueSupplier> valueSuppliers = ValueSupplier.loadedValueSuppliers(qp);
    if (valueSuppliers.isEmpty()) {
      if (defaultTargetSupplier == null) {
        throw new UnsupportedOperationException();
      } else {
        returnValue = defaultTargetSupplier.get();
      }
    } else {
      final Value<?> value = ValueSupplier.resolve(valueSuppliers, qp, ValueSupplier::loadedValueSuppliers);
      @SuppressWarnings("unchecked")
      final T rv = value == null ? null : (T)value.get();
      returnValue = rv;
    }
    return returnValue;
  }

}
