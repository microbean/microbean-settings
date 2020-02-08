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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

import java.lang.annotation.Annotation;

import java.lang.reflect.Executable;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Member;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import javax.enterprise.context.Dependent;

import javax.enterprise.context.spi.CreationalContext;

import javax.enterprise.event.Observes;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.CreationException;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.UnsatisfiedResolutionException;

import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.ProcessProducer;
import javax.enterprise.inject.spi.Producer;
import javax.enterprise.inject.spi.ProducerFactory;
import javax.enterprise.inject.spi.WithAnnotations;

import javax.enterprise.util.TypeLiteral;

import javax.inject.Singleton;

import org.microbean.settings.converter.StringConverter;

public class SettingsExtension implements Extension {


  /*
   * Instance fields.
   */


  private final Map<Set<Annotation>, Set<Type>> configuredTypes;

  private final Set<InjectionPoint> settingInjectionPoints;
  
  private final Set<Set<Annotation>> settingQualifierSets;

  private final Set<Set<Annotation>> settingsQualifierSets;

  private final Set<Type> knownConversionTypes;


  /*
   * Constructors.
   */


  public SettingsExtension() {
    super();
    this.configuredTypes = new HashMap<>();
    this.settingInjectionPoints = new HashSet<>();
    this.settingQualifierSets = new HashSet<>();
    this.settingsQualifierSets = new HashSet<>();
    this.knownConversionTypes = new HashSet<>(Collections.singleton(String.class));
  }


  /*
   * Observer methods.
   */


  private final <T, X extends Settings> void processSettingsInjectionPoint(@Observes final ProcessInjectionPoint<T, X> event) {
    final Set<Annotation> qualifiers = new HashSet<>(event.getInjectionPoint().getQualifiers());
    qualifiers.add(Any.Literal.INSTANCE);
    this.settingsQualifierSets.add(qualifiers);
  }

  private final <T, X> void processInjectionPoint(@Observes final ProcessInjectionPoint<T, X> event,
                                                  final BeanManager beanManager) {
    final InjectionPoint injectionPoint = event.getInjectionPoint();
    try {
      if (!this.processSettingInjectionPoint(injectionPoint)) {
        this.processNonSettingInjectionPoint(injectionPoint, beanManager);
      }
    } catch (final Exception definitionException) {
      event.addDefinitionError(definitionException);
    }
  }

  private final boolean processSettingInjectionPoint(final InjectionPoint injectionPoint) {
    final boolean returnValue;
    final Type type = injectionPoint.getType();
    if (Settings.class.equals(type)) {
      returnValue = false;
    } else {
      final Set<Annotation> injectionPointQualifiers = injectionPoint.getQualifiers();
      boolean containsSetting = false;
      for (final Annotation injectionPointQualifier : injectionPointQualifiers) {
        if (injectionPointQualifier instanceof Setting) {
          final Setting setting = (Setting)injectionPointQualifier;
          if (setting.required()) {
            final Object defaultValue = setting.defaultValue();
            if (defaultValue != null && !defaultValue.equals(Setting.UNSET)) {
              throw new DefinitionException("While processing the injection point " + injectionPoint +
                                            " the Setting annotation named " + setting.name() +
                                            " had a defaultValue element specified (" + defaultValue +
                                            ") and returned true from its required() element.");
            }
          }
          containsSetting = true;
          break;
        }
      }
      if (containsSetting) {
        this.knownConversionTypes.add(type);

        this.settingInjectionPoints.add(injectionPoint);

        final Set<Annotation> settingQualifiers = new HashSet<>(injectionPointQualifiers);
        settingQualifiers.add(Any.Literal.INSTANCE);
        this.settingQualifierSets.add(settingQualifiers);

        final Set<Annotation> settingsQualifiers = new HashSet<>(injectionPointQualifiers);
        settingsQualifiers.removeIf(e -> e instanceof Setting);
        if (settingsQualifiers.isEmpty()) {
          settingsQualifiers.add(Default.Literal.INSTANCE);
        }
        settingsQualifiers.add(Any.Literal.INSTANCE);
        this.settingsQualifierSets.add(settingsQualifiers);
        returnValue = true;
      } else {
        returnValue = false;
      }      
    }
    return returnValue;
  }

