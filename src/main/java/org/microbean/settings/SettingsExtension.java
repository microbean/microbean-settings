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

import java.lang.annotation.Annotation;

import java.lang.reflect.Executable;
import java.lang.reflect.Member;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.StandardELContext;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;

import javax.enterprise.context.spi.CreationalContext;

import javax.enterprise.event.Observes;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;

import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.Producer;
import javax.enterprise.inject.spi.ProducerFactory;

import javax.enterprise.util.TypeLiteral;

import javax.inject.Singleton;

public class SettingsExtension implements Extension {

  private final Set<Set<Annotation>> settingsQualifierSets;

  private final Set<Type> knownConversionTypes;

  public SettingsExtension() {
    super();
    this.settingsQualifierSets = new HashSet<>();
    this.knownConversionTypes = new HashSet<>(Collections.singleton(String.class));
  }

  private final <T, X extends Settings> void processSettingsInjectionPoint(@Observes final ProcessInjectionPoint<T, X> event) {
    final InjectionPoint injectionPoint = event.getInjectionPoint();
    final Set<Annotation> injectionPointQualifiers = injectionPoint.getQualifiers();
    assert injectionPointQualifiers != null;
    final Set<Annotation> qualifiers = new HashSet<>(injectionPointQualifiers);
    this.settingsQualifierSets.add(qualifiers);
  }

  private final <T, X> void processSettingInjectionPoint(@Observes final ProcessInjectionPoint<T, X> event) {
    final InjectionPoint injectionPoint = event.getInjectionPoint();
    final Type type = injectionPoint.getType();
    if (!Settings.class.equals(type)) {
      final Set<Annotation> injectionPointQualifiers = injectionPoint.getQualifiers();
      boolean containsSetting = false;
      for (final Annotation qualifier : injectionPointQualifiers) {
        if (qualifier instanceof Setting) {
          containsSetting = true;
          break;
        }
      }
      if (containsSetting) {
        this.knownConversionTypes.add(type);
        final Set<Annotation> qualifiers = new HashSet<>(injectionPointQualifiers);
        this.settingsQualifierSets.add(qualifiers);
      }
    }
  }

  private final void installConverterProviderBeans(@Observes final AfterBeanDiscovery event,
                                                   final BeanManager beanManager) {
    addBean(event,
            beanManager,
            (e, bm, nq) -> e.addBean()
              .types(ConverterProvider.class)
              .scope(Singleton.class)
              .qualifiers(nq)
              .createWith(cc -> new BeanManagerBackedConverterProvider(bm, nq)));
  }

  private final void installSourcesSupplierBeans(@Observes final AfterBeanDiscovery event,
                                                 final BeanManager beanManager) {
    addBean(event,
            beanManager,
            (e, bm, nq) -> e.addBean()
              .types(new TypeLiteral<BiFunction<String, Set<Annotation>, Set<Source>>>() {
                  private static final long serialVersionUID = 1L;
                }.getType())
              .scope(Singleton.class) // no need for proxies/ApplicationScoped
              .qualifiers(nq)
              .createWith(cc -> new BeanManagerBackedSourcesSupplier(bm)));
  }

  private final void installSettingsBeans(@Observes final AfterBeanDiscovery event,
                                          final BeanManager beanManager) {
    addBean(event,
            beanManager,
            (e, bm, nq) -> e.addBean()
              .addTransitiveTypeClosure(Settings.class)
              .scope(Singleton.class) // no need for proxies/ApplicationScoped
              .qualifiers(nq)
              .beanClass(Settings.class)
              .produceWith(instance -> {
                final Annotation[] qualifiersArray = nq.toArray(new Annotation[nq.size()]);   
                final BiFunction<? super String, ? super Set<Annotation>, ? extends Set<? extends Source>> sourcesSupplier =
                  instance.select(new TypeLiteral<BiFunction<? super String, ? super Set<Annotation>, ? extends Set<? extends Source>>>() {
                    private static final long serialVersionUID = 1L;
                  },
                  qualifiersArray).get();
                final ConverterProvider converterProvider = instance.select(ConverterProvider.class, qualifiersArray).get();
                final Iterable<? extends Arbiter> arbiters = instance.select(Arbiter.class, qualifiersArray);
                return new Settings(sourcesSupplier, converterProvider, arbiters);
              }));
  }

