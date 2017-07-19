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

package org.opencastproject.pm.api;

import static org.opencastproject.util.RequireUtil.notNull;
import static org.opencastproject.util.data.Collections.nil;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Option.some;

import org.opencastproject.pm.api.Course.EmailStatus;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.EqualsUtil;
import org.opencastproject.util.Jsons;
import org.opencastproject.util.Jsons.Obj;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Business object for a recording.
 */
public class Recording {

  public enum RecordingStatus {
    READY, OPTED_OUT, BLACKLISTED
  }

  public enum ReviewStatus {
    UNCONFIRMED, CONFIRMED, OPTED_OUT
  }

  /** The recording identifier */
  private Option<Long> id;

  /** The syllabus+ activity id */
  private String activityId;

  /** The MH scheduled event identifier */
  private Option<Long> eventId;

  /** The title of the recording */
  private String title;

  /** The staff for this recording */
  private List<Person> staff = new ArrayList<>();

  /** The course */
  private Option<Course> course = Option.<Course> none();

  /** The room */
  private Room room;

  /** The modification date */
  private Date modificationDate;

  /** Whether the recording is deleted */
  private boolean deleted;

  /** Flag for the blacklisted recording */
  private boolean blacklisted;

  /** The start date */
  private Date start;

  /** The stop date */
  private Date stop;

  /** The email status of the recording */
  private EmailStatus emailStatus;

  /** The review status of the recording */
  private ReviewStatus reviewStatus;

  /** The review date */
  private Option<Date> reviewDate;

  /** The participation list */
  private List<Person> participation = new ArrayList<>();

  /** The list of messages */
  private List<Message> messages = new ArrayList<>();

  /** The capture agent */
  private CaptureAgent captureAgent;

  /** The fingerprint */
  private Option<String> fingerprint;

  /** The scheduling schedulingSource */
  private Option<SchedulingSource> schedulingSource;

  /** Edit flag. If true the recording will be marked for editing. */
  private boolean edit;

  /** The inputs to be recorded */
  private String recordingInputs;

  /** The actions triggered by the recording */
  private List<Action> actions = new ArrayList<>();

  public Recording(Option<Long> id, String activityId, Option<Long> eventId, String title, List<Person> staff,
          Option<Course> course, Room room, Date modificationDate, boolean deleted, boolean blacklisted, Date start,
          Date stop, EmailStatus emailStatus, ReviewStatus reviewStatus, Option<Date> reviewDate, List<Person> participation,
          List<Message> messages, CaptureAgent captureAgent, List<Action> actions, Option<String> fingerprint,
          boolean edit, String recordingInputs) {
    this.id = id;
    this.activityId = activityId;
    this.eventId = eventId;
    this.title = title;
    // since this field is mutable a copy has to be created to both prevent side effects on the passed list
    // and to ensure the list is mutable
    this.staff = new ArrayList<>(staff);
    this.course = course;
    this.room = room;
    this.modificationDate = modificationDate;
    this.deleted = deleted;
    this.blacklisted = blacklisted;
    this.start = start;
    this.stop = stop;
    this.emailStatus = emailStatus;
    this.reviewStatus = reviewStatus;
    this.reviewDate = reviewDate;
    this.participation = new ArrayList<>(participation);
    this.messages = new ArrayList<>(messages);
    this.captureAgent = captureAgent;
    this.actions = new ArrayList<>(actions);
    this.fingerprint = fingerprint;
    this.edit = edit;
    this.recordingInputs = recordingInputs;
    this.schedulingSource = Option.none(SchedulingSource.class);
  }

  /**
   * Creates a recording
   *
   * @param activityId
   *          the activity id
   * @param title
   *          the recording title
   * @param staff
   *          the staff list
   * @param course
   *          the course
   * @param room
   *          the room
   * @param modificationDate
   *          the modification date
   * @param start
   *          the start date
   * @param stop
   *          the end date
   * @param participation
   *          the participation list
   * @param captureAgent
   *          the capture agent
   * @return
   */
  public static Recording recording(String activityId, String title, List<Person> staff, Course course, Room room,
          Date modificationDate, Date start, Date stop, List<Person> participation, CaptureAgent captureAgent) {
    return new Recording(none(Long.class), activityId, none(Long.class), title, staff, some(course), room,
            modificationDate, false, false, start, stop, EmailStatus.UNSENT, ReviewStatus.UNCONFIRMED, none(Date.class), participation,
            nil(Message.class), captureAgent, nil(Action.class), none(String.class), false, "default");
  }

