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

public record Accessor(String name, List<Class<?>> parameters, List<?> arguments) {

  public Accessor(final String name) {
    this(name, List.of(), List.of());
  }

  public Accessor(final int index) {
    this("[" + index + "]", List.of(Integer.class), List.of(Integer.valueOf(index)));
  }
  
  public Accessor {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(parameters, "parameters");
    Objects.requireNonNull(arguments, "arguments");
    if (parameters.size() != arguments.size()) {
      throw new IllegalArgumentException("paramters: " + parameters + "; arguments: " + arguments);
    }
  }

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
  
}
