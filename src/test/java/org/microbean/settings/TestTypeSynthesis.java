/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2019 microBean™.
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
package org.microbean.settings;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.enterprise.util.TypeLiteral;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestTypeSynthesis {

  public TestTypeSynthesis() {
    super();
  }

  @Test
  public void testGenerics() {
    final List<? extends List<? extends String>> list = new ArrayList<List<String>>();
    // Worth remembering that this one doesn't compile:
    // final List<          List<? extends String>> list2 = new ArrayList<List<String>>();
    final List<? extends List<          String>> list3 = new ArrayList<List<String>>();
  }

  @Test
  public void testWildcardTypeSynthesis() {
    // Jump through a bunch of hoops to get our hands on a simple,
    // arbitrary wildcard type.
    final TypeLiteral<?> tl = new TypeLiteral<List<? extends String>>() {
        private static final long serialVersionUID = 1L;
      };
    final ParameterizedType ptype = (ParameterizedType)tl.getType();
    final Type[] actualTypeArguments = ptype.getActualTypeArguments();
    assertNotNull(actualTypeArguments);
    assertEquals(1, actualTypeArguments.length);
    final Type requiredType = actualTypeArguments[0];
    assertTrue(requiredType instanceof WildcardType);
    assertEquals("? extends java.lang.String", requiredType.getTypeName());

    // Now pass that wildcard type to the
    // SettingsExtensions.synthesizeLegalBeanType() method and make
    // sure that since it is a single-bound wildcard type it can be
    // synthesized.
    final Type type = SettingsExtension.synthesizeLegalBeanType(requiredType, 1);
    assertEquals(String.class, type);
  }

  @Test
  public void testSimpleClassSynthesis() {
    final Type type = SettingsExtension.synthesizeLegalBeanType(String.class, 1);
    assertEquals(String.class, type);
  }

  @Test
  public <T> void testTypeSynthesis2() {
    final TypeLiteral<T> tl = new TypeLiteral<T>() {
        private static final long serialVersionUID = 1L;
      };
    final Type type = SettingsExtension.synthesizeLegalBeanType(tl.getType(), 1);
    assertEquals(Object.class, type);
  }

  interface Bork {

  }

}
