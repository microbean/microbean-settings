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

public final class EnvironmentVariableProvider extends AbstractProvider<String> {

  public EnvironmentVariableProvider() {
    super();
  }

  public boolean isSelectable(final ConfiguredSupplier<?> supplier, final Path2 absolutePath) {
    assert absolutePath.isAbsolute();
    return
      absolutePath.size() == 2 &&
      super.isSelectable(supplier, absolutePath) &&
      System.getenv(absolutePath.get(1).name()) != null;
    /*
    // /SHELL/java.lang.String has size 3: void.class (not shown), "SHELL", and String.class.
    return
      absolutePath.size() == 3 &&
      super.isSelectable(supplier, absolutePath) &&
      System.getenv(absolutePath.lastAccessor().name()) != null;
    */
  }
  
  public Value<?> get(final ConfiguredSupplier<?> supplier, final Path2 absolutePath) {
    assert absolutePath.isAbsolute();
    assert absolutePath.size() == 2;
    // assert absolutePath.size() == 3;
    return new Value<>(Qualifiers.of(), absolutePath, System.getenv(absolutePath.get(1).name()));
  }
  
}
