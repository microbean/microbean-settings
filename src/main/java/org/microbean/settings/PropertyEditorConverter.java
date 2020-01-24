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

import java.io.IOException;
import java.io.ObjectInputStream;

import java.util.Objects;

public final class PropertyEditorConverter<T> implements Converter<T> {

  private static final long serialVersionUID = 1L;

  private final Class<T> conversionClass;

  private transient PropertyEditor editor;

  public PropertyEditorConverter(final Class<T> conversionClass, final PropertyEditor editor) {
    super();
    this.conversionClass = Objects.requireNonNull(conversionClass);
    if (editor == null) {
      this.editor = PropertyEditorManager.findEditor(conversionClass);
    } else {
      this.editor = editor;
    }
  }

  @Override
  public final T convert(final Value value) {
    final T returnValue;
    if (value == null) {
      return null;
    } else {
      final String stringValue = value.get();
      if (stringValue == null) {
        returnValue = null;
      } else if (this.editor == null) {
        throw new IllegalArgumentException("No PropertyEditor available to convert " + stringValue);
      } else {
        synchronized (this.editor) {
          editor.setAsText(stringValue);
          T result = null;
          try {
            @SuppressWarnings("unchecked")
            final T temp = (T)editor.getValue();
            result = temp;
          } catch (final ClassCastException classCastException) {
            throw new IllegalArgumentException(stringValue, classCastException);
          } finally {
            returnValue = result;
          }
        }
      }
    }
    return returnValue;
  }

  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    if (in != null) {
      in.defaultReadObject();
      this.editor = PropertyEditorManager.findEditor(conversionClass);
    }
  }

}
