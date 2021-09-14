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
import java.util.Set;

import java.util.function.Supplier;

public abstract class Configured<T> implements Supplier<T> {


  /*
   * Static fields.
   */


  private static final ClassValue<Type> targetTypeExtractor = new TypeArgumentExtractor(Configured.class, 0);

  private static final Map<Key, List<Configured<?>>> cache;


  /*
   * Static initializer.
   */


  static {
    @SuppressWarnings("rawtypes")
    final ServiceLoader<Configured> configureds = ServiceLoader.load(Configured.class);
    final Map<Key, List<Configured<?>>> map = new HashMap<>();
    for (final Configured<?> configured : configureds) {
      map.computeIfAbsent(new Key(configured), k -> new ArrayList<>()).add(configured);
    }
    cache = Collections.unmodifiableMap(map);
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


  /*
   * Instance methods.
   */
  

  private final Type targetType() {
    return targetTypeExtractor.get(this.getClass());
  }

  @Override // Supplier<T>
  public T get() {
    return this.get(Set.of());
  }
  
  public abstract T get(final Set<?> qualifiers);

  @Override // Object
  public final int hashCode() {
    return Objects.hash(this.targetType(), this.discriminator);
  }

  @Override // Object
  public final boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other != null && this.getClass().equals(other.getClass())) {
      final Configured<?> her = (Configured<?>)other;
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
    return get(targetType, Set.of(), (String)null);
  }
  
  public static final <T> T get(final Type targetType, final String discriminator) {
    return get(targetType, Set.of(), discriminator);
  }

  public static final <T> T get(final Type targetType, final Set<?> qualifiers, final String discriminator) {
    final List<Configured<?>> configured = cache.get(new Key(targetType, discriminator));
    if (configured == null || configured.isEmpty()) {
      return null;
    } else if (configured.size() == 1) {
      @SuppressWarnings("unchecked")
      final Configured<T> returnValue = (Configured<T>)configured.get(0);
      return returnValue.get(Set.copyOf(qualifiers));
    } else {
      throw new UnsupportedOperationException();
    }
  }

  private static final record Key(Type targetType, Object discriminator) {

    private Key(final Configured<?> c) {
      this(c.targetType(), c.discriminator);
    }
    
  }

}
