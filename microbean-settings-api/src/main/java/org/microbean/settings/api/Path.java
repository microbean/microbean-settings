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
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

import java.util.stream.Stream;

import org.microbean.development.annotation.Convenience;

public final class Path implements Cloneable, Comparable<Path>, Iterable<Qualified<Type>> {


  /*
   * Static fields.
   */


  private static final Type BOTTOM_TYPE = BottomType.class;

  private static final Path ROOT = new Path();


  /*
   * Instance fields.
   */


  private final List<Qualified<Type>> list;

  private final boolean absolute;


  /*
   * Constructors.
   */

  private Path() {
    super();
    this.list = List.of(QualifiedRecord.of(BOTTOM_TYPE));
    this.absolute = true;
  }

  private Path(final Qualifiers qualifiers) {
    super();
    this.list = List.of(QualifiedRecord.of(qualifiers, BOTTOM_TYPE));
    this.absolute = true;
  }

  // Copy constructor
  private Path(final Path path) {
    super();
    this.list = List.copyOf(path.list);
    this.absolute = path.absolute;
  }

  private Path(final Path path, final Qualified<Type> element, final boolean absolute) {
    this(path.list, element, absolute);
  }

  private Path(final Qualified<Type> element, final boolean absolute) {
    super();
    this.list = List.of(Objects.requireNonNull(element, "element"));
    this.absolute = absolute;
  }

  private Path(final List<? extends Qualified<Type>> list, final boolean absolute) {
    super();
    if (list.isEmpty()) {
      throw new IllegalArgumentException("list.isEmpty()");
    }
    this.list = List.copyOf(list);
    this.absolute = absolute || this.list.isEmpty();
  }

  private Path(final List<? extends Qualified<Type>> list, final Qualified<Type> element, final boolean absolute) {
    super();
    Objects.requireNonNull(element, "element");
    final int size = list == null ? 0 : list.size();
    if (size <= 0) {
      this.list = List.of(element);
    } else {
      final List<Qualified<Type>> newList = new ArrayList<>(size + 1);
      newList.addAll(list);
      newList.add(element);
      this.list = Collections.unmodifiableList(newList);
    }
    this.absolute = absolute;
  }


  /*
   * Instance methods.
   */


  @Override // Cloneable
  public final Path clone() {
    try {
      return (Path)super.clone();
    } catch (final CloneNotSupportedException cloneNotSupportedException) {
      throw new AssertionError(cloneNotSupportedException.getMessage(), cloneNotSupportedException);
    }
  }

  public final boolean isAbsolute() {
    return this.absolute;
  }

  @Convenience
  public final Qualifiers rootQualifiers() {
    return this.list.get(0).qualifiers();
  }

  @Convenience
  public final Qualified<Type> target() {
    return this.list.get(this.list.size() - 1);
  }

  @Convenience
  public final boolean startsWith(final Qualified<Type> element) {
    return Objects.equals(this.list.get(0), element);
  }

  public final boolean startsWith(final Qualifiers qualifiers, final boolean subset, final Type type, final boolean subtype) {
    return matches(this.list.get(0), qualifiers, subset, type, subtype);
  }

  public final Path plus(final Path path) {
    if (path.isAbsolute()) {
      throw new IllegalArgumentException("path.isAbsolute(): " + path);
    }
    final List<Qualified<Type>> list = new ArrayList<>(this.list.size() + path.list.size());
    list.addAll(this.list);
    list.addAll(path.list);
    return new Path(list, this.isAbsolute());
  }

  public final Path plus(final Qualified<Type> element) {
    return new Path(list, element, this.isAbsolute());
  }

  @Convenience
  public final Path plus(final Type type) {
    return plus(QualifiedRecord.of(type));
  }

  public final boolean contains(final Path other) {
    return this.list.containsAll(other.list);
  }

  public final Qualified<Type> get(final int index) {
    return this.list.get(index);
  }

  public final int indexOf(final Qualified<Type> element) {
    return this.list.indexOf(element);
  }

  public final int indexOf(final Qualified<Type> element, final Predicate<? super Qualified<Type>> p) {
    for (int i = 0; i < this.list.size(); i++) {
      if (p.test(this.list.get(i))) {
        return i;
      }
    }
    return -1;
  }

  public final int indexOf(final Path path) {
    if (!this.absolute && path.absolute) {
      return -1;
    } else {
      // Probably more optimized than our indexOf(Path, BiPredicate)
      // method.
      return Collections.indexOfSubList(this.list, path.list);
    }
  }

  public final int indexOf(final Path path, final BiPredicate<? super Qualified<Type>, ? super Qualified<Type>> p) {
    if (!this.absolute && path.absolute) {
      return -1;
    } else {
      final int pathSize = path.list.size();
      final int sizeDiff = this.list.size() - pathSize;
      OUTER_LOOP:
      for (int i = 0; i <= sizeDiff; i++) {
        for (int j = 0, k = i; j < pathSize; j++, k++) {
          if (!p.test(this.list.get(k), path.list.get(j))) {
            continue OUTER_LOOP;
          }
        }
        return i;
      }
      return -1;
    }
  }

  public final int lastIndexOf(final Path path, final BiPredicate<? super Qualified<Type>, ? super Qualified<Type>> p) {
    if (!this.absolute && path.absolute) {
      return -1;
    } else {
      final int pathSize = path.list.size();
      final int sizeDiff = this.list.size() - pathSize;
      OUTER_LOOP:
      for (int i = sizeDiff; i >= 0; i--) {
        for (int j = 0, k = i; j < pathSize; j++, k++) {
          if (!p.test(this.list.get(k), path.list.get(j))) {
            continue OUTER_LOOP;
          }
        }
        return i;
      }
      return -1;
    }
  }

