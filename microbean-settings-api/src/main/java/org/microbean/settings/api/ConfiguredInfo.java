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

import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodHandle;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class ConfiguredInfo<T> {


  /*
   * Instance fields.
   */

  
  private final Map<String, Supplier<?>> valueSuppliers;


  /*
   * Constructors.
   */

  
  protected ConfiguredInfo(final Map<? extends String, ? extends Supplier<?>> valueSuppliers) {
    super();
    this.valueSuppliers = Map.copyOf(valueSuppliers);
  }


  /*
   * Instance methods.
   */

  
  public Supplier<T> valueSupplier() {
    return null;
  }

  public final Supplier<?> valueSupplier(final String name) {
    return this.valueSuppliers.get(name);
  }
  
  public final Supplier<?> valueSupplier(final Method m) {
    if (accept(m)) {
      return this.valueSupplier(m.getName());
    } else {
      return null;
    }
  }


  /*
   * Static methods.
   */


  public static final boolean accept(final Method m) {
    if (m != null && m.getDeclaringClass() != Object.class && m.getParameterCount() == 0) {
      final Object returnType = m.getReturnType();
      return returnType != void.class && returnType != Void.class;
    } else {
      return false;
    }
  }

}
