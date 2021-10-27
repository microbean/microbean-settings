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

import java.lang.reflect.Type;

import java.util.function.Supplier;

public interface Provider {

  public default Type upperBound() {
    return Object.class;
  }

  /**
   * Returns {@code true} if this {@link Provider} could potentially
   * be appropriate or relevant for the supplied {@link Context}.
   * Further selection may happen that rules out this {@link
   * Provider}, even if it returns {@code true} from this method.  If
   * it returns {@code false} from this method, no further selection
   * will happen.
   *
   * @param context the {@link Context} representing demand; must not
   * be {@code null}
   *
   * @return {@code true} if this {@link Provider} thinks it is
   * capable of satisfying the demand represented by the supplied
   * {@link Context}; {@code false} if it absolutely cannot do so
   */
  @Deprecated(forRemoval = true)
  public boolean isSelectable(final Context context);

  public boolean isSelectable(final Qualified<? extends Context2> context);

  @Deprecated(forRemoval = true)
  public Value<?> get(final Context context);

  public Value2<?> get(final Qualified<? extends Context2> context);


  /*
   * Inner and nested classes.
   */


  /**
   * A value returned by a {@link Provider} that is a {@link Supplier}
   * of some underlying, possibly {@code null}, value.
   *
   * @param <T> the type the value bears
   *
   * @param path a {@link Path} or path fragment qualifying the value
   * this {@link Value} represents
   *
   * @param value the value itself; may be and often is {@code null}
   *
   * @author <a href="https://about.me/lairdnelson"
   * target="_parent">Laird Nelson</a>
   */
  @Deprecated(forRemoval = true)
  public record Value<T>(Path path, T value) implements Supplier<T> {

    public Value() {
      this(null, null);
    }

    public Value(final Path path) {
      this(path, null);
    }

    public Value(final T value) {
      this(null, value);
    }

    public Value {
      if (path != null && value != null && !AssignableType.of(path.type()).isAssignable(value.getClass())) {
        throw new IllegalArgumentException("value: " + value);
      }
    }

    @Override
    public final T get() {
      return this.value();
    }

    public static final <T> Value<T> of() {
      return new Value<>();
    }

    public static final <T> Value<T> of(final Path path) {
      return new Value<>(path);
    }

    public static final <T> Value<T> of(final T value) {
      return new Value<>(value);
    }

    public static final <T> Value<T> of(final Path path, final T value) {
      return new Value<>(path, value);
    }

  }

  public record Value2<T>(Qualified<? extends PathFragment> qualifiedPath, T value) implements Supplier<T> {

    public Value2() {
      this(null, null);
    }

    public Value2(final Qualified<? extends PathFragment> qualifiedPath) {
      this(qualifiedPath, null);
    }
    
    public Value2(final PathFragment path) {
      this(Qualified.Record.of(path), null);
    }

    public Value2(final T value) {
      this(null, value);
    }

    public Value2 {
      if (qualifiedPath != null && value != null && !AssignableType.of(qualifiedPath.qualified().type()).isAssignable(value.getClass())) {
        throw new IllegalArgumentException("value: " + value);
      }
    }

    @Override
    public final T get() {
      return this.value();
    }

    public final Qualifiers qualifiers() {
      final Qualified<? extends PathFragment> qualifiedPath = this.qualifiedPath();
      return qualifiedPath == null ? Qualifiers.of() : qualifiedPath.qualifiers();
    }
    
    public final PathFragment path() {
      final Qualified<? extends PathFragment> qualifiedPath = this.qualifiedPath();
      return qualifiedPath == null ? null : qualifiedPath.qualified();
    }
    
    public final Type type() {
      final Qualified<? extends PathFragment> qualifiedPath = this.qualifiedPath();
      return qualifiedPath == null ? null : qualifiedPath.qualified().type();
    }

  }

}
