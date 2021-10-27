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
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import java.util.function.BiPredicate;

public final class PathFragment {

  private static final PathFragment ROOT = new PathFragment();
  
  private final List<Object> elements;
  
  private PathFragment() {
    super();
    this.elements = List.of(void.class);
  }

  private PathFragment(final Accessor accessor, final Type type) {
    this(List.of(), List.of(accessor), type);
  }

  private PathFragment(final List<? extends Accessor> accessors, final Type type) {
    this(List.of(), accessors, type);
  }
  
  private PathFragment(final Type type) {
    this(List.of(), List.of(), type);
  }

  private PathFragment(final List<?> existingElements, final List<? extends Accessor> accessors, final Type type) {
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

  public final int indexOf(final PathFragment pathFragment, final BiPredicate<? super Object, ? super Object> p) {
    final int pathFragmentSize = pathFragment.size();
    final int sizeDiff = this.size() - pathFragmentSize;
    OUTER_LOOP:
    for (int i = 0; i <= sizeDiff; i++) {
      for (int j = 0, k = i; j < pathFragmentSize; j++, k++) {
        if (!p.test(this.elements.get(k), pathFragment.elements.get(j))) {
          continue OUTER_LOOP;
        }
      }
      return i;
    }
    return -1;
  }

  public final int lastIndexOf(final PathFragment pathFragment, final BiPredicate<? super Object, ? super Object> p) {
    final int pathFragmentSize = pathFragment.size();
    final int sizeDiff = this.size() - pathFragmentSize;
    OUTER_LOOP:
    for (int i = sizeDiff; i >= 0; i--) {
      for (int j = 0, k = i; j < pathFragmentSize; j++, k++) {
        if (!p.test(this.elements.get(k), pathFragment.elements.get(j))) {
          continue OUTER_LOOP;
        }
      }
      return i;
    }
    return -1;
  }
  
  public final int indexOf(final PathFragment other) {
    return Collections.indexOfSubList(this.elements, other.elements);
  }

  public final int lastIndexOf(final PathFragment other) {
    return Collections.lastIndexOfSubList(this.elements, other.elements);
  }
  
  public final PathFragment plus(final String accessor, final Type type) {
    return this.plus(Accessor.of(accessor), type);
  }
  
  public final PathFragment plus(final Accessor accessor, final Type type) {
    return this.plus(List.of(accessor), type);
  }
  
  public final PathFragment plus(final List<? extends Accessor> accessors, final Type type) {
    return new PathFragment(this.elements, accessors, type);
  }

  // Drops the intermediate type.
  public final PathFragment merge(final String accessor, final Type type) {
    return this.merge(Accessor.of(accessor), type);
  }

  // Drops the intermediate type.
  public final PathFragment merge(final Accessor accessor, final Type type) {
    return this.merge(List.of(accessor), type);
  }

    // Drops the intermediate type.
  public final PathFragment merge(final List<? extends Accessor> accessors, final Type type) {
    return new PathFragment(this.elements.subList(0, this.elements.size() - 1), accessors, type);
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
      return this.elements.equals(((PathFragment)other).elements);
    }
  }

  @Override
  public final String toString() {
    // TODO: beef up
    return this.elements.toString();
  }

  public static final PathFragment of(final Type type) {
    return new PathFragment(type);
  }

  public static final PathFragment of(final String accessor, final Type type) {
    return new PathFragment(Accessor.of(accessor), type);
  }

  /*
  public static final PathFragment ofAbsolute(final String accessor, final Type type) {
    return root().plus(accessor, type);
  }
  */

  public static final PathFragment of(final Accessor accessor, final Type type) {
    return new PathFragment(accessor, type);
  }

  /*
  public static final PathFragment ofAbsolute(final Accessor accessor, final Type type) {
    return root().plus(accessor, type);
  }
  */

  
  public static final PathFragment of(final List<? extends Accessor> accessors, final Type type) {
    return new PathFragment(accessors, type);
  }

  /*
  public static final PathFragment ofAbsolute(final List<? extends Accessor> accessors, final Type type) {
    return root().plus(accessors, type);
  }
  */

  /*
  public static final PathFragment root() {
    return ROOT;
  }
  */

}
