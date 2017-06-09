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
import static org.opencastproject.pm.ui.teacher.PastRecordingsView.RETRACT_WORKFLOW_ID;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.pm.api.Person;
import org.opencastproject.pm.api.Recording;
import org.opencastproject.pm.api.Recording.ReviewStatus;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabaseException;
import org.opencastproject.pm.api.persistence.RecordingQuery;
import org.opencastproject.pm.api.persistence.RecordingView;
import org.opencastproject.security.api.UnauthorizedException;
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
  private final ParticipationManagementDatabase pm;
  private final RecordingQuery baseQuery;
  private WorkflowServices workflowServices;

  public RecordingContainer(final ParticipationManagementDatabase pm, final Option<WorkflowServices> workflowServices, Person teacher, Boolean future) {
    super(RecordingView.class);
    this.pm = pm;
    if (workflowServices.isSome()) {
      this.workflowServices = workflowServices.get();
    }
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
    final WorkflowInstance wi;
    try {
      log.debug(format("Query workflow service for event '%s' %s",
              recording.getTitle(),
              recording.getEventId().get()));
      wi = workflowServices.getSvc().getWorkflowById(recording.getEventId().get());
    } catch (WorkflowDatabaseException e) {
      log.error("Worflow database error: couldn't get status for {}: {}", recording.getEventId().get(), e.getMessage());
      recording.setProcessingStatus("error");
      return recording;
    } catch (NotFoundException e) {
      log.error("Workflow not found: couldn't get status for {}: {}", recording.getEventId().get(), e.getMessage());
      recording.setProcessingStatus("error");
      return recording;
    } catch (UnauthorizedException e) {
      log.error("Not authorized: couldn't get status for {}: {}", recording.getEventId().get(), e.getMessage());
      recording.setProcessingStatus("error");
      return recording;
    }

    recording.setWorkflowId(Option.some(wi.getId()));

    if (WorkflowServices.isPaused(wi)) {
      if (wi.getCurrentOperation() != null && ("schedule".equals(wi.getCurrentOperation().getTemplate()))) {
        recording.setProcessingStatus("upcoming");
      } else if (wi.getCurrentOperation() != null && ("trim".equals(wi.getCurrentOperation().getTemplate())
              || "editor".equals(wi.getCurrentOperation().getTemplate()))) {
        recording.setProcessingStatus("edit");
      } else {
        recording.setProcessingStatus("paused");
      }
    } else if (WorkflowServices.isRunning(wi)) {
      recording.setProcessingStatus("processing");
    } else if (WorkflowServices.isSucceeded(wi)) {
      // The associated workflow succeeded so the recording is maybe ready to be edited.
      // Check if there are any other workflows processing the media package of the recording.
      final WorkflowSet workflowInstances;
      try {
        log.debug("\tInitial workflow succeeded. Querying for subsequent workflows:");
        workflowInstances = workflowServices.getSvc().getWorkflowInstances(
                new WorkflowQuery()
                .withMediaPackage(wi.getMediaPackage().getIdentifier().compact()));
      } catch (WorkflowDatabaseException ex) {
        log.error("An error occurred while querying the workflow service", ex);
        recording.setProcessingStatus("error");
        return recording;
      }
      if (workflowInstances.size() == 0) {
        log.debug("\tNone found");
        recording.setProcessingStatus("published");
      } else {
        log.debug(format("\t%d found", workflowInstances.size()));
        final WorkflowInstance newWi = workflowInstances.getItems()[(int) (workflowInstances.size() - 1)];
        if (WorkflowServices.isPaused(newWi)) {
          if ("trim".equals(newWi.getCurrentOperation().getTemplate())
                  || "editor".equals(newWi.getCurrentOperation().getTemplate())) {
            recording.setProcessingStatus("edit");
          } else {
            recording.setProcessingStatus("paused");
          }
        } else if (WorkflowServices.isRunning(newWi)) {
          recording.setProcessingStatus("processing");
        } else if (WorkflowServices.isSucceeded(newWi)) {
          if (RETRACT_WORKFLOW_ID.equals(newWi.getTemplate())) {
            recording.setProcessingStatus("unpublished");
          } else {
            recording.setProcessingStatus("published");
          }
        }
        recording.setWorkflowId(Option.some(newWi.getId()));
      }
    } else {
      recording.setProcessingStatus("error");
    }

    return recording;
  }
}
