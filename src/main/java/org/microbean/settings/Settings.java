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

import java.beans.FeatureDescriptor;

import java.lang.annotation.Annotation;

import java.lang.reflect.Type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.PropertyNotFoundException;
import javax.el.StandardELContext;
import javax.el.ValueExpression;

import javax.enterprise.util.TypeLiteral;

public class Settings {

  private static final Comparator<Value> valueComparator = Comparator.<Value>comparingInt(v -> v.getQualifiers().size()).reversed();

  private final ConverterProvider converterProvider;

  private final BiFunction<? super String,
                           ? super Set<Annotation>,
                           ? extends Set<? extends Source>> sourcesSupplier;

  private final Iterable<? extends Arbiter> arbiters;

  public Settings() {
    super();

    final Set<Source> sources = new LinkedHashSet<>();
    sources.add(new SystemPropertiesSource());
    sources.add(new EnvironmentVariablesSource());
    this.sourcesSupplier = (name, qualifiers) -> Collections.unmodifiableSet(sources);

    this.converterProvider = new Converters();

    this.arbiters = Collections.singleton(new SourceOrderArbiter());
  }
  
  public Settings(final BiFunction<? super String,
                                   ? super Set<Annotation>,
                                   ? extends Set<? extends Source>> sourcesSupplier,
                  final ConverterProvider converterProvider,
                  final Iterable<? extends Arbiter> arbiters) {
    super();
    if (sourcesSupplier == null) {
      this.sourcesSupplier = (name, qualifiers) -> Collections.emptySet();
    } else {
      this.sourcesSupplier = sourcesSupplier;
    }
    this.converterProvider = Objects.requireNonNull(converterProvider);
    if (arbiters == null) {
      this.arbiters = Collections.emptySet();
    } else {
      this.arbiters = arbiters;
    }
  }

  public final String get(final String name,
                          final Set<Annotation> qualifiers,
                          final Supplier<? extends String> defaultValueSupplier) {
    return this.get(name,
                    qualifiers,
                    this.converterProvider.getConverter(String.class),
                    defaultValueSupplier);
  }

  public final <T> T get(final String name,
                         final Set<Annotation> qualifiers,
                         final Class<T> cls,
                         final Supplier<? extends String> defaultValueSupplier) {
    return this.get(name,
                    qualifiers,
                    this.converterProvider.getConverter(cls),
                    defaultValueSupplier);
  }

  public final <T> T get(final String name,
                         final Set<Annotation> qualifiers,
                         final TypeLiteral<T> typeLiteral,
                         final Supplier<? extends String> defaultValueSupplier) {
    return this.get(name,
                    qualifiers,
                    this.converterProvider.getConverter(typeLiteral),
                    defaultValueSupplier);
  }

  public final Object get(final String name,
                          final Set<Annotation> qualifiers,
                          final Type type,
                          final Supplier<? extends String> defaultValueSupplier) {
    return this.get(name,
                    qualifiers,
                    this.converterProvider.getConverter(type),
                    defaultValueSupplier);
  }

  public final <T> T get(final String name,
                         final Set<Annotation> qualifiers,
                         final Converter<? extends T> converter,
                         final Supplier<? extends String> defaultValueSupplier) {
    final ExpressionFactory expressionFactory = ExpressionFactory.newInstance();
    final StandardELContext elContext = new StandardELContext(expressionFactory);
    elContext.addELResolver(new SourceELResolver(this, expressionFactory, qualifiers));
    return this.get(Objects.requireNonNull(name),
                    qualifiers,
                    elContext,
                    expressionFactory,
                    Objects.requireNonNull(converter),
                    defaultValueSupplier);
  }

  private final <T> T get(final String name,
                          final Set<Annotation> qualifiers,
                          final ELContext elContext,
                          final ExpressionFactory expressionFactory,
                          final Converter<? extends T> converter,
                          final Supplier<? extends String> rawDefaultValueSupplier) {
    return Objects.requireNonNull(converter).convert(this.getValue(name,
                                                                   qualifiers,
                                                                   elContext,
                                                                   expressionFactory,
                                                                   rawDefaultValueSupplier));
  }

  private final Value getValue(final String name,
                               Set<Annotation> qualifiers,
                               final ELContext elContext,
                               final ExpressionFactory expressionFactory,
                               final Supplier<? extends String> rawDefaultValueSupplier) {
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

    final Set<? extends Source> sources = this.sourcesSupplier.apply(name, qualifiers);
    if (sources != null) {
      for (final Source source : sources) {
        if (source != null) {

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
    valuesToArbitrate = null;

    final String stringToInterpolate;
    if (selectedValue == null) {
      if (rawDefaultValueSupplier == null) {
        // There was no value that came up.  There's also no way to
        // get a default one.  So the value is missing.
        throw new NoSuchElementException(name + " (" + qualifiers + ")");
      } else {
        stringToInterpolate = rawDefaultValueSupplier.get();
      }
    } else {
      stringToInterpolate = selectedValue.get();
    }
    final String interpolatedString = this.interpolate(stringToInterpolate, elContext, expressionFactory, qualifiers);
    if (selectedValue == null) {
      selectedValue = new Value(null /* no Source; we synthesized this Value */, name, qualifiers, interpolatedString);
    } else {
      selectedValue = new Value(selectedValue, interpolatedString);
    }
    return selectedValue;
  }

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
    return source.getValue(name, qualifiers);
  }

  /**
   * Processes a {@link Collection} of {@link Value} instances that
   * were determined to be malformed in some way during the execution
   * of a {@link #get(String, Set, Converter, Supplier)} operation.
   *
   * <p>The default implementation of this method does nothing.
   * Overrides may consider throwing a {@link
   * MalformedValuesException} instead.</p>
   *
   * <p>{@link Value} instances in the supplied {@link Collection} of
   * {@link Value} instances will be discarded after this method
   * completes and are for informational purposes only.</p>
   *
   * @param name the name of the configuration setting for which a
   * {@link Value} is being retrieved by an invocation of the {@link
   * #get(String, Set, Converter, Supplier)} method; must not be
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
   * @see #get(String, Set, Converter, Supplier)
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
        if ("settings".equals(property)) {
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
        if ("settings".equals(property)) {
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
