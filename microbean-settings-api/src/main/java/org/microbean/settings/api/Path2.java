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

import java.lang.StackWalker.StackFrame;

import java.lang.reflect.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;

import java.util.stream.Stream;

import org.microbean.development.annotation.Experimental;

import org.microbean.type.Types;

public final class Path2 {


  /*
   * Static fields.
   */


  private static final StackWalker stackWalker = StackWalker.getInstance();

  private static final Path2 EMPTY = new Path2(List.of(), true);

  private static final Path2 ROOT = new Path2(List.of(Accessor2.root()), true);


  /*
   * Instance fields.
   */


  private final List<Accessor2> accessors;

  private final boolean transliterated;


  /*
   * Constructors.
   */


  private Path2(final List<? extends Accessor2> accessors, final boolean transliterated) {
    super();
    final int size = accessors.size();
    switch (size) {
    case 0:
      this.accessors = List.of();
      this.transliterated = true;
      break;
    default:
      final List<Accessor2> newList = new ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        final Accessor2 a = Objects.requireNonNull(accessors.get(i));
        if (i != 0 && (a.isRoot() || i + 1 >= size && a.type().isEmpty())) {
          throw new IllegalArgumentException("accessors: " + accessors);
        }
        newList.add(a);
      }
      this.accessors = Collections.unmodifiableList(newList);
      this.transliterated = transliterated;
    }
  }


  /*
   * Instance methods.
   */


  public final boolean isTransliterated() {
    return this.transliterated;
  }

  @Experimental
  public final Path2 transliterate(final BiFunction<? super String, ? super Accessor2, ? extends Accessor2> f) {
    if (f == null || this.transliterated) {
      return this;
    } else {
      final String userPackageName = stackWalker.walk(Path2::findUserPackageName);
      final int size = this.size() - 1; // don't include our trailing type
      final List<Accessor2> newAccessors = new ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        newAccessors.add(f.apply(userPackageName, this.get(i)));
      }
      return new Path2(newAccessors, true);
    }
  }

  public final boolean isEmpty() {
    return this.accessors.isEmpty();
  }

  public final boolean isRoot() {
    return this.size() == 1 && this.get(0).isRoot();
  }

  public final boolean isAbsolute() {
    return this.size() > 0 && this.get(0).isRoot();
  }

  public final int size() {
    return this.accessors.size();
  }

  public final Path2 plus(final String name, final Type type) {
    return this.plus(Accessor2.of(name, type));
  }

  public final Path2 plus(final Accessor2 accessor) {
    return this.plus(List.of(accessor));
  }

  public final Path2 plus(final Path2 path) {
    return this.plus(path.accessors);
  }

  public final Path2 plus(final List<? extends Accessor2> accessors) {
    final List<Accessor2> newAccessors = new ArrayList<>(this.accessors.size() + accessors.size());
    newAccessors.addAll(this.accessors);
    newAccessors.addAll(accessors);
    return new Path2(newAccessors, false);
  }

  public final Accessor2 get(final int index) {
    return this.accessors.get(index);
  }

  public final Type type() {
    final Type type = this.get(this.size() - 1).type().orElse(null);
    assert type != null : "Untyped final Accessor2: " + this.get(this.size() - 1);
    return type;
  }

  public final ClassLoader classLoader() {
    return Types.erase(this.type()).getClassLoader();
  }

  public final int indexOf(final Path2 other) {
    return other == this ? 0 : Collections.indexOfSubList(this.accessors, other.accessors);
  }

  public final int indexOf(final Path2 path, final BiPredicate<? super Accessor2, ? super Accessor2> p) {
    final int pathSize = path.size();
    final int sizeDiff = this.size() - pathSize;
    OUTER_LOOP:
    for (int i = 0; i <= sizeDiff; i++) {
      for (int j = 0, k = i; j < pathSize; j++, k++) {
        if (!p.test(this.accessors.get(k), path.accessors.get(j))) {
          continue OUTER_LOOP;
        }
      }
      return i;
    }
    return -1;
  }

  public final int lastIndexOf(final Path2 other) {
    return other == this ? 0 : Collections.lastIndexOfSubList(this.accessors, other.accessors);
  }

  public final int lastIndexOf(final Path2 path, final BiPredicate<? super Accessor2, ? super Accessor2> p) {
    final int pathSize = path.size();
    final int sizeDiff = this.size() - pathSize;
    OUTER_LOOP:
    for (int i = sizeDiff; i >= 0; i--) {
      for (int j = 0, k = i; j < pathSize; j++, k++) {
        if (!p.test(this.accessors.get(k), path.accessors.get(j))) {
          continue OUTER_LOOP;
        }
      }
      return i;
    }
    return -1;
  }

  public final boolean startsWith(final Path2 other) {
    if (other == this) {
      return true;
    } else if (other == null) {
      return false;
    } else {
      return this.indexOf(other) == 0;
    }
  }

  public final boolean startsWith(final Path2 other, final BiPredicate<? super Accessor2, ? super Accessor2> p) {
    return this.indexOf(other, p) == 0;
  }

  public final boolean endsWith(final Path2 other) {
    if (other == this) {
      return true;
    } else if (other == null) {
      return false;
    } else {
      final int lastIndex = this.lastIndexOf(other);
      return lastIndex >= 0 && lastIndex + other.size() == this.size();
    }
  }

  public final boolean endsWith(final Path2 other, final BiPredicate<? super Accessor2, ? super Accessor2> p) {
    final int lastIndex = this.lastIndexOf(other, p);
    return lastIndex >= 0 && lastIndex + other.size() == this.size();
  }

  @Override // Object
  public final int hashCode() {
    return this.accessors.hashCode();
  }

  public final boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other != null && this.getClass() == other.getClass()) {
      return this.accessors.equals(((Path2)other).accessors);
    } else {
      return false;
    }
  }

  public final String toString() {
    final StringJoiner sj = new StringJoiner("/");
    for (final Object accessor : this.accessors) {
      sj.add(accessor.toString());
    }
    return sj.toString();
  }


  /*
   * Static methods.
   */


  public static final Path2 root() {
    return ROOT;
  }

  static final Path2 of() {
    return EMPTY;
  }

  public static final Path2 of(final Accessor2 accessor) {
    return of(List.of(accessor));
  }

  public static final Path2 of(final List<? extends Accessor2> accessors) {
    if (accessors.isEmpty()) {
      return of();
    } else if (accessors.size() == 1 && accessors.get(0).isRoot()) {
      return root();
    } else {
      return new Path2(accessors, false);
    }
  }

  public static final Path2 of(final String name) {
    return new Path2(List.of(Accessor2.of(name, null, (List<? extends Class<?>>)null, (List<? extends String>)null)), false);
  }

  public static final Path2 of(final Type type) {
    return new Path2(List.of(Accessor2.of("", type, (List<? extends Class<?>>)null, (List<? extends String>)null)), false);
  }

  private static final String findUserPackageName(final Stream<StackFrame> stream) {
    final String className = stream.sequential()
      .dropWhile(f -> f.getClassName().startsWith(Path2.class.getPackageName()))
      .dropWhile(f -> f.getClassName().contains(".$Proxy")) // skip JDK proxies (and any other kind of proxies)
      .map(StackFrame::getClassName)
      .findFirst()
      .orElse(null);
    if (className == null) {
      return "";
    } else {
      final int lastIndex = className.lastIndexOf('.');
      if (lastIndex < 0) {
        return "";
      } else if (lastIndex == 0) {
        throw new AssertionError("className: " + className);
      } else {
        return className.substring(0, lastIndex);
      }
    }
  }

  public static final class Parser {

    private final ClassLoader cl;

    private final Accessor2.Parser parser;

    public Parser(final ClassLoader cl) {
      super();
      this.cl = Objects.requireNonNull(cl, "cl");
      this.parser = new Accessor2.Parser(cl);
    }

    public final Path2 parse(final CharSequence s) throws ClassNotFoundException {
      if (s.isEmpty()) {
        return Path2.of();
      } else {
        final List<Accessor2> accessors = new ArrayList<>(11);
        final int length = s.length();
        int start = 0;
        for (int i = 0; i < length; i++) {
          final int c = s.charAt(i);
          switch (c) {
          case '/':
            if (i + 1 < length) {
              accessors.add(this.parser.parse(s.subSequence(start, i)));
            } else {
              accessors.add(this.parser.parse(""));
            }
            start = i + 1;
            break;
          case '\\':
            if (i + 2 < length && s.charAt(i + 1) == '/') {
              i += 2;
            }
            break;
          default:
            break;
          }
        }
        // Cleanup
        if (start < length) {
          accessors.add(this.parser.parse(s.subSequence(start, length)));
        }
        assert !accessors.isEmpty();
        if (accessors.size() == 1 && accessors.get(0).isRoot()) {
          return Path2.root();
        } else {
          return new Path2(accessors, false);
        }
      }
    }

  }

}
