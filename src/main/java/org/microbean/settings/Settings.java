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

import java.beans.BeanInfo;
import java.beans.FeatureDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;

import java.lang.annotation.Annotation;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import javax.el.ELContext;
import javax.el.ELException; // for javadoc only
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.PropertyNotFoundException;
import javax.el.StandardELContext;
import javax.el.ValueExpression;

import javax.enterprise.inject.Typed;

import javax.enterprise.util.TypeLiteral;

import org.microbean.development.annotation.Experimental;

import org.microbean.settings.converter.PropertyEditorConverter;

/**
 * A provider of named setting values sourced from any number of
 * {@linkplain Source sources}.
 *
 * <p>Please see the <a
 * href="{@docRoot}/overview-summary.html#overview.description">overview</a>
 * for necessary context.</p>
 *
 * <h1>Class Organization</h1>
 *
 * <p>The bulk of the methods that belong to the {@link Settings}
 * class can be placed into two categories:</p>
 *
 * <ol>
 *
 * <li><strong>Value acquisition methods.</strong> These methods, such
 * as the canonical {@link #get(String, Set, Converter, BiFunction)}
 * method and its convenience forms, such as, simply, {@link
 * #get(String)}, acquire values from {@link Source}s, {@linkplain
 * #arbitrate(Set, String, Set, Collection) resolve ambiguities},
 * {@linkplain Converter#convert(Value) perform type conversion} and
 * so on.</li>
 *
 * <li><strong>Configuration methods.</strong> These methods, such as
 * the canonical {@link #configure(Object, Iterable, String, Set)}
 * method and its convenience forms, such as, simply, {@link
 * #configure(Object)}, fully configure Java Beans by using the value
 * acquisition methods above in conjunction with {@link
 * PropertyDescriptor} features from the Java Beans
 * specification.</li>
 *
 * </ol>
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @threadsafety Instances of this class are safe for concurrent use
 * by multiple threads.
 */
@Typed({ Settings.class })
public class Settings extends Source {


  /*
   * Static fields.
   */


  private static final Comparator<Value> valueComparator = Comparator.<Value>comparingInt(v -> v.getQualifiers().size()).reversed();

  /**
   * A convenient {@link BiFunction} suitable for use as a default
   * value function normally provided to the {@link #get(String, Set,
   * Type, BiFunction)} method and its ilk that returns {@code null}
   * when invoked.
   */
  public static final BiFunction<? super String,
                                 ? super Set<? extends Annotation>,
                                 ? extends String> NULL = (name, qualifiers) -> null;

  /**
   * A convenient {@link BiFunction} suitable for use as a default
   * value function normally provided to the {@link #get(String, Set,
   * Type, BiFunction)} method and its ilk that returns an empty
   * {@link String} when invoked.
   */
  public static final BiFunction<? super String,
                                 ? super Set<? extends Annotation>,
                                 ? extends String> EMPTY = (name, qualifiers) -> "";


  /*
   * Instance fields.
   */


  private final Set<Annotation> qualifiers;

  private final ConverterProvider converterProvider;

  private final BiFunction<? super String,
                           ? super Set<Annotation>,
                           ? extends Set<? extends Source>> sourcesFunction;

