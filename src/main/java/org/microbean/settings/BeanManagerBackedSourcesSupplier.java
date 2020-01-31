/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2019–2020 microBean™.
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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import java.util.function.BiFunction;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

public class BeanManagerBackedSourcesSupplier implements BiFunction<String, Set<Annotation>, Set<? extends Source>> {

  private final BeanManager beanManager;
  
  public BeanManagerBackedSourcesSupplier(final BeanManager beanManager) {
    super();
    this.beanManager = Objects.requireNonNull(beanManager);
  }

  @Override
  public Set<? extends Source> apply(final String name, final Set<Annotation> qualifiers) {
    Set<Bean<?>> beans;
    if (qualifiers == null) {
      beans = this.beanManager.getBeans(Source.class);
    } else {
      beans = this.beanManager.getBeans(Source.class, qualifiers.toArray(new Annotation[qualifiers.size()]));
      if (beans == null || beans.isEmpty()) {
        beans = this.beanManager.getBeans(Source.class);
      }
    }
    final Set<? extends Source> returnValue;
    if (beans == null || beans.isEmpty()) {
      returnValue = Collections.emptySet();
    } else {
      final Set<Source> sources = new LinkedHashSet<>();
      for (final Bean<?> bean : beans) {
        @SuppressWarnings("unchecked")
        final Source source = (Source)this.beanManager.getReference(bean, Source.class, this.beanManager.createCreationalContext(null));
        if (source != null) {
          sources.add(source);
        }
      }
      returnValue = sources;
    }
    return returnValue;
  }
  
}
