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

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;

import java.io.Serializable;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.net.URI;
import java.net.URL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;

import java.util.regex.Pattern;

import javax.enterprise.util.TypeLiteral;

public class Converters {

  private static final Map<Class<?>, Class<?>> wrapperClasses;

  static {
    wrapperClasses = new HashMap<>();
    wrapperClasses.put(boolean.class, Boolean.class);
    wrapperClasses.put(byte.class, Byte.class);
    wrapperClasses.put(char.class, Character.class);
    wrapperClasses.put(double.class, Double.class);
    wrapperClasses.put(float.class, Float.class);
    wrapperClasses.put(int.class, Integer.class);
    wrapperClasses.put(long.class, Long.class);
    wrapperClasses.put(short.class, Short.class);
  }

  private static final Pattern backslashCommaPattern = Pattern.compile("\\\\,");

  private static final Pattern splitPattern = Pattern.compile("(?<!\\\\),");
  
  private final Map<Type, Converter<?>> converters;
  
  public Converters() {
    super();
    this.converters = new ConcurrentHashMap<>();
  }

  public final <T> Converter<T> getConverter(final TypeLiteral<T> type) {
    return this.getConverter(type.getType());
  }

  public final <T> Converter<T> getConverter(final Class<T> type) {
    return this.getConverter((Type)type);
  }

  private final <T> Converter<T> getConverter(final Type type) {
    Objects.requireNonNull(type);
    @SuppressWarnings("unchecked")
    final Converter<T> converter = (Converter<T>)this.converters.computeIfAbsent(type, this::computeConverter);
    if (converter == null) {
      throw new IllegalArgumentException("No converter available for " + type);
    }
    return converter;
  }

  public final <T> Converter<T> putConverter(final Class<T> cls, final Converter<T> converter) {
    final Converter<T> returnValue;
    if (cls != null) {
      @SuppressWarnings("unchecked")
      final Converter<T> temp = (Converter<T>)this.putConverter((Type)cls, converter);
      returnValue = temp;
    } else {
      returnValue = null;
    }
    return returnValue;
  }

  public final <T> Converter<T> putConverter(final TypeLiteral<T> typeLiteral, final Converter<T> converter) {
    final Converter<T> returnValue;
    if (typeLiteral != null) {
      @SuppressWarnings("unchecked")
      final Converter<T> temp = (Converter<T>)this.putConverter(typeLiteral.getType(), converter);
      returnValue = temp;
    } else {
      returnValue = null;
    }
    return returnValue;
  }

  private final Converter<?> putConverter(final Type type, final Converter<?> converter) {
    final Converter<?> returnValue;
    if (type != null) {
      returnValue = this.converters.put(type, converter);
    } else {
      returnValue = null;
    }
    return returnValue;
  }

  public final <T> Converter<T> removeConverter(final Class<T> key) {
    return this.removeConverter((Object)key);
  }

  public final <T> Converter<T> removeConverter(final TypeLiteral<T> key) {
    return this.removeConverter((Object)key.getType());
  }

  private final <T> Converter<T> removeConverter(final Object key) {
    @SuppressWarnings("unchecked")
    final Converter<T> returnValue = (Converter<T>)this.converters.remove(key);
    return returnValue;
  }

  public final <T> T convert(final Value value, final Class<T> cls) {
    return this.convert(value, (Type)cls);
  }

  public final <T> T convert(final Value value, final TypeLiteral<T> typeLiteral) {
    return this.convert(value, typeLiteral.getType());
  }
  
  private final <T> T convert(final Value value, final Type type) {
    Converter<T> converter = this.getConverter(type);
    if (converter == null) {
      throw new IllegalArgumentException("\"" + value + "\" could not be converted to " + (type == null ? "null" : type.getTypeName()));
    }
    final T returnValue = converter.convert(value);
    return returnValue;
  }


  /*
   * Static methods.
   */

  
  private static final <T> Converter<T> getConverterFromStaticMethod(Class<?> methodHostClass, final String methodName, final Class<? extends CharSequence> soleParameterType) {
    Objects.requireNonNull(methodHostClass);
    Objects.requireNonNull(methodName);
    Objects.requireNonNull(soleParameterType);
    if (methodHostClass.isArray()) {
      throw new IllegalArgumentException("methodHostClass.isArray(): " + methodHostClass.getName());
    } else if (methodHostClass.isPrimitive()) {
      throw new IllegalArgumentException("methodHostClass.isPrimitive(): " + methodHostClass.getName());
    }
    Converter<T> returnValue = null;
    final Method method;
    Method temp = null;
    try {
      temp = methodHostClass.getMethod(methodName, soleParameterType);
    } catch (final NoSuchMethodException noSuchMethodException) {

    } finally {
      method = temp;
    }
    if (method != null && Modifier.isStatic(method.getModifiers()) && methodHostClass.isAssignableFrom(method.getReturnType())) {
      returnValue = new ExecutableBasedConverter<>(method);
    }
    return returnValue;
  }