  private final void processNonSettingInjectionPoint(final InjectionPoint injectionPoint,
                                                     final BeanManager beanManager) {
    Objects.requireNonNull(injectionPoint);
    Objects.requireNonNull(beanManager);
    final Set<Annotation> qualifiers = injectionPoint.getQualifiers();
    if (qualifiers != null && !qualifiers.isEmpty()) {
      for (final Annotation qualifier : qualifiers) {
        if (qualifier instanceof Configured) {
          Set<Type> types = this.configuredTypes.get(qualifiers);
          if (types == null) {
            types = new HashSet<>();
            this.configuredTypes.put(qualifiers, types);
          }
          types.add(injectionPoint.getType());
          break;
        }
      }
    }
  }

  private final void installConverterProviderBeans(@Observes final AfterBeanDiscovery event,
                                                   final BeanManager beanManager) {
    final Type type = ConverterProvider.class;
    addBean(event,
            beanManager,
            type,
            this.settingsQualifierSets,
            (e, bm, t, nq) -> e.addBean()
              .addTransitiveTypeClosure(BeanManagerBackedConverterProvider.class)
              .scope(Singleton.class)
              .qualifiers(nq)
              .beanClass(BeanManagerBackedConverterProvider.class)
              .createWith(cc -> new BeanManagerBackedConverterProvider(bm, nq)));
  }

  private final void installSourcesSupplierBeans(@Observes final AfterBeanDiscovery event,
                                                 final BeanManager beanManager) {
    final Type type = new TypeLiteral<BiFunction<String, Set<Annotation>, Set<Source>>>() {
      private static final long serialVersionUID = 1L;
    }.getType();
    addBean(event,
            beanManager,
            type,
            this.settingsQualifierSets,
            (e, bm, t, nq) -> e.addBean()
              .types(t)
              .scope(Singleton.class)
              .qualifiers(nq)
              .beanClass(BeanManagerBackedSourcesSupplier.class)
              .createWith(cc -> new BeanManagerBackedSourcesSupplier(bm)));
  }

  private final void installSettingsBeans(@Observes final AfterBeanDiscovery event,
                                          final BeanManager beanManager) {
    addBean(event,
            beanManager,
            Settings.class,
            this.settingsQualifierSets,
            (e, bm, t, nq) -> e.addBean()
              .addTransitiveTypeClosure(t)
              .scope(Singleton.class)
              .qualifiers(nq)
              .beanClass(Settings.class)
              .produceWith(instance -> {
                final Annotation[] qualifiersArray = nq.toArray(new Annotation[nq.size()]);
                final BiFunction<? super String, ? super Set<Annotation>, ? extends Set<? extends Source>> sourcesSupplier =
                  instance.select(new TypeLiteral<BiFunction<? super String,
                                                             ? super Set<Annotation>,
                                                             ? extends Set<? extends Source>>>() {
                    private static final long serialVersionUID = 1L;
                  },
                  qualifiersArray).get();
                final ConverterProvider converterProvider = instance.select(ConverterProvider.class, qualifiersArray).get();
                final Iterable<? extends Arbiter> arbiters = instance.select(Arbiter.class, qualifiersArray);
                return new Settings(nq, sourcesSupplier, converterProvider, arbiters);
              }));
  }

