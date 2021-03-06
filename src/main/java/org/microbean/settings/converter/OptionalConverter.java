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
package org.microbean.settings.converter;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.inject.Vetoed;

import javax.inject.Inject;

import org.microbean.settings.Converter;
import org.microbean.settings.Value;

@Vetoed
public class OptionalConverter<T> implements Converter<Optional<T>> {

  private static final long serialVersionUID = 1L;

  private final Converter<? extends T> baseConverter;

  public OptionalConverter(final Converter<? extends T> baseConverter) {
    super();
    this.baseConverter = Objects.requireNonNull(baseConverter);
  }

  @Override
  public Optional<T> convert(final Value value) {
    final Optional<T> returnValue;
    if (value == null) {
      returnValue = Optional.empty();
    } else {
      returnValue = Optional.ofNullable(this.baseConverter.convert(value));
    }
    return returnValue;
  }

}
