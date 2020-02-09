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

import java.lang.annotation.Annotation;

import java.util.Collections;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;

/**
 * A {@link Source} that retrieves values from environment variables.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see #getValue(String, Set)
 */
@ApplicationScoped
public class EnvironmentVariablesSource extends Source {


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link EnvironmentVariablesSource}.
   */
  public EnvironmentVariablesSource() {
    super();
  }


  /*
   * Instance methods.
   */

  
  /**
   * Returns a {@link Value} suitable for the supplied {@code name} by
   * making use of the {@link System#getenv(String)} method.
   *
   * @param name the name of the setting; must not be {@code null}
   *
   * @param qualifiers a {@link Set} of {@link Annotation}s qualifying
   * the request; the {@link Value} returned by this method will
   * {@linkplain Value#getQualifiers() have} a subset of these
   * qualifiers; must not be {@code null}
   *
   * @return a suitable {@link Value}, or {@code null} if an
   * invocation of {@link System#getenv(String)} with the supplied
   * {@code name} returns {@code null}
   *
   * @exception NullPointerException if either {@code name} or {@code
   * qualifiers} is {@code null}
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency No guarantees with respect to idempotency are made
   * about this method.
   *
   * @nullability This method may return {@code null}.
   */
  @Override
  public Value getValue(final String name, final Set<Annotation> qualifiers) {
    final Value returnValue;
    final String stringValue = System.getenv(name);
    if (stringValue == null) {
      returnValue = null;
    } else {
      returnValue = new Value(this, name, Collections.emptySet(), false, stringValue);
    }
    return returnValue;
  }
  
}
