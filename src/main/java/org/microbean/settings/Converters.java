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

import java.math.BigDecimal;
import java.math.BigInteger;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import java.net.URI;
import java.net.URL;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;

import java.util.logging.Level;

import java.util.regex.Pattern;

import javax.enterprise.util.TypeLiteral;

import org.microbean.settings.converter.*;

/**
 * A hub for the centralized conversion of {@link Value}s.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 */
public final class Converters implements ConverterProvider {


  /*
   * Static fields.
   */

  
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


  /*
   * Instance fields.
   */

  
  private final Map<Type, Converter<?>> converters;


  /*
   * Constructors.
   */

  
  /**
   * Creates a new {@link Converters} instance.
   */
  public Converters() {
    super();
    this.converters = new ConcurrentHashMap<>();
    installDefaultConverters(this);
  }

  /**
   * Creates a new {@link Converters} instance.
   *
   * @param converters a {@link Collection} of {@link Converter}s that
   * will be {@linkplain #putConverter(TypeLiteral, Converter)
   * installed} into this {@link Converters} instance; may be {@code
   * null}
   *
   * @exception java.util.ConcurrentModificationException if some
   * other thread modifies the supplied {@link Collection} while
   * iteration is in progress
   */
  public Converters(final Collection<? extends Converter<?>> converters) {
    super();
    this.converters = new ConcurrentHashMap<>();
    if (converters != null && !converters.isEmpty()) {
      for (final Converter<?> converter : converters) {
        if (converter != null) {
          final Type conversionType = getConversionType(converter);
          assert conversionType != null;
          this.putConverter(conversionType, converter);
        }
      }
    }

  }


