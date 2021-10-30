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

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.microbean.settings.api.Provider.Value;

public interface SupplierBroker {
  
  public default <T> Supplier<T> supplier(final Qualifiers contextQualifiers,
                                          final Type type,
                                          final Supplier<T> defaultSupplier) {
    return this.supplier(contextQualifiers, Path.of().plus(Accessor.of("supplier"), type), defaultSupplier);
  }

  public default <T> Supplier<T> supplier(final Qualifiers contextQualifiers,
                                          final Path path,
                                          final Supplier<T> defaultSupplier) {
    return this.supplier(new Context<>(this, contextQualifiers, this, path), defaultSupplier);
  }

  public default <T> Supplier<T> supplier(final Context<?> context,
                                          final Supplier<T> defaultSupplier) {
    return this.supplier(context, SupplierBroker::sink, SupplierBroker::sink, SupplierBroker::sink, defaultSupplier);
  }

  public <T> Supplier<T> supplier(final Context<?> context,
                                  final Consumer<? super Provider> rejectedProviders,
                                  final Consumer<? super Value<?>> rejectedValues,
                                  final Consumer<? super Value<?>> ambiguousValues,
                                  final Supplier<T> defaultSupplier);

  private static void sink(final Object ignored) {

  }

}
