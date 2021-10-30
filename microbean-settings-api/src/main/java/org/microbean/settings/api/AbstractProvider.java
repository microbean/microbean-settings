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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.util.function.Supplier;

public abstract class AbstractProvider<T> implements Provider {

  private static final ClassValue<Type> type = new ClassValue<>() {
      @Override
      protected final Type computeValue(final Class<?> c) {
        if (c != AbstractProvider.class && AbstractProvider.class.isAssignableFrom(c)) {
          return ((ParameterizedType)this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        } else {
          return null;
        }
      }
    };
  
  protected AbstractProvider() {
    super();
  }

  @Override // Provider
  public final Type upperBound() {
    return type.get(this.getClass());
  }
  
  @Override // Provider
  public boolean isSelectable(final SupplierBroker broker,
                              final Qualifiers qualifiers,
                              final Supplier<?> parentSupplier,
                              final Path path) {
    return AssignableType.of(path.type()).isAssignable(this.upperBound());
  }
  
}
