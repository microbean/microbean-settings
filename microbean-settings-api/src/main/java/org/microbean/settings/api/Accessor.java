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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import org.microbean.development.annotation.Experimental;

// Arguments are Strings because they are informative only and
// Accessors are contained by Paths which are often keys in maps.
public record Accessor(String name, List<Class<?>> parameters, List<String> arguments) {


  /*
   * Constructors.
   */


  public Accessor(final String name) {
    this(name, List.of(), List.of());
  }

  public Accessor(final int index) {
    this("[" + index + "]", List.of(Integer.class), List.of(Integer.toString(index)));
  }

  public Accessor {
    Objects.requireNonNull(name, "name");
    parameters = List.copyOf(parameters);
    arguments = List.copyOf(arguments);
    if (!arguments.isEmpty() && parameters.size() != arguments.size()) {      
      throw new IllegalArgumentException("parameters: " + parameters + "; arguments: " + arguments);
    }
  }


  /*
   * Instance methods.
   */


  public final int parameterCount() {
    return this.parameters().size();
  }

  public final Class<?> parameter(final int index) {
    return this.parameters().get(index);
  }

  public final int argumentCount() {
    assert this.arguments().isEmpty() ? true : this.parameterCount() == this.arguments.size();
    return this.arguments().size();
  }
  
  public final String argument(final int index) {
    return this.arguments().get(index);
  }

  // You can look at an Accessor as a pile of qualifiers.  That lets
  // us score them in the same way.  This can come in useful when path
  // matching.
  @Experimental
  public final Qualifiers toQualifiers() {
    final SortedMap<String, String> map = new TreeMap<>();
    map.put("name", this.name());
    final List<String> arguments = this.arguments();
    for (int i = 0; i < arguments.size(); i++) {
      map.put("arg" + i, arguments.get(i));
    }
    return Qualifiers.of(map);
  }

  @Override
  public final String toString() {
    final StringBuilder sb = new StringBuilder(this.name().replace(".", "\\."));
    final int parameterCount = this.parameterCount();
    if (parameterCount > 0) {
      sb.append(":");
      final int argumentCount = this.argumentCount();
      if (argumentCount <= 0) {
        for (int i = 0; i < parameterCount; i++) {
          sb.append(this.parameter(i).getName()).append("=");
          if (i + 1 < parameterCount) {
            sb.append(";");
          }
        }
      } else {
        for (int i = 0; i < parameterCount; i++) {
          sb.append(this.parameter(i).getName()).append("=").append(this.argument(i).replace("\"", "\\\"")).append("\"");
          if (i + 1 < parameterCount) {
            sb.append(";");
          }
        }
      }
    }
    return sb.toString();
  }

  /*
   * Static methods.
   */


  public static final Accessor of() {
    return new Accessor("");
  }

  public static final Accessor of(final String name) {
    return new Accessor(name);
  }

  public static final List<Accessor> of(final String name0, final String name1) {
    return List.of(Accessor.of(name0), Accessor.of(name1));
  }

  public static final List<Accessor> of(final String name0, final String name1, final String name2) {
    return List.of(Accessor.of(name0), Accessor.of(name1), Accessor.of(name2));
  }

  public static final List<Accessor> of(final Collection<? extends String> names) {
    if (names == null || names.isEmpty()) {
      return List.of();
    } else {
      return names.stream().map(Accessor::of).toList();
    }
  }

  public static final List<Accessor> of(final String... names) {
    if (names == null || names.length <= 0) {
      return List.of();
    } else {
      return Arrays.stream(names).map(Accessor::of).toList();
    }
  }

  public static final Accessor of(final int index) {
    return new Accessor(index);
  }

  public static final Accessor of(final String name, final Class<?> parameter) {
    return of(name, List.of(parameter), List.of());
  }
  
  public static final <T> Accessor of(final String name, final Class<T> parameter, final T argument) {
    return of(name, List.of(parameter), List.of(argument.toString()));
  }

  public static final Accessor of(final String name, final List<Class<?>> parameters, final List<String> arguments) {
    return new Accessor(name, parameters, arguments);
  }

}
