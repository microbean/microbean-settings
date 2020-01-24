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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;

public final class URLConverter implements Converter<URL> {

  private static final long serialVersionUID = 1L;

  public URLConverter() {
    super();
  }

  @Override
  public final URL convert(final Value value) {
    final URL returnValue;
    if (value == null) {
      returnValue = null;
    } else {
      final String stringValue = value.get();
      if (stringValue == null) {
        returnValue = null;
      } else {
        URL result = null;
        try {
          result = new URI(stringValue).toURL();
        } catch (final MalformedURLException | URISyntaxException exception) {
          throw new IllegalArgumentException(stringValue, exception);
        } finally {
          returnValue = result;
        }
      }
    }
    return returnValue;
  }

}
