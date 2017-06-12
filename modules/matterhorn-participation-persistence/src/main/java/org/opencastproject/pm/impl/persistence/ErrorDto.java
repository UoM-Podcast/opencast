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

import org.opencastproject.pm.api.Error;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * Entity object for synchronization error data.
 */
@Entity(name = "Error")
@Table(name = "mh_pm_error")
@NamedQueries({ @NamedQuery(name = "Error.findAll", query = "SELECT e FROM Error e"),
        @NamedQuery(name = "Error.clear", query = "DELETE FROM Error") })
public class ErrorDto {

  @Id
  @GeneratedValue
  @Column(name = "id")
  private long id;

  @Column(name = "type", nullable = false)
  private String type;

  @Column(name = "description")
  private String description;

  @Lob
  @Column(name = "source", length = 65535)
  private String source;

  /**
   * Default constructor
   */
  public ErrorDto() {
  }

  /**
   * Creates an error
   * 
   * @param type
   *          the error type
   * @param description
   *          the error description
   * @param source
   *          the error source
   */
  public ErrorDto(String type, String description, String source) {
    this.type = type;
    this.description = description;
    this.source = source;
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
   * Sets the error type
   * 
   * @param type
   *          the type
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * Returns the type
   * 
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the error description
   * 
   * @param description
   *          the description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Returns the description
   * 
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the error source
   * 
   * @param source
   *          the source
   */
  public void setSource(String source) {
    this.source = source;
  }

  /**
   * Returns the source
   * 
   * @return the source
   */
  public String getSource() {
    return source;
  }

  /**
   * Returns the business object of this error
   * 
   * @return the business object model of this error
   */
  public Error toError() {
    Error error = new Error(type, description, source);
    error.setId(id);
    return error;
  }

}
