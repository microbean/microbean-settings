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

/**
 * An immutable set of {@link String}-typed key-value pair coordinates
 * that locate configuration in configuration space.
 *
 * <p>This is a <a
 * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/doc-files/ValueBased.html">value-based
 * class</a>.</p>
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see #of(CharSequence, CharSequence)
 */
public final class Qualifiers {


  /*
   * Static fields.
   */


  private static final Qualifiers EMPTY_QUALIFIERS = new Qualifiers();


  /*
   * Instance fields.
   */


  private final SortedMap<String, String> qualifiers;


  /*
   * Constructors.
   */


  private Qualifiers() {
    this(emptySortedMap());
  }

  private Qualifiers(final Qualifier qualifier) {
    this(new TreeMap<>(Map.of(qualifier.name(), qualifier.value())));
  }

  private Qualifiers(final SortedSet<? extends Qualifier> qualifiers) {
    this(toMap(qualifiers));
  }

  private Qualifiers(final SortedMap<String, ? extends String> qualifiers) {
    super();
    this.qualifiers = qualifiers == null || qualifiers.isEmpty() ? emptySortedMap() : unmodifiableSortedMap(new TreeMap<>(qualifiers));
  }


  /*
   * Instance methods.
   */


  public final SortedMap<String, String> toMap() {
    return this.qualifiers;
  }

  public final boolean isEmpty() {
    return this.toMap().isEmpty();
  }

  public final int size() {
    return this.toMap().size();
  }

  public final Set<Entry<String, String>> entrySet() {
    return this.toMap().entrySet();
  }

  public final Set<String> keySet() {
    return this.toMap().keySet();
  }

  public final Object get(final CharSequence name) {
    return this.qualifiers.get(name.toString());
  }

  public final Stream<Qualifier> stream() {
    return this.toMap().entrySet().stream().map(Qualifier::of);
  }

  public final boolean contains(final Qualifiers other) {
    return this == other || this.size() >= other.size() && this.entrySet().containsAll(other.entrySet());
  }

  public final boolean isSubsetOf(final Qualifiers other) {
    return other == this || other.contains(this);
  }

  public final SortedSet<Qualifier> toQualifiers() {
    final SortedMap<? extends String, ? extends String> q = this.toMap();
    return q.isEmpty() ? emptySortedSet() : unmodifiableSortedSet(new TreeSet<>(q.entrySet().stream().map(Qualifier::of).toList()));
  }

  public final int intersectionSize(final Qualifiers other) {
    if (other == this) {
      // Just an identity check to rule this easy case out.
      return this.size();
    } else if (other == null || other.isEmpty()) {
      return 0;
    } else {
      final Set<? extends Entry<String, String>> otherEntrySet = other.entrySet();
      return (int)this.entrySet().stream()
        .filter(otherEntrySet::contains)
        .count();
    }
  }

  public final int symmetricDifferenceSize(final Qualifiers other) {
    if (other == this) {
      // Just an identity check to rule this easy case out.
      return 0;
    } else if (other == null || other.isEmpty()) {
      return this.size();
    } else {
      final Set<Entry<?, ?>> otherSymmetricDifference = new HashSet<>(this.entrySet());
      other.entrySet().stream()
        .filter(Predicate.not(otherSymmetricDifference::add))
        .forEach(otherSymmetricDifference::remove);
      return otherSymmetricDifference.size();
    }
  }

  @Override // Object
  public final int hashCode() {
    return this.toMap().hashCode();
  }

  @Override // Object
  public final boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other == null || this.getClass() != other.getClass()) {
      return false;
    } else {
      return this.toMap().equals(((Qualifiers)other).toMap());
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

  public static final Qualifiers of(final SortedMap<? extends CharSequence, ? extends CharSequence> map) {
    if (map == null || map.isEmpty()) {
      return of();
    } else {
      final SortedMap<String, String> newMap = new TreeMap<>();
      map.entrySet().forEach(e -> newMap.put(e.getKey().toString(), e.getValue().toString()));
      return new Qualifiers(newMap);
    }
  }

  public static final Qualifiers of(final SortedSet<? extends Qualifier> set) {
    return set == null || set.isEmpty() ? of() : of(toMap(set));
  }

  public static final Qualifiers of(final CharSequence value0) {
    return of("value", value0);
  }

  public static final Qualifiers of(final CharSequence name0, final CharSequence value0) {
    return new Qualifiers(Qualifier.of(name0, value0));
  }

  public static final Qualifiers of(final CharSequence name0, final CharSequence value0,
                                    final CharSequence name1, final CharSequence value1) {
    final SortedMap<String, String> map = new TreeMap<>();
    map.put(name0.toString(), value0.toString());
    map.put(name1.toString(), value1.toString());
    return new Qualifiers(map);
  }

  public static final Qualifiers of(final CharSequence name0, final CharSequence value0,
                                    final CharSequence name1, final CharSequence value1,
                                    final CharSequence name2, final CharSequence value2) {
    final SortedMap<String, String> map = new TreeMap<>();
    map.put(name0.toString(), value0.toString());
    map.put(name1.toString(), value1.toString());
    map.put(name2.toString(), value2.toString());
    return new Qualifiers(map);
  }

  public static final Qualifiers of(final CharSequence... nameValuePairs) {
    if (nameValuePairs == null || nameValuePairs.length <= 0) {
      return Qualifiers.of();
    } else if (nameValuePairs.length % 2 != 0) {
      throw new IllegalArgumentException("nameValuePairs: " + Arrays.toString(nameValuePairs));
    } else {
      final SortedMap<String, String> map = new TreeMap<>();
      for (int i = 0; i < nameValuePairs.length; i++) {
        map.put(nameValuePairs[i++].toString(), nameValuePairs[i].toString());
      }
      return new Qualifiers(map);
    }
  }

  private static final SortedMap<String, String> toMap(final SortedSet<? extends Qualifier> qs) {
    final SortedMap<String, String> map = new TreeMap<>();
    qs.forEach(q -> map.put(q.name(), q.value()));
    return unmodifiableSortedMap(map);
  }

}
