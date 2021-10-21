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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;

import java.util.function.Predicate;

import java.util.stream.Stream;

public record Accessor(Accessor parent, Qualified<String> name, Qualified<Type> targetType) implements Qualified<Accessor> {


  /*
   * Static fields.
   */


  private static final Accessor ROOT = new Accessor();

  private static final Qualified<String> EMPTY_NAME = QualifiedRecord.of("");


  /*
   * Constructors.
   */


  private Accessor() {
    this(QualifiedRecord.of(RootType.INSTANCE));
  }

  public Accessor(final Type targetType) {
    this(null, QualifiedRecord.of(""), QualifiedRecord.of(targetType));
  }

  public Accessor(final Qualified<Type> targetType) {
    this(null, QualifiedRecord.of(""), targetType);
  }

  public Accessor(final String name, final Qualified<Type> targetType) {
    this(null, name, targetType);
  }

  public Accessor(final Accessor parent, final Qualified<Type> targetType) {
    this(parent, QualifiedRecord.of(""), targetType);
  }

  public Accessor(final Accessor parent, final Type targetType) {
    this(parent, QualifiedRecord.of(""), QualifiedRecord.of(targetType));
  }
  
  public Accessor(final Accessor parent, final String name, final Qualified<Type> targetType) {
    this(parent, name == null || name.equals("") ? QualifiedRecord.of("") : QualifiedRecord.of(name), targetType);
  }

  public Accessor {
    name = QualifiedRecord.of(name);
    targetType = QualifiedRecord.of(targetType);
    if (targetType.qualified() == RootType.INSTANCE) {
      if (!targetType.qualifiers().isEmpty()) {
        throw new IllegalArgumentException("root's targetType cannot have qualifiers: " + targetType.qualifiers());
      } else if (parent != null) {
        throw new IllegalArgumentException("root cannot have parent: " + parent);
      } else if (!"".equals(name.qualified())) {
        throw new IllegalArgumentException("root cannot have a non-empty name: " + name);
      }
    }
  }


  /*
   * Instance methods.
   */


  @Override // Assignable<Accessor>
  public final Accessor assignable() {
    return this;
  }
  
  @Override // Assignable<Accessor>
  public final boolean isAssignable(final Accessor payload) {
    // TODO: implement for real
    return Qualified.super.isAssignable(payload);
  }

  @Override // Qualified<Accessor>
  public final Qualifiers qualifiers() {
    return this.name().qualifiers();
  }

  @Override // Qualified<Accessor>
  public final Accessor qualified() {
    return this;
  }

  public final boolean isRoot() {
    return this.equals(root());
  }

  public final Qualified<Type> parentType() {
    final Accessor parent = this.parent();
    return parent == null ? null : parent.targetType();
  }

  public final Accessor head() {
    Accessor a = this;
    while (a != null && a.parent() != null) {
      a = a.parent();
    }
    return a;
  }

  public final boolean isAbsolute() {
    return this.head().isRoot();
  }

  public final boolean isRelative() {
    return !this.isAbsolute();
  }

  public final Accessor plus(final Type targetType) {
    return new Accessor(this, EMPTY_NAME, QualifiedRecord.of(targetType));
  }
  
  public final Accessor plus(final Qualified<Type> targetType) {
    return new Accessor(this, EMPTY_NAME, targetType);
  }

  public final Accessor plus(final Qualified<String> name, final Type targetType) {
    return new Accessor(this, name, QualifiedRecord.of(targetType));
  }
  
  public final Accessor plus(final Qualified<String> name, final Qualified<Type> targetType) {
    return new Accessor(this, name, targetType);
  }

  public final boolean endsWith(final Accessor other) {
    if (other == null) {
      return false;
    } else {
      final Iterator<Accessor> mine = this.upstream().iterator();
      final Iterator<Accessor> hers = other.upstream().iterator();
      while (hers.hasNext()) {
        if (mine.hasNext()) {
          final Accessor herBit = hers.next();
          final Accessor myBit = mine.next();
          if (herBit.targetType().equals(myBit.targetType())) {
            if (herBit.name().equals(myBit.name())) {
              final Accessor herBitParent = herBit.parent();
              if (herBitParent == null) {
                assert !hers.hasNext();
                return true;
              } else if (!herBitParent.equals(this.parent())) {
                return false;
              } else {
                // continue
              }
            } else {
              return false;
            }
          } else {
            return false;
          }
        } else {
          return false;
        }
      }
      return !mine.hasNext();
    }
  }

  public final int length() {
    if (this.parent() == null) {
      return 1;
    } else {
      final int[] i = new int[] { 0 };
      this.upstream().forEach(a -> i[0]++);
      return i[0];
    }
  }

  public final Stream<Accessor> upstream() {
    return Stream.iterate(this, a -> a != null, a -> a.parent());
  }

  public final Stream<Accessor> downstream() {
    return downstream(Accessor::returnTrue);
  }
  
  public final Stream<Accessor> downstream(final Predicate<? super Accessor> p) {
    return
      this.upstream()
      .filter(p)
      .<Deque<Accessor>>collect(ArrayDeque::new,
                                (dq, a) -> dq.addFirst(a),
                                Accessor::addAll)
      .stream();
  }

  public final List<Qualified<?>> toList() {
    final List<Qualified<?>> list;
    if (this.parent() == null) {
      return List.of(this.name(), this.targetType());
    } else {
      list = new ArrayList<>();
      this.downstream()
        .filter(Predicate.not(Accessor::isRoot))
        .forEach(a -> {
            list.add(a.name());
            list.add(a.targetType());
          });
      return Collections.unmodifiableList(list);
    }
  }

  @Override
  public final String toString() {
    final StringJoiner sj = new StringJoiner("/");
    final Accessor parent = this.parent();
    if (parent != null) {
      sj.add(parent.toString());
    }
    final String name = Qualified.toString(this.name());
    if (name != null && !name.isEmpty()) {
      sj.add(name);
    }
    sj.add(Qualified.toString(this.targetType(), Accessor::toString));
    return sj.toString();
  }


  /*
   * Static methods.
   */


  public static final Accessor root() {
    return ROOT;
  }

  public static final Accessor of(final Accessor parent, final Qualified<String> name, final Qualified<Type> type) {
    return new Accessor(parent, name, type);
  }

  public static final Accessor of(final Accessor parent, final String name, final Qualified<Type> type) {
    return new Accessor(parent, name, type);
  }

  public static final Accessor of(final Accessor parent, final Type type) {
    return new Accessor(parent, type);
  }
  
  public static final Accessor of(final Type type) {
    return new Accessor(type);
  }

  private static final String toString(final Type type) {
    return type == null ? "null" : type.getTypeName();
  }

  private static final void addAll(final Collection<Accessor> c0, final Collection<Accessor> c1) {
    c0.addAll(c1);
  }

  private static final boolean returnTrue(final Object ignored) {
    return true;
  }
  
  private static final class RootType implements Type {

    private static final Type INSTANCE = new RootType();
    
    private RootType() {
      super();
    }

    @Override
    public final String toString() {
      return "/";
    }
    
  }
  
}
