/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2021 microBean™.
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

import java.util.Optional;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import java.util.function.Supplier;

import org.microbean.settings.api.Configured;
import org.microbean.settings.api.Path;
import org.microbean.settings.api.Qualifiers;

import org.microbean.settings.provider.AbstractProvider;
import org.microbean.settings.provider.Value;

public class SystemPropertiesQualifiersProvider extends AbstractProvider<Qualifiers> {

  public SystemPropertiesQualifiersProvider() {
    super();
  }

  @Override // AbstractProvider<Qualifiers>
  public Value<?> get(final Configured<?> supplier, final Path path) {
    // Use the configuration system to find a String under the path
    // :void/qualifierPrefix:java.lang.String.
    final String prefix = supplier.of("qualifierPrefix", String.class).orElse("qualifier.");
    final int prefixLength = prefix.length();
    final Properties systemProperties = System.getProperties();
    final SortedMap<String, String> map = new TreeMap<>();
    for (final String propertyName : systemProperties.stringPropertyNames()) {
      if (propertyName.startsWith(prefix) && propertyName.length() > prefixLength) {
        final String qualifierValue = systemProperties.getProperty(propertyName);
        if (qualifierValue != null) {
          map.put(propertyName.substring(prefixLength), qualifierValue);
        }
      }
    }
    return new Value<>(Qualifiers.of(), Path.of(Qualifiers.class), Qualifiers.of(map));
  }

}