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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.microbean.development.annotation.Experimental;
import org.microbean.development.annotation.Incomplete;

@Experimental
@Incomplete
public final record Path(Type rootType, Type targetType, List<String> components) {

  public Path(final Type targetType) {
    this(targetType, targetType, List.of());
  }

  public Path {
    rawClass(rootType); // validates
    rawClass(targetType); // validates
    components = components == null ? List.of() : List.copyOf(components);
  }

  public final int length() {
    return this.components().size();
  }

  public final boolean root() {
    return this.components().isEmpty() && this.targetType().equals(this.rootType());
  }

  public final Class<?> rootClass() {
    return rawClass(this.rootType());
  }

  public final Class<?> targetClass() {
    return rawClass(this.targetType());
  }

  public final ClassLoader classLoader() {
    return this.targetClass().getClassLoader();
  }

  public final boolean endsWith(final String s) {
    final List<?> components = this.components();
    return components.isEmpty() ? false : s.equals(components.get(components.size() - 1));
  }

  public final boolean contains(final Path other) {
    return
      other != null &&
      this.rootType().equals(other.rootType()) &&
      this.targetType().equals(other.targetType()) &&
      this.components().containsAll(other.components());
  }

  public final Path plus(final Type targetType, final String component) {
    final List<String> components = this.components();
    final List<String> newList;
    if (components.isEmpty()) {
      newList = List.of(Objects.requireNonNull(component, "component"));
    } else {
      newList = new ArrayList<>(components);
      newList.add(Objects.requireNonNull(component, "component"));
    }
    return new Path(this.rootType(), targetType, newList);
  }

  private static final Class<?> rawClass(final Type type) {
    if (type instanceof Class<?> c) {
      return c;
    } else if (type instanceof ParameterizedType ptype) {
      return rawClass(ptype);
    } else {
      throw new IllegalArgumentException("Unhandled type: " + type);
    }
  }

  private static final Class<?> rawClass(final ParameterizedType type) {
    return rawClass(type.getRawType());
  }

}
