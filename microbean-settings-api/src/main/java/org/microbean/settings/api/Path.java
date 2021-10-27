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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import java.util.function.BiPredicate;

public final class Path {

  private static final Path ROOT = new Path();

  private final List<Object> elements;

  private Path() {
    super();
    this.elements = List.of(void.class);
  }

  private Path(final Accessor accessor, final Type type) {
    this(List.of(), List.of(accessor), type);
  }

  private Path(final List<? extends Accessor> accessors, final Type type) {
    this(List.of(), accessors, type);
  }

  private Path(final Type type) {
    this(List.of(), List.of(), type);
  }

  private Path(final List<?> existingElements, final List<? extends Accessor> accessors, final Type type) {
    super();
    if (Objects.requireNonNull(type, "type") == void.class) {
      throw new IllegalArgumentException("type: " + type);
    }
    if (existingElements == null || existingElements.isEmpty()) {
      final int accessorsSize = accessors == null ? 0 : accessors.size();
      switch (accessorsSize) {
      case 0:
        this.elements = List.of(type);
        break;
      case 1:
        this.elements = List.of(accessors.get(0), type);
        break;
      case 2:
        this.elements = List.of(accessors.get(0), accessors.get(1), type);
        break;
      case 3:
        this.elements = List.of(accessors.get(0), accessors.get(1), accessors.get(2), type);
        break;
      default:
        final List<Object> elements = new ArrayList<>(accessors.size() + 1);
        for (final Accessor a : accessors) {
          elements.add(Objects.requireNonNull(a, "accessor"));
        }
        elements.add(type);
        this.elements = Collections.unmodifiableList(elements);
      }
    } else {
      final List<Object> elements = new ArrayList<>(existingElements.size() + accessors.size() + 1);
      elements.addAll(existingElements);
      for (final Accessor a : accessors) {
        elements.add(Objects.requireNonNull(a, "accessor"));
      }
      elements.add(type);
      this.elements = Collections.unmodifiableList(elements);
    }
  }

  /*
  public final boolean isAbsolute() {
    return this.elements.get(0) == void.class;
  }
  */

  public final int indexOf(final Path path, final BiPredicate<? super Object, ? super Object> p) {
    final int pathSize = path.size();
    final int sizeDiff = this.size() - pathSize;
    OUTER_LOOP:
    for (int i = 0; i <= sizeDiff; i++) {
      for (int j = 0, k = i; j < pathSize; j++, k++) {
        if (!p.test(this.elements.get(k), path.elements.get(j))) {
          continue OUTER_LOOP;
        }
      }
      return i;
    }
    return -1;
  }

  public final int lastIndexOf(final Path path, final BiPredicate<? super Object, ? super Object> p) {
    final int pathSize = path.size();
    final int sizeDiff = this.size() - pathSize;
    OUTER_LOOP:
    for (int i = sizeDiff; i >= 0; i--) {
      for (int j = 0, k = i; j < pathSize; j++, k++) {
        if (!p.test(this.elements.get(k), path.elements.get(j))) {
          continue OUTER_LOOP;
        }
      }
      return i;
    }
    return -1;
  }

  public final int indexOf(final Path other) {
    return Collections.indexOfSubList(this.elements, other.elements);
  }

  public final int lastIndexOf(final Path other) {
    return Collections.lastIndexOfSubList(this.elements, other.elements);
  }

  public final Path plus(final String accessor, final Type type) {
    return this.plus(Accessor.of(accessor), type);
  }

  public final Path plus(final Accessor accessor, final Type type) {
    return this.plus(List.of(accessor), type);
  }

  public final Path plus(final List<? extends Accessor> accessors, final Type type) {
    return new Path(this.elements, accessors, type);
  }

  // Drops the intermediate type.
  public final Path merge(final String accessor, final Type type) {
    return this.merge(Accessor.of(accessor), type);
  }

  // Drops the intermediate type.
  public final Path merge(final Accessor accessor, final Type type) {
    return this.merge(List.of(accessor), type);
  }

    // Drops the intermediate type.
  public final Path merge(final List<? extends Accessor> accessors, final Type type) {
    return new Path(this.elements.subList(0, this.elements.size() - 1), accessors, type);
  }

  public final int size() {
    return this.elements.size();
  }

  public final boolean isAccessor(final int index) {
    return this.elements.get(index) instanceof Accessor;
  }

  public final boolean isType(final int index) {
    return this.elements.get(index) instanceof Type;
  }

  public final Accessor getAccessor(final int index) {
    final Object o = this.elements.get(index);
    return o instanceof Accessor a ? a : null;
  }

  public final Type getType(final int index) {
    final Object o = this.elements.get(index);
    return o instanceof Type type ? type : null;
  }

  public final Type type() {
    return (Type)this.elements.get(this.elements.size() - 1);
  }

  @Override
  public final int hashCode() {
    return this.elements.hashCode();
  }

  @Override
  public final boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other == null || this.getClass() != other.getClass()) {
      return false;
    } else {
      return this.elements.equals(((Path)other).elements);
    }
  }

  @Override
  public final String toString() {
    // TODO: beef up
    return this.elements.toString();
  }

  public static final Path of(final Type type) {
    return new Path(type);
  }

  public static final Path of(final String accessor, final Type type) {
    return new Path(Accessor.of(accessor), type);
  }

  /*
  public static final Path ofAbsolute(final String accessor, final Type type) {
    return root().plus(accessor, type);
  }
  */

  public static final Path of(final Accessor accessor, final Type type) {
    return new Path(accessor, type);
  }

  /*
  public static final Path ofAbsolute(final Accessor accessor, final Type type) {
    return root().plus(accessor, type);
  }
  */


  public static final Path of(final List<? extends Accessor> accessors, final Type type) {
    return new Path(accessors, type);
  }

  /*
  public static final Path ofAbsolute(final List<? extends Accessor> accessors, final Type type) {
    return root().plus(accessors, type);
  }
  */

  /*
  public static final Path root() {
    return ROOT;
  }
  */

}
