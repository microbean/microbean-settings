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
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.TreeSet;

import java.util.stream.Stream;

import static java.util.Collections.emptySortedMap;
import static java.util.Collections.emptySortedSet;
import static java.util.Collections.unmodifiableSortedMap;
import static java.util.Collections.unmodifiableSortedSet;

public record Qualifiers(SortedMap<String, ?> qualifiers) implements Assignable<Qualifiers> {

  public Qualifiers(final Qualifier<?> qualifier) {
    this(new TreeMap<>(Map.of(qualifier.name(), qualifier.value())));
  }
  
  public Qualifiers(final SortedSet<Qualifier<?>> qualifiers) {
    this(toMap(qualifiers));
  }
  
  public Qualifiers {
    qualifiers = qualifiers == null || qualifiers.isEmpty() ? emptySortedMap() : unmodifiableSortedMap(new TreeMap<>(qualifiers));
  }

  @Override // Assignable<Qualifiers>
  public final Qualifiers assignable() {
    return this;
  }
  
  @Override // Assignable<Qualifiers>
  public final boolean isAssignable(final Qualifiers payload) {
    // Demand is for "red car".  Payload is, say, "red racing car".
    // It matches, for some intuitive level of matching.
    //
    // But suppose the qualifier is "env=test", so "env=test car" is
    // the demand.  Should "car" be assignable?  You *could* argue
    // yes: this match would occur only if there were no more specific
    // match.  It represents the "fallback" match: I couldn't give you
    // a "env=test car" but I can give you this generic "car" as a
    // fallback.  Note the *lack* of the "env" key.  Note also this is
    // still subset semantics: the empty set (in "car") is a subset of
    // "env=test".
    //
    // If demand is for "env=test car" and I have "env=production
    // car", that shouldn't match, ever.  And with subset semantics
    // this wouldn't.
    //
    // I *think* subset semantics are still the way to go.  Later on,
    // during resolution, we will definitely apply different rules.
    // Specifically, if there is a provider with "env=test car" and
    // one with "env=test,color=red car" and the demand was, exactly,
    // "env=test car", then the first provider would match and the
    // second one would be jettisoned.
    //
    // TODO: this is the heart of the matter, isn't it?
    //
    // For now, implement CDI rules.
    return this.isSubsetOf(payload);
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
    if (this.size() < her.size()) {
      return false;
    } else {
      return this.entrySet().containsAll(her.entrySet());
    }
  }

  public final Object get(final String name) {
    return this.qualifiers.get(name);
  }

  public Stream<Qualifier<?>> stream() {
    return this.qualifiers().entrySet().stream().map(Qualifier::of);
  }

  public final boolean isSubsetOf(final Qualifiers other) {
    if (other.isEmpty()) {
      return this.isEmpty();
    } else if (this.size() <= other.size()) {
      return other.contains(this);
    } else {
      return false;
    }
  }
  
  public final String toString() {
    final StringJoiner sj = new StringJoiner(";");
    this.stream().map(Qualifier::toString).forEach(sj::add);
    return sj.toString();
  }

  public final SortedSet<Qualifier<?>> toQualifiers() {
    final SortedMap<? extends String, ?> q = this.qualifiers();
    return q.isEmpty() ? emptySortedSet() : unmodifiableSortedSet(new TreeSet<>(q.entrySet().stream().map(Qualifier::of).toList()));
  }

  private static final SortedMap<String, ?> toMap(final SortedSet<? extends Qualifier<?>> qs) {
    final SortedMap<String, Object> map = new TreeMap<>();
    qs.forEach(q -> map.put(q.name(), q.value()));
    return unmodifiableSortedMap(map);
  }

  public static final Qualifiers of() {
    return new Qualifiers(emptySortedMap());
  }
  
  public static final <T> Qualifiers of(final SortedMap<? extends CharSequence, T> map) {
    final SortedMap<String, T> newMap = new TreeMap<>();
    map.entrySet().forEach(e -> newMap.put(e.getKey().toString(), e.getValue()));
    return new Qualifiers(newMap);
  }

  public static final <T> Qualifiers of(final SortedSet<? extends Qualifier<T>> set) {
    return of(toMap(set));
  }

  public static final Qualifiers of(final String name0, final Object value0) {
    return new Qualifiers(Qualifier.of(name0, value0));
  }

  public static final Qualifiers of(final String name0, final Object value0,
                                    final String name1, final Object value1)
  {
    final SortedMap<String, Object> map = new TreeMap<>();
    map.put(name0, value0);
    map.put(name1, value1);
    return new Qualifiers(map);
  }

  public static final Qualifiers of(final String... nameValuePairs) {
    if (nameValuePairs == null || nameValuePairs.length <= 0) {
      return Qualifiers.of();
    } else if (nameValuePairs.length % 2 != 0) {
      throw new IllegalArgumentException("nameValuePairs: " + Arrays.toString(nameValuePairs));
    } else {
      final SortedMap<String, Object> map = new TreeMap<>();
      for (int i = 0; i < nameValuePairs.length; i++) {        
        map.put(nameValuePairs[i++], nameValuePairs[i]);
      }
      return new Qualifiers(map);
    }
  }
  
}