  /*
   * Instance methods.
   */

  
  /**
   * Returns a {@link Converter} capable of {@linkplain
   * Converter#convert(Value) converting} {@link Value}s into objects
   * of the supplied {@code type}.
   *
   * @param <T> the conversion type
   *
   * @param type a {@link TypeLiteral} describing the conversion type;
   * must not be {@code null}
   *
   * @return a non-{@code null} {@link Converter} capable of
   * {@linkplain Converter#convert(Value) converting} {@link Value}s
   * into objects of the proper type
   *
   * @exception NullPointerException if {@code type} is {@code null}
   *
   * @exception IllegalArgumentException if no {@link Converter} is
   * available for the supplied {@code type}
   *
   * @idempotency Because {@link Converter}s can be {@linkplain
   * #putConverter(TypeLiteral, Converter) added to this
   * <code>Converters</code> instance}, this method may return
   * different {@link Converter} instances over time given the same
   * {@code type}.
   *
   * @nullability This method never returns {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  @Override
  public final <T> Converter<? extends T> getConverter(final TypeLiteral<T> type) {
    @SuppressWarnings("unchecked")
    final Converter<? extends T> returnValue = (Converter<? extends T>)this.getConverter(type.getType());
    return returnValue;
  }

  /**
   * Returns a {@link Converter} capable of {@linkplain
   * Converter#convert(Value) converting} {@link Value}s into objects
   * of the supplied {@code type}.
   *
   * @param <T> the conversion type
   *
   * @param type a {@link Class} describing the conversion type; must
   * not be {@code null}
   *
   * @return a non-{@code null} {@link Converter} capable of
   * {@linkplain Converter#convert(Value) converting} {@link Value}s
   * into objects of the proper type
   *
   * @exception NullPointerException if {@code type} is {@code null}
   *
   * @exception IllegalArgumentException if no {@link Converter} is
   * available for the supplied {@code type}
   *
   * @idempotency Because {@link Converter}s can be {@linkplain
   * #putConverter(Class, Converter) added to this
   * <code>Converters</code> instance}, this method may return
   * different {@link Converter} instances over time given the same
   * {@code type}.
   *
   * @nullability This method never returns {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  @Override
  public final <T> Converter<? extends T> getConverter(final Class<T> type) {
    @SuppressWarnings("unchecked")
    final Converter<? extends T> returnValue = (Converter<? extends T>)this.getConverter((Type)Objects.requireNonNull(type));
    return returnValue;
  }

  /**
   * Returns a {@link Converter} capable of {@linkplain
   * Converter#convert(Value) converting} {@link Value}s into objects
   * of the supplied {@code type}.
   *
   * @param type a {@link Type} describing the conversion type; must
   * not be {@code null}
   *
   * @return a non-{@code null} {@link Converter} capable of
   * {@linkplain Converter#convert(Value) converting} {@link Value}s
   * into objects of the proper type
   *
   * @exception NullPointerException if {@code type} is {@code null}
   *
   * @exception IllegalArgumentException if no {@link Converter} is
   * available for the supplied {@code type}
   *
   * @idempotency Because {@link Converter}s can be {@linkplain
   * #putConverter(Class, Converter) added to this
   * <code>Converters</code> instance}, this method may return
   * different {@link Converter} instances over time given the same
   * {@code type}.
   *
   * @nullability This method never returns {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  @Override
  public final Converter<?> getConverter(final Type type) {
    final Converter<?> returnValue = this.converters.computeIfAbsent(Objects.requireNonNull(type), this::computeConverter);
    if (returnValue == null) {
      throw new IllegalArgumentException("No converter available for " + type);
    }
    return returnValue;
  }

  /**
   * Given a {@link Type}, creates and returns a new suitable {@link
   * Converter}.
   *
   * @param <T> the conversion type
   *
   * @param type the {@link Type} for which a new {@link Converter}
   * should be returned; must not be {@code null}
   *
   * @return a new {@link Converter}, or {@code null}
   *
   * @exception NullPointerException if {@code type} is {@code null}
   *
   * @exception IllegalArgumentException if {@code type} designates a
   * concrete {@link Collection} class without a zero-argument
   * constructor
   *
   * @idempotency Repeated invocations of this method will return new
   * but otherwise equal instances of the same kind of result given
   * the same parameter values.
   *
   * @nullability This method may return {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  private final <T> Converter<? extends T> computeConverter(final Type type) {
    Objects.requireNonNull(type);
    Converter<? extends T> returnValue;
    if (CharSequence.class.equals(type) ||
        String.class.equals(type) ||
        Serializable.class.equals(type) ||
        Object.class.equals(type)) {
      @SuppressWarnings("unchecked")
      final Converter<? extends T> temp = (Converter<? extends T>)new StringConverter();
      returnValue = temp;

    } else if (Boolean.class.equals(type) || boolean.class.equals(type)) {
      @SuppressWarnings("unchecked")
      final Converter<? extends T> temp = (Converter<? extends T>)new BooleanConverter();
      returnValue = temp;

    } else if (URI.class.equals(type)) {
      @SuppressWarnings("unchecked")
      final Converter<? extends T> temp = (Converter<? extends T>)new URIConverter();
      returnValue = temp;

    } else if (URL.class.equals(type)) {
      @SuppressWarnings("unchecked")
      final Converter<? extends T> temp = (Converter<? extends T>)new URLConverter();
      returnValue = temp;

    } else if (Class.class.equals(type)) {
      @SuppressWarnings("unchecked")
      final Converter<? extends T> temp = (Converter<? extends T>)new ClassConverter<T>();
      returnValue = temp;

    } else if (type instanceof ParameterizedType) {
      final ParameterizedType parameterizedType = (ParameterizedType)type;
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
              @SuppressWarnings("unchecked")
              final T returnValue = (T)Converters.this.convert(value, (Type)conversionClass); // XXX recursive call
              return returnValue;
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
        throw new IllegalArgumentException("Unhandled conversion type: " + type);
      }

    } else if (type instanceof Class) {
      final Class<?> conversionClass = (Class<?>)type;
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

  /**
   * Installs the supplied {@link Converter} under the supplied {@link
   * Class} and returns any {@link Converter} previously installed
   * under a {@link Type} equal to that {@link Class}.
   *
   * @param <T> the conversion type
   *
   * @param type the {@link Class} describing the conversion type of
   * the supplied {@link Converter}; must not be {@code null}
   *
   * @param converter the {@link Converter} to install; must not be
   * {@code null}
   *
   * @return the {@link Converter} previously installed under a {@link
   * Type} equal to the supplied {@link Class}, or {@code null}
   *
   * @exception NullPointerException if {@code type} or {@code
   * converter} is {@code null}
   *
   * @idempotency This method is not idempotent.
   *
   * @nullability This method may return {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #getConverter(Class)
   */
  public final <T> Converter<? extends T> putConverter(final Class<T> type, final Converter<? extends T> converter) {
    @SuppressWarnings("unchecked")
    final Converter<? extends T> returnValue = (Converter<? extends T>)this.putConverter((Type)type, converter);
    return returnValue;
  }

