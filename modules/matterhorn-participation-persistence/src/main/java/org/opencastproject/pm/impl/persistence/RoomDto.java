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

import org.opencastproject.pm.api.Room;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Entity object for the room data.
 */
@Entity(name = "Room")
@Table(name = "mh_pm_room", uniqueConstraints = { @UniqueConstraint(columnNames = { "name" }) })
@NamedQueries({ @NamedQuery(name = "Room.findAll", query = "SELECT r FROM Room r"),
        @NamedQuery(name = "Room.findByName", query = "SELECT r FROM Room r where r.name = :name"),
        @NamedQuery(name = "Room.clear", query = "DELETE FROM Room") })
public class RoomDto implements BlacklistableDto {

  public static final String TYPE = "room";

  @Id
  @GeneratedValue
  @Column(name = "id")
  private long id;

  @Column(name = "name", nullable = false)
  private String name;

  /**
   * Default constructor
   */
  public RoomDto() {
  }

  /**
   * Creates a room
   * 
   * @param name
   *          the name
   */
  public RoomDto(String name) {
    this.name = notEmpty(name, "name");
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
   * Returns the business object of this room
   * 
   * @return the business object model of this room
   */
  public Room toRoom() {
    Room room = new Room(name);
    room.setId(id);
    return room;
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
