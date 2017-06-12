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

import org.opencastproject.pm.api.Action;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * Entity object for the action data.
 */
@Entity(name = "Action")
@Table(name = "mh_pm_action")
@NamedQueries({ @NamedQuery(name = "Action.findAll", query = "SELECT a FROM Action a"),
        @NamedQuery(name = "Action.clear", query = "DELETE FROM Action") })
public class ActionDto {

  @Id
  @GeneratedValue
  @Column(name = "id")
  private long id;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "handler", nullable = false)
  private String handler;

  /**
   * Default constructor
   */
  public ActionDto() {
  }

  /**
   * Creates an action
   * 
   * @param name
   *          the name
   * @param handler
   *          the handler
   */
  public ActionDto(String name, String handler) {
    this.handler = notEmpty(handler, "name");
    this.name = notEmpty(handler, "handler");
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
   * Sets the handler
   * 
   * @param handler
   *          the handler
   */
  public void setHandler(String handler) {
    this.handler = handler;
  }

  /**
   * Returns the handler
   * 
   * @return the handler
   */
  public String getHandler() {
    return handler;
  }

  /**
   * Returns the business object of this action
   * 
   * @return the business object model of this action
   */
  public Action toAction() {
    return new Action(id, name, handler);
  }

}
