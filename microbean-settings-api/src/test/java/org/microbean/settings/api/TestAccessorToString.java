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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.microbean.settings.api.Accessor.root;

final class TestAccessorToString {

  private TestAccessorToString() {
    super();
  }

  @Test
  final void testAccessorFragmentToString() {
    // No leading slash is an accessor fragment.
    assertEquals("java.lang.Number", Accessor.of(Number.class).toString());
  }

  @Test
  final void testRootToString() {
    // Single slash is root.
    assertEquals("/", root().toString());
  }

  @Test
  final void testAccessorToString() {
    // Double leading slash is one level down from root.
    assertEquals("//java.lang.Number", Accessor.of(root(), Number.class).toString());
  }

}
