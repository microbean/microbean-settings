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

import java.util.function.Function;

public interface Qualified<T> extends Assignable<T> {

  @Override // Assignable<T>
  public default T assignable() {
    return this.qualified();
  }

  @Override // Assignable<T>
  public default boolean isAssignable(final T payload) {
    return this.assignable().equals(payload);
  }
  
  public T qualified();

  public Qualifiers qualifiers();

  public static <T> String toString(final Qualified<T> q) {
    return toString(q, t -> t.toString());
  }
  
  public static <T> String toString(final Qualified<? extends T> q, final Function<? super T, ? extends String> f) {
    if (q == null) {
      return "";
    } else {
      final StringBuilder sb = new StringBuilder(String.valueOf(f.apply(q.qualified())));
      final Qualifiers qualifiers = q.qualifiers();
      if (!qualifiers.isEmpty()) {
        sb.append(":").append(qualifiers.toString());
      }
      return sb.toString();
    }
  }
  
}

