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

import java.lang.StackWalker.StackFrame;

import java.lang.reflect.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;

import java.util.stream.Stream;

import org.microbean.development.annotation.Experimental;
import org.microbean.development.annotation.Incomplete;

import org.microbean.type.Types;

public final class Path implements Assignable<Type> {

  private static final Path ROOT = new Path();

  private static final StackWalker stackWalker = StackWalker.getInstance();

  private final List<Object> elements;

  private final boolean transliterated;

  private Path() {
    super();
    this.elements = List.of(void.class);
    this.transliterated = true;
  }

  private Path(final Type type) {
    this(List.of(), List.of(), type, false);
  }

  private Path(final Accessor accessor, final Type type) {
    this(List.of(), List.of(accessor), type, false);
  }

  private Path(final List<? extends Accessor> accessors, final Type type) {
    this(List.of(), accessors, type, false);
  }

  private Path(final List<?> existingElements, final List<? extends Accessor> accessors, final Type type) {
    this(existingElements, accessors, type, false);
  }

  private Path(final List<?> existingElements, final List<? extends Accessor> accessors, final Type type, final boolean transliterated) {
    super();
    if (Objects.requireNonNull(type, "type") == void.class) {
      throw new IllegalArgumentException("type: " + type);
    }
    if (existingElements == null || existingElements.isEmpty()) {
      final int accessorsSize = accessors == null ? 0 : accessors.size();
      switch (accessorsSize) {
      case 0:
        this.elements = List.of(type);
        break;
      case 1:
        this.elements = List.of(accessors.get(0), type);
        break;
      case 2:
        this.elements = List.of(accessors.get(0), accessors.get(1), type);
        break;
      case 3:
        this.elements = List.of(accessors.get(0), accessors.get(1), accessors.get(2), type);
        break;
      case 4:
        this.elements = List.of(accessors.get(0), accessors.get(1), accessors.get(2), accessors.get(3), type);
        break;
      case 5:
        this.elements = List.of(accessors.get(0), accessors.get(1), accessors.get(2), accessors.get(3), accessors.get(4), type);
        break;
      default:
        final List<Object> elements = new ArrayList<>(accessors.size() + 1);
        for (final Accessor a : accessors) {
          elements.add(Objects.requireNonNull(a, "accessor"));
        }
        elements.add(type);
        this.elements = Collections.unmodifiableList(elements);
      }
    } else {
      final List<Object> elements = new ArrayList<>(existingElements.size() + accessors.size() + 1);
      elements.addAll(existingElements);
      for (final Accessor a : accessors) {
        elements.add(Objects.requireNonNull(a, "accessor"));
      }
      elements.add(type);
      this.elements = Collections.unmodifiableList(elements);
    }
    this.transliterated = transliterated;
  }

  @Override // Assignable<Type>
  public final Type assignable() {
    return this.type();
  }

  @Override // Assignable<Type>
  public final boolean isAssignable(final Type type) {
    return AssignableType.of(this.assignable()).isAssignable(type);
  }

  public final ClassLoader classLoader() {
    return Types.erase(this.type()).getClassLoader();
  }

  public final int indexOf(final Path other) {
    return Collections.indexOfSubList(this.elements, other.elements);
  }

  public final int indexOf(final Path path, final BiPredicate<? super Object, ? super Object> p) {
    final int pathSize = path.size();
    final int sizeDiff = this.size() - pathSize;
    OUTER_LOOP:
    for (int i = 0; i <= sizeDiff; i++) {
      for (int j = 0, k = i; j < pathSize; j++, k++) {
        if (!p.test(this.elements.get(k), path.elements.get(j))) {
          continue OUTER_LOOP;
        }
      }
      return i;
    }
    return -1;
  }

  public final boolean startsWith(final Path other) {
    if (other == this) {
      return true;
    } else if (other == null) {
      return false;
    } else {
      return this.indexOf(other) == 0;
    }
  }

  public final boolean startsWith(final Path other, final BiPredicate<? super Object, ? super Object> p) {
    return this.indexOf(other, p) == 0;
  }

  public final int lastIndexOf(final Path other) {
    return Collections.lastIndexOfSubList(this.elements, other.elements);
  }

