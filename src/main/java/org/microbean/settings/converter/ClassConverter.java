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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;

import org.microbean.settings.Converter;
import org.microbean.settings.Value;

@Dependent
public class ClassConverter<T> implements Converter<Class<T>> {

  private static final long serialVersionUID = 1L;

  public ClassConverter() {
    super();
  }

  @Override
  public Class<T> convert(final Value value) {
    final Class<T> returnValue;
    if (value == null) {
      returnValue = null;
    } else {
      final String className = value.get();
      if (className == null) {
        returnValue = null;
      } else {
        Class<T> result = null;
        try {
          @SuppressWarnings("unchecked")
          final Class<T> temp = (Class<T>)Class.forName(className, true, Thread.currentThread().getContextClassLoader());
          result = temp;
        } catch (final ClassNotFoundException classNotFoundException) {
          throw new IllegalArgumentException(className, classNotFoundException);
        } finally {
          returnValue = result;
        }
      }
    }
    return returnValue;
  }

}
