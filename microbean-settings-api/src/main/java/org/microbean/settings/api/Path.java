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
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.StringJoiner;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import java.util.stream.Stream;

import org.microbean.development.annotation.Convenience;

public final record Path(Path parent, String name, Type targetType) {


  /*
   * Constructors.
   */


  // Creates an absolute path
  @Convenience
  public Path() {
    this((Path)null, "", Object.class);
    assert this.absolute();
  }

  // Creates an absolute path
  @Convenience
  public Path(final Type targetType) {
    this((Path)null, "", targetType);
    assert this.absolute();
  }

  // Creates an absolute path
  @Convenience
  public Path(final Type parentType, final String name, final Type targetType) {
    this(new Path((Path)null, "", parentType), name, targetType);
    assert this.absolute();
  }

  // Creates a path fragment
  @Convenience
  public Path(final String name, final Type targetType) {
    this((Path)null, name, targetType);
    assert !this.absolute();
  }

  // Copy constructor
  @Convenience
  public Path(final Path path) {
    this(path.parent(), path.name(), path.targetType());
    assert this.equals(path);
  }

  public Path {
    Objects.requireNonNull(targetType, "targetType");
    Objects.requireNonNull(name, "name");
    if (name.isEmpty()) {
      if (parent != null) {
        throw new IllegalArgumentException("non-null parent with empty name: " + parent);
      }
    } else if (name.equals("*")) {
      throw new IllegalArgumentException("name: " + name);
    }
  }


  /*
   * Instance methods.
   */


  // Turns this Path into a fragment
  @Convenience
  public final Path detach() {
    final Path returnValue = this.parent() == null ? this : this.withParent(null);
    assert !returnValue.absolute();
    return returnValue;
  }

  public final Path withParent(final Path parent) {
    return Objects.equals(this.parent(), parent) ? this : new Path(parent, this.name(), this.targetType());
  }

  public final Path withName(final String name) {
    return Objects.equals(this.name(), name) ? this : new Path(this.parent(), name, this.targetType());
  }

  public final Path withTargetType(final Type targetType) {
    return Objects.equals(this.targetType(), targetType) ? this : new Path(this.parent(), this.name(), targetType);
  }

  public final boolean absolute() {
    final Path path = this.root();
    assert path.parent() == null;
    return path.name().isEmpty();
  }

  @Convenience // see all(Predicate)
  public final List<Path> all() {
    return this.all(Path::notNull);
  }

  @Convenience // see downstream(Predicate)
  public final List<Path> all(final Predicate<? super Path> predicate) {
    return this.downstream(predicate).toList();
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
    return this.upstream()
      .reduce(Path::returnParent)
      .orElseThrow();
  }

  public final boolean root(final Type type) {
    if (this.parent() == null) {
      assert this.name.isEmpty();
      return Objects.equals(this.targetType(), type);
    } else {
      return false;
    }
  }

  public final Path lastMatching(final Type startingType) {
    return this.lastMatching(startingType, null, null);
  }

  public final Path lastMatching(final Type startingType, final List<String> names) {
    return this.lastMatching(startingType, names, null);
  }

  public final Path lastMatching(final Type startingType,
                                 final List<String> names,
                                 final Type targetType) {
    final Predicate<Path> lastMatchingFilter =
      new LastMatchingFilter(startingType,
                             names == null || names.isEmpty() ? null : names.listIterator(names.size()),
                             targetType);
    return this.upstream()
      .filter(lastMatchingFilter)
      .findFirst()
      .orElse(null);
  }

  public final boolean endsWith(final Type startingType) {
    return this.endsWith(startingType, null);
  }

  public final boolean endsWith(final Type startingType, final List<String> names) {
    return Objects.equals(this, this.lastMatching(startingType, names));
  }

  public final Path firstMatching(final Type startingType) {
    return this.firstMatching(startingType, null, null);
  }

  public final Path firstMatching(final Type startingType, final List<? extends String> names) {
    return this.firstMatching(startingType, names, null);
  }
  
  public final Path firstMatching(final Type startingType, final List<? extends String> names, final Type targetType) {
    final Predicate<Path> firstMatchingFilter =
      new FirstMatchingFilter(startingType, names == null ? null : names.iterator(), targetType);
    return this.downstream()
      .filter(firstMatchingFilter)
      .reduce(Path::last)
      .orElse(null);
  }

  public final boolean startsWith(final Type startingType) {
    return this.startsWith(startingType, null);
  }

  public final boolean startsWith(final Type startingType, final List<String> names) {
    final Path firstMatching = this.firstMatching(startingType, names);
    return firstMatching != null && Objects.equals(startingType, firstMatching.root().targetType());
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
    return this.downstream()
      .filter(p -> p.parent() != null)
      .map(Path::name)
      .toList();
  }

  public final int length() {
    return this.upstream()
      .reduce(0, (i, p) -> i + 1, Integer::sum);
  }

  private final <I> Path traverse(final BiFunction<? super Path, I, ? extends I> intermediateFunction) {
    return this.traverse(Path::returnTrue, intermediateFunction, Path::returnPath);
  }

