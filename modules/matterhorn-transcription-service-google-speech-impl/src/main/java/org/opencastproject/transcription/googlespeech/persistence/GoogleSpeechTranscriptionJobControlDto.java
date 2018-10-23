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

package org.opencastproject.transcription.googlespeech.persistence;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlAttribute;

@Entity(name = "TranscriptionJobControl")
@Table(name = "mh_transcription_service_job")
@NamedQueries({
        @NamedQuery(name = "TranscriptionJobControl.findByMediaPackage", query = "SELECT jc FROM TranscriptionJobControl jc WHERE jc.mediaPackageId = :mediaPackageId ORDER BY jc.dateCreated DESC"),
        @NamedQuery(name = "TranscriptionJobControl.findByJob", query = "SELECT jc FROM TranscriptionJobControl jc WHERE jc.transcriptionJobId = :transcriptionJobId"),
        @NamedQuery(name = "TranscriptionJobControl.findByStatus", query = "SELECT jc FROM TranscriptionJobControl jc WHERE jc.status IN :status") })
public class GoogleSpeechTranscriptionJobControlDto {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", length = 128)
  @XmlAttribute
  private long id;

  @Column(name = "media_package_id", nullable = false, length = 128)
  private String mediaPackageId;

  @Column(name = "track_id", nullable = false, length = 128)
  private String trackId;

  // This is the google speech-to-text token id generated after a request
  @Column(name = "job_id", nullable = false, length = 128)
  private String transcriptionJobId;

  @Column(name = "date_created", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date dateCreated;

  @Column(name = "date_completed", nullable = true)
  @Temporal(TemporalType.TIMESTAMP)
  private Date dateCompleted;

  @Column(name = "status", nullable = true, length = 128)
  private String status;

  @Column(name = "track_duration", nullable = false)
  private long trackDuration;

  @Column(name = "provider_id", nullable = false)
  private int providerId;

  public GoogleSpeechTranscriptionJobControlDto() {
  }

  /** Constructor with all fields. */
  public GoogleSpeechTranscriptionJobControlDto(String mediaPackageId, String trackId, String transcriptionJobId, Date dateCreated,
          Date dateCompleted, String status, long trackDuration, int providerId) {
    super();
    this.mediaPackageId = mediaPackageId;
    this.trackId = trackId;
    this.transcriptionJobId = transcriptionJobId;
    this.dateCreated = dateCreated;
    this.dateCompleted = dateCompleted;
    this.status = status;
    this.trackDuration = trackDuration;
    this.providerId = providerId;
  }

  /** Convert into business object. */
  public GoogleSpeechTranscriptionJobControl toTranscriptionJobControl() {
    return new GoogleSpeechTranscriptionJobControl(mediaPackageId, trackId, transcriptionJobId, dateCreated, dateCompleted, status,
            trackDuration);
  }

  /** Store new job control */
  public static GoogleSpeechTranscriptionJobControlDto store(EntityManager em, String mediaPackageId, String trackId,
          String transcriptionJobId, String jobStatus, long trackDuration, int providerId) throws GoogleSpeechTranscriptionDatabaseException {
    GoogleSpeechTranscriptionJobControlDto dto = new GoogleSpeechTranscriptionJobControlDto(mediaPackageId, trackId, transcriptionJobId,
            new Date(), null, jobStatus, trackDuration, providerId);

    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      em.persist(dto);
      tx.commit();
      return dto;
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw new GoogleSpeechTranscriptionDatabaseException(e);
    } finally {
      em.close();
    }
  }

  /** Find a job control by its number. */
  public static GoogleSpeechTranscriptionJobControlDto findByJob(EntityManager em, String jobId)
          throws GoogleSpeechTranscriptionDatabaseException {
    Query query = null;
    try {
      query = em.createNamedQuery("TranscriptionJobControl.findByJob");
      query.setParameter("transcriptionJobId", jobId);
      return (GoogleSpeechTranscriptionJobControlDto) query.getSingleResult();
    } catch (NoResultException e) {
      return null; // Not found
    } catch (Exception e) {
      throw new GoogleSpeechTranscriptionDatabaseException(e);
    }
  }

  /** Find all job controls by media package. */
  @SuppressWarnings("unchecked")
  public static List<GoogleSpeechTranscriptionJobControlDto> findByMediaPackage(EntityManager em, final String mediaPackageId)
          throws GoogleSpeechTranscriptionDatabaseException {
    Query query = null;
    try {
      query = em.createNamedQuery("TranscriptionJobControl.findByMediaPackage");
      query.setParameter("mediaPackageId", mediaPackageId);
      return query.getResultList();
    } catch (Exception e) {
      throw new GoogleSpeechTranscriptionDatabaseException(e);
    }
  }

  /** Find all job controls by status. */
  @SuppressWarnings("unchecked")
  public static List<GoogleSpeechTranscriptionJobControlDto> findByStatus(EntityManager em, final String... status)
          throws GoogleSpeechTranscriptionDatabaseException {
    Collection<String> statusCol = new HashSet<String>();
    for (String st : status)
      statusCol.add(st);
    Query query = null;
    try {
      query = em.createNamedQuery("TranscriptionJobControl.findByStatus");
      query.setParameter("status", statusCol);
      return query.getResultList();
    } catch (Exception e) {
      throw new GoogleSpeechTranscriptionDatabaseException(e);
    }
  }

  /** Update job status */
  public static void updateStatus(EntityManager em, String jobId, String status) throws GoogleSpeechTranscriptionDatabaseException {
    GoogleSpeechTranscriptionJobControlDto dto = findByJob(em, jobId);
    if (dto == null)
      throw new GoogleSpeechTranscriptionDatabaseException("Job not found in database: " + jobId);

    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      dto.setStatus(status);
      if (GoogleSpeechTranscriptionJobControl.Status.TranscriptionComplete.name().equals(status))
        dto.setDateCompleted(new Date());
      em.merge(dto);
      tx.commit();
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw new GoogleSpeechTranscriptionDatabaseException(e);
    } finally {
      em.close();
    }
  }

  public static void delete(EntityManager em, String jobId) throws GoogleSpeechTranscriptionDatabaseException {
    GoogleSpeechTranscriptionJobControlDto dto = findByJob(em, jobId);
    if (dto == null)
      return;

    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      em.remove(dto);
      tx.commit();
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw new GoogleSpeechTranscriptionDatabaseException(e);
    } finally {
      em.close();
    }
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public void setDateCompleted(Date dateCompleted) {
    this.dateCompleted = dateCompleted;
  }

}
