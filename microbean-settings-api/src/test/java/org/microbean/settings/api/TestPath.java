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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TestPath {

  private TestPath() {
    super();
  }

  @Test
  final void testRoots() {
    testRoot(Object.class);
    testRoot(Number.class);
    testRoot(String.class);
  }

  private static final void testRoot(final Type type) {
    final Path path = new Path(type);
    assertNull(path.parent());
    assertNotNull(path.name());
    assertTrue(path.name().isEmpty());
  }

  @Test
  final void testCannotHaveEmptyNameInNonRootPath() {
    assertThrows(IllegalArgumentException.class, () -> {
        new Path(new Path(), "", Object.class);
      });
  }

  @Test
  final void testMustHaveEmptyNameInRootPath() {
    assertThrows(IllegalArgumentException.class, () -> {
        new Path((Path)null, "foo", Object.class);
      });
  }

  @Test
  final void testNullTargetTypeProhibited() {
    assertThrows(NullPointerException.class, () -> {
        new Path(new Path(), "frob", null);
      });
  }

  @Test
  final void testOf() {
    Path path = Path.of(Number.class, "a", Object.class, "b", Number.class, "c", String.class);
    assertNotNull(path);
    assertEquals("c", path.name());
    assertSame(String.class, path.targetType());
  }

  @Test
  final void testEndsWith() {
    final Path path = Path.of(Number.class, "a", Object.class, "b", Number.class, "c", String.class);
    assertNotNull(path);
    assertTrue(path.endsWith(Object.class, List.of("b", "c")));
    final Path root = new Path();
    assertTrue(root.endsWith(Object.class, List.of()));
  }

  @Test
  final void testStartsWith() {
    final Path root = new Path();
    assertTrue(root.startsWith(Object.class));
    final Path path = Path.of(Number.class, "a", Object.class, "b", CharSequence.class, "c", String.class);
    assertTrue(path.startsWith(Number.class));
    assertEquals("c", path.name());
    final Path ab = path.leadingPath(Number.class, List.of("a", "b"));
    assertNotNull(ab);
    assertEquals("b", ab.name());
    assertSame(CharSequence.class, ab.targetType());
    assertEquals(ab, path.parent());
  }

  @Test
  final void testPathString() {
    final Path path = Path.of(Number.class, "a", Object.class, "b", Number.class, "c", String.class);
    assertEquals("a.b.c", path.pathString());
    final Path root = new Path();
    assertTrue(root.name().isEmpty());
    assertTrue(root.names().isEmpty());
    assertTrue(root.pathString().isEmpty());
  }

  @Test
  final void testLength() {
    final Path root = new Path();
    assertEquals(1, root.length());
    final Path child = new Path(root, "fred", CharSequence.class);
    assertEquals(2, child.length());
  }

  @Test
  final void testAll() {
    final Path path = Path.of(Number.class, "a", Object.class, "b", Number.class, "c", String.class);
    final List<Path> all = path.all();
    assertNotNull(all);
    assertEquals(4, all.size());
    final Path root = all.get(0);
    assertNull(root.parent());
    assertTrue(root.name().isEmpty());
    assertSame(Number.class, root.targetType());
    final Path a = all.get(1);
    assertSame(root, a.parent());
    assertEquals("a", a.name());
    assertSame(Object.class, a.targetType());
    final Path b = all.get(2);
    assertSame(a, b.parent());
    assertEquals("b", b.name());
    assertSame(Number.class, b.targetType());
    final Path c = all.get(3);
    assertSame(b, c.parent());
    assertEquals("c", c.name());
    assertSame(String.class, c.targetType());
  }

}
