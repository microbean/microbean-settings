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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

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
  public <T> Converter<? extends T> getConverter(final TypeLiteral<T> type) {
    @SuppressWarnings("unchecked")
    final Converter<? extends T> returnValue = (Converter<? extends T>)this.getConverter(type.getType());
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
    final Type converterType = new ParameterizedTypeImpl(Converter.class, type);
    Set<Bean<?>> beans;
    final Set<Annotation> qualifiers = this.qualifiers;
    if (qualifiers == null) {
      beans = this.beanManager.getBeans(converterType);
    } else {
      beans = this.beanManager.getBeans(converterType, qualifiers.toArray(new Annotation[qualifiers.size()]));
      if (beans == null || beans.isEmpty()) {
        beans = this.beanManager.getBeans(converterType);
      }
    }
    final Bean<?> bean;
    if (beans == null || beans.isEmpty()) {
      bean = null;
    } else {
      bean = this.beanManager.resolve(beans);
    }
    if (bean == null) {
      returnValue = null;
    } else {
      @SuppressWarnings("unchecked")
      final Converter<?> temp = (Converter<?>)this.beanManager.getReference(bean, converterType, this.beanManager.createCreationalContext(null));
      returnValue = temp;
    }
    return returnValue;
  }

  private static final class ParameterizedTypeImpl implements ParameterizedType {

    private final Type ownerType;

    private final Type rawType;

    private final Type[] actualTypeArguments;

    private final int hashCode;

    private ParameterizedTypeImpl(final Class<?> rawType, final Type actualTypeArgument) {
      this(null, rawType, new Type[] { actualTypeArgument });
    }
    
    private ParameterizedTypeImpl(final Class<?> rawType, final Type[] actualTypeArguments) {
      this(null, rawType, actualTypeArguments);
    }
    
    private ParameterizedTypeImpl(final Type ownerType, final Class<?> rawType, final Type[] actualTypeArguments) {
      super();
      this.ownerType = ownerType;
      this.rawType = Objects.requireNonNull(rawType);
      this.actualTypeArguments = actualTypeArguments;
      this.hashCode = this.computeHashCode();
    }
    
    @Override
    public final Type getOwnerType() {
      return this.ownerType;
    }
    
    @Override
    public final Type getRawType() {
      return this.rawType;
    }
    
    @Override
    public final Type[] getActualTypeArguments() {
      return this.actualTypeArguments;
    }
    
    @Override
    public final int hashCode() {
      return this.hashCode;
    }

    private final int computeHashCode() {
      int hashCode = 17;
      
      final Object ownerType = this.getOwnerType();
      int c = ownerType == null ? 0 : ownerType.hashCode();
      hashCode = 37 * hashCode + c;
      
      final Object rawType = this.getRawType();
      c = rawType == null ? 0 : rawType.hashCode();
      hashCode = 37 * hashCode + c;
      
      final Type[] actualTypeArguments = this.getActualTypeArguments();
      c = Arrays.hashCode(actualTypeArguments);
      hashCode = 37 * hashCode + c;
      
      return hashCode;
    }
    
    @Override
    public final boolean equals(final Object other) {
      if (other == this) {
        return true;
      } else if (other instanceof ParameterizedType) {
        final ParameterizedType her = (ParameterizedType)other;
        
        final Object ownerType = this.getOwnerType();
        if (ownerType == null) {
          if (her.getOwnerType() != null) {
            return false;
          }
        } else if (!ownerType.equals(her.getOwnerType())) {
          return false;
        }
        
        final Object rawType = this.getRawType();
        if (rawType == null) {
          if (her.getRawType() != null) {
            return false;
          }
        } else if (!rawType.equals(her.getRawType())) {
          return false;
        }
        
        final Type[] actualTypeArguments = this.getActualTypeArguments();
        if (!Arrays.equals(actualTypeArguments, her.getActualTypeArguments())) {
          return false;
        }
        
        return true;
      } else {
        return false;
      }
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder();
      final Type rawType = this.getRawType();
      final Type ownerType = this.getOwnerType();
      if (ownerType == null) {
        sb.append(rawType.getTypeName());
      } else {
        sb.append(ownerType.getTypeName()).append("$");
        if (ownerType instanceof ParameterizedType) {
          final ParameterizedType ownerPType = (ParameterizedType)ownerType;
          final Type ownerRawType = ownerPType.getRawType();
          sb.append(rawType.getTypeName().replace(ownerRawType.getTypeName() + "$", ""));
        } else if (rawType instanceof Class) {
          sb.append(((Class<?>)rawType).getSimpleName());
        } else {
          sb.append(rawType.getTypeName());
        }
      }

      final Type[] actualTypeArguments = this.getActualTypeArguments();
      if (actualTypeArguments != null && actualTypeArguments.length > 0) {
        final StringJoiner stringJoiner = new StringJoiner(", ", "<", ">");
        stringJoiner.setEmptyValue("");
        for (final Type actualTypeArgument : actualTypeArguments) {
          stringJoiner.add(actualTypeArgument.getTypeName());
        }
        sb.append(stringJoiner.toString());
      }
      return sb.toString();
    }
    
  }

}