  /**
   * Creates a recording
   *
   * @param activityId
   *          the activity id
   * @param title
   *          the recording title
   * @param blacklisted
   *          the flag for the blacklisted recording
   * @param staff
   *          the staff list
   * @param course
   *          the course
   * @param room
   *          the room
   * @param modificationDate
   *          the modification date
   * @param start
   *          the start date
   * @param stop
   *          the end date
   * @param participation
   *          the participation list
   * @param messages
   *          the message list
   * @param eventId
   *          the event id
   * @param captureAgent
   *          the capture agent
   * @param actions
   *          the actions triggered by this recording
   * @param emailStatus
   *          the email status
   * @param reviewStatus
   *          the review status
   * @param reviewDate
   *          the review date
   * @param deleted
   *          the deleted flag
   * @param edit
   *          the editing flag
   * @param recordingInputs
   *          the recorded input
   * @return
   */
  public static Recording recording(String activityId, String title, boolean blacklisted, List<Person> staff,
          Option<Course> course, Room room, Date modificationDate, Date start, Date stop, List<Person> participation,
          List<Message> messages, Option<Long> eventId, CaptureAgent captureAgent, List<Action> actions,
          EmailStatus emailStatus, ReviewStatus reviewStatus, Option<Date> reviewDate, boolean deleted, boolean edit, String recordingInputs) {
    return new Recording(none(Long.class), activityId, eventId, title, staff, course, room, modificationDate, deleted,
            blacklisted, start, stop, emailStatus, reviewStatus, reviewDate, participation, messages, captureAgent, actions,
            none(String.class), edit, recordingInputs);
  }

  /**
   * Sets the recording identifier
   *
   * @param id
   *          the recording id
   */
  public void setId(Option<Long> id) {
    this.id = id;
  }

  /**
   * Returns the recording identifier
   *
   * @return the recording id
   */
  public Option<Long> getId() {
    return id;
  }

  public static final Function<Recording, Option<Long>> getId = new Function<Recording, Option<Long>>() {
    @Override
    public Option<Long> apply(Recording recording) {
      return recording.getId();
    }
  };

  /**
   * Sets the activity identifier
   *
   * @param activityId
   *          the activity id
   */
  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

  /**
   * Returns the activity id
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
   *          the recording tile
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
   * Sets the event identifier
   *
   * @param eventId
   *          the event id
   */
  public void setEventId(Long eventId) {
    this.eventId = option(eventId);
  }

  /**
   * Sets whether the recording is deleted
   *
   * @param deleted
   *          the deleted flag
   */
  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  /**
   * Returns whether the recording is deleted
   *
   * @return the deleted flag
   */
  public boolean isDeleted() {
    return deleted;
  }

  /**
   * Returns the event identifier
   *
   * @return the event id
   */
  public Option<Long> getEventId() {
    return eventId;
  }

  /**
   * Sets the action
   *
   * @param action
   *          the action
   */
  public void setAction(List<Action> action) {
    this.actions = action;
  }

  /**
   * Returns the action
   *
   * @return the action
   */
  public List<Action> getAction() {
    return actions;
  }

  /**
   * Add an action to trigger with this recording
   *
   * @param action
   *          the action to be triggered
   * @return true if this collection changed as a result of the call
   */
  public boolean addAction(Action action) {
    if (action == null)
      throw new IllegalArgumentException("The action must not be null!");

    return actions.add(action);
  }

  /**
   * Remove an action to trigger with this recording
   *
   * @param action
   *          the action to remove
   * @return true if this collection changed as a result of the call
   */
  public boolean removeAction(Action action) {
    if (action == null)
      throw new IllegalArgumentException("The action must not be null!");

    return actions.remove(action);
  }