  private final void installSettingProducers(@Observes final AfterBeanDiscovery event,
                                             final BeanManager beanManager) {
    if (!this.settingsQualifierSets.isEmpty()) {
      final AnnotatedType<SettingsExtension> annotatedType = beanManager.createAnnotatedType(SettingsExtension.class);
      final AnnotatedMethod<? super SettingsExtension> producerMethodTemplate = annotatedType.getMethods()
        .stream()
        .filter(m -> m.getJavaMember().getName().equals("producerMethodTemplate"))
        .findFirst() // ...and only
        .get();
      final BeanAttributes<?> delegate = beanManager.createBeanAttributes(producerMethodTemplate);
      for (final Set<Annotation> qualifiers : this.settingsQualifierSets) {
        for (final Type type : this.knownConversionTypes) {
          final ProducerFactory<SettingsExtension> containerProducerFactory = beanManager.getProducerFactory(producerMethodTemplate, null);
          final ProducerFactory<SettingsExtension> producerFactory = new ProducerFactory<SettingsExtension>() {
              @Override
              public final <T> Producer<T> createProducer(final Bean<T> bean) {
                final Producer<T> delegateProducer = containerProducerFactory.createProducer(bean);
                final Set<InjectionPoint> injectionPoints = qualifyInjectionPoints(delegateProducer.getInjectionPoints(), qualifiers);
                return new DelegatingProducer<T>(delegateProducer) {
                  @Override
                  public final Set<InjectionPoint> getInjectionPoints() {
                    return injectionPoints;
                  }
                };
              }
            };
          final BeanAttributes<?> beanAttributes = new FlexiblyTypedBeanAttributes<Object>(delegate, qualifiers, Collections.singleton(type));
          final Bean<?> bean = beanManager.createBean(beanAttributes, SettingsExtension.class, producerFactory);
          event.addBean(bean);
        }
      }
    }
  }

  @Dependent
  private static final Object producerMethodTemplate(final InjectionPoint injectionPoint,
                                                     final Settings settings) {
    Objects.requireNonNull(injectionPoint);
    Objects.requireNonNull(settings);
    final Set<Annotation> qualifiers = new HashSet<>(Objects.requireNonNull(injectionPoint.getQualifiers()));
    qualifiers.removeIf(e -> e instanceof Setting);
    return settings.get(Objects.requireNonNull(getName(injectionPoint)),
                        qualifiers,
                        injectionPoint.getType(),
                        getDefaultValueSupplier(injectionPoint));
  }

  private final void addBean(final AfterBeanDiscovery event,
                             final BeanManager beanManager,
                             final BeanAdder beanAdder) {
    final Set<Set<Annotation>> seen = new HashSet<>();
    for (final Set<Annotation> qualifiers : this.settingsQualifierSets) {
      final Set<Annotation> newQualifiers = new HashSet<>(qualifiers);
      newQualifiers.removeIf(e -> e instanceof Setting);
      if (!seen(beanManager, seen, newQualifiers)) {
        beanAdder.addBean(event, beanManager, newQualifiers);
      }
    }
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

  private static final Supplier<? extends String> getDefaultValueSupplier(final InjectionPoint injectionPoint) {
    return getDefaultValueSupplier(Objects.requireNonNull(extractSetting(injectionPoint)));
  }

  private static final Supplier<? extends String> getDefaultValueSupplier(final Setting setting) {
    Objects.requireNonNull(setting);
    final Supplier<? extends String> returnValue;
    final String defaultValue = setting.defaultValue();
    if (defaultValue == null || defaultValue.equals(Setting.UNSET)) {
      returnValue = SettingsExtension::returnNull;
    } else {
      returnValue = setting::defaultValue;
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

  private static final boolean seen(final BeanManager beanManager,
                                    final Set<Set<Annotation>> allSeen,
                                    final Set<Annotation> qualifiers) {
    Objects.requireNonNull(allSeen);
    Objects.requireNonNull(qualifiers);
    for (final Set<Annotation> seen : allSeen) {
      for (final Annotation seenQualifier : seen) {
        for (final Annotation qualifier : qualifiers) {
          if (!beanManager.areQualifiersEquivalent(seenQualifier, qualifier)) {
            return false;
          }
        }
      }
    }
    return true;
  }

  private static final String returnNull() {
    return null;
  }

  private static final Set<InjectionPoint> qualifyInjectionPoints(final Set<InjectionPoint> injectionPoints,
                                                                  final Set<Annotation> qualifiers) {
    final Set<InjectionPoint> returnValue = new LinkedHashSet<>();
    for (final InjectionPoint injectionPoint : injectionPoints) {
      if (injectionPoint instanceof InjectionPoint) {
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
      final Set<Annotation> newQualifiers = new LinkedHashSet<>(qualifiers);
      newQualifiers.addAll(originalQualifiers);
      returnValue = new FlexiblyQualifiedInjectionPoint(injectionPoint, newQualifiers);
    }
    return returnValue;
  }


  /*
   * Inner and nested classes.
   */


  private interface BeanAdder {

    void addBean(final AfterBeanDiscovery event, final BeanManager beanManager, final Set<Annotation> qualifiers);

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
