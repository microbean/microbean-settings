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
import java.lang.reflect.TypeVariable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import java.util.function.Supplier;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.PropertyNotFoundException;
import javax.el.StandardELContext;
import javax.el.ValueExpression;

import javax.enterprise.util.TypeLiteral;

public class Settings {

  public static final TypeLiteral<String> stringTypeLiteral = new TypeLiteral<String>() {
      private static final long serialVersionUID = 1L;
    };

  public static final Converter<String> stringConverter = new Converter<String>() {
      @Override
      public final String convert(final Value value) {
        return value == null ? null : value.get();
      }
    };

  private static final Comparator<Value> valueComparator = Comparator.<Value>comparingInt(v -> v.getQualifiers().size()).reversed();

  private final Map<Type, Converter<?>> converters;

  private final Iterable<? extends Source> sources;

  private final Iterable<? extends Arbiter> arbiters;

  public Settings(final Iterable<? extends Source> sources,
                  final Iterable<? extends Arbiter> arbiters) {
    super();
    if (sources == null) {
      this.sources = null;
    } else if (sources instanceof Collection) {
      this.sources = new ArrayList<>((Collection<? extends Source>)sources);
    } else {
      final Collection<Source> c = new ArrayList<>();
      for (final Source source : sources) {
        if (source != null) {
          c.add(source);
        }
      }
      this.sources = c;
    }
    if (arbiters == null) {
      this.arbiters = null;
    } else if (arbiters instanceof Collection) {
      this.arbiters = new ArrayList<>((Collection<? extends Arbiter>)arbiters);
    } else {
      final Collection<Arbiter> c = new ArrayList<>();
      for (final Arbiter arbiter : arbiters) {
        if (arbiter != null) {
          c.add(arbiter);
        }
      }
      this.arbiters = c;
    }
    this.converters = new HashMap<>();
    this.putConverter(String.class, stringConverter);
  }

  public <T> T getValue(final String name,
                        Set<Annotation> qualifiers,
                        final Class<T> cls,
                        final Supplier<? extends String> defaultValueSupplier) {
    final Set<Source> activeSources = new HashSet<>();
    final ExpressionFactory expressionFactory = ExpressionFactory.newInstance();
    final StandardELContext elContext = new StandardELContext(expressionFactory);
    elContext.addELResolver(new SourceELResolver(this, expressionFactory, activeSources, qualifiers));
    return this.getValue(name,
                         qualifiers,
                         activeSources,
                         elContext,
                         expressionFactory,
                         this.getConverter(cls),
                         defaultValueSupplier);
  }
  
  public <T> T getValue(final String name,
                        Set<Annotation> qualifiers,
                        final TypeLiteral<T> typeLiteral,
                        final Supplier<? extends String> defaultValueSupplier) {
    final Set<Source> activeSources = new HashSet<>();
    final ExpressionFactory expressionFactory = ExpressionFactory.newInstance();
    final StandardELContext elContext = new StandardELContext(expressionFactory);
    elContext.addELResolver(new SourceELResolver(this, expressionFactory, activeSources, qualifiers));
    return this.getValue(name,
                         qualifiers,
                         activeSources,
                         elContext,
                         expressionFactory,
                         this.getConverter(typeLiteral),
                         defaultValueSupplier);
  }

  public <T> T getValue(final String name,
                        Set<Annotation> qualifiers,
                        final Converter<T> converter,
                        final Supplier<? extends String> defaultValueSupplier) {
    final Set<Source> activeSources = new HashSet<>();
    final ExpressionFactory expressionFactory = ExpressionFactory.newInstance();
    final StandardELContext elContext = new StandardELContext(expressionFactory);
    elContext.addELResolver(new SourceELResolver(this, expressionFactory, activeSources, qualifiers));
    return this.getValue(name,
                         qualifiers,
                         activeSources,
                         elContext,
                         expressionFactory,
                         converter,
                         defaultValueSupplier);
  }

  private final <T> T getValue(final String name,
                               Set<Annotation> qualifiers,
                               final Set<Source> activeSources,
                               final ELContext elContext,
                               final ExpressionFactory expressionFactory,
                               final Converter<T> converter,
                               final Supplier<? extends String> rawDefaultValueSupplier) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(activeSources);
    Objects.requireNonNull(elContext);
    Objects.requireNonNull(expressionFactory);
    Objects.requireNonNull(converter);

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

