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
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Option.some;

import org.opencastproject.pm.api.Action;
import org.opencastproject.pm.api.Course;
import org.opencastproject.pm.api.Course.EmailStatus;
import org.opencastproject.pm.api.Message;
import org.opencastproject.pm.api.Person;
import org.opencastproject.pm.api.Recording;
import org.opencastproject.pm.api.Recording.ReviewStatus;
import org.opencastproject.pm.api.SchedulingSource;
import org.opencastproject.pm.api.persistence.EmailView;
import org.opencastproject.pm.api.persistence.RecordingView;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.util.data.Option;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

/**
 * Entity object for recording.
 */
@Entity(name = "Recording")
@Table(name = "mh_pm_recording", uniqueConstraints = {@UniqueConstraint(columnNames = {"activity", "start_date",
        "end_date", "room"})})
@NamedQueries({
        @NamedQuery(name = "Recording.findById", query = "SELECT r FROM Recording r WHERE r.id = :id"),
        @NamedQuery(name = "Recording.findByEventId", query = "SELECT r FROM Recording r WHERE r.eventId IS NOT NULL AND r.eventId = :id"),
        @NamedQuery(name = "Recording.findAll", query = "SELECT r FROM Recording r WHERE r.deleted = FALSE"),
        @NamedQuery(name = "Recording.findByRoom", query = "SELECT r FROM Recording r WHERE r.room = :room AND r.deleted = FALSE"),
        @NamedQuery(name = "Recording.findByPerson", query = "SELECT r FROM Recording r WHERE :person MEMBER OF r.staff AND r.deleted = FALSE"),
        @NamedQuery(name = "Recording.findGroupedByActivity", query = "SELECT r FROM Recording r WHERE r.emailStatus = :emailStatus AND r.deleted = FALSE ORDER BY r.activityId"),
        @NamedQuery(name = "Recording.count", query = "SELECT COUNT(r) FROM Recording r WHERE r.deleted = FALSE"),
        @NamedQuery(name = "Recording.countByDateRange", query = "SELECT COUNT(r) FROM Recording r WHERE r.start >= :start AND r.stop < :end AND r.deleted = FALSE"),
        @NamedQuery(name = "Recording.countBlacklisted", query = "SELECT COUNT(r) FROM Recording r WHERE r.blacklisted = TRUE AND r.deleted = FALSE"),
        @NamedQuery(name = "Recording.countBlacklistedEventsByDateRangeAndPerson", query = "SELECT COUNT(r) FROM Recording r WHERE r.start >= :start AND r.stop < :end AND :person MEMBER OF r.staff AND r.deleted = FALSE"),
        @NamedQuery(name = "Recording.countBlacklistedCoursesByDateRangeAndPerson", query = "SELECT COUNT(DISTINCT r.course) FROM Recording r WHERE r.start >= :start AND r.stop < :end AND :person MEMBER OF r.staff AND r.deleted = FALSE"),
        @NamedQuery(name = "Recording.countBlacklistedEventsByDateRangeAndRoom", query = "SELECT COUNT(r) FROM Recording r WHERE r.start >= :start AND r.stop < :end AND r.room = :room AND r.deleted = FALSE"),
        @NamedQuery(name = "Recording.countBlacklistedCoursesByDateRangeAndRoom", query = "SELECT COUNT(DISTINCT r.course) FROM Recording r WHERE r.start >= :start AND r.stop < :end AND r.room = :room AND r.deleted = FALSE"),
        @NamedQuery(name = "Recording.countBlacklistedByDateRange", query = "SELECT COUNT(r) FROM Recording r WHERE r.start >= :start AND r.stop < :end AND r.blacklisted = TRUE AND r.deleted = FALSE"),
        @NamedQuery(name = "Recording.countConfirmed", query = "SELECT COUNT(r) FROM Recording r WHERE r.reviewStatus = org.opencastproject.pm.api.Recording$ReviewStatus.CONFIRMED AND r.deleted = FALSE"),
        @NamedQuery(name = "Recording.countRespones", query = "SELECT COUNT(r) FROM Recording r WHERE r.reviewDate IS NOT NULL AND r.deleted = FALSE"),
        @NamedQuery(name = "Recording.countByEmailState", query = "SELECT COUNT(r) FROM Recording r WHERE r.emailStatus = :emailState AND r.deleted = FALSE"),
        @NamedQuery(name = "Recording.countByEmailStateForCourse", query = "SELECT COUNT(r) FROM Recording r WHERE r.course = :course AND r.emailStatus = :emailState AND r.deleted = FALSE"),
        @NamedQuery(name = "Recording.countByReviewState", query = "SELECT COUNT(r) FROM Recording r WHERE r.reviewStatus = :reviewState AND r.deleted = FALSE"),
        @NamedQuery(name = "Recording.countConfirmedByDateRange", query = "SELECT COUNT(r) FROM Recording r WHERE r.reviewDate >= :start AND r.reviewDate < :end AND r.reviewStatus = org.opencastproject.pm.api.Recording$ReviewStatus.CONFIRMED AND r.deleted = FALSE"),
        @NamedQuery(name = "Recording.countUnconfirmed", query = "SELECT COUNT(r) FROM Recording r WHERE r.reviewStatus = org.opencastproject.pm.api.Recording$ReviewStatus.UNCONFIRMED AND r.deleted = FALSE"),
        @NamedQuery(name = "Recording.countOptedOut", query = "SELECT COUNT(r) FROM Recording r WHERE r.reviewStatus = org.opencastproject.pm.api.Recording$ReviewStatus.OPTED_OUT AND r.deleted = FALSE"),
        @NamedQuery(name = "Recording.clear", query = "DELETE FROM Recording")})
