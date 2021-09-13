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

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import java.util.List;

import java.util.function.Supplier;

import org.microbean.development.annotation.Convenience;

final class InvocationHandler<T> implements java.lang.reflect.InvocationHandler {

  private final List<ConfiguredInfo<T>> cis;

  private final Supplier<? extends T> lastResort;
  
  InvocationHandler(final List<? extends ConfiguredInfo<T>> cis,
                    final Supplier<? extends T> lastResort) {
    super();
    this.cis = List.copyOf(cis);
    this.lastResort = lastResort;
  }

  @Convenience
  @SuppressWarnings("unchecked")
  public final T proxy(final ClassLoader classLoader, final Class<T> configurationInterface) {
    return (T)Proxy.newProxyInstance(classLoader, new Class<?>[] { configurationInterface }, this);
  }
  
  @Override
  public final Object invoke(final Object proxy,
                             final Method method,
                             final Object[] args)
    throws ReflectiveOperationException {
    if (method.getDeclaringClass() == Object.class) {
      return
        switch (method.getName()) {
        case "hashCode" -> System.identityHashCode(proxy);
        case "equals" -> proxy == args[0];
        case "toString" -> String.valueOf(proxy);
        default -> throw new AssertionError();
        };
    } else {
      for (final ConfiguredInfo<T> ci : this.cis) {
        final Supplier<?> valueSupplier = ci.valueSupplier(method);
        if (valueSupplier != null) {
          return valueSupplier.get();
        }
      }
      final T lastResort = this.lastResort == null ? null : this.lastResort.get();
      if (lastResort == null) {
        throw new UnsupportedOperationException(method.getName());
      } else {
        return method.invoke(lastResort, args);
      }
    }
  }
  
}
