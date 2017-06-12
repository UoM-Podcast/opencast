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

import static org.opencastproject.util.RequireUtil.notNull;

import org.opencastproject.pm.api.Person;
import org.opencastproject.pm.api.PersonType;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Entity object for person.
 */
@Entity(name = "Person")
@Table(name = "mh_pm_person", uniqueConstraints = { @UniqueConstraint(columnNames = { "email" }) })
@NamedQueries({
        @NamedQuery(name = "Person.findAll", query = "SELECT p FROM Person p"),
        @NamedQuery(name = "Person.findByEmailLike", query = "SELECT p FROM Person p WHERE UPPER(p.email) LIKE :query"),
        @NamedQuery(name = "Person.findByEmail", query = "SELECT p FROM Person p WHERE p.email = :email"),
        @NamedQuery(name = "Person.count", query = "SELECT COUNT(p) FROM Person p"),
        @NamedQuery(name = "Person.clear", query = "DELETE FROM Person") })
public class PersonDto implements BlacklistableDto {

  public static final String TYPE = "person";

  @Id
  @GeneratedValue
  @Column(name = "id")
  private long id;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "email", nullable = false)
  private String email;

  @OneToMany(fetch = FetchType.EAGER, cascade = { CascadeType.REMOVE })
  private List<PersonTypeDto> personTypes = new ArrayList<PersonTypeDto>();

  /**
   * Default constructor
   */
  public PersonDto() {
  }

  /**
   * Creates a person
   * 
   * @param name
   *          the name
   * @param email
   *          the email
   * @param personType
   *          the person type
   */
  public PersonDto(String name, String email, List<PersonTypeDto> personTypes) {
    this.name = name;
    this.email = email;
    this.personTypes = notNull(personTypes, "personTypes");
  }

  /**
   * Creates a person without type
   * 
   * @param name
   *          the name
   * @param email
   *          the email
   */
  public PersonDto(String name, String email) {
    this.name = name;
    this.email = email;
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
   * Sets the email
   * 
   * @param email
   *          the email
   */
  public void setEmail(String email) {
    this.email = email;
  }

  /**
   * Returns the email
   * 
   * @return the email
   */
  public String getEmail() {
    return email;
  }

  /**
   * Sets the person types
   * 
   * @param personTypes
   *          the person types
   */
  public void setPersonTypes(List<PersonTypeDto> personTypes) {
    this.personTypes = personTypes;
  }

  /**
   * Returns the person types
   * 
   * @return the person types
   */
  public List<PersonTypeDto> getPersonTypes() {
    return personTypes;
  }

  /**
   * Add a type to the person
   * 
   * @param type
   *          the type to add to this person
   * @return true if this collection changed as a result of the call
   */
  public boolean addType(PersonTypeDto type) {
    return personTypes.add(notNull(type, "type"));
  }

  /**
   * Remove a type from the person
   * 
   * @param type
   *          the type to remove from this person
   * @return true if this collection changed as a result of the call
   */
  public boolean removeType(PersonType type) {
    return personTypes.remove(notNull(type, "type"));
  }

  /**
   * Returns the business object of the person
   * 
   * @return the business object model of this person
   */
  public Person toPerson() {
    Person person = Person.person(name, email);
    for (PersonTypeDto pt : this.personTypes) {
      person.addType(pt.toPersonType());
    }
    person.setId(id);
    return person;
  }

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public long getId() {
    return id;
  }
}
