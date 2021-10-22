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

import java.util.List;

import org.junit.jupiter.api.Test;

import org.microbean.settings.api.Provider.Value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.microbean.settings.api.Path.absoluteOf;
import static org.microbean.settings.api.Path.fragmentOf;
import static org.microbean.settings.api.Path.root;

final class TestContext {

  private TestContext() {
    super();
  }

  @Test
  final void testBasicContextStructure() {
    final Context context =
      new Context(Path.root(Qualifiers.of("env", "test", "stability", "bad")).plus(Car.class),
                  new Car() {}, // Object
                  QualifiedRecord.of(Qualifiers.of("accessor", "getDrivetrain"),
                                     Drivetrain.class));
    assertSame(Drivetrain.class, context.target().qualified());
    assertSame(Car.class, context.parentPath().target().qualified());
  }
  
  private static final class DummyCarProvider implements Provider {

    @Override // Provider
    public final Value<?> get(final Context context) {
      return Value.of(null); // dummy response
    }

    @Override // Provider
    public boolean isSelectable(final Context context) {
      return context.target().isAssignable(Car.class);
    }
    
  }
  
  private static interface Car {

    default Drivetrain getDrivetrain() {
      return null; // dummy response
    }
    
  }

  private static interface Drivetrain {

  }

}
