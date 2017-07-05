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

import static org.opencastproject.util.data.Arrays.array;
import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Option.some;

import org.opencastproject.messages.MailService;
import org.opencastproject.messages.persistence.MessageSignatureDto;
import org.opencastproject.messages.persistence.MessageTemplateDto;
import org.opencastproject.pm.api.Action;
import org.opencastproject.pm.api.Blacklist;
import org.opencastproject.pm.api.Blacklistable;
import org.opencastproject.pm.api.Building;
import org.opencastproject.pm.api.CaptureAgent;
import org.opencastproject.pm.api.Course;
import org.opencastproject.pm.api.Course.EmailStatus;
import org.opencastproject.pm.api.Error;
import org.opencastproject.pm.api.Message;
import org.opencastproject.pm.api.Period;
import org.opencastproject.pm.api.Person;
import org.opencastproject.pm.api.PersonType;
import org.opencastproject.pm.api.Recording;
import org.opencastproject.pm.api.Recording.ReviewStatus;
import org.opencastproject.pm.api.Room;
import org.opencastproject.pm.api.SchedulingSource;
import org.opencastproject.pm.api.Synchronization;
import org.opencastproject.pm.api.SynchronizedRecording;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase.SortType;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabaseException;
import org.opencastproject.pm.api.persistence.RecordingQuery;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function2;
import org.opencastproject.util.data.Option;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Partial;
import org.joda.time.ReadableInstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/** Utility class for user directory persistence methods */
public final class ParticipationManagementPersistenceUtil {

  private static final Logger logger = LoggerFactory.getLogger(ParticipationManagementPersistenceUtil.class);

  private ParticipationManagementPersistenceUtil() {
  }

  /**
   * Find the entity from the given type with the given id
   *
   * @param id
   *          the identifier of the entity to find
   * @param em
   *          The entity manager
   * @param entityClass
   *          The class of the type to find
   * @return an {@link org.opencastproject.util.data.Option option} object
   */
  public static <A> Option<A> find(Option<Long> id, EntityManager em, Class<A> entityClass) {
    for (Long a : id) {
      return option(em.find(entityClass, a));
    }
    return none();
  }

  public static ActionDto mergeAction(Action action, EntityManager em) {
    Option<ActionDto> actionOption;
    if (action.getId() != null) {
      actionOption = find(option(action.getId()), em, ActionDto.class);
    } else {
      actionOption = none();
    }
    ActionDto dto;
    if (actionOption.isSome()) {
      dto = actionOption.get();
      dto.setHandler(action.getHandler());
      dto.setName(action.getName());
      em.merge(dto);
    } else {
      dto = new ActionDto(action.getName(), action.getHandler());
      em.persist(dto);
    }
    return dto;
  }

  public static Option<RoomDto> findRoom(Option<Long> id, String name, EntityManager em) {
    Option<RoomDto> roomDto = find(id, em, RoomDto.class);
    if (roomDto.isSome())
      return roomDto;

    Query q = em.createNamedQuery("Room.findByName").setParameter("name", name);
    try {
      return some((RoomDto) q.getSingleResult());
    } catch (NoResultException e) {
      return none();
    }
  }

  public static RoomDto mergeRoom(Room room, EntityManager em) {
    Option<RoomDto> roomOption = findRoom(option(room.getId()), room.getName(), em);
    RoomDto dto;
    if (roomOption.isSome()) {
      dto = roomOption.get();
      em.merge(dto);
    } else {
      dto = new RoomDto(room.getName());
      em.persist(dto);
    }
    return dto;
  }

  public static Option<CaptureAgentDto> findCaptureAgent(Option<Long> id, String agentId, EntityManager em) {
    Option<CaptureAgentDto> captureAgentDto = find(id, em, CaptureAgentDto.class);
    if (captureAgentDto.isSome())
      return captureAgentDto;

    Query q = em.createNamedQuery("CaptureAgent.findByAgentId").setParameter("agent", agentId);
    try {
      return some((CaptureAgentDto) q.getSingleResult());
    } catch (NoResultException e) {
      return none();
    }
  }

  public static CaptureAgentDto mergeCaptureAgent(CaptureAgent captureAgent, EntityManager em) {
    Option<CaptureAgentDto> captureAgentOption = findCaptureAgent(option(captureAgent.getId()),
            captureAgent.getMhAgent(), em);
    CaptureAgentDto dto;
    if (captureAgentOption.isSome()) {
      dto = captureAgentOption.get();
      em.merge(dto);
    } else {
      RoomDto room = mergeRoom(captureAgent.getRoom(), em);
      dto = new CaptureAgentDto(room, captureAgent.getMhAgent());
      em.persist(dto);
    }
    return dto;
  }

  public static Option<CourseDto> findCourse(Option<Long> id, String courseId, String seriesId, EntityManager em) {
    Option<CourseDto> courseDto = find(id, em, CourseDto.class);
    if (courseDto.isSome())
      return courseDto;

    Query q = em.createNamedQuery("Course.findById").setParameter("course", courseId);

    try {
      return some((CourseDto) q.getSingleResult());
    } catch (NoResultException e) {
      return none();
    }
  }

  public static CourseDto mergeCourse(Course course, EntityManager em) {
    Option<CourseDto> courseOption = findCourse(option(course.getId()), course.getCourseId(), course.getSeriesId(), em);
    CourseDto dto;

    SchedulingSourceDto schedulingSource = null;
    if (course.getSchedulingSource().isSome()) {
      schedulingSource = mergeSchedulingSource(course.getSchedulingSource().get(), em);
    }

    if (courseOption.isSome()) {
      dto = courseOption.get();
      dto.setName(course.getName());
      dto.setSeriesId(course.getSeriesId());
      dto.setDescription(course.getDescription());
      dto.setExternalCourseKey(course.getExternalCourseKey());
      dto.setOptedOut(course.isOptedOut());
      dto.setFingerprint(course.getFingerprint().getOrElseNull());
      dto.setSchedulingSource(schedulingSource);
      dto.setRequirements(course.getRequirements());
      dto.setEmailStatus(course.getEmailStatus());
      em.merge(dto);
    } else {
      dto = new CourseDto(course.getCourseId(), course.getSeriesId(), course.getName(), course.getDescription(),
              course.getExternalCourseKey(), course.isOptedOut(), course.getFingerprint().getOrElseNull(), course.getRequirements());
      dto.setSchedulingSource(schedulingSource);
      em.persist(dto);
      course.setId(dto.getId());
    }
    return dto;
  }

