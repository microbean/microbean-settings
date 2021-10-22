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

import static org.microbean.settings.api.Path.root;

public record Context(Path parentPath, // the path so far
                      Object object, // the "parent" object if known
                      Qualified<Type> target) { // the type of thing being requested with its qualifiers

  public Context(final Qualifiers rootQualifiers,
                 final Qualified<Type> target) {
    this(root(rootQualifiers),
         null,
         target);
  }

  public Context(final Path parentPath,
                 final Qualified<Type> target) {
    this(parentPath, null, target);
  }
  
  public Context {
    Objects.requireNonNull(parentPath, "parentPath");
    if (object != null) {
      Objects.requireNonNull(target, "target");
      if (!AssignableType.of(parentPath.target().qualified()).isAssignable(object.getClass())) {
        throw new IllegalArgumentException("object is of the wrong type for " + parentPath + ": " + object + " (" + object.getClass().getName() + ")");
      }
    }
  }  

  public final Path path() {
    return this.parentPath().plus(target);
  }

  public final AssignableType type() {
    return AssignableType.of(this.target().qualified());
  }

  public static final Context of(final Qualifiers rootQualifiers, final Qualified<Type> target) {
    return new Context(rootQualifiers, target);
  }
  
  
}
