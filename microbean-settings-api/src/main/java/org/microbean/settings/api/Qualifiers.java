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
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.TreeSet;

import java.util.function.Predicate;

import java.util.stream.Stream;

import static java.util.Collections.emptySortedMap;
import static java.util.Collections.emptySortedSet;
import static java.util.Collections.unmodifiableSortedMap;
import static java.util.Collections.unmodifiableSortedSet;

public final class Qualifiers {


  private static final Qualifiers EMPTY_QUALIFIERS = new Qualifiers();


  /*
   * Instance fields.
   */


  private final SortedMap<String, ?> qualifiers;


  /*
   * Constructors.
   */


  private Qualifiers() {
    this(emptySortedMap());
  }

  private Qualifiers(final Qualifier<?> qualifier) {
    this(new TreeMap<>(Map.of(qualifier.name(), qualifier.value())));
  }

  private Qualifiers(final SortedSet<Qualifier<?>> qualifiers) {
    this(toMap(qualifiers));
  }

  private Qualifiers(final SortedMap<String, ?> qualifiers) {
    super();
    this.qualifiers = qualifiers == null || qualifiers.isEmpty() ? emptySortedMap() : unmodifiableSortedMap(new TreeMap<>(qualifiers));
  }


  /*
   * Instance methods.
   */


  public final SortedMap<String, ?> qualifiers() {
    return this.qualifiers;
  }

  public final boolean isEmpty() {
    return this.qualifiers().isEmpty();
  }

  public final int size() {
    return this.qualifiers().size();
  }

  public final Set<? extends Entry<String, ?>> entrySet() {
    return this.qualifiers().entrySet();
  }

  public final Set<String> keySet() {
    return this.qualifiers().keySet();
  }

  public final boolean contains(final Qualifiers her) {
    return this.size() >= her.size() && this.entrySet().containsAll(her.entrySet());
  }

  public final Object get(final String name) {
    return this.qualifiers.get(name);
  }

  public final Stream<Qualifier<?>> stream() {
    return this.qualifiers().entrySet().stream().map(Qualifier::of);
  }

  public final boolean isSubsetOf(final Qualifiers other) {
    return other.contains(this);
  }

  public final SortedSet<Qualifier<?>> toQualifiers() {
    final SortedMap<? extends String, ?> q = this.qualifiers();
    return q.isEmpty() ? emptySortedSet() : unmodifiableSortedSet(new TreeSet<>(q.entrySet().stream().map(Qualifier::of).toList()));
  }

  public final int intersectionSize(final Qualifiers q1) {
    if (q1 == null || q1.isEmpty()) {
      return 0;
    } else if (this == q1) {
      // Just an identity check to rule this easy case out.
      return this.size();
    } else {
      final Set<? extends Entry<String, ?>> q1EntrySet = q1.entrySet();
      return (int)this.entrySet().stream()
        .filter(q1EntrySet::contains)
        .count();
    }
  }
  
  public final int symmetricDifferenceSize(final Qualifiers q1) {
    if (q1 == null || q1.isEmpty()) {
      return this.size();
    } else if (this == q1) {
      // Just an identity check to rule this easy case out.
      return 0;
    } else {
      final Set<Entry<?, ?>> q1SymmetricDifference = new HashSet<>(this.entrySet());
      q1.entrySet().stream()
        .filter(Predicate.not(q1SymmetricDifference::add))
        .forEach(q1SymmetricDifference::remove);
      return q1SymmetricDifference.size();
    }
  }

  @Override // Object
  public final int hashCode() {
    return this.qualifiers().hashCode();
  }

  @Override // Object
  public final boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other == null || this.getClass() != other.getClass()) {
      return false;
    } else {
      return this.qualifiers().equals(((Qualifiers)other).qualifiers());
    }
  }

  @Override // Object
  public final String toString() {
    final StringJoiner sj = new StringJoiner(";");
    this.stream().map(Qualifier::toString).forEach(sj::add);
    return sj.toString();
  }


  /*
   * Static methods.
   */


  public static final Qualifiers of() {
    return EMPTY_QUALIFIERS;
  }

  public static final <T> Qualifiers of(final SortedMap<? extends CharSequence, T> map) {
    if (map == null || map.isEmpty()) {
      return of();
    } else {
      final SortedMap<String, T> newMap = new TreeMap<>();
      map.entrySet().forEach(e -> newMap.put(e.getKey().toString(), e.getValue()));
      return new Qualifiers(newMap);
    }
  }

  public static final <T> Qualifiers of(final SortedSet<? extends Qualifier<T>> set) {
    return set == null || set.isEmpty() ? of() : of(toMap(set));
  }

  public static final Qualifiers of(final String name0, final Object value0) {
    return new Qualifiers(Qualifier.of(name0, value0));
  }

  public static final Qualifiers of(final String name0, final Object value0,
                                    final String name1, final Object value1) {
    final SortedMap<String, Object> map = new TreeMap<>();
    map.put(name0, value0);
    map.put(name1, value1);
    return new Qualifiers(map);
  }

  public static final Qualifiers of(final String name0, final Object value0,
                                    final String name1, final Object value1,
                                    final String name2, final Object value2) {
    final SortedMap<String, Object> map = new TreeMap<>();
    map.put(name0, value0);
    map.put(name1, value1);
    map.put(name2, value2);
    return new Qualifiers(map);
  }

  public static final Qualifiers of(final Object... nameValuePairs) {
    if (nameValuePairs == null || nameValuePairs.length <= 0) {
      return Qualifiers.of();
    } else if (nameValuePairs.length % 2 != 0) {
      throw new IllegalArgumentException("nameValuePairs: " + Arrays.toString(nameValuePairs));
    } else {
      final SortedMap<String, Object> map = new TreeMap<>();
      for (int i = 0; i < nameValuePairs.length; i++) {
        map.put((String)nameValuePairs[i++], nameValuePairs[i]);
      }
      return new Qualifiers(map);
    }
  }

  private static final SortedMap<String, ?> toMap(final SortedSet<? extends Qualifier<?>> qs) {
    final SortedMap<String, Object> map = new TreeMap<>();
    qs.forEach(q -> map.put(q.name(), q.value()));
    return unmodifiableSortedMap(map);
  }

}
