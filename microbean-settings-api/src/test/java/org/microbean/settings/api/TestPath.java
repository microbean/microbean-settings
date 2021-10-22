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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.microbean.settings.api.Path.absoluteOf;
import static org.microbean.settings.api.Path.fragmentOf;
import static org.microbean.settings.api.Path.root;

final class TestPath {

  private TestPath() {
    super();
  }

  @Test
  final void testRootPathToString() {
    assertEquals("/", root().toString());
    assertEquals("/:env=test", root(Qualifiers.of("env", "test")).toString());
  }
  
  @Test
  final void testAbsolutePathToString() {
    assertEquals("//java.lang.Number", root().plus(QualifiedRecord.of(Number.class)).toString());
  }

  @Test
  final void testPathFragmentToString() {
    assertEquals("java.lang.Number", fragmentOf(Number.class).toString());
  }

  @Test
  final void testOneRoot() {
    assertSame(root(), root());
    assertTrue(root().isAbsolute());
    assertEquals(1, root().size());
    assertNotNull(root().target());
  }

  @Test
  final void testSplitEdgeCases() {
    List<Path> p = Path.root().split(0);
    assertEquals(1, p.size());
    assertSame(Path.root(), p.get(0));
    p = Path.root().split(45);
    assertEquals(1, p.size());
    assertSame(Path.root(), p.get(0));
    p = Path.root().split(-12);
    assertEquals(1, p.size());
    assertSame(Path.root(), p.get(0));
  }

}
