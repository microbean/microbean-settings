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

public record Context(Object object, Path path) {


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


  public final Type type() {
    return path().type();
  }

  public final Context with(final Object object) {
    return object == this.object() ? this : new Context(object, this.path());
  }


  /*
   * Static methods.
   */


  public static final Context of(final Path path) {
    return new Context(path);
  }


}
