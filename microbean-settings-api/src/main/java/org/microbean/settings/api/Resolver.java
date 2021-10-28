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

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import java.util.function.Consumer;

import org.microbean.settings.api.Provider.Value;

public interface Resolver {

  public static final Resolver DEFAULT = new Resolver() {};

  public default <T> Value<T> resolve(final Collection<? extends Provider> providers,
                                      Qualifiers contextQualifiers,
                                      final Context<?> context,
                                      Consumer<? super Provider> rejectedProviders,
                                      Consumer<? super Value<?>> rejectedValues,
                                      Disambiguator disambiguator,
                                      Consumer<? super Value<?>> ambiguousValues) {
    Objects.requireNonNull(context, "context");
    if (contextQualifiers == null) {
      contextQualifiers = Qualifiers.of();
    }
    if (rejectedProviders == null) {
      rejectedProviders = Resolver::sink;
    }
    if (rejectedValues == null) {
      rejectedValues = Resolver::sink;
    }
    if (disambiguator == null) {
      disambiguator = Disambiguator.DEFAULT;
    }
    if (ambiguousValues == null) {
      ambiguousValues = Resolver::sink;
    }
    if (providers == null || providers.isEmpty()) {
      return null;
    } else if (providers.size() == 1) {
      final Provider provider = providers instanceof List<? extends Provider> list ? list.get(0) : providers.iterator().next();
      if (provider == null) {
        return null;
      } else if (!isSelectable(contextQualifiers, context, provider)) {
        rejectedProviders.accept(provider);
        return null;
      } else {
        final Value<?> value = provider.get(contextQualifiers, context);
        if (value == null) {
          return null;
        } else if (!isSelectable(contextQualifiers, context, value)) {
          rejectedValues.accept(value);
          return null;
        } else {
          @SuppressWarnings("unchecked")
          final Value<T> v = (Value<T>)value;
          return v;
        }
      }
    } else {
      Value<?> candidate = null;
      Provider candidateProvider = null;
      int candidatePathSize = Integer.MIN_VALUE;
      int candidateScore = Integer.MIN_VALUE;
      for (final Provider provider : providers) {
        if (provider != null && isSelectable(contextQualifiers, context, provider)) {
          Value<?> value = provider.get(contextQualifiers, context);
          while (value != null) { // NOTE
            if (isSelectable(contextQualifiers, context.path(), value)) {
              if (candidate == null) {
                candidate = value;
                candidateProvider = provider;
                candidatePathSize = candidate.path().size();
                candidateScore = contextQualifiers.relativeScore(candidate.qualifiers());
                value = null;
              } else {
                final Path valuePath = value.path();
                // Let's do qualifiers first.  This is an arbitrary decision.
                final int valueScore = contextQualifiers.relativeScore(value.qualifiers());
                if (valueScore < candidateScore) {
                  rejectedValues.accept(value);
                  value = null;
                } else if (valueScore == candidateScore) {
                  final int valuePathSize = valuePath.size();
                  if (valuePathSize < candidatePathSize) {
                    rejectedValues.accept(value);
                    value = null;
                  } else if (valuePathSize == candidatePathSize) {
                    final Value<?> disambiguatedValue =
                      disambiguator.disambiguate(contextQualifiers, context, candidateProvider, candidate, provider, value);
                    if (disambiguatedValue == null) {
                      ambiguousValues.accept(candidate);
                      ambiguousValues.accept(value);
                      value = null;
                      // TODO: I'm not sure whether to null the
                      // candidate bits and potentially grab another
                      // less suitable one, keep the existing one even
                      // though it's ambiguous, or, if we keep it, to
                      // break or continue.  For now I'm going to keep
                      // it and continue.
                    } else if (disambiguatedValue.equals(candidate)) {
                      rejectedValues.accept(value);
                      value = null;
                    } else if (disambiguatedValue.equals(value)) {
                      rejectedValues.accept(candidate);
                      candidate = disambiguatedValue;
                      candidateProvider = provider;
                      candidatePathSize = valuePathSize;
                      candidateScore = valueScore;
                      value = null;
                    } else {
                      value = disambiguatedValue; // NOTE; this will run through the while loop again
                    }
                  } else {
                    rejectedValues.accept(candidate);
                    candidate = value;
                    candidateProvider = provider;
                    candidatePathSize = valuePathSize;
                    candidateScore = valueScore;
                    value = null;
                  }
                } else {
                  rejectedValues.accept(candidate);
                  candidate = value;
                  candidateProvider = provider;
                  candidatePathSize = valuePath.size();
                  candidateScore = valueScore;
                  value = null;
                }
              }
            } else {
              rejectedValues.accept(value);
              value = null;
            }
          }
        } else {
          rejectedProviders.accept(provider);
        }
      }
      @SuppressWarnings("unchecked")
      final Value<T> c = (Value<T>)candidate;
      return c;
    }
  }

  public static boolean isSelectable(final Qualifiers contextQualifiers,
                                     final Context<?> context,
                                     final Provider provider) {
    return
      context.isAssignable(provider.upperBound()) &&
      provider.isSelectable(contextQualifiers, context);
  }

  public static boolean isSelectable(final Qualifiers contextQualifiers,
                                     final Context<?> context,
                                     final Value<?> value) {
    return
      isSelectable(contextQualifiers,
                   context.path(),
                   value.qualifiers(),
                   value.path());
  }

  public static boolean isSelectable(final Qualifiers contextQualifiers,
                                     final Path contextPath,
                                     final Value<?> value) {
    return
      isSelectable(contextQualifiers,
                   contextPath,
                   value.qualifiers(),
                   value.path());
  }

  public static boolean isSelectable(final Qualifiers contextQualifiers,
                                     final Path contextPath,
                                     final Qualifiers valuePathQualifiers,
                                     final Path valuePath) {
    if (AssignableType.of(contextPath.type()).isAssignable(valuePath.type())) {
      if (contextQualifiers.isEmpty() ||
          valuePathQualifiers.isEmpty() ||
          contextQualifiers.intersectionSize(valuePathQualifiers) > 0) {
        // TODO: this is just endsWith()
        final int lastIndex = contextPath.lastIndexOf(valuePath);
        return lastIndex >= 0 && lastIndex + valuePath.size() == contextPath.size();
      }
    }
    return false;
  }

  private static void sink(final Object ignored) {

  }

}