  /**
   * Sets the capture agent
   *
   * @param captureAgent
   *          the capture agent
   */
  public void setCaptureAgent(CaptureAgent captureAgent) {
    this.captureAgent = captureAgent;
  }

  /**
   * Returns the capture agent
   *
   * @return the capture agent
   */
  public CaptureAgent getCaptureAgent() {
    return captureAgent;
  }

  /**
   * Sets the course
   *
   * @param course
   *          the course
   */
  public void setCourse(Option<Course> course) {
    this.course = course;
  }

  /**
   * Returns the course
   *
   * @return the course
   */
  public Option<Course> getCourse() {
    return course;
  }

  public static final Function<Recording, Option<Course>> getCourse = new Function<Recording, Option<Course>>() {
    @Override
    public Option<Course> apply(Recording recording) {
      return recording.getCourse();
    }
  };

  /**
   * Sets the modification date
   *
   * @param modificationDate
   *          the modification date
   */
  public void setModificationDate(Date modificationDate) {
    this.modificationDate = modificationDate;
  }

  /**
   * Returns the modification date
   *
   * @return the modificationDate date
   */
  public Date getModificationDate() {
    return modificationDate;
  }

  /**
   * Sets the start date
   *
   * @param start
   *          the start date
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
   *          the stop date
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
   *          the room
   */
  public void setRoom(Room room) {
    this.room = room;
  }

  /**
   * Returns the room
   *
   * @return the room
   */
  public Room getRoom() {
    return room;
  }

  /**
   * Sets the staff list
   *
   * @param staff
   *          the staff list
   */
  public void setStaff(List<Person> staff) {
    this.staff = notNull(staff, "staff");
  }

  /**
   * Returns the staff list
   *
   * @return the staff list
   */
  public List<Person> getStaff() {
    return staff;
  }

  public static final Function<Recording, List<Person>> getStaff = new Function<Recording, List<Person>>() {
    @Override
    public List<Person> apply(Recording recording) {
      return recording.getStaff();
    }
  };

  /**
   * Add a member to the staff
   *
   * @param staffMember
   *          the new staff member
   * @return true if this collection changed as a result of the call
   */
  public boolean addStaffMember(Person staffMember) {
    return staff.add(notNull(staffMember, "staffMember"));
  }

  /**
   * Remove a member of the staff
   *
   * @param staffMember
   *          the member to remove from the staff
   * @return true if this collection changed as a result of the call
   */
  public boolean removeStaffMember(Person staffMember) {
    return staff.remove(notNull(staffMember, "staffMember"));
  }

  /**
   * Sets the participation list
   *
   * @param participation
   *          the participation list
   */
  public void setParticipation(List<Person> participation) {
    this.participation = notNull(participation, "participation");
  }

  /**
   * Returns the participation list
   *
   * @return the participation list
   */
  public List<Person> getParticipation() {
    return participation;
  }

  /**
   * Add a participant to the recording
   *
   * @param participant
   *          the participant to add to this recording
   * @return true if this collection changed as a result of the call
   */
  public boolean addParticipant(Person participant) {
    return participation.add(notNull(participant, "participant"));
  }

  /**
   * Remove a participant from this recording
   *
   * @param participant
   *          the participant to remove from this recording
   * @return true if this collection changed as a result of the call
   */
  public boolean removeParticipant(Person participant) {
    return participation.remove(notNull(participant, "participant"));
  }

  /**
   * Sets the email status
   *
   * @param emailStatus
   *          the review status
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
   *          the review status
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
   *          the review date
   */
  public void setReviewDate(Option<Date> reviewDate) {
    this.reviewDate = reviewDate;
  }

  /**
   * Returns the review date
   *
   * @return the review date
   */
  public Option<Date> getReviewDate() {
    return reviewDate;
  }