  /**
   * Installs the supplied {@link Converter} under the supplied {@link
   * TypeLiteral}'s {@link TypeLiteral#getType() Type} and returns any
   * {@link Converter} previously installed under a {@link Type} equal
   * to that {@link Type}.
   *
   * @param <T> the conversion type
   *
   * @param type the {@link TypeLiteral} describing the conversion
   * type of the supplied {@link Converter}; must not be {@code null}
   *
   * @param converter the {@link Converter} to install; must not be
   * {@code null}
   *
   * @return the {@link Converter} previously installed under a {@link
   * Type} equal to the supplied {@link TypeLiteral}'s {@link
   * TypeLiteral#getType() Type}, or {@code null}
   *
   * @exception NullPointerException if {@code type} or {@code
   * converter} is {@code null}
   *
   * @idempotency This method is not idempotent.
   *
   * @nullability This method may return {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #getConverter(TypeLiteral)
   */
  public final <T> Converter<? extends T> putConverter(final TypeLiteral<T> type, final Converter<? extends T> converter) {
    @SuppressWarnings("unchecked")
    final Converter<? extends T> returnValue = (Converter<? extends T>)this.putConverter(type.getType(), converter);
    return returnValue;
  }

  /**
   * Installs the supplied {@link Converter} under the supplied {@link
   * Type} and returns any {@link Converter} previously installed
   * under a {@link Type} equal to that {@link Type}.
   *
   * @param type the {@link Type} describing the conversion type of
   * the supplied {@link Converter}; must not be {@code null}
   *
   * @param converter the {@link Converter} to install; must not be
   * {@code null}
   *
   * @return the {@link Converter} previously installed under a {@link
   * Type} equal to the supplied {@link Type}, or {@code null}
   *
   * @exception NullPointerException if {@code type} or {@code
   * converter} is {@code null}
   *
   * @idempotency This method is not idempotent.
   *
   * @nullability This method may return {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see #getConverter(Type)
   */
  private final Converter<?> putConverter(final Type type, final Converter<?> converter) {
    return this.converters.put(Objects.requireNonNull(type), Objects.requireNonNull(converter));
  }

