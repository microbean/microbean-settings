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

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.microbean.settings.api.Path.Element;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TestProxyingProvider {

  private TestProxyingProvider() {
    super();
  }

  @Test
  final void explore() {
    final ConfiguredSupplier<Car> carCs = ConfiguredSupplier.of().plus(Car.class);
    final Car car = carCs.get();
    assertNotNull(car);
    assertSame(car, carCs.get());
    final Powertrain pt = car.getPowertrain();
    assertNotNull(pt);
    assertSame(pt, car.getPowertrain());
    final Engine engine = pt.getEngine();
    assertNotNull(engine);
    assertSame(engine, pt.getEngine());
    engine.start();
    assertEquals(18, car.getWheel("LF").getDiameterInInches());
    assertEquals(24, car.getWheel("LR").getDiameterInInches());
  }

  public static interface Car {

    public Powertrain getPowertrain();

    public Wheel getWheel(final String wheelSpecifier);

  }

  public static interface Powertrain {

    public Engine getEngine();

  }

  public static interface Engine {

    public default void start() {

    }

  }

  public static interface Wheel {

    public default int getDiameterInInches() {
      return 18;
    }

  }

  public static final class LRWheelProvider extends AbstractProvider<Wheel> {

    public LRWheelProvider() {
      super();
    }

    @Override
    public final boolean isSelectable(final ConfiguredSupplier<?> supplier,
                                      final Path path) {
      if (super.isSelectable(supplier, path)) {
        assertSame(Wheel.class, path.type());
        final Element e = path.get(path.size() - 1);
        final List<String> arguments = e.arguments().orElse(null);
        return "wheel".equals(e.name()) && arguments != null && !arguments.isEmpty() && "LR".equals(arguments.get(0));
      } else {
        return false;
      }
    }

    public final Value<Wheel> get(final ConfiguredSupplier<?> supplier,
                                  final Path path) {
      assertSame(Wheel.class, path.type());
      final Element e = path.get(path.size() - 1);
      assertEquals("wheel", e.name());
      assertEquals(List.of(String.class), e.parameters().orElseThrow());
      assertEquals("LR", e.arguments().orElseThrow().get(0));
      return new Value<>(Qualifiers.of(),
                         Path.of(Element.of("wheel",
                                            Wheel.class,
                                            List.of(String.class),
                                            List.of("LR"))),
                         new Wheel() {
                           @Override
                           public final int getDiameterInInches() {
                             return 24;
                           }
                         });
    }

  }

}
