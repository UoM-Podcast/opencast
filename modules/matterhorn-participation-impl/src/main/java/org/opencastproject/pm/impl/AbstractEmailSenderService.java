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

package org.opencastproject.pm.impl;

import static java.lang.String.format;
import static org.opencastproject.util.EqualsUtil.eq;
import static org.opencastproject.util.EqualsUtil.hash;
import static org.opencastproject.util.IoSupport.withFile;
import static org.opencastproject.util.data.Collections.unique;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Prelude.unexhaustiveMatch;

import org.opencastproject.kernel.mail.EmailAddress;
import org.opencastproject.messages.Mail;
import org.opencastproject.messages.MailService;
import org.opencastproject.messages.MailServiceException;
import org.opencastproject.messages.TemplateType;
import org.opencastproject.pm.api.Course;
import org.opencastproject.pm.api.Course.EmailStatus;
import org.opencastproject.pm.api.EmailSender;
import org.opencastproject.pm.api.Error;
import org.opencastproject.pm.api.Message;
import org.opencastproject.pm.api.Person;
import org.opencastproject.pm.api.Recording;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabaseException;
import org.opencastproject.pm.api.persistence.RecordingQuery;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Effect2;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Base implementation of {@link org.opencastproject.pm.api.EmailSender}. Free from any template rendering. */
public abstract class AbstractEmailSenderService implements EmailSender {
  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(AbstractEmailSenderService.class);

  /** Return the run mode. */
  protected abstract Mode getMode();

  protected abstract ParticipationManagementDatabase getDb();

  protected abstract MailService getMailService();

  /**
   * Render an invitation email body for <code>recipient</code> concerning a list of recordings based on
   * <code>message</code>.
   *
   * @see TemplateType.Invitation
   */
  protected abstract String renderInvitationBody(String template, TemplateType.Invitation.Data data);

  protected abstract String getOptOutLink(String teacherId);

  @Override
  public void sendMessagesForRecordings(final Collection<Recording> recordings, final Collection<Course> courses,
          Message message, boolean store) {

    // persist Message for this round of sending
    if (getMode().isUpdateRecordings() && store) {
      try {
        // get object with id set
        message = getDb().updateMessage(message);
      } catch (ParticipationManagementDatabaseException e) {
        logger.error(format("Unable to store message %s: %s", message, e.getMessage()));
      }
    }

    // dispatch by message type
    final TemplateType.Type type = message.getTemplate().getType().getType();
    switch (type) {
      case INVITATION:
        sendInvitationMessages(recordings, courses, message, store);
        break;
      default:
        logger.error("Unsupported message type" + type);
    }
  }

  @Override
  public String renderInvitationBody(Message message, Collection<Recording> recordingsOfStaffMember,
          Collection<Course> coursesOfStaffMember, Person staffMember) {
    // Create a unique list of courses from unsent recordings and unsent courses
    Set<Course> allCourses = new HashSet<Course>();
    allCourses.addAll(coursesOfStaffMember);
    allCourses.addAll(extractCoursesUnique(recordingsOfStaffMember));

    final List<TemplateType.Invitation.Module> modules = mlist(allCourses).map(toModule).value();
    return renderInvitationBody(message.getTemplate().getBody(), new TemplateType.Invitation.Data(
            staffMember.getName(), getOptOutLink(staffMember.getEmail()), modules));
  }

//  @Override
//  public void sendErrorMessage(Message message) {
//    try {
//        // send mail
//        List<EmailAddress> to = new ArrayList<EmailAddress>();
//        List<EmailAddress> cc = new ArrayList<EmailAddress>();
//        // FIXME: UoM hack to CC sender (podcast-service)
//        to.add(message.getSignature().getSender());
//
//        sendMail(new Mail(message.getSignature().getSender(), message.getSignature().getReplyTo(), to,
//                Option.option(cc),
//                message.getTemplate().getSubject(), message.getTemplate().getBody()));
//      } catch (Exception e) {
//        logger.error(format("Error while sending email to %s: %s", message.getSignature().getSender(), e));
//        message.addError(new Error("email", "error while sending email to " + message.getSignature().getSender(), ExceptionUtils
//                .getStackTrace(e)));
//      }
//  }
//
  /** Extract all courses from the given recordings and ensure that there are no duplicates. */
  public Collection<Course> extractCoursesUnique(Collection<Recording> recordings) {
    return unique(mlist(recordings).bind(Recording.getCourse).value(), Course.getId);
  }