  public final int lastIndexOf(final Path path, final BiPredicate<? super Object, ? super Object> p) {
    final int pathSize = path.size();
    final int sizeDiff = this.size() - pathSize;
    OUTER_LOOP:
    for (int i = sizeDiff; i >= 0; i--) {
      for (int j = 0, k = i; j < pathSize; j++, k++) {
        if (!p.test(this.elements.get(k), path.elements.get(j))) {
          continue OUTER_LOOP;
        }
      }
      return i;
    }
    return -1;
  }

  public final boolean endsWith(final Path other) {
    if (other == this) {
      return true;
    } else if (other == null) {
      return false;
    } else {
      final int lastIndex = this.lastIndexOf(other);
      return lastIndex >= 0 && lastIndex + other.size() == this.size();
    }
  }

  public final boolean endsWith(final Path other, final BiPredicate<? super Object, ? super Object> p) {
    final int lastIndex = this.lastIndexOf(other, p);
    return lastIndex >= 0 && lastIndex + other.size() == this.size();
  }

  public final Path plus(final Type type) {
    return this.plus(Accessor.of(), type);
  }

  public final Path plus(final String accessor, final Type type) {
    return this.plus(Accessor.of(accessor), type);
  }

  public final Path plus(final Accessor accessor, final Type type) {
    return this.plus(List.of(accessor), type);
  }

  public final Path plus(final List<? extends Accessor> accessors, final Type type) {
    return new Path(this.elements, accessors, type, false);
  }

  public final Path plus(final Path path) {
    final int size = path.size();
    if (size == 1) {
      assert path.isType(0);
      return new Path(this.elements, List.of(Accessor.of()), path.type(), this.transliterated);
    } else {
      assert size > 1;
      assert path.isType(0) ? path.type(0) == void.class : path.isAccessor(0);
      assert path.isType(size - 1);
      final List<Object> newElements = new ArrayList<>(this.elements);
      newElements.addAll(path.elements.subList(0, size - 1));
      return new Path(newElements, List.of(), path.type(), false);
    }
  }

  // Drops the intermediate type.
  public final Path merge(final String accessor, final Type type) {
    return this.merge(Accessor.of(accessor), type);
  }

  // Drops the intermediate type.
  public final Path merge(final Accessor accessor, final Type type) {
    return this.merge(List.of(accessor), type);
  }

  // Drops the intermediate type.
  public final Path merge(final List<? extends Accessor> accessors, final Type type) {
    return new Path(this.elements.subList(0, this.elements.size() - 1), accessors, type, false);
  }

  public final int size() {
    return this.elements.size();
  }

  public final boolean isAccessor(final int index) {
    return this.elements.get(index) instanceof Accessor;
  }

  public final boolean isType(final int index) {
    return this.elements.get(index) instanceof Type;
  }

  public final Accessor accessor(final int index) {
    final Object o = this.elements.get(index);
    return o instanceof Accessor a ? a : null;
  }

  public final Accessor lastAccessor() {
    return this.accessor(this.size() - 2);
  }

  public final Type type(final int index) {
    final Object o = this.elements.get(index);
    return o instanceof Type type ? type : null;
  }

  public final Type type() {
    return (Type)this.elements.get(this.elements.size() - 1);
  }

  public final boolean isRoot() {
    return this.isAbsolute() && this.elements.size() == 1;
  }

  public final boolean isAbsolute() {
    return this.type(0) == void.class;
  }

