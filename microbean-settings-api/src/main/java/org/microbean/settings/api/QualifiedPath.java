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
package org.microbean.settings.api;

import java.lang.reflect.Type;

import java.util.Map;
import java.util.Objects;

import org.microbean.development.annotation.OverridingDiscouraged;
import org.microbean.development.annotation.OverridingEncouraged;

public interface QualifiedPath extends Prioritized {

  @Override // Prioritized
  public default int priority() {
    return this.path().priority();
  }
  
  public Path path();

  @OverridingEncouraged
  public default Map<?, ?> applicationQualifiers() {
    return Map.of();
  }

  @OverridingDiscouraged
  public default boolean isAssignable(final Map<?, ?> qualifiers) {
    return Qualifiers.isAssignable(this.applicationQualifiers(), qualifiers);
  }

  public default Type type() {
    return this.path().targetType();
  }

  public default Class<?> rawClass() {
    return this.path().targetClass();
  }

  public static record Record(Path path, Map<?, ?> applicationQualifiers) implements QualifiedPath {

    public Record {
      Objects.requireNonNull(path, "path");
      applicationQualifiers = applicationQualifiers == null ? Map.of() : Map.copyOf(applicationQualifiers);
    }

  }

}