  /** Send message of type "invitation". */
  public void sendInvitationMessages(final Collection<Recording> recordings, final Collection<Course> courses,
          final Message message, final boolean store) {
    final Map<EmailStatus, Course.EmailStatus> emailStatusMap = new HashMap();
    emailStatusMap.put(EmailStatus.FAILED, Course.EmailStatus.FAILED);
    emailStatusMap.put(EmailStatus.UNSENT, Course.EmailStatus.UNSENT);
    emailStatusMap.put(EmailStatus.SENT, Course.EmailStatus.SENT);
    final Multimap<PersonKey, Recording> recordingsByStaffMember = groupRecordingsByStaffMember(recordings);
    final Multimap<PersonKey, Course> coursesByStaffMember = groupCoursesByStaffMember(courses);
    // Create a unique set of recipients from unsent recordings and unsent course notifications
    Set<PersonKey> recipients = new HashSet<PersonKey>();
    recipients.addAll(recordingsByStaffMember.keySet());
    recipients.addAll(coursesByStaffMember.keySet());

    int i = 0;
    int limit = -1; // -1 no limit
    EmailStatus emailState = EmailStatus.UNSENT;
    logger.info(format("######## Start sending %d invitations using %s", recipients.size(), message.getTemplate()));

    if (getMode().getType() != Mode.Type.SMTP) {
      limit = ((Mode.TestMode)getMode()).getLimitRecordings();
    }

    // send one email per staff member
    for (PersonKey personKey : recipients) {
      Person staffMember = personKey.getPerson();
      final Collection<Recording> recordingsOfStaffMember = recordingsByStaffMember.get(personKey);
      final Collection<Course> coursesOfStaffMember = coursesByStaffMember.get(personKey);

      logger.debug(format("Preparing invitation %d of %d, for %s", i, recordings.size(), staffMember.getEmail()));
      // Render body and set it temporary to the template
      String body = renderInvitationBody(message,
              recordingsOfStaffMember,
              coursesOfStaffMember,
              staffMember);
      if (message.isInsertSignature())
        body = body.concat("\r\n").concat(message.getSignature().getSignature());

      try {
        // send mail
        List<EmailAddress> to = new ArrayList<EmailAddress>();
        List<EmailAddress> cc = new ArrayList<EmailAddress>();
        to.add(getRecipient(staffMember));

        // FIXME: UoM hack to CC sender (podcast-service)
        if (true) {
          cc.add(message.getSignature().getSender());
        }
        sendMail(new Mail(message.getSignature().getSender(), message.getSignature().getReplyTo(), to,
                Option.option(cc),
                message.getTemplate().getSubject(), body));
        emailState = EmailStatus.SENT;
      } catch (Exception e) {
        emailState = EmailStatus.FAILED;
        logger.error(format("Error while sending email to %s: %s", staffMember.getEmail(), e));
        message.addError(new Error("email", "error while sending email to " + staffMember.getEmail(), ExceptionUtils
                .getStackTrace(e)));
      }

      if (getMode().isUpdateRecordings() && store) {
        logger.info("Update {} recordings in DB, email status set to {}", recordingsOfStaffMember.size(), emailState.toString());

        for (Recording recording : recordingsOfStaffMember) {
          Recording dbRecording;

          // To keep track of the emailStatus update our copy of the recording
          if (recording.getEmailStatus() != EmailStatus.FAILED) {
            recording.setEmailStatus(emailState);
          }

          try {
            // Update the latest version of the recording as the reviewstatus may have been modified
            // Current JPA version does not support partial updates
            dbRecording = getDb().getRecording(recording.getId().get());
            dbRecording.setEmailStatus(recording.getEmailStatus());
            // Only add message if not present
            Boolean messagePresent = false;
            for (Message m : dbRecording.getMessages()) {
              if (m.getId() != null && m.getId().equals(message.getId())) {
                messagePresent = true;
              }
            }
            if (!messagePresent) {
              dbRecording.addMessage(message);
            }
            dbRecording.setReviewDate(Option.some(new Date()));
            getDb().updateRecording(dbRecording);
          } catch (NotFoundException e) {
            logger.error(format("Unable to update recording %s: %s", recording, e.getMessage()));
          } catch (ParticipationManagementDatabaseException e) {
            logger.error(format("Unable to update recording %s: %s", recording, e.getMessage()));
          }
        }

        logger.info("Update {} courses in DB, email status set to {}", coursesOfStaffMember.size(), emailState.toString());

        for (Course course : coursesOfStaffMember) {
          if (course.getEmailStatus() != Course.EmailStatus.FAILED) {
            course.setEmailStatus(emailStatusMap.get(emailState));
          }
          try {
            getDb().updateCourse(course);
          } catch (ParticipationManagementDatabaseException e) {
            logger.error(format("Unable to update course %s: %s", course, e.getMessage()));
          }
        }
      }
      i++;

      if (limit - i == 0) {
        logger.info(format("Test Emails limited to %d", limit));
        break;
      }
    }
    logger.info(format("Finished sending of %d invitations using %s ########", recipients.size(), message.getTemplate()));
  }

