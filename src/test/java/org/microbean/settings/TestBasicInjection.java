/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2019 microBean™.
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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;

import javax.enterprise.event.Observes;

import javax.enterprise.inject.se.SeContainerInitializer;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@ApplicationScoped
public class TestBasicInjection {


  /*
   * Instance fields.
   */


  private AutoCloseable container;

  @Inject
  @Setting(name = "java.home")
  private String javaHome;

  @Inject
  @Setting(name = "test.map")
  private Map<String, String> testMap;

  @Inject
  @Setting(name = "test.map")
  private Set<Entry<String, String>> testEntrySet;

  @Inject
  @Setting(name = "crap", required = true, defaultValue = "garbage")
  private List<String> crap;
  
  @Inject
  @Setting(name = "test.list")
  private List<? extends String> testList;

  @Inject
  @Setting(name = "cls", defaultValue = "java.lang.Integer")
  private Class<?> cls;

  @Inject
  @Setting(name = "nonexistent", defaultValue = "${settings[\"java.home\"]}")
  private String nonexistent;
  

  /*
   * Constructors.
   */


  public TestBasicInjection() {
    super();
  }


  /*
   * Instance methods.
   */


  @Before
  public void startContainer() throws Exception {
    this.stopContainer();
    System.setProperty("cls", "java.lang.Integer");
    System.setProperty("test.map", "a=b,c=d,e=f");
    System.setProperty("test.list", "a,b,c");
    final SeContainerInitializer initializer = SeContainerInitializer.newInstance();
    initializer.addBeanClasses(TestBasicInjection.class);
    this.container = initializer.initialize();
  }

  @After
  public void stopContainer() throws Exception {
    if (this.container != null) {
      this.container.close();
      this.container = null;
      System.clearProperty("test.map");
      System.clearProperty("test.list");
      System.clearProperty("cls");
    }
  }

  private void onStartup(@Observes @Initialized(ApplicationScoped.class) final Object event,
                         @Setting(name = "test.list") final String testSet) {
    assertNotNull(event);
    assertEquals("a,b,c", testSet);
    assertNotNull(this.testMap);
    assertEquals(3, this.testMap.size());
    assertEquals("b", this.testMap.get("a"));
    assertEquals("d", this.testMap.get("c"));
    assertEquals("f", this.testMap.get("e"));
    assertEquals(Integer.class, this.cls);
    assertEquals(this.javaHome, nonexistent);
  }

  @Test
  public void testContainerStartup() {

  }

}