  private static final <T> Converter<T> getConverterFromConstructor(Class<T> constructorHostClass, final Class<? extends CharSequence> soleParameterType) {
    Objects.requireNonNull(constructorHostClass);
    Objects.requireNonNull(soleParameterType);
    if (constructorHostClass.isPrimitive()) {
      throw new IllegalArgumentException("constructorHostClass.isPrimitive(): " + constructorHostClass.getName());
    } else if (constructorHostClass.isArray()) {
      throw new IllegalArgumentException("constructorHostClass.isArray(): " + constructorHostClass.getName());
    }

    Converter<T> returnValue = null;
    final Constructor<T> constructor;
    Constructor<T> temp = null;
    try {
      temp = constructorHostClass.getConstructor(soleParameterType);
    } catch (final NoSuchMethodException noSuchMethodException) {

    } finally {
      constructor = temp;
    }
    if (constructor != null) {
      returnValue = new ExecutableBasedConverter<>(constructor);
    }
    return returnValue;
  }

  private static final String[] split(final String text) {
    final String[] returnValue;
    if (text == null) {
      returnValue = new String[0];
    } else {
      returnValue = splitPattern.split(text);
      assert returnValue != null;
      for (int i = 0; i < returnValue.length; i++) {
        returnValue[i] = backslashCommaPattern.matcher(returnValue[i]).replaceAll(",");
      }
    }
    return returnValue;
  }


