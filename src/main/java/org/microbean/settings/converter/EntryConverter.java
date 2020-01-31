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

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Set;

import java.util.regex.Pattern;

import javax.enterprise.inject.Vetoed;

import org.microbean.settings.Converter;
import org.microbean.settings.Value;

@Vetoed
public class EntryConverter<K, V> implements Converter<Entry<K, V>> {

  private static final long serialVersionUID = 1L;
  
  private static final Pattern backslashEqualsPattern = Pattern.compile("\\\\=");

  private static final Pattern splitPattern = Pattern.compile("(?<!\\\\)=");

  private final Converter<? extends K> keyConverter;

  private final Converter<? extends V> valueConverter;
  
  public EntryConverter(final Converter<? extends K> keyConverter,
                        final Converter<? extends V> valueConverter) {
    super();
    this.keyConverter = keyConverter;
    this.valueConverter = valueConverter;
  }

  @Override
  public Entry<K, V> convert(final Value value) {
    final Entry<K, V> returnValue;
    if (value == null) {
      returnValue = null;
    } else {
      final String stringValue = value.get();
      if (stringValue == null) {
        returnValue = null;
      } else {
        final String[] parts = split(stringValue);
        assert parts != null;
        final String keyString;
        if (parts.length > 0) {
          keyString = parts[0];
        } else {
          keyString = null;
        }
        final String valueString;
        if (parts.length > 1) {
          assert parts.length == 2 : "Unexpected parts: " + Arrays.asList(parts);
          valueString = parts[1];
        } else {
          valueString = null;
        }
        returnValue = new SimpleImmutableEntry<>(this.keyConverter.convert(new Value(value, keyString)),
                                                 this.valueConverter.convert(new Value(value, valueString)));
      }
    }
    return returnValue;
  }

  private static final String[] split(final String text) {
    final String[] returnValue;
    if (text == null) {
      returnValue = new String[0];
    } else {
      returnValue = splitPattern.split(text, 2);
      assert returnValue != null;
      for (int i = 0; i < returnValue.length; i++) {
        // TODO: maybe just do this replacement on the key, not the
        // value?  That would eventually allow maps of maps.
        returnValue[i] = backslashEqualsPattern.matcher(returnValue[i]).replaceAll("=");
      }
    }
    return returnValue;
  }
                           

}
