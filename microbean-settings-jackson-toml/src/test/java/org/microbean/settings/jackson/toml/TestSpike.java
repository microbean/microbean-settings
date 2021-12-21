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
package org.microbean.settings.jackson.toml;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.junit.jupiter.api.Test;

import org.microbean.settings.api.Loader;
import org.microbean.settings.api.Path;
import org.microbean.settings.api.Qualifiers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.microbean.settings.api.Loader.loader;

final class TestSpike {

  private TestSpike() {
    super();
  }

  @Test
  final void testSpike() {

    // This treats the whole application.toml as a Frobnicator,
    // ignoring unknown properties.  I'm not sure this is a great use
    // case but it's at least possible.
    final Frobnicator f = loader().load(Frobnicator.class).orElse(null);
    assertNotNull(f);
    assertEquals(37, f.getFrobnicationInterval());

    // This extracts an object out of application.toml named "gorp",
    // also ignoring unknown properties.
    final Blatz b = loader().load("gorp", Blatz.class).orElse(null);
    assertNotNull(b);
    assertEquals("foo", b.getBlatz());

    // This goes after a single string.
    final String blatz = loader().load(List.of("gorp", "blatz"), String.class).orElse(null);
  }

  @JsonAutoDetect(creatorVisibility = Visibility.NONE,
                  fieldVisibility = Visibility.NONE,
                  getterVisibility = Visibility.NONE,
                  isGetterVisibility = Visibility.NONE,
                  setterVisibility = Visibility.NONE)
  public static final class Blatz {

    private final String blatz;

    @JsonCreator
    public Blatz(@JsonProperty(value = "blatz", required = true) final String blatz) {
      super();
      this.blatz = blatz;
    }

    @JsonProperty(value = "blatz")
    public final String getBlatz() {
      return this.blatz;
    }
    
  }

  @JsonAutoDetect(creatorVisibility = Visibility.NONE,
                  fieldVisibility = Visibility.NONE,
                  getterVisibility = Visibility.NONE,
                  isGetterVisibility = Visibility.NONE,
                  setterVisibility = Visibility.NONE)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static final class Frobnicator {

    private final int frobnicationInterval;

    @JsonCreator
    public Frobnicator(@JsonProperty(value = "frobnicationInterval", required = true) final int frobnicationInterval) {
      super();
      this.frobnicationInterval = frobnicationInterval;
    }

    @JsonProperty(value = "frobnicationInterval")
    public final int getFrobnicationInterval() {
      return this.frobnicationInterval;
    }
    
  }
  
}