  /**
   * Uninstalls and returns any {@link Converter} stored under a key
   * equal to the supplied {@code key}.
   *
   * @param <T> the conversion type
   *
   * @param key the key designating the {@link Converter} to remove;
   * must not be {@code null}
   *
   * @return the {@link Converter} that was uninstalled, or {@code
   * null}
   *
   * @exception NullPointerException if {@code key} is {@code null}
   *
   * @idempotency This method will uninstall and return any {@link
   * Converter} stored under a key equal to the supplied {@code key},
   * and, if then invoked with the same {@code key}, will return
   * {@code null} thereafter.
   *
   * @nullability This method may return {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public final <T> Converter<? extends T> removeConverter(final Class<T> key) {
    @SuppressWarnings("unchecked")
    final Converter<? extends T> returnValue = (Converter<? extends T>)this.removeConverter((Object)key);
    return returnValue;
  }

  /**
   * Uninstalls and returns any {@link Converter} stored under a key
   * equal to the supplied {@code key}.
   *
   * @param <T> the conversion type
   *
   * @param key the key designating the {@link Converter} to remove;
   * must not be {@code null}
   *
   * @return the {@link Converter} that was uninstalled, or {@code
   * null}
   *
   * @exception NullPointerException if {@code key} is {@code null}
   *
   * @idempotency This method will uninstall and return any {@link
   * Converter} stored under a key equal to the supplied {@code key},
   * and, if then invoked with the same {@code key}, will return
   * {@code null} thereafter.
   *
   * @nullability This method may return {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public final <T> Converter<? extends T> removeConverter(final TypeLiteral<T> key) {
    @SuppressWarnings("unchecked")
    final Converter<? extends T> returnValue = (Converter<? extends T>)this.removeConverter((Object)key.getType());
    return returnValue;
  }

  /**
   * Uninstalls and returns any {@link Converter} stored under a key
   * equal to the supplied {@code key}.
   *
   * @param key the key designating the {@link Converter} to remove;
   * must not be {@code null}
   *
   * @return the {@link Converter} that was uninstalled, or {@code
   * null}
   *
   * @exception NullPointerException if {@code key} is {@code null}
   *
   * @idempotency This method will uninstall and return any {@link
   * Converter} stored under a key equal to the supplied {@code key},
   * and, if then invoked with the same {@code key}, will return
   * {@code null} thereafter.
   *
   * @nullability This method may return {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  private final Converter<?> removeConverter(final Object key) {
    return this.converters.remove(Objects.requireNonNull(key));
  }

  /**
   * Converts the supplied {@link Value} to an object of the proper
   * type and returns the result of the conversion using a {@link
   * Converter} {@linkplain #putConverter(Class, Converter) previously
   * installed} under a {@link Type} equal to the supplied {@link
   * Class}.
   *
   * @param <T> the conversion type
   *
   * @param value the {@link Value} to convert; may be {@code null}
   *
   * @param type the {@link Class} designating the {@link
   * Converter} to use; must not be {@code null}
   *
   * @return the result of the conversion, which may be {@code null}
   *
   * @exception NullPointerException if {@code type} is {@code null}
   *
   * @exception IllegalArgumentException if conversion fails because
   * of a problem with the supplied {@link Value}
   *
   * @exception ConversionException if conversion fails for any other
   * reason
   *
   * @idempotency This method may return different results when
   * supplied with the same parameter values over repeated
   * invocations.
   *
   * @nullability This method may return {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public final <T> T convert(final Value value, final Class<T> type) {
    @SuppressWarnings("unchecked")
    final T returnValue = (T)this.convert(value, (Type)type);
    return returnValue;
  }

  /**
   * Converts the supplied {@link Value} to an object of the proper
   * type and returns the result of the conversion using a {@link
   * Converter} {@linkplain #putConverter(Class, Converter) previously
   * installed} under a {@link Type} equal to the supplied {@link
   * TypeLiteral}'s {@link TypeLiteral#getType() Type}.
   *
   * @param <T> the conversion type
   *
   * @param value the {@link Value} to convert; may be {@code null}
   *
   * @param type the {@link TypeLiteral} designating the {@link
   * Converter} to use; must not be {@code null}
   *
   * @return the result of the conversion, which may be {@code null}
   *
   * @exception NullPointerException if {@code type} is {@code null}
   *
   * @exception IllegalArgumentException if conversion fails because
   * of a problem with the supplied {@link Value}
   *
   * @exception ConversionException if conversion fails for any other
   * reason
   *
   * @idempotency This method may return different results when
   * supplied with the same parameter values over repeated
   * invocations.
   *
   * @nullability This method may return {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  public final <T> T convert(final Value value, final TypeLiteral<T> type) {
    @SuppressWarnings("unchecked")
    final T returnValue = (T)this.convert(value, type.getType());
    return returnValue;
  }

  /**
   * Converts the supplied {@link Value} to an object of the proper
   * type and returns the result of the conversion using a {@link
   * Converter} {@linkplain #putConverter(Class, Converter) previously
   * installed} under a {@link Type} equal to the supplied {@code
   * type}.
   *
   * @param value the {@link Value} to convert; may be {@code null}
   *
   * @param type the {@link Type} designating the {@link Converter} to
   * use; must not be {@code null}
   *
   * @return the result of the conversion, which may be {@code null}
   *
   * @exception NullPointerException if {@code type} is {@code null}
   *
   * @exception IllegalArgumentException if conversion fails because
   * of a problem with the supplied {@link Value}
   *
   * @exception ConversionException if conversion fails for any other
   * reason
   *
   * @idempotency This method may return different results when
   * supplied with the same parameter values over repeated
   * invocations.
   *
   * @nullability This method may return {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  private final Object convert(final Value value, final Type type) {
    return this.getConverter(type).convert(value);
  }


  /*
   * Static methods.
   */


