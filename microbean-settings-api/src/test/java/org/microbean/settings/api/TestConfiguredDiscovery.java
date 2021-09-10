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

import java.net.URL;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class TestConfiguredDiscovery {

  TestConfiguredDiscovery() {
    super();
  }

  @Test
  final void testConfiguredDiscovery() {
    final Bar bar = Configured.get(Bar.class);
    assertNotNull(bar);
    System.out.println(bar.getJavaHome());
  }

  public static final class Foo extends Configured<Bar, SystemPropertiesCoordinates> {

    @Override
    public final Bar get(final SystemPropertiesCoordinates coordinates) {
      return new Bar() {
        @Override
        public final String getJavaHome() {
          return coordinates.get("java.home", String.class);
        }
      };
    }    

  }

  public static interface Bar {

    public String getJavaHome();
    
  }

}
