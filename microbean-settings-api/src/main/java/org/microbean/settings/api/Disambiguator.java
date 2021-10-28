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

import org.microbean.settings.api.Provider.Value;

public interface Disambiguator {

  public static final Disambiguator DEFAULT = new Disambiguator() {};

  public default Value<?> disambiguate(final Qualifiers contextQualifiers,
                                       final Context<?> context,
                                       final Provider p0,
                                       final Value<?> v0,
                                       final Provider p1,
                                       final Value<?> v1) {
    return null;
  }
  
}
