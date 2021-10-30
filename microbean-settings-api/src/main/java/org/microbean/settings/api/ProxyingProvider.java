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

import java.util.function.Supplier;

import org.microbean.type.Types;

public class ProxyingProvider extends AbstractProvider<Object> {

  public ProxyingProvider() {
    super();
  }

  @Override // Provider
  public boolean isSelectable(final SupplierBroker broker,
                              final Qualifiers qualifiers,
                              final Supplier<?> parentSupplier,
                              final Path path) {
    return super.isSelectable(broker, qualifiers, parentSupplier, path) && this.isProxyable(path);
  }

  public boolean isProxyable(final Path path) {
    final Type type = path.type();
    return type instanceof Class<?> c && c.isInterface();
  }

  @Override // Provider
  public Value<?> get(final SupplierBroker broker,
                      final Qualifiers qualifiers,
                      final Supplier<?> parentSupplier,
                      final Path path) {
    final Class<?> c = Types.erase(path.type());
    return null; // not supported yet
  }

  /*
  private final Object computeProxy(final QualifiedPath.Record qpr,
                                    final Function<? super QualifiedPath, ? extends Collection<ValueSupplier>> valueSuppliers) {
    final Path path = qpr.path();
    final Class<?> targetClass = path.targetClass();
    return
      Proxy.newProxyInstance(targetClass.getClassLoader(),
                             new Class<?>[] { targetClass },
                             new Handler(qpr,
                                         this.defaultTargetSupplierFunction.apply(path),
                                         this.pathComponentFunction,
                                         valueSuppliers));
  }
  */
  
}
