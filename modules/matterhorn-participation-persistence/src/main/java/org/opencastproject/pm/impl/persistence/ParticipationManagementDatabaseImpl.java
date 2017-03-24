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

import static org.opencastproject.pm.impl.persistence.ParticipationManagementPersistenceUtil.checkBlacklistRecording;
import static org.opencastproject.pm.impl.persistence.ParticipationManagementPersistenceUtil.find;
import static org.opencastproject.pm.impl.persistence.ParticipationManagementPersistenceUtil.findLastSynchronization;
import static org.opencastproject.pm.impl.persistence.ParticipationManagementPersistenceUtil.mergeBlacklist;
import static org.opencastproject.pm.impl.persistence.ParticipationManagementPersistenceUtil.mergeBuilding;
import static org.opencastproject.pm.impl.persistence.ParticipationManagementPersistenceUtil.mergeCourse;
import static org.opencastproject.pm.impl.persistence.ParticipationManagementPersistenceUtil.mergeMessage;
import static org.opencastproject.pm.impl.persistence.ParticipationManagementPersistenceUtil.mergePerson;
import static org.opencastproject.pm.impl.persistence.ParticipationManagementPersistenceUtil.mergeRecording;
import static org.opencastproject.pm.impl.persistence.ParticipationManagementPersistenceUtil.mergeRoom;
import static org.opencastproject.pm.impl.persistence.ParticipationManagementPersistenceUtil.mergeSchedulingSource;
import static org.opencastproject.pm.impl.persistence.ParticipationManagementPersistenceUtil.saveSynchronization;
import static org.opencastproject.pm.impl.persistence.ParticipationManagementPersistenceUtil.setDateForDailyQuery;
import static org.opencastproject.pm.impl.persistence.ParticipationManagementPersistenceUtil.setDateForQuarterQuery;
import static org.opencastproject.pm.impl.persistence.ParticipationManagementPersistenceUtil.setDateForWeeklyQuery;
import static org.opencastproject.util.data.Option.option;

import org.opencastproject.pm.api.Blacklist;
import org.opencastproject.pm.api.Blacklistable;
import org.opencastproject.pm.api.Building;
import org.opencastproject.pm.api.CaptureAgent;
import org.opencastproject.pm.api.Course;
import org.opencastproject.pm.api.Course.EmailStatus;
import org.opencastproject.pm.api.Message;
import org.opencastproject.pm.api.Period;
import org.opencastproject.pm.api.Person;
import org.opencastproject.pm.api.Recording;
import org.opencastproject.pm.api.Recording.ReviewStatus;
import org.opencastproject.pm.api.Room;
import org.opencastproject.pm.api.SchedulingSource;
import org.opencastproject.pm.api.Synchronization;
import org.opencastproject.pm.api.persistence.EmailView;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabaseException;
import org.opencastproject.pm.api.persistence.RecordingQuery;
import org.opencastproject.pm.api.persistence.RecordingView;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.persistence.Queries;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

/**
 * Implements {@link ParticipationManagementDatabase}. Defines permanent storage for participation management.
 */
public class ParticipationManagementDatabaseImpl implements ParticipationManagementDatabase {

  public static final String PERSISTENCE_UNIT = "org.opencastproject.pm.impl.persistence";

  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(ParticipationManagementDatabaseImpl.class);

  /** Factory used to create {@link EntityManager}s for transactions */
  protected EntityManagerFactory emf = null;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService;

  /** The security service */
  protected SecurityService securityService;

  /**
   * Creates {@link EntityManagerFactory} using persistence provider and properties passed via OSGi.
   *
   * @param cc
   */
  public void activate(ComponentContext cc) {
     logger.info("Activating persistence manager for participation management");
//    emf = persistenceProvider.createEntityManagerFactory(PERSISTENCE_UNIT, persistenceProperties);
  }
;
  /** OSGi DI */
  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  /**
   * Closes entity manager factory.
   *
   * @param cc
   */
  public void deactivate(ComponentContext cc) {
    emf.close();
  }

