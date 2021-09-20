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

import java.lang.reflect.AnnotatedElement;

import java.util.Comparator;

import static java.util.Comparator.comparingInt;

// https://ux.stackexchange.com/questions/73445/is-a-higher-priority-a-smaller-number

// "Highest priority" wins; i.e. NOT "quality is job 1", NOT "DefCon 1". Sort descending.
// So the most important thing is Integer.MAX_VALUE; the least is Integer.MIN_VALUE.
public interface Prioritized {

  public default int priority() {
    return priority(this.getClass());
  }

  // TODO: warn about inconsistent with equals().
  public static final Comparator<Object> COMPARATOR_ASCENDING = comparingInt(Prioritized::priority);

  // TODO: warn about inconsistent with equals().
  public static final Comparator<Object> COMPARATOR_DESCENDING = COMPARATOR_ASCENDING.reversed();

  public static int priority(final Object o) {
    if (o instanceof Prioritized p) {
      return p.priority();
    } else if (o instanceof Priority p) {
      return p.value();
    } else if (o instanceof AnnotatedElement c) {
      return priority(c.getAnnotation(Priority.class));
    } else {
      return 0;
    }
  }

}
