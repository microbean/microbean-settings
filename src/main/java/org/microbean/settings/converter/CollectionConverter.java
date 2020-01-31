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

import java.util.Collection;
import java.util.Objects;

import java.util.function.Function;

import java.util.regex.Pattern;

import javax.enterprise.inject.Vetoed;

import org.microbean.settings.Converter;
import org.microbean.settings.Value;

@Vetoed
public class CollectionConverter<C extends Collection<T>, T> implements Converter<C> {

  private static final long serialVersionUID = 1L;

  private static final Pattern backslashCommaPattern = Pattern.compile("\\\\,");

  private static final Pattern splitPattern = Pattern.compile("(?<!\\\\),");

  private final Function<? super Integer, ? extends C> collectionCreator;

  private final Function<? super C, ? extends C> immutableCollectionCreator;

  private final Converter<? extends T> scalarConverter;

  public CollectionConverter(final Function<? super Integer, ? extends C> collectionCreator,
                             final Function<? super C, ? extends C> immutableCollectionCreator,
                             final Converter<? extends T> scalarConverter) {
    super();
    this.collectionCreator = Objects.requireNonNull(collectionCreator);
    this.immutableCollectionCreator = immutableCollectionCreator;
    this.scalarConverter = Objects.requireNonNull(scalarConverter);
  }

  @Override
  public final C convert(final Value value) {
    final C returnValue;
    if (value == null) {
      returnValue = null;
    } else {
      final C collection;
      final String stringValue = value.get();
      if (stringValue == null || stringValue.isEmpty()) {
        collection = this.collectionCreator.apply(Integer.valueOf(0));
      } else {
        final String[] parts = split(stringValue);
        assert parts != null;
        assert parts.length > 0;
        collection = this.collectionCreator.apply(Integer.valueOf(parts.length));
        if (collection == null) {
          throw new IllegalArgumentException("collectionCreator.apply(" + parts.length + ") == null");
        }
        for (final String part : parts) {
          collection.add(this.scalarConverter.convert(new Value(value, part)));
        }
      }
      assert collection != null;
      if (this.immutableCollectionCreator != null) {
        returnValue = this.immutableCollectionCreator.apply(collection);
      } else {
        returnValue = collection;
      }
    }
    return returnValue;
  }

  private static final String[] split(final String text) {
    final String[] returnValue;
    if (text == null) {
      returnValue = new String[0];
    } else {
      returnValue = splitPattern.split(text);
      assert returnValue != null;
      for (int i = 0; i < returnValue.length; i++) {
        returnValue[i] = backslashCommaPattern.matcher(returnValue[i]).replaceAll(",");
      }
    }
    return returnValue;
  }


}
