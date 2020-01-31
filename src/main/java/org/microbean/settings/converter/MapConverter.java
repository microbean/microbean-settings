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

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Collections;
import java.util.Set;
import java.util.Objects;

import javax.enterprise.inject.Vetoed;

import org.microbean.settings.Converter;
import org.microbean.settings.Value;

@Vetoed
public class MapConverter<K, V> implements Converter<Map<K, V>> {

  private static final long serialVersionUID = 1L;

  private final Converter<? extends Set<? extends Entry<? extends K, ? extends V>>> converter;

  public MapConverter(final Converter<? extends Set<? extends Entry<? extends K, ? extends V>>> converter) {
    super();
    this.converter = Objects.requireNonNull(converter);
  }

  @Override
  public Map<K, V> convert(final Value value) {
    final Map<K, V> returnValue;
    final Set<? extends Entry<? extends K, ? extends V>> entrySet = this.converter.convert(value);
    if (entrySet == null) {
      returnValue = null;
    } else if (entrySet.isEmpty()) {
      returnValue = Collections.emptyMap();
    } else {
      final Set<Entry<K, V>> properlyTypedEntrySet = new LinkedHashSet<>();
      for (final Entry<? extends K, ? extends V> entry : entrySet) {
        properlyTypedEntrySet.add(new SimpleImmutableEntry<>(entry));
      }
      final Set<Entry<K, V>> canonicalEntrySet = Collections.unmodifiableSet(properlyTypedEntrySet);
      returnValue = new AbstractMap<K, V>() {
          @Override
          public final Set<Entry<K, V>> entrySet() {
            return canonicalEntrySet;
          }
        };
    }
    return returnValue;
  }

}
