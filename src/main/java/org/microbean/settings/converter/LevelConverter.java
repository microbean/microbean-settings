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
package org.microbean.settings.converter;

import java.util.logging.Level;

import javax.enterprise.context.ApplicationScoped;

import org.microbean.settings.Converter;
import org.microbean.settings.Value;

@ApplicationScoped
public class LevelConverter implements Converter<Level> {

  private static final long serialVersionUID = 1L;

  public LevelConverter() {
    super();
  }

  @Override
  public Level convert(final Value value) {
    final Level returnValue;
    if (value == null) {
      returnValue = null;
    } else {
      final String stringValue = value.get();
      if (stringValue == null) {
        returnValue = null;
      } else {
        returnValue = Level.parse(stringValue);
      }
    }
    return returnValue;
  }

}