  /**
   * Returns a new {@link ExecutableBasedConverter} adapting the
   * designated {@code static} method.
   *
   * @param <T> the conversion type
   *
   * @param methodHostClass the {@link Class} whose {@link
   * Class#getMethod(String, Class...)} method will be called; must
   * not be {@code null}
   *
   * @param methodName the name of the {@code static} method to supply
   * to a new {@link ExecutableBasedConverter}; must not be {@code
   * null}
   *
   * @param soleParameterType the {@link Class} of the single
   * parameter of the method in question; must not be {@code null}; an
   * invocation of {@link Class#isAssignableFrom(Class)} on {@link
   * CharSequence CharSequence.class} with {@code soleParameterType}
   * as its parameter value must return {@code true}
   *
   * @return a suitable {@link Converter}, or {@code null}
   *
   * @exception NullPointerException if either {@code
   * methodHostClass}, {@code methodName} or {@code soleParameterType}
   * is {@code null}
   *
   * @exception IllegalArgumentException if {@code methodHostClass}
   * designates an array-typed or primitive class
   *
   * @nullability This method may return {@code null}.
   *
   * @idempotency Repeated invocations of this method will return the
   * same result given the same parameter values.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
  private static final <T> Converter<T> getConverterFromStaticMethod(Class<?> methodHostClass,
                                                                     final String methodName,
                                                                     final Class<? extends CharSequence> soleParameterType) {
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

  /**
   * Returns a new {@link ExecutableBasedConverter} adapting the
   * designated {@link Constructor}.
   *
   * @param <T> the conversion type
   *
   * @param constructorHostClass the {@link Class} whose {@link
   * Class#getConstructor(Class...)} method will be called; must not
   * be {@code null}
   *
   * @param soleParameterType the {@link Class} of the single
   * parameter of the {@link Constructor} in question; must not be
   * {@code null}; an invocation of {@link
   * Class#isAssignableFrom(Class)} on {@link CharSequence
   * CharSequence.class} with {@code soleParameterType} as its
   * parameter value must return {@code true}
   *
   * @return a suitable {@link Converter}, or {@code null}
   *
   * @exception NullPointerException if either {@code
   * constructorHostClass} or {@code soleParameterType} is {@code
   * null}
   *
   * @exception IllegalArgumentException if {@code
   * constructorHostClass} designates an array-typed or primitive
   * class
   *
   * @nullability This method may return {@code null}.
   *
   * @idempotency Repeated invocations of this method will return the
   * same result given the same parameter values.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   */
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

  private static final Type getConversionType(final Converter<?> converter) {
    final Type returnValue = getConversionType(Objects.requireNonNull(converter).getClass(), null, null);
    assert returnValue != null;
    return returnValue;
  }