  /**
   * OSGi callback to set the user directory service.
   *
   * @param userDirectoryService
   *          the user directory service
   */
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /**
   * OSGi callback to set security service
   *
   * @param securityService
   *          the security service
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean clearDatabase() throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    EntityTransaction tx = null;

    logger.info("Resetting participation management database...");
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      Queries.named.update(em, "Action.clear");
      Queries.named.update(em, "Blacklist.clear");
      Queries.named.update(em, "Building.clear");
      Queries.named.update(em, "CaptureAgent.clear");
      Queries.named.update(em, "Course.clear");
      Queries.named.update(em, "Error.clear");
      Queries.named.update(em, "Message.clear");
      Queries.named.update(em, "Period.clear");
      Queries.named.update(em, "Person.clear");
      Queries.named.update(em, "PersonType.clear");
      Queries.named.update(em, "Recording.clear");
      Queries.named.update(em, "Room.clear");
      Queries.named.update(em, "Synchronization.clear");
      Queries.named.update(em, "SynchronizedRecording.clear");
      Queries.named.update(em, "SchedulingSource.clear");
      tx.commit();
      logger.info("Participation management database reset");
      return true;
    } catch (Exception e) {
      logger.error("Could not (CASCADE) reset the participation management: {}", e.getMessage());
      if (tx.isActive())
        tx.rollback();

      logger.info("Resetting participation management database one table at time ...");
      try {
        // Because Prod DB was created with out CASCADE on delete property on the tables
        // the code above fails. Delete all the tables we use in the correct order.
        em = emf.createEntityManager();
        tx = em.getTransaction();
        tx.begin();
        Queries.sql.query(em, "DELETE FROM mh_pm_recording_messages");
        Queries.sql.query(em, "DELETE FROM mh_pm_recording_staff");
        Queries.sql.query(em, "DELETE FROM mh_pm_synchronization_mh_pm_synchronized_recording");
        Queries.named.update(em, "SynchronizedRecording.clear");
        Queries.named.update(em, "Recording.clear");
        Queries.named.update(em, "CaptureAgent.clear");
        Queries.named.update(em, "Synchronization.clear");
        Queries.named.update(em, "Error.clear");
        Queries.sql.query(em, "DELETE FROM mh_pm_message_mh_pm_error");
        Queries.sql.query(em, "DELETE FROM mh_pm_building_mh_pm_room");
        Queries.named.update(em, "Room.clear");
        Queries.named.update(em, "Building.clear");
        Queries.named.update(em, "Course.clear");
        Queries.named.update(em, "Message.clear");
        Queries.named.update(em, "Person.clear");
        Queries.named.update(em, "SchedulingSource.clear");

        tx.commit();
        logger.info("Participation management database reset");
        return true;
      } catch (Exception ee) {
        if (tx.isActive())
          tx.rollback();
        throw new ParticipationManagementDatabaseException(ee);
      }
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public Synchronization getLastSynchronization() throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Option<SynchronizationDto> syncDto = findLastSynchronization(em);
      if (syncDto.isSome()) {
        return syncDto.get().toSynchronization(userDirectoryService);
      } else {
        return null;
      }
    } catch (Exception e) {
      logger.error("Could not get last synchronization: {}", ExceptionUtils.getStackTrace(e));
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public Synchronization storeLatestSynchronization(Synchronization sync)
          throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      SynchronizationDto dto = saveSynchronization(sync, em);
      tx.commit();
      return dto.toSynchronization(userDirectoryService);
    } catch (Exception e) {
      logger.error("Could not store synchronization: {}", e.getMessage());
      if (tx.isActive())
        tx.rollback();
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public long countTotalRecordings() throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Recording.count");
      Number countResult = (Number) q.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public long countWeekRecordings() throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Recording.countByDateRange");
      q = setDateForWeeklyQuery(q);
      Number countResult = (Number) q.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public long countDailyRecordings() throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Recording.countByDateRange");
      setDateForDailyQuery(q);
      Number countResult = (Number) q.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public long countWeeklyBlacklistedRecordings() throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Recording.countBlacklistedByDateRange");
      setDateForWeeklyQuery(q);
      Number countResult = (Number) q.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public long countDailyBlacklistedRecordings() throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Recording.countBlacklistedByDateRange");
      setDateForDailyQuery(q);
      Number countResult = (Number) q.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public long countMessagesSent() throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Message.count");
      Number countResult = (Number) q.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public long countQuarterMessagesSent() throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Message.countByDateRange");
      setDateForQuarterQuery(q);
      Number countResult = (Number) q.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public long countDailyMessagesSent() throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Message.countByDateRange");
      setDateForDailyQuery(q);
      Number countResult = (Number) q.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public long countMessageErrors() throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Message.countErrors");
      Number countResult = (Number) q.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public long countTotalResponses() throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Recording.countRespones");
      Number countResult = (Number) q.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public long countCoursesByEmailState(EmailStatus emailStatus) throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Course.countByEmailState");
      q.setParameter("emailState", emailStatus);
      Number countResult = (Number) q.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public long countConfirmedResponses() throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Recording.countConfirmed");
      Number countResult = (Number) q.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public long countQuarterConfirmedResponses() throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Recording.countConfirmedByDateRange");
      setDateForQuarterQuery(q);
      Number countResult = (Number) q.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public long countDailyConfirmedResponses() throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Recording.countConfirmedByDateRange");
      setDateForDailyQuery(q);
      Number countResult = (Number) q.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public long countUnconfirmedResponses() throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Recording.countUnconfirmed");
      Number countResult = (Number) q.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public long countOptedOutResponses() throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Recording.countOptedOut");
      Number countResult = (Number) q.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public long countRecordingsByEmailState(EmailStatus emailStatus) throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Recording.countByEmailState");
      q.setParameter("emailState", emailStatus);
      Number countResult = (Number) q.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public long countCourseRecordingsByEmailState(Course course, EmailStatus emailStatus) throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Recording.countByEmailStateForCourse");
      CourseDto dto = new CourseDto(course.getCourseId());
      dto.setId(course.getId());
      q.setParameter("course", dto);
      q.setParameter("emailState", emailStatus);
      Number countResult = (Number) q.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  public long countRecordingsByReviewState(ReviewStatus reviewStatus) throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Recording.countByReviewState");
      q.setParameter("reviewState", reviewStatus);
      Number countResult = (Number) q.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  public long countPeriods() throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Period.count");
      Number countResult = (Number) q.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public long countPersons() throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Person.count");
      Number countResult = (Number) q.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public Course findCourseBySeries(String series) throws ParticipationManagementDatabaseException, NotFoundException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Course.findBySeries");
      q.setParameter("series", series);
      CourseDto course = (CourseDto) q.getSingleResult();
      return course.toCourse();
    } catch (NoResultException e) {
      throw new NotFoundException();
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Course> getCourses() throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Course.findAll");
      List<Course> courses = new ArrayList<Course>();
      List<CourseDto> result = q.getResultList();
      for (CourseDto c : result) {
        courses.add(c.toCourse());
      }
      return courses;
    } catch (Exception e) {
      logger.error("Could not get the courses: {}", ExceptionUtils.getStackTrace(e));
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public Course updateCourse(Course course) throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      CourseDto updatedCourse = mergeCourse(course, em);
      tx.commit();
      return updatedCourse.toCourse();
    } catch (Exception e) {
      logger.error("Could not update message: {}", ExceptionUtils.getStackTrace(e));
      if (tx.isActive())
        tx.rollback();
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public List<Message> getMessagesByRecordingId(long recordingId, Option<SortType> sortType)
          throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      List<MessageDto> messageDtos = ParticipationManagementPersistenceUtil.getMessagesByRecordingId(em, recordingId,
              sortType);
      List<Message> messages = new ArrayList<Message>();
      for (MessageDto messageDto : messageDtos) {
        messages.add(messageDto.toMessage(userDirectoryService));
      }
      return messages;
    } catch (Exception e) {
      logger.error("Could not get messages by recording {}: {}", recordingId, ExceptionUtils.getStackTrace(e));
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

//  @Override
  @SuppressWarnings("unchecked")
  public List<Message> getMessagesBySeriesId(String seriesId) throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Message.findBySeries");
      q.setParameter("series", seriesId);
      List<Message> messages = new ArrayList<Message>();
      List<MessageDto> result = q.getResultList();
      for (MessageDto m : result) {
        messages.add(m.toMessage(userDirectoryService));
      }
      return messages;
    } catch (Exception e) {
      logger.error("Could not get messages by series {}: {}", seriesId, ExceptionUtils.getStackTrace(e));
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public Message updateMessage(Message message) throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      String orgId = securityService.getOrganization().getId();
      MessageDto updatedMessage = mergeMessage(message, orgId, em);
      tx.commit();
      return updatedMessage.toMessage(userDirectoryService);
    } catch (Exception e) {
      logger.error("Could not update message: {}", ExceptionUtils.getStackTrace(e));
      if (tx.isActive())
        tx.rollback();
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  private PersonDto getPersonDto(long personId) throws ParticipationManagementDatabaseException, NotFoundException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Option<PersonDto> personDto = ParticipationManagementPersistenceUtil.findPerson(Option.some(personId), null, em);
      if (personDto.isNone())
        throw new NotFoundException();
      return personDto.get();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not get person '{}': {}", personId, ExceptionUtils.getStackTrace(e));
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public Person getPerson(long personId) throws ParticipationManagementDatabaseException, NotFoundException {
    return getPersonDto(personId).toPerson();
  }

  @Override
  public Person getPerson(String email) throws ParticipationManagementDatabaseException, NotFoundException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Option<PersonDto> personDto = ParticipationManagementPersistenceUtil.findPerson(Option.<Long> none(), email, em);
      if (personDto.isNone())
        throw new NotFoundException();
      return personDto.get().toPerson();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not get person with email '{}': {}", email, ExceptionUtils.getStackTrace(e));
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  private RoomDto getRoomDto(long roomId) throws ParticipationManagementDatabaseException, NotFoundException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Option<RoomDto> roomDto = ParticipationManagementPersistenceUtil.findRoom(Option.some(roomId), null, em);
      if (roomDto.isNone())
        throw new NotFoundException();
      return roomDto.get();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not get room '{}': {}", roomId, ExceptionUtils.getStackTrace(e));
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public Room getRoom(long roomId) throws ParticipationManagementDatabaseException, NotFoundException {
    return getRoomDto(roomId).toRoom();
  }

  @Override
  public Room updateRoom(Room room) throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      RoomDto updatedRoom = mergeRoom(room, em);
      tx.commit();
      return updatedRoom.toRoom();
    } catch (Exception e) {
      logger.error("Could not room '{}': {}", room, ExceptionUtils.getStackTrace(e));
      if (tx.isActive())
        tx.rollback();
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public Recording getRecording(long recordingId) throws ParticipationManagementDatabaseException, NotFoundException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Option<RecordingDto> recordingDto = ParticipationManagementPersistenceUtil.findRecording(recordingId, em);
      if (recordingDto.isNone())
        throw new NotFoundException();
      return recordingDto.get().toRecording(userDirectoryService);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not get recording '{}': {}", recordingId, ExceptionUtils.getStackTrace(e));
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public Recording getRecordingByEvent(long eventId) throws ParticipationManagementDatabaseException, NotFoundException {
    EntityManager em = null;
    try {
      logger.info("Getting Recording by Event '" + eventId + "'");
      em = emf.createEntityManager();
      Option<RecordingDto> recordingDto = ParticipationManagementPersistenceUtil.findRecordingByEvent(eventId, em);
      if (recordingDto.isNone())
        throw new NotFoundException();
      return recordingDto.get().toRecording(userDirectoryService);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not get recording by event '{}': {}", eventId, ExceptionUtils.getStackTrace(e));
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public Recording reviewRecording(long id, ReviewStatus status) throws ParticipationManagementDatabaseException,
          NotFoundException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      Option<RecordingDto> recordingOption = ParticipationManagementPersistenceUtil.findRecording(id, em);
      if (recordingOption.isNone())
        throw new NotFoundException();

      RecordingDto dto = recordingOption.get();
      dto.setReviewDate(new Date());
      dto.setReviewStatus(status);
      em.merge(dto);
      tx.commit();
      return dto.toRecording(userDirectoryService);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not set review status on recording '{}': {}", id, ExceptionUtils.getStackTrace(e));
      if (tx.isActive())
        tx.rollback();
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public Recording trimRecording(long id, boolean trim) throws ParticipationManagementDatabaseException,
          NotFoundException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      Option<RecordingDto> recordingOption = ParticipationManagementPersistenceUtil.findRecording(id, em);
      if (recordingOption.isNone())
        throw new NotFoundException();

      RecordingDto dto = recordingOption.get();
      dto.setTrim(trim);
      tx.commit();
      return dto.toRecording(userDirectoryService);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not set trim status on recording '{}': {}", id, ExceptionUtils.getStackTrace(e));
      if (tx.isActive())
        tx.rollback();
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public Recording updateRecording(Recording recording) throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      String orgId = securityService.getOrganization().getId();
      RecordingDto rec = mergeRecording(recording, orgId, em);
      tx.commit();
      recording.setId(Option.<Long> some(rec.getId()));
      return rec.toRecording(userDirectoryService);
    } catch (Exception e) {
      logger.error("Could not update recording '{}': {}", recording, ExceptionUtils.getStackTrace(e));
      if (tx.isActive())
        tx.rollback();
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public void deleteRecording(long recordingId) throws ParticipationManagementDatabaseException, NotFoundException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Option<RecordingDto> recordingOption = ParticipationManagementPersistenceUtil.findRecording(recordingId, em);
      if (recordingOption.isNone())
        throw new NotFoundException();
      recordingOption.get().setDeleted(true);
      updateRecording(recordingOption.get().toRecording(userDirectoryService));
    } catch (NotFoundException e) {
      throw e;
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public List<RecordingView> findRecordingsAsView(RecordingQuery query) throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      List<RecordingView> recordingsView = new ArrayList<RecordingView>();
      for (RecordingDto dto : ParticipationManagementPersistenceUtil.findRecordingsDto(query, em)) {
        recordingsView.add(dto.toRecordingView());
      }
      return recordingsView;
    } catch (Exception e) {
      logger.error("Could not get recordings: {}", ExceptionUtils.getStackTrace(e));
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public List<EmailView> findRecordingsAsEmailView(EmailStatus emailStatus) throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      List<EmailView> emailViews = new ArrayList<EmailView>();
      TypedQuery<RecordingDto> q = em.createNamedQuery("Recording.findGroupedByActivity", RecordingDto.class);
      q.setParameter("emailStatus", emailStatus);
      List<RecordingDto> recordingDtos = q.getResultList();
      String activityId = "";
      int  count = 1;
      RecordingDto recDto = null;
      for (RecordingDto dto : recordingDtos) {
        if (! activityId.equals(dto.getActivityId())) {
          if (recDto != null) {
            emailViews.add(recDto.toEmailView(count));
          }
          recDto = dto;
          activityId = dto.getActivityId();
          count = 1;
        } else {
          count++;
        }
      }
      if (recDto != null) {
          emailViews.add(recDto.toEmailView(count));
      }
      return emailViews;
    } catch (Exception e) {
      logger.error("Could not get recordings: {}", ExceptionUtils.getStackTrace(e));
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public List<Recording> findRecordings(RecordingQuery query) throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      List<Recording> recordings = new ArrayList<Recording>();
      for (RecordingDto dto : ParticipationManagementPersistenceUtil.findRecordingsDto(query, em)) {
        recordings.add(dto.toRecording(userDirectoryService));
      }
      return recordings;
    } catch (Exception e) {
      logger.error("Could not get recordings: {}", ExceptionUtils.getStackTrace(e));
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public List<Course> findCoursesByEmailState(EmailStatus emailStatus)  throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      List<Course> courses = new ArrayList<Course>();
      Query q = em.createNamedQuery("Course.findByEmailState");
      q.setParameter("emailState", emailStatus);

      List<CourseDto> courseDtos = q.getResultList();
      for (CourseDto dto : courseDtos) {
        courses.add(dto.toCourse());
      }
      return courses;
    } catch (Exception e) {
      logger.error("Could not get courses: {}", ExceptionUtils.getStackTrace(e));
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public List<EmailView> findCoursesAsEmailView(EmailStatus emailStatus) throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      List<EmailView> emailViews = new ArrayList<EmailView>();
      Query q = em.createNamedQuery("Course.findByEmailState");
      q.setParameter("emailState", emailStatus);

      List<CourseDto> courseDtos = q.getResultList();
      for (CourseDto dto : courseDtos) {
         emailViews.add(dto.toEmailView());
      }
      return emailViews;
    } catch (Exception e) {
      logger.error("Could not get courses: {}", ExceptionUtils.getStackTrace(e));
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public Blacklist getBlacklist(long blacklistId) throws ParticipationManagementDatabaseException, NotFoundException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Option<Blacklist> blacklist = ParticipationManagementPersistenceUtil.findBlacklist(blacklistId, em);
      if (blacklist.isNone())
        throw new NotFoundException();
      return blacklist.get();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not get blacklist '{}': {}", blacklistId, ExceptionUtils.getStackTrace(e));
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public List<Blacklist> findBlacklists(String type, Option<Integer> limit, Option<Integer> offset,
          Option<String> name, Option<String> purpose, Option<String> sort)
          throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      return ParticipationManagementPersistenceUtil.findBlacklistsByType(em, type, limit, offset, name, purpose, sort);
    } catch (Exception e) {
      logger.error("Could not get blacklists by type {}: {}", type, ExceptionUtils.getStackTrace(e));
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public List<Blacklist> findBlacklists(Blacklistable... blacklistable) throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      return ParticipationManagementPersistenceUtil.findBlacklists(em, blacklistable);
    } catch (Exception e) {
      logger.error("Could not get blacklists: {}", ExceptionUtils.getStackTrace(e));
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public Blacklist updateBlacklist(Blacklist blacklist) throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    EntityTransaction tx = null;

    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();

      BlacklistDto dto = mergeBlacklist(blacklist, em);

      Blacklist updatedBlacklist = new Blacklist(dto.getId(), blacklist.getBlacklisted());
      for (PeriodDto p : dto.getPeriods()) {
        updatedBlacklist.addPeriod(p.toPeriod());
      }

      String orgId = securityService.getOrganization().getId();
      checkBlacklistRecording(dto, orgId, em, userDirectoryService);
      tx.commit();

      return updatedBlacklist;
    } catch (Exception e) {
      logger.error("Could not update blacklist '{}': {}", blacklist, ExceptionUtils.getStackTrace(e));
      if (tx.isActive())
        tx.rollback();
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public void deleteBlacklist(long blacklistId, Option<Long> periodId) throws ParticipationManagementDatabaseException,
          NotFoundException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      String orgId = securityService.getOrganization().getId();
      BlacklistDto dto = em.find(BlacklistDto.class, blacklistId);
      if (dto == null)
        throw new NotFoundException();
      if (periodId.isNone()) {
        ParticipationManagementPersistenceUtil.deletePeriods(dto.getPeriods(), em);
        dto.setPeriods(new ArrayList<PeriodDto>());

        checkBlacklistRecording(dto, orgId, em, userDirectoryService);
        em.remove(dto);
      } else {
        PeriodDto period = null;
        for (PeriodDto p : dto.getPeriods()) {
          if (p.getId() == periodId.get()) {
            period = p;
            break;
          }
        }
        if (period == null)
          throw new NotFoundException();

        dto.removePeriod(period);
        em.merge(dto);
        em.remove(period);
        checkBlacklistRecording(dto, orgId, em, userDirectoryService);
        if (dto.getPeriods().size() == 0)
          em.remove(dto);
      }
      tx.commit();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not delete blacklist entry '{}': {}", blacklistId, ExceptionUtils.getStackTrace(e));
      if (tx.isActive())
        tx.rollback();
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<String> getPurposesByType(String type) throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Period.findPurposeByType");
      q.setParameter("type", type);
      return q.getResultList();
    } catch (Exception e) {
      logger.error("Could not get purposes by type {}: {}", type, ExceptionUtils.getStackTrace(e));
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Person> getPersons() throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Person.findAll");
      List<Person> persons = new ArrayList<Person>();
      List<PersonDto> result = q.getResultList();
      for (PersonDto b : result) {
        persons.add(b.toPerson());
      }
      return persons;
    } catch (Exception e) {
      logger.error("Could not get persons: {}", ExceptionUtils.getStackTrace(e));
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Room> getRooms() throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Room.findAll");
      List<Room> rooms = new ArrayList<Room>();
      List<RoomDto> result = q.getResultList();
      for (RoomDto b : result) {
        rooms.add(b.toRoom());
      }
      return rooms;
    } catch (Exception e) {
      logger.error("Could not get rooms: {}", ExceptionUtils.getStackTrace(e));
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Building> getBuildings() throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Building.findAll");
      List<Building> buildings = new ArrayList<Building>();
      List<BuildingDto> result = q.getResultList();
      for (BuildingDto b : result) {
        buildings.add(b.toBuilding());
      }
      return buildings;
    } catch (Exception e) {
      logger.error("Could not get buildings: {}", ExceptionUtils.getStackTrace(e));
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public Building updateBuilding(Building building) throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      BuildingDto updatedBuilding = mergeBuilding(building, em);
      tx.commit();
      return updatedBuilding.toBuilding();
    } catch (Exception e) {
      logger.error("Could not update building '{}': {}", building, ExceptionUtils.getStackTrace(e));
      if (tx.isActive())
        tx.rollback();
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public void deleteBuilding(Long id) throws ParticipationManagementDatabaseException, NotFoundException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      Option<BuildingDto> dto = find(option(id), em, BuildingDto.class);
      if (dto.isNone())
        throw new NotFoundException();
      em.remove(dto.get());
      tx.commit();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not delete building entry '{}': {}", id, ExceptionUtils.getStackTrace(e));
      if (tx.isActive())
        tx.rollback();
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public Person updatePerson(Person person) throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      PersonDto updatedPerson = mergePerson(person, em);
      tx.commit();
      return updatedPerson.toPerson();
    } catch (Exception e) {
      logger.error("Could not update building '{}': {}", person, ExceptionUtils.getStackTrace(e));
      if (tx.isActive())
        tx.rollback();
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public void deletePerson(Long id) throws ParticipationManagementDatabaseException, NotFoundException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      Option<PersonDto> dto = find(option(id), em, PersonDto.class);
      if (dto.isNone())
        throw new NotFoundException();
      em.remove(dto.get());
      tx.commit();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not delete building entry '{}': {}", id, ExceptionUtils.getStackTrace(e));
      if (tx.isActive())
        tx.rollback();
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Person> getInstructors() throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Person.findAll");
      List<Person> persons = new ArrayList<Person>();
      List<PersonDto> result = q.getResultList();
      for (PersonDto p : result) {
        persons.add(p.toPerson());
      }
      return persons;
    } catch (Exception e) {
      logger.error("Could not get instructors: {}", ExceptionUtils.getStackTrace(e));
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<CaptureAgent> getCaptureAgents() throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("CaptureAgent.findAll");
      List<CaptureAgent> agents = new ArrayList<CaptureAgent>();
      List<CaptureAgentDto> result = q.getResultList();
      for (CaptureAgentDto c : result) {
        agents.add(c.toCaptureAgent());
      }
      return agents;
    } catch (Exception e) {
      logger.error("Could not get the capture agents: {}", ExceptionUtils.getStackTrace(e));
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Person> findPersons(String emailQuery, int offset, int limit)
          throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Person.findByEmailLike").setMaxResults(limit).setFirstResult(offset);
      q.setParameter("query", emailQuery.toUpperCase());
      List<Person> persons = new ArrayList<Person>();
      List<PersonDto> result = q.getResultList();
      for (PersonDto b : result) {
        persons.add(b.toPerson());
      }
      return persons;
    } catch (Exception e) {
      logger.error("Could not get persons by like query {}: {}", emailQuery, ExceptionUtils.getStackTrace(e));
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public long countBlacklistedEventsByRoom(int roomId, long startTime, long endTime)
          throws ParticipationManagementDatabaseException, NotFoundException {
    EntityManager em = null;
    Date startDate = new Date(startTime);
    Date endDate = new Date(endTime);
    logger.info("Looking for the number of events effect by room {} not being available between {} and {}", new Object[] {new Long(roomId), startDate, endDate});
    try {
      RoomDto room = getRoomDto(roomId);
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Recording.countBlacklistedEventsByDateRangeAndRoom");
      q.setParameter("start", startDate);
      q.setParameter("end", endDate);
      q.setParameter("room", room);
      Number countResult = (Number) q.getSingleResult();
      return countResult.longValue();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not count the number of blacklisted events by room with room id {} startTime {} endTime {}: {}", new Object[] {new Long(roomId), new Long(startTime), new Long(endTime), ExceptionUtils.getStackTrace(e)});
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public long countBlacklistedCoursesByRoom(int roomId, long startTime, long endTime)
          throws ParticipationManagementDatabaseException, NotFoundException {
    EntityManager em = null;
    Date startDate = new Date(startTime);
    Date endDate = new Date(endTime);
    logger.info("Looking for the number of courses effect by room {} not being available between {} and {}", new Object[] {new Long(roomId), startDate, endDate});
    try {
      RoomDto room = getRoomDto(roomId);
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Recording.countBlacklistedCoursesByDateRangeAndRoom");
      q.setParameter("start", startDate);
      q.setParameter("end", endDate);
      q.setParameter("room", room);
      Number countResult = (Number) q.getSingleResult();
      return countResult.longValue();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not count the number of blacklisted events by room with room id {} startTime {} endTime {}: {}", new Object[] {new Long(roomId), new Long(startTime), new Long(endTime), ExceptionUtils.getStackTrace(e)});
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public long countBlacklistedEventsByPerson(int personId, long startTime, long endTime)
          throws ParticipationManagementDatabaseException, NotFoundException {
    EntityManager em = null;
    Date startDate = new Date(startTime);
    Date endDate = new Date(endTime);
    logger.info("Looking for the number of events effect by person {} not being available between {} and {}", new Object[] {new Long(personId), startDate, endDate});
    try {
      PersonDto person = getPersonDto(personId);
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Recording.countBlacklistedEventsByDateRangeAndPerson");
      q.setParameter("start", startDate);
      q.setParameter("end", endDate);
      q.setParameter("person", person);
      Number countResult = (Number) q.getSingleResult();
      return countResult.longValue();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not count the number of blacklisted events by person id {} startTime {} endTime {}: {}", new Object[] {new Long(personId), new Long(startTime), new Long(endTime), ExceptionUtils.getStackTrace(e)});
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public long countBlacklistedCoursesByPerson(int personId, long startTime, long endTime)
          throws ParticipationManagementDatabaseException, NotFoundException {
    EntityManager em = null;
    Date startDate = new Date(startTime);
    Date endDate = new Date(endTime);
    logger.info("Looking for the number of courses effect by person {} not being available between {} and {}", new Object[] {new Long(personId), startDate, endDate});
    try {
      PersonDto person = getPersonDto(personId);
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Recording.countBlacklistedCoursesByDateRangeAndPerson");
      q.setParameter("start", startDate);
      q.setParameter("end", endDate);
      q.setParameter("person", person);
      Number countResult = (Number) q.getSingleResult();
      return countResult.longValue();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not count the number of blacklisted events by person id {} startTime {} endTime {}: {}", new Object[] {new Long(personId), new Long(startTime), new Long(endTime), ExceptionUtils.getStackTrace(e)});
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public SchedulingSource updateSchedulingSource(SchedulingSource source) throws ParticipationManagementDatabaseException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      SchedulingSourceDto updatedSchedulingSource = mergeSchedulingSource(source, em);
      tx.commit();
      return updatedSchedulingSource.toSchedulingSource();
    } catch (Exception e) {
      logger.error("Could not update scheduling source '{}': {}", source, e.getMessage());
      if (tx.isActive())
        tx.rollback();
      throw new ParticipationManagementDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public List<Message> getMessagesBySeriesId(String seriesId, Option<SortType> sortType) throws ParticipationManagementDatabaseException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public Blacklist getBlacklistByPeriodId(long periodId) throws ParticipationManagementDatabaseException, NotFoundException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public Period getPeriod(long periodId) throws ParticipationManagementDatabaseException, NotFoundException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public Period updatePeriod(Period period) throws ParticipationManagementDatabaseException, NotFoundException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void deletePeriod(long periodId) throws ParticipationManagementDatabaseException, NotFoundException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public long countRecordingsByDateRangeAndRoom(long roomId, long startTime, long endTime) throws ParticipationManagementDatabaseException, NotFoundException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public List<Recording> findRecordingsByDateRangeAndRoom(long roomId, long startTime, long endTime) throws ParticipationManagementDatabaseException, NotFoundException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public long countAffectedCoursesByDateRangeAndRoom(long roomId, long startTime, long endTime) throws ParticipationManagementDatabaseException, NotFoundException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public List<Course> findAffectedCoursesByDateRangeAndRoom(long roomId, long startTime, long endTime) throws ParticipationManagementDatabaseException, NotFoundException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public long countRecordingsByDateRangeAndPerson(long personId, long startTime, long endTime) throws ParticipationManagementDatabaseException, NotFoundException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public List<Recording> findRecordingsByDateRangeAndPerson(long personId, long startTime, long endTime) throws ParticipationManagementDatabaseException, NotFoundException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public long countAffectedCoursesByDateRangeAndPerson(long personId, long startTime, long endTime) throws ParticipationManagementDatabaseException, NotFoundException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public List<Course> findAffectedCoursesByDateRangeAndPerson(long personId, long startTime, long endTime) throws ParticipationManagementDatabaseException, NotFoundException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
}