  private static Function2<Option<PeriodDto>, PeriodDto, Boolean> contains = new Function2<Option<PeriodDto>, PeriodDto, Boolean>() {
    @Override
    public Boolean apply(Option<PeriodDto> toDelete, PeriodDto toMatch) {
      if (toDelete.isNone())
        return true;
      return toDelete.get().getId() == toMatch.getId();
    }
  };

  public static void deletePeriods(List<PeriodDto> oldPeriods, List<PeriodDto> newPeriods, EntityManager em) {
    for (PeriodDto p : oldPeriods) {
      Option<PeriodDto> periodOption = find(option(p.getId()), em, PeriodDto.class);
      boolean remain = mlist(newPeriods).exists(contains.curry(periodOption));
      if (!remain) {
        em.remove(periodOption.get());
      }
    }
  }

  public static void deletePeriods(List<PeriodDto> periods, EntityManager em) {
    for (PeriodDto p : periods) {
      Option<PeriodDto> periodOption = find(option(p.getId()), em, PeriodDto.class);
      if (periodOption.isSome()) {
        em.remove(periodOption.get());
      }
    }
  }

  public static PeriodDto mergePeriod(Period period, EntityManager em) {
    Option<PeriodDto> periodOption;
    if (period.getId() != null) {
      periodOption = find(period.getId(), em, PeriodDto.class);
    } else {
      periodOption = none();
    }
    PeriodDto dto;
    if (periodOption.isSome()) {
      dto = periodOption.get();
      dto.setStart(period.getStart());
      dto.setEnd(period.getEnd());
      dto.setPurpose(period.getPurpose().getOrElseNull());
      dto.setComment(period.getComment().getOrElseNull());
      em.merge(dto);
    } else {
      dto = new PeriodDto(period.getStart(), period.getEnd(), period.getPurpose().getOrElseNull(), period.getComment()
              .getOrElseNull());
      em.persist(dto);
    }
    return dto;
  }

  public static BlacklistDto mergeBlacklist(Blacklist blacklist, EntityManager em) {
    List<PeriodDto> periods = new ArrayList<PeriodDto>();
    for (Period p : blacklist.getPeriods()) {
      periods.add(mergePeriod(p, em));
    }

    BlacklistableDto blacklistableDto;
    Blacklistable blacklisted = blacklist.getBlacklisted();
    if (RoomDto.TYPE.equals(blacklisted.getType())) {
      Room r = (Room) blacklisted;
      blacklistableDto = mergeRoom(r, em);
    } else if (PersonDto.TYPE.equals(blacklisted.getType())) {
      Person p = (Person) blacklisted;
      blacklistableDto = mergePerson(p, em);
    } else {
      throw new IllegalStateException("Unknown blacklistable type '" + blacklisted.getType() + "' found!");
    }

    Option<BlacklistDto> blacklistOption;
    if (blacklist.getId() != null) {
      blacklistOption = find(option(blacklist.getId()), em, BlacklistDto.class);
    } else {
      List<Blacklist> blacklists = findBlacklists(em, blacklisted);
      if (blacklists.size() > 1)
        throw new RuntimeException("Contraint violation excpetion: multiple blacklists of " + blacklisted.getName()
                + " found");

      if (blacklists.isEmpty()) {
        blacklistOption = none();
      } else {
        blacklistOption = find(some(blacklists.get(0).getId()), em, BlacklistDto.class);
      }
    }

    BlacklistDto dto;
    if (blacklistOption.isSome()) {
      dto = blacklistOption.get();
      deletePeriods(dto.getPeriods(), periods, em);
      dto.setPeriods(periods);
      dto.setBlacklisted(blacklistableDto);
      em.merge(dto);
    } else {
      dto = new BlacklistDto(blacklistableDto, periods);
      em.persist(dto);
    }
    return dto;
  }

  public static Option<RecordingDto> findRecording(long recordingId, EntityManager em) {
    Query q = em.createNamedQuery("Recording.findById").setParameter("id", recordingId);
    try {
      return some((RecordingDto) q.getSingleResult());
    } catch (NoResultException e) {
      return none();
    }
  }

  public static Option<RecordingDto> findRecordingByEvent(long eventId, EntityManager em) {
    Query q = em.createNamedQuery("Recording.findByEventId").setParameter("id", eventId);
    try {
      return some((RecordingDto) q.getSingleResult());
    } catch (NoResultException e) {
      return none();
    }
  }

  /**
   * Gets a list of recording matching with the given query
   *
   * @param query
   *          the query
   * @param em
   *          Entity manager
   * @return a list of recording entity
   */
  public static List<RecordingDto> findRecordingsDto(RecordingQuery query, EntityManager em) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<RecordingDto> q = cb.createQuery(RecordingDto.class);
    Root<RecordingDto> recording = q.from(RecordingDto.class);
    q.select(recording);

    List<Predicate> predicates = new ArrayList<Predicate>();

    for (String activityId : query.getActivityId())
      predicates.add(cb.equal(recording.get("activityId"), activityId));
    for (Room room : query.getRoom())
      predicates.add(cb.equal(recording.get("room").get("name"), room.getName()));
    for (Date start : query.getStartDate())
      predicates.add(cb.equal(recording.get("start").as(Date.class), start));
    for (Date end : query.getEndDate())
      predicates.add(cb.equal(recording.get("stop").as(Date.class), end));

