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
package org.microbean.settings;

import java.util.function.Supplier;

import org.microbean.settings.api.Configured;
import org.microbean.settings.api.Path;
import org.microbean.settings.api.Qualifiers;

import org.microbean.settings.provider.AbstractProvider;
import org.microbean.settings.provider.Value;

public final class EnvironmentVariableProvider extends AbstractProvider<String> {

  public EnvironmentVariableProvider() {
    super();
  }

  @Override // AbstractProvider<String>
  public <T> Value<T> get(final Configured<?> requestor, final Path<T> absolutePath) {
    assert absolutePath.isAbsolute();
    assert absolutePath.startsWith(requestor.absolutePath());
    assert !absolutePath.equals(requestor.absolutePath());

    // On Unix systems, there is absolutely no question that the
    // environment is entirely immutable, even when probed via
    // System#getenv(String).  See
    // https://github.com/openjdk/jdk/blob/dfacda488bfbe2e11e8d607a6d08527710286982/src/java.base/unix/classes/java/lang/ProcessEnvironment.java#L67-L91.
    //
    // Things are ever so slightly more murky in Windows land.  As of
    // JDK 17, the environment there is also entirely immutable:
    // https://github.com/openjdk/jdk/blob/dfacda488bfbe2e11e8d607a6d08527710286982/src/java.base/windows/classes/java/lang/ProcessEnvironment.java#L257-L258
    // but the class is not as "immutable looking" as the Unix one and
    // it seems to be designed for updating in some cases.
    // Nevertheless, for the System#getenv(String) case, the
    // environment is immutable.
    //
    // TL;DR: System.getenv("foo") will always return a value for
    // "foo" if ever there was one, and will always return null if
    // there wasn't.

    if (absolutePath.size() == 2) {
      final String name = absolutePath.last().name();
      final String value = System.getenv(name);
      if (value != null) {
        @SuppressWarnings("unchecked")
        final Value<T> returnValue =
          new Value<>(null, // no defaults
                      Qualifiers.of(),
                      absolutePath,
                      (Supplier<T>)() -> (T)value,
                      false, // nulls are not permitted
                      true); // deterministic
        return returnValue;
      }
    }
    return null;
  }

}