  private final void installSettingProducers(@Observes final AfterBeanDiscovery event,
                                             final BeanManager beanManager) {
    if (!this.settingQualifierSets.isEmpty()) {
      final AnnotatedType<SettingsExtension> annotatedType = beanManager.createAnnotatedType(SettingsExtension.class);
      final AnnotatedMethod<? super SettingsExtension> producerMethodTemplate = annotatedType.getMethods()
        .stream()
        .filter(m -> m.getJavaMember().getName().equals("producerMethodTemplate"))
        .findFirst() // ...and only
        .get();
      final BeanAttributes<?> delegate = beanManager.createBeanAttributes(producerMethodTemplate);
      for (final Set<Annotation> settingQualifiers : this.settingQualifierSets) {
        final Set<Annotation> settingsQualifiers = new HashSet<>(settingQualifiers);
        settingsQualifiers.removeIf(e -> e instanceof Setting);
        if (settingsQualifiers.isEmpty()) {
          settingsQualifiers.add(Default.Literal.INSTANCE);
        }
        for (final Type type : this.knownConversionTypes) {
          if (noBeans(beanManager, type, settingQualifiers)) {
            // type is the type of, say, an injection point.  So it
            // could be a wildcard or a type variable.  Or it could be
            // a ParameterizedType with recursive wildcards or type
            // variables.
            final BeanAttributes<?> beanAttributes =
              new FlexiblyTypedBeanAttributes<Object>(delegate, settingQualifiers, Collections.singleton(synthesizeLegalBeanType(type, 1)));
            final ProducerFactory<SettingsExtension> defaultProducerFactory =
              beanManager.getProducerFactory(producerMethodTemplate, null);
            final ProducerFactory<SettingsExtension> producerFactory = new ProducerFactory<SettingsExtension>() {
                @Override
                public final <T> Producer<T> createProducer(final Bean<T> bean) {
                  final Producer<T> defaultProducer = defaultProducerFactory.createProducer(bean);
                  final Set<InjectionPoint> injectionPoints =
                  qualifyInjectionPoints(defaultProducer.getInjectionPoints(), settingsQualifiers);
                  return new DelegatingProducer<T>(defaultProducer) {
                    @Override
                    public final Set<InjectionPoint> getInjectionPoints() {
                      return injectionPoints;
                    }
                  };
                }
              };
            final Bean<?> bean = beanManager.createBean(beanAttributes, SettingsExtension.class, producerFactory);
            event.addBean(bean);
          }
        }
      }
    }
  }

  private <T> void installConfiguredBeans(@Observes final AfterBeanDiscovery event,
                                          final BeanManager beanManager) {
    final Set<Entry<Set<Annotation>, Set<Type>>> entrySet = this.configuredTypes.entrySet();
    for (final Entry<Set<Annotation>, Set<Type>> entry : entrySet) {
      final Set<Annotation> qualifiers = entry.getKey();
      assert qualifiers != null;
      assert qualifiers.contains(Configured.Literal.INSTANCE);
      final Set<Type> types = entry.getValue();
      assert types != null;
      for (final Type type : types) {
        if (noBeans(beanManager, type, qualifiers)) {
          final Set<Annotation> newQualifiers = new HashSet<>(qualifiers);
          newQualifiers.removeIf(e -> e instanceof Configured);
          if (newQualifiers.isEmpty()) {
            newQualifiers.add(Default.Literal.INSTANCE);
          }
          final Annotation[] qualifiersArray = newQualifiers.toArray(new Annotation[newQualifiers.size()]);
          final Set<Bean<?>> nonConfiguredBeans = beanManager.getBeans(type, qualifiersArray);
          if (nonConfiguredBeans != null && !nonConfiguredBeans.isEmpty()) {
            @SuppressWarnings("unchecked")
            final Bean<T> bean = (Bean<T>)beanManager.resolve(nonConfiguredBeans);
            assert bean.getTypes().contains(type);
            event.<T>addBean()
              .scope(bean.getScope())
              .types(type)
              .beanClass(bean.getBeanClass())
              .qualifiers(qualifiers)
              .createWith(cc -> {
                  Set<Bean<?>> settingsBeans = beanManager.getBeans(Settings.class, qualifiersArray);
                  if (settingsBeans == null || settingsBeans.isEmpty()) {
                    settingsBeans = beanManager.getBeans(Settings.class);
                  }
                  final Bean<?> settingsBean = beanManager.resolve(settingsBeans);
                  final Settings settings = (Settings)beanManager.getReference(settingsBean, Settings.class, cc);
                  
                  final T contextualInstance = bean.create(cc);
                  try {
                    settings.configure(contextualInstance, qualifiers);
                  } catch (final IntrospectionException | ReflectiveOperationException exception) {
                    throw new CreationException(exception.getMessage(), exception);
                  }
                  return contextualInstance;
                })
              .destroyWith((contextualInstance, cc) -> bean.destroy(contextualInstance, cc));
          }          
        }
      }
    }
  }
  
