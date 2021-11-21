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
package org.microbean.settings.provider;

import java.lang.reflect.Type;

import java.util.List;

import java.util.function.BiPredicate;

import org.microbean.development.annotation.Experimental;

import org.microbean.settings.api.Configured;
import org.microbean.settings.api.Path;
import org.microbean.settings.api.Path.Element;
import org.microbean.settings.api.Qualifiers;

/**
 * An interface whose implementations handle various kinds of
 * ambiguity that arise when a {@link Configured} seeks configured
 * objects by way of various {@link Provider}s.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 */
@Proxy(false)
public interface AmbiguityHandler {

  /**
   * Called to notify this {@link AmbiguityHandler} that a {@link
   * Provider} was discarded during the search for a configured
   * object.
   *
   * <p>The default implementation of this method does nothing.</p>
   *
   * @param rejector the {@link Configured} that rejected the {@link
   * Provider}; must not be @{code null}
   *
   * @param absolutePath the {@linkplain Path#isAbsolute() absolute
   * <code>Path</code>} for which a configured object is being sought;
   * must not be {@code null}
   *
   * @param provider the rejected {@link Provider}, which may be
   * {@code null}
   *
   * @exception NullPointerException if either {@code rejector} or
   * {@code absolutePath} is {@code null}
   */
  public default void providerRejected(final Configured<?> rejector, final Path<?> absolutePath, final Provider provider) {

  }

  /**
   * Called to notify this {@link AmbiguityHandler} that a {@link
   * Value} provided by a {@link Provider} was discarded during the
   * search for a configured object.
   *
   * <p>The default implementation of this method does nothing.</p>
   *
   * @param rejector the {@link Configured} that rejected the {@link
   * Provider}; must not be @{code null}
   *
   * @param absolutePath the {@linkplain Path#isAbsolute() absolute
   * <code>Path</code>} for which a configured object is being sought;
   * must not be {@code null}
   *
   * @param provider the {@link Provider} providing the rejected
   * value; must not be {@code null}
   *
   * @param value the rejected {@link Value}; must not be {@code null}
   *
   * @exception NullPointerException if any argument is {@code null}
   */
  public default void valueRejected(final Configured<?> rejector, final Path<?> absolutePath, final Provider provider, final Value<?> value) {

  }

  public default int score(final Qualifiers referenceQualifiers, final Qualifiers valueQualifiers) {
    final int intersectionSize = referenceQualifiers.intersectionSize(valueQualifiers);
    if (intersectionSize > 0) {
      return
        intersectionSize == valueQualifiers.size() ?
        intersectionSize :
        intersectionSize - referenceQualifiers.symmetricDifferenceSize(valueQualifiers);
    } else {
      return -(referenceQualifiers.size() + valueQualifiers.size());
    }
  }

