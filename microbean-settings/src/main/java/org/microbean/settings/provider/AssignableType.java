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
package org.microbean.settings.provider;

import java.lang.reflect.Type;

import org.microbean.type.CovariantTypeSemantics;
import org.microbean.type.TypeSemantics;

public record AssignableType(Type type) implements Type {


  /*
   * Static fields.
   */


  private static final TypeSemantics covariantTypeSemantics = new CovariantTypeSemantics(false);


  /*
   * Ionstance methods.
   */


  @Override // Type
  public final String getTypeName() {
    return this.type().getTypeName();
  }

  public boolean isAssignable(final Type payload) {
    return covariantTypeSemantics.isAssignable(this.type(),
                                               payload instanceof AssignableType a ? a.type() : payload);
  }

  @Override // Object
  public final String toString() {
    return this.type().toString();
  }


  /*
   * Static methods.
   */


  public static final AssignableType of(final AssignableType type) {
    return type;
  }
  
  public static final AssignableType of(final Type type) {
    return type instanceof AssignableType a ? a : new AssignableType(type);
  }

}