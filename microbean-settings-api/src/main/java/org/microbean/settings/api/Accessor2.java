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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class Accessor2 {

  private static final Accessor2 EMPTY = new Accessor2("", null, null, null, false);

  private static final Accessor2 ROOT = new Accessor2("", void.class, null, null, true);
  
  private final String name;

  private final Optional<Type> type;

  private final Optional<List<Class<?>>> parameters;

  private final Optional<List<String>> arguments;

  public Accessor2(final String name,
                   final Type type,
                   final List<? extends Class<?>> parameters,
                   final List<? extends String> arguments) {
    this(name, type, parameters, arguments, false);
  }

  private Accessor2(final String name,
                    final Type type,
                    final List<? extends Class<?>> parameters,
                    final List<? extends String> arguments,
                    final boolean root) {
    super();
    this.name = name == null ? "" : name;
    if (type == null) {
      this.type = Optional.empty();
    } else if (!root && type == void.class) {
      throw new IllegalArgumentException("type: " + type);
    } else {
      this.type = Optional.of(type);
    }
    if (parameters == null) {
      this.parameters = Optional.empty();
      if (arguments == null) {
        this.arguments = Optional.empty();
      } else {
        throw new IllegalArgumentException("arguments: " + arguments + "; parameters: null");
      }
    } else if (arguments == null) {
      this.parameters = Optional.of(List.copyOf(parameters));
      this.arguments = Optional.empty();
    } else if (parameters.size() == arguments.size()) {
      this.parameters = Optional.of(List.copyOf(parameters));
      this.arguments = Optional.of(List.copyOf(arguments));
    } else {
      throw new IllegalArgumentException("parameters: " + parameters + "; arguments: " + arguments);
    }
  }

  /**
   * Returns the non-{@code null} name of this {@link Accessor2}.
   *
   * <p><strong>Note:</strong> if the resulting {@link String}
   * {@linkplain String#isEmpty() is empty}, then during any matching
   * operations the name may be considered to match all possible
   * names.</p>
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @return the non-{@code null} name of this {@link Accessor2},
   * which may {@linkplain String#isEmpty() be empty} indicating the
   * special semantics described above
   *
   * @nullability This method never returns {@code null}.
   *
   * @threadsafety This method is safe for concurrent use by multiple
   * threads.
   *
   * @idempotency This method is idempotent and deterministic.
   */
  public final String name() {
    return this.name;
  }

  public final Optional<Type> type() {
    return this.type;
  }

  public final Optional<List<Class<?>>> parameters() {
    return this.parameters;
  }

  public final Optional<List<String>> arguments() {
    return this.arguments;
  }

  public final boolean isRoot() {
    return this.type().orElse(null) == void.class && this.isEmpty();
  }

  public final boolean isEmpty() {
    return this.name().isEmpty();
  }
  
  @Override
  public final int hashCode() {
    return Objects.hash(this.name(), this.type(), this.parameters(), this.arguments());
  }

  @Override
  public final boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other != null && this.getClass() == other.getClass()) {
      final Accessor2 her = (Accessor2)other;
      return        
        Objects.equals(this.name(), her.name()) &&
        Objects.equals(this.type(), her.type()) &&
        Objects.equals(this.parameters(), her.parameters()) &&
        Objects.equals(this.arguments(), her.arguments());
    } else {
      return false;
    }
  }

  @Override
  public final String toString() {
    // oogle.boogle(java.lang.String="eep",java.lang.Integer="3"):java.lang.CharSequence
    final StringBuilder sb = new StringBuilder(this.name());
    final Optional<List<Class<?>>> parameters = this.parameters();
    if (parameters.isPresent()) {
      sb.append("(");
      final List<Class<?>> ps = parameters.orElseThrow();
      final List<?> as = this.arguments().orElseGet(List::of);
      for (int i = 0; i < ps.size(); i++) {
        sb.append(ps.get(i).getName()).append("=\"").append(as.get(i).toString()).append("\"");
        if (i + 1 < ps.size()) {
          sb.append(",");
        }
      }
      sb.append(")");
    }
    final Optional<Type> type = this.type();
    if (type.isPresent()) {
      sb.append(":").append(type.orElseThrow().getTypeName());
    }
    return sb.toString();
  }


  /*
   * Static methods.
   */

  
  public static final Accessor2 root() {
    return ROOT;
  }
  
  public static final Accessor2 of() {
    return EMPTY;
  }

  public static final Accessor2 of(final String name,
                                   final Type type,
                                   final List<? extends Class<?>> parameters,
                                   final List<? extends String> arguments) {
    return new Accessor2(name, type, parameters, arguments);
  }

  public static final Accessor2 of(final String name) {
    return new Accessor2(name, null, null, null);
  }

  public static final Accessor2 of(final String name,
                                   final Type type) {
    return new Accessor2(name, type, null, null);
  }

  public static final Accessor2 of(final Type type) {
    return new Accessor2("", type, null, null);
  }

  public static final Accessor2 of(final String name,
                                   final Type type,
                                   final Class<?> parameter,
                                   final String argument) {
    return new Accessor2(name, type, List.of(parameter), List.of(argument));
  }

  public static final class Parser {

    private static final int NAME = 1;

    private static final int TYPE = 2;

    private static final int ARGUMENTS = 3;
    
    private final ClassLoader cl;
    
    public Parser(final ClassLoader cl) {
      super();
      this.cl = Objects.requireNonNull(cl, "cl");
    }

    public final Accessor2 parse(final CharSequence s) throws ClassNotFoundException {
      int state = NAME;
      final StringBuilder sb = new StringBuilder();
      String name = null;
      Type type = null;
      List<Class<?>> params = null;
      List<String> args = null;
      final int length = s.length();
      for (int i = 0; i < length; i++) {
        final int c = s.charAt(i);
        final int next = i + 1 < length ? s.charAt(i + 1) : -1;
        switch (c) {

        case '(':
          switch (state) {
          case NAME:
            switch (next) {
            case -1:
              throw new IllegalArgumentException(iae(s, i));
            default:
              name = sb.toString();
              sb.setLength(0);
              params = new ArrayList<>(3);
              state = ARGUMENTS;
              break;
            }
            break;
          case ARGUMENTS:
          case TYPE:
            throw new IllegalArgumentException(iae(s, i));
          default:
            throw new IllegalStateException();
          }
          break;

        case ')':
          switch (state) {
          case ARGUMENTS:
            if (sb.isEmpty()) {
              if (args == null) {
                args = List.of();
              }
            } else {
              if (args == null) {
                params.add(loadClass(sb.toString()));
              } else {
                args.add(sb.toString());
              }
              sb.setLength(0);
            }
            state = TYPE;
            break;
          case NAME:
          case TYPE:
            throw new IllegalArgumentException(iae(s, i));
          default:
            throw new IllegalStateException();
          }
          break;

        case '\\':
          switch (next) {
          case -1:
            throw new IllegalArgumentException(iae(s, i));
          default:
            sb.append((char)next);
            ++i;
            break;
          }
          break;

        case ',':
          switch (state) {
          case NAME:
            sb.append((char)c);          
            break;
          case ARGUMENTS:
            assert name != null;
            assert type == null;
            assert params != null;
            if (params.isEmpty()) {
              throw new IllegalArgumentException(iae(s, i));
            }
            args.add(sb.toString());
            sb.setLength(0);
            break;
          case TYPE:
            throw new IllegalArgumentException(iae(s, i));
          default:
            throw new IllegalStateException();
          }
          break;

        case '=':
          switch (state) {
          case NAME:
            sb.append((char)c);
            break;
          case ARGUMENTS:
            if (!sb.isEmpty()) {
              params.add(loadClass(sb.toString()));            
              sb.setLength(0);
            }
            if (args == null) {
              args = new ArrayList<>(3);
            }
            break;
          case TYPE:
            throw new IllegalArgumentException(iae(s, i));
          default:
            throw new IllegalStateException();
          }
          break;

        case ':':
          switch (state) {
          case NAME:
            name = sb.toString();
            sb.setLength(0);
            state = TYPE;
            break;
          case ARGUMENTS:
            sb.append((char)c);
            break;            
          case TYPE:
            if (!sb.isEmpty()) {
              throw new IllegalArgumentException(iae(s, i));
            }
            break;
          default:
            throw new IllegalStateException();
          }
          break;            
          
        default:
          sb.append((char)c);
          break;
        }
      }

      // Cleanup
      switch (state) {

      case NAME:
        name = sb.toString();
        sb.setLength(0);
        break;
        
      case ARGUMENTS:
        if (!sb.isEmpty()) {
          if (params == null) {
            params = List.of(loadClass(sb.toString()));
          } else {
            assert !params.isEmpty();
            args.add(sb.toString());
          }
          sb.setLength(0);
        }
        break;

      case TYPE:
        type = loadType(sb.toString());
        sb.setLength(0);
        break;
        
      default:
        throw new IllegalStateException();
      }

      assert params == null ? args == null : args == null || args.size() <= params.size() : s + "; params: " + params + "; args: " + args;

      if (name.isEmpty() && params == null && args == null) {
        if (type == null) {
          return Accessor2.of();
        } else if (type == void.class) {
          return Accessor2.root();
        }
      }
      return new Accessor2(name, type, params, args);
    }

    private final String iae(final CharSequence s, final int pos) {
      final StringBuilder sb = new StringBuilder(s.toString()).append(System.lineSeparator());
      for (int i = 0; i < pos; i++) {
        sb.append(' ');
      }
      sb.append('^').append(System.lineSeparator());
      return sb.toString();
    }
    
    private final Class<?> loadClass(final String s) throws ClassNotFoundException {
      return switch (s) {
      case "boolean" -> boolean.class;
      case "char" -> char.class;
      case "double" -> double.class;
      case "float" -> float.class;
      case "int" -> int.class;
      case "long" -> long.class;
      case "short" -> short.class;
      case "void" -> void.class;
      default -> Class.forName(s, false, this.cl);
      };
    }
    
    private final Type loadType(final String s) throws ClassNotFoundException {
      return loadClass(s);
    }
    
  }

}