  private final void validate(@Observes final AfterDeploymentValidation event,
                              final BeanManager beanManager) {
    final CreationalContext<?> cc = beanManager.createCreationalContext(null);
    try {
      for (final InjectionPoint settingInjectionPoint : this.settingInjectionPoints) {
        beanManager.validate(settingInjectionPoint);
        beanManager.getInjectableReference(settingInjectionPoint, cc);
      }
    } finally {
      cc.release();
    }
    this.settingInjectionPoints.clear();
    this.settingQualifierSets.clear();
    this.settingsQualifierSets.clear();
    this.configuredTypes.clear();
  }


  /*
   * Utility methods.
   */


  private final void addBean(final AfterBeanDiscovery event,
                             final BeanManager beanManager,
                             final Type type,
                             final Set<Set<Annotation>> qualifiersSets,
                             final BeanAdder beanAdder) {
    for (final Set<Annotation> qualifiers : qualifiersSets) {
      if (noBeans(beanManager, type, qualifiers)) {
        beanAdder.addBean(event, beanManager, type, qualifiers);
      }
    }
  }


  /*
   * Static methods.
   */


  /**
   * A <strong>method template that must not be called
   * directly</strong> used in building <a
   * href="https://lairdnelson.wordpress.com/2017/02/11/dynamic-cdi-producer-methods/">dynamic
   * producer methods</a>.
   *
   * @param injectionPoint the injection point for which a setting
   * value is destined; must not be {@code null}
   *
   * @param settings an appropriate {@link Settings} to use to
   * {@linkplain Settings#get(String, Set, Type, Supplier) acquire a
   * value}; must not be {@code null}
   *
   * @return a value for a setting, or {@code null}
   *
   * @nullability This method may return {@code null}.
   *
   * @idempotency No guarantees with respect to idempotency are made
   * of this method.
   *
   * @threadsafety This method is safe for concurrent use by mulitple
   * threads.
   *
   * @see <a
   * href="https://lairdnelson.wordpress.com/2017/02/11/dynamic-cdi-producer-methods/">Dynamic
   * CDI Producer Methods</a>
   *
   * @deprecated This method should be called only by the CDI container.
   */
  @Dependent
  @Deprecated
  private static final Object producerMethodTemplate(final InjectionPoint injectionPoint,
                                                     final BeanManager beanManager,
                                                     final Settings settings) {
    Objects.requireNonNull(injectionPoint);
    Objects.requireNonNull(settings);
    final Set<Annotation> qualifiers = new HashSet<>(Objects.requireNonNull(injectionPoint.getQualifiers()));
    qualifiers.removeIf(e -> e instanceof Setting);
    if (qualifiers.isEmpty()) {
      qualifiers.add(Default.Literal.INSTANCE);
    }
    return settings.get(getName(injectionPoint),
                        qualifiers,
                        injectionPoint.getType(),
                        getDefaultValueSupplier(injectionPoint, beanManager));
  }

  private static final Setting extractSetting(final InjectionPoint injectionPoint) {
    Setting returnValue = null;
    if (injectionPoint != null) {
      returnValue = extractSetting(injectionPoint.getQualifiers());
    }
    return returnValue;
  }

  private static final Setting extractSetting(final Set<Annotation> qualifiers) {
    Setting returnValue = null;
    if (qualifiers != null && !qualifiers.isEmpty()) {
      for (final Annotation qualifier : qualifiers) {
        if (qualifier instanceof Setting) {
          returnValue = (Setting)qualifier;
          break;
        }
      }
    }
    return returnValue;
  }

