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

@Documented
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE })
public @interface Setting {

  static final String UNSET = "\u2400"; // see https://en.wikipedia.org/wiki/Control_Pictures

  String name();

  @Nonbinding
  String defaultValue() default UNSET;

  @Nonbinding
  boolean required() default false;

  public static final class Literal extends AnnotationLiteral<Setting> implements Setting {

    private static final long serialVersionUID = 1L;

    private final String name;

    private final String defaultValue;

    private final boolean required;

    public Literal(final String name, final String defaultValue, final boolean required) {
      this.name = name == null ? UNSET : name;
      this.defaultValue = defaultValue == null ? UNSET : defaultValue;
      this.required = required;
    }

    @Override
    public final String name() {
      return this.name;
    }

    @Override
    public final boolean required() {
      return this.required;
    }

    @Override
    public final String defaultValue() {
      return this.defaultValue;
    }
    
  }

}
