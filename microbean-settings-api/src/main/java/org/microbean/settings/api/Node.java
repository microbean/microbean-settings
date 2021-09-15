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

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;

import java.util.function.Supplier;

import org.microbean.development.annotation.Experimental;
import org.microbean.development.annotation.Incomplete;

@Experimental
@Incomplete // Name will change
public abstract class Node<T> implements ValueSupplier<T> {


  /*
   * Static fields.
   */


  private static final ClassValue<Type> typeExtractor = new TypeArgumentExtractor(Node.class, 0);

  private static final Map<Key, List<Node<?>>> cache;


  /*
   * Static initializer.
   */


  static {
    @SuppressWarnings("rawtypes")
    final ServiceLoader<Node> nodes = ServiceLoader.load(Node.class);
    final Map<Key, List<Node<?>>> map = new HashMap<>();
    for (final Node<?> node : nodes) {
      map.computeIfAbsent(new Key(node), k -> new ArrayList<>()).add(node);
    }
    cache = Collections.unmodifiableMap(map);
  }


  /*
   * Instance fields.
   */


  private final Type parentType;

  private final String name;

  private final Set<?> qualifiers;

  private final ValueSupplier<? extends T> valueSupplier;

  private final Map<String, ValueSupplier<?>> valueSuppliers;

  private final Supplier<? extends T> lastResortSupplier;

  private final InvocationHandler invocationHandler;


  /*
   * Constructors.
   */


  protected Node(final Type parentType, // nullable
                 final String name, // e.g. a path component, a property name; nullable
                 final Set<?> qualifiers, // e.g. environment=test, region=useast
                 final ValueSupplier<? extends T> valueSupplier, // e.g. this Node simply supplies a value
                 final Map<? extends String, ? extends ValueSupplier<?>> valueSuppliers, // e.g. this Node doesn't supply a value but some of its properties
                 final Supplier<? extends T> lastResortSupplier) { // nullable (hmm; TODO) last-ditch defaults
    super();
    this.parentType = parentType;
    this.name = name;
    this.qualifiers = Set.copyOf(qualifiers);
    if (valueSupplier == null) {
      if (valueSuppliers == null || valueSuppliers.isEmpty()) {
        // Kind of a pathological case
        this.valueSuppliers = Map.of();
        if (lastResortSupplier == null) {
          this.valueSupplier = Node::returnNull;
        } else {
          this.valueSupplier = qs -> lastResortSupplier.get();
        }
      } else {
        this.valueSuppliers = Map.copyOf(valueSuppliers);
        this.valueSupplier = this::proxy;
      }
    } else if (valueSuppliers == null || valueSuppliers.isEmpty()) {
      this.valueSuppliers = Map.of();
      this.valueSupplier = valueSupplier;
    } else {
      // One or the other
      throw new IllegalArgumentException("valueSupplier: " + valueSupplier + "; valueSuppliers: " + valueSuppliers);
    }
    this.lastResortSupplier = lastResortSupplier;
    this.invocationHandler = new InvocationHandler();
  }


  /*
   * Instance methods.
   */


  @Override // ValueSupplier<T>
  public final T get(final Set<?> qualifiers) {
    return this.valueSupplier.get(qualifiers);
  }

  private final Type type() {
    return typeExtractor.get(this.getClass());
  }

  private final ValueSupplier<?> valueSupplier(final String name) {
    return name == null ? null : this.valueSuppliers.get(name);
  }

  private final ValueSupplier<?> valueSupplier(final Method m) {
    if (accept(m)) {
      return this.valueSupplier(m.getName());
    } else {
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private final T proxy(final Set<?> qualifiers) {
    final Class<?> c = rawClass(this.type());
    return (T)Proxy.newProxyInstance(c.getClassLoader(), new Class<?>[] { c }, this.invocationHandler);
  }


  /*
   * Static methods.
   */


  @SuppressWarnings("unchecked")
  public static final <T> T get(final Type type, final Set<?> qualifiers) {
    final List<Node<?>> nodes = nodes(null, type, null);
    if (nodes == null || nodes.isEmpty()) {
      throw new NoSuchElementException();
    } else if (nodes.size() > 1) {
      throw new IllegalStateException("nodes: " + nodes);
    }
    return ((Node<T>)nodes.get(0)).get(Set.copyOf(qualifiers));
  }

  private static final List<Node<?>> nodes(final Type parentType, final Type type, final String name) {
    return cache.get(new Key(parentType, type, name));
  }

  private static final boolean accept(final Method m) {
    if (m != null && m.getDeclaringClass() != Object.class && m.getParameterCount() == 0) {
      final Object returnType = m.getReturnType();
      return returnType != void.class && returnType != Void.class;
    } else {
      return false;
    }
  }

  private static final Class<?> rawClass(final Type type) {
    if (type instanceof Class<?> c) {
      return c;
    } else if (type instanceof ParameterizedType ptype) {
      return rawClass(ptype);
    } else {
      throw new IllegalArgumentException("type: " + type);
    }
  }

  private static final Class<?> rawClass(final ParameterizedType type) {
    return rawClass(type.getRawType());
  }

  private static final <T> T returnNull(final Set<?> qualifiers) {
    return null;
  }


  /*
   * Inner and nested classes.
   */


  private final class InvocationHandler implements java.lang.reflect.InvocationHandler {

    private InvocationHandler() {
      super();
    }

    @Override // InvocationHandler
    public final Object invoke(final Object proxy, final Method method, final Object[] args) throws ReflectiveOperationException {
      if (method.getDeclaringClass() == Object.class) {
        return
          switch (method.getName()) {
          case "hashCode" -> System.identityHashCode(proxy);
          case "equals" -> proxy == args[0];
          case "toString" -> String.valueOf(proxy);
          default -> throw new AssertionError();
          };
      } else {
        final ValueSupplier<?> valueSupplier = Node.this.valueSupplier(method);
        if (valueSupplier == null) {
          final List<Node<?>> nodes = nodes(Node.this.parentType, method.getGenericReturnType(), method.getName());
          if (nodes == null || nodes.isEmpty()) {
            final T lr = Node.this.lastResortSupplier == null ? null : Node.this.lastResortSupplier.get();
            if (lr == null) {
              throw new UnsupportedOperationException(method.getName());
            } else {
              return method.invoke(lr, args);
            }
          } else if (nodes.size() == 1) {
            return nodes.get(0).get(Node.this.qualifiers);
          } else {
            throw new UnsupportedOperationException("Unhandled; too many nodes for " + method + " and " + Node.this + ": " + nodes);
          }
        } else {
          return valueSupplier.get(Node.this.qualifiers);
        }
      }
    }

  }

  private static final record Key(Type parentType, Type type, String name) {

    private Key(final Node<?> node) {
      this(node.parentType, node.type(), node.name);
    }

  }

}
