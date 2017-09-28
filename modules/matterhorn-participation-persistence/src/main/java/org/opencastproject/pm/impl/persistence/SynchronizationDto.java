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

import org.opencastproject.pm.api.Synchronization;
import org.opencastproject.security.api.UserDirectoryService;

import java.util.ArrayList;
import java.util.Date;
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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

/**
 * Entity object for synchronization data.
 */
@Entity(name = "Synchronization")
@Table(name = "mh_pm_synchronization", uniqueConstraints = { @UniqueConstraint(columnNames = { "date" }) })
@NamedQueries({
        @NamedQuery(name = "Synchronization.findAll", query = "SELECT s FROM Synchronization s ORDER BY s.date DESC"),
        @NamedQuery(name = "Synchronization.findByDate", query = "SELECT s FROM Synchronization s WHERE s.date = :date"),
        @NamedQuery(name = "Synchronization.clear", query = "DELETE FROM Synchronization") })
public class SynchronizationDto {

  @Id
  @GeneratedValue
  @Column(name = "id")
  private long id;

  @Column(name = "date", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date date;

  @OneToMany(fetch = FetchType.EAGER, cascade = { CascadeType.REMOVE })
  private List<ErrorDto> errors = new ArrayList<ErrorDto>();

  @OneToMany(fetch = FetchType.EAGER, cascade = { CascadeType.REMOVE })
  private List<SynchronizedRecordingDto> synchronizedRecordings = new ArrayList<SynchronizedRecordingDto>();

  /**
   * Default constructor
   */
  public SynchronizationDto() {
  }

  /**
   * Creates a synchronization point without errors and synchronized recordings logs
   *
   * @param date
   *          the synchronization date
   */
  public SynchronizationDto(Date date) {
    this.date = notNull(date, "date");
  }

  /**
   * Creates a synchronization point with errors
   *
   * @param date
   *          the synchronization date
   * @param errors
   *          the error list
   * @param synchronisedRecordings
   *          the synchronized recordings
   */
  public SynchronizationDto(Date date, List<ErrorDto> errors, List<SynchronizedRecordingDto> synchronisedRecordings) {
    this.date = notNull(date, "date");
    this.errors = notNull(errors, "errors");
    this.synchronizedRecordings = notNull(synchronisedRecordings, "synchronizedRecordings");
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
   * Sets a synchronization error list.
   *
   * @param errors
   *          the error list
   */
  public void setErrors(List<ErrorDto> errors) {
    this.errors = notNull(errors, "errors");
  }

  /**
   * Returns the error list
   *
   * @return the error list
   */
  public List<ErrorDto> getErrors() {
    return errors;
  }

  /**
   * Add an error to the synchronization
   *
   * @param error
   *          the error to add to this synchronization
   *
   * @return true if this collection changed as a result of the call
   */
  public boolean addError(ErrorDto error) {
    return errors.add(notNull(error, "error"));
  }

  /**
   * Remove an error from the synchronization
   *
   * @param error
   *          the error to remove from this synchronization
   *
   * @return true if this collection changed as a result of the call
   */
  public boolean removeError(ErrorDto error) {
    return errors.remove(notNull(error, "error"));
  }

  /**
   * Sets a synchronized recordings list.
   *
   * @param synchronizedRecordings
   *          the synchronized recordings list
   */
  public void setSynchronizedRecordings(List<SynchronizedRecordingDto> synchronizedRecordings) {
    this.synchronizedRecordings = notNull(synchronizedRecordings, "synchronizedRecordings");
  }

  /**
   * Returns the synchronized recordings list.
   *
   * @return the synchronized recordings list
   */
  public List<SynchronizedRecordingDto> getSynchronizedRecordings() {
    return synchronizedRecordings;
  }

  /**
   * Add an error to the synchronized recordings list.
   *
   * @param synchronizedRecording
   *          the synchronized recordings list.
   *
   * @return true if this collection changed as a result of the call
   */
  public boolean addSynchronizedRecording(SynchronizedRecordingDto synchronizedRecording) {
    return synchronizedRecordings.add(notNull(synchronizedRecording, "synchronizedRecording"));
  }

  /**
   * Remove an error from the synchronized recordings list.
   *
   * @param synchronizedRecording
   *          the synchronized recordings list.
   *
   * @return true if this collection changed as a result of the call
   */
  public boolean removeSynchronizedRecording(SynchronizedRecordingDto synchronizedRecording) {
    return synchronizedRecordings.remove(notNull(synchronizedRecording, "synchronizedRecording"));
  }

  /**
   * Sets the synchronization date
   *
   * @param date
   */
  public void setDate(Date date) {
    this.date = date;
  }

  /**
   * Returns the synchronization date
   *
   * @return the synchronization date
   */
  public Date getDate() {
    return date;
  }

  /**
   * Returns the business object of this synchronization
   *
   * @return the business object model of this synchronization
   */
  public Synchronization toSynchronization(UserDirectoryService userDirectoryService) {
    Synchronization sync = new Synchronization(date);
    for (ErrorDto e : getErrors()) {
      sync.addError(e.toError());
    }
    for (SynchronizedRecordingDto s : getSynchronizedRecordings()) {
      sync.addSynchronizedRecording(s.toSynchronizedRecording(userDirectoryService));
    }
    sync.setId(id);
    return sync;
  }

}