public class RecordingDto {

  @Id
  @GeneratedValue
  @Column(name = "id")
  private long id;

  @Column(name = "activity", nullable = false)
  private String activityId;

  @Column(name = "title", nullable = false)
  private String title;

  @OneToMany
  @JoinTable(name = "mh_pm_recording_staff", joinColumns = {@JoinColumn(name = "recording_id", referencedColumnName = "id")}, inverseJoinColumns = {@JoinColumn(name = "person_id", referencedColumnName = "id")})
  private List<PersonDto> staff = new ArrayList<PersonDto>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course", referencedColumnName = "id")
  private CourseDto course;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "room", referencedColumnName = "id")
  private RoomDto room;

  @Column(name = "modification_date", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date modificationDate;

  @Column(name = "deleted", nullable = false)
  private boolean deleted = false;

  @Column(name = "start_date", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date start;

  @Column(name = "end_date", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date stop;

  @Column(name = "blacklisted", nullable = false)
  private boolean blacklisted = false;

  @Enumerated(EnumType.STRING)
  private EmailStatus emailStatus;

  @Enumerated(EnumType.STRING)
  private ReviewStatus reviewStatus;

  @Column(name = "review_date")
  @Temporal(TemporalType.TIMESTAMP)
  private Date reviewDate;

  @Column(name = "fingerprint", nullable = true, length = 32)
  private String fingerprint;

  @OneToMany
  @JoinTable(name = "mh_pm_recording_participation", joinColumns = {@JoinColumn(name = "recording_id", referencedColumnName = "id")}, inverseJoinColumns = {@JoinColumn(name = "person_id", referencedColumnName = "id")})
  private List<PersonDto> participation = new ArrayList<PersonDto>();

  @OneToMany
  @JoinTable(name = "mh_pm_recording_messages", joinColumns = {@JoinColumn(name = "recording_id", referencedColumnName = "id")}, inverseJoinColumns = {@JoinColumn(name = "message_id", referencedColumnName = "id")})
  private List<MessageDto> messages = new ArrayList<MessageDto>();

  @Column(name = "event_id", nullable = true)
  private Long eventId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "capture_agent", referencedColumnName = "id")
  private CaptureAgentDto captureAgent;

  @OneToMany
  @JoinTable(name = "mh_pm_recording_action", joinColumns = {@JoinColumn(name = "recording_id", referencedColumnName = "id")}, inverseJoinColumns = {@JoinColumn(name = "action_id", referencedColumnName = "id")})
  private List<ActionDto> actions = new ArrayList<ActionDto>();

  @Column(name = "edit", nullable = false)
  private boolean edit;

  @Column(name = "recording_inputs", nullable = false)
  private String recordingInputs;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "source", referencedColumnName = "id")
  private SchedulingSourceDto schedulingSource;

  /**
   * Default constructor
   */
  public RecordingDto() {
  }

  /**
   * Creates a recording
   *
   * @param activityId
   *         the activity identifier
   * @param title
   *         the recording title
   * @param blacklisted
   *         the blacklisted flag
   * @param staff
   *         the staff
   * @param course
   *         the course
   * @param room
   *         the room
   * @param modificationDate
   *         the modification date
   * @param start
   *         the start date
   * @param stop
   *         the end date
   * @param participation
   *         the participation list
   * @param messages
   *         the message list
   * @param eventId
   *         the event id
   * @param captureAgent
   *         the capture agent
   * @param action
   *         the action
   * @param emailStatus
   *         the review status
   * @param reviewStatus
   *         the review status
   * @param reviewDate
   *         the review date
   * @param deleted
   *         the deleted flag
   * @param fingerprint
   *         the entry's hash value
   * @param edit
   * @param recordingInputs
   */
  public RecordingDto(String activityId, String title, boolean blacklisted, List<PersonDto> staff, CourseDto course,
                      RoomDto room, Date modificationDate, Date start, Date stop, List<PersonDto> participation,
                      List<MessageDto> messages, Long eventId, CaptureAgentDto captureAgent, List<ActionDto> action,
                      EmailStatus emailStatus, ReviewStatus reviewStatus, Date reviewDate, boolean deleted, String fingerprint,
                      boolean edit, String recordingInputs) {
    if (staff != null)
      this.staff = staff;
    if (participation != null)
      this.participation = participation;
    if (messages != null)
      this.messages = messages;
    if (action != null)
      this.actions = action;

    this.activityId = activityId;
    this.title = title;
    this.blacklisted = blacklisted;
    this.course = course;
    this.room = room;
    this.modificationDate = modificationDate;
    this.start = start;
    this.stop = stop;
    this.eventId = eventId;
    this.captureAgent = captureAgent;
    this.emailStatus = emailStatus;
    this.reviewStatus = reviewStatus;
    this.reviewDate = reviewDate;
    this.deleted = deleted;
    this.fingerprint = fingerprint;
    this.edit = edit;
    this.recordingInputs = recordingInputs;
  }

  /**
   * Sets the recording identifier
   *
   * @param id
   *         the recording id
   */
  public void setId(long id) {
    this.id = id;
  }

  /**
   * Returns the recording identifier
   *
   * @return the recording id
   */
  public long getId() {
    return id;
  }

  /**
   * Sets the activity identifier
   *
   * @param activityId
   *         the activity id
   */
  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

  /**
   * Returns the activity identifier
   *
   * @return the activity id
   */
  public String getActivityId() {
    return activityId;
  }

  /**
   * Sets the recording title
   *
   * @param title
   *         the recording tile
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * Returns the recording title
   *
   * @returns the recording tile
   */
  public String getTitle() {
    return this.title;
  }

  /**
   * Whether the recording is blacklisted
   *
   * @returns the blacklisted flag
   */
  public boolean isBlacklisted() {
    return blacklisted;
  }

  /**
   * Sets the event identifier
   *
   * @param eventId
   *         the event id
   */
  public void setEventId(Long eventId) {
    this.eventId = eventId;
  }

  /**
   * Returns the event identifier
   *
   * @return the event id
   */
  public long getEventId() {
    return eventId;
  }

  /**
   * Sets wether the recording is deleted
   *
   * @param deleted
   *         the deletion flag
   */
  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  /**
   * Wether the recording is deleted
   *
   * @return the deleted flag
   */
  public boolean isDeleted() {
    return deleted;
  }

  /**
   * Sets the action
   *
   * @param action
   *         the action
   */
  public void setAction(List<ActionDto> action) {
    this.actions = action;
  }

  /**
   * Returns the action
   *
   * @return the action
   */
  public List<ActionDto> getAction() {
    return actions;
  }

  /**
   * Add an action to trigger with this recording
   *
   * @param action
   *         the action to be triggered
   * @return true if this collection changed as a result of the call
   */
  public boolean addAction(ActionDto action) {
    if (action == null) {
      throw new IllegalArgumentException("The action must not be null!");
    }
    return actions.add(action);
  }

  /**
   * Remove an action to trigger with this recording
   *
   * @param action
   *         the action to remove
   * @return true if this collection changed as a result of the call
   */
  public boolean removeAction(ActionDto action) {
    if (action == null) {
      throw new IllegalArgumentException("The action must not be null!");
    }
    return actions.remove(action);
  }

  /**
   * Sets the capture agent
   *
   * @param captureAgent
   *         the capture agent
   */
  public void setCaptureAgent(CaptureAgentDto captureAgent) {
    this.captureAgent = captureAgent;
  }

  /**
   * Returns the capture agent
   *
   * @return the capture agent
   */
  public CaptureAgentDto getCaptureAgent() {
    return captureAgent;
  }

  /**
   * Sets the course
   *
   * @param course
   *         the course
   */
  public void setCourse(CourseDto course) {
    this.course = course;
  }

  /**
   * Returns the course
   *
   * @return the course
   */
  public CourseDto getCourse() {
    return course;
  }

  /**
   * Sets the modification date
   *
   * @param modificationDate
   *         the modification date
   */
  public void setModificationDate(Date modificationDate) {
    this.modificationDate = modificationDate;
  }

  /**
   * Returns the modification date
   *
   * @return the modification date
   */
  public Date getModificationDate() {
    return modificationDate;
  }

  /**
   * Sets the start date
   *
   * @param start
   *         the start date
   */
  public void setStart(Date start) {
    this.start = start;
  }

  /**
   * Returns the start date
   *
   * @return the start date
   */
  public Date getStart() {
    return start;
  }

  /**
   * Sets the stop date
   *
   * @param stop
   *         the stop date
   */
  public void setStop(Date stop) {
    this.stop = stop;
  }

  /**
   * Returns the stop date
   *
   * @return the stop date
   */
  public Date getStop() {
    return stop;
  }

  /**
   * Sets the room
   *
   * @param room
   *         the room
   */
  public void setRoom(RoomDto room) {
    this.room = room;
  }

  /**
   * Returns the room
   *
   * @return the room
   */
  public RoomDto getRoom() {
    return room;
  }

  /**
   * Sets the staff list
   *
   * @param staff
   *         the staff list
   */
  public void setStaff(List<PersonDto> staff) {
    this.staff = notNull(staff, "staff");
  }

  /**
   * Returns the staff list
   *
   * @return the staff list
   */
  public List<PersonDto> getStaff() {
    return staff;
  }

  /**
   * Add a member to the staff
   *
   * @param staffMember
   *         the new staff member
   * @return true if this collection changed as a result of the call
   */
  public boolean addStaffMember(PersonDto staffMember) {
    return staff.add(notNull(staffMember, "staffMember"));
  }

  /**
   * Remove a member of the staff
   *
   * @param staffMember
   *         the member to remove from the staff
   * @return true if this collection changed as a result of the call
   */
  public boolean removeStaffMember(PersonDto staffMember) {
    return staff.remove(notNull(staffMember, "staffMember"));
  }

  /**
   * Sets the participation list
   *
   * @param participation
   *         the participation list
   */
  public void setParticipation(List<PersonDto> participation) {
    this.participation = notNull(participation, "participation");
  }

  /**
   * Returns the participation list
   *
   * @return the participation list
   */
  public List<PersonDto> getParticipation() {
    return participation;
  }

  /**
   * Add a participant to the recording
   *
   * @param participant
   *         the participant to add to this recording
   * @return true if this collection changed as a result of the call
   */
  public boolean addParticipant(PersonDto participant) {
    return participation.add(notNull(participant, "participant"));
  }

  /**
   * Remove a participant from this recording
   *
   * @param participant
   *         the participant to remove from this recording
   * @return true if this collection changed as a result of the call
   */
  public boolean removeParticipant(PersonDto participant) {
    return participation.remove(notNull(participant, "participant"));
  }

  /**
   * Sets the email status
   *
   * @param emailStatus
   *         the email status
   */
  public void setEmailStatus(EmailStatus emailStatus) {
    this.emailStatus = emailStatus;
  }

  /**
   * Returns the email status
   *
   * @return the email status
   */
  public EmailStatus getEmailStatus() {
    return emailStatus;
  }

  /**
   * Sets the review status
   *
   * @param reviewStatus
   *         the review status
   */
  public void setReviewStatus(ReviewStatus reviewStatus) {
    this.reviewStatus = reviewStatus;
  }

  /**
   * Returns the review status
   *
   * @return the review status
   */
  public ReviewStatus getReviewStatus() {
    return reviewStatus;
  }

  /**
   * Sets the review date
   *
   * @param reviewDate
   *         the review date
   */
  public void setReviewDate(Date reviewDate) {
    this.reviewDate = reviewDate;
  }

  /**
   * Returns the review date
   *
   * @return the review date
   */
  public Date getReviewDate() {
    return reviewDate;
  }

  /**
   * Sets the message list
   *
   * @param messages
   *         the message list
   */
  public void setMessages(List<MessageDto> messages) {
    this.messages = notNull(messages, "messages");
  }

  /**
   * Returns the message list
   *
   * @return the message list
   */
  public List<MessageDto> getMessages() {
    return messages;
  }

  /**
   * Add a message to the recording
   *
   * @param message
   *         the message to add to this recording
   * @return true if this collection changed as a result of the call
   */
  public boolean addMessage(MessageDto message) {
    return messages.add(notNull(message, "message"));
  }

  /**
   * Remove a message from the recording
   *
   * @param message
   *         the message to remove from this recording
   * @return true if this collection changed as a result of the call
   */
  public boolean removeMessage(MessageDto message) {
    return messages.remove(notNull(message, "message"));
  }

  /**
   * Sets the blacklisted flag
   *
   * @param blacklisted
   *         the status of the blacklisted flag
   */
  public void setBlacklisted(boolean blacklisted) {
    this.blacklisted = blacklisted;
  }

  /**
   * Returns the value of blacklisted flag
   *
   * @return the value of the blacklisted flag
   */
  public boolean getBlacklisted() {
    return blacklisted;
  }

  /**
   * Returns a fingerprint for this recording.
   *
   * @return the fingerprint
   */
  public Option<String> getFingerprint() {
    return Option.option(fingerprint);
  }

  /**
   * Sets a fingerprint for this recording, which is supposed to be a 32 bit hash.
   *
   * @param fingerprint
   *         the fingerprint
   * @throws IllegalArgumentException
   *         if the hash is longer than 32 bit
   */
  public void setFingerprint(String fingerprint) throws IllegalArgumentException {
    if (StringUtils.isNotBlank(fingerprint) && fingerprint.length() > 32)
      throw new IllegalArgumentException("Fingerprint must not be longer than 32 bit");
    this.fingerprint = fingerprint;
  }

  public boolean isEdit() {
    return edit;
  }

  public void setEdit(boolean edit) {
    this.edit = edit;
  }

  public String getRecordingInputs() {
    return recordingInputs;
  }

  public void setRecordingInputs(String recordingInputs) {
    this.recordingInputs = recordingInputs;
  }

  /**
   * Returns the scheduling schedulingSource
   *
   * @return the scheduling source
   */
  public Option<SchedulingSourceDto> getSchedulingSource() {
    return Option.option(schedulingSource);
  }

  /**
   * Sets the scheduling schedulingSource
   *
   * @param schedulingSource
   *         the scheduling source
   */
  public void setSchedulingSource(SchedulingSourceDto schedulingSource) {
    this.schedulingSource = schedulingSource;
  }

  /**
   * Returns the business object of the recording
   *
   * @return the business object model of this recording
   */
  public Recording toRecording(UserDirectoryService userDirectoryService) {
    final Recording rec = toRecordingWithoutMessages();
    // add the messages
    for (MessageDto m : this.messages) {
      rec.addMessage(m.toMessage(userDirectoryService));
    }
    return rec;
  }

  /**
   * Convert to a {@link org.opencastproject.pm.api.Recording} but do not add
   * the {@link org.opencastproject.pm.api.Message}s
   */
  private Recording toRecordingWithoutMessages() {
    Option<Course> c = course == null ? none(Course.class) : some(course.toCourse());
    Option<SchedulingSource> s = schedulingSource == null ? none(SchedulingSource.class) : some(schedulingSource.toSchedulingSource());
    Recording rec = Recording.recording(activityId,
            title,
            blacklisted,
            new ArrayList<Person>(),
            c,
            room.toRoom(),
            modificationDate,
            start,
            stop,
            new ArrayList<Person>(),
            new ArrayList<Message>(),
            option(eventId),
            captureAgent.toCaptureAgent(),
            new ArrayList<Action>(),
            emailStatus,
            reviewStatus,
            option(reviewDate),
            deleted,
            edit,
            recordingInputs);
    for (PersonDto p : this.staff) {
      rec.addStaffMember(p.toPerson());
    }
    for (PersonDto p : this.participation) {
      rec.addParticipant(p.toPerson());
    }
    for (ActionDto a : this.actions) {
      rec.addAction(a.toAction());
    }
    rec.setId(some(id));
    rec.setSchedulingSource(s);
    rec.setFingerprint(option(fingerprint));
    return rec;
  }

  public RecordingView toRecordingView() {
    // the view does not deal with messages so they can be left out because
    // they are expensive to create
    return RecordingView.fromRecording(toRecordingWithoutMessages());
  }

  public EmailView toEmailView(int count) {
    return EmailView.fromRecording(toRecordingWithoutMessages(), count);
  }
}
