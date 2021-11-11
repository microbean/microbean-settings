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

public final class Path {


  /*
   * Static fields.
   */


  private static final StackWalker stackWalker = StackWalker.getInstance();

  private static final Path ROOT = new Path(List.of(Accessor.root()), true);


  /*
   * Instance fields.
   */


  private final List<Accessor> accessors;

  private final boolean transliterated;


  /*
   * Constructors.
   */


  private Path(final List<? extends Accessor> accessors, final boolean transliterated) {
    super();
    final int size = accessors.size();
    switch (size) {
    case 0:
      throw new IllegalArgumentException("accessors.isEmpty()");
    default:
      final List<Accessor> newList = new ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        final Accessor a = Objects.requireNonNull(accessors.get(i));
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
  public final Path transliterate(final BiFunction<? super String, ? super Accessor, ? extends Accessor> f) {
    if (f == null || this.transliterated) {
      return this;
    } else {
      final String userPackageName = stackWalker.walk(Path::findUserPackageName);
      final int size = this.size();
      final List<Accessor> newAccessors = new ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        newAccessors.add(f.apply(userPackageName, this.get(i)));
      }
      return new Path(newAccessors, true);
    }
  }

  public final boolean isEmpty() {
    return this.accessors.isEmpty();
  }

  public final boolean isRoot() {
    return this.size() == 1 && this.first().isRoot();
  }

  public final boolean isAbsolute() {
    return this.size() > 0 && this.first().isRoot();
  }

  public final int size() {
    return this.accessors.size();
  }

  public final Path plus(final String name, final Type type) {
    return this.plus(Accessor.of(name, type));
  }

  public final Path plus(final Accessor accessor) {
    return this.plus(List.of(accessor));
  }

  public final Path plus(final Path path) {
    return this.plus(path.accessors);
  }

  public final Path plus(final List<? extends Accessor> accessors) {
    if (accessors.isEmpty()) {
      return this;
    } else {
      final List<Accessor> newAccessors = new ArrayList<>(this.accessors.size() + accessors.size());
      newAccessors.addAll(this.accessors);
      newAccessors.addAll(accessors);
      return new Path(newAccessors, false);
    }
  }

  public final Accessor get(final int index) {
    return this.accessors.get(index);
  }

  public final Accessor first() {
    return this.get(0);
  }

  public final Accessor last() {
    return this.accessors.get(this.size() - 1);
  }

  public final Type type() {
    final Type type = this.last().type().orElse(null);
    assert type != null : "Untyped final Accessor: " + this.last();
    return type;
  }

  public final ClassLoader classLoader() {
    return Types.erase(this.type()).getClassLoader();
  }

  public final int indexOf(final Path other) {
    return other == this ? 0 : Collections.indexOfSubList(this.accessors, other.accessors);
  }

  public final int indexOf(final Path path, final BiPredicate<? super Accessor, ? super Accessor> p) {
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

  public final int lastIndexOf(final Path other) {
    return other == this ? 0 : Collections.lastIndexOfSubList(this.accessors, other.accessors);
  }

  public final int lastIndexOf(final Path path, final BiPredicate<? super Accessor, ? super Accessor> p) {
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

  public final boolean startsWith(final Path other) {
    if (other == this) {
      return true;
    } else if (other == null) {
      return false;
    } else {
      return this.indexOf(other) == 0;
    }
  }

  public final boolean startsWith(final Path other, final BiPredicate<? super Accessor, ? super Accessor> p) {
    return this.indexOf(other, p) == 0;
  }

  public final boolean endsWith(final Path other) {
    if (other == this) {
      return true;
    } else if (other == null) {
      return false;
    } else {
      final int lastIndex = this.lastIndexOf(other);
      return lastIndex >= 0 && lastIndex + other.size() == this.size();
    }
  }

  public final boolean endsWith(final Path other, final BiPredicate<? super Accessor, ? super Accessor> p) {
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
      return this.accessors.equals(((Path)other).accessors);
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


  public static final Path root() {
    return ROOT;
  }

  public static final Path of(final Accessor accessor) {
    return of(List.of(accessor));
  }

  public static final Path of(final List<? extends Accessor> accessors) {
    if (accessors.isEmpty()) {
      throw new IllegalArgumentException("accessors.isEmpty()");
    } else if (accessors.size() == 1 && accessors.get(0).isRoot()) {
      return root();
    } else {
      return new Path(accessors, false);
    }
  }

  public static final Path of(final String name) {
    return new Path(List.of(Accessor.of(name, null, (List<? extends Class<?>>)null, (List<? extends String>)null)), false);
  }

  public static final Path of(final Type type) {
    return new Path(List.of(Accessor.of("", type, (List<? extends Class<?>>)null, (List<? extends String>)null)), false);
  }

  private static final String findUserPackageName(final Stream<StackFrame> stream) {
    final String className = stream.sequential()
      .dropWhile(f -> f.getClassName().startsWith(Path.class.getPackageName()))
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

    private final Accessor.Parser parser;

    public Parser(final ClassLoader cl) {
      super();
      this.cl = Objects.requireNonNull(cl, "cl");
      this.parser = new Accessor.Parser(cl);
    }

    public final Path parse(final CharSequence s) throws ClassNotFoundException {
      if (s.isEmpty()) {
        throw new IllegalArgumentException("s.isEmpty()");
      } else {
        final List<Accessor> accessors = new ArrayList<>(11);
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
          return Path.root();
        } else {
          return new Path(accessors, false);
        }
      }
    }

  }

}
