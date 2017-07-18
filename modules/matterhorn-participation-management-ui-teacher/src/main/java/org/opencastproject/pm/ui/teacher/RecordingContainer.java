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

package org.opencastproject.pm.ui.teacher;

import static java.lang.String.format;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.event.comment.EventComment;
import org.opencastproject.event.comment.EventCommentException;
import org.opencastproject.event.comment.EventCommentService;
import org.opencastproject.pm.api.Person;
import org.opencastproject.pm.api.Recording;
import org.opencastproject.pm.api.Recording.ReviewStatus;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabaseException;
import org.opencastproject.pm.api.persistence.RecordingQuery;
import org.opencastproject.pm.api.persistence.RecordingView;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowSet;

import com.vaadin.data.util.BeanItemContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class RecordingContainer extends BeanItemContainer<RecordingView> implements Serializable {
  private static final Logger log = LoggerFactory.getLogger(RecordingContainer.class);
  private final TeacherPm teacherPm;
  private final ParticipationManagementDatabase pm;
  private final RecordingQuery baseQuery;
  private final EventCommentService eventCommentService;
  private final SchedulerService schedulerService;
  private final WorkflowServiceUtils workflowServiceUtils;

  public RecordingContainer(final TeacherPm teacherPm,
          final Option<WorkflowServiceUtils> workflowServices,
          Person teacher, Boolean future) {
    super(RecordingView.class);
    this.teacherPm = teacherPm;
    this.pm = teacherPm.getParticipationManagementDatabase().get().get();
    this.eventCommentService = teacherPm.getEventCommentService();
    this.schedulerService = teacherPm.getSchedulerService();
    this.workflowServiceUtils = workflowServices.getOrElseNull();

    this.baseQuery = RecordingQuery.createWithoutDeleted().withStaff(new Person[]{teacher}).withoutBlacklisted();

    try {
      final List<Recording> recordings = pm.findRecordings(baseQuery);
      final Date now = new Date();
      RecordingView view;
      log.debug("Found " + recordings.size() + " recordings");
      for (Recording r : recordings) {
        view = RecordingView.fromRecording(r);
        if (!future) {
          if (view.getStartDate().before(now)) {
            view = addProcessingStatus(view, now);
            if ("upcoming".equals(view.getProcessingStatus()) || "failed".equals(view.getProcessingStatus()) || "not_recorded".equals(view.getProcessingStatus())) {
              // ignore events that aren't scheduled to be recorded
              continue;
            }
          addBean(view);
          }
        } else {
          if (view.getStartDate().after(now)) {
            // Initially confirm of all upcoming recordings
            if (r.getReviewStatus() != ReviewStatus.CONFIRMED && r.getReviewStatus() != ReviewStatus.OPTED_OUT) {
              r.setReviewStatus(ReviewStatus.CONFIRMED);
              r.setReviewDate(some(new Date()));
              log.trace("Update recording " + r.getId());
              pm.updateRecording(r);
            }

            addBean(view);
          }
        }
      }
    } catch (ParticipationManagementDatabaseException e) {
      chuck(e);
    }
  }

  public final void refresh() {
    Date now = new Date();

    log.trace("Refresh");
    try {
      removeAllItems();
      for (RecordingView view : pm.findRecordingsAsView(baseQuery)) {
        if (view.getStartDate().after(now)) {
          addBean(view);
        }
      }
    } catch (ParticipationManagementDatabaseException e) {
      chuck(e);
    }

  }

  /**
   * Add the processing status to the recording view.
   * Depending on the outcome return an appropriate component.
   */
  private RecordingView addProcessingStatus(RecordingView recording, Date now) {
    if (recording.getStatus() == Recording.RecordingStatus.OPTED_OUT && !recording.requiresRecording()) {
      recording.setProcessingStatus("not_recorded");
    } else if (recording.getEventId().isNone()) {
      if (recording.getEndDate().before(now)) {
        recording.setProcessingStatus("failed");
      } else {
        recording.setProcessingStatus("upcoming");
      }
    } else {
      checkRecordingStatus(recording);
    }

    return recording;
  }

  /**
   * Check the status of the given recording with the workflow service.
   * Depending on the outcome return an appropriate component.
   */
  private RecordingView checkRecordingStatus(RecordingView recording) {
    final String mediaPackageId;
    final WorkflowInstance wi;
    final WorkflowSet workflowInstances;
    final Long eventId = recording.getEventId().get();

    // First check if event is being recorded
    Date now = new Date();

    if (recording.getStartDate().before(now) && recording.getEndDate().after(now)) {
      recording.setProcessingStatus("capturing");
      return recording;
    }

    // If event has completed check workflows
    try {
      log.debug(format("Query workflow service for event '%s' %s",
              recording.getTitle(),
              eventId));
      mediaPackageId = schedulerService.getMediaPackageId(eventId);
      workflowInstances = workflowServiceUtils.getSvc().getWorkflowInstances(
                new WorkflowQuery().withMediaPackage(mediaPackageId));
    } catch (WorkflowDatabaseException e) {
      log.error("Worflow database error: couldn't get status for {}: {}", eventId, e.getMessage());
      recording.setProcessingStatus("error");
      return recording;
    } catch (NotFoundException e) {
      log.error("Workflow not found: couldn't get status for {}: {}", eventId, e.getMessage());
      recording.setProcessingStatus("error");
      return recording;
    } catch (SchedulerException e) {
      log.error("Scheduler can't find event {}: {}", eventId, e.getMessage());
      recording.setProcessingStatus("error");
      return recording;
    }

    recording.seMediaPackageId(Option.some(mediaPackageId));

    if (workflowInstances.size() > 0) {
      wi = workflowInstances.getItems()[0];
      recording.setWorkflowId(Option.some(wi.getId()));

      // Users may still pause a running workflow in Opencast
      if (WorkflowServiceUtils.isPaused(wi)) {
        recording.setProcessingStatus("paused");
      } else if (WorkflowServiceUtils.isRunning(wi)) {
        recording.setProcessingStatus("processing");
      } else if (WorkflowServiceUtils.isSucceeded(wi)) {
        // The associated workflow succeeded so the recording is maybe ready to be edited.
        log.debug("\tInitial workflow succeeded. Checking for subsequent workflows:");

        if (workflowInstances.size() == 1) {
          log.debug("\tNone found");
          if (isAwaitingEditing(mediaPackageId)) {
            recording.setProcessingStatus("edit");
          } else {
            recording.setProcessingStatus("published");
          }
        } else {
          log.debug(format("\t%d found", workflowInstances.size()));
          final WorkflowInstance newWi = workflowInstances.getItems()[(int) (workflowInstances.size() - 1)];
          if (WorkflowServiceUtils.isPaused(newWi)) {
            recording.setProcessingStatus("paused");
          } else if (WorkflowServiceUtils.isRunning(newWi)) {
            recording.setProcessingStatus("processing");
          } else if (WorkflowServiceUtils.isSucceeded(newWi)) {
            if (this.teacherPm.getWorkflowRetract().get().equals(newWi.getTemplate())) {
              recording.setProcessingStatus("unpublished");
            } else if (isAwaitingEditing(mediaPackageId)) {
              recording.setProcessingStatus("edit");
            } else {
              recording.setProcessingStatus("published");
            }
          } else {
            recording.setProcessingStatus("error");
          }
          recording.setWorkflowId(Option.some(newWi.getId()));
        }
      } else {
        recording.setProcessingStatus("error");
      }
    }

    return recording;
  }

  private boolean isAwaitingEditing(String identifier) {
    try {
      // check if edit comment has been resolved
      List<EventComment> comments = eventCommentService.getComments(identifier);
      for (EventComment comment : comments) {
        if (EventComment.REASON_NEEDS_CUTTING.equalsIgnoreCase(comment.getReason())
                && !comment.isResolvedStatus()) {
          return true;
        }
      }
    } catch (EventCommentException e) {
      log.error("Can't get comments for event {}: {}", identifier, e.getMessage());
    }

    return false;
  }
}