  /**
   * Returns a score indicating the relative specificity of {@code
   * valuePath} with respect to {@code absoluteReferencePath}, or
   * {@link Integer#MIN_VALUE} if {@code valuePath} is wholly
   * unsuitable for further consideration or processing.
   *
   * <p>This is <em>not</em> a comparison method.</p>
   *
   * <p>The following preconditions must hold or undefined behavior
   * will result:</p>
   *
   * <ul>
   *
   * <li>Neither parameter's value may be {@code null}.</li>
   *
   * <li>{@code absoluteReferencePath} must be {@linkplain
   * Path#isAbsolute() absolute}
   *
   * <li>{@code valuePath} must be <em>selectable</em> with respect to
   * <code>absoluteReferencePath</code>, where the definition of
   * selectability is described below</li>
   *
   * </ul>
   *
   * <p>For {@code valuePath} to "be selectable" with respect to
   * {@code absoluteReferencePath} for the purposes of this method and
   * for no other purpose, {@code true} must be returned by a
   * hypothetical invocation of code whose behavior is that of the
   * following:</p>
   *
   * <blockquote><pre>absoluteReferencePath.endsWith(valuePath, {@link
   * ElementsMatchBiPredicate#INSTANCE
   * ElementsMatchBiPredicate.INSTANCE});</pre></blockquote>
   * 
   * <p>Note that such an invocation is <em>not</em> made by the
   * default implementation of this method, but logically precedes it
   * when this method is called in the natural course of events by the
   * {@link Configured#of(Path)} method.</p>
   *
   * <p>If, during scoring, {@code valuePath} is found to be wholly
   * unsuitable for further consideration or processing, {@link
   * Integer#MIN_VALUE} will be returned to indicate this.  Overrides
   * must follow suit or undefined behavior elsewhere in this
   * framework will result.</p>
   *
   * @param absoluteReferencePath the {@link Path} against which to
   * score the supplied {@code valuePath}; must not be {@code null};
   * must adhere to the preconditions above
   *
   * @param valuePath the {@link Path} to score against the supplied
   * {@code absoluteReferencePath}; must not be {@code null}; must
   * adhere to the preconditions above
   *
   * @return a relative score for {@code valuePath} with respect to
   * {@code absoluteReferencePath}; meaningless on its own
   * <em>unless</em> it is {@link Integer#MIN_VALUE} in which case the
   * supplied {@code valuePath} will be treated as wholly unsuitable
   * for further consideration or processing
   *
   * @exception NullPointerException if either parameter is {@code
   * null}
   *
   * @exception IllegalArgumentException if certain preconditions have
   * been violated
   *
   * @see Configured#of(Path)
   *
   * @threadsafety The default implementation of this method is, and
   * its overrides must be, safe for concurrent use by multiple
   * threads.
   *
   * @idempotency The default implementation of this method is, and
   * its overrides must be, idempotent and deterministic.
   * Specifically, the same score is and must be returned whenever
   * this method is invoked with the same {@link Path}s.
   */
  @Experimental
  public default int score(final Path<?> absoluteReferencePath, final Path<?> valuePath) {
    if (!absoluteReferencePath.isAbsolute()) {
      throw new IllegalArgumentException("absoluteReferencePath: " + absoluteReferencePath);
    }

    final int lastValuePathIndex = absoluteReferencePath.lastIndexOf(valuePath, ElementsMatchBiPredicate.INSTANCE);
    assert lastValuePathIndex >= 0 : "absoluteReferencePath: " + absoluteReferencePath + "; valuePath: " + valuePath;
    assert lastValuePathIndex + valuePath.size() == absoluteReferencePath.size() : "absoluteReferencePath: " + absoluteReferencePath + "; valuePath: " + valuePath;

    int score = valuePath.size();
    for (int valuePathIndex = 0; valuePathIndex < valuePath.size(); valuePathIndex++) {
      final int referencePathIndex = lastValuePathIndex + valuePathIndex;

      final Element<?> referenceElement = absoluteReferencePath.get(referencePathIndex);
      final Element<?> valueElement = valuePath.get(valuePathIndex);
      if (!referenceElement.name().equals(valueElement.name())) {
        return Integer.MIN_VALUE;
      }

      final Type referenceType = referenceElement.type().orElse(null);
      final Type valueType = valueElement.type().orElse(null);
      if (referenceType == null) {
        if (valueType != null) {
          return Integer.MIN_VALUE;
        }
      } else if (valueType == null) {
        return Integer.MIN_VALUE;
      } else if (referenceType.equals(valueType)) {
        ++score;
      } else if (!AssignableType.of(referenceType).isAssignable(valueType)) {
        return Integer.MIN_VALUE;
      }

      final List<Class<?>> referenceParameters = referenceElement.parameters().orElse(null);
      final List<Class<?>> valueParameters = valueElement.parameters().orElse(null);
      if (referenceParameters == null) {
        if (valueParameters != null) {
          return Integer.MIN_VALUE;
        }
      } else if (valueParameters == null || referenceParameters.size() != valueParameters.size()) {
        return Integer.MIN_VALUE;
      }

      final List<String> referenceArguments = referenceElement.arguments().orElse(null);
      final List<String> valueArguments = valueElement.arguments().orElse(null);
      if (referenceArguments == null) {
        if (valueArguments != null) {
          return Integer.MIN_VALUE;
        }
      } else if (valueArguments == null) {
        // The value is indifferent with respect to arguments. It
        // *could* be suitable but not *as* suitable as one that
        // matched.  Don't adjust the score.
      } else {
        final int referenceArgsSize = referenceArguments.size();
        final int valueArgsSize = valueArguments.size();
        if (referenceArgsSize < valueArgsSize) {
          // The value path is unsuitable because it provided too
          // many arguments.
          return Integer.MIN_VALUE;
        } else if (referenceArguments.equals(valueArguments)) {
          score += referenceArgsSize;
        } else if (referenceArgsSize == valueArgsSize) {
          // Same sizes, but different arguments.  The value is not suitable.
          return Integer.MIN_VALUE;
        } else if (valueArgsSize == 0) {
          // The value is indifferent with respect to arguments. It
          // *could* be suitable but not *as* suitable as one that
          // matched.  Don't adjust the score.
        } else {
          // The reference element had, say, two arguments, and the
          // value had, say, one.  We treat this as a mismatch.
          return Integer.MIN_VALUE;
        }
      }
    }
    return score;
  }

