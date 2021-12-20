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
package org.microbean.settings.jackson;

import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

import java.util.Objects;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.microbean.development.annotation.Convenience;

import org.microbean.settings.provider.CachingSupplier;

import org.microbean.settings.api.Loader;
import org.microbean.settings.api.Path;

import org.microbean.settings.jackson.JacksonProvider;

public class InputStreamJacksonProvider<T> extends JacksonProvider<T> {

  private final BiFunction<? super Loader<?>, ? super Path<?>, ? extends ObjectCodec> objectCodecFunction;
  
  private final BiFunction<? super Loader<?>, ? super Path<?>, ? extends InputStream> inputStreamFunction;

  private final Consumer<? super InputStream> inputStreamReadConsumer;

  private InputStreamJacksonProvider() {
    super();
    throw new UnsupportedOperationException();
  }

  public InputStreamJacksonProvider(final Supplier<? extends ObjectMapper> mapperSupplier, final String resourceName) {
    this(objectCodecFunction(mapperSupplier),
         (c, p) -> inputStream(p.classLoader(), resourceName),
         InputStreamJacksonProvider::closeInputStream);
  }
  
  public InputStreamJacksonProvider(final BiFunction<? super Loader<?>, ? super Path<?>, ? extends ObjectCodec> objectCodecFunction,
                                    final BiFunction<? super Loader<?>, ? super Path<?>, ? extends InputStream> inputStreamFunction,
                                    final Consumer<? super InputStream> inputStreamReadConsumer) {
    super();
    this.objectCodecFunction = Objects.requireNonNull(objectCodecFunction, "objectCodecFunction");
    this.inputStreamFunction = Objects.requireNonNull(inputStreamFunction, "inputStreamFunction");
    this.inputStreamReadConsumer = Objects.requireNonNull(inputStreamReadConsumer, "inputStreamReadConsumer");
  }

  @Override // JacksonProvider<T>
  protected <T> ObjectCodec objectCodec(final Loader<?> requestor, final Path<T> absolutePath) {
    return this.objectCodecFunction.apply(requestor, absolutePath);
  }

  @Override // JacksonProvider<T>
  protected <T> TreeNode rootNode(final Loader<?> requestor, final Path<T> absolutePath, final ObjectCodec reader) {
    TreeNode returnValue = null;
    if (reader != null) {
      InputStream is = null;
      RuntimeException runtimeException = null;
      JsonParser parser = null;
      try {
        is = this.inputStreamFunction.apply(requestor, absolutePath);
        if (is != null) {
          parser = reader.getFactory().createParser(is);
          parser.setCodec(reader);
          returnValue = parser.readValueAsTree();
        }
      } catch (final IOException ioException) {
        runtimeException = new UncheckedIOException(ioException.getMessage(), ioException);
      } catch (final RuntimeException e) {
        runtimeException = e;
      } finally {
        try {
          if (parser != null) {
            parser.close();
          }
        } catch (final IOException ioException) {
          if (runtimeException == null) {
            runtimeException = new UncheckedIOException(ioException.getMessage(), ioException);
          } else {
            runtimeException.addSuppressed(ioException);
          }
        } catch (final RuntimeException e) {
          if (runtimeException == null) {
            runtimeException = e;
          } else {
            runtimeException.addSuppressed(e);
          }
        } finally {
          try {
            if (is != null) {
              this.inputStreamReadConsumer.accept(is);
            }
          } catch (final RuntimeException e) {
            if (runtimeException == null) {
              runtimeException = e;
            } else {
              runtimeException.addSuppressed(e);
            }
          } finally {
            if (runtimeException != null) {
              throw runtimeException;
            }
          }
        }
      }
    }
    return returnValue;
  }

  @Convenience
  protected static final InputStream inputStream(final ClassLoader cl, final String resourceName) {
    final InputStream returnValue;
    InputStream temp = cl == null ? ClassLoader.getSystemResourceAsStream(resourceName) : cl.getResourceAsStream(resourceName);
    if (temp == null) {
      try {
        temp = new BufferedInputStream(Files.newInputStream(Paths.get(System.getProperty("user.dir", "."), resourceName)));
      } catch (final FileNotFoundException /* this probably isn't thrown */ | NoSuchFileException e) {

      } catch (final IOException ioException) {
        throw new UncheckedIOException(ioException.getMessage(), ioException);
      } finally {
        returnValue = temp;
      }
    } else if (temp instanceof BufferedInputStream) {
      returnValue = (BufferedInputStream)temp;
    } else {
      returnValue = new BufferedInputStream(temp);
    }
    return returnValue;
  }

  protected static final void closeInputStream(final InputStream is) {
    if (is != null) {
      try {
        is.close();
      } catch (final IOException ioException) {
        throw new UncheckedIOException(ioException.getMessage(), ioException);
      }
    }
  }

  private static final <T extends ObjectMapper> BiFunction<? super Loader<?>, ? super Path<?>, ? extends ObjectCodec> objectCodecFunction(final Supplier<T> mapperSupplier) {
    if (mapperSupplier == null) {
      return InputStreamJacksonProvider::returnNull;
    } else {
      return (c, p) -> {
        // Note that otherwise potential infinite loops are handled in
        // the Settings class.
        final ObjectMapper mapper = c.of(ObjectMapper.class).orElseGet(mapperSupplier);
        if (mapper == null) {
          return null;
        }
        final JavaType javaType = mapper.constructType(p.type());
        return mapper.canDeserialize(javaType) ? mapper.readerFor(javaType) : null;
      };
    }
  }
  
  private static final <T> T returnNull(final Object ignored, final Object alsoIgnored) {
    return null;
  }
  
}
