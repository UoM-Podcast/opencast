/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.pm.impl.persistence;

import static org.opencastproject.util.RequireUtil.notEmpty;

import org.opencastproject.pm.api.PersonType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Entity object for person type.
 */
@Entity(name = "PersonType")
@Table(name = "mh_pm_person_type", uniqueConstraints = { @UniqueConstraint(columnNames = { "name" }) })
@NamedQueries({ @NamedQuery(name = "PersonType.findAll", query = "SELECT p FROM PersonType p"),
        @NamedQuery(name = "PersonType.findByName", query = "SELECT p FROM PersonType p WHERE p.name = :name"),
        @NamedQuery(name = "PersonType.clear", query = "DELETE FROM PersonType") })
public class PersonTypeDto {

  @Id
  @GeneratedValue
  @Column(name = "id")
  private long id;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "function_name", nullable = false)
  private String function;

  /**
   * Default constructor
   */
  public PersonTypeDto() {
  }

  /**
   * Creates a person type
   * 
   * @param name
   *          the name
   * @param function
   *          the function
   */
  public PersonTypeDto(String name, String function) {
    this.name = notEmpty(name, "name");
    this.function = notEmpty(function, "function");
  }

  /**
   * Returns the id of this entity
   * 
   * @return the id as long
   */
  public long getId() {
    return id;
  }

  /**
   * Sets the name
   * 
   * @param name
   *          the name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the name
   * 
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the function
   * 
   * @param function
   *          the function
   */
  public void setFunction(String function) {
    this.function = function;
  }

  /**
   * Returns the function
   * 
   * @return the function
   */
  public String getFunction() {
    return function;
  }

  /**
   * Returns the business object of the person type
   * 
   * @return the business object model of this person type
   */
  public PersonType toPersonType() {
    PersonType personType = new PersonType(name, function);
    personType.setId(id);
    return personType;
  }
}