  /**
   * Given two {@link Value}s and some contextual objects, chooses one
   * over the other and returns it, or synthesizes a new {@link Value}
   * and returns that, or indicates that disambiguation is impossible
   * by returning {@code null}.
   *
   * @param <U> the type of objects the {@link Value}s in question can
   * supply
   *
   * @param requestor the {@link Configured} currently seeking a
   * {@link Value}; must not be {@code null}
   *
   * @param absolutePath an {@linkplain Path#isAbsolute() absolute
   * <code>Path</code>} for which a value is being sought; must not be
   * {@code null}
   *
   * @param p0 the {@link Provider} that supplied the first {@link
   * Value}; must not be {@code null}
   *
   * @param v0 the first {@link Value}; must not be {@code null}
   *
   * @param p1 the {@link Provider} that supplied the second {@link
   * Value}; must not be {@code null}
   *
   * @param v1 the second {@link Value}; must not be {@code null}
   *
   * @return the {@link Value} to use instead; ordinarily one of the
   * two supplied {@link Value}s but may be {@code null} to indicate
   * that disambiguation was impossible, or an entirely different
   * {@link Value} altogether
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @nullability The default implementation of this method and its
   * overrides may return {@code null}.
   *
   * @threadsafety The default implementation of this method is, and
   * its overrides must be, safe for concurrent use by multiple
   * threads.
   *
   * @idempotency The default implementation of this method is, and
   * its overrides must be, idempotent. The default implementation of
   * this method is deterministic, but its overrides need not be.
   */
  public default <U> Value<U> disambiguate(final Configured<?> requestor,
                                           final Path<?> absolutePath,
                                           final Provider p0,
                                           final Value<U> v0,
                                           final Provider p1,
                                           final Value<U> v1) {
    return null;
  }

  @Experimental
  public static final class ElementsMatchBiPredicate implements BiPredicate<Element<?>, Element<?>> {

    public static final ElementsMatchBiPredicate INSTANCE = new ElementsMatchBiPredicate();

    private ElementsMatchBiPredicate() {
      super();
    }

    @Override // BiPredicate<Element<?>, Element<?>>
    public final boolean test(final Element<?> e1, final Element<?> e2) {
      final String name1 = e1.name();
      final String name2 = e2.name();
      if (!name1.isEmpty() && !name2.isEmpty() && !name1.equals(name2)) {
        // Empty names have special significance in that they "match"
        // any other name.
        return false;
      }

      final Type t1 = e1.type().orElse(null);
      final Type t2 = e2.type().orElse(null);
      if (t1 != null && t2 != null && !AssignableType.of(t1).isAssignable(t2)) {
        return false;
      }

      final List<Class<?>> p1 = e1.parameters().orElse(null);
      final List<Class<?>> p2 = e2.parameters().orElse(null);
      if (p1 != null && p2 != null) {
        if (p1.size() != p2.size()) {
          return false;
        } else {
          for (int i = 0; i < p1.size(); i++) {
            if (!p1.get(i).isAssignableFrom(p2.get(i))) {
              return false;
            }
          }
        }
      }

      return true;
    }

  }

}
