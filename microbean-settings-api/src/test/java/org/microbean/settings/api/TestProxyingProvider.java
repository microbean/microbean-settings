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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

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
  
}
