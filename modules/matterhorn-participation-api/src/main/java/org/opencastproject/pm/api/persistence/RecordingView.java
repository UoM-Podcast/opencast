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

package org.opencastproject.pm.api.persistence;

import org.opencastproject.pm.api.Action;
import org.opencastproject.pm.api.Course;
import org.opencastproject.pm.api.Course.EmailStatus;
import org.opencastproject.pm.api.Person;
import org.opencastproject.pm.api.Recording;
import org.opencastproject.pm.api.Recording.RecordingStatus;
import org.opencastproject.util.data.Option;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Business object for a recording view.
 */
public class RecordingView {
  private final long id;

  private final Option<Long> eventId;

  private final String title;

  private final String presenter;

  private final String course;

  private final Date startDate;

  private final Date endDate;

  private final String room;

  private final String agent;

  private final String agentInputs;

  private final RecordingStatus status;

  private final EmailStatus emailStatus;

  private final Boolean requiresRecording;

  private final Boolean requiresCaptions;

  private final String actions;

  private final boolean edit;

  private final String input;

  // these values are set externally
  private Option<Long> workflowId;

  // these values are set externally
  private Option<String> mediaPackageId;

  private String processingStatus;

  public RecordingView(long id, Option<Long> eventId, String title, String presenter, String course, Date start,
          Date end, String room, String agent, String agentInputs, EmailStatus emailStatus, RecordingStatus status, Boolean requiresRecording, Boolean requiresCaptions, String actions, boolean edit, String input) {
    this.id = id;
    this.eventId = eventId;
    this.title = title;
    this.presenter = presenter;
    this.course = course;
    this.startDate = start;
    this.endDate = end;
    this.room = room;
    this.agent = agent;
    this.agentInputs = agentInputs;
    this.status = status;
    this.emailStatus = emailStatus;
    this.requiresRecording = requiresRecording;
    this.requiresCaptions = requiresCaptions;
    this.actions = actions;
    this.edit = edit;
    this.input = input;
    this.workflowId = Option.none();
    this.mediaPackageId = Option.none();
  }

  public static RecordingView fromRecording(Recording recording) {
    final List<String> actions = new ArrayList<String>();
    for (Action a : recording.getAction()) {
      actions.add(a.getName());
    }
    final List<String> presenters = new ArrayList<String>();
    for (Person p : recording.getStaff()) {
      presenters.add(p.getName());
    }
    final String c;
    Boolean requiredRecording = false;
    Boolean captionRecording = false;
    EmailStatus emailStatus = EmailStatus.UNSENT;
    if (recording.getCourse().isSome()) {
      c = recording.getCourse().get().getCourseId();
      List<String> requirements = recording.getCourse().get().getRequirements();
      requiredRecording = requirements.contains(Course.REQUIREMENT_RECORD);
      captionRecording = requirements.contains(Course.REQUIREMENT_CAPTIONS);
      emailStatus = recording.getCourse().get().getEmailStatus();
    } else {
      c = "";
    }
    return new RecordingView(
            recording.getId().get(),
            recording.getEventId(),
            recording.getTitle(),
            StringUtils.join(presenters, ", "),
            c,
            recording.getStart(),
            recording.getStop(),
            recording.getRoom().getName(),
            recording.getCaptureAgent().getMhAgent(),
            recording.getCaptureAgent().getInputs(),
            recording.getEmailStatus(),
            recording.getRecordingStatus(false),
            requiredRecording,
            captionRecording,
            StringUtils.join(actions, ", "),
            recording.isEdit(),
            recording.getRecordingInputs());
  }

  public String getTitle() {
    return title;
  }

  public String getPresenter() {
    return presenter;
  }

  public String getCourse() {
    return course;
  }

  public Date getStartDate() {
    return startDate;
  }

  public Date getEndDate() {
    return endDate;
  }

  public String getRoom() {
    return room;
  }

  public String getAgent() {
    return agent;
  }

  public String getAgentInputs() {
    return agentInputs;
  }

  public RecordingStatus getStatus() {
    return status;
  }

  public EmailStatus getEmailStatus() {
    return emailStatus;
  }

  public Boolean requiresRecording() {
    return requiresRecording;
  }

  public Boolean requiresCaptions() {
    return requiresCaptions;
  }

  public String getActions() {
    return actions;
  }

  public long getId() {
    return id;
  }

  public Option<Long> getEventId() {
    return eventId;
  }

  public boolean isEdit() {
    return edit;
  }

  public String getRecordingInput() {
    return input;
  }

  public Option<Long> getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(Option<Long> workflowId) {
    this.workflowId = workflowId;
  }

  public Option<String> getMediaPackageId() {
    return mediaPackageId;
  }

  public void seMediaPackageId(Option<String> mediaPackageId) {
    this.mediaPackageId = mediaPackageId;
  }
  public String getProcessingStatus() {
    return processingStatus;
  }

  public void setProcessingStatus(String processingStatus) {
    this.processingStatus = processingStatus;
  }
}
