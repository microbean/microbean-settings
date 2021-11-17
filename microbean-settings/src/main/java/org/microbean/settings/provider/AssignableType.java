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

import java.util.Objects;

import org.microbean.type.CovariantTypeSemantics;
import org.microbean.type.TypeSemantics;

/**
 * A {@link Type} that captures covariant assignability rules.
 *
 * @param type the {@link Type} to which assignability tests will be
 * applied; must not be {@code null}
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see CovariantTypeSemantics
 */
public record AssignableType(Type type) implements Type {


  /*
   * Static fields.
   */


  private static final TypeSemantics covariantTypeSemantics = new CovariantTypeSemantics(false);


  /**
   * Creates a new {@link AssignableType}.
   *
   * @param type the {@link Type} to which assignability tests will be
   * applied; must not be {@code null}
   *
   * @exception NullPointerException if {@code type} is {@code null}
   */
  public AssignableType {
    Objects.requireNonNull(type, "type");
  }


  /*
   * Instance methods.
   */


  /**
   * Returns a type name for this {@link AssignableType}.
   *
   * @return a type name for this {@link AssignableType}; never {@code
   * null}
   *
   * @nullability This method never returns {@code null} so long as
   * the {@link Type} returned by the {@link #type()} method does not
   * return {@code null} from its {@link Type#getTypeName()} method.
   *
   * @idempotency This method is idempotent and deterministic so long
   * as the {@link Type} returned by the {@link #type()} method's
   * {@link Type#getTypeName()} implementation is idempotent and
   * deterministic.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads so long as the {@link Type} returned by the {@link
   * #type()} method's {@link Type#getTypeName()} implementation is
   * safe for concurrent use by multiple threads.
   */
  @Override // Type
  public final String getTypeName() {
    return this.type().getTypeName();
  }

  /**
   * Returns {@code true} if and only if a reference bearing the
   * supplied {@link Type} {@linkplain
   * CovariantTypeSemantics#isAssignable(Type, Type) can be assigned}
   * to a reference bearing the return value of the {@link #type()}
   * method according to {@linkplain CovariantTypeSemantics covariant
   * type semantics}.
   *
   * @param payload the {@link Type} to test; must not be {@code null}
   *
   * @return {@code true} if and only if a reference bearing the
   * supplied {@link Type} can be assigned to a reference bearing the
   * return value of the {@link #type()} method
   *
   * @exception NullPointerException if {@code payload} is {@code
   * null}
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   */
  public final boolean isAssignable(final Type payload) {
    return
      covariantTypeSemantics.isAssignable(this.type(),
                                          payload instanceof AssignableType a ? a.type() : Objects.requireNonNull(payload));
  }

  /**
   * Returns a non-{@code null} {@link String} representation of this
   * {@link AssignableType}.
   *
   * @return a non-{@code null} {@link String} representation of this
   * {@link AssignableType}
   *
   * @nullability This method never returns {@code null} so long as
   * the {@link Type} returned by the {@link #type()} method does not
   * return {@code null} from its {@link Object#toString() toString()}
   * method.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads so long as the {@link Type} returned by the {@link
   * #type()} method's {@link Object#toString() toString()} method is
   * safe for concurrent use by multiple threads.
   *
   * @idempotency This method is idempotent and deterministic so long
   * as the {@link Type} returned by the {@link #type()} method's
   * {@link Object#toString() toString()} method is idempotent and
   * deterministic.
   */
  @Override // Object
  public final String toString() {
    return this.type().toString();
  }


  /*
   * Static methods.
   */


  /**
   * Returns the supplied {@link AssignableType}.
   *
   * @param type the {@link AssignableType} to return; may be {@code
   * null}
   *
   * @return the supplied {@link AssignableType}
   *
   * @nullability This method will return {@code null} if {@code type}
   * is {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   */
  public static final AssignableType of(final AssignableType type) {
    return type;
  }

  /**
   * Returns an {@link AssignableType} suitable for the supplied
   * {@link Type}.
   *
   * <p>If the supplied {@link Type} is an {@link AssignableType} then
   * the {@link #of(AssignableType)} method is invoked and its return
   * value is returned.</p>
   *
   * <p>If the supplied {@link Type} is not an {@link AssignableType},
   * then a new {@link AssignableType} {@linkplain
   * #AssignableType(Type) is created} and returned.</p>
   *
   * @param type the {@link Type} in question; must not be {@code null}
   *
   * @return an {@link AssignableType}; never {@code null}
   *
   * @exception NullPointerException if {@code type} is {@code null}
   *
   * @nullability This method never returns {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   */
  public static final AssignableType of(final Type type) {
    return type instanceof AssignableType a ? of(a) : new AssignableType(type);
  }

}