  public final int lastIndexOf(final Qualified<Type> element) {
    return this.list.lastIndexOf(element);
  }

  public final int lastIndexOf(final Qualified<Type> element, final Predicate<? super Qualified<Type>> p) {
    for (int i = this.list.size(); i >= 0; i--) {
      if (p.test(this.list.get(i))) {
        return i;
      }
    }
    return -1;
  }

  public final int lastIndexOf(final Path path) {
    if (!this.absolute && path.absolute) {
      return -1;
    } else {
      // Probably more optimized than our lastIndexOf(Path,
      // BiPredicate) method.
      return Collections.lastIndexOfSubList(this.list, path.list);
    }
  }

  @Convenience
  public final List<Path> splitAfter(final Type type) {
    return this.splitAfter(QualifiedRecord.of(type));
  }

  @Convenience
  public final List<Path> splitAfter(final Qualified<Type> element) {
    final int i = this.list.indexOf(element);
    return i < 0 ? List.of(this) : this.split(i);
  }

  @Convenience
  public final List<Path> splitAfterLast(final Qualified<Type> element) {
    final int i = this.list.lastIndexOf(element);
    return i < 0 ? List.of(this) : this.split(i);
  }

  public final List<Path> splitAfter(final Qualifiers qualifiers, final boolean subset, final Type type, final boolean subtype) {
    int index = -1;
    for (int i = 0; i < this.list.size(); i++) {
      if (matches(this.list.get(i), qualifiers, subset, type, subtype)) {
        index = i;
        break;
      }
    }
    return this.split(index);
  }

  public final List<Path> splitAfterLast(final Qualifiers qualifiers, final boolean subset, final Type type, final boolean subtype) {
    int index = -1;
    for (int i = this.list.size() - 1; i >= 0; i--) {
      if (matches(this.list.get(i), qualifiers, subset, type, subtype)) {
        index = i;
        break;
      }
    }
    return this.split(index);
  }

  public final List<Path> split(final int i) {
    if (i < 0 || i >= this.list.size() || this.list.size() == 1) {
      return List.of(this);
    } else {
      return
        List.of(new Path(this.list.subList(0, i), this.isAbsolute()),
                new Path(this.list.subList(i, this.list.size()), false));
    }
  }

  @Convenience
  public final Type qualified(final int index) {
    final Qualified<Type> element = this.get(index);
    return element == null ? null : element.qualified();
  }

  @Convenience
  public final Qualifiers qualifiers(final int index) {
    final Qualified<Type> element = this.get(index);
    return element == null ? null : element.qualifiers();
  }

  public final int size() {
    return this.list.size();
  }

  public final boolean isRoot() {
    return this.list.size() == 1 && this.list.get(0).qualified() == BOTTOM_TYPE;
  }

  @Override // Iterable<Qualified<Type>>
  public final Iterator<Qualified<Type>> iterator() {
    return this.list.iterator();
  }

  public final Stream<Qualified<Type>> stream() {
    return this.list.stream().sequential();
  }

  @Override // Comparable<Path>
  public final int compareTo(final Path other) {
    if (other == null) {
      return -1; // nulls go to the right
    } else if (this.equals(other)) {
      return 0;
    } else {
      return Integer.signum(other.list.size() - this.list.size());
    }
  }

  @Override // Object
  public final int hashCode() {
    return this.list.hashCode();
  }

  @Override // Object
  public final boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other == null) {
      return false;
    } else if (this.getClass() == other.getClass()) {
      return this.list.equals(((Path)other).list);
    } else {
      return false;
    }
  }

  @Override // Object
  public final String toString() {
    final StringBuilder sb = new StringBuilder();
    if (this.absolute) {
      sb.append("/");
    }
    final Iterator<Qualified<Type>> i = this.iterator();
    if (i.hasNext()) {
      final Qualified<Type> e = i.next();
      if (e.qualified() == BOTTOM_TYPE) {
        final Qualifiers q = e.qualifiers();
        if (!q.isEmpty()) {
          sb.append(":").append(q.toString());
        }
      } else {
        sb.append(Qualified.toString(e, Path::toString));
      }
      if (i.hasNext()) {
        sb.append("/");
      }
    }
    while (i.hasNext()) {
      sb.append(Qualified.toString(i.next(), Path::toString));
      if (i.hasNext()) {
        sb.append("/");
      }
    }
    return sb.toString();
  }


  /*
   * Static methods.
   */


  public static final Path absoluteOf(final Type type) {
    return absoluteOf(QualifiedRecord.of(type));
  }

  public static final Path absoluteOf(final Qualified<Type> element) {
    return new Path(element, true);
  }

  public static final Path fragmentOf(final Type type) {
    return fragmentOf(QualifiedRecord.of(type));
  }

  public static final Path fragmentOf(final Qualified<Type> element) {
    return new Path(element, false);
  }

  public static final Path root() {
    return ROOT;
  }

  public static final Path root(final Qualifiers qualifiers) {
    return new Path(qualifiers);
  }

  private static final boolean matches(final Qualified<Type> element, final Qualifiers qualifiers, final boolean subset, final Type type, final boolean subtype) {
    if (subset) {
      if (!element.qualifiers().isAssignable(qualifiers)) {
        // elementQualifiers <- qualifiers failed
        return false;
      }
    } else if (!element.qualifiers().equals(qualifiers)) {
      return false;
    }
    final Type elementType = element.qualified();
    return subtype ? AssignableType.of(elementType).isAssignable(type) : elementType.equals(type);
  }

  private static final String toString(final Type type) {
    return type == null ? "null" : type.getTypeName();
  }


  /*
   * Inner and nested classes.
   */


  private static final class BottomType {

    private BottomType() {
      super();
    }

  }


}
