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
package org.microbean.settings;

import java.util.Collection;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import org.microbean.settings.api.Configured;

import org.microbean.settings.provider.Provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

final class TestSystemPropertiesNonDeterminism {

  private TestSystemPropertiesNonDeterminism() {
    super();
  }

  @Test
  final void testJavaHome() {
    final String old = System.getProperty("java.home");
    assertNotNull(old);
    final Properties systemProperties = System.getProperties();
    assertNotNull(systemProperties);
    assertEquals(old, systemProperties.getProperty("java.home"));
    assertEquals(old, systemProperties.get("java.home"));
    try {
      // Yes, you can do this sort of thing with no problems at all.
      systemProperties.put("java.home", Integer.valueOf(7));
      assertEquals(Integer.valueOf(7), systemProperties.get("java.home"));
      assertNull(System.getProperty("java.home")); // !!!
    } finally {
      final Object x = systemProperties.put("java.home", old);
      assertEquals(Integer.valueOf(7), x);
    }
    assertSame(systemProperties, System.getProperties());
    assertEquals(old, System.getProperty("java.home"));
    assertEquals(old, System.getProperties().get("java.home"));
  }

}
