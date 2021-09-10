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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;

import java.util.function.Supplier;

public abstract class Configured<T, C extends Coordinates> {


  /*
   * Static fields.
   */


  private static final ClassValue<Type> targetTypeExtractor = new TypeArgumentExtractor(Configured.class, 0);

  private static final ClassValue<Type> coordinatesTypeExtractor = new TypeArgumentExtractor(Configured.class, 1);

  private static final Map<Key, List<Configured<?, ?>>> cache;

  private static final Coordinates coordinates;


  /*
   * Static initializer.
   */


  static {
    @SuppressWarnings("rawtypes")
    final ServiceLoader<Configured> configureds = ServiceLoader.load(Configured.class);
    final Map<Key, List<Configured<?, ?>>> map = new HashMap<>();
    for (final Configured<?, ?> configured : configureds) {
      map.computeIfAbsent(new Key(configured), k -> new ArrayList<>()).add(configured);
    }
    cache = Collections.unmodifiableMap(map);
    coordinates = ServiceLoader.load(Coordinates.class).findFirst().orElse(SystemPropertiesCoordinates.INSTANCE);
  }


  /*
   * Instance fields.
   */

  
  private final Object discriminator;
  
  

  /*
   * Constructors.
   */


  /**
   * Creates a new {@link Configured}.
   */
  protected Configured() {
    this((String)null);
  }
  
  /**
   * Creates a new {@link Configured}.
   *
   * @param discriminator an object used to distinguish this
   * {@link Configured} from all others of its type; may be and often is
   * {@code null}
   */
  protected Configured(final String discriminator) {
    super();
    final Type targetType = this.targetType();
    final Class<?> targetClass;
    if (targetType instanceof Class<?> c) {
      targetClass = c;
    } else if (targetType instanceof ParameterizedType ptype) {
      targetClass = (Class<?>)ptype.getRawType();
    } else {
      throw new AssertionError();
    }
    if (!targetClass.isInterface()) {
      throw new IllegalStateException();
    }
    this.discriminator = discriminator;
  }

  protected final Type targetType() {
    return targetTypeExtractor.get(this.getClass());
  }

  protected final Type coordinatesType() {
    return coordinatesTypeExtractor.get(this.getClass());
  }

  public abstract T get(final C coordinates);

  @Override // Object
  public final int hashCode() {
    return this.discriminator == null ? 0 : this.discriminator.hashCode();
  }

  @Override // Object
  public final boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other == null) {
      return false;
    } else if (this.getClass().equals(other.getClass())) {
      final Configured<?, ?> her = (Configured<?, ?>)other;
      return
        Objects.equals(this.targetType(), her.targetType()) &&
        Objects.equals(this.discriminator, her.discriminator);
    } else {
      return false;
    }
  }


  /*
   * Static methods.
   */


  public static final <T> T get(final Type targetType) {
    return get(targetType, coordinates, (String)null);
  }
  
  public static final <T> T get(final Type targetType, final String discriminator) {
    return get(targetType, coordinates, discriminator);
  }

  public static final <T, C extends Coordinates> T get(final Type targetType, final C coordinates, final String discriminator) {
    final List<Configured<?, ?>> configured = cache.get(new Key(targetType, coordinates.coordinatesType(), discriminator));
    if (configured == null || configured.isEmpty()) {
      return null;
    } else if (configured.size() == 1) {
      @SuppressWarnings("unchecked")
      final Configured<T, C> returnValue = (Configured<T, C>)configured.get(0);
      return returnValue.get(coordinates);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  private static final record Key(Type targetType, Type coordinatesType, Object discriminator) {

    private Key(final Configured<?, ?> c) {
      this(c.targetType(), c.coordinatesType(), c.discriminator);
    }
    
  }

}
