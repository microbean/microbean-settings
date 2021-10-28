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

import org.microbean.settings.api.Provider.Value;

public class AmbiguousConfigurationException extends RuntimeException {

  private static final long serialVersionUID = 1L;
  
  private final List<Value<?>> values;
  
  public AmbiguousConfigurationException(final Collection<? extends Value<?>> values) {
    super();
    this.values = List.copyOf(values);
  }

  public final List<Value<?>> values() {
    return this.values;
  }

  @Override
  public String toString() {
    return this.values().toString();
  }

}