  private static final Supplier<? extends String> getDefaultValueSupplier(final InjectionPoint injectionPoint,
                                                                          final BeanManager beanManager) {
    Objects.requireNonNull(injectionPoint);
    Objects.requireNonNull(beanManager);
    final Setting setting = Objects.requireNonNull(extractSetting(injectionPoint));
    final Supplier<? extends String> returnValue;
    if (setting.required()) {
      returnValue = () -> {
        final Set<Annotation> qualifiers = new HashSet<>(Objects.requireNonNull(injectionPoint.getQualifiers()));
        qualifiers.removeIf(e -> e instanceof Setting);
        if (qualifiers.isEmpty()) {
          qualifiers.add(Default.Literal.INSTANCE);
        }
        throw new UnsatisfiedResolutionException("No value was found in any source for the setting named " + getName(injectionPoint) + " with qualifiers " + qualifiers);
      };
    } else {
      final String defaultValue = setting.defaultValue();
      if (defaultValue == null || defaultValue.equals(Setting.UNSET)) {
        returnValue = SettingsExtension::returnNull;
      } else {
        returnValue = setting::defaultValue;
      }
    }
    return returnValue;
  }

  private static final String getName(final InjectionPoint injectionPoint) {
    return getName(extractSetting(injectionPoint), injectionPoint.getAnnotated());
  }

  private static final String getName(final Setting setting, final Annotated annotated) {
    Objects.requireNonNull(setting);
    Objects.requireNonNull(annotated);
    String name = setting.name();
    if (name == null || name.isEmpty() || name.equals(Setting.UNSET)) {
      if (annotated instanceof AnnotatedField) {
        name = ((AnnotatedField)annotated).getJavaMember().getName();
      } else if (annotated instanceof AnnotatedParameter) {
        final AnnotatedParameter<?> annotatedParameter = (AnnotatedParameter<?>)annotated;
        final int parameterIndex = annotatedParameter.getPosition();
        final Member member = annotatedParameter.getDeclaringCallable().getJavaMember();
        final Parameter[] parameters = ((Executable)member).getParameters();
        final Parameter parameter = parameters[parameterIndex];
        if (parameter.isNamePresent()) {
          name = parameter.getName();
        } else {
          throw new IllegalStateException("The parameter at index " +
                                          parameterIndex +
                                          " in " +
                                          member +
                                          " did not have a name available via reflection. " +
                                          "Make sure you compiled its enclosing class, " +
                                          member.getDeclaringClass().getName() +
                                          ", with the -parameters option supplied to javac, " +
                                          " or make use of the name() element of the " +
                                          Setting.class.getName() +
                                          " annotation.");
        }
      }
    }
    return name;
  }

  private static final String returnNull() {
    return null;
  }

  private static final Set<InjectionPoint> qualifyInjectionPoints(final Set<InjectionPoint> injectionPoints,
                                                                  final Set<Annotation> qualifiers) {
    final Set<InjectionPoint> returnValue = new HashSet<>();
    for (final InjectionPoint injectionPoint : injectionPoints) {
      final Type injectionPointType = injectionPoint.getType();
      if (InjectionPoint.class.equals(injectionPointType) || BeanManager.class.equals(injectionPointType)) {
        returnValue.add(injectionPoint);
      } else {
        returnValue.add(qualifyInjectionPoint(injectionPoint, qualifiers));
      }
    }
    return returnValue;
  }

  private static final InjectionPoint qualifyInjectionPoint(final InjectionPoint injectionPoint,
                                                            final Set<Annotation> qualifiers) {
    final InjectionPoint returnValue;
    final Set<Annotation> originalQualifiers = injectionPoint.getQualifiers();
    if (originalQualifiers == null || originalQualifiers.isEmpty()) {
      if (qualifiers == null || qualifiers.isEmpty()) {
        returnValue = injectionPoint;
      } else {
        returnValue = new FlexiblyQualifiedInjectionPoint(injectionPoint, qualifiers);
      }
    } else if (qualifiers == null || qualifiers.isEmpty() || originalQualifiers.equals(qualifiers)) {
      returnValue = injectionPoint; // just leave it alone
    } else {
      final Set<Annotation> newQualifiers = new HashSet<>(qualifiers);
      newQualifiers.addAll(originalQualifiers);
      returnValue = new FlexiblyQualifiedInjectionPoint(injectionPoint, newQualifiers);
    }
    return returnValue;
  }