    for (Date start : query.getStartDateRange())
      predicates.add(cb.greaterThanOrEqualTo(recording.get("start").as(Date.class), start));
    for (Date end : query.getEndDateRange())
      predicates.add(cb.lessThanOrEqualTo(recording.get("stop").as(Date.class), end));
    for (Course c : query.getCourse()) {
      predicates.add(cb.equal(recording.get("course").get("courseId"), c.getCourseId()));
      predicates.add(cb.equal(recording.get("course").get("seriesId"), c.getSeriesId()));
    }
    for (ReviewStatus reviewStatus : query.getReviewStatus())
      predicates.add(cb.equal(recording.get("reviewStatus").as(ReviewStatus.class), reviewStatus));

    for (EmailStatus emailStatus : query.getEmailStatus())
      predicates.add(cb.equal(recording.get("emailStatus").as(EmailStatus.class), emailStatus));

    for (Person p : query.getStaff())
      predicates.add(cb.isMember(p.getEmail(), recording.get("staff").<Collection<String>> get("email")));

    // TODO
    // for (Person p : query.getAssistedStudents())
    // predicates.add(cb.isMember(p.getEmail(), recording.get("????").<Collection<String>> get("email")));

    for (Person p : query.getParticipation())
      predicates.add(cb.isMember(p.getEmail(), recording.get("participation").<Collection<String>> get("email")));

    // TODO full-text presenters and performance test since joins are needed
    for (String text : query.getFullText()) {
      List<Predicate> fullTextPredicates = new ArrayList<Predicate>();
      fullTextPredicates.add(cb.like(recording.<String> get("title"), "%" + text + "%"));
      fullTextPredicates.add(cb.like(recording.get("room").<String> get("name"), "%" + text + "%"));
      fullTextPredicates.add(cb.like(recording.get("captureAgent").<String> get("mhAgent"), "%" + text + "%"));
      predicates.add(cb.or(fullTextPredicates.toArray(new Predicate[fullTextPredicates.size()])));
    }

    if (query.isEvent().isSome())
      predicates.add(cb.isNotNull(recording.get("eventId")));

    if (query.isBlacklisted().isSome())
      predicates.add(cb.equal(recording.get("blacklisted"), query.isBlacklisted().get().booleanValue()));

    if (query.isDeleted().isSome())
      predicates.add(cb.equal(recording.get("deleted"), query.isDeleted().get().booleanValue()));

