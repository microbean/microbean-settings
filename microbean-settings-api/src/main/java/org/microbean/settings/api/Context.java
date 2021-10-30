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

import java.util.Objects;

import java.util.function.Supplier;

import org.microbean.development.annotation.Convenience;

// TODO: as written this now has a reference to SupplierBroker, and
// SupplierBroker has a reference to this.  Java lets this happen just
// fine, but I'm not thrilled about it.  Working on making what is
// currently Settings be, effectively, this class.  Remember that a
// Path always starts with Accessors, not a Type.
public record Context<T>(SupplierBroker broker, Qualifiers qualifiers, T object, Path path) {


  /*
   * Constructors.
   */


  public Context {
    Objects.requireNonNull(qualifiers, "qualifiers");
    Objects.requireNonNull(path, "path");
  }


  /*
   * Instance methods.
   */


  @Convenience
  public final Type type() {
    return this.path().type();
  }
  
  @SuppressWarnings("unchecked")
  public final <U extends T> Context<U> with(final U object) {
    return object == this.object() ? (Context<U>)this : new Context<>(this.broker(), this.qualifiers(), object, this.path());
  }


  /*
   * Static methods.
   */


}
