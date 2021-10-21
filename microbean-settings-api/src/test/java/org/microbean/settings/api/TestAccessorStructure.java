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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.microbean.settings.api.Accessor.root;

final class TestAccessorStructure {

  private TestAccessorStructure() {
    super();
  }

  @Test
  final void testAccessorFragmentStructure() {
    assertFalse(Accessor.of(Number.class).isAbsolute());
  }

  @Test
  final void testRootStructure() {
    assertTrue(root().isAbsolute());
  }

  @Test
  final void testAbsoluteAccessorStructure() {
    assertTrue(Accessor.of(root(), Number.class).isAbsolute());
  }

  @Test
  final void testRootHead() {
    final Accessor root = root();
    assertSame(root, root.head());
  }

  @Test
  final void testFragmentHead() {
    final Accessor fragment = Accessor.of(Number.class);
    assertSame(fragment, fragment.head());
  }

  @Test
  final void testPlusEquality() {
    assertEquals(root().plus(Number.class), new Accessor(root(), Number.class));
  }

  @Test
  final void testPlus() {
    final Accessor a4 = root().plus(Number.class).plus(Integer.class).plus(CharSequence.class);
    assertTrue(a4.isAbsolute());
    assertEquals(root(), a4.head());
    assertNotEquals(a4, a4.head());
  }

  @Test
  final void testUpstream() {
    final Accessor a4 = root().plus(Number.class).plus(Integer.class).plus(CharSequence.class);
    final int[] i = new int[] { 0 };
    a4.upstream().forEach(a -> i[0]++);
    assertEquals(4, i[0]);
    assertEquals(4, a4.length());
  }

  @Test
  final void testToList() {
    final List<Qualified<?>> list = root().plus(Number.class).plus(Integer.class).plus(CharSequence.class).toList();
    assertEquals(6, list.size(), list.toString());
    assertEquals("", list.get(0).qualified());
    assertSame(Number.class, list.get(1).qualified());
    assertEquals("", list.get(2).qualified());
    assertSame(Integer.class, list.get(3).qualified());
    assertEquals("", list.get(4).qualified());
    assertSame(CharSequence.class, list.get(5).qualified());
  }
}
