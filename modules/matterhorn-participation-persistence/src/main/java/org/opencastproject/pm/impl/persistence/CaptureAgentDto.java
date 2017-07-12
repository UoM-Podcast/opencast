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

import org.opencastproject.pm.api.CaptureAgent;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Entity object for the capture agent.
 */
@Entity(name = "CaptureAgent")
@Table(name = "mh_pm_capture_agent", uniqueConstraints = { @UniqueConstraint(columnNames = { "mh_agent" }) })
@NamedQueries({
        @NamedQuery(name = "CaptureAgent.findAll", query = "SELECT c FROM CaptureAgent c"),
        @NamedQuery(name = "CaptureAgent.findByAgentId", query = "SELECT c FROM CaptureAgent c WHERE c.mhAgent = :agent"),
        @NamedQuery(name = "CaptureAgent.clear", query = "DELETE FROM CaptureAgent") })
public class CaptureAgentDto {

  @Id
  @GeneratedValue
  @Column(name = "id")
  private long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "room", referencedColumnName = "id")
  private RoomDto room;

  @Column(name = "mh_agent", nullable = false)
  private String mhAgent;

  @Column(name = "inputs", nullable = false)
  private String inputs;

  /**
   * Default constructor
   */
  public CaptureAgentDto() {
  }

  /**
   * Creates an capture agent
   *
   * @param id
   *          the id
   * @param agentId
   *          the agent id
   */
  public CaptureAgentDto(RoomDto room, String agentId, String inputs) {
    this.room = notNull(room, "room");
    this.mhAgent = notEmpty(agentId, "mhAgent");
    this.inputs = notEmpty(inputs, "inputs");
  }

  /**
   * Gets the room containing the capture agent
   *
   * @return the room
   */
  public RoomDto getRoom() {
    return room;
  }

  /**
   * Sets the room containing the capture agent
   *
   * @param room
   *          the room containing the agent
   */
  public void setRoom(RoomDto room) {
    this.room = room;
  }

  /**
   * Sets the MH agent identifier
   *
   * @param mhAgent
   *          the agent id
   */
  public void setMhAgent(String mhAgent) {
    this.mhAgent = mhAgent;
  }

  /**
   * Returns the MH agent identifier
   *
   * @return the agent id
   */
  public String getMhAgent() {
    return mhAgent;
  }

   /**
   * Gets the recording inputs of the capture agent
   *
   * @return inputs
   *          | separate string of inputs
   */
  public String getInputs() {
    return inputs;
  }

  /**
   * Sets the recording inputs of the capture agent
   *
   * @param inputs
   *          the room containing the agent
   */
  public void setInputs(String inputs) {
    this.inputs = inputs;
  }

  /**
   * Returns the business object of this capture agent
   *
   * @return the business object model of this capture agent
   */
  public CaptureAgent toCaptureAgent() {
    CaptureAgent ca = new CaptureAgent(room.toRoom(), mhAgent, inputs);
    ca.setId(id);
    return ca;
  }
}
