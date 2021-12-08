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
import java.io.UncheckedIOException;

import java.lang.reflect.Type;

import java.util.Iterator;
import java.util.List;

import java.util.function.Supplier;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;

import com.fasterxml.jackson.core.type.TypeReference;

import org.microbean.settings.api.Configured;
import org.microbean.settings.api.Path;
import org.microbean.settings.api.Path.Element;
import org.microbean.settings.api.Qualifiers;

import org.microbean.settings.provider.AbstractProvider;
import org.microbean.settings.provider.Value;

public abstract class JacksonProvider<T> extends AbstractProvider<T> {


  /*
   * Static fields.
   */


  private static final Logger logger = Logger.getLogger(JacksonProvider.class.getName());


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link JacksonProvider}.
   */
  protected JacksonProvider() {
    super();
  }


  /*
   * Instance methods.
   */


  /**
   * Returns a {@link Value} suitable for the supplied {@link
   * Configured} and {@link Path}, or {@code null} if there is no such
   * {@link Value} now <strong>and if there never will be such a
   * {@link Value}</strong> for the supplied arguments.
   *
   * <p>The following assertions will be true when this method is
   * called in the normal course of events:</p>
   *
   * <ul>
   *
   * <li>{@code assert absolutePath.isAbsolute();}</li>
   *
   * <li>{@code assert
   * absolutePath.startsWith(requestor.absolutePath());}</li>
   *
   * <li>{@code assert
   * !absolutePath.equals(requestor.absolutePath());}</li>
   *
   * </ul>
   *
   * <p>This implementation first {@linkplain #objectCodec(Configured,
   * Path) acquires} an {@link ObjectCodec} and then {@linkplain
   * #rootNode(Configured, Path, ObjectCodec) uses it to acquire the
   * root <code>TreeNode</code> of a document}.  With this {@link
   * TreeNode} in hand, it treats the supplied {@code absolutePath} as
   * a series of names terminating in a type, much like, in principle,
   * <a href="https://datatracker.ietf.org/doc/html/rfc6901"
   * target="_parent">JSON Pointer</a>.</p>
   *
   * <p>TODO: FINISH</p>
   *
   * @param requestor the {@link Configured} seeking a {@link Value};
   * must not be {@code null}
   *
   * @param absolutePath an {@linkplain Path#isAbsolute() absolute
   * <code>Path</code>} for which the supplied {@link Configured} is
   * seeking a value; must not be {@code null}
   *
   * @return a {@link Value} more or less suitable for the combination
   * of the supplied {@link Configured} and {@link Path}, or {@code
   * null} if there is no such {@link Value} now <strong>and if there
   * never will be such a {@link Value}</strong> for the supplied
   * arguments
   *
   * @exception NullPointerException if either {@code requestor} or
   * {@code absolutePath} is {@code null}
   *
   * @exception IllegalArgumentException if {@code absolutePath}
   * {@linkplain Path#isAbsolute() is not absolute}
   *
   * @nullability Implementations of this method may return {@code
   * null}.
   *
   * @threadsafety Implementations of this method must be safe for
   * concurrent use by multiple threads.
   *
   * @idempotency Implementations of this method must be idempotent
   * but are not assumed to be deterministic.
   */
  @Override // AbstractProvider<Object>
  public final <T> Value<T> get(final Configured<?> requestor, final Path<T> absolutePath) {
    assert absolutePath.isAbsolute();
    assert absolutePath.startsWith(requestor.absolutePath());
    assert !absolutePath.equals(requestor.absolutePath());
    if (absolutePath.size() <= 1) {
      return null;
    }
    final ObjectCodec reader = this.objectCodec(requestor, absolutePath);
    if (reader == null) {
      return null;
    }
    TreeNode node = this.rootNode(requestor, absolutePath, reader);
    if (node == null) {
      return null;
    }
    final Iterator<Element<?>> elementIterator = absolutePath.iterator();
    elementIterator.next(); // skip the root element; we know size() > 1
    while (elementIterator.hasNext()) {
      final Element<?> element = elementIterator.next();
      if (elementIterator.hasNext()) {
        if (node.isArray()) {
          final List<String> arguments = element.arguments().orElse(null);
          if (arguments != null && arguments.size() == 1) {
            final Class<?> soleParameter = element.parameters().orElseThrow().get(0);
            if (soleParameter == int.class || soleParameter == Integer.class) {
              node = node.get(Integer.parseInt(arguments.get(0)));
            } else {
              node = null;
            }
          } else {
            node = null;
          }
        } else if (node.isObject()) {
          node = node.get(element.name());
        } else {
          assert !node.isContainerNode();
          node = null;
        }
        if (node == null) {
          return null;
        }
      } else if (node.isArray()) {
        final List<String> arguments = element.arguments().orElse(null);
        if (arguments != null && arguments.size() == 1) {
          final Class<?> soleParameter = element.parameters().orElseThrow().get(0);
          if (soleParameter == int.class || soleParameter == Integer.class) {
            final TreeNode temp = node.get(Integer.parseInt(arguments.get(0)));
            if (temp != null) {
              node = temp;
            }
          }
        }
      } else if (node.isObject()) {
        final TreeNode temp = node.get(element.name());
        if (temp != null) {
          node = temp;
        }
      } else {
        assert !node.isContainerNode();
      }
    }
    try {
      return
        this.value(requestor,
                   absolutePath,
                   node.traverse(reader).readValueAs(new TypeReference<>() {
                       // This is a slight abuse of the TypeReference
                       // class, but the getType() method is not final
                       // and it is public, so this seems to be at
                       // least possible using a public API. It also
                       // avoids type loss we'd otherwise incur (if we
                       // used readValueAs(Class), for example).
                       @Override
                       public final Type getType() {
                         return absolutePath.type();
                       }
                     }));
    } catch (final JsonProcessingException jpe) {
      if (logger.isLoggable(Level.FINE)) {
        logger.logp(Level.FINE, this.getClass().getName(), "get", jpe.getMessage(), jpe);
      }
      return null;
    } catch (final IOException ioException) {
      throw new UncheckedIOException(ioException.getMessage(), ioException);
    }
  }

  @SuppressWarnings("unchecked")
  protected <T> Value<T> value(final Configured<?> requestor,
                               final Path<T> absolutePath,
                               final T value) {
    return new Value<>(null,
                       Qualifiers.of(),
                       (Path<T>)Path.of(absolutePath.type()),
                       () -> value,
                       false,
                       true);
  }

  /**
   * Returns an {@link ObjectCodec} suitable for the combination of
   * the supplied {@link Configured} and {@link Path}, or {@code null}
   * if there is no such {@link ObjectCodec}.
   *
   * <p>This method is called by the {@link #get(Configured, Path)}
   * method in the normal course of events.</p>
   *
   * @param <T> the type of the supplied {@link Path}
   *
   * @param requestor the {@link Configured} seeking a {@link Value};
   * must not be {@code null}
   *
   * @param absolutePath an {@linkplain Path#isAbsolute() absolute
   * <code>Path</code>} for which the supplied {@link Configured} is
   * seeking a value; must not be {@code null}
   *
   * @return an {@link ObjectCodec} suitable for the combination of
   * the supplied {@link Configured} and {@link Path}, or {@code null}
   *
   * @nullability Implementations of this method may return {@code null}.
   *
   * @threadsafety Implementations of this method must be safe for
   * concurrent use by multiple threads.
   *
   * @idempotency Implementations of this method must be idempotent,
   * but not necessarily deterministic.
   */
  protected abstract <T> ObjectCodec objectCodec(final Configured<?> requestor, final Path<T> absolutePath);

  /**
   * Returns a {@link TreeNode} representing the root of an abstract
   * document suitable for the combination of the supplied {@link
   * Configured} and {@link Path}, or {@code null} if there is no such
   * {@link TreeNode}.
   *
   * <p>This method will not be called by the {@link #get(Configured,
   * Path)} method if the {@link #objectCodec(Configured, Path)}
   * method returns {@code null}.  Otherwise, it will be called
   * immediately afterwards on the same thread.</p>
   *
   * @param <T> the type of the supplied {@link Path}
   *
   * @param requestor the {@link Configured} seeking a {@link Value};
   * must not be {@code null}
   *
   * @param absolutePath an {@linkplain Path#isAbsolute() absolute
   * <code>Path</code>} for which the supplied {@link Configured} is
   * seeking a value; must not be {@code null}
   *
   * @param reader for convenience, the {@link ObjectCodec} returned
   * by this {@link JacksonProvider}'s {@link #objectCodec(Configured,
   * Path)} method; must not be {@code null}
   *
   * @return a {@link TreeNode} representing the root of an abstract
   * document suitable for the combination of the supplied {@link
   * Configured} and {@link Path}, or {@code null}
   *
   * @nullability Implementations of this method may return {@code null}.
   *
   * @threadsafety Implementations of this method must be safe for
   * concurrent use by multiple threads.
   *
   * @idempotency Implementations of this method must be idempotent,
   * but not necessarily deterministic.
   */
  protected abstract <T> TreeNode rootNode(final Configured<?> requestor, final Path<T> absolutePath, final ObjectCodec reader);

}
