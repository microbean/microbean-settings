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

import java.lang.reflect.Proxy;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import static org.microbean.settings.api.Handler.propertyName;

final class TestHandler {

  private TestHandler() {
    super();
  }

  @Test
  final void testWithEmptyApplicationQualifiers() {
    test(Map.of(), "Puce");
  }

  @Test
  final void testWithDevApplicationQualifiers() {
    test(Map.of("dev", Boolean.TRUE), "Red");
  }

  private static final void test(final Map<?, ?> applicationQualifiers, final String expectedColorName) {
    final Car defaultCar = new CarDefaults();
    final Handler<Car> h =
      new Handler<>(Car.class,
                    applicationQualifiers,
                    () -> defaultCar,
                    TestHandler::valueSuppliers,
                    Handler::propertyName);
    final Car car = h.get();
    assertNotSame(defaultCar, car);
    final Wheel wheel = car.getWheel();
    final Color color = wheel.getColor();
    assertEquals(expectedColorName, color.name());
  }

  @Test
  final void testPropertyName() {
    assertEquals("fooBah", propertyName("getFooBah", false));
    assertEquals("FOO", propertyName("getFOO", false));
    assertEquals("foo", propertyName("Foo", false));
    assertEquals("foo", propertyName("foo", false));
    assertEquals("FOO", propertyName("FOO", false));
    assertEquals("FOo", propertyName("FOo", false));
    assertEquals("foO", propertyName("FoO", false));
    assertEquals("foo", propertyName("isfoo", true));
    assertEquals("foo", propertyName("isFoo", true));
    assertEquals("foo", propertyName("getfoo", true));
    assertEquals("foo", propertyName("getFoo", true));
    assertEquals("toString", propertyName("toString", false));
  }

  private static final Collection<ValueSupplier> valueSuppliers(final Path path, final Map<?, ?> applicationQualifiers) {
    if (path.rootType().equals(Car.class) &&
        path.targetClass().equals(String.class) &&
        path.components().equals(List.of("wheel", "color", "name")) &&
        applicationQualifiers.equals(Map.of("dev", Boolean.TRUE))) {
      return List.of(new DevWheelColorString());
    } else {
      return List.of();
    }
  }
      
  private static final class DevWheelColorString implements ValueSupplier {

    private DevWheelColorString() {
      super();
    }
    
    @Override
    public final <T> Value<T> get(final Path path, final Map<?, ?> qualifiers) {
      if (path.rootType().equals(Car.class) &&
          path.targetClass().equals(String.class) &&
          path.components().equals(List.of("wheel", "color", "name")) &&
          qualifiers.equals(Map.of("dev", Boolean.TRUE))) {
        return new Value<>("Red", path, qualifiers).cast();
      } else {
        return null;
      }
    }

  }

  private static interface Car {

    public Wheel getWheel();

    public Color getColor();
    
  }

  private static interface Wheel {

    public Color getColor();
    
  }

  private static interface Color {

    public default String name() {
      return "Puce";
    }
    
  }

  // Last ditch fallback defaults, e.g. null, 0, false, etc.
  // Or users could throw exceptions to indicate missing data
  private static final class CarDefaults implements Car {

    @Override
    public Wheel getWheel() {
      return null;
    }

    @Override
    public Color getColor() {
      return null;
    }
    
  }
  
}