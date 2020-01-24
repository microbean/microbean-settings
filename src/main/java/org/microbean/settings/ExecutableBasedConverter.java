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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.util.Objects;

public class ExecutableBasedConverter<T> implements Converter<T> {

  private static final long serialVersionUID = 1L;

  private transient Executable executable;

  public ExecutableBasedConverter(final Method method) {
    super();
    this.executable = Objects.requireNonNull(method);
    if (!Modifier.isStatic(method.getModifiers())) {
      throw new IllegalArgumentException("method is not static: " + method);
    }
    final Class<?>[] parameterTypes = method.getParameterTypes();
    if (parameterTypes == null || parameterTypes.length < 1 || !(CharSequence.class.isAssignableFrom(parameterTypes[0]))) {
      throw new IllegalArgumentException("method does not take a single CharSequence-assignable parameter: " + method);
    }
  }

  public ExecutableBasedConverter(final Constructor<T> constructor) {
    super();
    this.executable = Objects.requireNonNull(constructor);
    final Class<?>[] parameterTypes = constructor.getParameterTypes();
    if (parameterTypes == null || parameterTypes.length < 1 || !(CharSequence.class.isAssignableFrom(parameterTypes[0]))) {
      throw new IllegalArgumentException("constructor does not take a single CharSequence-assignable parameter: " + constructor);
    }
  }

  @Override
  public final T convert(final Value value) {
    final T returnValue;
    if (value == null) {
      returnValue = null;
    } else {
      final String stringValue = value.get();
      if (stringValue == null) {
        returnValue = null;
      } else {
        T convertedObject = null;
        try {
          if (this.executable instanceof Method) {
            @SuppressWarnings("unchecked")
            final T invocationResult = (T)((Method)this.executable).invoke(null, stringValue);
            convertedObject = invocationResult;
          } else {
            assert this.executable instanceof Constructor;
            @SuppressWarnings("unchecked")
            final T invocationResult = ((Constructor<T>)this.executable).newInstance(stringValue);
            convertedObject = invocationResult;
          }
        } catch (final ReflectiveOperationException reflectiveOperationException) {
          throw new IllegalArgumentException(stringValue, reflectiveOperationException);
        } finally {
          returnValue = convertedObject;
        }
      }
    }
    return returnValue;
  }

  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    if (in != null) {
      in.defaultReadObject();
      final boolean constructor = in.readBoolean();
      final Class<?> declaringClass = (Class<?>)in.readObject();
      assert declaringClass != null;
      final String methodName;
      if (constructor) {
        methodName = null;
      } else {
        methodName = in.readUTF();
        assert methodName != null;
      }
      final Class<?>[] parameterTypes = (Class<?>[])in.readObject();
      assert parameterTypes != null;
      try {
        if (constructor) {
          this.executable = declaringClass.getDeclaredConstructor(parameterTypes);
        } else {
          this.executable = declaringClass.getMethod(methodName, parameterTypes);
        }
      } catch (final ReflectiveOperationException reflectiveOperationException) {
        throw new IOException(reflectiveOperationException.getMessage(), reflectiveOperationException);
      }
      assert this.executable != null;
    }
  }

  private void writeObject(final ObjectOutputStream out) throws IOException {
    if (out != null) {
      out.defaultWriteObject();
      assert this.executable != null;
      final boolean constructor = this.executable instanceof Constructor;
      out.writeBoolean(constructor); // true means Constructor
      out.writeObject(this.executable.getDeclaringClass());
      if (!constructor) {
        out.writeUTF(this.executable.getName());
      }
      out.writeObject(this.executable.getParameterTypes());
    }
  }

}
