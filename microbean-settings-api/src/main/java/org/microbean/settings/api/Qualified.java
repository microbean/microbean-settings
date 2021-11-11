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

import java.util.Objects;

import java.util.function.Function;
import java.util.function.BiPredicate;

public interface Qualified<T> {


  /*
   * Instance methods.
   */


  public T qualified();

  public Qualifiers qualifiers();


  /*
   * Inner and nested classes.
   */


  public record Record<T>(Qualifiers qualifiers, T qualified) implements Qualified<T> {

    
    /*
     * Constructors.
     */


    public Record {
      Objects.requireNonNull(qualifiers, "qualifiers");
      Objects.requireNonNull(qualified, "qualified");
    }


    /*
     * Static methods.
     */


    public static final <T> Record<T> of(final Qualified<T> q) {
      return q instanceof Record<T> qr ? qr : new Record<>(q.qualifiers(), q.qualified());
    }

    public static final <T> Record<T> of(final T qualified) {
      return of(Qualifiers.of(), qualified);
    }

    public static final <T> Record<T> of(final Qualifiers qualifiers, final T qualified) {
      return new Record<>(qualifiers, qualified);
    }

  }

}
