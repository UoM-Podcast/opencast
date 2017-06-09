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

import org.opencastproject.pm.api.SynchronizedRecording;
import org.opencastproject.pm.api.SynchronizedRecording.SynchronizationStatus;
import org.opencastproject.security.api.UserDirectoryService;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * Entity object for synchronized recording log.
 */
@Entity(name = "SynchronizedRecording")
@Table(name = "mh_pm_synchronized_recording")
@NamedQueries({ @NamedQuery(name = "SynchronizedRecording.findAll", query = "SELECT s FROM SynchronizedRecording s"),
        @NamedQuery(name = "SynchronizedRecording.clear", query = "DELETE FROM SynchronizedRecording") })
public class SynchronizedRecordingDto {

  @Id
  @GeneratedValue
  @Column(name = "id")
  private long id;

  @Enumerated(EnumType.STRING)
  private SynchronizationStatus status;

  @JoinColumn(name = "recording", referencedColumnName = "id")
  private RecordingDto recording;

  /**
   * Default constructor
   */
  public SynchronizedRecordingDto() {
  }

  /**
   * Creates a synchronization log for a recording
   * 
   * @param status
   * @param recording
   */
  public SynchronizedRecordingDto(SynchronizationStatus status, RecordingDto recording) {
    this.status = status;
    this.recording = notNull(recording, "recording");
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
   * Sets the synchronization log status
   * 
   * @param status
   *          the synchronization log status
   */
  public void setStatus(SynchronizationStatus status) {
    this.status = status;
  }

  /**
   * Returns the synchronization log status
   * 
   * @return the synchronization log status
   */
  public SynchronizationStatus getStatus() {
    return this.status;
  }

  /**
   * Sets the recording related to this synchronization log
   * 
   * @param recording
   *          the recording
   */
  public void setRecording(RecordingDto recording) {
    this.recording = notNull(recording, "recording");
  }

  /**
   * Returns the recording related to this synchronization log
   * 
   * @return the recording
   */
  public RecordingDto getRecording() {
    return this.recording;
  }

  /**
   * Returns the business object of this synchronized recording log
   * 
   * @return the business object model of this synchronized recording log
   */
  public SynchronizedRecording toSynchronizedRecording(UserDirectoryService userDirectoryService) {
    return new SynchronizedRecording(id, status, recording.toRecording(userDirectoryService));
  }

}
