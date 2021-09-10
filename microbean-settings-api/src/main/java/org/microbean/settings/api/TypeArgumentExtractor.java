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

final class TypeArgumentExtractor extends ClassValue<Type> {

  private final Class<?> stop;
  
  private final int typeParameterIndex;
  
  TypeArgumentExtractor(final Class<?> stop, final int typeParameterIndex) {
    super();
    final int typeVariablesCount = stop.getTypeParameters().length;
    if (typeVariablesCount <= 0) {
      throw new IllegalArgumentException("stop: " + stop);
    } else if (typeParameterIndex < 0 || typeParameterIndex >= typeVariablesCount) {
      throw new IndexOutOfBoundsException(typeParameterIndex);
    }
    this.stop = stop;
    this.typeParameterIndex = typeParameterIndex;
  }

  @Override // ClassValue<Type>
  protected final Type computeValue(Class<?> stopSubclass) {
    if (this.stop.isAssignableFrom(stopSubclass)) {
      Class<?> superclass = stopSubclass.getSuperclass();
      Object gc = stopSubclass.getGenericSuperclass();
      while (!this.stop.equals(superclass)) {
        gc = stopSubclass.getGenericSuperclass();
        stopSubclass = superclass;
        superclass = superclass.getSuperclass();
      }
      return ((ParameterizedType)gc).getActualTypeArguments()[this.typeParameterIndex];
    } else {
      return null;
    }
  }
  
}