  /**
   * Returns the recording status
   *
   * @param ignoreOptOut - override lecturer's optopt choice
   *
   * @return the recording status
   */
  public RecordingStatus getRecordingStatus(Boolean ignoreOptOut) {
    if (blacklisted)
      return RecordingStatus.BLACKLISTED;

    if (ignoreOptOut)
      return RecordingStatus.READY;

    if (course.isSome() && course.get().isOptedOut())
      return RecordingStatus.OPTED_OUT;

    switch (reviewStatus) {
      case UNCONFIRMED:
      case CONFIRMED:
        return RecordingStatus.READY;
      case OPTED_OUT:
        return RecordingStatus.OPTED_OUT;
      default:
        throw new IllegalStateException("Unknown review status: " + reviewStatus);
    }
  }

  /**
   * Sets the message list
   *
   * @param messages
   *          the message list
   */
  public void setMessages(List<Message> messages) {
    this.messages = notNull(messages, "messages");
  }

  /**
   * Returns the message list
   *
   * @return the message list
   */
  public List<Message> getMessages() {
    return messages;
  }

  /**
   * Add a message to the recording
   *
   * @param message
   *          the message to add to this recording
   * @return true if this collection changed as a result of the call
   */
  public boolean addMessage(Message message) {
    return messages.add(notNull(message, "message"));
  }

  /**
   * Remove a message from the recording
   *
   * @param message
   *          the message to remove from this recording
   * @return true if this collection changed as a result of the call
   */
  public boolean removeMessage(Message message) {
    return messages.remove(notNull(message, "message"));
  }

  /**
   * Sets the blacklisted flag
   *
   * @param blacklisted
   *          the status of the blacklisted flag
   */
  public void setBlacklisted(boolean blacklisted) {
    this.blacklisted = blacklisted;
  }

  /**
   * Returns the value of blacklisted flag
   *
   * @return the value of the blacklisted flag
   */
  public boolean isBlacklisted() {
    return blacklisted;
  }

  /**
   * Sets the recording's fingerprint, which should not exceed 32 bits.
   *
   * @param fingerprint
   *          the fingerprint
   */
  public void setFingerprint(Option<String> fingerprint) {
    this.fingerprint = fingerprint;
  }

  /**
   * Returns the recording's fingerprint.
   *
   * @return the fingerprint
   */
  public Option<String> getFingerprint() {
    return fingerprint;
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

  public void setSchedulingSource(Option<SchedulingSource> schedulingSource) {
    this.schedulingSource = schedulingSource;
  }

  public Option<SchedulingSource> getSchedulingSource() {
    return schedulingSource;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Recording recording = (Recording) o;
    return id.equals(recording.getId()) && activityId.equals(recording.getActivityId())
            && title.equals(recording.getTitle())
            && blacklisted == recording.isBlacklisted()
            && staff.equals(recording.getStaff())
            && course.equals(recording.getCourse())
            && room.equals(recording.getRoom())
            && modificationDate.equals(recording.getModificationDate())
            && start.equals(recording.getStart())
            && stop.equals(recording.getStop())
            && participation.equals(recording.getParticipation())
            && messages.equals(recording.getMessages())
            && eventId.equals(recording.getEventId())
            && captureAgent.equals(recording.getCaptureAgent())
            && actions.equals(recording.getAction())
            && emailStatus.equals(recording.getEmailStatus())
            && (reviewDate == null && recording.getReviewDate() == null || reviewDate.equals(recording.getReviewDate()))
            && reviewStatus.equals(recording.getReviewStatus())
            && deleted == recording.isDeleted()
            && edit == recording.edit
            && schedulingSource.equals(recording.getSchedulingSource());
  }

  @Override
  public int hashCode() {
    return EqualsUtil.hash(id, activityId, title, blacklisted, staff, course, room, modificationDate, start, stop,
            participation, messages, eventId, captureAgent, actions, emailStatus, reviewStatus, reviewDate, deleted, edit, schedulingSource);
  }

  @Override
  public String toString() {
    return "Recording:" + id;
  }

  public Obj toJson() {
    return Jsons.obj(Jsons.p("title", title), Jsons.p("start", DateTimeSupport.toUTC(start.getTime())),
            Jsons.p("end", DateTimeSupport.toUTC(start.getTime())), Jsons.p("location", room.toJson()));
  }
}
