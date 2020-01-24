/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2020 microBean™.
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
package org.microbean.settings;

public final class BooleanConverter implements Converter<Boolean> {

  private static final long serialVersionUID = 1L;

  public BooleanConverter() {
    super();
  }

  @Override
  public final Boolean convert(final Value value) {
    final Boolean returnValue;
    if (value == null) {
      returnValue = Boolean.FALSE;
    } else {
      final String rawValue = value.get();
      returnValue = Boolean.valueOf(rawValue != null &&
                                    ("true".equalsIgnoreCase(rawValue) ||
                                     "y".equalsIgnoreCase(rawValue) ||
                                     "yes".equalsIgnoreCase(rawValue) ||
                                     "on".equalsIgnoreCase(rawValue) ||
                                     "1".equals(rawValue)));
    }
    return returnValue;
  }

}