  private static final boolean noBeans(final BeanManager beanManager, final Type type, final Set<Annotation> qualifiers) {
    Objects.requireNonNull(beanManager);
    Objects.requireNonNull(type);
    final Collection<?> beans;
    if (qualifiers == null || qualifiers.isEmpty()) {
      beans = beanManager.getBeans(type);
    } else {
      beans = beanManager.getBeans(type, qualifiers.toArray(new Annotation[qualifiers.size()]));
    }
    return beans == null || beans.isEmpty();
  }

  static Type synthesizeLegalBeanType(final Type type) {
    return synthesizeLegalBeanType(type, -1, 0);
  }

  static Type synthesizeLegalBeanType(final Type type, final int depth) {
    return synthesizeLegalBeanType(type, Math.max(0, depth), 0);
  }
  
  static Type synthesizeLegalBeanType(final Type type, final int depth, final int currentLevel) {
    final Type returnValue;
    if (type instanceof Class || depth == 0 || (depth > 0 && currentLevel > depth)) {
      returnValue = type;
    } else if (type instanceof ParameterizedType) {
      final ParameterizedType ptype = (ParameterizedType)type;
      final Type rawType = ptype.getRawType();
      assert rawType instanceof Class;
      final Type[] actualTypeArguments = ptype.getActualTypeArguments();
      assert actualTypeArguments != null;
      assert actualTypeArguments.length > 0;
      final Collection<Type> newTypeArguments = new ArrayList<>();
      for (final Type actualTypeArgument : actualTypeArguments) {
        newTypeArguments.add(synthesizeLegalBeanType(actualTypeArgument, depth, currentLevel + 1)); // XXX recursive
      }
      returnValue = new ParameterizedTypeImplementation(ptype.getOwnerType(),
                                                        (Class<?>)rawType,
                                                        newTypeArguments.toArray(new Type[newTypeArguments.size()]));
    } else if (type instanceof WildcardType) {
      final WildcardType wtype = (WildcardType)type;
      final Type[] upperBounds = wtype.getUpperBounds();
      assert upperBounds != null;
      assert upperBounds.length > 0;
      final Type[] lowerBounds = wtype.getLowerBounds();
      assert lowerBounds != null;
      if (lowerBounds.length == 0) {
        // Upper-bounded wildcard, e.g. ? extends Something
        if (upperBounds.length == 1) {
          // Turn ? extends Something into Something
          returnValue = synthesizeLegalBeanType(upperBounds[0], depth, currentLevel + 1); // XXX recursive
        } else {
          // Too complicated/unsupported; just let it fly and CDI will
          // fail later
          returnValue = type;
        }
      } else {
        // Lower-bounded wildcard, e.g. ? super Something
        assert upperBounds.length == 1;
        assert Object.class.equals(upperBounds[0]);
        if (lowerBounds.length == 1) {
          // Turn ? super Something into Something
          returnValue = synthesizeLegalBeanType(lowerBounds[0], depth, currentLevel + 1); // XXX recursive
        } else {
          // Too complicated/unsupported; just let it fly and CDI will
          // fail later
          returnValue = type;
        }
      }
    } else if (type instanceof TypeVariable) {
      final TypeVariable<?> tv = (TypeVariable<?>)type;
      final Type[] bounds = tv.getBounds();
      assert bounds != null;
      assert bounds.length > 0;
      if (bounds.length == 1) {
        returnValue = synthesizeLegalBeanType(bounds[0], depth, currentLevel + 1); // XXX recursive
      } else {
        // Too complicated/unsupported; just let it fly and CDI will
        // fail later
        returnValue = type;
      }
    } else if (type instanceof GenericArrayType) {
      returnValue = type;
    } else {
      throw new IllegalArgumentException("Unsupported Type implementation: " + type);
    }
    return returnValue;
  }


  /*
   * Inner and nested classes.
   */


  @FunctionalInterface
  private interface BeanAdder {

