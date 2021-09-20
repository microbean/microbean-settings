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

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.microbean.settings.api.Path2.ROOT;

final class TestPath {

  private TestPath() {
    super();
  }

  @Test
  final void testRoot() {
    final Path2 path = ROOT;
    assertNull(path.parent());
    assertNotNull(path.components());
    assertTrue(path.components().isEmpty());
  }

  @Test
  final void testNoNullParentExceptForRoot() {
    assertThrows(IllegalArgumentException.class, () -> {
        new Path2((Path2)null, List.of("foo", "bar"), Object.class);
      });
    final Path2 root = new Path2((Path2)null, List.of(), Object.class);
    assertEquals(ROOT, root);
  }

  @Test
  final void testEmpty() {
    Path2 path = new Path2(List.of());
    assertEquals(ROOT, path);
    assertTrue(path.root());
    path = new Path2();
    assertEquals(ROOT, path);
    assertTrue(path.root());
  }

  @Test
  final void testGoodTypeRoot() {
    final Path2 path = new Path2(ROOT, List.of(), Number.class);
    assertSame(ROOT, path.parent());
    assertNotNull(path.components());
    assertTrue(path.components().isEmpty());
    assertSame(Number.class, path.targetType());
    assertTrue(path.typeRoot());
  }

  @Test
  final void testBadEmptyComponents() {
    final Path2 path = new Path2(Number.class);
    assertSame(ROOT, path.parent());
    assertNotNull(path.components());
    assertTrue(path.components().isEmpty());
    assertSame(Number.class, path.targetType());
    assertTrue(path.typeRoot());
    assertThrows(IllegalArgumentException.class, () -> {
        // Can't change target type
        new Path2(path, List.of(), CharSequence.class);
      });
  }

  @Test
  final void testRealPath() {
    final Path2 path = new Path2(List.of("a", "b"));
    assertSame(ROOT, path.parent());
    assertSame(Object.class, path.targetType());
    assertFalse(path.typeRoot());
    assertFalse(path.root());
    final Path2 child = new Path2(path, List.of("c"));
    assertSame(path, child.parent());
    assertFalse(child.typeRoot());
    assertFalse(child.root());
    assertEquals("c", child.componentsString());
    assertEquals("a.b.c", child.absoluteComponentsString());
  }
  
}
