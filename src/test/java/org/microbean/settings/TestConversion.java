/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2020 microBean™.
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

import org.junit.Before;
import org.junit.Test;

import org.microbean.settings.converter.StringConverter;

import static org.junit.Assert.assertTrue;

public class TestConversion {

  private Converters converters;
  
  public TestConversion() {
    super();
  }

  @Before
  public void setUp() {
    this.converters = new Converters();
  }

  @Test
  public void testGenerics() {
    final Converter<? extends CharSequence> c = converters.getConverter(CharSequence.class);
    assertTrue(c instanceof StringConverter);
  }

}
