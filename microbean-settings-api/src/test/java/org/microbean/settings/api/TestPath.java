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

import java.lang.StackWalker.StackFrame;

import java.lang.reflect.Type;

import java.net.URI;
import java.net.URISyntaxException;

import java.security.CodeSource;

import java.util.List;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TestPath {

  private static final StackWalker stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

  private TestPath() {
    super();
  }

  @Test
  final void testParseRoot() throws ClassNotFoundException {
    assertSame(Path.root(), new Path.Parser(this.getClass().getClassLoader()).parse(":void"));
  }

  @Test
  final void testParseEmpty() throws ClassNotFoundException {
    assertThrows(IllegalArgumentException.class,
                 () -> new Path.Parser(this.getClass().getClassLoader()).parse(""));
  }

  @Test
  final void testParseFoo() throws ClassNotFoundException {
    assertEquals(Path.of(Accessor.of("foo")), new Path.Parser(this.getClass().getClassLoader()).parse("foo"));
  }

  @Test
  final void testPathParse() throws ClassNotFoundException {
    assertEquals(Path.of(List.of(Accessor.of(),
                                 Accessor.of("foo", String.class, List.of(String.class, Integer.class), List.of("goop", "4")))),
                 new Path.Parser(this.getClass().getClassLoader()).parse("/foo(java.lang.String=goop,java.lang.Integer=4):java.lang.String"));
  }

  @Test
  final void testPathParseWithEscapedSlash() throws ClassNotFoundException {
    assertEquals(Path.of(List.of(Accessor.of(),
                                 Accessor.of("/foo", String.class, List.of(String.class, Integer.class), List.of("goop", "4")))),
                 new Path.Parser(this.getClass().getClassLoader()).parse("/\\/foo(java.lang.String=goop,java.lang.Integer=4):java.lang.String"));
  }


}
