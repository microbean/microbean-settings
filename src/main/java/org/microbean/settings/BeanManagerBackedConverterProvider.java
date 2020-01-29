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
package org.microbean.settings;

import java.lang.annotation.Annotation;

import java.lang.reflect.Type;

import java.util.Objects;
import java.util.Set;

import javax.enterprise.inject.Vetoed;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import javax.enterprise.util.TypeLiteral;

@Vetoed
public class BeanManagerBackedConverterProvider implements ConverterProvider {

  private final BeanManager beanManager;

  private final Set<Annotation> qualifiers;

  public BeanManagerBackedConverterProvider(final BeanManager beanManager,
                                            final Set<Annotation> qualifiers) {
    super();
    this.beanManager = Objects.requireNonNull(beanManager);
    this.qualifiers = qualifiers;
  }

  @Override
  public <T> Converter<? extends T> getConverter(final TypeLiteral<T> typeLiteral) {
    @SuppressWarnings("unchecked")
    final Converter<? extends T> returnValue = (Converter<? extends T>)this.getConverter(typeLiteral.getType());
    return returnValue;
  }

  @Override
  public <T> Converter<? extends T> getConverter(final Class<T> type) {
    @SuppressWarnings("unchecked")
    final Converter<? extends T> returnValue = (Converter<? extends T>)this.getConverter((Type)type);
    return returnValue;
  }

  @Override
  public Converter<?> getConverter(final Type type) {
    final Converter<?> returnValue;
    Set<Bean<?>> beans;
    final Set<Annotation> qualifiers = this.qualifiers;
    if (qualifiers == null) {
      beans = this.beanManager.getBeans(type);
    } else {
      beans = this.beanManager.getBeans(type, qualifiers.toArray(new Annotation[qualifiers.size()]));
      if (beans == null || beans.isEmpty()) {
        beans = this.beanManager.getBeans(type);
      }
    }
    if (beans == null || beans.isEmpty()) {
      returnValue = null;
    } else {
      final Bean<?> bean = this.beanManager.resolve(beans);
      if (bean == null) {
        returnValue = null;
      } else {
        @SuppressWarnings("unchecked")
        final Converter<?> temp = (Converter<?>)this.beanManager.getReference(bean, type, this.beanManager.createCreationalContext(null));
        returnValue = temp;
      }
    }
    return returnValue;
  }

}