  /*
   * Inner and nested classes.
   */
  
  
  private final <T> Converter<T> computeConverter(final Type k) {
    Converter<T> returnValue;
    if (CharSequence.class.equals(k) ||
        String.class.equals(k) ||
        Serializable.class.equals(k) ||
        Object.class.equals(k)) {
      returnValue = new Converter<T>() {
          private static final long serialVersionUID = 1L;
          @Override
          public final T convert(final Value value) {
            final T returnValue;
            if (value == null) {
              returnValue = null;
            } else {
              final String stringValue = value.get();
              if (stringValue == null) {
                returnValue = null;
              } else {
                @SuppressWarnings("unchecked")
                  final T temp = (T)stringValue;
                returnValue = temp;
              }
            }
            return returnValue;
          }
        };

    } else if (Boolean.class.equals(k) || boolean.class.equals(k)) {
      @SuppressWarnings("unchecked")
      final Converter<T> temp = (Converter<T>)new BooleanConverter();
      returnValue = temp;

    } else if (URI.class.equals(k)) {
      @SuppressWarnings("unchecked")
      final Converter<T> temp = (Converter<T>)new URIConverter();
      returnValue = temp;

    } else if (URL.class.equals(k)) {
      @SuppressWarnings("unchecked")
      final Converter<T> temp = (Converter<T>)new URLConverter();
      returnValue = temp;

    } else if (Class.class.equals(k)) {
      @SuppressWarnings("unchecked")
      final Converter<T> temp = (Converter<T>)new ClassConverter<T>();
      returnValue = temp;

    } else if (k instanceof ParameterizedType) {
      final ParameterizedType parameterizedType = (ParameterizedType)k;
      final Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
      assert actualTypeArguments != null;
      assert actualTypeArguments.length > 0;
      final Type rawType = parameterizedType.getRawType();
      assert rawType instanceof Class : "!(parameterizedType.getRawType() instanceof Class): " + rawType;
      final Class<?> conversionClass = (Class<?>)rawType;
      assert !conversionClass.isArray();

      if (Optional.class.isAssignableFrom(conversionClass)) {
        assert actualTypeArguments.length == 1;
        final Type firstTypeArgument = actualTypeArguments[0];
        returnValue = new Converter<T>() {
            private static final long serialVersionUID = 1L;
            @Override
            @SuppressWarnings("unchecked")
            public final T convert(final Value value) {
              return (T)Optional.ofNullable(Converters.this.convert(value, firstTypeArgument)); // XXX recursive call
            }
          };

      } else if (Class.class.isAssignableFrom(conversionClass)) {
        returnValue = new Converter<T>() {
            private static final long serialVersionUID = 1L;
            @Override
            public final T convert(final Value value) {
              return Converters.this.convert(value, (Type)conversionClass); // XXX recursive call
            }
          };

      } else if (Collection.class.isAssignableFrom(conversionClass)) {
        returnValue = new Converter<T>() {
            private static final long serialVersionUID = 1L;
            @Override
            public final T convert(final Value value) {
              final T returnValue;
              if (value == null) {
                returnValue = null;
              } else {
                final String stringValue = value.get();
                if (stringValue == null) {
                  returnValue = null;
                } else {
                  Collection<Object> container = null;
                  if (conversionClass.isInterface()) {
                    if (Set.class.isAssignableFrom(conversionClass)) {
                      container = new HashSet<>();
                    } else {
                      container = new ArrayList<>();
                    }
                  } else {
                    try {
                      @SuppressWarnings("unchecked")
                        final Collection<Object> temp = (Collection<Object>)conversionClass.getDeclaredConstructor().newInstance();
                      container = temp;
                    } catch (final ReflectiveOperationException reflectiveOperationException) {
                      throw new IllegalArgumentException(stringValue, reflectiveOperationException);
                    }
                  }
                  assert container != null;
                  final Type firstTypeArgument = actualTypeArguments[0];
                  final String[] parts = split(stringValue);
                  assert parts != null;
                  assert parts.length > 0;
                  for (final String part : parts) {
                    final Object scalar = Converters.this.convert(new Value(value, part), firstTypeArgument); // XXX recursive call
                    container.add(scalar);
                  }
                  @SuppressWarnings("unchecked")
                    final T temp = (T)container;
                  returnValue = temp;
                }
              }
              return returnValue;
            }
          };
      } else {
        throw new IllegalArgumentException("Unhandled conversion type: " + k);
      }

    } else if (k instanceof Class) {
      final Class<?> conversionClass = (Class<?>)k;
      if (conversionClass.isArray()) {
        returnValue = new Converter<T>() {
            private static final long serialVersionUID = 1L;
            @Override
            public final T convert(final Value value) {
              final T returnValue;
              if (value == null) {
                returnValue = null;
              } else {
                final String stringValue = value.get();
                if (stringValue == null) {
                  returnValue = null;
                } else {
                  final String[] parts = split(stringValue);
                  assert parts != null;
                  @SuppressWarnings("unchecked")
                    final T container = (T)Array.newInstance(conversionClass.getComponentType(), parts.length);
                  for (int i = 0; i < parts.length; i++) {
                    final Object scalar = Converters.this.convert(new Value(value, parts[i]), conversionClass.getComponentType()); // XXX recursive call
                    Array.set(container, i, scalar);
                  }
                  returnValue = container;
                }
              }
              return returnValue;
            }
          };

      } else {
        final Class<?> cls;
        if (conversionClass.isPrimitive()) {
          cls = wrapperClasses.get(conversionClass);
          assert cls != null;
        } else {
          cls = conversionClass;
        }
        returnValue = getConverterFromStaticMethod(cls, "of", String.class);
        if (returnValue == null) {
          returnValue = getConverterFromStaticMethod(cls, "of", CharSequence.class);
          if (returnValue == null) {
            returnValue = getConverterFromStaticMethod(cls, "valueOf", String.class);
            if (returnValue == null) {
              returnValue = getConverterFromStaticMethod(cls, "valueOf", CharSequence.class);
              if (returnValue == null) {
                returnValue = getConverterFromStaticMethod(cls, "parse", String.class);
                if (returnValue == null) {
                  returnValue = getConverterFromStaticMethod(cls, "parse", CharSequence.class);
                  if (returnValue == null) {
                    @SuppressWarnings("unchecked")
                      final Class<T> temp = (Class<T>)cls;
                    returnValue = getConverterFromConstructor(temp, String.class);
                    if (returnValue == null) {
                      returnValue = getConverterFromConstructor(temp, CharSequence.class);
                      if (returnValue == null) {
                        final PropertyEditor editor = PropertyEditorManager.findEditor(cls);
                        if (editor != null) {
                          returnValue = new PropertyEditorConverter<T>(temp, editor);
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    } else {
      returnValue = null;
    }
    return returnValue;
  }
  
}
