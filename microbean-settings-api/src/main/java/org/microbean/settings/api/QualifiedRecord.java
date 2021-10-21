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

import java.util.function.Function;

public record QualifiedRecord<T>(Qualifiers qualifiers, T qualified) implements Qualified<T> {

  @Override
  public final String toString() {
    return Qualified.toString(this);
  }

  public final String toString(final Function<? super T, ? extends String> f) {
    return Qualified.toString(this, f);
  }

  public static final <T> QualifiedRecord<T> of(final Qualified<T> q) {
    return q instanceof QualifiedRecord<T> qr ? qr : new QualifiedRecord<>(q.qualifiers(), q.qualified());
  }

  public static final <T> QualifiedRecord<T> of(final T qualified) {
    return of(Qualifiers.of(), qualified);
  }

  public static final <T> QualifiedRecord<T> of(final Qualifiers qualifiers, final T qualified) {
    return new QualifiedRecord<>(qualifiers, qualified);
  }
  
}

