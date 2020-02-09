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
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import java.lang.reflect.Type;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import java.util.function.Supplier;

import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.Nonbinding;

import javax.inject.Qualifier;

/**
 * A {@link Qualifier} indicating that the annotated element should be
 * provided via a {@link Settings}-originated value acquisition
 * operation, such as {@link Settings#get(String, Set, Converter,
 * BiFunction)}.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see Settings#get(String, Set, Converter, BiFunction)
 */
@Documented
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE })
public @interface Setting {


  /*
   * Static fields.
   */


  /**
   * A value indicating {@code null} for use in annotation elements,
   * since annotation elements cannot return {@code null}.
   *
   * @see #defaultValue()
   */
  static final String UNSET = "\u2400"; // see https://en.wikipedia.org/wiki/Control_Pictures


  /*
   * Elements.
   */


  /**
   * The setting name.
   *
   * @return the setting name; never {@code null} but possibly {@link
   * #UNSET}
   */
  String name();

  /**
   * The default value for the setting.
   *
   * @return the default value; never {@code null} but possibly {@link
   * #UNSET}
   */
  @Nonbinding
  String defaultValue() default UNSET;

  /**
   * Whether a value for the setting must be present; it is illegal to
   * set this to {@code true} and set the {@link #defaultValue()
   * defaultValue} element to a value other than {@link #UNSET}
   *
   * @return whether a value for the setting must be present
   */
  @Nonbinding
  boolean required() default false;


  /*
   * Inner and nested classes.
   */


  /**
   * An {@link AnnotationLiteral} that implements the {@link Setting}
   * interface/annotation.
   *
   * @author <a href="https://about.me/lairdnelson"
   * target="_parent">Laird Nelson</a>
   *
   * @see Setting
   */
  public static final class Literal extends AnnotationLiteral<Setting> implements Setting {

    private static final long serialVersionUID = 1L;

    private final String name;

    private final String defaultValue;

    private final boolean required;

    /**
     * Creates a new {@link Literal}.
     *
     * @param name the value for the {@link Setting#name() name}
     * element; may be {@code null} in which case {@link
     * Setting#UNSET} will be used instead
     *
     * @param defaultValue the value for the {@link
     * Setting#defaultValue() defaultValue} element; may be {@code
     * null} in which case {@link Setting#UNSET} will be used instead
     *
     * @param required the value for the {@link Setting#required()
     * required} element
     */
    public Literal(final String name, final String defaultValue, final boolean required) {
      this.name = name == null ? UNSET : name;
      this.defaultValue = defaultValue == null ? UNSET : defaultValue;
      this.required = required;
    }

    /**
     * The setting name.
     *
     * @return the setting name; never {@code null} but possibly {@link
     * #UNSET}
     */
    @Override
    public final String name() {
      return this.name;
    }

    /**
     * The default value for the setting.
     *
     * @return the default value; never {@code null} but possibly
     * {@link #UNSET}
     */
    @Override
    public final String defaultValue() {
      return this.defaultValue;
    }

    /**
     * Whether a value for the setting must be present; it is illegal
     * to set this to {@code true} and set the {@link #defaultValue()
     * defaultValue} element to a value other than {@link #UNSET}
     *
     * @return whether a value for the setting must be present
     */
    @Override
    public final boolean required() {
      return this.required;
    }

  }

}
