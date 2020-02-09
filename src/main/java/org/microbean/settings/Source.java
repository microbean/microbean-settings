/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2019–2020 microBean™.
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

import java.util.Set;

import org.microbean.settings.Value;

/**
 * An abstraction of a source of {@link String}-typed configuration
 * values.
 *
 * <p>{@link Source} instances are used most commonly by {@link
 * Settings} instances, which layer conversion and arbitration atop
 * them.</p>
 *
 * @threadsafety Instances of this class must be safe for concurrent
 * use by multiple threads.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see #getValue(String, Set)
 *
 * @see Value
 *
 * @see Settings
 *
 * @see Settings#get(String, Set, Class, BiFunction)
 */
public abstract class Source {

  /**
   * Creates or otherwise acquires and returns a {@link Value} for the
   * supplied {@code name} and {@link Set} of qualifiers.
   *
   * @param name the name of the setting for which a {@link Value} is
   * to be returned; must not be {@code null}
   * 
   * @param qualifiers a {@link Set} of qualifier {@link Annotation}
   * instances; may be {@code null}
   *
   * @return a suitable {@link Value}, or {@code null} if no {@link
   * Value} could be created or acquired
   *
   * @exception NullPointerException if {@code name} is {@code null}
   *
   * @nullability Implementations of this method are permitted to
   * return {@code null}.
   *
   * @idempotency An implementation of this method need not be
   * idempotent.  That is, two invocations supplied with the same
   * {@code name} and {@code qualifiers} parameter values may or may
   * not return {@link Value}s that are identical, {@linkplain
   * Object#equals(Object) equal} or neither.
   *
   * @threadsafety Implementations of this method must be safe for
   * concurrent use by multiple threads.
   */
  public abstract Value getValue(final String name, final Set<Annotation> qualifiers);
  
}
