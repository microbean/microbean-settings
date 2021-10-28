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

public record Context<T>(T object, Path path) implements Assignable<Type>, Supplier<T> {


  /*
   * Constructors.
   */


  public Context(final Path path) {
    this(null, path);
  }

  public Context {
    Objects.requireNonNull(path, "path");
  }


  /*
   * Instance methods.
   */


  @Override // Assignable<Type>
  public final Type assignable() {
    return this.type();
  }

  @Override // Assignable<Type>
  public final boolean isAssignable(final Type type) {
    return AssignableType.of(this.assignable()).isAssignable(type);
  }

  @Override // Supplier<T>
  public final T get() {
    return this.object();
  }
  
  public final Type type() {
    return this.path().type();
  }

  @SuppressWarnings("unchecked")
  public final <U extends T> Context<U> with(final U object) {
    return object == this.object() ? (Context<U>)this : new Context<>(object, this.path());
  }


  /*
   * Static methods.
   */


  public static final Context<?> of(final Path path) {
    return new Context<>(path);
  }


}