  // can return null during recursive invocations only
  private static final Type getConversionType(final Type type, Set<Type> seen, Map<TypeVariable<?>, Type> reifiedTypes) {
    Type returnValue = null;
    Objects.requireNonNull(type);
    if (seen == null) {
      seen = new HashSet<>();
    }
    if (!seen.contains(type)) {
      seen.add(type);

      if (reifiedTypes == null) {
        reifiedTypes = new HashMap<>();
      }

      if (type instanceof Class) {
        final Class<?> c = (Class<?>)type;
        if (Converter.class.isAssignableFrom(c)) {
          final Type[] genericInterfaces = c.getGenericInterfaces();
          assert genericInterfaces != null;
          for (final Type genericInterface : genericInterfaces) {
            returnValue = getConversionType(genericInterface, seen, reifiedTypes); // XXX recursive call
            if (returnValue != null) {
              break;
            }
          }
          if (returnValue == null) {
            returnValue = getConversionType(c.getSuperclass(), seen, reifiedTypes); // XXX recursive call
          }
        }

      } else if (type instanceof ParameterizedType) {
        final ParameterizedType pt = (ParameterizedType)type;
        final Type rawType = pt.getRawType();
        assert rawType instanceof GenericDeclaration;
        final TypeVariable<?>[] typeParameters = ((GenericDeclaration)rawType).getTypeParameters();
        assert typeParameters != null;
        final Type[] actualTypeArguments = pt.getActualTypeArguments();
        assert actualTypeArguments != null;
        assert actualTypeArguments.length == typeParameters.length;
        if (actualTypeArguments.length > 0) {
          for (int i = 0; i < actualTypeArguments.length; i++) {
            Type actualTypeArgument = actualTypeArguments[i];
            if (actualTypeArgument instanceof Class ||
                actualTypeArgument instanceof ParameterizedType) {
              reifiedTypes.put(typeParameters[i], actualTypeArgument);
            } else if (actualTypeArgument instanceof TypeVariable) {
              final Type reifiedType = reifiedTypes.get((TypeVariable)actualTypeArgument);
              if (reifiedType == null) {
                reifiedTypes.put((TypeVariable)actualTypeArgument, Object.class);
                actualTypeArgument = Object.class;
              } else {
                actualTypeArgument = reifiedType;
              }
              assert actualTypeArgument != null;
              assert (actualTypeArgument instanceof ParameterizedType || actualTypeArgument instanceof Class) : "Unexpected actualTypeArgument: " + actualTypeArgument;
              reifiedTypes.put(typeParameters[i], actualTypeArgument);
            }
          }
        }
        if (Converter.class.equals(rawType)) {
          assert actualTypeArguments.length == 1;
          final Type typeArgument = actualTypeArguments[0];
          if (typeArgument instanceof Class ||
              typeArgument instanceof ParameterizedType) {
            returnValue = typeArgument;
          } else if (typeArgument instanceof TypeVariable) {
            final TypeVariable<?> typeVariable = (TypeVariable<?>)typeArgument;
            returnValue = reifiedTypes.get(typeVariable);
            assert returnValue instanceof ParameterizedType || returnValue instanceof Class : "Unexpected returnValue: " + returnValue;
          } else {
            throw new IllegalArgumentException("Unhandled conversion type: " + typeArgument);
          }
        } else {
          returnValue = getConversionType(rawType, seen, reifiedTypes); // XXX recursive call
        }

      } else {
        throw new IllegalArgumentException("Unhandled type: " + type);
      }

    }
    return returnValue;
  }

  private static final void installDefaultConverters(final Converters converters) {
    final StringConverter stringConverter = new StringConverter();
    converters.putConverter(BigDecimal.class, new BigDecimalConverter());
    converters.putConverter(BigInteger.class, new BigIntegerConverter());
    converters.putConverter(Boolean.class, new BooleanConverter());
    converters.putConverter(Byte.class, new ByteConverter());
    converters.putConverter(Calendar.class, new CalendarConverter());
    converters.putConverter(CharSequence.class, stringConverter);
    converters.putConverter(Date.class, new DateConverter());
    converters.putConverter(Double.class, new DoubleConverter());
    converters.putConverter(Duration.class, new DurationConverter());
    converters.putConverter(Float.class, new FloatConverter());
    converters.putConverter(Instant.class, new InstantConverter());
    converters.putConverter(Integer.class, new IntegerConverter());
    converters.putConverter(Level.class, new LevelConverter());
    converters.putConverter(LocalDate.class, new LocalDateConverter());
    converters.putConverter(LocalDateTime.class, new LocalDateTimeConverter());
    converters.putConverter(LocalTime.class, new LocalTimeConverter());
    converters.putConverter(Long.class, new LongConverter());
    converters.putConverter(MonthDay.class, new MonthDayConverter());
    converters.putConverter(Object.class, stringConverter);
    converters.putConverter(OffsetDateTime.class, new OffsetDateTimeConverter());
    converters.putConverter(OffsetTime.class, new OffsetTimeConverter());
    converters.putConverter(Period.class, new PeriodConverter());
    converters.putConverter(Short.class, new ShortConverter());
    converters.putConverter(String.class, stringConverter);
    converters.putConverter(StringBuffer.class, new StringBufferConverter());
    converters.putConverter(StringBuilder.class, new StringBuilderConverter());
    converters.putConverter(URI.class, new URIConverter());
    converters.putConverter(URL.class, new URLConverter());
    converters.putConverter(Year.class, new YearConverter());
    converters.putConverter(YearMonth.class, new YearMonthConverter());
    converters.putConverter(ZonedDateTime.class, new ZonedDateTimeConverter());
  }

}
