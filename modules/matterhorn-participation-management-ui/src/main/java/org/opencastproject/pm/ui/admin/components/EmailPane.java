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
package org.opencastproject.pm.ui.admin.components;

import static org.opencastproject.kernel.mail.EmailAddress.emailAddress;
import static org.opencastproject.pm.ui.common.util.UiUtil.hlayout;
import static org.opencastproject.pm.ui.common.util.UiUtil.tableStringColGen;
import static org.opencastproject.pm.ui.common.util.UiUtil.vlayout;
import static org.opencastproject.pm.ui.common.util.UiUtil.withMargin;
import static org.opencastproject.util.data.Arrays.array;

import org.opencastproject.messages.MessageSignature;
import org.opencastproject.messages.MessageTemplate;
import org.opencastproject.pm.api.Course;
import org.opencastproject.pm.api.Course.EmailStatus;
import org.opencastproject.pm.api.EmailSender;
import org.opencastproject.pm.api.Message;
import org.opencastproject.pm.api.Person;
import org.opencastproject.pm.api.Recording;
import org.opencastproject.pm.api.persistence.EmailView;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabaseException;
import org.opencastproject.pm.api.persistence.RecordingQuery;
import org.opencastproject.pm.ui.common.util.I18N;
import org.opencastproject.pm.ui.common.util.UiUtil;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityContext;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.data.Cell;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.VerticalLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmailPane extends CustomComponent {

  private static final Logger logger = LoggerFactory.getLogger(EmailPane.class);
  private Long nRecordingsSent = 0L;
  private Long nRecordingsUnsent = 0L;
  private Long nRecordingsFailed = 0L;
  private Long nCoursesSent = 0L;
  private Long nCoursesUnsent = 0L;
  private Long nCoursesFailed = 0L;
  private List<Recording> recordings;
  private List<Course> courses;

  private final Button sendMailButton;
  private final Button resendFailedMailButton;
  private final Label coursesDescriptionSent;
  private final Label coursesDescriptionUnsent;
  private final Label coursesDescriptionFailed;
  private final Label recordingsDescriptionSent;
  private final Label recordingsDescriptionUnsent;
  private final Label recordingsDescriptionFailed;
  private final Label error;
  private final Table table = new Table();
  private final EmailContainer emailContainer;
  private final ProgressIndicator indicator;
  private final HorizontalLayout buttonsContainer;
//  private final HorizontalLayout mailButtonsContainer;
  private final Cell<Option<ParticipationManagementDatabase>> pm;
  private final Cell<Option<EmailSender>> emailSenderService;
  private final SecurityService securityService;
  private final I18N i18n;

  private static final String COL_COURSE = "Course";
  private static final String COL_START = "Start Date";
  private static final String COL_STAFF = "Staff";
  private static final String COL_ROOM = "Room";
  private static final String COL_EMAIL_STATUS = "Email Status";
  private static final String COL_RECORDING_STATUS = "Recording Status";
  private static final String COL_RECORDINGS_AFFECTED = "Recordings Affected";

  private static final String[] COLS = array(COL_COURSE, COL_RECORDINGS_AFFECTED, COL_STAFF, COL_EMAIL_STATUS, COL_ROOM, COL_START);

  /**
   * Thread pool to run the background workers.
   */
  private final ExecutorService executorService = Executors.newCachedThreadPool();

  private String systemUserName;

  public EmailPane(final SecurityService securityService,
          final Cell<Option<ParticipationManagementDatabase>> pm, final Cell<Option<EmailSender>> emailSenderService,
          final String systemUserName, final I18N i18n) {
    this.securityService = securityService;
    this.pm = pm;
    this.emailSenderService = emailSenderService;
    this.systemUserName = systemUserName;
    this.i18n = i18n;

    sendMailButton = createSendMailButton();
    resendFailedMailButton = createResendFailedMailButton();
    indicator = new ProgressIndicator(new Float(0.0));
    indicator.setIndeterminate(true);
    indicator.setPollingInterval(500);
    indicator.setVisible(false);
    indicator.setEnabled(false);
    buttonsContainer = hlayout(sendMailButton, UiUtil.hspacer(), resendFailedMailButton, indicator);

    coursesDescriptionUnsent = UiUtil.label("", UiUtil.htmlLabel);
    coursesDescriptionUnsent.setCaption(i18n.s("tab.email.courses.description.unsent") + nCoursesUnsent);
    coursesDescriptionSent = UiUtil.label("", UiUtil.htmlLabel);
    coursesDescriptionSent.setCaption(i18n.s("tab.email.courses.description.sent") + nCoursesSent);
    coursesDescriptionFailed = UiUtil.label("", UiUtil.htmlLabel);
    coursesDescriptionFailed.setCaption(i18n.s("tab.email.courses.description.failed") + nCoursesFailed);
    recordingsDescriptionUnsent = UiUtil.label("", UiUtil.htmlLabel);
    recordingsDescriptionUnsent.setCaption(i18n.s("tab.email.recordings.description.unsent") + nRecordingsUnsent);
    recordingsDescriptionSent = UiUtil.label("", UiUtil.htmlLabel);
    recordingsDescriptionSent.setCaption(i18n.s("tab.email.recordings.description.sent") + nRecordingsSent);
    recordingsDescriptionFailed = UiUtil.label("", UiUtil.htmlLabel);
    recordingsDescriptionFailed.setCaption(i18n.s("tab.email.recordings.description.failed") + nRecordingsFailed);

    error = UiUtil.label("", UiUtil.htmlLabel);
    error.setVisible(false);

    VerticalLayout recordingDescriptions = vlayout(recordingsDescriptionUnsent, recordingsDescriptionSent, recordingsDescriptionFailed);
    VerticalLayout courseDescriptions = vlayout(coursesDescriptionUnsent, coursesDescriptionSent, coursesDescriptionFailed);
    setCompositionRoot(vlayout(withMargin, hlayout(recordingDescriptions, UiUtil.hspacer(), courseDescriptions), table, UiUtil.vspacer(), buttonsContainer, error));

    emailContainer = new EmailContainer(pm, EmailStatus.FAILED);
    table.setContainerDataSource(emailContainer, new ArrayList<String>());
    table.setWidth(100, Unit.PERCENTAGE);
    table.setCacheRate(4);
    table.addGeneratedColumn(COL_COURSE, tableStringColGen(new Function<EmailView, String>() {
      @Override
      public String apply(EmailView emailView) {
        return emailView.getCourse();
      }
    }));
    table.addGeneratedColumn(COL_RECORDINGS_AFFECTED, tableStringColGen(new Function<EmailView, String>() {
      @Override
      public String apply(EmailView emailView) {
        if (emailView.getNumberAffected() > 0) {
          return Integer.toString(emailView.getNumberAffected());
        } else {
          return i18n.s("tab.email.table.course.failed");
        }
      }
    }));
    table.addGeneratedColumn(COL_STAFF, new ColumnGenerator() {
      @Override
      public Object generateCell(Table t, final Object item, Object colId) {
        final EmailView emailView = (EmailView) item;
        VerticalLayout verticalLayout = new VerticalLayout();
        for (Person p : emailView.getPresenters()) {
          verticalLayout.addComponent(new Label(p.getName() + " (" + p.getEmail() + ")"));
        }
        return verticalLayout;
      }
    });
    table.addGeneratedColumn(COL_EMAIL_STATUS, tableStringColGen(new Function<EmailView, String>() {
      @Override
      public String apply(EmailView emailView) {
        return emailView.getEmailStatus().toString();
      }
    }));
    table.addGeneratedColumn(COL_ROOM, tableStringColGen(new Function<EmailView, String>() {
      @Override
      public String apply(EmailView emailView) {
        return emailView.getRoom();
      }
    }));
    table.addGeneratedColumn(COL_START, tableStringColGen(new Function<EmailView, String>() {
      @Override
      public String apply(EmailView emailView) {
        return emailView.getStartDate().toString();
      }
    }));
    update();
  }

  protected void invokeUIChange(Effect0 effect) {
    VaadinSession session = getSession();
    if (session != null) {
      session.lock();
    }
    try {
      effect.apply();
    } finally {
      if (session != null) {
        session.unlock();
      }
    }
  }

  /**
   * Create the button to send the mail on click. Overwrite to customize.
   */
  protected Button createSendMailButton() {
    return new Button(i18n.s("tab.email.button.send"), new Button.ClickListener() {
      @Override
      public void buttonClick(Button.ClickEvent event) {
        sendMail(EmailStatus.UNSENT);
      }
    });
  }

  /**
   * Create the button to resend failed mail on click. Overwrite to customize.
   */
  protected Button createResendFailedMailButton() {
    return new Button(i18n.s("tab.email.button.resend"), new Button.ClickListener() {
      @Override
      public void buttonClick(Button.ClickEvent event) {
        sendMail(EmailStatus.FAILED);
      }
    });
  }

  /**
   * Update the unsent/failed emails
   */
  public void update() {
    if (pm.get().isNone()) {
      error.setVisible(true);
      error.setCaption(i18n.s("tab.email.errors"));
      return;
    }

    // Create an indicator that makes you look busy
    indicator.setVisible(true);
    indicator.setEnabled(true);
    sendMailButton.setEnabled(false);
    resendFailedMailButton.setVisible(false);
    error.setVisible(false);
    error.setCaption("");

    if (securityService == null) {
      logger.warn("No security context available");
      return;
    }
    Organization org = securityService.getOrganization();
    if (org == null) {
      org = new DefaultOrganization();
      securityService.setOrganization(org);
    }
    final SecurityContext sctx = new SecurityContext(securityService, org, securityService.getUser());
    executorService.execute(new Runnable() {
      @Override
      public void run() {
        sctx.runInContext(new Effect0() {
          @Override
          protected void run() {
            try {
              nRecordingsUnsent = pm.get().get().countRecordingsByEmailState(EmailStatus.UNSENT);
              nRecordingsSent = pm.get().get().countRecordingsByEmailState(EmailStatus.SENT);
              nRecordingsFailed = pm.get().get().countRecordingsByEmailState(EmailStatus.FAILED);
            } catch (ParticipationManagementDatabaseException e) {
              logger.error("Unable to count recordings {}", e.getMessage());
              invokeUIChange(new Effect0() {
                @Override
                protected void run() {
                  error.setVisible(true);
                  error.setCaption(i18n.s("tab.email.error.recordings.db"));
                }
              });
              return;
            }

            try {
              nCoursesUnsent = pm.get().get().countCoursesByEmailState(EmailStatus.UNSENT);
              nCoursesSent = pm.get().get().countCoursesByEmailState(EmailStatus.SENT);
              nCoursesFailed = pm.get().get().countCoursesByEmailState(EmailStatus.FAILED);
            } catch (ParticipationManagementDatabaseException e) {
              logger.error("Unable to count courses {}", e.getMessage());
              invokeUIChange(new Effect0() {
                @Override
                protected void run() {
                  error.setVisible(true);
                  error.setCaption(i18n.s("tab.email.error.courses.db"));
                }
              });
              return;
            }

            invokeUIChange(new Effect0() {
              @Override
              protected void run() {
                recordingsDescriptionUnsent.setCaption(i18n.s("tab.email.recordings.description.unsent") + nRecordingsUnsent);
                recordingsDescriptionSent.setCaption(i18n.s("tab.email.recordings.description.sent") + nRecordingsSent);
                recordingsDescriptionFailed.setCaption(i18n.s("tab.email.recordings.description.failed") + nRecordingsFailed);
                coursesDescriptionUnsent.setCaption(i18n.s("tab.email.courses.description.unsent") + nCoursesUnsent);
                coursesDescriptionSent.setCaption(i18n.s("tab.email.courses.description.sent") + nCoursesSent);
                coursesDescriptionFailed.setCaption(i18n.s("tab.email.courses.description.failed") + nCoursesFailed);

                indicator.setEnabled(false);
                indicator.setVisible(false);
                sendMailButton.setEnabled(true);

                if (emailContainer.size() > 0) {
                  table.refreshRowCache();
                  table.setPageLength(Math.min(emailContainer.size(), 20));
                  table.setVisible(true);
                  resendFailedMailButton.setVisible(true);
                } else {
//                    descriptionFailed.setCaption("");
                  resendFailedMailButton.setVisible(false);
                  table.setVisible(false);
                  resendFailedMailButton.setVisible(false);
                }
              }
            });
          }
        });
      }
    });
  }

  /**
   * Create a default Message
   *
   * @return a default test message
   */
  private Message getDefaultMessage() {
    User user = SecurityUtil.createSystemUser(systemUserName, new DefaultOrganization());
    Person creator = Person.person("Lecture Capture Service", "lecture-capture@university.org");
    MessageSignature msgSign = MessageSignature.messageSignature("admin", user,
            emailAddress(creator.getEmail(), creator.getName()), "Send by admin");

    String body = IoSupport.loadTxtFromClassPath("mail-template-invitation.ftl", getClass()).get();

    // Create an ad-hoc version of the templates as we are reading it from the
    // class path every time
    MessageTemplate tmpl = new MessageTemplate("invitation", user,
            "Lecture capture opt-out reminder - please ensure that your lecture recording options are correctly set",
            body).createAdHocCopy();
    return new Message(creator, tmpl, msgSign);
  }

  /**
   * Send mails for each recordings in the list
   */
  private void sendMail(final EmailStatus status) {
    if (pm.get().isNone()) {
      error.setVisible(true);
      error.setCaption(i18n.s("tab.email.errors"));
      return;
    }

    // Create an indicator that makes you look busy
    indicator.setVisible(true);
    indicator.setEnabled(true);
    sendMailButton.setEnabled(false);
    resendFailedMailButton.setEnabled(false);
    error.setVisible(false);
    error.setCaption("");

    if (securityService == null) {
      logger.warn("No security context available");
      return;
    }
    Organization org = securityService.getOrganization();
    if (org == null) {
      org = new DefaultOrganization();
      securityService.setOrganization(org);
    }
    final SecurityContext sctx = new SecurityContext(securityService, org, securityService.getUser());
    final Organization contextOrg = org;
    executorService.execute(new Runnable() {
      @Override
      public void run() {
        sctx.runInContext(new Effect0() {
          @Override
          protected void run() {
            logger.info("Sending emails. Organization is " + contextOrg);
            try {
              recordings = pm
                      .get()
                      .get()
                      .findRecordings(
                              RecordingQuery.createWithoutDeleted().withEmailStatus(status));
            } catch (ParticipationManagementDatabaseException e) {
              logger.error("Unable to find unsent recordings! {}", e.getMessage());
              invokeUIChange(new Effect0() {
                @Override
                protected void run() {
                  error.setVisible(true);
                  error.setCaption(i18n.s("tab.email.error.db"));
                  indicator.setEnabled(false);
                  indicator.setVisible(false);
                  sendMailButton.setEnabled(true);
                  resendFailedMailButton.setEnabled(false);
                }
              });
              return;
            }

            try {
              courses = pm.get().get().findCoursesByEmailState(EmailStatus.valueOf(status.toString()));
            } catch (ParticipationManagementDatabaseException e) {
              logger.error("Unable to find unsent courses! {}", e.getMessage());
              invokeUIChange(new Effect0() {
                @Override
                protected void run() {
                  error.setVisible(true);
                  error.setCaption(i18n.s("tab.email.error.db"));
                  indicator.setEnabled(false);
                  indicator.setVisible(false);
                  sendMailButton.setEnabled(true);
                  resendFailedMailButton.setEnabled(false);
                }
              });
              return;
            }

            // Resent email status to unsent
            if (status != EmailStatus.UNSENT) {
              for (Recording r : recordings) {
                r.setEmailStatus(EmailStatus.UNSENT);
              }

              for (Course c : courses) {
                c.setEmailStatus(EmailStatus.UNSENT);
              }
            }

            for (EmailSender es : emailSenderService.get()) {
              es.sendMessagesForRecordings(recordings, courses, getDefaultMessage(), true);
            }

            invokeUIChange(new Effect0() {
              @Override
              protected void run() {
                update();
              }
            });
          }
        });
      }
    });
  }
}
