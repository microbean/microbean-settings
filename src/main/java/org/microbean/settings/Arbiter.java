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

import java.io.Serializable;

import java.lang.annotation.Annotation;

import java.util.Collection;
import java.util.Collections; // for javadoc only
import java.util.Set;

/**
 * Provides <a href="{@docRoot}/overview-summary.html#ambiguity">value
 * arbitration services</a> most commonly to a {@link Settings}
 * instance in cases where {@linkplain Value setting values} would
 * otherwise be indistinguishable from one another.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see #arbitrate(Set, String, Set, Collection)
 *
 * @see Settings#arbitrate(Set, String, Set, Collection)
 *
 * @see Settings#Settings(Set, BiFunction, ConverterProvider,
 * Iterable)
 */
public abstract class Arbiter implements Serializable {


  /*
   * Static fields.
   */

  
  /**
   * The version of this class for {@linkplain Serializable
   * serialization purposes}.
   */
  private static final long serialVersionUID = 1L;


  /*
   * Constructors.
   */

  
  /**
   * Creates a new {@link Arbiter}.
   */
  protected Arbiter() {
    super();
  }


  /*
   * Instance methods.
   */


  /**
   * Performs <em>value arbitration</em> on a {@link Collection} of
   * {@link Value}s that (normally) a {@link Settings} instance
   * determined were indistinguishable during value acquisition, and
   * returns the {@link Value} to be used instead (normally drawn from
   * the {@link Collection} according to some heuristic).
   *
   * @param sources the {@link Set} of {@link Source}s in effect
   * during the current value acquisition operation; must not be
   * {@code null}; must be {@linkplain
   * Collections#unmodifiableSet(Set) unmodifiable}; must be safe for
   * concurrent read-only access by multiple threads
   *
   * @param name the name of the setting value being sought; must not
   * be {@code null}
   *
   * @param qualifiers the {@link Set} of qualifier {@link
   * Annotation}s in effect during the current value acquisition
   * operation; must not be {@code null}; must be {@linkplain
   * Collections#unmodifiableSet(Set) unmodifiable}; must be safe for
   * concurrent read-only access by multiple threads
   *
   * @param values the {@link Collection} of {@link Value}s acquired
   * during the current value acquisition operation that were deemed
   * to be indistinguishable; must not be {@code null}; must be
   * {@linkplain Collections#unmodifiableSet(Set) unmodifiable}; must
   * be safe for concurrent read-only access by multiple threads
   *
   * @return the result of value arbitration as a single {@link
   * Value}, or {@code null} if this {@link Arbiter} could not select
   * a single {@link Value}
   *
   * @exception NullPointerException if any parameter value is {@code
   * null}
   *
   * @exception ArbitrationException if there was a procedural problem
   * with arbitration
   */
  public abstract Value arbitrate(final Set<? extends Source> sources, // non-null
                                  final String name, // non-null
                                  final Set<? extends Annotation> qualifiers,
                                  final Collection<? extends Value> values);
  
}
