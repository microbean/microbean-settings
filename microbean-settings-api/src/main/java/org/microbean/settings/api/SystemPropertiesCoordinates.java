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

final class SystemPropertiesCoordinates implements Coordinates {

  static final SystemPropertiesCoordinates INSTANCE = new SystemPropertiesCoordinates();

  private SystemPropertiesCoordinates() {
    super();
  }

  public final <T> T get(final String key, final Class<T> c) {
    return c.cast(System.getProperties().get(key));
  }

  @Override
  public final boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other == null) {
      return false;
    } else if (this.getClass().equals(other.getClass())) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  public final int hashCode() {
    return 1;
  }

}