  /** Transport. */
  private void sendMail(final Mail mail) throws MailServiceException {
    switch (getMode().getType()) {
      case SMTP:
        logger.info(format("Sending invitation email to %s", mail.getRecipients().get(0)));
        getMailService().send(mail);
        break;
      case TEST_SMTP:
        logger.info(format("Sending test invitation email to %s", mail.getRecipients().get(0)));
        getMailService().send(mail);
        break;
      case TEST_FILE:
        final File dir = new File(((Mode.TestFileMode) getMode()).getDirectory());
        if (!dir.isDirectory()) {
          logger.error(format("%s does not exist or is not a directory", dir));
        } else {
          final File out = new File(dir, asFileName(UUID.randomUUID().toString() + "-"
                  + mail.getRecipients().get(0).toString())
                  + ".txt");
          if (out.exists()) {
            logger.warn(format("%s already exists", out));
          }
          logger.info(format("Writing test email to %s", out));
          withFile(out, new Effect2<OutputStream, File>() {
            @Override
            protected void run(OutputStream out, File file) {
              final PrintWriter writer = new PrintWriter(out);
              writer.print(mail.toString());
              writer.flush();
            }
          });
        }
        break;
      default:
        unexhaustiveMatch();
    }
  }

  private String asFileName(String s) {
    return s.replaceAll("\\W", "_");
  }

  /** Get the recipient in dependency of the current run mode. */
  private EmailAddress getRecipient(Person staff) {
    switch (getMode().getType()) {
      case SMTP:
        return staff.getEmailAddress();
      case TEST_SMTP:
        return EmailAddress.emailAddress(((Mode.TestSmtpMode) getMode()).getRecipient(), staff.getName());
      case TEST_FILE:
        return staff.getEmailAddress();
      default:
        return unexhaustiveMatch();
    }
  }

  private final Function<Course, TemplateType.Invitation.Module> toModule = new Function<Course, TemplateType.Invitation.Module>() {
    @Override
    public TemplateType.Invitation.Module apply(Course course) {
      int lecturesChanged = 0;
      try {
        lecturesChanged = (int)getDb().countCourseRecordingsByEmailState(course, EmailStatus.UNSENT);
      } catch (ParticipationManagementDatabaseException ex) {
       logger.error("Can't get course's unset recordings: {}", ex.getMessage());
      }
      return new TemplateType.Invitation.Module(
              course.getName(),
              course.getDescription(),
              lecturesChanged,
              course.getRequirements().contains(Course.REQUIREMENT_RECORD),
              course.getEmailStatus() == Course.EmailStatus.UNSENT);
    }
  };

  /** Group a list of recordings by staff member. */
  private Multimap<PersonKey, Recording> groupRecordingsByStaffMember(Collection<Recording> recordings) {
    final Multimap<PersonKey, Recording> m = ArrayListMultimap.create();
    for (Recording r : recordings) {
      for (Person p : r.getStaff()) {
        m.put(new PersonKey(p), r);
      }
    }
    return m;
  }

  /** Group a list of courses by staff member. */
  private Multimap<PersonKey, Course> groupCoursesByStaffMember(Collection<Course> courses) {
    final Multimap<PersonKey, Course> m = ArrayListMultimap.create();

    for (Course c : courses) {
      RecordingQuery query = RecordingQuery.createWithoutDeleted().withCourse(c);
      Collection<Recording> courseRecordings;
      Set<Person> courseStaff = new HashSet<Person>();

      try {
        courseRecordings = getDb().findRecordings(query);

        for (Recording r : courseRecordings) {
          courseStaff.addAll(r.getStaff());
        }

        for (Person p : courseStaff) {
          m.put(new PersonKey(p), c);
        }

      } catch (ParticipationManagementDatabaseException ex) {
        logger.error("Could get course recordings: {}", ex.getMessage());
      }
    }
    return m;
  }