  private final Iterable<? extends Arbiter> arbiters;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link Settings}.
   *
   * <p>The created instance will use an empty {@link Set} of
   * qualifiers by default.</p>
   *
   * <p>The created instance will source its values from System
   * properties and environment variables, in that order.</p>
   *
   * <p>The created instance will use a new {@link Converters}
   * instance as its underlying source of {@link Converter}
   * instances.</p>
   *
   * <p>The created instance will use a single {@link
   * SourceOrderArbiter} as its mechanism for value arbitration.</p>
   *
   * @see #Settings(Set, BiFunction, ConverterProvider, Iterable)
   */
  public Settings() {
    super();

    this.qualifiers = Collections.emptySet();

    final Set<Source> sources = new LinkedHashSet<>();
    sources.add(new SystemPropertiesSource());
    sources.add(new EnvironmentVariablesSource());
    this.sourcesFunction = (name, qualifiers) -> Collections.unmodifiableSet(sources);

    this.converterProvider = new Converters();

    this.arbiters = Collections.singleton(new SourceOrderArbiter());
  }

  /**
   * Creates a new {@link Settings}.
   *
   * @param sourcesFunction a {@link BiFunction} that accepts a
   * setting name and a {@link Set} of {@linkplain Annotation
   * qualifier annotations} and returns a {@link Set} of {@link
   * Source}s appropriate for the request represented by its inputs;
   * may be {@code null}; may return {@code null}; if non-{@code null}
   * and this new {@link Settings} will be used concurrently by
   * multiple threads, then this parameter value must be safe for
   * concurrent use by multiple threads
   *
   * @param converterProvider a {@link ConverterProvider}; must not be
   * {@code null}; if this new {@link Settings} will be used
   * concurrently by multiple threads, then this parameter value must
   * be safe for concurrent use by multiple threads
   *
   * @param arbiters an {@link Iterable} of {@link Arbiter}s; may be
   * {@code null}; if this new {@link Settings} will be used
   * concurrently by multiple threads, then this parameter value must
   * be safe for concurrent use by multiple threads and {@link
   * Iterator}s produced by its {@link Iterable#iterator() iterator()}
   * method must also be safe for concurrent iteration by multiple
   * threads
   *
   * @exception NullPointerException if {@code converterProvider} is
   * {@code null}
   */
  public Settings(final BiFunction<? super String,
                                   ? super Set<Annotation>,
                                   ? extends Set<? extends Source>> sourcesFunction,
                  final ConverterProvider converterProvider,
                  final Iterable<? extends Arbiter> arbiters) {
    this(null, sourcesFunction, converterProvider, arbiters);
  }

  /**
   * Creates a new {@link Settings}.
   *
   * @param qualifiers a {@link Set} of {@linkplain Annotation
   * annotations} that can be used to further qualify the selection of
   * appropriate values; may be {@code null}; will be iterated over
   * with no synchronization or locking and shallowly copied by this
   * constructor
   *
   * @param sourcesFunction a {@link BiFunction} that accepts a
   * setting name and a {@link Set} of {@linkplain Annotation
   * qualifier annotations} and returns a {@link Set} of {@link
   * Source}s appropriate for the request represented by its inputs;
   * may be {@code null}; may return {@code null}; if non-{@code null}
   * and this new {@link Settings} will be used concurrently by
   * multiple threads, then this parameter value must be safe for
   * concurrent use by multiple threads
   *
   * @param converterProvider a {@link ConverterProvider}; must not be
   * {@code null}; if this new {@link Settings} will be used
   * concurrently by multiple threads, then this parameter value must
   * be safe for concurrent use by multiple threads
   *
   * @param arbiters an {@link Iterable} of {@link Arbiter}s; may be
   * {@code null}; if this new {@link Settings} will be used
   * concurrently by multiple threads, then this parameter value must
   * be safe for concurrent use by multiple threads and {@link
   * Iterator}s produced by its {@link Iterable#iterator() iterator()}
   * method must also be safe for concurrent iteration by multiple
   * threads
   *
   * @exception NullPointerException if {@code converterProvider} is
   * {@code null}
   */
  public Settings(final Set<Annotation> qualifiers,
                  final BiFunction<? super String,
                                   ? super Set<Annotation>,
                                   ? extends Set<? extends Source>> sourcesFunction,
                  final ConverterProvider converterProvider,
                  final Iterable<? extends Arbiter> arbiters) {
    super();
    if (qualifiers == null || qualifiers.isEmpty()) {
      this.qualifiers = Collections.emptySet();
    } else {
      this.qualifiers = Collections.unmodifiableSet(new LinkedHashSet<>(qualifiers));
    }
    if (sourcesFunction == null) {
      this.sourcesFunction = (name, qs) -> Collections.emptySet();
    } else {
      this.sourcesFunction = sourcesFunction;
    }
    this.converterProvider = Objects.requireNonNull(converterProvider);
    if (arbiters == null) {
      this.arbiters = Collections.emptySet();
    } else {
      this.arbiters = arbiters;
    }
  }


  /*
   * Instance methods.
   */


  /**
   * Returns a suitable {@link String} value for a setting named by
   * the supplied {@code name}.
   *
   * @param name the name of the setting for which a value is to be
   * returned; must not be {@code null}
   *
   * @return a suitable value (possibly {@code null})
   *
   * @exception NullPointerException if {@code name} is {@code null}
   *
   * @exception NoSuchElementException if no value could be sourced
   *
   * @exception ValueAcquisitionException if there was a procedural
   * problem acquiring a value
   *
   * @exception ArbitrationException if there was a problem performing
   * value arbitration
   *
   * @exception AmbiguousValuesException if arbitration completed but
   * could not resolve an ambiguity between potential return values
   *
   * @exception MalformedValuesException if the {@link
   * #handleMalformedValues(String, Set, Collection)} method was
   * overridden and the override throws a {@link
   * MalformedValuesException}
   *
   * @exception ELException if there was an error related to
   * expression language parsing or evaluation
   *
   * @threadsafety This method is and its overrides must be safe for
   * concurrent use by multiple threads.
   *
   * @idempotency No guarantees of any kind are made with respect to
   * the idempotency of this method or its overrides.
   *
   * @nullability This method may return {@code null}.
   *
   * @see #get(String, Set, Converter, BiFunction)
   */
  public final String get(final String name) {
    return this.get(name,
                    this.qualifiers,
                    this.converterProvider.getConverter(String.class),
                    null);
  }

  /**
   * Returns a suitable {@link String} value for a setting named by
   * the supplied {@code name} and with default value semantics
   * implemented by the optional supplied {@code
   * defaultValueFunction}.
   *
   * @param name the name of the setting for which a value is to be
   * returned; must not be {@code null}
   *
   * @param defaultValueFunction a {@link BiFunction} accepting a
   * setting name and a {@link Set} of qualifier {@link Annotation}s
   * that returns a default {@link String}-typed value when a value
   * could not sourced; may be {@code null} in which case if no value
   * can be sourced a {@link NoSuchElementException} will be thrown;
   * may return {@code null}; must be safe for concurrent use by
   * mulitple threads; must not call any of this {@link Settings}
   * instance's methods or undefined behavior will result
   *
   * @return a suitable value (possibly {@code null})
   *
   * @exception NullPointerException if {@code name} is {@code null}
   *
   * @exception NoSuchElementException if {@code defaultValueFunction}
   * was {@code null} and no value could be sourced
   *
   * @exception ValueAcquisitionException if there was a procedural
   * problem acquiring a value
   *
   * @exception ArbitrationException if there was a problem performing
   * value arbitration
   *
   * @exception AmbiguousValuesException if arbitration completed but
   * could not resolve an ambiguity between potential return values
   *
   * @exception MalformedValuesException if the {@link
   * #handleMalformedValues(String, Set, Collection)} method was
   * overridden and the override throws a {@link
   * MalformedValuesException}
   *
   * @exception ELException if there was an error related to
   * expression language parsing or evaluation
   *
   * @threadsafety This method is and its overrides must be safe for
   * concurrent use by multiple threads.
   *
   * @idempotency No guarantees of any kind are made with respect to
   * the idempotency of this method or its overrides.
   *
   * @nullability This method may return {@code null}.
   *
   * @see #get(String, Set, Converter, BiFunction)
   */
  public final String get(final String name,
                          final BiFunction<? super String, ? super Set<? extends Annotation>, ? extends String> defaultValueFunction) {
    return this.get(name,
                    this.qualifiers,
                    this.converterProvider.getConverter(String.class),
                    defaultValueFunction);
  }

  /**
   * Returns a suitable {@link String} value for a setting named by
   * the supplied {@code name} and qualified with the supplied {@code
   * qualifiers}.
   *
   * @param name the name of the setting for which a value is to be
   * returned; must not be {@code null}
   *
   * @param qualifiers a {@link Set} of {@link Annotation}s to further
   * qualify the selection of the value to be returned; may be {@code
   * null}; if non-{@code null} then this parameter value must be safe
   * for concurrent iteration by multiple threads
   *
   * @return a suitable value (possibly {@code null})
   *
   * @exception NullPointerException if {@code name} is {@code null}
   *
   * @exception NoSuchElementException if no value could be sourced
   *
   * @exception ValueAcquisitionException if there was a procedural
   * problem acquiring a value
   *
   * @exception ArbitrationException if there was a problem performing
   * value arbitration
   *
   * @exception AmbiguousValuesException if arbitration completed but
   * could not resolve an ambiguity between potential return values
   *
   * @exception MalformedValuesException if the {@link
   * #handleMalformedValues(String, Set, Collection)} method was
   * overridden and the override throws a {@link
   * MalformedValuesException}
   *
   * @exception ELException if there was an error related to
   * expression language parsing or evaluation
   *
   * @threadsafety This method is and its overrides must be safe for
   * concurrent use by multiple threads.
   *
   * @idempotency No guarantees of any kind are made with respect to
   * the idempotency of this method or its overrides.
   *
   * @nullability This method may return {@code null}.
   *
   * @see #get(String, Set, Converter, BiFunction)
   */
  public final String get(final String name,
                          final Set<Annotation> qualifiers) {
    return this.get(name,
                    qualifiers,
                    this.converterProvider.getConverter(String.class),
                    null);
  }

  /**
   * Returns a suitable {@link String} value for a setting named by
   * the supplied {@code name} and qualified with the supplied {@code
   * qualifiers} and with default value semantics implemented by the
   * optional supplied {@code defaultValueFunction}.
   *
   * @param name the name of the setting for which a value is to be
   * returned; must not be {@code null}
   *
   * @param qualifiers a {@link Set} of {@link Annotation}s to further
   * qualify the selection of the value to be returned; may be {@code
   * null}; if non-{@code null} then this parameter value must be safe
   * for concurrent iteration by multiple threads
   *
   * @param defaultValueFunction a {@link BiFunction} accepting a
   * setting name and a {@link Set} of qualifier {@link Annotation}s
   * that returns a default {@link String}-typed value when a value
   * could not sourced; may be {@code null} in which case if no value
   * can be sourced a {@link NoSuchElementException} will be thrown;
   * may return {@code null}; must be safe for concurrent use by
   * mulitple threads; must not call any of this {@link Settings}
   * instance's methods or undefined behavior will result
   *
   * @return a suitable value (possibly {@code null})
   *
   * @exception NullPointerException if {@code name} is {@code null}
   *
   * @exception NoSuchElementException if {@code defaultValueFunction}
   * was {@code null} and no value could be sourced
   *
   * @exception ValueAcquisitionException if there was a procedural
   * problem acquiring a value
   *
   * @exception ArbitrationException if there was a problem performing
   * value arbitration
   *
   * @exception AmbiguousValuesException if arbitration completed but
   * could not resolve an ambiguity between potential return values
   *
   * @exception MalformedValuesException if the {@link
   * #handleMalformedValues(String, Set, Collection)} method was
   * overridden and the override throws a {@link
   * MalformedValuesException}
   *
   * @exception ELException if there was an error related to
   * expression language parsing or evaluation
   *
   * @threadsafety This method is and its overrides must be safe for
   * concurrent use by multiple threads.
   *
   * @idempotency No guarantees of any kind are made with respect to
   * the idempotency of this method or its overrides.
   *
   * @nullability This method may return {@code null}.
   *
   * @see #get(String, Set, Converter, BiFunction)
   */
  public final String get(final String name,
                          final Set<Annotation> qualifiers,
                          final BiFunction<? super String, ? super Set<? extends Annotation>, ? extends String> defaultValueFunction) {
    return this.get(name,
                    qualifiers,
                    this.converterProvider.getConverter(String.class),
                    defaultValueFunction);
  }

  //----------------------------------------------------------------------------

  /**
   * Returns a suitable value for a setting named by the supplied
   * {@code name} as {@linkplain Converter#convert(Value) converted}
   * by the {@link Converter} {@linkplain
   * ConverterProvider#getConverter(Class) located} using the supplied
   * {@link Class}.
   *
   * @param <T> the type to which any value should be {@linkplain
   * Converter#convert(Value) converted}
   *
   * @param name the name of the setting for which a value is to be
   * returned; must not be {@code null}
   *
   * @param type a {@link Class} used to {@linkplain
   * ConverterProvider#getConverter(Class) locate} an appropriate
   * {@link Converter}; must not be {@code null}
   *
   * @return a suitable value (possibly {@code null})
   *
   * @exception NullPointerException if either {@code name} or {@code
   * class} is {@code null}
   *
   * @exception IllegalArgumentException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason; see {@link Converter#convert(Value)}
   *
   * @exception ConversionException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason other than bad inputs
   *
   * @exception NoSuchElementException if no value could be sourced
   *
   * @exception ValueAcquisitionException if there was a procedural
   * problem acquiring a value
   *
   * @exception ArbitrationException if there was a problem performing
   * value arbitration
   *
   * @exception AmbiguousValuesException if arbitration completed but
   * could not resolve an ambiguity between potential return values
   *
   * @exception MalformedValuesException if the {@link
   * #handleMalformedValues(String, Set, Collection)} method was
   * overridden and the override throws a {@link
   * MalformedValuesException}
   *
   * @exception ELException if there was an error related to
   * expression language parsing or evaluation
   *
   * @threadsafety This method is and its overrides must be safe for
   * concurrent use by multiple threads.
   *
   * @idempotency No guarantees of any kind are made with respect to
   * the idempotency of this method or its overrides.
   *
   * @nullability This method may return {@code null}.
   *
   * @see #get(String, Set, Converter, BiFunction)
   */
  public final <T> T get(final String name,
                         final Class<T> type) {
    return this.get(name,
                    this.qualifiers,
                    this.converterProvider.getConverter(type),
                    null);
  }

  /**
   * Returns a suitable value for a setting named by the supplied
   * {@code name} as {@linkplain Converter#convert(Value) converted}
   * by the {@link Converter} {@linkplain
   * ConverterProvider#getConverter(Class) located} using the supplied
   * {@link Class}, and with default value semantics implemented by
   * the optional supplied {@code defaultValueFunction}.
   *
   * @param <T> the type to which any value should be {@linkplain
   * Converter#convert(Value) converted}
   *
   * @param name the name of the setting for which a value is to be
   * returned; must not be {@code null}
   *
   * @param type a {@link Class} used to {@linkplain
   * ConverterProvider#getConverter(Class) locate} an appropriate
   * {@link Converter}; must not be {@code null}
   *
   * @param defaultValueFunction a {@link BiFunction} accepting a
   * setting name and a {@link Set} of qualifier {@link Annotation}s
   * that returns a default {@link String}-typed value when a value
   * could not sourced; may be {@code null} in which case if no value
   * can be sourced a {@link NoSuchElementException} will be thrown;
   * may return {@code null}; must be safe for concurrent use by
   * mulitple threads; must not call any of this {@link Settings}
   * instance's methods or undefined behavior will result
   *
   * @return a suitable value (possibly {@code null})
   *
   * @exception NullPointerException if either {@code name} or {@code
   * class} is {@code null}
   *
   * @exception IllegalArgumentException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason; see {@link Converter#convert(Value)}
   *
   * @exception ConversionException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason other than bad inputs
   *
   * @exception NoSuchElementException if {@code defaultValueFunction}
   * was {@code null} and no value could be sourced
   *
   * @exception ValueAcquisitionException if there was a procedural
   * problem acquiring a value
   *
   * @exception ArbitrationException if there was a problem performing
   * value arbitration
   *
   * @exception AmbiguousValuesException if arbitration completed but
   * could not resolve an ambiguity between potential return values
   *
   * @exception MalformedValuesException if the {@link
   * #handleMalformedValues(String, Set, Collection)} method was
   * overridden and the override throws a {@link
   * MalformedValuesException}
   *
   * @exception ELException if there was an error related to
   * expression language parsing or evaluation
   *
   * @threadsafety This method is and its overrides must be safe for
   * concurrent use by multiple threads.
   *
   * @idempotency No guarantees of any kind are made with respect to
   * the idempotency of this method or its overrides.
   *
   * @nullability This method may return {@code null}.
   *
   * @see #get(String, Set, Converter, BiFunction)
   */
  public final <T> T get(final String name,
                         final Class<T> type,
                         final BiFunction<? super String, ? super Set<? extends Annotation>, ? extends String> defaultValueFunction) {
    return this.get(name,
                    this.qualifiers,
                    this.converterProvider.getConverter(type),
                    defaultValueFunction);
  }

  /**
   * Returns a suitable value for a setting named by the supplied
   * {@code name} and qualified with the supplied {@code qualifiers},
   * as {@linkplain Converter#convert(Value) converted} by the {@link
   * Converter} {@linkplain ConverterProvider#getConverter(Class)
   * located} using the supplied {@link Class}, and with default value
   * semantics implemented by the optional supplied {@code
   * defaultValueFunction}.
   *
   * @param <T> the type to which any value should be {@linkplain
   * Converter#convert(Value) converted}
   *
   * @param name the name of the setting for which a value is to be
   * returned; must not be {@code null}
   *
   * @param qualifiers a {@link Set} of {@link Annotation}s to further
   * qualify the selection of the value to be returned; may be {@code
   * null}; if non-{@code null} then this parameter value must be safe
   * for concurrent iteration by multiple threads
   *
   * @param type a {@link Class} used to {@linkplain
   * ConverterProvider#getConverter(Class) locate} an appropriate
   * {@link Converter}; must not be {@code null}
   *
   * @param defaultValueFunction a {@link BiFunction} accepting a
   * setting name and a {@link Set} of qualifier {@link Annotation}s
   * that returns a default {@link String}-typed value when a value
   * could not sourced; may be {@code null} in which case if no value
   * can be sourced a {@link NoSuchElementException} will be thrown;
   * may return {@code null}; must be safe for concurrent use by
   * mulitple threads; must not call any of this {@link Settings}
   * instance's methods or undefined behavior will result
   *
   * @return a suitable value (possibly {@code null})
   *
   * @exception NullPointerException if either {@code name} or {@code
   * type} is {@code null}
   *
   * @exception IllegalArgumentException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason; see {@link Converter#convert(Value)}
   *
   * @exception ConversionException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason other than bad inputs
   *
   * @exception NoSuchElementException if {@code defaultValueFunction}
   * was {@code null} and no value could be sourced
   *
   * @exception ValueAcquisitionException if there was a procedural
   * problem acquiring a value
   *
   * @exception ArbitrationException if there was a problem performing
   * value arbitration
   *
   * @exception AmbiguousValuesException if arbitration completed but
   * could not resolve an ambiguity between potential return values
   *
   * @exception MalformedValuesException if the {@link
   * #handleMalformedValues(String, Set, Collection)} method was
   * overridden and the override throws a {@link
   * MalformedValuesException}
   *
   * @exception ELException if there was an error related to
   * expression language parsing or evaluation
   *
   * @threadsafety This method is and its overrides must be safe for
   * concurrent use by multiple threads.
   *
   * @idempotency No guarantees of any kind are made with respect to
   * the idempotency of this method or its overrides.
   *
   * @nullability This method may return {@code null}.
   *
   * @see #get(String, Set, Converter, BiFunction)
   */
  public final <T> T get(final String name,
                         final Set<Annotation> qualifiers,
                         final Class<T> type,
                         final BiFunction<? super String, ? super Set<? extends Annotation>, ? extends String> defaultValueFunction) {
    return this.get(name,
                    qualifiers,
                    this.converterProvider.getConverter(type),
                    defaultValueFunction);
  }

  //----------------------------------------------------------------------------

  /**
   * Returns a suitable value for a setting named by the supplied
   * {@code name} as {@linkplain Converter#convert(Value) converted}
   * by the {@link Converter} {@linkplain
   * ConverterProvider#getConverter(TypeLiteral) located} using the
   * supplied {@link TypeLiteral}.
   *
   * @param <T> the type to which any value should be {@linkplain
   * Converter#convert(Value) converted}
   *
   * @param name the name of the setting for which a value is to be
   * returned; must not be {@code null}
   *
   * @param typeLiteral a {@link TypeLiteral} used to {@linkplain
   * ConverterProvider#getConverter(TypeLiteral) locate} an
   * appropriate {@link Converter}; must not be {@code null}
   *
   * @return a suitable value (possibly {@code null})
   *
   * @exception NullPointerException if either {@code name} or {@code
   * typeLiteral} is {@code null}
   *
   * @exception IllegalArgumentException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason; see {@link Converter#convert(Value)}
   *
   * @exception ConversionException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason other than bad inputs
   *
   * @exception NoSuchElementException if no value could be sourced
   *
   * @exception ValueAcquisitionException if there was a procedural
   * problem acquiring a value
   *
   * @exception ArbitrationException if there was a problem performing
   * value arbitration
   *
   * @exception AmbiguousValuesException if arbitration completed but
   * could not resolve an ambiguity between potential return values
   *
   * @exception MalformedValuesException if the {@link
   * #handleMalformedValues(String, Set, Collection)} method was
   * overridden and the override throws a {@link
   * MalformedValuesException}
   *
   * @exception ELException if there was an error related to
   * expression language parsing or evaluation
   *
   * @threadsafety This method is and its overrides must be safe for
   * concurrent use by multiple threads.
   *
   * @idempotency No guarantees of any kind are made with respect to
   * the idempotency of this method or its overrides.
   *
   * @nullability This method may return {@code null}.
   *
   * @see #get(String, Set, Converter, BiFunction)
   */
  public final <T> T get(final String name,
                         final TypeLiteral<T> typeLiteral) {
    return this.get(name,
                    this.qualifiers,
                    this.converterProvider.getConverter(typeLiteral),
                    null);
  }

  /**
   * Returns a suitable value for a setting named by the supplied
   * {@code name} as {@linkplain Converter#convert(Value) converted}
   * by the {@link Converter} {@linkplain
   * ConverterProvider#getConverter(TypeLiteral) located} using the
   * supplied {@link TypeLiteral}, and with default value semantics
   * implemented by the optional supplied {@code
   * defaultValueFunction}.
   *
   * @param <T> the type to which any value should be {@linkplain
   * Converter#convert(Value) converted}
   *
   * @param name the name of the setting for which a value is to be
   * returned; must not be {@code null}
   *
   * @param typeLiteral a {@link TypeLiteral} used to {@linkplain
   * ConverterProvider#getConverter(TypeLiteral) locate} an
   * appropriate {@link Converter}; must not be {@code null}
   *
   * @param defaultValueFunction a {@link BiFunction} accepting a
   * setting name and a {@link Set} of qualifier {@link Annotation}s
   * that returns a default {@link String}-typed value when a value
   * could not sourced; may be {@code null} in which case if no value
   * can be sourced a {@link NoSuchElementException} will be thrown;
   * may return {@code null}; must be safe for concurrent use by
   * mulitple threads; must not call any of this {@link Settings}
   * instance's methods or undefined behavior will result
   *
   * @return a suitable value (possibly {@code null})
   *
   * @exception NullPointerException if either {@code name} or {@code
   * typeLiteral} is {@code null}
   *
   * @exception IllegalArgumentException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason; see {@link Converter#convert(Value)}
   *
   * @exception ConversionException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason other than bad inputs
   *
   * @exception NoSuchElementException if {@code defaultValueFunction}
   * was {@code null} and no value could be sourced
   *
   * @exception ValueAcquisitionException if there was a procedural
   * problem acquiring a value
   *
   * @exception ArbitrationException if there was a problem performing
   * value arbitration
   *
   * @exception AmbiguousValuesException if arbitration completed but
   * could not resolve an ambiguity between potential return values
   *
   * @exception MalformedValuesException if the {@link
   * #handleMalformedValues(String, Set, Collection)} method was
   * overridden and the override throws a {@link
   * MalformedValuesException}
   *
   * @exception ELException if there was an error related to
   * expression language parsing or evaluation
   *
   * @threadsafety This method is and its overrides must be safe for
   * concurrent use by multiple threads.
   *
   * @idempotency No guarantees of any kind are made with respect to
   * the idempotency of this method or its overrides.
   *
   * @nullability This method may return {@code null}.
   *
   * @see #get(String, Set, Converter, BiFunction)
   */
  public final <T> T get(final String name,
                         final TypeLiteral<T> typeLiteral,
                         final BiFunction<? super String, ? super Set<? extends Annotation>, ? extends String> defaultValueFunction) {
    return this.get(name,
                    this.qualifiers,
                    this.converterProvider.getConverter(typeLiteral),
                    defaultValueFunction);
  }

  /**
   * Returns a suitable value for a setting named by the supplied
   * {@code name} and qualified with the supplied {@code qualifiers},
   * as {@linkplain Converter#convert(Value) converted} by the {@link
   * Converter} {@linkplain
   * ConverterProvider#getConverter(TypeLiteral) located} using the
   * supplied {@link TypeLiteral}, and with default value semantics
   * implemented by the optional supplied {@code
   * defaultValueFunction}.
   *
   * @param <T> the type to which any value should be {@linkplain
   * Converter#convert(Value) converted}
   *
   * @param name the name of the setting for which a value is to be
   * returned; must not be {@code null}
   *
   * @param qualifiers a {@link Set} of {@link Annotation}s to further
   * qualify the selection of the value to be returned; may be {@code
   * null}; if non-{@code null} then this parameter value must be safe
   * for concurrent iteration by multiple threads
   *
   * @param typeLiteral a {@link TypeLiteral} used to {@linkplain
   * ConverterProvider#getConverter(TypeLiteral) locate} an
   * appropriate {@link Converter}; must not be {@code null}
   *
   * @param defaultValueFunction a {@link BiFunction} accepting a
   * setting name and a {@link Set} of qualifier {@link Annotation}s
   * that returns a default {@link String}-typed value when a value
   * could not sourced; may be {@code null} in which case if no value
   * can be sourced a {@link NoSuchElementException} will be thrown;
   * may return {@code null}; must be safe for concurrent use by
   * mulitple threads; must not call any of this {@link Settings}
   * instance's methods or undefined behavior will result
   *
   * @return a suitable value (possibly {@code null})
   *
   * @exception NullPointerException if either {@code name} or {@code
   * typeLiteral} is {@code null}
   *
   * @exception IllegalArgumentException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason; see {@link Converter#convert(Value)}
   *
   * @exception ConversionException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason other than bad inputs
   *
   * @exception NoSuchElementException if {@code defaultValueFunction}
   * was {@code null} and no value could be sourced
   *
   * @exception ValueAcquisitionException if there was a procedural
   * problem acquiring a value
   *
   * @exception ArbitrationException if there was a problem performing
   * value arbitration
   *
   * @exception AmbiguousValuesException if arbitration completed but
   * could not resolve an ambiguity between potential return values
   *
   * @exception MalformedValuesException if the {@link
   * #handleMalformedValues(String, Set, Collection)} method was
   * overridden and the override throws a {@link
   * MalformedValuesException}
   *
   * @exception ELException if there was an error related to
   * expression language parsing or evaluation
   *
   * @threadsafety This method is and its overrides must be safe for
   * concurrent use by multiple threads.
   *
   * @idempotency No guarantees of any kind are made with respect to
   * the idempotency of this method or its overrides.
   *
   * @nullability This method may return {@code null}.
   *
   * @see #get(String, Set, Converter, BiFunction)
   */
  public final <T> T get(final String name,
                         final Set<Annotation> qualifiers,
                         final TypeLiteral<T> typeLiteral,
                         final BiFunction<? super String, ? super Set<? extends Annotation>, ? extends String> defaultValueFunction) {
    return this.get(name,
                    qualifiers,
                    this.converterProvider.getConverter(typeLiteral),
                    defaultValueFunction);
  }

  //----------------------------------------------------------------------------

  /**
   * Returns a suitable value for a setting named by the supplied
   * {@code name} as {@linkplain Converter#convert(Value) converted}
   * by the {@link Converter} {@linkplain
   * ConverterProvider#getConverter(Type) located} using the supplied
   * {@link Type}.
   *
   * @param name the name of the setting for which a value is to be
   * returned; must not be {@code null}
   *
   * @param type a {@link Type} used to {@linkplain
   * ConverterProvider#getConverter(Type) locate} an appropriate
   * {@link Converter}; must not be {@code null}
   *
   * @return a suitable value (possibly {@code null})
   *
   * @exception NullPointerException if either {@code name} or {@code
   * type} is {@code null}
   *
   * @exception IllegalArgumentException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason; see {@link Converter#convert(Value)}
   *
   * @exception ConversionException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason other than bad inputs
   *
   * @exception NoSuchElementException if no value could be sourced
   *
   * @exception ValueAcquisitionException if there was a procedural
   * problem acquiring a value
   *
   * @exception ArbitrationException if there was a problem performing
   * value arbitration
   *
   * @exception AmbiguousValuesException if arbitration completed but
   * could not resolve an ambiguity between potential return values
   *
   * @exception MalformedValuesException if the {@link
   * #handleMalformedValues(String, Set, Collection)} method was
   * overridden and the override throws a {@link
   * MalformedValuesException}
   *
   * @exception ELException if there was an error related to
   * expression language parsing or evaluation
   *
   * @threadsafety This method is and its overrides must be safe for
   * concurrent use by multiple threads.
   *
   * @idempotency No guarantees of any kind are made with respect to
   * the idempotency of this method or its overrides.
   *
   * @nullability This method may return {@code null}.
   *
   * @see #get(String, Set, Converter, BiFunction)
   */
  public final Object get(final String name,
                          final Type type) {
    return this.get(name,
                    this.qualifiers,
                    this.converterProvider.getConverter(type),
                    null);
  }

  /**
   * Returns a suitable value for a setting named by the supplied
   * {@code name} and qualified with the supplied {@code qualifiers},
   * as {@linkplain Converter#convert(Value) converted} by the {@link
   * Converter} {@linkplain ConverterProvider#getConverter(Type)
   * located} using the supplied {@link Type}.
   *
   * @param name the name of the setting for which a value is to be
   * returned; must not be {@code null}
   *
   * @param qualifiers a {@link Set} of {@link Annotation}s to further
   * qualify the selection of the value to be returned; may be {@code
   * null}; if non-{@code null} then this parameter value must be safe
   * for concurrent iteration by multiple threads
   *
   * @param type a {@link Type} used to {@linkplain
   * ConverterProvider#getConverter(Type) locate} an appropriate
   * {@link Converter}; must not be {@code null}
   *
   * @return a suitable value (possibly {@code null})
   *
   * @exception NullPointerException if either {@code name} or {@code
   * type} is {@code null}
   *
   * @exception IllegalArgumentException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason; see {@link Converter#convert(Value)}
   *
   * @exception ConversionException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any reason
   * other than bad inputs
   *
   * @exception NoSuchElementException if no value could be sourced
   *
   * @exception ValueAcquisitionException if there was a procedural
   * problem acquiring a value
   *
   * @exception ArbitrationException if there was a problem performing
   * value arbitration
   *
   * @exception AmbiguousValuesException if arbitration completed but
   * could not resolve an ambiguity between potential return values
   *
   * @exception MalformedValuesException if the {@link
   * #handleMalformedValues(String, Set, Collection)} method was
   * overridden and the override throws a {@link
   * MalformedValuesException}
   *
   * @exception ELException if there was an error related to
   * expression language parsing or evaluation
   *
   * @threadsafety This method is and its overrides must be safe for
   * concurrent use by multiple threads.
   *
   * @idempotency No guarantees of any kind are made with respect to
   * the idempotency of this method or its overrides.
   *
   * @nullability This method may return {@code null}.
   *
   * @see #get(String, Set, Converter, BiFunction)
   */
  public final Object get(final String name,
                          final Set<Annotation> qualifiers,
                          final Type type) {
    return this.get(name,
                    qualifiers,
                    this.converterProvider.getConverter(type),
                    null);
  }

  /**
   * Returns a suitable value for a setting named by the supplied
   * {@code name} as {@linkplain Converter#convert(Value) converted}
   * by the {@link Converter} {@linkplain
   * ConverterProvider#getConverter(Type) located} using the supplied
   * {@link Type}, and with default value semantics implemented by the
   * optional supplied {@code defaultValueFunction}.
   *
   * @param name the name of the setting for which a value is to be
   * returned; must not be {@code null}
   *
   * @param type a {@link Type} used to {@linkplain
   * ConverterProvider#getConverter(Type) locate} an appropriate
   * {@link Converter}; must not be {@code null}
   *
   * @param defaultValueFunction a {@link BiFunction} accepting a
   * setting name and a {@link Set} of qualifier {@link Annotation}s
   * that returns a default {@link String}-typed value when a value
   * could not sourced; may be {@code null} in which case if no value
   * can be sourced a {@link NoSuchElementException} will be thrown;
   * may return {@code null}; must be safe for concurrent use by
   * mulitple threads; must not call any of this {@link Settings}
   * instance's methods or undefined behavior will result
   *
   * @return a suitable value (possibly {@code null})
   *
   * @exception NullPointerException if either {@code name} or {@code
   * type} is {@code null}
   *
   * @exception IllegalArgumentException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason; see {@link Converter#convert(Value)}
   *
   * @exception ConversionException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any reason
   * other than bad inputs
   *
   * @exception NoSuchElementException if {@code defaultValueFunction}
   * was {@code null} and no value could be sourced
   *
   * @exception ValueAcquisitionException if there was a procedural
   * problem acquiring a value
   *
   * @exception ArbitrationException if there was a problem performing
   * value arbitration
   *
   * @exception AmbiguousValuesException if arbitration completed but
   * could not resolve an ambiguity between potential return values
   *
   * @exception MalformedValuesException if the {@link
   * #handleMalformedValues(String, Set, Collection)} method was
   * overridden and the override throws a {@link
   * MalformedValuesException}
   *
   * @exception ELException if there was an error related to
   * expression language parsing or evaluation
   *
   * @threadsafety This method is and its overrides must be safe for
   * concurrent use by multiple threads.
   *
   * @idempotency No guarantees of any kind are made with respect to
   * the idempotency of this method or its overrides.
   *
   * @nullability This method may return {@code null}.
   *
   * @see #get(String, Set, Converter, BiFunction)
   */
  public final Object get(final String name,
                          final Type type,
                          final BiFunction<? super String, ? super Set<? extends Annotation>, ? extends String> defaultValueFunction) {
    return this.get(name,
                    this.qualifiers,
                    this.converterProvider.getConverter(type),
                    defaultValueFunction);
  }

  /**
   * Returns a suitable value for a setting named by the supplied
   * {@code name} and qualified with the supplied {@code qualifiers},
   * as {@linkplain Converter#convert(Value) converted} by the {@link
   * Converter} {@linkplain ConverterProvider#getConverter(Type)
   * located} using the supplied {@link Type}, and with default value
   * semantics implemented by the optional supplied {@code
   * defaultValueFunction}.
   *
   * @param name the name of the setting for which a value is to be
   * returned; must not be {@code null}
   *
   * @param qualifiers a {@link Set} of {@link Annotation}s to further
   * qualify the selection of the value to be returned; may be {@code
   * null}; if non-{@code null} then this parameter value must be safe
   * for concurrent iteration by multiple threads
   *
   * @param type a {@link Type} used to {@linkplain
   * ConverterProvider#getConverter(Type) locate} an appropriate
   * {@link Converter}; must not be {@code null}
   *
   * @param defaultValueFunction a {@link BiFunction} accepting a
   * setting name and a {@link Set} of qualifier {@link Annotation}s
   * that returns a default {@link String}-typed value when a value
   * could not sourced; may be {@code null} in which case if no value
   * can be sourced a {@link NoSuchElementException} will be thrown;
   * may return {@code null}; must be safe for concurrent use by
   * mulitple threads; must not call any of this {@link Settings}
   * instance's methods or undefined behavior will result
   *
   * @return a suitable value (possibly {@code null})
   *
   * @exception NullPointerException if either {@code name} or {@code
   * type} is {@code null}
   *
   * @exception IllegalArgumentException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason; see {@link Converter#convert(Value)}
   *
   * @exception ConversionException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any reason
   * other than bad inputs
   *
   * @exception NoSuchElementException if {@code defaultValueFunction}
   * was {@code null} and no value could be sourced
   *
   * @exception ValueAcquisitionException if there was a procedural
   * problem acquiring a value
   *
   * @exception ArbitrationException if there was a problem performing
   * value arbitration
   *
   * @exception AmbiguousValuesException if arbitration completed but
   * could not resolve an ambiguity between potential return values
   *
   * @exception MalformedValuesException if the {@link
   * #handleMalformedValues(String, Set, Collection)} method was
   * overridden and the override throws a {@link
   * MalformedValuesException}
   *
   * @exception ELException if there was an error related to
   * expression language parsing or evaluation
   *
   * @threadsafety This method is and its overrides must be safe for
   * concurrent use by multiple threads.
   *
   * @idempotency No guarantees of any kind are made with respect to
   * the idempotency of this method or its overrides.
   *
   * @nullability This method may return {@code null}.
   *
   * @see #get(String, Set, Converter, BiFunction)
   */
  public final Object get(final String name,
                          final Set<Annotation> qualifiers,
                          final Type type,
                          final BiFunction<? super String, ? super Set<? extends Annotation>, ? extends String> defaultValueFunction) {
    return this.get(name,
                    qualifiers,
                    this.converterProvider.getConverter(type),
                    defaultValueFunction);
  }

  //----------------------------------------------------------------------------

  /**
   * Returns a suitable value for a setting named by the supplied
   * {@code name} as {@linkplain Converter#convert(Value) converted}
   * by the supplied {@link Converter}.
   *
   * @param <T> the type to which any value should be {@linkplain
   * Converter#convert(Value) converted}
   *
   * @param name the name of the setting for which a value is to be
   * returned; must not be {@code null}
   *
   * @param converter a {@link Converter} used to {@linkplain
   * Converter#convert(Value) convert} a {@link String} value into a
   * setting value of the appropriate type; must not be {@code null};
   * must either be safe for concurrent use by multiple threads or
   * created specially for this method invocation or supplied in a
   * context where it is known that only one thread is executing at a
   * time
   *
   * @return a suitable value (possibly {@code null})
   *
   * @exception NullPointerException if either {@code name} or {@code
   * converter} is {@code null}
   *
   * @exception IllegalArgumentException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason; see {@link Converter#convert(Value)}
   *
   * @exception ConversionException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any reason
   * other than bad inputs
   *
   * @exception NoSuchElementException if no value could be sourced
   *
   * @exception ValueAcquisitionException if there was a procedural
   * problem acquiring a value
   *
   * @exception ArbitrationException if there was a problem performing
   * value arbitration
   *
   * @exception AmbiguousValuesException if arbitration completed but
   * could not resolve an ambiguity between potential return values
   *
   * @exception MalformedValuesException if the {@link
   * #handleMalformedValues(String, Set, Collection)} method was
   * overridden and the override throws a {@link
   * MalformedValuesException}
   *
   * @exception ELException if there was an error related to
   * expression language parsing or evaluation
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency No guarantees of any kind are made with respect to
   * the idempotency of this method.
   *
   * @nullability This method may return {@code null}.
   *
   * @see #get(String, Set, Converter, BiFunction)
   */
  public final <T> T get(final String name,
                         final Converter<? extends T> converter) {
    return this.get(name,
                    this.qualifiers,
                    converter,
                    null);
  }

  /**
   * Returns a suitable value for a setting named by the supplied
   * {@code name} and qualified with the supplied {@code qualifiers},
   * as {@linkplain Converter#convert(Value) converted} by the
   * supplied {@link Converter}.
   *
   * @param <T> the type to which any value should be {@linkplain
   * Converter#convert(Value) converted}
   *
   * @param name the name of the setting for which a value is to be
   * returned; must not be {@code null}
   *
   * @param qualifiers a {@link Set} of {@link Annotation}s to further
   * qualify the selection of the value to be returned; may be {@code
   * null}; if non-{@code null} then this parameter value must be safe
   * for concurrent iteration by multiple threads
   *
   * @param converter a {@link Converter} used to {@linkplain
   * Converter#convert(Value) convert} a {@link String} value into a
   * setting value of the appropriate type; must not be {@code null};
   * must either be safe for concurrent use by multiple threads or
   * created specially for this method invocation or supplied in a
   * context where it is known that only one thread is executing at a
   * time
   *
   * @return a suitable value (possibly {@code null})
   *
   * @exception NullPointerException if either {@code name} or {@code
   * converter} is {@code null}
   *
   * @exception IllegalArgumentException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason; see {@link Converter#convert(Value)}
   *
   * @exception ConversionException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any reason
   * other than bad inputs
   *
   * @exception NoSuchElementException if no value could be sourced
   *
   * @exception ValueAcquisitionException if there was a procedural
   * problem acquiring a value
   *
   * @exception ArbitrationException if there was a problem performing
   * value arbitration
   *
   * @exception AmbiguousValuesException if arbitration completed but
   * could not resolve an ambiguity between potential return values
   *
   * @exception MalformedValuesException if the {@link
   * #handleMalformedValues(String, Set, Collection)} method was
   * overridden and the override throws a {@link
   * MalformedValuesException}
   *
   * @exception ELException if there was an error related to
   * expression language parsing or evaluation
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency No guarantees of any kind are made with respect to
   * the idempotency of this method.
   *
   * @nullability This method may return {@code null}.
   *
   * @see #get(String, Set, Converter, BiFunction)
   */
  public final <T> T get(final String name,
                         final Set<Annotation> qualifiers,
                         final Converter<? extends T> converter) {
    return this.get(name,
                    qualifiers,
                    converter,
                    null);
  }

  /**
   * Returns a suitable value for a setting named by the supplied
   * {@code name} as {@linkplain Converter#convert(Value) converted}
   * by the supplied {@link Converter} and with default value
   * semantics implemented by the optional supplied {@code
   * defaultValueFunction}.
   *
   * @param <T> the type to which any value should be {@linkplain
   * Converter#convert(Value) converted}
   *
   * @param name the name of the setting for which a value is to be
   * returned; must not be {@code null}
   *
   * @param converter a {@link Converter} used to {@linkplain
   * Converter#convert(Value) convert} a {@link String} value into a
   * setting value of the appropriate type; must not be {@code null};
   * must either be safe for concurrent use by multiple threads or
   * created specially for this method invocation or supplied in a
   * context where it is known that only one thread is executing at a
   * time
   *
   * @param defaultValueFunction a {@link BiFunction} accepting a
   * setting name and a {@link Set} of qualifier {@link Annotation}s
   * that returns a default {@link String}-typed value when a value
   * could not sourced; may be {@code null} in which case if no value
   * can be sourced a {@link NoSuchElementException} will be thrown;
   * may return {@code null}; must be safe for concurrent use by
   * mulitple threads; must not call any of this {@link Settings}
   * instance's methods or undefined behavior will result
   *
   * @return a suitable value (possibly {@code null})
   *
   * @exception NullPointerException if either {@code name} or {@code
   * converter} is {@code null}
   *
   * @exception IllegalArgumentException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason; see {@link Converter#convert(Value)}
   *
   * @exception ConversionException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any reason
   * other than bad inputs
   *
   * @exception NoSuchElementException if {@code defaultValueFunction}
   * was {@code null} and no value could be sourced
   *
   * @exception ValueAcquisitionException if there was a procedural
   * problem acquiring a value
   *
   * @exception ArbitrationException if there was a problem performing
   * value arbitration
   *
   * @exception AmbiguousValuesException if arbitration completed but
   * could not resolve an ambiguity between potential return values
   *
   * @exception MalformedValuesException if the {@link
   * #handleMalformedValues(String, Set, Collection)} method was
   * overridden and the override throws a {@link
   * MalformedValuesException}
   *
   * @exception ELException if there was an error related to
   * expression language parsing or evaluation
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency No guarantees of any kind are made with respect to
   * the idempotency of this method.
   *
   * @nullability This method may return {@code null}.
   *
   * @see #get(String, Set, Converter, BiFunction)
   */
  public final <T> T get(final String name,
                         final Converter<? extends T> converter,
                         final BiFunction<? super String, ? super Set<? extends Annotation>, ? extends String> defaultValueFunction) {
    return this.get(name,
                    this.qualifiers,
                    converter,
                    defaultValueFunction);
  }

  /**
   * Returns a suitable value for a setting named by the supplied
   * {@code name} and qualified with the supplied {@code qualifiers},
   * as {@linkplain Converter#convert(Value) converted} by the
   * supplied {@link Converter} and with default value semantics
   * implemented by the optional supplied {@code
   * defaultValueFunction}.
   *
   * @param <T> the type to which any value should be {@linkplain
   * Converter#convert(Value) converted}
   *
   * @param name the name of the setting for which a value is to be
   * returned; must not be {@code null}
   *
   * @param qualifiers a {@link Set} of {@link Annotation}s to further
   * qualify the selection of the value to be returned; may be {@code
   * null}; if non-{@code null} then this parameter value must be safe
   * for concurrent iteration by multiple threads
   *
   * @param converter a {@link Converter} used to {@linkplain
   * Converter#convert(Value) convert} a {@link String} value into a
   * setting value of the appropriate type; must not be {@code null};
   * must either be safe for concurrent use by multiple threads or
   * created specially for this method invocation or supplied in a
   * context where it is known that only one thread is executing at a
   * time
   *
   * @param defaultValueFunction a {@link BiFunction} accepting a
   * setting name and a {@link Set} of qualifier {@link Annotation}s
   * that returns a default {@link String}-typed value when a value
   * could not sourced; may be {@code null} in which case if no value
   * can be sourced a {@link NoSuchElementException} will be thrown;
   * may return {@code null}; must be safe for concurrent use by
   * mulitple threads; must not call any of this {@link Settings}
   * instance's methods or undefined behavior will result
   *
   * @return a suitable value (possibly {@code null})
   *
   * @exception NullPointerException if either {@code name} or {@code
   * converter} is {@code null}
   *
   * @exception IllegalArgumentException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason; see {@link Converter#convert(Value)}
   *
   * @exception ConversionException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any reason
   * other than bad inputs
   *
   * @exception NoSuchElementException if {@code defaultValueFunction}
   * was {@code null} and no value could be sourced
   *
   * @exception ValueAcquisitionException if there was a procedural
   * problem acquiring a value
   *
   * @exception ArbitrationException if there was a problem performing
   * value arbitration
   *
   * @exception AmbiguousValuesException if arbitration completed but
   * could not resolve an ambiguity between potential return values
   *
   * @exception MalformedValuesException if the {@link
   * #handleMalformedValues(String, Set, Collection)} method was
   * overridden and the override throws a {@link
   * MalformedValuesException}
   *
   * @exception ELException if there was an error related to
   * expression language parsing or evaluation
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency No guarantees of any kind are made with respect to
   * the idempotency of this method.
   *
   * @nullability This method may return {@code null}.
   */
  public final <T> T get(final String name,
                         final Set<Annotation> qualifiers,
                         final Converter<? extends T> converter,
                         final BiFunction<? super String, ? super Set<? extends Annotation>, ? extends String> defaultValueFunction) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(converter);
    final ExpressionFactory expressionFactory = ExpressionFactory.newInstance();
    final StandardELContext elContext = new StandardELContext(expressionFactory);
    elContext.addELResolver(new SourceELResolver(this, expressionFactory, qualifiers));
    return this.get(name,
                    qualifiers,
                    elContext,
                    expressionFactory,
                    converter,
                    defaultValueFunction);
  }

  //----------------------------------------------------------------------------

  /**
   * Returns a suitable value for a setting named by the supplied
   * {@code name} and qualified with the supplied {@code qualifiers},
   * as {@linkplain Converter#convert(Value) converted} by the
   * supplied {@link Converter} and with default value semantics
   * implemented by the optional supplied {@code
   * defaultValueFunction}.
   *
   * @param name the name of the setting for which a value is to be
   * returned; must not be {@code null}
   *
   * @param qualifiers a {@link Set} of {@link Annotation}s to further
   * qualify the selection of the value to be returned; may be {@code
   * null}; if non-{@code null} then this parameter value must be safe
   * for concurrent iteration by multiple threads
   *
   * @param elContext an {@link ELContext}; must not be {@code null};
   * must either be safe for concurrent use by multiple threads
   * (highly unlikely) or created specially for this method invocation
   * or supplied in a context where it is known that only one thread
   * is executing at a time
   *
   * @param expressionFactory an {@link ExpressionFactory} used to
   * {@linkplain ExpressionFactory#createValueExpression(ELContext,
   * String, Class) create <code>ValueExpression</code>}s; must not be
   * {@code null}; must either be safe for concurrent use by multiple
   * threads (highly unlikely) or created specially for this method
   * invocation or supplied in a context where it is known that only
   * one thread is executing at a time
   *
   * @param converter a {@link Converter} used to {@linkplain
   * Converter#convert(Value) convert} a {@link String} value into a
   * setting value of the appropriate type; must not be {@code null};
   * must either be safe for concurrent use by multiple threads or
   * created specially for this method invocation or supplied in a
   * context where it is known that only one thread is executing at a
   * time
   *
   * @param defaultValueFunction a {@link BiFunction} accepting a
   * setting name and a {@link Set} of qualifier {@link Annotation}s
   * that returns a default {@link String}-typed value when a value
   * could not sourced; may be {@code null} in which case if no value
   * can be sourced a {@link NoSuchElementException} will be thrown;
   * may return {@code null}; must be safe for concurrent use by
   * mulitple threads; must not call any of this {@link Settings}
   * instance's methods or undefined behavior will result
   *
   * @exception NullPointerException if either {@code name}, {@code
   * elContext}, {@code expressionFactory} or {@code converter} is
   * {@code null}
   *
   * @exception IllegalArgumentException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason; see {@link Converter#convert(Value)}
   *
   * @exception ConversionException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason other than bad inputs
   *
   * @exception NoSuchElementException if {@code defaultValueFunction}
   * was {@code null} and no value could be sourced
   *
   * @exception ValueAcquisitionException if there was a procedural
   * problem acquiring a value
   *
   * @exception ArbitrationException if there was a problem performing
   * value arbitration
   *
   * @exception AmbiguousValuesException if arbitration completed but
   * could not resolve an ambiguity between potential return values
   *
   * @exception MalformedValuesException if the {@link
   * #handleMalformedValues(String, Set, Collection)} method was
   * overridden and the override throws a {@link
   * MalformedValuesException}
   *
   * @exception ELException if there was an error related to
   * expression language parsing or evaluation
   *
   * @threadsafety This method is and its overrides must be safe for
   * concurrent use by multiple threads.
   *
   * @idempotency No guarantees of any kind are made with respect to
   * the idempotency of this method or its overrides.
   *
   * @nullability This method may return {@code null}.
   */
  final <T> T get(final String name,
                  final Set<Annotation> qualifiers,
                  final ELContext elContext,
                  final ExpressionFactory expressionFactory,
                  final Converter<? extends T> converter,
                  final BiFunction<? super String, ? super Set<? extends Annotation>, ? extends String> defaultValueFunction) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(converter);
    Value value =
      this.getValue(name,
                    qualifiers,
                    elContext,
                    expressionFactory,
                    defaultValueFunction);

    final String stringToInterpolate;
    if (value == null) {
      if (defaultValueFunction == null) {
        // There was no value that came up.  There's also no way to
        // get a default one.  So the value is missing.
        throw new NoSuchElementException(name + " (" + qualifiers + ")");
      } else {
        stringToInterpolate = defaultValueFunction.apply(name, qualifiers);
      }
    } else {
      stringToInterpolate = value.get();
    }
    final String interpolatedString = this.interpolate(stringToInterpolate, elContext, expressionFactory, qualifiers);
    if (value == null) {
      value = new Value(null /* no Source; we synthesized this Value */, name, qualifiers, interpolatedString);
    } else {
      value = new Value(value, interpolatedString);
    }
    return converter.convert(value);
  }

  //----------------------------------------------------------------------------

  /**
   * Implements the {@link Source#getValue(String, Set)} method so
   * that this {@link Settings} can be conveniently used as a {@link
   * Source} from a higher-order {@link Settings}.
   *
   * <p>End users should never need to call this method directly.</p>
   *
   * @param name the name of the setting for which a value is to be
   * returned; must not be {@code null}
   *
   * @param qualifiers a {@link Set} of {@link Annotation}s to further
   * qualify the selection of the value to be returned; may be {@code
   * null}; if non-{@code null} then this parameter value must be safe
   * for concurrent iteration by multiple threads
   *
   * @return a suitable {@link Value}, or {@code null} if no {@link
   * Value} could be created or acquired
   *
   * @exception NullPointerException if {@code name} is {@code null}
   *
   * @exception ValueAcquisitionException if there was a procedural
   * problem acquiring a {@link Value}
   *
   * @nullability This method may return {@code null}.
   *
   * @idempotency This method may not be idempotent.  That is, two
   * invocations supplied with the same {@code name} and {@code
   * qualifiers} parameter values may or may not return {@link Value}s
   * that are identical, {@linkplain Object#equals(Object) equal} or
   * neither.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @see Source#getValue(String, Set)
   */
  @Experimental
  @Override
  public final Value getValue(final String name, final Set<Annotation> qualifiers) {
    final ExpressionFactory expressionFactory = ExpressionFactory.newInstance();
    final StandardELContext elContext = new StandardELContext(expressionFactory);
    elContext.addELResolver(new SourceELResolver(this, expressionFactory, qualifiers));
    try {
      return this.getValue(name, qualifiers, elContext, expressionFactory, NULL);
    } catch (final AmbiguousValuesException ambiguousValuesException) {
      throw new ValueAcquisitionException(ambiguousValuesException.getMessage(), ambiguousValuesException);
    }
  }

  private final Value getValue(final String name,
                               Set<Annotation> qualifiers,
                               final ELContext elContext,
                               final ExpressionFactory expressionFactory,
                               final BiFunction<? super String,
                                                ? super Set<? extends Annotation>,
                                                ? extends String> defaultValueFunction) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(elContext);
    Objects.requireNonNull(expressionFactory);

    final int qualifiersSize;
    if (qualifiers == null || qualifiers.isEmpty()) {
      qualifiers = Collections.emptySet();
      qualifiersSize = 0;
    } else {
      qualifiers = Collections.unmodifiableSet(qualifiers);
      qualifiersSize = qualifiers.size();
    }

    // The candidate for returning.
    Value selectedValue = null;

    // Values that need to be arbitrated; i.e. conflicts.
    Queue<Value> conflictingValues = null;

    // Bad values.
    Collection<Value> badValues = null;

    final Set<? extends Source> sources = this.sourcesFunction.apply(name, qualifiers);
    if (sources != null) {
      for (final Source source : sources) {
        if (source != null && source != this) {

          final Value value = source.getValue(name, qualifiers);

          if (value != null) {

            if (name.equals(value.getName())) {

              Set<Annotation> valueQualifiers = value.getQualifiers();
              final int valueQualifiersSize;
              if (valueQualifiers == null) {
                valueQualifiersSize = 0;
                valueQualifiers = Collections.emptySet();
              } else {
                valueQualifiersSize = valueQualifiers.size();
              }

              if (qualifiersSize < valueQualifiersSize) {
                // The value said it was for, say, {a=b, c=d}.  We
                // supplied just {a=b}, or maybe even {q=r}.  So
                // somehow the value's qualifiers were overly
                // specific.  It doesn't matter whether some of the
                // value's qualifiers were contained by us or not;
                // it's a bad value either way.
                if (badValues == null) {
                  badValues = new ArrayList<>();
                }
                badValues.add(value);

              } else if (qualifiers.equals(valueQualifiers)) {
                // We asked for, say, {a=b, c=d}; the value said it
                // was for exactly {a=b, c=d}.  That's very good: we
                // now have an exact match.  We hope it's going to
                // be the only one, i.e. that no other source ALSO
                // produces an exact match.  If we have two exact
                // matches, then they're obviously a conflict.
                if (selectedValue == null) {
                  if (conflictingValues == null || conflictingValues.isEmpty()) {
                    // There aren't any conflicts yet; this is good.
                    // This value will be our candidate.
                    selectedValue = value;
                  } else {
                    // The conflicting values queue already contains
                    // something, and the only way it could is when
                    // a prior exact match happened.  That is, we
                    // got an exact match, true, but we already
                    // *had* an exact match, so now we have TWO
                    // exact matches, and therefore we don't in fact
                    // have a candidate--instead, add it to the
                    // conflicting values queue.
                    conflictingValues.add(value);
                  }
                } else {
                  // We have an exact match, but we already
                  // identified a candidate, so oops, we have to
                  // treat our prior match and this one as
                  // non-candidates.
                  if (conflictingValues == null) {
                    conflictingValues = new PriorityQueue<>(valueComparator);
                  }
                  conflictingValues.add(selectedValue);
                  conflictingValues.add(value);
                  // "Erase" our prior best candidate, now that we
                  // have more than one.
                  selectedValue = null;
                }

              } else if (qualifiersSize == valueQualifiersSize) {
                // We asked for, e.g., {a=b} and we got back {c=d}.
                // Bad value!  The configuration subsystem handed
                // back a value containing coordinates not drawn
                // from the configurationCoordinatesSet.  We know
                // this because we already tested for Set equality,
                // which failed, so this test means disparate
                // entries.
                if (badValues == null) {
                  badValues = new ArrayList<>();
                }
                badValues.add(value);

              } else if (selectedValue != null) {
                assert qualifiersSize > valueQualifiersSize;
                // We asked for {a=b, c=d}; we got back either {a=b}
                // or {c=d} or {q=x}.  We already have selectedValue
                // set so we already had a better match at some
                // point.

                if (!qualifiers.containsAll(valueQualifiers)) {
                  // We asked for {a=b, c=d}; we got back {q=x}.
                  // That's a bad value.
                  if (badValues == null) {
                    badValues = new ArrayList<>();
                  }
                  badValues.add(value);
                }

              } else if (qualifiers.containsAll(valueQualifiers)) {
                assert qualifiersSize > valueQualifiersSize;
                // We specified, e.g., {a=b, c=d, e=f} and they
                // have, say, {c=d, e=f} or {a=b, c=d} etc. but not,
                // say, {q=r}.  So we got a
                // less-than-perfect-but-possibly-suitable match.
                if (conflictingValues == null) {
                  conflictingValues = new PriorityQueue<>(valueComparator);
                }
                conflictingValues.add(value);

              } else {
                // Bad value!
                if (badValues == null) {
                  badValues = new ArrayList<>();
                }
                badValues.add(value);

              }

            } else {
              // We asked for "frobnicationInterval"; they responded
              // with "hostname".  Bad value.
              if (badValues == null) {
                badValues = new ArrayList<>();
              }
              badValues.add(value);

            }
          }
        }
      }
    }

    if (badValues != null && !badValues.isEmpty()) {
      this.handleMalformedValues(name, qualifiers, badValues);
    }

    Collection<Value> valuesToArbitrate = null;

    if (selectedValue == null) {

      if (conflictingValues != null) {

        // Perform arbitration.  The first "round" of arbitration is
        // hard-coded, effectively: we check to see if all conflicting
        // values are of different specificities.  If they are, then
        // the most specific value is selected and the others are
        // discarded.  Otherwise, if any two conflicting values share
        // specificities, then they are added to a collection over
        // which our supplied Arbiters will operate.

        int highestSpecificitySoFarEncountered = -1;

        while (!conflictingValues.isEmpty()) {

          final Value value = conflictingValues.poll();
          assert value != null;

          final int valueSpecificity = Math.max(0, value.getQualifiers().size());
          assert highestSpecificitySoFarEncountered < 0 || valueSpecificity <= highestSpecificitySoFarEncountered;

          if (highestSpecificitySoFarEncountered < 0 || valueSpecificity < highestSpecificitySoFarEncountered) {
            if (selectedValue == null) {
              assert valuesToArbitrate == null || valuesToArbitrate.isEmpty();
              selectedValue = value;
              highestSpecificitySoFarEncountered = valueSpecificity;
            } else if (valuesToArbitrate == null || valuesToArbitrate.isEmpty()) {
              // We have a selected value that is non-null, and no
              // further values to arbitrate, so we're done.  We know
              // we picked the most specific value so we effectively
              // discard the others.
              break;
            } else {
              valuesToArbitrate.add(value);
            }
          } else if (valueSpecificity == highestSpecificitySoFarEncountered) {
            assert selectedValue != null;
            if (value.isAuthoritative()) {
              if (selectedValue.isAuthoritative()) {
                // Both say they're authoritative; arbitration required
                if (valuesToArbitrate == null) {
                  valuesToArbitrate = new ArrayList<>();
                }
                valuesToArbitrate.add(selectedValue);
                selectedValue = null;
                valuesToArbitrate.add(value);
              } else {
                // value is authoritative; selectedValue is not; so swap
                // them
                selectedValue = value;
              }
            } else if (selectedValue.isAuthoritative()) {
              // value is not authoritative; selected value is; so just
              // drop value on the floor; it's not authoritative.
            } else {
              // Neither is authoritative; arbitration required.
              if (valuesToArbitrate == null) {
                valuesToArbitrate = new ArrayList<>();
              }
              valuesToArbitrate.add(selectedValue);
              selectedValue = null;
              valuesToArbitrate.add(value);
            }
          } else {
            assert false : "valueSpecificity > highestSpecificitySoFarEncountered: " +
              valueSpecificity + " > " + highestSpecificitySoFarEncountered;
          }
        }
      }
    }

    if (selectedValue == null) {
      if (valuesToArbitrate == null || valuesToArbitrate.isEmpty()) {
        selectedValue = this.arbitrate(sources, name, qualifiers, Collections.emptySet());
      } else {
        selectedValue = this.arbitrate(sources, name, qualifiers, Collections.unmodifiableCollection(valuesToArbitrate));
        if (selectedValue == null) {
          throw new AmbiguousValuesException(valuesToArbitrate);
        }
      }
    }

    return selectedValue;
  }

  //----------------------------------------------------------------------------

  /**
   * Configures the supplied Java Bean by {@linkplain #get(String,
   * Set, Converter, BiFunction) acquiring setting values} named after
   * the supplied {@link PropertyDescriptor}s and using their
   * {@linkplain PropertyDescriptor#getWriteMethod() affiliated write
   * methods} to set the corresponding values.
   *
   * <p>This implementation calls the {@link #configure(Object,
   * Iterable, String, Set)} method with sensible defaults.</p>
   *
   * @param object the {@link Object} to configure; must not be {@code null}
   *
   * @exception NullPointerException if either {@code object} or {@code
   * beanInfo} is {@code null}
   *
   * @exception IntrospectionException if introspection of the
   * supplied {@code object} fails
   *
   * @exception ReflectiveOperationException if there was a problem
   * invoking a {@linkplain PropertyDescriptor#getWriteMethod() write
   * method}; the supplied {@link Object} may, in this case, be left
   * in an inconsistent state
   *
   * @exception IllegalArgumentException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason; see {@link Converter#convert(Value)}; the supplied {@link
   * Object} may, in this case, be left in an inconsistent state
   *
   * @exception ConversionException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason other than bad inputs; the supplied {@link Object} may, in
   * this case, be left in an inconsistent state
   *
   * @exception ValueAcquisitionException if there was a procedural
   * problem acquiring a value; the supplied {@link Object} may, in
   * this case, be left in an inconsistent state
   *
   * @exception ArbitrationException if there was a problem performing
   * value arbitration; the supplied {@link Object} may, in this case,
   * be left in an inconsistent state
   *
   * @exception AmbiguousValuesException if arbitration completed but
   * could not resolve an ambiguity between potential return values;
   * the supplied {@link Object} may, in this case, be left in an
   * inconsistent state
   *
   * @exception MalformedValuesException if the {@link
   * #handleMalformedValues(String, Set, Collection)} method was
   * overridden and the override throws a {@link
   * MalformedValuesException}; the supplied {@link Object} may, in
   * this case, be left in an inconsistent state
   *
   * @exception ELException if there was an error related to
   * expression language parsing or evaluation; the supplied {@link
   * Object} may, in this case, be left in an inconsistent state
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency No guarantees are made with respect to the
   * idempotency of this method.
   *
   * @see #configure(Object, Iterable, String, Set)
   */
  public final void configure(final Object object)
    throws IntrospectionException, ReflectiveOperationException {
    this.configure(object,
                   Arrays.asList(Introspector.getBeanInfo(object.getClass()).getPropertyDescriptors()),
                   null,
                   this.qualifiers);
  }

  /**
   * Configures the supplied Java Bean by {@linkplain #get(String,
   * Set, Converter, BiFunction) acquiring setting values} named after
   * the supplied {@link PropertyDescriptor}s and using their
   * {@linkplain PropertyDescriptor#getWriteMethod() affiliated write
   * methods} to set the corresponding values.
   *
   * <p>This implementation calls the {@link #configure(Object,
   * Iterable, String, Set)} method with sensible defaults.</p>
   *
   * @param object the {@link Object} to configure; must not be {@code null}
   *
   * @param prefix a {@link String} that will be prepended to each
   * {@linkplain PropertyDescriptor#getName()
   * <code>PropertyDescriptor</code> name} before using the result as
   * the name of a setting for which a value {@linkplain #get(String,
   * Set, Converter, BiFunction) will be acquired}; may be {@code
   * null}
   *
   * @exception NullPointerException if either {@code object} or {@code
   * beanInfo} is {@code null}
   *
   * @exception IntrospectionException if introspection of the
   * supplied {@code object} fails
   *
   * @exception ReflectiveOperationException if there was a problem
   * invoking a {@linkplain PropertyDescriptor#getWriteMethod() write
   * method}; the supplied {@link Object} may, in this case, be left
   * in an inconsistent state
   *
   * @exception IllegalArgumentException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason; see {@link Converter#convert(Value)}; the supplied {@link
   * Object} may, in this case, be left in an inconsistent state
   *
   * @exception ConversionException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason other than bad inputs; the supplied {@link Object} may, in
   * this case, be left in an inconsistent state
   *
   * @exception ValueAcquisitionException if there was a procedural
   * problem acquiring a value; the supplied {@link Object} may, in
   * this case, be left in an inconsistent state
   *
   * @exception ArbitrationException if there was a problem performing
   * value arbitration; the supplied {@link Object} may, in this case,
   * be left in an inconsistent state
   *
   * @exception AmbiguousValuesException if arbitration completed but
   * could not resolve an ambiguity between potential return values;
   * the supplied {@link Object} may, in this case, be left in an
   * inconsistent state
   *
   * @exception MalformedValuesException if the {@link
   * #handleMalformedValues(String, Set, Collection)} method was
   * overridden and the override throws a {@link
   * MalformedValuesException}; the supplied {@link Object} may, in
   * this case, be left in an inconsistent state
   *
   * @exception ELException if there was an error related to
   * expression language parsing or evaluation; the supplied {@link
   * Object} may, in this case, be left in an inconsistent state
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency No guarantees are made with respect to the
   * idempotency of this method.
   *
   * @see #configure(Object, Iterable, String, Set)
   */
  public final void configure(final Object object,
                              final String prefix)
    throws IntrospectionException, ReflectiveOperationException {
    this.configure(object,
                   Arrays.asList(Introspector.getBeanInfo(object.getClass()).getPropertyDescriptors()),
                   prefix,
                   this.qualifiers);
  }

  /**
   * Configures the supplied Java Bean by {@linkplain #get(String,
   * Set, Converter, BiFunction) acquiring setting values} named after
   * the supplied {@link PropertyDescriptor}s and using their
   * {@linkplain PropertyDescriptor#getWriteMethod() affiliated write
   * methods} to set the corresponding values.
   *
   * <p>This implementation calls the {@link #configure(Object,
   * Iterable, String, Set)} method with sensible defaults.</p>
   *
   * @param object the {@link Object} to configure; must not be {@code null}
   *
   * @param qualifiers a {@link Set} of {@link Annotation}s to further
   * qualify the selection of values; may be {@code null}; if
   * non-{@code null} then this parameter value must be safe for
   * concurrent iteration by multiple threads
   *
   * @exception NullPointerException if either {@code object} or {@code
   * beanInfo} is {@code null}
   *
   * @exception IntrospectionException if introspection of the
   * supplied {@code object} fails
   *
   * @exception ReflectiveOperationException if there was a problem
   * invoking a {@linkplain PropertyDescriptor#getWriteMethod() write
   * method}; the supplied {@link Object} may, in this case, be left
   * in an inconsistent state
   *
   * @exception IllegalArgumentException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason; see {@link Converter#convert(Value)}; the supplied {@link
   * Object} may, in this case, be left in an inconsistent state
   *
   * @exception ConversionException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason other than bad inputs; the supplied {@link Object} may, in
   * this case, be left in an inconsistent state
   *
   * @exception ValueAcquisitionException if there was a procedural
   * problem acquiring a value; the supplied {@link Object} may, in
   * this case, be left in an inconsistent state
   *
   * @exception ArbitrationException if there was a problem performing
   * value arbitration; the supplied {@link Object} may, in this case,
   * be left in an inconsistent state
   *
   * @exception AmbiguousValuesException if arbitration completed but
   * could not resolve an ambiguity between potential return values;
   * the supplied {@link Object} may, in this case, be left in an
   * inconsistent state
   *
   * @exception MalformedValuesException if the {@link
   * #handleMalformedValues(String, Set, Collection)} method was
   * overridden and the override throws a {@link
   * MalformedValuesException}; the supplied {@link Object} may, in
   * this case, be left in an inconsistent state
   *
   * @exception ELException if there was an error related to
   * expression language parsing or evaluation; the supplied {@link
   * Object} may, in this case, be left in an inconsistent state
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency No guarantees are made with respect to the
   * idempotency of this method.
   *
   * @see #configure(Object, Iterable, String, Set)
   */
  public final void configure(final Object object,
                              final Set<Annotation> qualifiers)
    throws IntrospectionException, ReflectiveOperationException {
    this.configure(object,
                   Arrays.asList(Introspector.getBeanInfo(object.getClass()).getPropertyDescriptors()),
                   null,
                   qualifiers);
  }

  /**
   * Configures the supplied Java Bean by {@linkplain #get(String,
   * Set, Converter, BiFunction) acquiring setting values} named after
   * the supplied {@link PropertyDescriptor}s and using their
   * {@linkplain PropertyDescriptor#getWriteMethod() affiliated write
   * methods} to set the corresponding values.
   *
   * <p>This implementation calls the {@link #configure(Object,
   * Iterable, String, Set)} method with sensible defaults.</p>
   *
   * @param object the {@link Object} to configure; must not be {@code null}
   *
   * @param prefix a {@link String} that will be prepended to each
   * {@linkplain PropertyDescriptor#getName()
   * <code>PropertyDescriptor</code> name} before using the result as
   * the name of a setting for which a value {@linkplain #get(String,
   * Set, Converter, BiFunction) will be acquired}; may be {@code
   * null}
   *
   * @param qualifiers a {@link Set} of {@link Annotation}s to further
   * qualify the selection of values; may be {@code null}; if
   * non-{@code null} then this parameter value must be safe for
   * concurrent iteration by multiple threads
   *
   * @exception NullPointerException if either {@code object} or {@code
   * beanInfo} is {@code null}
   *
   * @exception IntrospectionException if introspection of the
   * supplied {@code object} fails
   *
   * @exception ReflectiveOperationException if there was a problem
   * invoking a {@linkplain PropertyDescriptor#getWriteMethod() write
   * method}; the supplied {@link Object} may, in this case, be left
   * in an inconsistent state
   *
   * @exception IllegalArgumentException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason; see {@link Converter#convert(Value)}; the supplied {@link
   * Object} may, in this case, be left in an inconsistent state
   *
   * @exception ConversionException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason other than bad inputs; the supplied {@link Object} may, in
   * this case, be left in an inconsistent state
   *
   * @exception ValueAcquisitionException if there was a procedural
   * problem acquiring a value; the supplied {@link Object} may, in
   * this case, be left in an inconsistent state
   *
   * @exception ArbitrationException if there was a problem performing
   * value arbitration; the supplied {@link Object} may, in this case,
   * be left in an inconsistent state
   *
   * @exception AmbiguousValuesException if arbitration completed but
   * could not resolve an ambiguity between potential return values;
   * the supplied {@link Object} may, in this case, be left in an
   * inconsistent state
   *
   * @exception MalformedValuesException if the {@link
   * #handleMalformedValues(String, Set, Collection)} method was
   * overridden and the override throws a {@link
   * MalformedValuesException}; the supplied {@link Object} may, in
   * this case, be left in an inconsistent state
   *
   * @exception ELException if there was an error related to
   * expression language parsing or evaluation; the supplied {@link
   * Object} may, in this case, be left in an inconsistent state
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency No guarantees are made with respect to the
   * idempotency of this method.
   *
   * @see #configure(Object, Iterable, String, Set)
   */
  public final void configure(final Object object,
                              final String prefix,
                              final Set<Annotation> qualifiers)
    throws IntrospectionException, ReflectiveOperationException {
    this.configure(object,
                   Arrays.asList(Introspector.getBeanInfo(object.getClass()).getPropertyDescriptors()),
                   prefix,
                   qualifiers);
  }

  /**
   * Configures the supplied Java Bean by {@linkplain #get(String,
   * Set, Converter, BiFunction) acquiring setting values} named after
   * the supplied {@link PropertyDescriptor}s and using their
   * {@linkplain PropertyDescriptor#getWriteMethod() affiliated write
   * methods} to set the corresponding values.
   *
   * <p>This implementation calls the {@link #configure(Object,
   * Iterable, String, Set)} method with sensible defaults.</p>
   *
   * @param object the {@link Object} to configure; must not be {@code null}
   *
   * @param beanInfo a {@link BeanInfo} {@linkplain
   * BeanInfo#getPropertyDescriptors() providing access to
   * <code>PropertyDescriptor</code>s}; must not be {@code null}
   *
   * @exception NullPointerException if either {@code object} or {@code
   * beanInfo} is {@code null}
   *
   * @exception ReflectiveOperationException if there was a problem
   * invoking a {@linkplain PropertyDescriptor#getWriteMethod() write
   * method}; the supplied {@link Object} may, in this case, be left
   * in an inconsistent state
   *
   * @exception IllegalArgumentException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason; see {@link Converter#convert(Value)}; the supplied {@link
   * Object} may, in this case, be left in an inconsistent state
   *
   * @exception ConversionException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason other than bad inputs; the supplied {@link Object} may, in
   * this case, be left in an inconsistent state
   *
   * @exception ValueAcquisitionException if there was a procedural
   * problem acquiring a value; the supplied {@link Object} may, in
   * this case, be left in an inconsistent state
   *
   * @exception ArbitrationException if there was a problem performing
   * value arbitration; the supplied {@link Object} may, in this case,
   * be left in an inconsistent state
   *
   * @exception AmbiguousValuesException if arbitration completed but
   * could not resolve an ambiguity between potential return values;
   * the supplied {@link Object} may, in this case, be left in an
   * inconsistent state
   *
   * @exception MalformedValuesException if the {@link
   * #handleMalformedValues(String, Set, Collection)} method was
   * overridden and the override throws a {@link
   * MalformedValuesException}; the supplied {@link Object} may, in
   * this case, be left in an inconsistent state
   *
   * @exception ELException if there was an error related to
   * expression language parsing or evaluation; the supplied {@link
   * Object} may, in this case, be left in an inconsistent state
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency No guarantees are made with respect to the
   * idempotency of this method.
   *
   * @see #configure(Object, Iterable, String, Set)
   */
  public final void configure(final Object object,
                              final BeanInfo beanInfo)
    throws ReflectiveOperationException {
    this.configure(object,
                   Arrays.asList(beanInfo.getPropertyDescriptors()),
                   null,
                   this.qualifiers);
  }

  /**
   * Configures the supplied Java Bean by {@linkplain #get(String,
   * Set, Converter, BiFunction) acquiring setting values} named after
   * the supplied {@link PropertyDescriptor}s and using their
   * {@linkplain PropertyDescriptor#getWriteMethod() affiliated write
   * methods} to set the corresponding values.
   *
   * <p>This implementation calls the {@link #configure(Object,
   * Iterable, String, Set)} method with sensible defaults.</p>
   *
   * @param object the {@link Object} to configure; must not be {@code null}
   *
   * @param beanInfo a {@link BeanInfo} {@linkplain
   * BeanInfo#getPropertyDescriptors() providing access to
   * <code>PropertyDescriptor</code>s}; must not be {@code null}
   *
   * @param prefix a {@link String} that will be prepended to each
   * {@linkplain PropertyDescriptor#getName()
   * <code>PropertyDescriptor</code> name} before using the result as
   * the name of a setting for which a value {@linkplain #get(String,
   * Set, Converter, BiFunction) will be acquired}; may be {@code
   * null}
   *
   * @exception NullPointerException if either {@code object} or {@code
   * beanInfo} is {@code null}
   *
   * @exception ReflectiveOperationException if there was a problem
   * invoking a {@linkplain PropertyDescriptor#getWriteMethod() write
   * method}; the supplied {@link Object} may, in this case, be left
   * in an inconsistent state
   *
   * @exception IllegalArgumentException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason; see {@link Converter#convert(Value)}; the supplied {@link
   * Object} may, in this case, be left in an inconsistent state
   *
   * @exception ConversionException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason other than bad inputs; the supplied {@link Object} may, in
   * this case, be left in an inconsistent state
   *
   * @exception ValueAcquisitionException if there was a procedural
   * problem acquiring a value; the supplied {@link Object} may, in
   * this case, be left in an inconsistent state
   *
   * @exception ArbitrationException if there was a problem performing
   * value arbitration; the supplied {@link Object} may, in this case,
   * be left in an inconsistent state
   *
   * @exception AmbiguousValuesException if arbitration completed but
   * could not resolve an ambiguity between potential return values;
   * the supplied {@link Object} may, in this case, be left in an
   * inconsistent state
   *
   * @exception MalformedValuesException if the {@link
   * #handleMalformedValues(String, Set, Collection)} method was
   * overridden and the override throws a {@link
   * MalformedValuesException}; the supplied {@link Object} may, in
   * this case, be left in an inconsistent state
   *
   * @exception ELException if there was an error related to
   * expression language parsing or evaluation; the supplied {@link
   * Object} may, in this case, be left in an inconsistent state
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency No guarantees are made with respect to the
   * idempotency of this method.
   *
   * @see #configure(Object, Iterable, String, Set)
   */
  public final void configure(final Object object,
                              final BeanInfo beanInfo,
                              final String prefix)
    throws ReflectiveOperationException {
    this.configure(object,
                   Arrays.asList(beanInfo.getPropertyDescriptors()),
                   prefix,
                   this.qualifiers);
  }

  /**
   * Configures the supplied Java Bean by {@linkplain #get(String,
   * Set, Converter, BiFunction) acquiring setting values} named after
   * the supplied {@link PropertyDescriptor}s and using their
   * {@linkplain PropertyDescriptor#getWriteMethod() affiliated write
   * methods} to set the corresponding values.
   *
   * <p>This implementation calls the {@link #configure(Object,
   * Iterable, String, Set)} method with sensible defaults.</p>
   *
   * @param object the {@link Object} to configure; must not be {@code null}
   *
   * @param beanInfo a {@link BeanInfo} {@linkplain
   * BeanInfo#getPropertyDescriptors() providing access to
   * <code>PropertyDescriptor</code>s}; must not be {@code null}
   *
   * @param qualifiers a {@link Set} of {@link Annotation}s to further
   * qualify the selection of values; may be {@code null}; if
   * non-{@code null} then this parameter value must be safe for
   * concurrent iteration by multiple threads
   *
   * @exception NullPointerException if either {@code object} or {@code
   * beanInfo} is {@code null}
   *
   * @exception ReflectiveOperationException if there was a problem
   * invoking a {@linkplain PropertyDescriptor#getWriteMethod() write
   * method}; the supplied {@link Object} may, in this case, be left
   * in an inconsistent state
   *
   * @exception IllegalArgumentException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason; see {@link Converter#convert(Value)}; the supplied {@link
   * Object} may, in this case, be left in an inconsistent state
   *
   * @exception ConversionException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason other than bad inputs; the supplied {@link Object} may, in
   * this case, be left in an inconsistent state
   *
   * @exception ValueAcquisitionException if there was a procedural
   * problem acquiring a value; the supplied {@link Object} may, in
   * this case, be left in an inconsistent state
   *
   * @exception ArbitrationException if there was a problem performing
   * value arbitration; the supplied {@link Object} may, in this case,
   * be left in an inconsistent state
   *
   * @exception AmbiguousValuesException if arbitration completed but
   * could not resolve an ambiguity between potential return values;
   * the supplied {@link Object} may, in this case, be left in an
   * inconsistent state
   *
   * @exception MalformedValuesException if the {@link
   * #handleMalformedValues(String, Set, Collection)} method was
   * overridden and the override throws a {@link
   * MalformedValuesException}; the supplied {@link Object} may, in
   * this case, be left in an inconsistent state
   *
   * @exception ELException if there was an error related to
   * expression language parsing or evaluation; the supplied {@link
   * Object} may, in this case, be left in an inconsistent state
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency No guarantees are made with respect to the
   * idempotency of this method.
   *
   * @see #configure(Object, Iterable, String, Set)
   */
  public final void configure(final Object object,
                              final BeanInfo beanInfo,
                              final Set<Annotation> qualifiers)
    throws ReflectiveOperationException {
    this.configure(object,
                   Arrays.asList(beanInfo.getPropertyDescriptors()),
                   null,
                   qualifiers);
  }

  /**
   * Configures the supplied Java Bean by {@linkplain #get(String,
   * Set, Converter, BiFunction) acquiring setting values} named after
   * the supplied {@link PropertyDescriptor}s and using their
   * {@linkplain PropertyDescriptor#getWriteMethod() affiliated write
   * methods} to set the corresponding values.
   *
   * <p>This implementation calls the {@link #configure(Object,
   * Iterable, String, Set)} method with sensible defaults.</p>
   *
   * @param object the {@link Object} to configure; must not be {@code null}
   *
   * @param beanInfo a {@link BeanInfo} {@linkplain
   * BeanInfo#getPropertyDescriptors() providing access to
   * <code>PropertyDescriptor</code>s}; must not be {@code null}
   *
   * @param prefix a {@link String} that will be prepended to each
   * {@linkplain PropertyDescriptor#getName()
   * <code>PropertyDescriptor</code> name} before using the result as
   * the name of a setting for which a value {@linkplain #get(String,
   * Set, Converter, BiFunction) will be acquired}; may be {@code
   * null}
   *
   * @param qualifiers a {@link Set} of {@link Annotation}s to further
   * qualify the selection of values; may be {@code null}; if
   * non-{@code null} then this parameter value must be safe for
   * concurrent iteration by multiple threads
   *
   * @exception NullPointerException if either {@code object} or {@code
   * beanInfo} is {@code null}
   *
   * @exception ReflectiveOperationException if there was a problem
   * invoking a {@linkplain PropertyDescriptor#getWriteMethod() write
   * method}; the supplied {@link Object} may, in this case, be left
   * in an inconsistent state
   *
   * @exception IllegalArgumentException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason; see {@link Converter#convert(Value)}; the supplied {@link
   * Object} may, in this case, be left in an inconsistent state
   *
   * @exception ConversionException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason other than bad inputs; the supplied {@link Object} may, in
   * this case, be left in an inconsistent state
   *
   * @exception ValueAcquisitionException if there was a procedural
   * problem acquiring a value; the supplied {@link Object} may, in
   * this case, be left in an inconsistent state
   *
   * @exception ArbitrationException if there was a problem performing
   * value arbitration; the supplied {@link Object} may, in this case,
   * be left in an inconsistent state
   *
   * @exception AmbiguousValuesException if arbitration completed but
   * could not resolve an ambiguity between potential return values;
   * the supplied {@link Object} may, in this case, be left in an
   * inconsistent state
   *
   * @exception MalformedValuesException if the {@link
   * #handleMalformedValues(String, Set, Collection)} method was
   * overridden and the override throws a {@link
   * MalformedValuesException}; the supplied {@link Object} may, in
   * this case, be left in an inconsistent state
   *
   * @exception ELException if there was an error related to
   * expression language parsing or evaluation; the supplied {@link
   * Object} may, in this case, be left in an inconsistent state
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency No guarantees are made with respect to the
   * idempotency of this method.
   *
   * @see #configure(Object, Iterable, String, Set)
   */
  public final void configure(final Object object,
                              final BeanInfo beanInfo,
                              final String prefix,
                              final Set<Annotation> qualifiers)
    throws ReflectiveOperationException {
    this.configure(object,
                   Arrays.asList(beanInfo.getPropertyDescriptors()),
                   prefix,
                   qualifiers);
  }

  /**
   * Configures the supplied Java Bean by {@linkplain #get(String,
   * Set, Converter, BiFunction) acquiring setting values} named after
   * the supplied {@link PropertyDescriptor}s and using their
   * {@linkplain PropertyDescriptor#getWriteMethod() affiliated write
   * methods} to set the corresponding values.
   *
   * <p>This implementation calls the {@link #configure(Object,
   * Iterable, String, Set)} method with sensible defaults.</p>
   *
   * @param object the {@link Object} to configure; must not be {@code null}
   *
   * @param propertyDescriptors an {@link Iterable} of {@link
   * PropertyDescriptor}s; must not be {@code null}
   *
   * @exception NullPointerException if either {@code object} or {@code
   * propertyDescriptors} is {@code null}
   *
   * @exception ReflectiveOperationException if there was a problem
   * invoking a {@linkplain PropertyDescriptor#getWriteMethod() write
   * method}; the supplied {@link Object} may, in this case, be left
   * in an inconsistent state
   *
   * @exception IllegalArgumentException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason; see {@link Converter#convert(Value)}; the supplied {@link
   * Object} may, in this case, be left in an inconsistent state
   *
   * @exception ConversionException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason other than bad inputs; the supplied {@link Object} may, in
   * this case, be left in an inconsistent state
   *
   * @exception ValueAcquisitionException if there was a procedural
   * problem acquiring a value; the supplied {@link Object} may, in
   * this case, be left in an inconsistent state
   *
   * @exception ArbitrationException if there was a problem performing
   * value arbitration; the supplied {@link Object} may, in this case,
   * be left in an inconsistent state
   *
   * @exception AmbiguousValuesException if arbitration completed but
   * could not resolve an ambiguity between potential return values;
   * the supplied {@link Object} may, in this case, be left in an
   * inconsistent state
   *
   * @exception MalformedValuesException if the {@link
   * #handleMalformedValues(String, Set, Collection)} method was
   * overridden and the override throws a {@link
   * MalformedValuesException}; the supplied {@link Object} may, in
   * this case, be left in an inconsistent state
   *
   * @exception ELException if there was an error related to
   * expression language parsing or evaluation; the supplied {@link
   * Object} may, in this case, be left in an inconsistent state
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency No guarantees are made with respect to the
   * idempotency of this method.
   *
   * @see #configure(Object, Iterable, String, Set)
   */
  public final void configure(final Object object,
                              final Iterable<? extends PropertyDescriptor> propertyDescriptors)
    throws ReflectiveOperationException {
    this.configure(object,
                   propertyDescriptors,
                   null,
                   this.qualifiers);
  }

   /**
   * Configures the supplied Java Bean by {@linkplain #get(String,
   * Set, Converter, BiFunction) acquiring setting values} named after
   * the supplied {@link PropertyDescriptor}s and using their
   * {@linkplain PropertyDescriptor#getWriteMethod() affiliated write
   * methods} to set the corresponding values.
   *
   * <p>This implementation calls the {@link #configure(Object,
   * Iterable, String, Set)} method with sensible defaults.</p>
   *
   * @param object the {@link Object} to configure; must not be {@code null}
   *
   * @param propertyDescriptors an {@link Iterable} of {@link
   * PropertyDescriptor}s; must not be {@code null}
   *
   * @param prefix a {@link String} that will be prepended to each
   * {@linkplain PropertyDescriptor#getName()
   * <code>PropertyDescriptor</code> name} before using the result as
   * the name of a setting for which a value {@linkplain #get(String,
   * Set, Converter, BiFunction) will be acquired}; may be {@code
   * null}
   *
   * @exception NullPointerException if either {@code object} or {@code
   * propertyDescriptors} is {@code null}
   *
   * @exception ReflectiveOperationException if there was a problem
   * invoking a {@linkplain PropertyDescriptor#getWriteMethod() write
   * method}; the supplied {@link Object} may, in this case, be left
   * in an inconsistent state
   *
   * @exception IllegalArgumentException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason; see {@link Converter#convert(Value)}; the supplied {@link
   * Object} may, in this case, be left in an inconsistent state
   *
   * @exception ConversionException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason other than bad inputs; the supplied {@link Object} may, in
   * this case, be left in an inconsistent state
   *
   * @exception ValueAcquisitionException if there was a procedural
   * problem acquiring a value; the supplied {@link Object} may, in
   * this case, be left in an inconsistent state
   *
   * @exception ArbitrationException if there was a problem performing
   * value arbitration; the supplied {@link Object} may, in this case,
   * be left in an inconsistent state
   *
   * @exception AmbiguousValuesException if arbitration completed but
   * could not resolve an ambiguity between potential return values;
   * the supplied {@link Object} may, in this case, be left in an
   * inconsistent state
   *
   * @exception MalformedValuesException if the {@link
   * #handleMalformedValues(String, Set, Collection)} method was
   * overridden and the override throws a {@link
   * MalformedValuesException}; the supplied {@link Object} may, in
   * this case, be left in an inconsistent state
   *
   * @exception ELException if there was an error related to
   * expression language parsing or evaluation; the supplied {@link
   * Object} may, in this case, be left in an inconsistent state
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency No guarantees are made with respect to the
   * idempotency of this method.
   *
   * @see #configure(Object, Iterable, String, Set)
   */
  public final void configure(final Object object,
                              final Iterable<? extends PropertyDescriptor> propertyDescriptors,
                              final String prefix)
    throws ReflectiveOperationException {
    this.configure(object,
                   propertyDescriptors,
                   prefix,
                   this.qualifiers);
  }

  /**
   * Configures the supplied Java Bean by {@linkplain #get(String,
   * Set, Converter, BiFunction) acquiring setting values} named after
   * the supplied {@link PropertyDescriptor}s and using their
   * {@linkplain PropertyDescriptor#getWriteMethod() affiliated write
   * methods} to set the corresponding values.
   *
   * <p>For each {@link PropertyDescriptor} reachable from the
   * supplied {@link Iterable}:</p>
   *
   * <ul>
   *
   * <li>A setting name is synthesized by concatenating the value of
   * the supplied {@code prefix} (or the empty string if the supplied
   * {@code prefix} is {@code null}) and the return value of the
   * {@link PropertyDescriptor#getName()} method.</li>
   *
   * <li>The {@link PropertyDescriptor} is {@linkplain
   * PropertyDescriptor#getWriteMethod() interrogated for a write
   * method}.  If there is no write method, processing stops.</li>
   *
   * <li>A {@link Type} is acquired for the (writable) property in
   * question by first seeing if it has a {@code propertyType}
   * {@linkplain PropertyDescriptor#getValue(String) attribute} set to
   * a {@link Type} or a {@link TypeLiteral} (very uncommon) and then
   * by using the return value of its {@link
   * PropertyDescriptor#getPropertyType()} method.</li>
   *
   * <li>A {@link Converter} is acquired by first synthesizing one
   * wrapped around the return value of the {@link
   * PropertyDescriptor#createPropertyEditor(Object)} method, if that
   * method returns a non-{@code null} value, and then by {@linkplain
   * ConverterProvider#getConverter(Type) retrieving one normally}
   * otherwise.</li>
   *
   * <li>A default value is sought as the {@link String}-typed value
   * of the {@link PropertyDescriptor}'s {@code defaultValue}
   * {@linkplain PropertyDescriptor#getValue(String) attribute}.  If
   * the value of that attribute is {@code null} or not a {@link
   * String} (very common) then no default value will be used.</li>
   *
   * <li>The {@link #get(String, Set, Converter, BiFunction)} method
   * is called.  If the method throws a {@link
   * NoSuchElementException}, then processing stops.</li>
   *
   * <li>The return value from the previous step is passed to an
   * invocation of the {@link PropertyDescriptor}'s {@linkplain
   * PropertyDescriptor#getWriteMethod() write method} on the supplied
   * {@code object}.</li>
   *
   * </ul>
   *
   * <p>The net effect is that only writable Java Bean properties for
   * which there is a setting value will be set to that value.</p>
   *
   * @param object the {@link Object} to configure; must not be {@code null}
   *
   * @param propertyDescriptors an {@link Iterable} of {@link
   * PropertyDescriptor}s; must not be {@code null}
   *
   * @param prefix a {@link String} that will be prepended to each
   * {@linkplain PropertyDescriptor#getName()
   * <code>PropertyDescriptor</code> name} before using the result as
   * the name of a setting for which a value {@linkplain #get(String,
   * Set, Converter, BiFunction) will be acquired}; may be {@code
   * null}
   *
   * @param qualifiers a {@link Set} of {@link Annotation}s to further
   * qualify the selection of values; may be {@code null}; if
   * non-{@code null} then this parameter value must be safe for
   * concurrent iteration by multiple threads
   *
   * @exception NullPointerException if either {@code object} or {@code
   * propertyDescriptors} is {@code null}
   *
   * @exception ReflectiveOperationException if there was a problem
   * invoking a {@linkplain PropertyDescriptor#getWriteMethod() write
   * method}; the supplied {@link Object} may, in this case, be left
   * in an inconsistent state
   *
   * @exception IllegalArgumentException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason; see {@link Converter#convert(Value)}; the supplied {@link
   * Object} may, in this case, be left in an inconsistent state
   *
   * @exception ConversionException if {@linkplain
   * Converter#convert(Value) conversion} could not occur for any
   * reason other than bad inputs; the supplied {@link Object} may, in
   * this case, be left in an inconsistent state
   *
   * @exception ValueAcquisitionException if there was a procedural
   * problem acquiring a value; the supplied {@link Object} may, in
   * this case, be left in an inconsistent state
   *
   * @exception ArbitrationException if there was a problem performing
   * value arbitration; the supplied {@link Object} may, in this case,
   * be left in an inconsistent state
   *
   * @exception AmbiguousValuesException if arbitration completed but
   * could not resolve an ambiguity between potential return values;
   * the supplied {@link Object} may, in this case, be left in an
   * inconsistent state
   *
   * @exception MalformedValuesException if the {@link
   * #handleMalformedValues(String, Set, Collection)} method was
   * overridden and the override throws a {@link
   * MalformedValuesException}; the supplied {@link Object} may, in
   * this case, be left in an inconsistent state
   *
   * @exception ELException if there was an error related to
   * expression language parsing or evaluation; the supplied {@link
   * Object} may, in this case, be left in an inconsistent state
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency No guarantees are made with respect to the
   * idempotency of this method.
   */
  public final void configure(final Object object,
                              final Iterable<? extends PropertyDescriptor> propertyDescriptors,
                              final String prefix,
                              final Set<Annotation> qualifiers)
    throws ReflectiveOperationException {
    Objects.requireNonNull(object);
    Objects.requireNonNull(propertyDescriptors);
    for (final PropertyDescriptor pd : propertyDescriptors) {
      if (pd != null) {
        final String name = pd.getName();
        if (name != null) {
          final Method writeMethod = pd.getWriteMethod();
          if (writeMethod != null) {

            final String settingName;
            if (prefix == null) {
              settingName = name;
            } else {
              settingName = new StringBuilder(prefix).append(name).toString();
            }

            final Type type;
            final Object typeObject = pd.getValue("propertyType");
            if (typeObject instanceof Type) {
              type = (Type)typeObject;
            } else if (typeObject instanceof TypeLiteral) {
              type = ((TypeLiteral<?>)typeObject).getType();
            } else {
              type = pd.getPropertyType();
            }
            assert type != null;

            final Converter<?> converter;
            final PropertyEditor propertyEditor = pd.createPropertyEditor(object);
            if (propertyEditor != null && type instanceof Class) {
              converter = new PropertyEditorConverter<Object>((Class<?>)type, propertyEditor);
            } else {
              converter = this.converterProvider.getConverter(type);
            }

            final BiFunction<? super String, ? super Set<? extends Annotation>, ? extends String> defaultValueFunction;
            final Object defaultValue = pd.getValue("defaultValue");
            if (defaultValue instanceof String) {
              defaultValueFunction = (n, qs) -> (String)defaultValue;
            } else {
              defaultValueFunction = null;
            }

            try {
              writeMethod.invoke(object, this.get(settingName, qualifiers, converter, defaultValueFunction));
            } catch (final NoSuchElementException noSuchElementException) {
              // That's fine
            }

          }
        }
      }
    }
  }

  //----------------------------------------------------------------------------
  
  /**
   * Performs <em>value arbitration</em> on a {@link Collection} of
   * {@link Value}s that this {@link Settings} instance determined
   * were indistinguishable during value acquisition, and returns the
   * {@link Value} to be used instead (normally drawn from the {@link
   * Collection} according to some heuristic).
   *
   * <p>This implementation {@linkplain Iterable#iterator() iterates}
   * over the {@link Arbiter} instances {@linkplain #Settings(Set,
   * BiFunction, ConverterProvider, Iterable) supplied at construction
   * time} and asks each in turn to {@linkplain Arbiter#arbitrate(Set,
   * String, Set, Collection) perform value arbitration}.  The first
   * non-{@code null} value from an {@link Arbiter} is used as the
   * return value from this method; otherwise {@code null} is
   * returned.</p>
   *
   * @param sources the {@link Set} of {@link Source}s in effect
   * during the current value acquisition operation; must not be
   * {@code null}; must be {@linkplain
   * Collections#unmodifiableSet(Set) unmodifiable}; must be safe for
   * concurrent read-only access by multiple threads
   *
   * @param name the name of the setting value being sought; must not
   * be {@code null}
   *
   * @param qualifiers the {@link Set} of qualifier {@link
   * Annotation}s in effect during the current value acquisition
   * operation; must not be {@code null}; must be {@linkplain
   * Collections#unmodifiableSet(Set) unmodifiable}; must be safe for
   * concurrent read-only access by multiple threads
   *
   * @param values the {@link Collection} of {@link Value}s acquired
   * during the current value acquisition operation that were deemed
   * to be indistinguishable; must not be {@code null}; must be
   * {@linkplain Collections#unmodifiableSet(Set) unmodifiable}; must
   * be safe for concurrent read-only access by multiple threads
   *
   * @return the result of value arbitration as a single {@link
   * Value}, or {@code null} if arbitration could not select a single
   * {@link Value}
   *
   * @see Arbiter
   *
   * @see Arbiter#arbitrate(Set, String, Set, Collection)
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency No guarantees of any kind are made with respect to
   * the idempotency of this method.
   *
   * @nullability This method may return {@code null}.
   */
  protected Value arbitrate(final Set<? extends Source> sources,
                            final String name,
                            final Set<Annotation> qualifiers,
                            final Collection<? extends Value> values) {
    final Value returnValue;
    Value temp = null;
    final Iterable<? extends Arbiter> arbiters = this.arbiters;
    if (arbiters == null) {
      returnValue = null;
    } else {
      for (final Arbiter arbiter : arbiters) {
        if (arbiter != null) {
          temp = arbiter.arbitrate(sources, name, qualifiers, values);
          if (temp != null) {
            break;
          }
        }
      }
      returnValue = temp;
    }
    return returnValue;
  }

  /**
   * Given a {@link Source}, a name of a setting and a (possibly
   * {@code null}) {@link Set} of qualifying {@link Annotation}s,
   * returns a {@link Value} for the supplied {@code name} originating
   * from the supplied {@link Source}.
   *
   * <p>The default implementation of this method calls the {@link
   * Source#getValue(String, Set)} method on the supplied {@link
   * Source}, passing the remaining arguments to it, and returns its
   * result.</p>
   *
   * @param source the {@link Source} of the {@link Value} to be
   * returned; must not be {@code null}
   *
   * @param name the name of the setting for which a {@link Value} is
   * to be returned; must not be {@code null}
   *
   * @param qualifiers an {@link Collections#unmodifiableSet(Set)
   * unmodifiable} {@link Set} of qualifying {@link Annotation}s; may
   * be {@code null}
   *
   * @return an appropriate {@link Value}, or {@code null}
   *
   * @exception NullPointerException if {@code source} or {@code name}
   * is {@code null}
   *
   * @exception UnsupportedOperationException if an override of this
   * method attempts to modify the {@code qualifiers} parameter
   *
   * @exception ValueAcquisitionException if there was an exceptional
   * problem acquiring a {@link Value}, but not in the relatively
   * common case that an appropriate {@link Value} could not be
   * located
   *
   * @nullability This method and its overrides are permitted to
   * return {@code null}.
   *
   * @idempotency No guarantees of any kind are made with respect to
   * the idempotency of this method or its overrides.
   *
   * @threadsafety This method is and its overrides must be safe for
   * concurrent use by multiple threads.
   *
   * @see Source#getValue(String, Set)
   */
  protected Value getValue(final Source source, final String name, final Set<Annotation> qualifiers) {
    final Value returnValue;
    if (source == this) {
      returnValue = null;
    } else {
      returnValue = source.getValue(name, qualifiers);
    }
    return returnValue;
  }

  /**
   * Processes a {@link Collection} of {@link Value} instances that
   * were determined to be malformed in some way during the execution
   * of a {@link #get(String, Set, Converter, BiFunction)} operation.
   *
   * <p>The default implementation of this method does nothing.
   * Overrides may consider throwing a {@link
   * MalformedValuesException} instead.</p>
   *
   * <p>Overrides of this method should not call any of this {@link
   * Settings} instance's other methods (especially {@link
   * #get(String, Set, Converter, BiFunction)}) as undefined behavior
   * or infinite loops may result.</p>
   *
   * <p>{@link Value} instances in the supplied {@link Collection} of
   * {@link Value} instances will be discarded after this method
   * completes and are for informational purposes only.</p>
   *
   * @param name the name of the configuration setting for which a
   * {@link Value} is being retrieved by an invocation of the {@link
   * #get(String, Set, Converter, BiFunction)} method; must not be
   * {@code null}
   *
   * @param qualifiers an {@linkplain Collections#unmodifiableSet(Set)
   * unmodifiable} {@link Set} of qualifier {@link Annotation}s
   * qualifying the value retrieval operation; may be {@code null}
   *
   * @param badValues a non-{@code null} {@linkplain
   * Collections#unmodifiableCollection(Collection) unmodifiable}
   * {@link Collection} of {@link Value}s that have been determined to
   * be malformed in some way
   *
   * @exception NullPointerException if {@code name} or {@code
   * badValues} is {@code null}
   *
   * @exception UnsupportedOperationException if an override attempts
   * to modify either of the {@code qualifiers} or the {@code
   * badValues} parameters
   *
   * @exception MalformedValuesException if processing should abort
   *
   * @threadsafety This method is and its overrides must be safe for
   * concurrent use by multiple threads.
   *
   * @idempotency No guarantees of any kind are made with respect to
   * the idempotency of this method or its overrides.
   *
   * @see #get(String, Set, Converter, BiFunction)
   *
   * @see Value
   */
  protected void handleMalformedValues(final String name, final Set<Annotation> qualifiers, final Collection<? extends Value> badValues) {

  }

  private final String interpolate(final String value,
                                   final ELContext elContext,
                                   final ExpressionFactory expressionFactory,
                                   final Set<Annotation> qualifiers) {
    Objects.requireNonNull(elContext);
    Objects.requireNonNull(expressionFactory);
    final String returnValue;
    if (value == null) {
      returnValue = null;
    } else {
      String temp = null;
      try {
        final ValueExpression valueExpression = expressionFactory.createValueExpression(elContext, value, String.class);
        assert valueExpression != null;
        temp = String.class.cast(valueExpression.getValue(elContext));
      } finally {
        returnValue = temp;
      }
    }
    return returnValue;
  }


  /*
   * Static methods.
   */


  /*
   * Inner and nested classes.
   */


  private static final class Key {

    private final String name;

    private final Set<Annotation> qualifiers;

    private Key(final String name, final Set<Annotation> qualifiers) {
      super();
      this.name = Objects.requireNonNull(name);
      if (qualifiers == null || qualifiers.isEmpty()) {
        this.qualifiers = Collections.emptySet();
      } else {
        this.qualifiers = Collections.unmodifiableSet(new HashSet<>(qualifiers));
      }
    }

    private final String getName() {
      return this.name;
    }

    private final Set<Annotation> getQualifiers() {
      return this.qualifiers;
    }

    @Override
    public final int hashCode() {
      int hashCode = 17;
      final Object name = this.getName();
      int c = name == null ? 0 : name.hashCode();
      hashCode = 37 * hashCode + c;
      final Collection<?> qualifiers = this.getQualifiers();
      c = qualifiers == null || qualifiers.isEmpty() ? 0 : qualifiers.hashCode();
      hashCode = 37 * hashCode + c;
      return hashCode;
    }

    @Override
    public boolean equals(final Object other) {
      if (other == this) {
        return true;
      } else if (other instanceof Key) {
        final Key her = (Key)other;
        final Object name = this.getName();
        if (name == null) {
          if (her.getName() != null) {
            return false;
          }
        } else if (!name.equals(her.getName())) {
          return false;
        }
        final Collection<?> qualifiers = this.getQualifiers();
        if (qualifiers == null || qualifiers.isEmpty()) {
          final Collection<?> herQualifiers = her.getQualifiers();
          if (herQualifiers != null && !herQualifiers.isEmpty()) {
            return false;
          }
        } else if (!qualifiers.equals(her.getQualifiers())) {
          return false;
        }
        return true;
      } else {
        return false;
      }
    }

  }

  private static final class SourceELResolver extends ELResolver {

    private static final Set<String> MAGIC_NAMES;

    static {
      MAGIC_NAMES = new HashSet<>();
      MAGIC_NAMES.add("s");
      MAGIC_NAMES.add("settings");
    }

    private final Settings settings;

    private final ExpressionFactory expressionFactory;

    private final Set<Annotation> qualifiers;

    private SourceELResolver(final Settings settings,
                             final Set<Annotation> qualifiers) {
      this(settings,
           ExpressionFactory.newInstance(),
           qualifiers);
    }

    private SourceELResolver(final Settings settings,
                             final ExpressionFactory expressionFactory,
                             final Set<Annotation> qualifiers) {
      super();
      this.settings = Objects.requireNonNull(settings);
      this.expressionFactory = expressionFactory;
      if (qualifiers == null) {
        this.qualifiers = Collections.emptySet();
      } else {
        this.qualifiers = Collections.unmodifiableSet(qualifiers);
      }
    }

    @Override
    public final Class<?> getCommonPropertyType(final ELContext elContext, final Object base) {
      return Object.class;
    }

    @Override
    public final Iterator<FeatureDescriptor> getFeatureDescriptors(final ELContext elContext, final Object base) {
      return Collections.emptyIterator();
    }

    @Override
    public final boolean isReadOnly(final ELContext elContext, final Object base, final Object property) {
      if (elContext != null && (property instanceof String || property instanceof Settings)) {
        elContext.setPropertyResolved(true);
      }
      return true;
    }

    @Override
    public final Class<?> getType(final ELContext elContext, final Object base, final Object property) {
      Objects.requireNonNull(elContext);
      Class<?> returnValue = null;
      if (base == null) {
        if (MAGIC_NAMES.contains(property)) {
          elContext.setPropertyResolved(true);
          returnValue = this.settings.getClass();
        }
      } else if (base instanceof Settings && property instanceof String) {
        final Settings settings = (Settings)base;
        final String value =
          settings.get((String)property,
                       this.qualifiers,
                       elContext,
                       this.expressionFactory,
                       settings.converterProvider.getConverter(String.class),
                       null);
        elContext.setPropertyResolved(true);
        if (value == null) {
          throw new PropertyNotFoundException((String)property);
        }
        returnValue = String.class;
      }
      // Note that as currently written returnValue may be null.
      return returnValue;
    }

    @Override
    public final Object getValue(final ELContext elContext, final Object base, final Object property) {
      Objects.requireNonNull(elContext);
      Object returnValue = null;
      if (base == null) {
        if (MAGIC_NAMES.contains(property)) {
          elContext.setPropertyResolved(true);
          returnValue = this.settings;
        }
      } else if (base instanceof Settings && property instanceof String) {
        final Settings settings = (Settings)base;
        final String value =
          settings.get((String)property,
                       this.qualifiers,
                       elContext,
                       this.expressionFactory,
                       settings.converterProvider.getConverter(String.class),
                       null);
        elContext.setPropertyResolved(true);
        if (value == null) {
          throw new PropertyNotFoundException((String)property);
        }
        returnValue = value;
      }
      // Note that as currently written returnValue may be null.
      return returnValue;
    }

    @Override
    public final void setValue(final ELContext elContext, final Object base, final Object property, final Object value) {
      if (elContext != null) {
        elContext.setPropertyResolved(false);
      }
    }

  }

}