    if (sources != null) {
      for (final Source source : sources) {
        if (source != null) {

          final Value value;
          try {
            if (activeSources.contains(source)) {
              value = null;
            } else {
              activeSources.add(source);
              value = source.getValue(name, qualifiers);
            }
          } finally {
            activeSources.remove(source);
          }

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
                assert selectedValue == null;
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
    assert activeSources.isEmpty();

    if (badValues != null && !badValues.isEmpty()) {
      this.handleMalformedValues(name, qualifiers, badValues);
    }

    Collection<Value> valuesToArbitrate = null;

    if (selectedValue == null) {

      if (conflictingValues != null) {

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
            assert false : "valueSpecificity > highestSpecificitySoFarEncountered: " + valueSpecificity + " > " + highestSpecificitySoFarEncountered;
          }
        }
      }
    }

    if (selectedValue == null) {
      selectedValue = this.arbitrate(name, qualifiers, valuesToArbitrate == null || valuesToArbitrate.isEmpty() ? Collections.emptySet() : Collections.unmodifiableCollection(valuesToArbitrate));
    }
    valuesToArbitrate = null;

    final String stringToInterpolate;
    if (selectedValue == null) {
      if (rawDefaultValueSupplier == null) {
        stringToInterpolate = null;
      } else {
        stringToInterpolate = rawDefaultValueSupplier.get();
      }
    } else {
      stringToInterpolate = selectedValue.get();
    }
    final String interpolatedString =
      this.interpolate(stringToInterpolate,
                       elContext,
                       expressionFactory,
                       activeSources,
                       qualifiers);
    if (selectedValue == null) {
      selectedValue = new Value(null, name, qualifiers, interpolatedString);
    } else {
      selectedValue = new Value(selectedValue, interpolatedString);
    }    
    final T returnValue = converter.convert(selectedValue);
    return returnValue;
  }

  public <T> Converter<T> getConverter(final Class<T> cls) {
    Objects.requireNonNull(cls);
    @SuppressWarnings("unchecked")
    final Converter<T> returnValue = (Converter<T>)this.converters.get(cls);
    return returnValue;
  }
  
  public <T> Converter<T> getConverter(final TypeLiteral<T> typeLiteral) {
    Objects.requireNonNull(typeLiteral);
    final Type type = typeLiteral.getType();
    @SuppressWarnings("unchecked")
    final Converter<T> returnValue = (Converter<T>)this.converters.get(type);
    return returnValue;
  }

  public <T> Converter<T> putConverter(final Class<T> cls, final Converter<T> converter) {
    final Converter<T> returnValue;
    if (cls != null) {
      @SuppressWarnings("unchecked")
      final Converter<T> temp = (Converter<T>)this.converters.put(cls, converter);
      returnValue = temp;
    } else {
      returnValue = null;
    }
    return returnValue;
  }
  
  public <T> Converter<T> putConverter(final TypeLiteral<T> typeLiteral, final Converter<T> converter) {
    final Converter<T> returnValue;
    if (typeLiteral != null) {
      @SuppressWarnings("unchecked")
      final Converter<T> temp = (Converter<T>)this.converters.put(typeLiteral.getType(), converter);
      returnValue = temp;
    } else {
      returnValue = null;
    }
    return returnValue;
  }

  public <T> Converter<T> removeConverter(final Object key) {
    @SuppressWarnings("unchecked")
    final Converter<T> returnValue = (Converter<T>)this.converters.remove(key);
    return returnValue;
  }
  
  public String interpolate(final String value,
                            final Set<Annotation> qualifiers) {
    final Set<Source> activeSources = new HashSet<>();
    final ExpressionFactory expressionFactory = ExpressionFactory.newInstance();
    final StandardELContext elContext = new StandardELContext(expressionFactory);
    elContext.addELResolver(new SourceELResolver(this, expressionFactory, activeSources, qualifiers));
    return this.interpolate(value, elContext, expressionFactory, activeSources, qualifiers);
  }

  private final String interpolate(final String value,
                                   final ELContext elContext,
                                   final ExpressionFactory expressionFactory,
                                   final Set<Source> activeSources,
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

  protected Value arbitrate(final String name,
                            final Set<Annotation> qualifiers,
                            final Collection<? extends Value> values) {
    final Value returnValue;
    if (this.arbiters == null) {
      returnValue = null;
    } else {
      Value temp = null;
      for (final Arbiter arbiter : this.arbiters) {
        if (arbiter != null) {
          temp = arbiter.arbitrate(name, qualifiers, values);
          if (temp != null) {
            break;
          }
        }
      }
      returnValue = temp;
    }
    return returnValue;
  }

  protected Value getValue(final Source source, final String name, final Set<Annotation> qualifiers) {
    return source.getValue(name, qualifiers);
  }

  protected void handleMalformedValues(final String name, final Set<Annotation> qualifiers, final Collection<? extends Value> badValues) {

  }


  /*
   * Static methods.
   */


  /*
   * Inner and nested classes.
   */


  private static final class SourceELResolver extends ELResolver {

    private final Settings settings;

    private final ExpressionFactory expressionFactory;

    private final Set<Source> activeSources;

    private final Set<Annotation> qualifiers;

    private SourceELResolver(final Settings settings,
                             final Set<Annotation> qualifiers) {
      this(settings,
           ExpressionFactory.newInstance(),
           new HashSet<>(),
           qualifiers);
    }

    private SourceELResolver(final Settings settings,
                             final ExpressionFactory expressionFactory,
                             final Set<Source> activeSources,
                             final Set<Annotation> qualifiers) {
      super();
      this.settings = Objects.requireNonNull(settings);
      this.expressionFactory = expressionFactory;
      this.activeSources = activeSources;
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
        final String value =
          ((Settings)base).getValue((String)property,
                                    this.qualifiers,
                                    this.activeSources,
                                    elContext,
                                    this.expressionFactory,
                                    stringConverter,
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
        final String value =
          ((Settings)base).getValue((String)property,
                                    this.qualifiers,
                                    this.activeSources,
                                    elContext,
                                    this.expressionFactory,
                                    stringConverter,
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