  /**
   * Define a stable key for {@link Person}. The current implementation of equals and hashCode is not usable since it is
   * unstable. This class may be removed as soon as Person provides correct implementations for equals and hashCode.
   */
  public static final class PersonKey {
    private final Person person;
    private final Option<Long> id;

    public PersonKey(Person person) {
      this.person = person;
      this.id = option(person.getId());
    }

    public Person getPerson() {
      return person;
    }

    @Override
    public int hashCode() {
      return id.hashCode();
    }

    @Override
    public boolean equals(Object that) {
      return (this == that) || (that instanceof PersonKey && eqFields((PersonKey) that));
    }

    private boolean eqFields(PersonKey that) {
      return eq(id, that.id);
    }
  }

  public abstract static class Mode {
    private Mode() {
    }

    public enum Type {
      SMTP, TEST_SMTP, TEST_FILE
    }

    public abstract Type getType();

    public abstract boolean isUpdateRecordings();

    public static final class SmtpMode extends Mode {
      @Override
      public Type getType() {
        return Type.SMTP;
      }

      @Override
      public boolean isUpdateRecordings() {
        return true;
      }

      @Override
      public int hashCode() {
        return 1;
      }

      @Override
      public boolean equals(Object that) {
        return (this == that) || (that instanceof SmtpMode);
      }

      @Override
      public String toString() {
        return getType().toString();
      }
    }

    private abstract static class TestMode extends Mode {
      private final int limitRecordings;
      private final boolean updateRecordings;

      protected TestMode(int limitRecordings, boolean updateRecordings) {
        this.limitRecordings = limitRecordings;
        this.updateRecordings = updateRecordings;
      }

      public int getLimitRecordings() {
        return limitRecordings;
      }

      @Override
      public boolean isUpdateRecordings() {
        return updateRecordings;
      }
    }

    public static final class TestSmtpMode extends TestMode {
      private final String recipient;

      public TestSmtpMode(String recipient, int limitRecordings, boolean updateRecordings) {
        super(limitRecordings, updateRecordings);
        this.recipient = recipient;
      }

      @Override
      public Type getType() {
        return Type.TEST_SMTP;
      }

      public String getRecipient() {
        return recipient;
      }

      @Override
      public int hashCode() {
        return hash(recipient, getLimitRecordings(), isUpdateRecordings());
      }

      @Override
      public boolean equals(Object that) {
        return (this == that) || (that instanceof TestSmtpMode && eqFields((TestSmtpMode) that));
      }

      private boolean eqFields(TestSmtpMode that) {
        return eq(this.recipient, that.recipient) && eq(this.getLimitRecordings(), that.getLimitRecordings())
                && eq(this.isUpdateRecordings(), that.isUpdateRecordings());
      }

      @Override
      public String toString() {
        return format("%s{recipient=%s,limit=%d,update=%s}", getType(), recipient, getLimitRecordings(),
                isUpdateRecordings());
      }
    }

    public static final class TestFileMode extends TestMode {
      private final String directory;

      public TestFileMode(String directory, int limitRecordings, boolean updateRecordings) {
        super(limitRecordings, updateRecordings);
        this.directory = directory;
      }

      @Override
      public Type getType() {
        return Type.TEST_FILE;
      }

      public String getDirectory() {
        return directory;
      }

      @Override
      public int hashCode() {
        return hash(directory, getLimitRecordings(), isUpdateRecordings());
      }

      @Override
      public boolean equals(Object that) {
        return (this == that) || (that instanceof TestFileMode && eqFields((TestFileMode) that));
      }

      private boolean eqFields(TestFileMode that) {
        return eq(this.directory, that.directory) && eq(this.getLimitRecordings(), that.getLimitRecordings())
                && eq(this.isUpdateRecordings(), that.isUpdateRecordings());
      }

      @Override
      public String toString() {
        return format("%s{directory=%s,limit=%d,update=%s}", getType(), directory, getLimitRecordings(),
                isUpdateRecordings());
      }
    }
  }

}
