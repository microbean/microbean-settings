/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2020 microBean™.
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
package org.microbean.settings;

import java.io.Serializable;

import java.lang.annotation.Annotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class SourceOrderArbiter extends Arbiter {

  private static final long serialVersionUID = 1L;

  public SourceOrderArbiter() {
    super();
  }

  @Override
  public Value arbitrate(final Set<? extends Source> sources,
                         final String name,
                         final Set<? extends Annotation> qualifiers,
                         final Collection<? extends Value> values) {
    Objects.requireNonNull(sources);
    Objects.requireNonNull(name);
    final Value returnValue;
    if (values == null || values.isEmpty()) {
      returnValue = null;
    } else if (values.size() == 1) {
      if (values instanceof List) {
        returnValue = ((List<? extends Value>)values).get(0);
      } else {
        returnValue = values.iterator().next();
      }
    } else {
      returnValue = Collections.min(values, new SourceOrderComparator(sources));
    }
    return returnValue;
  }

  private static final class SourceOrderComparator implements Comparator<Value>, Serializable {

    private static final long serialVersionUID = 1L;

    private final List<? extends Source> list;
    
    private SourceOrderComparator(final Set<? extends Source> sources) {
      super();
      this.list = new ArrayList<>(Objects.requireNonNull(sources));
    }

    @Override
    public final int compare(final Value first, final Value second) {
      if (first == null) {
        throw new IllegalArgumentException("first == null");
      } else if (second == null) {
        throw new IllegalArgumentException("second == null");
      }
      final Source firstSource = first.getSource();
      if (firstSource == null) {
        throw new IllegalArgumentException("first.getSource() == null: " + first);
      }
      final Source secondSource = second.getSource();
      if (secondSource == null) {
        throw new IllegalArgumentException("second.getSource() == null: " + second);
      }
      final int firstIndex = this.list.indexOf(firstSource);
      if (firstIndex < 0) {
        throw new IllegalArgumentException("!(this.list.contains(first.getSource()))");
      }
      final int secondIndex = this.list.indexOf(secondSource);
      if (secondIndex < 0) {
        throw new IllegalArgumentException("!(this.list.contains(second.getSource()))");
      }
      if (firstIndex == secondIndex) {
        throw new IllegalArgumentException("Arbitration was invoked on two Values from the same Source");
      } else if (firstIndex < secondIndex) {
        return -1;
      } else {
        return 1;
      }
    }
    
  }
  
  
}
