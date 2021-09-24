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
import java.util.Objects;
import java.util.StringJoiner;

import org.microbean.development.annotation.Convenience;

public final record Path(Path parent, String name, Type targetType) {

  @Convenience
  public Path() {
    this((Path)null, "", Object.class);
  }

  @Convenience
  public Path(final Type targetType) {
    this((Path)null, "", targetType);
  }

  @Convenience
  public Path(final Type parentRootType, final String name, final Type targetType) {
    this(new Path(parentRootType), name, targetType);
  }

  public Path {
    Objects.requireNonNull(targetType, "targetType");
    if (parent == null) {
      if (!name.isEmpty()) {
        throw new IllegalArgumentException("null parent with non-empty name: " + name);
      }
    } else if (name.isEmpty()) {
      throw new IllegalArgumentException("empty name with non-null parent: " + parent);
    }
  }

  public final List<Path> all() {
    final Deque<Path> dq = new ArrayDeque<>(5);
    Path path = this;
    Path parent = this.parent();
    while (parent != null) {
      dq.addFirst(path);
      path = parent;
      parent = path.parent();
    }
    dq.addFirst(path);
    return List.copyOf(dq);
  }

  public final Class<?> targetClass() {
    return Types.rawClass(this.targetType());
  }

  public final ClassLoader classLoader() {
    return this.targetClass().getClassLoader();
  }

  public final Path plus(final String name, final Type targetType) {
    return new Path(this, name, targetType);
  }

  public final Path root() {
    Path path = this;
    Path parent = path.parent();
    while (parent != null) {
      path = parent;
      parent = path.parent();
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

  public final Path trailingPath(final Type startingType) {
    // Kind of a pathological :-) case
    return this.targetType().equals(Objects.requireNonNull(startingType, "startingType")) ? this : null;
  }

  public final Path trailingPath(final Type startingType, final List<String> names) {
    Objects.requireNonNull(startingType, "startingType");
    if (names.isEmpty()) {
      return this.targetType().equals(startingType) ? this : null;
    }
    final ListIterator<String> listIterator = names.listIterator(names.size());
    Path path = this;
    Path parent = path.parent();
    while (listIterator.hasPrevious() && parent != null) {
      if (!listIterator.previous().equals(path.name())) {
        return null;
      }
      path = parent;
      parent = path.parent();
    }
    return path.targetType().equals(startingType) ? path : null;
  }

  public final boolean endsWith(final Type startingType) {
    // Kind of a pathological :-) case; i.e. no names
    return this.targetType().equals(Objects.requireNonNull(startingType, "startingType"));
  }

  public final boolean endsWith(final Type startingType, final List<String> names) {
    return trailingPath(startingType, names) != null;
  }

  public final Path leadingPath(final Type startingType) {
    return this.leadingPath(startingType, List.of());
  }

  public final Path leadingPath(final Type startingType, final List<String> names) {
    Objects.requireNonNull(startingType, "startingType");
    final Deque<Path> dq = new ArrayDeque<>(5);
    Path path = this;
    Path parent = path.parent();
    while (parent != null) {
      dq.addFirst(path);
      path = parent;
      parent = path.parent();
    }
    if (path.targetType().equals(startingType)) {
      if (!names.isEmpty()) {
        path = null;
        final Iterator<Path> iterator = dq.iterator();
        for (final String name : names) {
          if (name.isEmpty()) {
            throw new IllegalArgumentException("names: " + names);
          }
          if (iterator.hasNext()) {
            final Path p = iterator.next();
            final String pName = p.name();
            if (pName.isEmpty()) {
              // Skip the root
              assert p.parent() == null;
            } else if (pName.equals(name)) {
              path = p;
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
    return this.leadingPath(startingType, List.of()) != null;
  }

  public final boolean startsWith(final Type startingType, final List<String> names) {
    return this.leadingPath(startingType, names) != null;
  }

  @Convenience
  public final Type parentType() {
    final Path parent = this.parent();
    return parent == null ? this.targetType() : parent.targetType();
  }

  @Convenience
  public final Class<?> parentClass() {
    return Types.rawClass(this.parentType());
  }

  @Convenience
  public final String parentName() {
    final Path parent = this.parent();
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
    Path path = this;
    Path parent = path.parent();
    while (parent != null) {
      dq.addFirst(path.name());
      path = parent;
      parent = path.parent();
    }
    return List.copyOf(dq);
  }

  public final int length() {
    Path path = this;
    Path parent = path.parent();
    int size = 1;
    while (parent != null) {
      path = parent;
      parent = path.parent();
      size++;
    }
    return size;
  }

  @Convenience
  public static final Path of(final Type rootType,
                              final String name0, final Type type0) {
    return
      new Path(rootType)
      .plus(name0, type0);
  }

  @Convenience
  public static final Path of(final Type rootType,
                              final String name0, final Type type0,
                              final String name1, final Type type1) {
    return
      new Path(rootType)
      .plus(name0, type0)
      .plus(name1, type1);
  }

  @Convenience
  public static final Path of(final Type rootType,
                              final String name0, final Type type0,
                              final String name1, final Type type1,
                              final String name2, final Type type2) {
    return
      new Path(rootType)
      .plus(name0, type0)
      .plus(name1, type1)
      .plus(name2, type2);
  }

  @Convenience
  public static final Path of(final Type rootType,
                              final String name0, final Type type0,
                              final String name1, final Type type1,
                              final String name2, final Type type2,
                              final String name3, final Type type3) {
    return
      new Path(rootType)
      .plus(name0, type0)
      .plus(name1, type1)
      .plus(name2, type2)
      .plus(name3, type3);
  }

  @Convenience
  public static final Path of(final Type rootType,
                              final String name0, final Type type0,
                              final String name1, final Type type1,
                              final String name2, final Type type2,
                              final String name3, final Type type3,
                              final String name4, final Type type4) {
    return
      new Path(rootType)
      .plus(name0, type0)
      .plus(name1, type1)
      .plus(name2, type2)
      .plus(name3, type3)
      .plus(name4, type4);
  }

  @Convenience
  public static final Path of(final Type rootType,
                              final String name0, final Type type0,
                              final String name1, final Type type1,
                              final String name2, final Type type2,
                              final String name3, final Type type3,
                              final String name4, final Type type4,
                              final String name5, final Type type5) {
    return
      new Path(rootType)
      .plus(name0, type0)
      .plus(name1, type1)
      .plus(name2, type2)
      .plus(name3, type3)
      .plus(name4, type4)
      .plus(name5, type5);
  }

  @Convenience
  public static final Path of(final Type rootType, final Object... components) {
    Path path = new Path(rootType);
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
          assert name != null;
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
