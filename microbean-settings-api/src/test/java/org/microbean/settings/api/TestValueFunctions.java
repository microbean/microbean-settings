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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import java.net.URL;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import java.util.stream.Stream;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class TestValueFunctions {

  TestValueFunctions() {
    super();
  }

  @Test
  final void testValueSupplier() {
    final Configuration lastResort = new Configuration() {

        private int age;
        
        @Override
        public final String getName() {
          return null;
        }

        @Override
        public final void frobnicate() {

        }

        @Override
        public final int getAge() {
          return this.age;
        }

        @Override
        public final void setAge(final int age) {
          this.age = age;
        }
        
      };
    final ConfiguredInfo<Configuration> ci =
      new ConfiguredInfo<>(Map.of("getAge", () -> 42,
                                  "getName", () -> "Fred"));
    final Configuration c;
    final Supplier<Configuration> s = ci.valueSupplier();
    if (s == null) {
      c =
        (Configuration)Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                                              new Class<?>[] { Configuration.class },
                                              new InvocationHandler<Configuration>(List.of(ci), () -> lastResort));
    } else {
      c = s.get();
    }
    assertEquals(c, c);
    assertEquals(42, c.getAge());
    assertEquals(System.identityHashCode(c), c.hashCode());
  }

  public static interface Configuration {

    public String getName();

    public void frobnicate();

    public int getAge();

    public void setAge(final int age);
    
  }

}