    q.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));

    TypedQuery<RecordingDto> typedQuery = em.createQuery(q);
    List<RecordingDto> recordingDtos = typedQuery.getResultList();
    return recordingDtos;
  }

  public static RecordingDto mergeRecording(Recording recording, String organization, EntityManager em) {
    List<PersonDto> staff = new ArrayList<PersonDto>();
    for (Person p : recording.getStaff()) {
      staff.add(mergePerson(p, em));
    }

    List<MessageDto> messages = new ArrayList<MessageDto>();
    for (Message m : recording.getMessages()) {
      messages.add(mergeMessage(m, organization, em));
    }

    List<ActionDto> actions = new ArrayList<ActionDto>();
    for (Action a : recording.getAction()) {
      actions.add(mergeAction(a, em));
    }

    List<PersonDto> participation = new ArrayList<PersonDto>();
    for (Person p : recording.getParticipation()) {
      participation.add(mergePerson(p, em));
    }

    CourseDto course = null;
    if (recording.getCourse().isSome())
      course = mergeCourse(recording.getCourse().get(), em);

    SchedulingSourceDto schedulingSource = null;
    if (recording.getSchedulingSource().isSome()) {
      schedulingSource = mergeSchedulingSource(recording.getSchedulingSource().get(), em);
    }

    RoomDto room = mergeRoom(recording.getRoom(), em);
    CaptureAgentDto captureAgent = mergeCaptureAgent(recording.getCaptureAgent(), em);

    Option<RecordingDto> recordingOption;
    if (recording.getId().isSome()) {
      recordingOption = findRecording(recording.getId().get(), em);
    } else {
      recordingOption = none();
    }
    RecordingDto dto;
    if (recordingOption.isSome()) {
      dto = recordingOption.get();
      dto.setActivityId(recording.getActivityId());
      dto.setTitle(recording.getTitle());
      dto.setBlacklisted(recording.isBlacklisted());
      dto.setStaff(staff);
      dto.setCourse(course);
      dto.setRoom(room);
      dto.setModificationDate(recording.getModificationDate());
      dto.setStart(recording.getStart());
      dto.setStop(recording.getStop());
      dto.setParticipation(participation);
      dto.setMessages(messages);
      dto.setEventId(recording.getEventId().getOrElseNull());
      dto.setCaptureAgent(captureAgent);
      dto.setAction(actions);
      dto.setEmailStatus(recording.getEmailStatus());
      dto.setReviewStatus(recording.getReviewStatus());
      dto.setReviewDate(recording.getReviewDate().getOrElseNull());
      dto.setDeleted(recording.isDeleted());
      dto.setFingerprint(recording.getFingerprint().getOrElseNull());
      dto.setEdit(recording.isEdit());
      dto.setRecordingInput(recording.getRecordingInput());
      dto.setSchedulingSource(schedulingSource);
      em.merge(dto);
    } else {
      dto = new RecordingDto(recording.getActivityId(), recording.getTitle(), recording.isBlacklisted(), staff, course,
              room, recording.getModificationDate(), recording.getStart(), recording.getStop(), participation,
              messages, recording.getEventId().getOrElseNull(), captureAgent, actions, recording.getEmailStatus(),
              recording.getReviewStatus(), recording.getReviewDate().getOrElseNull(), recording.isDeleted(),
              recording.getFingerprint().getOrElseNull(), recording.isEdit(), recording.getRecordingInput());
      dto.setSchedulingSource(schedulingSource);
      em.persist(dto);
    }
    return dto;
  }

  public static Option<SynchronizationDto> findLastSynchronization(EntityManager em) {
    Query q = em.createNamedQuery("Synchronization.findAll").setMaxResults(1);
    try {
      return some((SynchronizationDto) q.getSingleResult());
    } catch (NoResultException e) {
      return none();
    }
  }

  public static Option<SynchronizationDto> findSynchronization(Date date, EntityManager em) {
    Query q = em.createNamedQuery("Synchronization.findByDate").setParameter("date", date);
    try {
      return some((SynchronizationDto) q.getSingleResult());
    } catch (NoResultException e) {
      return none();
    }
  }

  public static SynchronizationDto saveSynchronization(Synchronization sync, EntityManager em) {
    // Transform errors and synchronized recording list to dto object list
    SynchronizationDto dto = new SynchronizationDto(sync.getDate());
    List<ErrorDto> errors = new ArrayList<ErrorDto>();
    for (Error e : sync.getErrors()) {
      errors.add(mergeError(e, em));
    }
    for (SynchronizedRecording sr : sync.getSynchronizedRecordings()) {
      dto.addSynchronizedRecording(mergeSynchronizedRecording(sr, em));
    }
    dto.setErrors(errors);
    em.persist(dto);
    return dto;
  }

  public static ErrorDto mergeError(Error error, EntityManager em) {
    Option<ErrorDto> errorOption = find(option(error.getId()), em, ErrorDto.class);
    ErrorDto dto;
    if (errorOption.isSome()) {
      dto = errorOption.get();
      dto.setType(error.getType());
      dto.setDescription(error.getDescription());
      dto.setSource(error.getSource());
      em.merge(dto);
    } else {
      dto = new ErrorDto(error.getType(), error.getDescription(), error.getSource());
      em.persist(dto);
    }
    return dto;
  }

  public static SynchronizedRecordingDto mergeSynchronizedRecording(SynchronizedRecording syncRecording,
          EntityManager em) {
    Option<SynchronizedRecordingDto> syncRecordingOption = find(option(syncRecording.getId()), em,
            SynchronizedRecordingDto.class);
    RecordingDto recording = em.find(RecordingDto.class, syncRecording.getRecording().getId().get());
    SynchronizedRecordingDto dto;
    if (syncRecordingOption.isSome()) {
      dto = syncRecordingOption.get();
      dto.setStatus(syncRecording.getStatus());
      dto.setRecording(recording);
      em.merge(dto);
    } else {
      dto = new SynchronizedRecordingDto(syncRecording.getStatus(), recording);
      em.persist(dto);
    }
    return dto;
  }

  /**
   * Returns all of the messages by a given recording id.
   *
   * @param em
   *          The entity manager to use for the query.
   * @param recordingId
   *          The id of the recording.
   * @param sortType
   *          The sort, if any, to apply to the results.
   * @return A list of messages that are related to the recording id.
   */
  public static List<MessageDto> getMessagesByRecordingId(EntityManager em, long recordingId, Option<SortType> sortType) {
    TypedQuery<RecordingToMessagesDto> getMessageIds = em.createNamedQuery("RecordingToMessage.findByRecordingId",
            RecordingToMessagesDto.class);
    getMessageIds.setParameter("recordingId", recordingId);
    // The reason we are getting RecordingToMessageDto instead of RecordingDto is so that we can sort the result of
    // messages using the database engine.
    List<RecordingToMessagesDto> recordingToMessageIds = getMessageIds.getResultList();
    Collection<String> messageIds = new LinkedList<String>();
    for (RecordingToMessagesDto recordingToMessageDto : recordingToMessageIds) {
      messageIds.add(Long.toString(recordingToMessageDto.getMessageId()));
    }
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<MessageDto> cq = cb.createQuery(MessageDto.class);
    Root<MessageDto> messageRoot = cq.from(MessageDto.class);
    Join<MessageDto, MessageSignatureDto> messageSignatureJoin = messageRoot.join("signature", JoinType.INNER);
    cq.select(messageRoot);

    Expression<String> exp = messageRoot.get("id");
    Predicate idPredicate = exp.in(messageIds);
    cq.where(idPredicate);
    if (!sortType.isNone()) {
      if (sortType.get().equals(SortType.DATE)) {
        logger.trace("Getting messages sorted by date. ");
        cq.orderBy(cb.asc(messageRoot.get("creationDate")));
      } else if (sortType.get().equals(SortType.DATE_DESC)) {
        logger.trace("Getting messages sorted by date descending");
        cq.orderBy(cb.desc(messageRoot.get("creationDate")));
      } else if (sortType.get().equals(SortType.SENDER)) {
        logger.trace("Getting messages sorted by sender.");
        cq.orderBy(cb.asc(messageSignatureJoin.get("sender")));
      } else if (sortType.get().equals(SortType.SENDER_DESC)) {
        logger.trace("Getting messages sorted by sender descending.");
        cq.orderBy(cb.desc(messageSignatureJoin.get("sender")));
      } else {
        logger.trace("Recording sort type was none.");
      }
    }
    TypedQuery<MessageDto> typedQuery = em.createQuery(cq);
    List<MessageDto> messages = typedQuery.getResultList();
    return messages;
  }

  /**
   * Get all of the recording ids from a series.
   *
   * @param em
   *          The entity manager
   * @param seriesId
   *          The series id to look for messages.
   * @return A Collection of message ids as Strings.
   */
  private static Collection<String> getMessageIdsFromSeries(EntityManager em, String seriesId) {
    // Get Recordings from series.
    TypedQuery<RecordingDto> recordingQuery = em.createNamedQuery("Recording.findBySeries", RecordingDto.class);
    recordingQuery.setParameter("series", seriesId);
    List<RecordingDto> recordings = recordingQuery.getResultList();

    Collection<String> messageIds = new LinkedList<String>();
    for (RecordingDto recording : recordings) {
      for (MessageDto message : recording.getMessages()) {
        messageIds.add(Long.toString(message.getId()));
      }
    }
    return messageIds;
  }

  /**
   * Returns all of the messages by a given series id.
   *
   * @param em
   *          The entity manager to use for the query.
   * @param seriesId
   *          The id of the recording.
   * @param sortType
   *          The sort, if any, to apply to the results.
   * @return A list of messages that are related to the recording id.
   */
  public static List<MessageDto> getMessagesBySeriesId(EntityManager em, String seriesId, Option<SortType> sortType) {
    Collection<String> messageIds = getMessageIdsFromSeries(em, seriesId);

    if (messageIds.size() > 0) {
      CriteriaBuilder cb = em.getCriteriaBuilder();
      CriteriaQuery<MessageDto> messageQuery = cb.createQuery(MessageDto.class);
      Root<MessageDto> messageRoot = messageQuery.from(MessageDto.class);
      Join<MessageDto, MessageSignatureDto> messageSignatureJoin = messageRoot.join("signature", JoinType.INNER);
      messageQuery.select(messageRoot);
      Expression<String> exp = messageRoot.get("id");
      Predicate idPredicate = exp.in(messageIds);
      messageQuery.where(idPredicate);
      if (!sortType.isNone()) {
        if (sortType.get().equals(SortType.DATE)) {
          logger.trace("Getting messages sorted by date. ");
          messageQuery.orderBy(cb.asc(messageRoot.get("creationDate")));
        } else if (sortType.get().equals(SortType.DATE_DESC)) {
          logger.trace("Getting messages sorted by date descending");
          messageQuery.orderBy(cb.desc(messageRoot.get("creationDate")));
        } else if (sortType.get().equals(SortType.SENDER)) {
          logger.trace("Getting messages sorted by sender.");
          messageQuery.orderBy(cb.asc(messageSignatureJoin.get("sender")));
        } else if (sortType.get().equals(SortType.SENDER_DESC)) {
          logger.trace("Getting messages sorted by sender descending.");
          messageQuery.orderBy(cb.desc(messageSignatureJoin.get("sender")));
        }
      } else {
        logger.trace("Recording sort type was none.");
      }
      TypedQuery<MessageDto> typedQuery = em.createQuery(messageQuery);
      return typedQuery.getResultList();
    }

    return Collections.<MessageDto> emptyList();
  }

  public static MessageDto mergeMessage(Message message, String organization, EntityManager em) {
    PersonDto creator = mergePerson(message.getCreator(), em);
    MessageTemplateDto template = MailService.mergeMessageTemplate(message.getTemplate(), organization, em);
    MessageSignatureDto signature = MailService.mergeMessageSignature(message.getSignature(), organization, em);
    List<ErrorDto> errors = new ArrayList<ErrorDto>();
    for (Error e : message.getErrors()) {
      errors.add(mergeError(e, em));
    }

    Option<MessageDto> messageOption = find(option(message.getId()), em, MessageDto.class);
    MessageDto dto;
    if (messageOption.isSome()) {
      dto = messageOption.get();
      dto.setCreationDate(message.getCreationDate());
      dto.setCreator(creator);
      dto.setTemplate(template);
      dto.setSignature(signature);
      dto.setErrors(errors);
      em.merge(dto);
    } else {
      dto = new MessageDto(message.getCreationDate(), creator, template, signature, errors);
      em.persist(dto);
    }
    return dto;
  }

  public static Option<PersonDto> findPerson(Option<Long> id, String email, EntityManager em) {
    Option<PersonDto> personDto = find(id, em, PersonDto.class);
    if (personDto.isSome())
      return personDto;

    Query q = em.createNamedQuery("Person.findByEmail").setParameter("email", email);
    try {
      return some((PersonDto) q.getSingleResult());
    } catch (NoResultException e) {
      return none();
    }
  }

  public static PersonDto mergePerson(Person person, EntityManager em) {
    List<PersonTypeDto> personTypes = new ArrayList<PersonTypeDto>();
    for (PersonType p : person.getPersonTypes()) {
      personTypes.add(mergePersonType(p, em));
    }

    Option<PersonDto> personOption = findPerson(option(person.getId()), person.getEmail(), em);
    PersonDto dto;
    if (personOption.isSome()) {
      dto = personOption.get();
      dto.setName(person.getName());
      dto.setPersonTypes(personTypes);
      em.merge(dto);
    } else {
      dto = new PersonDto(person.getName(), person.getEmail());
      dto.setPersonTypes(personTypes);
      em.persist(dto);
    }
    return dto;
  }

  public static Option<PersonTypeDto> findPersonType(Option<Long> id, String name, EntityManager em) {
    Option<PersonTypeDto> personTypeDto = find(id, em, PersonTypeDto.class);
    if (personTypeDto.isSome())
      return personTypeDto;

    Query q = em.createNamedQuery("PersonType.findByName").setParameter("name", name);
    try {
      return some((PersonTypeDto) q.getSingleResult());
    } catch (NoResultException e) {
      return none();
    }
  }

  public static PersonTypeDto mergePersonType(PersonType personType, EntityManager em) {
    Option<PersonTypeDto> personTypeOption = findPersonType(option(personType.getId()), personType.getName(), em);
    PersonTypeDto dto;
    if (personTypeOption.isSome()) {
      dto = personTypeOption.get();
      dto.setName(personType.getName());
      dto.setFunction(personType.getFunction());
      em.merge(dto);
    } else {
      dto = new PersonTypeDto(personType.getName(), personType.getFunction());
      em.persist(dto);
    }
    return dto;
  }

  public static Option<BuildingDto> findBuilding(Long id, String name, EntityManager em) {
    Option<BuildingDto> buildingDto = find(option(id), em, BuildingDto.class);
    if (buildingDto.isSome())
      return buildingDto;

    Query q = em.createNamedQuery("Building.findByName").setParameter("name", name);
    try {
      return some((BuildingDto) q.getSingleResult());
    } catch (NoResultException e) {
      return none();
    }
  }

  public static BuildingDto mergeBuilding(Building building, EntityManager em) {
    List<RoomDto> rooms = new ArrayList<RoomDto>();
    for (Room r : building.getRooms()) {
      rooms.add(mergeRoom(r, em));
    }

    Option<BuildingDto> buildingOption = findBuilding(building.getId(), building.getName(), em);
    BuildingDto dto;
    if (buildingOption.isSome()) {
      dto = buildingOption.get();
      dto.setRooms(rooms);
      em.merge(dto);
    } else {
      dto = new BuildingDto(building.getName(), rooms);
      em.persist(dto);
    }
    return dto;
  }

  /**
   * Add the correct start and end value for the given weekly count query.
   * <p/>
   * Please note that the start instant is inclusive while the end instant is exclusive.
   *
   * @param query
   *          The query where the parameters have to be added
   * @return the same query instance
   */
  public static Query setDateForWeeklyQuery(Query query) {
    final DateTime lastMonday = new DateTime().withTimeAtStartOfDay().withDayOfWeek(DateTimeConstants.MONDAY);
    return query.setParameter("start", lastMonday.toDate()).setParameter("end", lastMonday.plusWeeks(1).toDate());
  }

  /**
   * Add the correct start and end value for the given daily count query.
   * <p/>
   * Please note that the start instant is inclusive while the end instant is exclusive.
   *
   * @param query
   *          The query where the parameters have to be added
   * @return the same query instance
   */
  public static Query setDateForDailyQuery(Query query) {
    final DateTime today = new DateTime().withTimeAtStartOfDay();
    return query.setParameter("start", today.toDate()).setParameter("end", today.plusDays(1).toDate());
  }

  private static final DateTimeFieldType[] partialFields = array(DateTimeFieldType.monthOfYear(),
          DateTimeFieldType.dayOfMonth());

  /** Create a Partial consisting of only month and day. */
  public static Partial partial(int month, int dayOfMonth) {
    return new Partial(partialFields, new int[] { month, dayOfMonth });
  }

  /** Create a Partial from an Instant extracting month and day. */
  public static Partial partialize(ReadableInstant instant) {
    return partial(instant.get(DateTimeFieldType.monthOfYear()), instant.get(DateTimeFieldType.dayOfMonth()));
  }

  /** The beginnings of a quarter independent from the year. */
  public static final List<Partial> quarterBeginnings = list(partial(DateTimeConstants.JANUARY, 1),
          partial(DateTimeConstants.APRIL, 1), partial(DateTimeConstants.JULY, 1),
          partial(DateTimeConstants.OCTOBER, 1));

  /**
   * Add the correct start and end value for the given quarter count query
   * <p/>
   * Please note that the start instant is inclusive while the end instant is exclusive.
   *
   * @param query
   *          The query where the parameters have to be added
   * @return the same query instance
   */
  public static Query setDateForQuarterQuery(Query query) {
    final DateTime today = new DateTime().withTimeAtStartOfDay();
    final Partial partialToday = partialize(today);
    final DateTime quarterBeginning = mlist(quarterBeginnings).foldl(quarterBeginnings.get(0),
            new Function2<Partial, Partial, Partial>() {
              @Override
              public Partial apply(Partial sum, Partial quarterBeginning) {
                return partialToday.isAfter(quarterBeginning) ? quarterBeginning : sum;
              }
            }).toDateTime(today);
    return query.setParameter("start", quarterBeginning.toDate()).setParameter("end",
            quarterBeginning.plusMonths(3).toDate());
  }

  public static boolean checkBlacklistRecording(BlacklistDto blacklist, String organization, EntityManager em,
          UserDirectoryService userDirectoryService) throws ParticipationManagementDatabaseException {
    List<RecordingDto> recordings = new ArrayList<RecordingDto>();
    boolean blacklisted = false;
    boolean updated = false;

    if (Room.TYPE.equals(blacklist.getType())) {
      Option<RoomDto> roomDto = findRoom(option(blacklist.getBlacklistedId()), null, em);
      if (roomDto.isSome())
        recordings = findRecordingsDto(RecordingQuery.createWithoutDeleted().withRoom(roomDto.get().toRoom()), em);
    } else if (Person.TYPE.equals(blacklist.getType())) {
      Option<PersonDto> personDto = findPerson(option(blacklist.getBlacklistedId()), null, em);
      if (personDto.isSome())
        recordings = findRecordingsDto(
                RecordingQuery.createWithoutDeleted().withStaff(new Person[] { personDto.get().toPerson() }), em);
    }

    // If no recording related to the given blacklist, no change are done
    if (recordings.size() < 1)
      return false;

    for (RecordingDto recording : recordings) {
      for (PeriodDto p : blacklist.getPeriods()) {
        if ((p.getStart().compareTo(recording.getStart()) >= 0 && p.getStart().compareTo(recording.getStop()) <= 0)
                || (p.getEnd().compareTo(recording.getStart()) >= 0 && p.getEnd().compareTo(recording.getStop()) <= 0))
          blacklisted = true;
      }

      // Update the recording if the blacklisting element has been updated
      if (blacklisted != recording.isBlacklisted()) {
        if (!blacklisted && hasRecordingBlacklistedPeriods(em, recording))
          break;

        recording.setBlacklisted(blacklisted);
        mergeRecording(recording.toRecording(userDirectoryService), organization, em);
        updated = true;
      }
    }

    return updated;
  }

  private static Function<PeriodDto, Period> toPeriod = new Function<PeriodDto, Period>() {

    @Override
    public Period apply(PeriodDto a) {
      return a.toPeriod();
    }
  };

  private static Function2<BlacklistDto, RoomDto, Blacklist> toRoom = new Function2<BlacklistDto, RoomDto, Blacklist>() {
    @Override
    public Blacklist apply(BlacklistDto b, RoomDto r) {
      List<Period> periods = mlist(b.getPeriods()).map(toPeriod).value();
      return new Blacklist(b.getId(), r.toRoom(), new ArrayList<Period>(periods));
    }
  };

  private static Function2<BlacklistDto, PersonDto, Blacklist> toPerson = new Function2<BlacklistDto, PersonDto, Blacklist>() {
    @Override
    public Blacklist apply(BlacklistDto b, PersonDto p) {
      List<Period> periods = mlist(b.getPeriods()).map(toPeriod).value();
      return new Blacklist(b.getId(), p.toPerson(), new ArrayList<Period>(periods));
    }
  };

  public static Option<Blacklist> findBlacklist(long blacklistId, EntityManager em) {
    Query q = em.createNamedQuery("Blacklist.findById").setParameter("id", blacklistId);

    BlacklistDto dto;
    try {
      dto = (BlacklistDto) q.getSingleResult();
    } catch (NoResultException e) {
      return none();
    }

    if (Room.TYPE.equals(dto.getType())) {
      return findRoom(some(dto.getBlacklistedId()), null, em).map(toRoom.curry(dto));
    } else if (Person.TYPE.equals(dto.getType())) {
      return findPerson(some(dto.getBlacklistedId()), null, em).map(toPerson.curry(dto));
    } else {
      return none();
    }
  }

  public static Option<Blacklist> findBlacklistByPeriodId(long periodId, EntityManager em) {
    Query q = em.createNamedQuery("Blacklist.findByPeriodId").setParameter("periodId", periodId);

    BlacklistDto dto;
    try {
      dto = (BlacklistDto) q.getSingleResult();
    } catch (NoResultException e) {
      return none();
    }

    if (Room.TYPE.equals(dto.getType())) {
      return findRoom(some(dto.getBlacklistedId()), null, em).map(toRoom.curry(dto));
    } else if (Person.TYPE.equals(dto.getType())) {
      return findPerson(some(dto.getBlacklistedId()), null, em).map(toPerson.curry(dto));
    } else {
      return none();
    }
  }

  public static List<Blacklist> findBlacklistsByType(EntityManager em, String type, Option<Integer> limit,
          Option<Integer> offset, Option<String> name, Option<String> purpose, Option<String> sortOption) {

    if (sortOption.isNone())
      sortOption = Option.some("NAME");

    String[] sortArray = StringUtils.split(sortOption.get(), "_");
    String sortField = sortArray[0].toLowerCase();

    boolean asc = true;
    if (sortArray.length > 1) {
      if ("desc".equals(sortArray[1].toLowerCase()))
        asc = false;
    }

    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Object[]> q = cb.createQuery(Object[].class);
    Root<BlacklistDto> blacklist = q.from(BlacklistDto.class);
    Root<RoomDto> room = q.from(RoomDto.class);
    Root<PersonDto> person = q.from(PersonDto.class);

    Path<String> idPath = blacklist.get("id");
    Path<List<PeriodDto>> periodPath = blacklist.get("periods");
    HashMap<Long, Blacklist> blacklists = new LinkedHashMap<Long, Blacklist>();
    List<Predicate> predicates;
    TypedQuery<Object[]> typedQuery;
    List<Object[]> blacklistsDto;

    if (Room.TYPE.equals(type)) {
      q.select(cb.array(idPath, periodPath, room));

      predicates = new ArrayList<Predicate>();
      predicates.add(cb.equal(blacklist.get("type"), Room.TYPE));
      predicates.add(cb.equal(blacklist.get("blacklistedId"), room.get("id")));
      // Filter by Name
      for (String n : name)
        predicates.add(cb.equal(room.get("name"), n));
      for (String p : purpose)
        predicates.add(cb.equal(periodPath.get("purpose"), p));
      q.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));

      // Define sort fields and order
      Path<Object> namePath = room.get("name");
      Path<Object> datePath = periodPath.get("start");
      Path<Object> purposePath = periodPath.get("purpose");

      List<Order> order = getSortOrder(sortField, asc, cb, namePath, datePath, purposePath);
      q.orderBy(order);

      typedQuery = em.createQuery(q);

      // Limit and offset for paging
      for (int o : offset)
        typedQuery.setFirstResult(o);
      for (int l : limit)
        typedQuery.setMaxResults(l);

      blacklistsDto = typedQuery.getResultList();
      for (Object[] dto : blacklistsDto) {
        long id = (Long) dto[0];
        Blacklist b = blacklists.get(id);
        if (b == null)
          b = new Blacklist(id, ((RoomDto) dto[2]).toRoom());
        Period period = ((PeriodDto) dto[1]).toPeriod();
        if (!b.getPeriods().contains(period))
          b.addPeriod(period);
        blacklists.put(id, b);
      }
    } else if (Person.TYPE.equals(type)) {
      q.select(cb.array(idPath, periodPath, person));

      predicates = new ArrayList<Predicate>();
      predicates.add(cb.equal(blacklist.get("type"), Person.TYPE));
      predicates.add(cb.equal(blacklist.get("blacklistedId"), person.get("id")));
      // Filter by Name
      for (String n : name)
        predicates.add(cb.equal(person.get("name"), n));
      for (String p : purpose)
        predicates.add(cb.equal(periodPath.get("purpose"), p));
      q.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));

      // Define sort fields and order
      Path<Object> namePath = person.get("name");
      Path<Object> datePath = periodPath.get("start");
      Path<Object> purposePath = periodPath.get("purpose");

      List<Order> order = getSortOrder(sortField, asc, cb, namePath, datePath, purposePath);
      q.orderBy(order);

      typedQuery = em.createQuery(q);

      // Limit and offset for paging
      for (int o : offset)
        typedQuery.setFirstResult(o);
      for (int l : limit)
        typedQuery.setMaxResults(l);

      blacklistsDto = typedQuery.getResultList();
      for (Object[] dto : blacklistsDto) {
        long id = (Long) dto[0];
        Blacklist b = blacklists.get(id);
        if (b == null)
          b = new Blacklist(id, ((PersonDto) dto[2]).toPerson());
        Period period = ((PeriodDto) dto[1]).toPeriod();
        if (!b.getPeriods().contains(period))
          b.addPeriod(period);
        blacklists.put(id, b);
      }
    }

    return new ArrayList<Blacklist>(blacklists.values());
  }

  private static List<Order> getSortOrder(String sortField, boolean asc, CriteriaBuilder cb, Path<Object> namePath,
          Path<Object> datePath, Path<Object> purposePath) {
    List<Order> order = new ArrayList<Order>();
    if (asc) {
      if ("period".equals(sortField)) {
        order.add(cb.asc(datePath));
        order.add(cb.asc(namePath));
        order.add(cb.asc(purposePath));
      } else if ("purpose".equals(sortField)) {
        order.add(cb.asc(purposePath));
        order.add(cb.asc(namePath));
        order.add(cb.asc(datePath));
      } else {
        order.add(cb.asc(namePath));
        order.add(cb.asc(datePath));
        order.add(cb.asc(purposePath));
      }
    } else {
      if ("period".equals(sortField)) {
        order.add(cb.desc(datePath));
        order.add(cb.asc(namePath));
        order.add(cb.asc(purposePath));
      } else if ("purpose".equals(sortField)) {
        order.add(cb.desc(purposePath));
        order.add(cb.asc(namePath));
        order.add(cb.asc(datePath));
      } else {
        order.add(cb.desc(namePath));
        order.add(cb.asc(datePath));
        order.add(cb.asc(purposePath));
      }
    }
    return order;
  }

  public static List<Blacklist> findBlacklists(EntityManager em, Blacklistable... blacklistable) {
    if (blacklistable == null)
      blacklistable = new Blacklistable[0];

    boolean hasRoom = false;
    boolean hasPerson = false;
    for (Blacklistable bl : blacklistable) {
      if (!hasRoom && Room.TYPE.equals(bl.getType())) {
        hasRoom = true;
      } else if (!hasPerson && Person.TYPE.equals(bl.getType())) {
        hasPerson = true;
      }
      if (hasPerson && hasRoom)
        break;
    }

    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Object[]> q = cb.createQuery(Object[].class);
    Root<BlacklistDto> blacklist = q.from(BlacklistDto.class);
    Root<RoomDto> room = q.from(RoomDto.class);
    Root<PersonDto> person = q.from(PersonDto.class);

    Path<String> idPath = blacklist.get("id");
    Path<List<PeriodDto>> periodPath = blacklist.get("periods");
    Map<Long, Blacklist> blacklists = new HashMap<Long, Blacklist>();
    List<Predicate> predicates;
    TypedQuery<Object[]> typedQuery;
    List<Object[]> blacklistsDto;

    if (hasRoom || !hasRoom && !hasPerson) {
      q.select(cb.array(idPath, periodPath, room));

      predicates = new ArrayList<Predicate>();
      predicates.add(cb.equal(blacklist.get("type"), Room.TYPE));
      predicates.add(cb.equal(blacklist.get("blacklistedId"), room.get("id")));
      List<Predicate> rooms = new ArrayList<Predicate>();
      for (Blacklistable bl : blacklistable) {
        if (!Room.TYPE.equals(bl.getType()))
          continue;
        rooms.add(cb.equal(room.get("name"), bl.getName()));
      }
      predicates.add(cb.or(rooms.toArray(new Predicate[rooms.size()])));
      q.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));

      typedQuery = em.createQuery(q);
      blacklistsDto = typedQuery.getResultList();
      for (Object[] dto : blacklistsDto) {
        long id = (Long) dto[0];
        Blacklist b = blacklists.get(id);
        if (b == null)
          b = new Blacklist(id, ((RoomDto) dto[2]).toRoom());
        Period period = ((PeriodDto) dto[1]).toPeriod();
        if (!b.getPeriods().contains(period))
          b.addPeriod(period);
        blacklists.put(id, b);
      }
    }

    // -----------------------------------------------------

    if (hasPerson || !hasRoom && !hasPerson) {
      q.select(cb.array(idPath, periodPath, person));

      predicates = new ArrayList<Predicate>();
      predicates.add(cb.equal(blacklist.get("type"), Person.TYPE));
      predicates.add(cb.equal(blacklist.get("blacklistedId"), person.get("id")));
      List<Predicate> persons = new ArrayList<Predicate>();
      for (Blacklistable bl : blacklistable) {
        if (!Person.TYPE.equals(bl.getType()))
          continue;
        Person p = (Person) bl;
        persons.add(cb.equal(person.get("email"), p.getEmail()));
      }
      predicates.add(cb.or(persons.toArray(new Predicate[persons.size()])));
      q.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));

      typedQuery = em.createQuery(q);
      blacklistsDto = typedQuery.getResultList();
      for (Object[] dto : blacklistsDto) {
        long id = (Long) dto[0];
        Blacklist b = blacklists.get(id);
        if (b == null)
          b = new Blacklist(id, ((PersonDto) dto[2]).toPerson());
        Period period = ((PeriodDto) dto[1]).toPeriod();
        if (!b.getPeriods().contains(period))
          b.addPeriod(period);
        blacklists.put(id, b);
      }
    }
    return new ArrayList<Blacklist>(blacklists.values());
  }

  private static boolean hasRecordingBlacklistedPeriods(EntityManager em, RecordingDto recording)
          throws ParticipationManagementDatabaseException {
    boolean found = false;
    List<Blacklistable> blacklistables = new ArrayList<Blacklistable>();
    blacklistables.add(recording.getRoom().toRoom());
    for (PersonDto p : recording.getStaff())
      blacklistables.add(p.toPerson());
    List<Blacklist> blacklists = findBlacklists(em, blacklistables.toArray(new Blacklistable[blacklistables.size()]));
    outer: for (Blacklist b : blacklists) {
      for (Period period : b.getPeriods()) {
        if ((period.getStart().compareTo(recording.getStart()) >= 0 && period.getStart().compareTo(recording.getStop()) <= 0)
                || (period.getEnd().compareTo(recording.getStart()) >= 0 && period.getEnd().compareTo(
                        recording.getStop()) <= 0))
          found = true;
        if (found)
          break outer;
      }
    }
    return found;
  }

  public static Option<SchedulingSourceDto> findSchedulingSource(Option<Long> id, String description, EntityManager em) {
    Option<SchedulingSourceDto> schedulingSourceDto = find(id, em, SchedulingSourceDto.class);
    if (schedulingSourceDto.isSome())
      return schedulingSourceDto;

    Query q = em.createNamedQuery("SchedulingSource.findByDescription").setParameter("description", description);
    try {
      return some((SchedulingSourceDto) q.getSingleResult());
    } catch (NoResultException e) {
      return none();
    }
  }

  public static SchedulingSourceDto mergeSchedulingSource(SchedulingSource source, EntityManager em) {
    Option<SchedulingSourceDto> sourceOption = findSchedulingSource(option(source.getId()), source.getSource(), em);
    SchedulingSourceDto dto;
    if (sourceOption.isSome()) {
      dto = sourceOption.get();
      dto.setSource(source.getSource());
      em.merge(dto);
    } else {
      dto = new SchedulingSourceDto(source.getSource());
      em.persist(dto);
    }
    return dto;
  }
}
