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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;

import javax.inject.Qualifier;

import org.microbean.development.annotation.Experimental;

/**
 * A {@link Qualifier} indicating that a relevant instance should be
 * {@linkplain Settings#configure(Object, Iterable, String, Set)
 * configured} immediately after being instantiated.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see Settings#configure(Object, Iterable, String, Set)
 */
@Documented
@Experimental
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE })
public @interface Configured {

  /**
   * An {@link AnnotationLiteral} that implements the {@link
   * Configured} interface/annotation.
   *
   * @author <a href="https://about.me/lairdnelson"
   * target="_parent">Laird Nelson</a>
   *
   * @see Configured
   */
  @Experimental
  public static final class Literal extends AnnotationLiteral<Configured> implements Configured {


    /*
     * Static fields.
     */


    /**
     * The version of this class for {@linkplain Serializable
     * serialization purposes}.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The sole instance of this class.
     *
     * @nullability This field is never {@code null}.
     */
    public static final Configured INSTANCE = new Literal();


    /*
     * Constructors.
     */


    /**
     * Creates a new {@link Literal}.
     */
    private Literal() {
      super();
    }

  }

}
