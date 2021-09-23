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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import java.util.function.Function;
import java.util.function.Supplier;

// The "top level" ValueSupplier.
public abstract class Configured2<T> extends AbstractValueSupplier implements Supplier<T> {


  /*
   * Static fields.
   */

  
  private static final TypeArgumentExtractor typeArgumentExtractor = new TypeArgumentExtractor(Configured2.class, 0);


  /*
   * Instance fields.
   */

  
  private final Map<?, ?> qualifiers;


  /*
   * Constructors.
   */

  
  protected Configured2(final Map<?, ?> qualifiers) {
    super();
    this.qualifiers = qualifiers == null ? Map.of() : Map.copyOf(qualifiers);
  }


  /*
   * Instance methods.
   */
  

  public final Map<?, ?> qualifiers() {
    return this.qualifiers;
  }
  
  @Override // ValueSupplier
  public boolean respondsFor(final QualifiedPath qualifiedPath) {
    return
      qualifiedPath.path().isRoot(typeArgumentExtractor.get(this.getClass())) &&
      qualifiedPath.applicationQualifiers().equals(this.qualifiers());
  }

  @Override // ValueSupplier
  public final Value<T> get(final QualifiedPath qualifiedPath,
                            final Function<? super QualifiedPath, ? extends Collection<ValueSupplier>> valueSuppliers) {
    if (this.respondsFor(qualifiedPath)) {
      return new Value<>(this.get(), qualifiedPath.path(), this.qualifiers());
    } else {
      return null;
    }
  }


  /*
   * Static methods.
   */
  

  public static final <T> T get(final Type type) {
    return get(type, null);
  }
  
  public static final <T> T get(final Type type, final Supplier<? extends T> defaultTargetSupplier) {
    final QualifiedPath qp = new QualifiedPath.Record(new Path(type), Qualifiers.application());
    final Supplier<?> value;
    final Collection<ValueSupplier> valueSuppliers = ValueSupplier.loadedValueSuppliers(qp);
    if (valueSuppliers.isEmpty()) {
      final ValueSupplier valueSupplier = useProxyBasedMechanism(qp, defaultTargetSupplier);
      if (valueSupplier == null) {
        throw new UnsupportedOperationException();
      }
      value = valueSupplier.get(qp, ValueSupplier::loadedValueSuppliers);
    } else {
      value = ValueSupplier.resolve(valueSuppliers, qp, ValueSupplier::loadedValueSuppliers);
    }
    @SuppressWarnings("unchecked")
    final T rv = value == null ? null : (T)value.get();
    return rv;
  }

  private static final <T> ValueSupplier useProxyBasedMechanism(final QualifiedPath qp, Supplier<? extends T> defaultTargetSupplier) {
    return null; // TODO: implement
  }
  
}
