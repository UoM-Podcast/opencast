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

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.replay;
import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Collections.nil;
//import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

//import org.opencastproject.kernel.mail.EmailAddress;
import org.opencastproject.messages.MailService;
//import org.opencastproject.messages.MessageSignature;
import org.opencastproject.messages.TemplateType;
import org.opencastproject.pm.api.Action;
import org.opencastproject.pm.api.CaptureAgent;
import org.opencastproject.pm.api.Course;
import org.opencastproject.pm.api.Course.EmailStatus;
import org.opencastproject.pm.api.EmailSender;
import org.opencastproject.pm.api.Message;
import org.opencastproject.pm.api.Person;
import org.opencastproject.pm.api.PersonType;
import org.opencastproject.pm.api.Recording;
import org.opencastproject.pm.api.Recording.RecordingInput;
import org.opencastproject.pm.api.Recording.ReviewStatus;
import org.opencastproject.pm.api.Room;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.User;
import org.opencastproject.util.IoSupport;

import org.easymock.EasyMock;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

/** This is not a real unit test but a usage demo. */
@Ignore
public class AbstractEmailSenderServiceTest {

  @Test
  public void testSendEmail() throws Exception {
    final MailService mailService = new MailService();
    OrganizationDirectoryService organizationDirectoryService = EasyMock
            .createNiceMock(OrganizationDirectoryService.class);
    List<Organization> organizations = new ArrayList<Organization>();
    organizations.add(new DefaultOrganization());
    EasyMock.expect(organizationDirectoryService.getOrganizations()).andReturn(organizations).anyTimes();
    replay(organizationDirectoryService);
    mailService.setOrganizationDirectoryService(organizationDirectoryService);
    final ParticipationManagementDatabase db = createNiceMock(ParticipationManagementDatabase.class);
    replay(db);
    final OsgiEmailSenderService emailSenderService = new OsgiEmailSenderService();
    final EmailSender sender = new AbstractEmailSenderService() {

      @Override
      protected Mode getMode() {
        return new Mode.TestFileMode(System.getProperty("java.io.tmpdir"), -1, false);
      }

      @Override
      protected ParticipationManagementDatabase getDb() {
        return db;
      }

      @Override
      protected String renderInvitationBody(String template, TemplateType.Invitation.Data data) {
        return emailSenderService.renderInvitationBody(template, data);
      }

      @Override
      protected String getOptOutLink(String teacherId) {
        return "/pmm/optout?teacher=" + teacherId;
      }

      @Override
      protected MailService getMailService() {
        return mailService;
      }

    };

    List<Recording> recordings = list(recording(1), recording(2));
    HashSet<Course> courses = new HashSet<Course>();
    for (Recording r : recordings) {
      courses.add(r.getCourse().get());
    }

    sender.sendMessagesForRecordings(recordings,courses, message(), false);
  }

  private Recording recording(int nr) {
    final Room room = new Room("Room " + nr);
    return Recording.recording(
            "activity-" + nr,
            "Recording " + nr,
            false,
            list(new Person(2L, "Dr. Klaus", "ced@gmx.de", nil(PersonType.class)), new Person(3L, "Dr. Paul",
                    "ced@neopoly.de", nil(PersonType.class))), some(new Course((long) nr, "course-" + nr, "series",
                    "Course " + nr, "This is course " + nr, "courseKey-" + nr, null)), room, new Date(), new Date(), new Date(),
            list(new Person(4L, "Student 1", "ced@gmx.de", nil(PersonType.class))), nil(Message.class), some(1L),
            new CaptureAgent(room, "ca-" + nr, "SCREEN"), nil(Action.class), EmailStatus.UNSENT, ReviewStatus.UNCONFIRMED,
            some(new Date()), false, false, RecordingInput.SCREEN);
  }

  private Message message() {
    final User user = new JaxbUser("ced@gmx.de", null, "Admin", "ced@gmx.de", "test", new DefaultOrganization(),
            new HashSet<JaxbRole>());
    final Person admin = Person.fromUser(user);
//    final MessageSignature messageSignature = new MessageSignature(1L, "admin", user, admin.getEmailAddress(),
//            none(EmailAddress.class), "Send by admin", new Date());
    final String body = IoSupport.loadTxtFromClassPath("mail-template-invitation.ftl", getClass()).get();
    return null;
  //      Message(admin, new MessageTemplate(TemplateType.INVITATION.toString(), user, "Invitation", body, TemplateType.INVITATION,
    //        new Date(), messageSignature);
  }
}
