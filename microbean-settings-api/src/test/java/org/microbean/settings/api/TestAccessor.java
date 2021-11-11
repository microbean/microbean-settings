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

final class TestAccessor {

  private static final StackWalker stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

  private TestAccessor() {
    super();
  }

  @Test
  final void testParseRoot() throws ClassNotFoundException {
    assertSame(Accessor.root(), new Accessor.Parser(this.getClass().getClassLoader()).parse(":void"));
  }

  @Test
  final void testParseEmpty() throws ClassNotFoundException {
    assertSame(Accessor.of(), new Accessor.Parser(this.getClass().getClassLoader()).parse(""));
  }

  @Test
  final void testParseFoo() throws ClassNotFoundException {
    assertEquals(Accessor.of("foo"), new Accessor.Parser(this.getClass().getClassLoader()).parse("foo"));
  }

  @Test
  final void testParseFooStringNoParamsNoArgs() throws ClassNotFoundException {
    assertEquals(Accessor.of("foo", String.class),
                 new Accessor.Parser(this.getClass().getClassLoader()).parse("foo:java.lang.String"));
  }

  @Test
  final void testParseFooStringEmptyParamsEmptyArgs() throws ClassNotFoundException {
    assertEquals(Accessor.of("foo", String.class, List.of(), List.of()),
                 new Accessor.Parser(this.getClass().getClassLoader()).parse("foo():java.lang.String"));
  }

  @Test
  final void testParseFooStringOneParamNoArgs() throws ClassNotFoundException {
    assertEquals(Accessor.of("foo", String.class, List.of(String.class), null),
                 new Accessor.Parser(this.getClass().getClassLoader()).parse("foo(java.lang.String):java.lang.String"));
  }

  @Test
  final void testParseFooStringTwoParamsTwoArgs() throws ClassNotFoundException {
    assertEquals(Accessor.of("foo", String.class, List.of(String.class, Integer.class), List.of("goop", "4")),
                 new Accessor.Parser(this.getClass().getClassLoader()).parse("foo(java.lang.String=goop,java.lang.Integer=4):java.lang.String"));
  }

  @Test
  final void testParseFooStringBadComma() throws ClassNotFoundException {
    assertThrows(IllegalArgumentException.class,
                 () -> new Accessor.Parser(this.getClass().getClassLoader()).parse("foo(,java.lang.Integer=4):java.lang.String"));
  }

}