  @Experimental
  public final Path transliterate(final BiFunction<? super String, ? super Accessor, ? extends Accessor> f) {
    if (f == null || this.transliterated || this.type(0) != void.class || this.size() == 1) {
      return this;
    } else {
      final String userPackageName = stackWalker.walk(Path::findUserPackageName);
      final int size = this.size() - 1; // don't include our trailing type
      final List<Object> newElements = new ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        if (this.isType(i)) {
          newElements.add(this.elements.get(i));
        } else if (this.isAccessor(i)) {
          newElements.add(f.apply(userPackageName, this.accessor(i)));
        } else {
          throw new AssertionError("element: " + this.elements.get(i));
        }
      }
      return new Path(newElements, List.of(), this.type(), true);
    }
  }

  @Experimental
  public final boolean isTransliterated() {
    return this.transliterated;
  }

  @Override
  public final int hashCode() {
    // Note: we deliberately do NOT include this.transliterated.
    return this.elements.hashCode();
  }

  @Override
  public final boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other == null || this.getClass() != other.getClass()) {
      return false;
    } else {
      // Note: we deliberately do NOT include this.transliterated.
      return this.elements.equals(((Path)other).elements);
    }
  }

  @Override
  public final String toString() {
    final StringJoiner sj = new StringJoiner("/");
    final int size = this.size();
    if (size <= 0) {
      sj.setEmptyValue("/");
    } else {
      for (int i = 0; i < size; i++) {
        if (this.isType(i)) {
          final Type leadingType = this.type(i);
          if (leadingType == void.class) {
            sj.add("");
          } else {
            sj.add(leadingType.getTypeName());
          }
        } else {
          sj.add(this.accessor(i).toString());
        }
      }
    }
    return sj.toString();
  }

  public static final Path of() {
    return ROOT;
  }

  public static final Path of(final Type type) {
    return new Path(type);
  }

  public static final Path of(final String accessor, final Type type) {
    return new Path(Accessor.of(accessor), type);
  }

  public static final Path of(final Accessor accessor, final Type type) {
    return new Path(accessor, type);
  }

  public static final Path of(final List<? extends Accessor> accessors, final Type type) {
    return new Path(accessors, type);
  }

  private static final String findUserPackageName(final Stream<StackFrame> stream) {
    final String className = stream.sequential()
      .dropWhile(f -> f.getClassName().startsWith(Settings.class.getPackageName()))
      .dropWhile(f -> f.getClassName().contains(".$Proxy")) // skip JDK proxies (and any other kind of proxies)
      .map(StackFrame::getClassName)
      .findFirst()
      .orElse(null);
    if (className == null) {
      return "";
    } else {
      final int lastIndex = className.lastIndexOf('.');
      if (lastIndex < 0) {
        return "";
      } else if (lastIndex == 0) {
        throw new AssertionError("className: " + className);
      } else {
        return className.substring(0, lastIndex);
      }
    }
  }

  @Incomplete
  static final class Parser {

    private static final int START = 0;

    private static final int NAME = 1;

    private static final int TYPE = 2;

    private static final int PARAMETERS = 3;

    private static final int ARGUMENTS = 4;

    Parser() {
      super();
    }

    public final Path parse(final String s, final ClassLoader cl) throws ClassNotFoundException {
      int state = START;
      final List<Object> elements = new ArrayList<>(11);
      final StringBuilder sb = new StringBuilder();
      String name = null;
      Class<?> type = null;
      final List<Class<?>> params = new ArrayList<>(3);
      final List<String> args = new ArrayList<>(3);
      for (int i = 0; i < s.length(); i++) {
        final int c = s.charAt(i);
        switch (c) {

        case '/':
          switch (state) {
          case START:
            assert name == null;
            assert type == null;
            assert params.isEmpty();
            assert args.isEmpty();
            type = void.class;
            elements.add(type);
            sb.setLength(0);
            state = NAME;
            break;
          case NAME:
            assert params.isEmpty();
            assert args.isEmpty();
            name = sb.toString();
            elements.add(Accessor.of(name, List.of(), List.of()));
            sb.setLength(0);
            type = null;
            // state = NAME;
            break;
          case TYPE:
            assert params.isEmpty();
            assert args.isEmpty();
            if (type != null) {
              throw new IllegalArgumentException(s);
            }
            type = typeFor(sb.toString(), cl);
            elements.add(type);
            sb.setLength(0);
            name = null;
            state = NAME;
            break;
          case PARAMETERS:
            assert type == null;
            assert args.isEmpty();
            state = ARGUMENTS;
            break;
          case ARGUMENTS:
            assert name != null;
            assert type == null;
            assert !params.isEmpty();
            if (sb.isEmpty()) {
              elements.add(Accessor.of(name, params, List.of()));
            } else {
              args.add(sb.toString());
              elements.add(Accessor.of(name, params, args));
              sb.setLength(0);
            }
            name = null;
            // type = null;
            params.clear();
            args.clear();
            state = NAME;
            break;
          default:
            sb.append((char)c);
          }
          break;

        case ':':
          switch (state) {
          case START:
            throw new IllegalArgumentException(s);
          case NAME:
            assert params.isEmpty();
            assert args.isEmpty();
            name = sb.toString();
            sb.setLength(0);
            type = null;
            state = PARAMETERS;
            break;
          case TYPE:
            throw new IllegalArgumentException(s);
          case PARAMETERS:
            throw new IllegalArgumentException(s);
          case ARGUMENTS:
            assert !params.isEmpty();
            sb.append((char)c);
            break;
          default:
            throw new IllegalStateException();
          }
          break;

        case '.':
          switch (state) {
          case START:
            throw new IllegalArgumentException(s);
          case NAME:
            assert params.isEmpty();
            assert args.isEmpty();
            if (type != null) {
              throw new IllegalArgumentException(s); // two types in a row
            }
            sb.append((char)c);
            name = null;
            state = TYPE;
            break;
          case PARAMETERS:
            assert type == null;
            assert args.isEmpty();
            sb.append((char)c);
            break;
          default:
            sb.append((char)c);
            break;
          }
          break;

        case ';':
          switch (state) {
          case START:
            throw new IllegalArgumentException(s);
          case NAME:
            throw new IllegalArgumentException(s);
          case TYPE:
            throw new IllegalArgumentException(s);
          case PARAMETERS:
            throw new IllegalArgumentException(s);
          case ARGUMENTS:
            assert params.isEmpty();
            args.add(sb.toString());
            sb.setLength(0);
            state = PARAMETERS;
            break;
          default:
            throw new IllegalStateException();
          }
          break;

        case '\\':
          switch (state) {
          case START:
            throw new IllegalArgumentException(s);
          case NAME:
            sb.append((char)c);
            break;
          case TYPE:
            throw new IllegalArgumentException(s);
          case PARAMETERS:
            throw new IllegalArgumentException(s);
          case ARGUMENTS:
            if (i + 1 < s.length() && s.charAt(i + 1) == '"') {
              i++;
              sb.append("\"");
            } else {
              sb.append('\\');
            }
            break;
          default:
            throw new IllegalStateException();
          }
          break;

        case '\"':
          switch (state) {
          case START:
          case NAME:
          case TYPE:
          case PARAMETERS:
            throw new IllegalArgumentException(s);
          case ARGUMENTS:
            // skip it
            break;
          default:
            throw new IllegalStateException();
          }
          break;

        case '=':
          switch (state) {
          case START:
            throw new IllegalArgumentException(s);
          case NAME:
            throw new IllegalArgumentException(s);
          case TYPE:
            throw new IllegalArgumentException(s);
          case PARAMETERS:
            assert name != null;
            assert type == null;
            assert args.isEmpty();
            params.add(typeFor(sb.toString(), cl));
            sb.setLength(0);
            if (i + 1 < s.length()) {
              if (s.charAt(i + 1) == '\"') {
                ++i;
              }
              state = ARGUMENTS;
            }
            break;
          case ARGUMENTS:
            assert name != null;
            assert type == null;
            assert !params.isEmpty();
            sb.append((char)c);
            break;
          default:
            throw new IllegalStateException();
          }
          break;

        default:
          switch (state) {
          case START:
            state = NAME;
            break;
          default:
            break;
          }
          sb.append((char)c);
          break;
        }
      }

      switch (state) {
      case START:
        // empty
        throw new IllegalArgumentException(s);
      case NAME:
        if (!sb.isEmpty()) {
          throw new IllegalArgumentException(s);
        }
        break;
      case TYPE:
        assert name == null;
        assert type == null;
        assert params.isEmpty();
        assert args.isEmpty();
        elements.add(typeFor(sb.toString(), cl));
        sb.setLength(0);
        break;
      case PARAMETERS:
        throw new IllegalArgumentException(s);
      case ARGUMENTS:
        throw new IllegalArgumentException(s);
      default:
        throw new IllegalStateException();
      }
      if (elements.size() == 1 && elements.get(0) == void.class) {
        return Path.of();
      } else {
        return new Path(elements.subList(0, elements.size() - 1), List.of(), (Type)elements.get(elements.size() - 1));
      }
    }

  }

  private static final Class<?> typeFor(final String s, final ClassLoader cl) throws ClassNotFoundException {
    return switch (s) {
    case "boolean" -> boolean.class;
    case "char" -> char.class;
    case "double" -> double.class;
    case "float" -> float.class;
    case "int" -> int.class;
    case "long" -> long.class;
    case "short" -> short.class;
    case "void" -> void.class;
    default -> Class.forName(s, false, cl);
    };
  }

}
