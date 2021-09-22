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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.StringJoiner;

public final record Path2(Path2 parent, String name, Type targetType) {

  public Path2() {
    this((Path2)null, "", Object.class);
  }

  // Creates a new root
  public Path2(final Type targetType) {
    this((Path2)null, "", targetType);
  }

  public Path2(final Type parentRootType, final String name, final Type targetType) {
    this(new Path2(parentRootType), name, targetType);
  }

  public Path2 {
    Objects.requireNonNull(targetType, "targetType");
    if (parent == null) {
      if (!name.isEmpty()) {
        throw new IllegalArgumentException("name: " + name);
      }
    } else if (name.isEmpty()) {
      throw new IllegalArgumentException("empty name; non-null parent: " + parent);
    }
  }

  public final List<Path2> all() {
    final Deque<Path2> kids = new ArrayDeque<>(5);
    Path2 path = this;
    Path2 parent = this.parent();
    while (parent != null) {
      kids.addFirst(path);
      path = parent;
      parent = parent.parent();
    }
    kids.addFirst(path);
    return List.copyOf(kids);
  }

  public final Class<?> targetClass() {
    return Types.rawClass(this.targetType());
  }

  public final Path2 plus(final String name, final Type targetType) {
    return new Path2(this, name, targetType);
  }

  public final Path2 root() {
    Path2 path = this;
    Path2 parent = path.parent();
    while (parent != null) {
      path = parent;
      parent = parent.parent();
    }
    return path;
  }

  public final boolean isRoot() {
    return this.isRoot(Object.class);
  }

  public final boolean isRoot(final Type type) {
    if (this.parent == null) {
      assert this.name.isEmpty();
      return this.targetType().equals(type);
    } else {
      return false;
    }
  }

  public final Path2 trailingPath(final Type startingType, final List<String> names) {
    Objects.requireNonNull(startingType, "startingType");
    if (names.isEmpty()) {
      if (this.parent == null && this.targetType().equals(startingType)) {
        assert this.name.isEmpty();
        return this;
      } else {
        return null;
      }
    }
    final ListIterator<String> listIterator = names.listIterator(names.size());
    Path2 path = this;
    Path2 parent = path.parent();
    while (listIterator.hasPrevious() && parent != null) {
      if (!listIterator.previous().equals(path.name())) {
        return null;
      }
      path = parent;
      parent = parent.parent();
    }
    return path.targetType().equals(startingType) ? path : null;
  }

  public final boolean endsWith(final Type startingType, final List<String> names) {
    return trailingPath(startingType, names) != null;
  }

  public final Path2 leadingPath(final Type startingType) {
    return this.leadingPath(startingType, List.of());
  }

  public final Path2 leadingPath(final Type startingType, final List<String> names) {
    Objects.requireNonNull(startingType, "startingType");
    final Deque<Path2> kids = new ArrayDeque<>(5);
    Path2 path = this;
    Path2 parent = this.parent();
    while (parent != null) {
      kids.addFirst(path);
      path = parent;
      parent = parent.parent();
    }
    if (path.targetType().equals(startingType)) {
      if (!names.isEmpty()) {
        path = null;
        final Iterator<Path2> kidIterator = kids.iterator();
        for (final String name : names) {
          if (name.isEmpty()) {
            throw new IllegalArgumentException("names: " + names);
          }
          if (kidIterator.hasNext()) {
            final Path2 kid = kidIterator.next();
            final String kidName = kid.name();
            if (kidName.isEmpty()) {
              // Skip the root
              assert kid.parent() == null;
            } else if (kidName.equals(name)) {
              path = kid;
            } else {
              path = null;
              break;
            }
          } else {
            path = null;
            break;
          }
        }
      }
      return path;
    } else {
      return null;
    }
  }

  public final boolean startsWith(final Type startingType) {
    return this.leadingPath(startingType) != null;
  }

  public final Type parentType() {
    final Path2 parent = this.parent();
    return parent == null ? this.targetType() : parent.targetType();
  }

  public final Class<?> parentClass() {
    return Types.rawClass(this.parentType());
  }

  public final String parentName() {
    final Path2 parent = this.parent();
    return parent == null ? "" : parent.name();
  }

  public final Type rootType() {
    return this.root().targetType();
  }

  public final Class<?> rootClass() {
    return Types.rawClass(this.rootType());
  }

  public final String pathString() {
    return this.pathString(".");
  }

  public final String pathString(final CharSequence separator) {
    final StringJoiner sj = new StringJoiner(separator);
    this.names().forEach(sj::add);
    return sj.toString();
  }

  public final List<String> names() {
    final Deque<String> dq = new ArrayDeque<>(5);
    Path2 path = this;
    Path2 parent = path.parent();
    while (parent != null) {
      dq.addFirst(path.name());
      path = parent;
      parent = parent.parent();
    }
    return List.copyOf(dq);
  }

  public final int length() {
    Path2 path = this;
    Path2 parent = path.parent();
    int size = 1;
    while (parent != null) {
      path = parent;
      parent = parent.parent();
      size++;
    }
    return size;
  }

  public static final Path2 of(final Type rootType,
                               final String name0, final Type type0) {
    return new Path2(rootType).plus(name0, type0);
  }

  public static final Path2 of(final Type rootType,
                               final String name0, final Type type0,
                               final String name1, final Type type1) {
    return
      new Path2(rootType)
      .plus(name0, type0)
      .plus(name1, type1);
  }

  public static final Path2 of(final Type rootType,
                               final String name0, final Type type0,
                               final String name1, final Type type1,
                               final String name2, final Type type2) {
    return
      new Path2(rootType)
      .plus(name0, type0)
      .plus(name1, type1)
      .plus(name2, type2);
  }

  public static final Path2 of(final Type rootType,
                               final String name0, final Type type0,
                               final String name1, final Type type1,
                               final String name2, final Type type2,
                               final String name3, final Type type3) {
    return
      new Path2(rootType)
      .plus(name0, type0)
      .plus(name1, type1)
      .plus(name2, type2)
      .plus(name3, type3);
  }

  public static final Path2 of(final Type rootType, final Object... components) {
    Path2 path = new Path2(rootType);
    if (components != null && components.length > 0) {
      String name = null;
      for (int i = 0; i < components.length; i++) {
        if (i % 2 == 0) {
          if (components[i] instanceof String s) {
            assert name == null;
            name = s;
          } else {
            throw new IllegalArgumentException("components; components[" + i + "]: " + components[i]);
          }
        } else if (components[i] instanceof Type type) {
          path = path.plus(name, type);
          name = null;
        } else {
          throw new IllegalArgumentException("components; components[" + i + "]: " + components[i]);
        }
      }
    }
    return path;
  }

}