    void addBean(final AfterBeanDiscovery event,
                 final BeanManager beanManager,
                 final Type type,
                 final Set<Annotation> qualifiers);

  }

  private static class DelegatingBeanAttributes<T> implements BeanAttributes<T> {

    private final BeanAttributes<?> delegate;

    protected DelegatingBeanAttributes(final BeanAttributes<?> delegate) {
      super();
      this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public String getName() {
      return this.delegate.getName();
    }

    @Override
    public Set<Annotation> getQualifiers() {
      return this.delegate.getQualifiers();
    }

    @Override
    public Class<? extends Annotation> getScope() {
      return this.delegate.getScope();
    }

    @Override
    public Set<Class<? extends Annotation>>	getStereotypes() {
      return this.delegate.getStereotypes();
    }

    @Override
    public Set<Type> getTypes() {
      return this.delegate.getTypes();
    }

    @Override
    public boolean isAlternative() {
      return this.delegate.isAlternative();
    }

    @Override
    public String toString() {
      return this.delegate.toString();
    }

  }

  private static class FlexiblyTypedBeanAttributes<T> extends DelegatingBeanAttributes<T> {

    private final Set<Annotation> qualifiers;

    private final Set<Type> types;

    private FlexiblyTypedBeanAttributes(final BeanAttributes<?> delegate,
                                        final Set<Annotation> qualifiers,
                                        final Set<Type> types) {
      super(delegate);
      if (qualifiers == null) {
        this.qualifiers = null;
      } else if (qualifiers.isEmpty()) {
        this.qualifiers = Collections.emptySet();
      } else {
        this.qualifiers = Collections.unmodifiableSet(qualifiers);
      }
      if (types == null) {
        this.types = null;
      } else if (types.isEmpty()) {
        this.types = Collections.emptySet();
      } else {
        this.types = Collections.unmodifiableSet(types);
      }
    }

    @Override
    public Set<Annotation> getQualifiers() {
      return this.qualifiers;
    }

    @Override
    public Set<Type> getTypes() {
      return this.types;
    }

  }

  private static class DelegatingInjectionPoint implements InjectionPoint {

    private final InjectionPoint delegate;

    protected DelegatingInjectionPoint(final InjectionPoint delegate) {
      super();
      this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public Annotated getAnnotated() {
      return this.delegate.getAnnotated();
    }

    @Override
    public Bean<?> getBean() {
      return this.delegate.getBean();
    }

    @Override
    public Member getMember() {
      return this.delegate.getMember();
    }

    @Override
    public Set<Annotation> getQualifiers() {
      return this.delegate.getQualifiers();
    }

    @Override
    public Type getType() {
      return this.delegate.getType();
    }

    @Override
    public boolean isDelegate() {
      return this.delegate.isDelegate();
    }

    @Override
    public boolean isTransient() {
      return this.delegate.isTransient();
    }

  }

  private static class FlexiblyQualifiedInjectionPoint extends DelegatingInjectionPoint {

    private final Set<Annotation> qualifiers;

    private FlexiblyQualifiedInjectionPoint(final InjectionPoint delegate,
                                            final Set<Annotation> qualifiers) {
      super(delegate);
      if (qualifiers == null) {
        this.qualifiers = null;
      } else if (qualifiers.isEmpty()) {
        this.qualifiers = Collections.emptySet();
      } else {
        this.qualifiers = Collections.unmodifiableSet(qualifiers);
      }
    }

    @Override
    public Set<Annotation> getQualifiers() {
      return this.qualifiers;
    }

  }

  private static class DelegatingProducer<T> implements Producer<T> {

    private final Producer<T> delegate;

    protected DelegatingProducer(final Producer<T> producer) {
      super();
      this.delegate = Objects.requireNonNull(producer);
    }

    @Override
    public void dispose(final T instance) {
      this.delegate.dispose(instance);
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
      return this.delegate.getInjectionPoints();
    }

    @Override
    public T produce(final CreationalContext<T> cc) {
      return this.delegate.produce(cc);
    }

  }

}
