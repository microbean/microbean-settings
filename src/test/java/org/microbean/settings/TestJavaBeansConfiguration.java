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

import java.beans.IntrospectionException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TestJavaBeansConfiguration {


  /*
   * Constructors.
   */


  public TestJavaBeansConfiguration() {
    super();
  }


  /*
   * Instance methods.
   */


  @Before
  public void startContainer() throws Exception {
    System.setProperty("abe.firstName", "Abraham");
    System.setProperty("abe.lastName", "Lincoln");
    
  }

  @After
  public void stopContainer() throws Exception {
    System.clearProperty("abe.firstName");
    System.clearProperty("abe.lastName");
  }

  @Test
  public void testConfiguration() throws IntrospectionException, ReflectiveOperationException {
    final Person person = new Person();
    person.setAge(211);
    final Settings settings = new Settings();
    assertNull(person.getFirstName());
    assertNull(person.getLastName());
    settings.configure(person, "abe.");
    assertEquals("Abraham", person.getFirstName());
    assertEquals("Lincoln", person.getLastName());
    assertEquals(211, person.getAge());
  }

  private static final class Person {

    private String firstName;

    private String lastName;

    private int age;
    
    private Person() {
      super();
    }

    public int getAge() {
      return this.age;
    }

    public void setAge(final int age) {
      this.age = age;
    }

    public String getFirstName() {
      return this.firstName;
    }

    public void setFirstName(final String firstName) {
      this.firstName = firstName;
    }

    public String getLastName() {
      return this.lastName;
    }

    public void setLastName(final String lastName) {
      this.lastName = lastName;
    }

  }

}
