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
  public boolean isSelectable(final Context context);

  public Value<?> get(final Context context);

  public record Value<T>(T value) implements Supplier<T> {

    public Value() {
      this(null);
    }
    
    @Override
    public final T get() {
      return this.value();
    }

    public static final <T> Value<T> of() {
      return new Value<>();
    }

    public static final <T> Value<T> of(final T value) {
      return new Value<>(value);
    }

  }

}
