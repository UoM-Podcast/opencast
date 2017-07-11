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
import static org.opencastproject.pm.ui.common.util.UiUtil.label;
import static org.opencastproject.pm.ui.common.util.UiUtil.vlayout;
import static org.opencastproject.pm.ui.common.util.UiUtil.withMargin;
import static org.opencastproject.util.data.Arrays.array;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.archive.api.ArchiveException;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.pm.api.Person;
import org.opencastproject.pm.api.Recording;
import org.opencastproject.pm.api.persistence.RecordingView;
import org.opencastproject.pm.ui.common.util.I18N;
import org.opencastproject.pm.ui.common.util.UiUtil;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.util.SecurityContext;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.util.data.Option;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowInstance;

import com.vaadin.server.Page;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.UI;

import org.apache.commons.lang.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.vaadin.dialogs.ConfirmDialog;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PastRecordingsView extends RecordingsView {
  private static final Logger log = LoggerFactory.getLogger(PastRecordingsView.class);

  private static final String COL_REC_STATUS = "rec_status";
  private static final String COL_ACTION_EDIT = "edit";
  private static final String COL_ACTIONS = "actions";

  private enum Action {
    EDIT, PAUSE, RETRACT
  };

  // FIXME: these values should be a in a properties file
  private static final String EDITOR_DOMAIN = "http://oc-admin.localdomain";

  private static final String[] COLS = array(COL_REC_STATUS, COL_TITLE, COL_START_DATE, COL_TIME, COL_ROOM,
          COL_ACTION_EDIT, COL_ACTIONS);

  private final TeacherPm teacherPm;
  private final WorkflowServiceUtils workflowServiceUtils;
  private final ArchiveServiceUtils archiveServiceUtils;
  private final SecurityService securityService;
  private final Date now = new Date();

  private final ExecutorService executorService = Executors.newCachedThreadPool();

  public PastRecordingsView(final I18N i18n,
          final String email,
          final TeacherPm teacherPm,
          final WorkflowServiceUtils workflowServices) {
    super(i18n, i18n.s("past.title"), i18n.s("past.text"),
            email, teacherPm, Option.some(workflowServices), false);

    log.debug("Create PastRecordingsView");
    this.teacherPm = teacherPm;
    this.workflowServiceUtils = workflowServices;
    this.securityService = teacherPm.getSecurityService();
    this.archiveServiceUtils = teacherPm.getArchiveServiceUtils();

    content.addComponents(vlayout(withMargin, table));

    table.addGeneratedColumn(COL_REC_STATUS, new ColumnGenerator() {
      @Override
      public Object generateCell(Table t, Object item, Object colId) {
        RecordingView recording = (RecordingView) item;
        String required = recording.requiresRecording() ? " " + i18n.s("table.rec_status.required") : "";
        Label recStatusLabel = new Label(
                i18n.s("table.rec_status." + recording.getProcessingStatus()) + required);
        recStatusLabel.setPrimaryStyleName("pm_" + recording.getProcessingStatus());
        return recStatusLabel;
      }
    });
    table.setColumnWidth(COL_REC_STATUS, COL_WIDTH_MEDIUM);
    table.addGeneratedColumn(COL_ACTION_EDIT, new ColumnGenerator() {
      @Override
      public Object generateCell(Table t, final Object item, Object colId) {
        final RecordingView recording = (RecordingView) item;
        final String required = recording.requiresRecording() ? i18n.s("confirm.text.required") : "";

        if ("published".equals(recording.getProcessingStatus())) {
          return new Button(i18n.s("table.action.edit"), new ClickListener() {
            @Override
            public void buttonClick(final ClickEvent event) {
              // Disable edit button to prevent multiple clicks.
              event.getButton().setEnabled(true);
              ConfirmDialog.show(UI.getCurrent(), i18n.s("confirm.text.edit"), required,
                      i18n.s("confirm.affirmative"), i18n.s("confirm.negative"), new ConfirmDialog.Listener() {
                @Override
                public void onClose(ConfirmDialog dialog) {
                  if (dialog.isConfirmed()) {
                    if (!runAction(Action.EDIT, recording)) {
                      event.getButton().setEnabled(true);
                    }
                  } else {
                    event.getButton().setEnabled(true);
                  }
                }
              });
            }
          });
        } else if ("edit".equals(recording.getProcessingStatus()) && recording.getWorkflowId().isSome()) {
          return label(format("<a target=\"_blank\" href=\"%s/admin-ng/index.html#/events/events/%s/tools/editor\">%s</a>",
                  teacherPm.getEditServer().get(), recording.getMediaPackageId().get(), i18n.s("table.action.edit.link")), UiUtil.htmlLabel);
        }
        return new Label("");
      }
    });
    table.setColumnWidth(COL_ACTION_EDIT, COL_WIDTH_MEDIUM);
    table.addGeneratedColumn(COL_ACTIONS, new ColumnGenerator() {
      @Override
      public Object generateCell(Table t, final Object item, Object colId) {
        final RecordingView recording = (RecordingView) item;
        final String required = recording.requiresRecording() ? i18n.s("confirm.text.required") : "";

        if ("published".equals(recording.getProcessingStatus())) {
          return new Button(i18n.s("table.action.retract"), new ClickListener() {
            @Override
            public void buttonClick(final ClickEvent event) {
              // Disable edit button to prevent multiple clicks.
              event.getButton().setEnabled(true);
              ConfirmDialog.show(UI.getCurrent(), i18n.s("confirm.text.retract"), required,
                      i18n.s("confirm.affirmative"), i18n.s("confirm.negative"), new ConfirmDialog.Listener() {
                @Override
                public void onClose(ConfirmDialog dialog) {
                  if (dialog.isConfirmed()) {
                    if (!runAction(Action.RETRACT, recording)) {
                      event.getButton().setEnabled(true);
                    }
                  } else {
                    event.getButton().setEnabled(true);
                  }
                }
              });
            }
          });
        } else if ("processing".equals(recording.getProcessingStatus())) {
          return new Button(i18n.s("table.action.pause"), new ClickListener() {
            @Override
            public void buttonClick(final ClickEvent event) {
              // Disable edit button to prevent multiple clicks.
              event.getButton().setEnabled(true);
              ConfirmDialog.show(UI.getCurrent(), i18n.s("confirm.text.pause"), required,
                      i18n.s("confirm.affirmative"), i18n.s("confirm.negative"), new ConfirmDialog.Listener() {
               @Override
                public void onClose(ConfirmDialog dialog) {
                  if (dialog.isConfirmed()) {
                    if (!runAction(Action.PAUSE, recording)) {
                      event.getButton().setEnabled(true);
                    }
                  } else {
                    event.getButton().setEnabled(true);
                  }
                }
              });
            }
          });
        } else {
          return new Label("");
        }
      }
    });
    table.setColumnWidth(COL_ACTIONS, COL_WIDTH_MEDIUM);

    table.setVisibleColumns(COLS);
    table.setSortAscending(true);
    super.setTableColHeaders(i18n, COLS);
  }

  private boolean runAction(final Action action, final RecordingView recording) {
    final WorkflowInstance wi;

    try {
      wi = workflowServiceUtils.getSvc().getWorkflowById(recording.getWorkflowId().get());
    } catch (WorkflowDatabaseException e) {
      showError(format("Unable to process action on recording %s", recording.getTitle()));
      return false;
    } catch (NotFoundException e) {
      showError(format("The recording with id %s cannot be found", recording.getTitle()));
      return false;
    } catch (UnauthorizedException e) {
      showError(format("You're not authorized to access recording %s", recording.getTitle()));
      return false;
    }

    if (action != Action.PAUSE && WorkflowServiceUtils.notFinished(wi)) {
      Notification.show(format("Recording %s busy", recording.getTitle()),
              "There is still a workflow running on that recording. Please try again later.", Type.WARNING_MESSAGE);
      return false;
    } else {
      switch (action) {
        case EDIT:
          return editRecording(recording, wi);
        case PAUSE:
          return pauseWorkflow(recording, wi);
        case RETRACT:
          return retractRecording(recording, wi);
        default:
          log.error("Invalid action {}", action.toString());
          return false;
      }
    }
  }

  private boolean editRecording(final RecordingView recording, final WorkflowInstance wi) {
    showResponse(format("Edit recording %s", recording.getTitle()),
            "You'll receive an email once the recording is ready to be edited");
    final SecurityContext sctx = new SecurityContext(securityService, securityService.getOrganization(),
            securityService.getUser());
    executorService.execute(new Runnable() {
      @Override
      public void run() {
        sctx.runInContext(new Effect0() {
          @Override
          protected void run() {
            String mpId = wi.getMediaPackage().getIdentifier().toString();

            log.info("Action: User {} initiated editing of mediapackage {}", email, mpId);
            log.info("\tStart retract workflow on media package " + mpId);

            try {
              MediaPackage mp = archiveServiceUtils.getMediaPackage(mpId);

              if (workflowServiceUtils.runWorkflow(teacherPm.getWorkflowRetract().get(), mp).isSome()) {
                final Map<String, String> workflowProperties = teacherPm.getWorkflowEditConfig().get();
                Option<Recording> rec = getRecording(recording.getId());
                if (rec.isNone()) {
                  Notification.show(format("The recording %s could not be found", recording.getTitle()), Type.ERROR_MESSAGE);
                  return;
                }
                workflowProperties.put(teacherPm.getEmailProperty().get(), getStaffMailList(rec.get()));

                log.info("\tStart default workflow with edit/review on media package " + mpId);
                // Get updated version of mediapackge
                MediaPackage retractedMp = archiveServiceUtils.getMediaPackage(mpId);
                workflowServiceUtils.startWorkflow(teacherPm.getWorkflowEdit().get(), retractedMp, workflowProperties);
              }
            } catch (NotFoundException e) {
              log.error("Mediapackage not found: {}", e.getMessage());
            } catch (UnauthorizedException e) {
              log.error("Does not have right to access series: {}", e.getMessage());
            } catch (ArchiveException e) {
              log.error("Archive return an internal errror: {}", e.getMessage());
            } finally {
              showError(format("The recording %s could not set for editing", recording.getTitle()));
            }
          }

          private Option<Recording> getRecording(long recordingId) {
            try {
              Recording rec = pmDb.getRecording(recordingId);
              return some(rec);
            } catch (NotFoundException e) {
              return none();
            } catch (Exception e) {
              return chuck(e);
            }
          }

          private String getStaffMailList(Recording rec) {
            String emailAddresses = "";
            List<String> staffMailList = new ArrayList<>();
            for (Person p : rec.getStaff()) {
              if (StringUtils.isNotBlank(p.getEmail())) {
                staffMailList.add(p.getEmail());
              }
            }

            if (staffMailList.size() > 0) {
              emailAddresses = StringUtils.join(staffMailList, ",");
            }

            return emailAddresses;
          }
        });
      }
    });
    return true;
  }

  private boolean retractRecording(final RecordingView recording, final WorkflowInstance wi) {
    showResponse(format("Removing recording %s from the Video Portal", recording.getTitle()),
            "It will take few minutes before the video is no longer available");
    final SecurityContext sctx = new SecurityContext(securityService, securityService.getOrganization(),
            securityService.getUser());
    executorService.execute(new Runnable() {
      @Override
      public void run() {
        sctx.runInContext(new Effect0() {
          @Override
          protected void run() {
            String mpId = wi.getMediaPackage().getIdentifier().toString();

            log.info("Action: User {} initited retraction of mediapackage {}", email, mpId);
            log.info("\tStart retract workflow on media package " + mpId);

            try {
              MediaPackage mp = archiveServiceUtils.getMediaPackage(mpId);
              final Map<String, String> workflowProperties = new HashMap<>();
              workflowServiceUtils.startWorkflow(teacherPm.getWorkflowRetract().get(), mp, workflowProperties);
            } catch (NotFoundException e) {
              log.error("Mediapackage not found: {}", e.getMessage());
            } catch (UnauthorizedException e) {
              log.error("Does not have right to access series: {}", e.getMessage());
            } catch (ArchiveException e) {
              log.error("Archive return an internal errror: {}", e.getMessage());
            } finally {
              showError(format("The recording %s could not be retracted", recording.getTitle()));
            }
          }
        });
      }
    });
    return true;
  }

  private boolean pauseWorkflow(final RecordingView recording, final WorkflowInstance wi) {
    showResponse("Attempting to pausing processing of recording " + recording.getTitle(),
            "It may not be possible to pause processing before publication, please check Video Portal");
    log.info("Action: User {} initited pausing of workflow {}", email, wi.getId());

    if (workflowServiceUtils.pauseWorkflow(wi.getId())) {
      return true;
    } else {
      showError(format("Processing of recording %s could not be paused", recording.getTitle()));
      return false;
    }
  }

  private void showResponse(final String caption, final String description) {
    Notification response = new Notification(caption, description, Type.HUMANIZED_MESSAGE);
    response.setDelayMsec(2000);
    response.show(Page.getCurrent());
  }

  private void showError(final String error) {
    Notification.show(error, Type.ERROR_MESSAGE);
  }
}
