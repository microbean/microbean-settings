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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TestPath {

  private TestPath() {
    super();
  }

  @Test
  final void testAdd() {
    final Path pf = Path.of(Car.class);
    assertSame(Car.class, pf.type());
    assertEquals(1, pf.size());
  }

  @Test
  final void testEndsWith() {
    final Path path = Path.of().plus(Car.class).plus("shape", Shape.class);
    final Path other = Path.of("shape", Square.class);
    assertTrue(path.endsWith(other, (o1, o2) -> {
          if (o1 instanceof Accessor) {
            return o1.equals(o2);
          } else if (o1 instanceof Type t1 && o2 instanceof Type t2) {
            return AssignableType.of(t1).isAssignable(t2);
          } else {
            return false;
          }
        }));
  }

  private static interface Car {

  }

  private static interface Shape {

  }

  private static interface Square extends Shape {

  }

}
