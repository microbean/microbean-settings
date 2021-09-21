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

import java.util.AbstractList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import java.util.function.BiFunction;

public class ListValueSupplier implements ValueSupplier {

  @Override // ValueSupplier
  public boolean respondsFor(final Path path, final Map<?, ?> applicationQualifiers) {
    return path != null && path.targetClass() == List.class;
  }

  @Override // ValueSupplier
  public <T> Value<T> get(final Path path,
                          final Map<?, ?> applicationQualifiers,
                          final BiFunction<? super Path, ? super Map<?, ?>, ? extends Collection<ValueSupplier>> valueSuppliers) {
    final Value<T> value;
    final Type targetType = path.targetType();
    if (path == null || Types.rawClass(targetType) != List.class) {
      value = null;
    } else {
      final Type listType = ((ParameterizedType)targetType).getRawType();
      // The path is something like a.b.frobs; frobs is a List<WhoKnows>
      // anyIndexPath will be a.b.frobs.*
      final Path anyIndexPath = path.plus(listType, Path.ANY);
      final Collection<ValueSupplier> relevantValueSuppliers = valueSuppliers.apply(anyIndexPath, applicationQualifiers);
      final List<ValueSupplier> vsList = relevantValueSuppliers == null || relevantValueSuppliers.isEmpty() ? List.of() : List.copyOf(relevantValueSuppliers);
      final List<Object> l = new AbstractList<>() {
          @Override // AbstractList<Object>
          public final Object get(final int index) {
            // indexPath will be, e.g. a.b.frobs.3
            final Path indexPath = path.plus(index);
            final Collection<ValueSupplier> relevantValueSuppliers = valueSuppliers.apply(indexPath, applicationQualifiers);
            if (relevantValueSuppliers == null || relevantValueSuppliers.isEmpty()) {
              return null;
            } else {
              // TODO: qualifier matching logic/resolution; see Configured
              throw new UnsupportedOperationException();
            }
          }
          @Override
          public final boolean isEmpty() {
            return vsList.isEmpty();
          }
          @Override
          public final int size() {
            // TODO: this isn't quite right; maybe throw UnsupportedOperationException instead and deal with Iterators
            return vsList.size();
          }
        };
      throw new UnsupportedOperationException(); // TODO implement
    }
    return value;
  }
  
}
