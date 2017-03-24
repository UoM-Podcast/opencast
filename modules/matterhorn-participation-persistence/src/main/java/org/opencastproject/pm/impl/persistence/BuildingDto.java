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
import static org.opencastproject.util.RequireUtil.notNull;

import org.opencastproject.pm.api.Building;
import org.opencastproject.pm.api.Room;

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
 * Entity object for the buidling data.
 */
@Entity(name = "Building")
@Table(name = "mh_pm_building", uniqueConstraints = { @UniqueConstraint(columnNames = { "name" }) })
@NamedQueries({ @NamedQuery(name = "Building.findAll", query = "SELECT b FROM Building b"),
        @NamedQuery(name = "Building.findByName", query = "SELECT b FROM Building b WHERE b.name = :name"),
        @NamedQuery(name = "Building.clear", query = "DELETE FROM Building") })
public class BuildingDto {

  @Id
  @GeneratedValue
  @Column(name = "id")
  private long id;

  @Column(name = "name", nullable = false)
  private String name;

  @OneToMany(fetch = FetchType.EAGER, cascade = { CascadeType.REMOVE })
  private List<RoomDto> rooms = new ArrayList<RoomDto>();

  /**
   * Default constructor
   */
  public BuildingDto() {
  }

  /**
   * Creates a building
   * 
   * @param name
   *          the name
   * @param rooms
   *          the list of rooms
   */
  public BuildingDto(String name, List<RoomDto> rooms) {
    this.name = notEmpty(name, "name");
    this.rooms = notNull(rooms, "rooms");
  }

  /**
   * Creates a new building with an empty list of rooms
   * 
   * @param name
   *          the name
   */
  public BuildingDto(String name) {
    this.name = notEmpty(name, "name");
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
   * Sets the room list
   * 
   * @param rooms
   *          the room list
   */
  public void setRooms(List<RoomDto> rooms) {
    this.rooms = notNull(rooms, "rooms");
  }

  /**
   * Returns the room list
   * 
   * @return the room list
   */
  public List<RoomDto> getRooms() {
    return rooms;
  }

  /**
   * Add a room to the building
   * 
   * @param room
   *          the room to add to this building
   * @return true if this collection changed as a result of the call
   */
  public boolean addRoom(RoomDto room) {
    return rooms.add(notNull(room, "room"));
  }

  /**
   * Remove a room from the building
   * 
   * @param room
   *          the room to remove from this building
   * @return true if this collection changed as a result of the call
   */
  public boolean removeRoom(Room room) {
    return rooms.remove(notNull(room, "room"));
  }

  /**
   * Returns the business object of this building
   * 
   * @return the business object of this building
   */
  public Building toBuilding() {
    Building building = new Building(name);
    for (RoomDto r : this.rooms) {
      building.addRoom(r.toRoom());
    }
    building.setId(id);
    return building;
  }
}
