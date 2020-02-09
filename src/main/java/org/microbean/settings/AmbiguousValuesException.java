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

import java.io.Serializable; // for javadoc only

import java.util.Collection;

/**
 * A {@link SettingsException} indicating that some {@link Value}
 * instances were found to be ambiguous.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 */
public class AmbiguousValuesException extends SettingsException {


  /*
   * Static fields.
   */


  /**
   * The version of this class for {@linkplain Serializable
   * serialization purposes}.
   */
  private static final long serialVersionUID = 1L;


  /*
   * Instance fields.
   */


  /**
   * The {@link Collection} of {@link Value} instances found to be
   * ambiguous.
   *
   * @nullability This field may be {@code null} at any point.
   */
  private final Collection<Value> values;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link AmbiguousValuesException}.
   *
   * @param values the {@link Collection} of {@link Value} instances
   * found to be ambiguous; may be {@code null}; stored by reference
   *
   * @see #getValues()
   */
  public AmbiguousValuesException(final Collection<Value> values) {
    super();
    this.values = values;
  }


  /*
   * Instance methods.
   */


  /**
   * Returns the {@link Collection} of {@link Value} instances found
   * to be ambiguous.
   *
   * @return the {@link Collection} of {@link Value} instances found
   * to be ambiguous, or {@code null}
   *
   * @nullability This method and its overrides may return {@code
   * null}.
   *
   * @idempotency No guarantees with respect to idempotency are made
   * about this method or its overrides.
   */
  public Collection<Value> getValues() {
    return this.values;
  }

}
