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

import java.util.List;
import java.util.StringJoiner;

final record Path2(Path2 parent, List<String> components, Type targetType) {

  static final Path2 ROOT = new Path2((Path2)null, List.of(), Object.class);

  Path2() {
    this(ROOT, List.of(), Object.class);
  }
  
  Path2(final List<String> components) {
    this(ROOT, components, Object.class);
  }
  
  Path2(final Type targetType) {
    this(ROOT, List.of(), targetType);
  }

  Path2(final Path2 parent, final List<String> components) {
    this(parent, components, Object.class);
  }

  Path2(final Path2 parent, final Type targetType) {
    this(parent, List.of(), targetType);
  }
  
  Path2(final List<String> components, final Type targetType) {
    this(ROOT, components, targetType);
  }

  Path2(final Type rootType, final List<String> components, final Type targetType) {
    this(new Path2(ROOT, List.of(), rootType), components, targetType);
  }

  Path2 {
    Types.rawClass(targetType);
    if (components == null || components.isEmpty()) {
      if (parent == null) {
        if (targetType == Object.class) {
          // We're building ROOT; this is OK
        } else {
          throw new IllegalArgumentException("parent: " + parent + "; targetType: " + targetType + "; components: " + components);
        }
      } else if (root(parent)) {
        // Parent is ROOT. Empty components.
        if (targetType == Object.class) {
          // We ARE ROOT, effectively.
          parent = null;
        }
      } else if (targetType.equals(parent.targetType())) {
        // Non-root parent. Empty components. Same target type. We ARE our parent.
        components = parent.components();
        parent = parent.parent();
      } else {
        // Non-root parent. Empty components. Different target type. Bad.
        throw new IllegalArgumentException("targetType: " + targetType);
      }
    } else if (parent == null) {
      throw new IllegalArgumentException("parent: " + parent);
    }
    components = List.copyOf(components);
  }

  public final boolean root() {
    return root(this);
  }

  public final boolean typeRoot() {
    return root(this.parent()) && this.components.isEmpty();
  }

  public final Class<?> targetClass() {
    return Types.rawClass(this.targetType());
  }
  
  public final String componentsString() {
    final List<String> components = this.components();
    if (components.isEmpty()) {
      return "";
    } else {
      final StringJoiner sj = new StringJoiner(".");
      components.forEach(c -> sj.add(c));
      return sj.toString();
    }
  }

  public final String absoluteComponentsString() {
    final Path2 parent = this.parent();
    String s;
    if (parent == null) {
      s = this.componentsString();
      return s == null ? "" : s;
    } else {
      final StringJoiner sj = new StringJoiner(".");
      s = parent.absoluteComponentsString();
      if (s != null && !s.isEmpty()) {
        sj.add(s);
      }
      s = this.componentsString();
      if (s != null && !s.isEmpty()) {
        sj.add(s);
      }
      return sj.toString();
    }
  }

  private static final boolean root(final Path2 path) {
    return path.parent() == null && path.components().isEmpty() && path.targetType() == Object.class;
  }
  
}
