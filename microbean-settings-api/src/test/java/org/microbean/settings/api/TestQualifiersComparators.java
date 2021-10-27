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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TestQualifiersComparators {

  private TestQualifiersComparators() {
    super();
  }

  @Test
  final void testSpecificityComparatorSizeBasedOrderingAscending() {
    final Qualifiers q0 = Qualifiers.of("a", "b", "c", "d");
    final Qualifiers q1 = Qualifiers.of();
    final List<Qualifiers> list = arrayListOf(q0, q1);
    Collections.sort(list, Qualifiers.SpecificityComparator.INSTANCE);
    assertSame(q1, list.get(0));
    assertSame(q0, list.get(1));
  }

  @Test
  final void testScorer() {
    final Qualifiers a = Qualifiers.of("a", "x");
    final Scorer s = new Scorer(a);

    final Qualifiers cd = Qualifiers.of("c", "x");
    assertEquals(0, s.intersectionSize(cd));
    assertEquals(2, s.symmetricDifferenceSize(cd));
    assertEquals(-2, s.score(cd));

    final Qualifiers ab = Qualifiers.of("a", "x",
                                        "b", "x");
    assertEquals(1, s.intersectionSize(ab));
    assertEquals(1, s.symmetricDifferenceSize(ab));
    assertEquals(0, s.score(ab));

    assertEquals(1, s.intersectionSize(a));
    assertEquals(0, s.symmetricDifferenceSize(a));
    assertEquals(1, s.score(a));

    assertEquals(-a.size(), s.score(null));
        
  }

  @SafeVarargs
  private static final <T> ArrayList<T> arrayListOf(final T... stuff) {
    final ArrayList<T> list = new ArrayList<>(stuff.length);
    for (final T element : stuff) {
      list.add(element);
    }
    return list;
  }

  public static final class Scorer {

    private final Qualifiers q0;
    
    public Scorer(final Qualifiers q0) {
      super();
      this.q0 = Objects.requireNonNull(q0, "q0");
    }

    public final int score(final Qualifiers q1) {
      if (valid(q1)) {
        final int intersectionSize = this.intersectionSize(q1);
        if (intersectionSize > 0) {
          if (intersectionSize == q1.size()) {
            assert this.q0.equals(q1);
            return intersectionSize;
          } else {
            return intersectionSize - this.symmetricDifferenceSize(q1);
          }
        } else {
          return -(this.q0.size() + q1.size());
        }
      } else {
        return -this.q0.size();
      }
    }

    private static final boolean valid(final Qualifiers q1) {
      return q1 != null;
    }
    
    public final int intersectionSize(final Qualifiers q1) {
      if (!valid(q1) || q1.isEmpty()) {
        return 0;
      } else if (this.q0 == q1) {
        // Just an identity check to rule this easy case out.
        return this.q0.size();
      } else {
        final Set<? extends Entry<String, ?>> q1EntrySet = q1.entrySet();
        return (int)this.q0.entrySet().stream()
          .filter(q1EntrySet::contains)
          .count();
      }
    }

    public final int symmetricDifferenceSize(final Qualifiers q1) {
      if (!valid(q1) || q1.isEmpty()) {
        return this.q0.size();
      } else if (this.q0 == q1) {
        // Just an identity check to rule this easy case out.
        return 0;
      } else {
        final Set<Entry<?, ?>> q1SymmetricDifference = new HashSet<>(this.q0.entrySet());
        q1.entrySet().stream()
          .filter(Predicate.not(q1SymmetricDifference::add))
          .forEach(q1SymmetricDifference::remove);
        return q1SymmetricDifference.size();
      }
    }
    
  }
  
}