  private final <T, I> T traverse(final BiFunction<? super Path, I, ? extends I> intermediateFunction,
                                  final BiFunction<? super Path, I, ? extends T> terminalFunction) {
    return this.traverse(Path::returnTrue, intermediateFunction, terminalFunction);
  }

  private final <T, I> T traverse(final Predicate<? super Path> parentPredicate,
                                  final BiFunction<? super Path, I, ? extends I> intermediateFunction,
                                  final BiFunction<? super Path, I, ? extends T> terminalFunction) {
    Path path = this;
    Path parent = path.parent();
    I acc = null;
    while (parent != null && parentPredicate.test(parent)) {
      if ((acc = intermediateFunction.apply(path, acc)) == null) {
        break;
      }
      path = parent;
      parent = path.parent();
    }
    return terminalFunction.apply(path, acc);
  }

  @Convenience
  public final Stream<Path> downstream() {
    return this.downstream(null);
  }

  public final Stream<Path> downstream(final Predicate<? super Path> predicate) {
    return this.upstream(predicate)
      .<Deque<Path>>collect(ArrayDeque::new,
                            (dq, p) -> dq.addFirst(p),
                            Path::addAll)
      .stream();
  }

  public final Stream<Path> upstream() {
    return upstream(null);
  }

  public final Stream<Path> upstream(final Predicate<? super Path> predicate) {
    final Predicate<Path> notNull = Path::notNull;
    return Stream.iterate(this, predicate == null ? notNull : notNull.and(predicate), Path::parent);
  }

  public final Iterator<Path> ancestorIterator() {
    return new AncestorIterator(this);
  }

  public final Iterator<Path> descendantIterator() {
    return this.all().iterator();
  }


  /*
   * Static methods.
   */


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

  private static final boolean returnTrue(final Object ignored) {
    return true;
  }

  private static final Path returnPath(final Path p, final Object ignored) {
    return p;
  }

  private static final Path last(final Path p0, final Path p1) {
    return p1;
  }

  private static final Path returnParent(final Path path, final Path parent) {
    assert Objects.equals(path.parent(), parent);
    return parent;
  }

  private static final boolean notNull(final Path p) {
    return p != null;
  }

  private static final void addAll(final Collection<Path> c0, final Collection<Path> c1) {
    c0.addAll(c1);
  }

  private static final class AncestorIterator implements Iterator<Path> {

    private Path parent;

    private Path p;

    private AncestorIterator(final Path p) {
      super();
      this.p = p;
    }

    @Override
    public final boolean hasNext() {
      return this.p != null;
    }

    @Override
    public final Path next() {
      final Path p = this.p;
      this.p = p.parent();
      return p;
    }

  }

  private static final class FirstMatchingFilter implements Predicate<Path> {

    private Type startingType;

    private final Iterator<? extends String> nameIterator;

    private final Type targetType;

    private FirstMatchingFilter(final Type startingType,
                                final Iterator<? extends String> nameIterator,
                                final Type targetType) {
      super();
      this.startingType = startingType;
      this.nameIterator = nameIterator;
      this.targetType = targetType;
    }

    @Override
    public final boolean test(final Path p) {
      final boolean returnValue;
      if (p == null) {
        returnValue = false;
      } else if (this.startingType == null) {
        if (this.nameIterator == null) {
          if (this.targetType == null) {
            // Nothing to filter, so true.
            returnValue = true;
          } else {
            returnValue = Objects.equals(p.targetType(), this.targetType);
          }
        } else if (this.nameIterator.hasNext()) {
          returnValue = Objects.equals(p.name(), this.nameIterator.next());
        } else if (this.targetType != null) {
          returnValue = Objects.equals(p.targetType(), this.targetType);
        } else {
          // Yes, false: we exhausted the names so everything
          // afterwards gets rejected
          returnValue = false;
        }
      } else if (Objects.equals(p.targetType(), this.startingType)) {
        this.startingType = null; // record that a match took place
        returnValue = true;
      } else {
        returnValue = false;
      }
      return returnValue;
    }

  }

  private static final class LastMatchingFilter implements Predicate<Path> {

    private final Type startingType;

    private final ListIterator<? extends String> nameIterator;

    private Type targetType;

    private LastMatchingFilter(final Type startingType,
                               final ListIterator<? extends String> nameIterator,
                               final Type targetType) {
      super();
      this.startingType = Objects.requireNonNull(startingType, "startingType");
      this.nameIterator = nameIterator == null ? List.<String>of().listIterator() : nameIterator;
      this.targetType = targetType;
    }

    @Override
    public final boolean test(final Path p) {
      final boolean returnValue;
      if (p == null) {
        returnValue = false;
      } else if (this.targetType != null) {
        if (Objects.equals(p.targetType(), this.targetType)) {
          this.targetType = null; // signal a match
          returnValue = true;
        } else {
          returnValue = false;
        }
      } else if (!this.nameIterator.hasPrevious()) {
        returnValue = Objects.equals(p.targetType(), this.startingType);
      } else {
        returnValue = Objects.equals(p.name(), this.nameIterator.previous());
      }
      return returnValue;
    }

  }

}
