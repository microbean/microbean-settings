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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import org.microbean.development.annotation.Experimental;
import org.microbean.development.annotation.Incomplete;

@Experimental
@Incomplete
public final record Path(Type rootType, Type targetType, List<String> components) {

  public Path(final Type targetType) {
    this(targetType, targetType, List.of());
  }

  public Path {
    Types.rawClass(rootType); // validates
    Types.rawClass(targetType); // validates
    if (components == null || components.isEmpty()) {
      components = List.of();
    } else {
      final List<String> newComponents = new ArrayList<>(components.size());
      for (final String component : components) {
        if (component != null && !component.isEmpty()) {
          newComponents.add(component);
        }
      }
      if (newComponents.isEmpty()) {
        components = List.of();
      } else {
        components = Collections.unmodifiableList(newComponents);
      }
    }
  }

  public final boolean root() {
    return this.components().isEmpty() && this.targetType().equals(this.rootType());
  }

  public final Class<?> targetClass() {
    return Types.rawClass(this.targetType());
  }

  public final ClassLoader classLoader() {
    return this.targetClass().getClassLoader();
  }

  public final boolean endsWith(final String s) {
    final List<?> components = this.components();
    return components.isEmpty() ? false : s.equals(components.get(components.size() - 1));
  }

  public final String lastComponent() {
    final List<String> components = this.components();
    return components.isEmpty() ? "" : components.get(components.size() - 1);
  }

  public final Path with(final Type targetType) {
    return new Path(this.rootType(), targetType, this.components());
  }

  public final Path plus(final String component) {
    return this.plus(Object.class, component);
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

  public final String componentsString() {
    final List<String> components = this.components();
    if (components.isEmpty()) {
      return "";
    } else if (components.size() == 1) {
      return components.get(0);
    } else {
      final StringJoiner sj = new StringJoiner(".");
      components.forEach(sj::add);
      return sj.toString();
    }
  }

}
