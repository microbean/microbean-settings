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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import java.util.function.Function;
import java.util.function.Supplier;

public class ListValueSupplier extends AbstractValueSupplier {

  private final Map<Path, Supplier<List<?>>> map;

  private final Function<? super QualifiedPath, ? extends Map<?, ?>> qualifiersFunction;

  public ListValueSupplier(final Path path, final Supplier<List<?>> supplier) {
    this(Map.of(Objects.requireNonNull(path, "path"), supplier),
         qp -> qp.applicationQualifiers());
  }
  
  public ListValueSupplier(final Map<? extends Path, ? extends Supplier<List<?>>> map,
                           final Function<? super QualifiedPath, ? extends Map<?, ?>> qualifiersFunction) {
    super();
    if (map == null || map.isEmpty()) {
      this.map = Map.of();
    } else {
      for (final Path path : map.keySet()) {
        if (!List.class.equals(path.targetClass())) {
          throw new IllegalArgumentException("map: " + map);
        }
      }
      this.map = Map.copyOf(map);
    }
    this.qualifiersFunction = Objects.requireNonNull(qualifiersFunction, "qualifiersFunction");
  }
  
  @Override // ValueSupplier
  public boolean respondsFor(final QualifiedPath qualifiedPath) {
    if (qualifiedPath != null) {
      final Path path = qualifiedPath.path();
      return path != null && this.map.containsKey(path) && this.qualifiersFunction.apply(qualifiedPath) != null;
    } else {
      return false;
    }
  }

  @Override // ValueSupplier
  public Value<?> get(final QualifiedPath qualifiedPath,
                      final Function<? super QualifiedPath, ? extends Collection<ValueSupplier>> valueSuppliers) {
    final Path path = qualifiedPath.path();    
    final Supplier<List<?>> listSupplier = path == null ? null : this.map.get(path);
    return listSupplier == null ? null : new Value<>(listSupplier.get(), path, this.qualifiersFunction.apply(qualifiedPath));
  }
  
}
