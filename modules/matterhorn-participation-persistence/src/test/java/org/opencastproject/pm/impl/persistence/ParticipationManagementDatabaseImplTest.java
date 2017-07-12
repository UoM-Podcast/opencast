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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opencastproject.pm.api.Period.period;
import static org.opencastproject.pm.api.Person.person;
//import static org.opencastproject.pm.impl.persistence.ParticipationManagementPersistenceUtil.partialize;
//import static org.opencastproject.pm.impl.persistence.ParticipationManagementPersistenceUtil.quarterBeginnings;
import static org.opencastproject.util.data.Collections.nil;
//import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.none;
//import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.persistence.PersistenceUtil.newTestEntityManagerFactory;

import org.opencastproject.kernel.mail.EmailAddress;
import org.opencastproject.messages.MessageSignature;
import org.opencastproject.messages.MessageTemplate;
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
import org.opencastproject.pm.api.Recording.RecordingInput;
import org.opencastproject.pm.api.Recording.ReviewStatus;
import org.opencastproject.pm.api.Room;
import org.opencastproject.pm.api.Synchronization;
import org.opencastproject.pm.api.SynchronizedRecording;
import org.opencastproject.pm.api.SynchronizedRecording.SynchronizationStatus;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabaseException;
import org.opencastproject.pm.api.persistence.RecordingQuery;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.util.NotFoundException;
//import org.opencastproject.util.data.Function2;
import org.opencastproject.util.data.Option;

import org.easymock.EasyMock;
import org.joda.time.DateTime;
//import org.joda.time.Partial;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import javax.persistence.EntityManager;

public class ParticipationManagementDatabaseImplTest {
  private ParticipationManagementDatabaseImpl pmDB = null;

  @Before
  public void setUp() throws Exception {
    // Set up the annotation service
    User user = new JaxbUser("test", null, "Test User", "test@test.com", "test", new DefaultOrganization(),
            new HashSet<JaxbRole>());

    UserDirectoryService userDirectoryService = EasyMock.createNiceMock(UserDirectoryService.class);
    EasyMock.expect(userDirectoryService.loadUser((String) EasyMock.anyObject())).andReturn(user).anyTimes();
    EasyMock.replay(userDirectoryService);

    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.replay(securityService);

    pmDB = new ParticipationManagementDatabaseImpl();
    pmDB.setUserDirectoryService(userDirectoryService);
    pmDB.setSecurityService(securityService);
    pmDB.setEntityManagerFactory(newTestEntityManagerFactory(ParticipationManagementDatabaseImpl.PERSISTENCE_UNIT));
  }

  @After
  public void tearDown() throws Exception {
    pmDB.deactivate(null);
  }

  @Test
  public void testCreateAndFindSynchronisation() {
    Recording testRecording = createRecording();
    Date currentDate = new Date();
    Synchronization sync = new Synchronization(currentDate);
    sync.addError(new Error("database", "test", "stacktrace"));
    SynchronizedRecording badSyncRecording = new SynchronizedRecording(SynchronizationStatus.NEW, testRecording);
    sync.addSynchronizedRecording(badSyncRecording);

    try {
      sync = pmDB.storeLatestSynchronization(sync);
      fail("Able to save a synchronization entity with unknown recording");
    } catch (ParticipationManagementDatabaseException e) {
      Assert.assertNotNull(e);
    }

    try {
      Recording updateRecording = pmDB.updateRecording(testRecording);
      sync.removeSynchronizedRecording(badSyncRecording);
      sync.addSynchronizedRecording(new SynchronizedRecording(SynchronizationStatus.NEW, updateRecording));
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to store the recordings: " + e.getMessage());
    }

    try {
      sync = pmDB.storeLatestSynchronization(sync);
    } catch (ParticipationManagementDatabaseException e) {
      fail("Not able to save a synchronization entity: " + e.getMessage());
    }

    try {
      Synchronization lastSync = pmDB.getLastSynchronization();
      assertTrue(sync.equals(lastSync));
    } catch (ParticipationManagementDatabaseException e) {
      fail("Not able to get the last synchronization: " + e.getMessage());
    }
  }

  @Test
  public void testCRUMessage() {
    EntityManager em = pmDB.emf.createEntityManager();

    User user = new JaxbUser("george@test.com", null, "George", "george@test.com", "test", new DefaultOrganization(),
            new HashSet<JaxbRole>());

    Person admin = Person.fromUser(user);
    Person help = person("Frank", "frank@test.com");
    DateTime today = new DateTime().withHourOfDay(12);
    List<Error> errors = new ArrayList<Error>();
    errors.add(new Error("Test", "Test description", "Test source"));
    MessageTemplate tmplInvitation = new MessageTemplate("Invitation", user, "test", "test");
    MessageSignature messageSignature = MessageSignature.messageSignature("TEst", user,
            EmailAddress.emailAddress(user.getEmail(), user.getName()), "Test signature");
    Message msg1 = new Message(today.toDate(), admin, tmplInvitation, messageSignature, new ArrayList<Error>());
    Message msg2 = new Message(today.minusMonths(1).toDate(), help, tmplInvitation, messageSignature,
            new ArrayList<Error>());
    Message msg3 = new Message(today.plusHours(1).toDate(), admin, tmplInvitation, messageSignature, errors);

    // Create
    try {
      msg1 = pmDB.updateMessage(msg1);
      msg2 = pmDB.updateMessage(msg2);
      msg3 = pmDB.updateMessage(msg3);
    } catch (ParticipationManagementDatabaseException e) {
      fail("Not able to save a message: " + e.getMessage());
    }

    // Read
    try {
      assertEquals(3, pmDB.countMessagesSent());
      assertEquals(2, pmDB.countDailyMessagesSent());
      assertEquals(1, pmDB.countMessageErrors());
//      assertTrue(ParticipationManagementPersistenceUtil.find(option(msg1.getId()), em, MessageDto.class).isSome());
//      assertTrue(ParticipationManagementPersistenceUtil.find(option(msg2.getId()), em, MessageDto.class).isSome());
//      assertTrue(ParticipationManagementPersistenceUtil.find(option(msg3.getId()), em, MessageDto.class).isSome());
    } catch (ParticipationManagementDatabaseException e) {
      fail("Not able to get the message: " + e.getMessage());
    }

    // Update
//    try {
//      msg2.addError(errors.get(0));
//      msg2 = pmDB.updateMessage(msg2);
//      assertEquals(2, pmDB.countMessageErrors());
//      Option<MessageDto> foundMsg = ParticipationManagementPersistenceUtil.find(option(msg2.getId()), em,
//              MessageDto.class);
//      if (foundMsg.isSome()) {
//        assertEquals(1, foundMsg.get().getErrors().size());
//        assertEquals(msg2.getErrors().get(0), foundMsg.get().getErrors().get(0).toError());
//      } else {
//        fail("Update message not found!");
//      }
//    } catch (ParticipationManagementDatabaseException e) {
//      fail("Not able to update a message signature entity: " + e.getMessage());
//    }
  }

  @Test
  public void testCRUDBuilding() {
    Building building1 = new Building("Main building");
    Building building2 = new Building("Library");
    Building building3 = new Building("Research-center");

    // Create
    try {
      building1 = pmDB.updateBuilding(building1);
      building2 = pmDB.updateBuilding(building2);
      building3 = pmDB.updateBuilding(building3);
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to update the buildings: " + e.getMessage());
    }

    // Read
    try {
      List<Building> buildings = pmDB.getBuildings();
      assertEquals(3, buildings.size());
      Assert.assertTrue(buildings.contains(building1) && buildings.contains(building2) && buildings.contains(building3));
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to get the buildings: " + e.getMessage());
    }

    // Update
    try {
      building1.setName("Center building");
      building1 = pmDB.updateBuilding(building1);
      List<Building> buildings = pmDB.getBuildings();
      assertEquals(3, buildings.size());
      Assert.assertTrue(buildings.contains(building1) && buildings.contains(building2) && buildings.contains(building3));
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to get the buildings: " + e.getMessage());
    }

    // Delete
    try {
      pmDB.deleteBuilding(building1.getId());
      List<Building> buildings = pmDB.getBuildings();
      assertEquals(2, buildings.size());
      Assert.assertTrue(!buildings.contains(building1) && buildings.contains(building2)
              && buildings.contains(building3));
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to delete a person: " + e.getMessage());
    } catch (NotFoundException e) {
      fail("Unable to found a person: " + e.getMessage());
    }
  }

  @Test
  public void testCRUDPersons() {
    Person person1 = person("Dr. Carter", "carter@institution.com");
    Person person2 = person("Mr. Lewis", "lewis@institution.com");
    Person person3 = person("Professor Jackson", "jackson@institution.com");

    // Create
    try {
      person1 = pmDB.updatePerson(person1);
      person2 = pmDB.updatePerson(person2);
      person3 = pmDB.updatePerson(person3);
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to persist the persons: " + e.getMessage());
    }

    // Read
    try {
      List<Person> persons = pmDB.getInstructors();
      assertEquals(3, persons.size());
      Assert.assertTrue(persons.contains(person1) && persons.contains(person2) && persons.contains(person3));
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to get the persons: " + e.getMessage());
    }

    // Update
    try {
      person1.setName("Dr. Meyer");
      person1 = pmDB.updatePerson(person1);
      List<Person> persons = pmDB.getInstructors();
      assertEquals(3, persons.size());
      Assert.assertTrue(persons.contains(person1) && persons.contains(person2) && persons.contains(person3));
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to update the persons: " + e.getMessage());
    }

    // Delete
    try {
      pmDB.deletePerson(person1.getId());
      List<Person> persons = pmDB.getInstructors();
      assertEquals(2, persons.size());
      Assert.assertTrue(!persons.contains(person1) && persons.contains(person2) && persons.contains(person3));
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to delete a person: " + e.getMessage());
    } catch (NotFoundException e) {
      fail("Unable to found a person: " + e.getMessage());
    }
  }

  @Test
  public void testGetRecordingByEventId() {
    try {
      pmDB.getRecordingByEvent(4L);
      fail("Didn't throw not found exception");
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    } catch (Exception e) {
      fail("Unable to get the recordings: " + e.getMessage());
    }

    Recording recording = createRecording();
    try {
      pmDB.updateRecording(recording);
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to store the recordings: " + e.getMessage());
    }

    try {
      Recording r = pmDB.getRecordingByEvent(4L);
      Assert.assertNotNull(r);
    } catch (NotFoundException e) {
      fail("Did not found the recording!");
    } catch (Exception e) {
      fail("Unable to get the recordings: " + e.getMessage());
    }
  }

  @Test
  public void testFindRecordings() {
    try {
      List<Recording> recordings = pmDB.findRecordings(RecordingQuery.createWithoutDeleted());
      assertEquals(0, recordings.size());
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to get the recordings: " + e.getMessage());
    }

    Recording recording = createRecording();
    try {
      pmDB.updateRecording(recording);
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to store the recordings: " + e.getMessage());
    }

    try {
      List<Recording> recordings = pmDB.findRecordings(RecordingQuery.createWithoutDeleted());
      assertEquals(1, recordings.size());
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to get the recordings: " + e.getMessage());
    }

//    try {
//      RecordingQuery query = RecordingQuery.createWithoutDeleted().withCourse(recording.getCourse().get())
//              .withStartDateRange(recording.getStart()).withEndDateRange(recording.getStop()).withoutBlacklisted()
//              .withFullText("ula").withStaff(recording.getStaff().toArray(new Person[recording.getStaff().size()]))
//              // .withAssistedStudents()
//              .withParticipation(recording.getParticipation().toArray(new Person[recording.getParticipation().size()]));
//      List<Recording> recordings = pmDB.findRecordings(query);
//      assertEquals(1, recordings.size());
//    } catch (ParticipationManagementDatabaseException e) {
//      fail("Unable to get the recordings: " + e.getMessage());
//    }
  }

  @Ignore
  @Test
  public void testFindBlacklists() {
    try {
      List<Blacklist> blacklists = pmDB.findBlacklists(new Blacklistable[0]);
      assertEquals(0, blacklists.size());
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to get the blacklists: " + e.getMessage());
    }

    final Blacklist personBlacklist;
    final Blacklist personBlacklist2;
    final Blacklist roomBlacklist;
    final Blacklist roomBlacklist2;
    try {
      personBlacklist = pmDB.updateBlacklist(createPersonBlacklist());
      personBlacklist2 = pmDB.updateBlacklist(createPersonBlacklist2());
      roomBlacklist = pmDB.updateBlacklist(createRoomBlacklist());
      roomBlacklist2 = pmDB.updateBlacklist(createRoomBlacklist2());
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to store the blacklists: " + e.getMessage());
      throw new RuntimeException();
    }

    try {
      final List<Blacklist> blacklists = pmDB.findBlacklists(new Blacklistable[0]);
      assertEquals(4, blacklists.size());
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to get the blacklists: " + e.getMessage());
    }

    try {
      List<Blacklist> blacklists = pmDB.findBlacklists(personBlacklist.getBlacklisted());
      assertEquals(1, blacklists.size());
      Blacklist blacklist = blacklists.get(0);
      assertEquals(personBlacklist, blacklist);
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to get the blacklists: " + e.getMessage());
    }

    try {
      List<Blacklist> blacklists = pmDB.findBlacklists(roomBlacklist.getBlacklisted());
      assertEquals(1, blacklists.size());
      Blacklist blacklist = blacklists.get(0);
      assertEquals(roomBlacklist, blacklist);
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to get the blacklists: " + e.getMessage());
    }

    try {
      List<Blacklist> blacklists = pmDB
              .findBlacklists(roomBlacklist.getBlacklisted(), personBlacklist.getBlacklisted());
      assertEquals(2, blacklists.size());
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to get the blacklists: " + e.getMessage());
    }

    try {
      List<Blacklist> blacklists = pmDB.findBlacklists(roomBlacklist.getBlacklisted(), roomBlacklist2.getBlacklisted());
      assertEquals(2, blacklists.size());
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to get the blacklists: " + e.getMessage());
    }

    try {
      List<Blacklist> blacklists = pmDB.findBlacklists(personBlacklist.getBlacklisted(),
              personBlacklist2.getBlacklisted());
      assertEquals(2, blacklists.size());
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to get the blacklists: " + e.getMessage());
    }

    try {
      List<Blacklist> blacklists = pmDB.findBlacklists(personBlacklist.getBlacklisted(),
              personBlacklist2.getBlacklisted(), roomBlacklist.getBlacklisted());
      assertEquals(3, blacklists.size());
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to get the blacklists: " + e.getMessage());
    }

    try {
      List<Blacklist> blacklists = pmDB.findBlacklists(Person.TYPE, Option.<Integer> none(), Option.<Integer> none(),
              Option.some("Lukas"), Option.<String> none(), Option.<String> none());
      assertEquals(1, blacklists.size());
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to get the blacklists: " + e.getMessage());
    }

    try {
      List<Blacklist> blacklists = pmDB.findBlacklists(Person.TYPE, Option.<Integer> none(), Option.<Integer> none(),
              Option.<String> none(), Option.some("a"), Option.<String> none());
      assertEquals(2, blacklists.size());
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to get the blacklists: " + e.getMessage());
    }
  }

  @Test
  public void testCountAndDeleteBlacklist() {
    Room auditorium = new Room("Auditorium");
    Person lukas = person("Lukas", "lukas@university.org");
    Person peter = person("Peter", "peter@university.org");
    PersonType teacher = new PersonType("teacher", "computer science");
    PersonType student = new PersonType("student", "computer science");
    PersonType assistant = new PersonType("assistant", "computer science");
    lukas.addType(teacher);
    peter.addType(student);
    peter.addType(assistant);
    List<Period> periods = new ArrayList<Period>();
    periods.add(period(new DateTime().withHourOfDay(10).withMinuteOfHour(40).toDate(), new DateTime().withHourOfDay(11)
            .withMinuteOfHour(40).toDate()));
    periods.add(period(new DateTime().plusDays(3).toDate(), new DateTime().plusDays(5).toDate()));

    Blacklist personBlacklist = new Blacklist(lukas, periods);
    Blacklist roomBlacklist = new Blacklist(auditorium, periods);

    Recording rec1 = createRecording();
    rec1.setStart(new DateTime().withHourOfDay(11).withMinuteOfHour(30).toDate());
    rec1.setStop(new DateTime().withHourOfDay(12).withMinuteOfHour(30).toDate());
    Recording rec2 = createRecording();
    rec2.setStart(new DateTime().withHourOfDay(10).withMinuteOfHour(30).toDate());
    rec2.setStop(new DateTime().withHourOfDay(11).withMinuteOfHour(30).toDate());

    rec1.setRoom(auditorium);
    rec2.setActivityId("a2");
    rec2.addStaffMember(lukas);

    // Store entities
    try {
      rec1 = pmDB.updateRecording(rec1);
      rec2 = pmDB.updateRecording(rec2);
      personBlacklist = pmDB.updateBlacklist(personBlacklist);
      roomBlacklist = pmDB.updateBlacklist(roomBlacklist);
      assertEquals(2, pmDB.countDailyBlacklistedRecordings());
      assertEquals(2, pmDB.countWeeklyBlacklistedRecordings());
      assertEquals(4, pmDB.countPeriods());
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to store the blacklists: " + e.getMessage());
    }

    // Update
    try {
      DateTime today = new DateTime().withHourOfDay(12);
      int dayInWeek = today.getDayOfWeek() == 7 ? 1 : today.getDayOfWeek() + 1;
      rec1 = pmDB.getRecording(rec1.getId().get());
      rec1.setStart(today.withDayOfWeek(dayInWeek).toDate());
      rec1.setStop(today.withDayOfWeek(dayInWeek).plusMinutes(60).toDate());
      rec1 = pmDB.updateRecording(rec1);
      roomBlacklist.setPeriods(new ArrayList<Period>());
      roomBlacklist.addPeriod(period(today.withDayOfWeek(dayInWeek).minusMinutes(20).toDate(),
              today.withDayOfWeek(dayInWeek).plusMinutes(20).toDate()));
      personBlacklist.setPeriods(new ArrayList<Period>());
      personBlacklist.addPeriod(period(new DateTime().plusDays(3).toDate(), new DateTime().plusDays(5).toDate()));
      personBlacklist = pmDB.updateBlacklist(personBlacklist);
      roomBlacklist = pmDB.updateBlacklist(roomBlacklist);
      assertEquals(0, pmDB.countDailyBlacklistedRecordings());
      assertEquals(1, pmDB.countWeeklyBlacklistedRecordings());
      assertEquals(2, pmDB.countPeriods());
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to update the blacklists: " + e.getMessage());
    } catch (NotFoundException e) {
      fail("Unable to find the blacklists: " + e.getMessage());
    }

    // Delete
    try {
      pmDB.deleteBlacklist(personBlacklist.getId(), Option.<Long> none());
      assertEquals(0, pmDB.countDailyBlacklistedRecordings());
      assertEquals(1, pmDB.countPeriods());
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to delete the blacklists: " + e.getMessage());
    } catch (NotFoundException e) {
      fail("Unable to find the blacklists: " + e.getMessage());
    }
  }

  @Test
  public void testUniqueConstraintBlacklist() {
    final Blacklist personBlacklist;
    try {
      personBlacklist = pmDB.updateBlacklist(createPersonBlacklist());
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to store the blacklists: " + e.getMessage());
      throw new RuntimeException();
    }

    List<Period> periods = new ArrayList<Period>();
    periods.add(period(new DateTime().plusDays(2).toDate(), new DateTime().plusDays(9).toDate()));

    Blacklist personBlacklist2 = new Blacklist(personBlacklist.getBlacklisted(), periods);
    try {
      pmDB.updateBlacklist(personBlacklist2);
      assertEquals(1, pmDB.countPeriods());
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to update the blacklists: " + e.getMessage());
      throw new RuntimeException();
    }
  }

  @Test
  public void testUpdateRecording() {
    Recording recording = createRecording();
    try {
      recording = pmDB.updateRecording(recording);
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to store the recording: " + e.getMessage());
    }

    try {
      Recording rec = pmDB.getRecording(recording.getId().get());
      assertEquals(recording, rec);
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to get the recording: " + e.getMessage());
    } catch (NotFoundException e) {
      fail("Recording not found: " + e.getMessage());
    }

    recording.setBlacklisted(true);
    recording.setEventId(25L);
    recording.addAction(new Action("new", "nothing"));

    try {
      Recording rec = pmDB.updateRecording(recording);
      assertEquals(recording, rec);
      Assert.assertTrue(rec.isBlacklisted());
      assertEquals(25L, recording.getEventId().get(), 0L);
      assertEquals(1, rec.getAction().size());
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to update the recording: " + e.getMessage());
    }
  }

  @Test
  public void testReviewRecording() {
    Recording recording = createRecording();

    try {
      recording = pmDB.updateRecording(recording);
      recording = pmDB.reviewRecording(recording.getId().get(), Recording.ReviewStatus.CONFIRMED);
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to update the recording: " + e.getMessage());
    } catch (NotFoundException e) {
      fail("Unable to find the recording: " + e.getMessage());
    }

    assertEquals(Recording.ReviewStatus.CONFIRMED, recording.getReviewStatus());
    assertEquals(Recording.RecordingStatus.READY, recording.getRecordingStatus(false));

    try {
      recording = pmDB.reviewRecording(recording.getId().get(), Recording.ReviewStatus.OPTED_OUT);
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to update the recording: " + e.getMessage());
    } catch (NotFoundException e) {
      fail("Unable to find the recording: " + e.getMessage());
      e.printStackTrace();
    }

    assertEquals(Recording.ReviewStatus.OPTED_OUT, recording.getReviewStatus());
    assertEquals(Recording.RecordingStatus.OPTED_OUT, recording.getRecordingStatus(false));
  }

  @Test
  public void testDeleteRecording() {
    Recording recording = createRecording();
    try {
      recording = pmDB.updateRecording(recording);
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to store the recording: " + e.getMessage());
    }

    try {
      List<Recording> recordings = pmDB.findRecordings(RecordingQuery.createWithoutDeleted());
      assertEquals(1, recordings.size());
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to get the recordings: " + e.getMessage());
    }

    try {
      pmDB.deleteRecording(recording.getId().get());
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to delete the recording: " + e.getMessage());
    } catch (NotFoundException e) {
      fail("Recording not found: " + e.getMessage());
    }

    try {
      Recording recording2 = pmDB.getRecording(recording.getId().get());
      Assert.assertTrue(recording2.isDeleted());
    } catch (Exception e) {
      fail("Unable to delete the recording: " + e.getMessage());
    }

    try {
      List<Recording> recordings = pmDB.findRecordings(RecordingQuery.createWithoutDeleted());
      assertEquals(0, recordings.size());
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to get the recordings: " + e.getMessage());
    }
  }

  @Test
  public void testgetPeriodsByType() {
    try {
      pmDB.updateBlacklist(createPersonBlacklist());
      pmDB.updateBlacklist(createPersonBlacklist2());
      pmDB.updateBlacklist(createRoomBlacklist());
      pmDB.updateBlacklist(createRoomBlacklist2());
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to store the blacklists: " + e.getMessage());
    }

    try {
      List<String> purposes = pmDB.getPurposesByType("unknown");
      assertEquals(0, purposes.size());
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to get unknown periods: " + e.getMessage());
    }

    try {
      List<String> purposes = pmDB.getPurposesByType(Person.TYPE);
      assertEquals(1, purposes.size());
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to get person periods: " + e.getMessage());
    }

    try {
      List<String> purposes = pmDB.getPurposesByType(Room.TYPE);
      assertEquals(2, purposes.size());
    } catch (ParticipationManagementDatabaseException e) {
      fail("Unable to get room periods: " + e.getMessage());
    }
  }

//  private DateTime getDateForQuarter() {
//    final DateTime today = new DateTime().withTimeAtStartOfDay();
//    final Partial partialToday = partialize(today);
//    final DateTime quarterBeginning = mlist(quarterBeginnings).foldl(quarterBeginnings.get(0),
//            new Function2<Partial, Partial, Partial>() {
//              @Override
//              public Partial apply(Partial sum, Partial quarterBeginning) {
//                return partialToday.isAfter(quarterBeginning) ? quarterBeginning : sum;
//              }
//            }).toDateTime(today);
//
//    return quarterBeginning;
//  }

  private Blacklist createRoomBlacklist() {
    Room room = new Room("Auditorium");
    List<Period> periods = new ArrayList<Period>();
    periods.add(period(new DateTime().plusHours(2).toDate(), new DateTime().plusHours(4).toDate(), "a", "none"));
    periods.add(period(new DateTime().plusDays(1).toDate(), new DateTime().plusDays(2).toDate(), "b", "none"));
    return new Blacklist(room, periods);
  }

  private Blacklist createRoomBlacklist2() {
    Room room = new Room("Videoroom");
    List<Period> periods = new ArrayList<Period>();
    periods.add(period(new DateTime().plusHours(5).toDate(), new DateTime().plusHours(9).toDate(), "a", "none"));
    periods.add(period(new DateTime().plusDays(2).toDate(), new DateTime().plusDays(4).toDate(), "b", "none"));
    return new Blacklist(room, periods);
  }

  private Blacklist createPersonBlacklist() {
    Person person = person("Lukas", "lukas@university.org");
    person.addType(new PersonType("ceo", "nothing"));
    person.addType(new PersonType("employee", "everything"));
    List<Period> periods = new ArrayList<Period>();
    periods.add(period(new DateTime().plusHours(5).toDate(), new DateTime().plusHours(6).toDate(), "a", "none"));
    periods.add(period(new DateTime().plusDays(3).toDate(), new DateTime().plusDays(5).toDate()));
    return new Blacklist(person, periods);
  }

  private Blacklist createPersonBlacklist2() {
    Person person = person("Peter", "peter@university.org");
    person.addType(new PersonType("ceo", "nothing"));
    person.addType(new PersonType("employee", "everything"));
    List<Period> periods = new ArrayList<Period>();
    periods.add(period(new DateTime().plusHours(4).toDate(), new DateTime().plusHours(5).toDate(), "a", "none"));
    periods.add(period(new DateTime().plusDays(9).toDate(), new DateTime().plusDays(12).toDate()));
    return new Blacklist(person, periods);
  }

  private Recording createRecording() {
    List<Person> staff = new ArrayList<Person>();
    List<PersonType> staffTypes = new ArrayList<PersonType>();
    staffTypes.add(new PersonType("group", "open door"));
    staff.add(person("Staff 1", "staff1@university.org", staffTypes));
    staff.add(person("Staff 2", "staff2@university.org", staffTypes));

    List<Person> students = new ArrayList<Person>();
    List<PersonType> studentTypes = new ArrayList<PersonType>();
    studentTypes.add(new PersonType("student", "learning"));
    students.add(person("Student 1", "student1@university.org", studentTypes));
    students.add(person("Student 2", "student2@university.org", studentTypes));

    Room room = new Room("Aula");
    CaptureAgent captureAgent = new CaptureAgent(room, CaptureAgent.getMhAgentIdFromRoom(room), "SCREEN");
    Course course = new Course("mathe", "uuid", "Math", "Simple course about algebra.", "ckey");

    return Recording.recording("activityId", "Test title", false, staff, some(course), room, new Date(), new DateTime()
            .plusHours(2).toDate(), new DateTime().plusHours(3).toDate(), students, nil(Message.class), some(4L),
            captureAgent, nil(Action.class), EmailStatus.UNSENT, ReviewStatus.UNCONFIRMED, none(Date.class), false, false, RecordingInput.SCREEN);
  }

  private Message createAndPersistMessage(Person creator, MessageTemplate msgTmpl, MessageSignature msgSig,
          Option<Date> creationDate) {
    Message message = new Message(creator, msgTmpl, msgSig);
    if (creationDate.isSome()) {
      message.setCreationDate(creationDate.get());
    }

    try {
      message = pmDB.updateMessage(message);
    } catch (ParticipationManagementDatabaseException e) {
      fail("Not able to persist message " + message + ": " + e.getMessage());
    }
    return message;
  }

//  @Test
//  public void testMessageCountQueries() {
//    User user = new JaxbUser("test@email.ch", null, "teste", "test@email.ch", "test", new DefaultOrganization(),
//            new HashSet<JaxbRole>());
//
//    Person person = Person.fromUser(user);
//    DateTime today = new DateTime();
//    DateTime startQuarter = getDateForQuarter().plusDays(1);
//
//    MessageTemplate tmplInvitation = new MessageTemplate("Invitation", user, "test", "test");
//    MessageSignature messageSignature = MessageSignature.messageSignature("Test", user,
//            EmailAddress.emailAddress(user.getEmail(), user.getName()), "Test signature");
//
//    Message msg = createAndPersistMessage(person, tmplInvitation, messageSignature, option(today.toDate()));
//    createAndPersistMessage(person, tmplInvitation, messageSignature, option(today.toDate()));
//    createAndPersistMessage(person, tmplInvitation, messageSignature, option(today.toDate()));
//    createAndPersistMessage(person, tmplInvitation, messageSignature, option(today.toDate()));
//    createAndPersistMessage(person, tmplInvitation, messageSignature, option(startQuarter.toDate()));
//    createAndPersistMessage(person, tmplInvitation, messageSignature, option(startQuarter.toDate()));
//
//    msg.addError(new Error("Test error", "Simple error for tests", "unit tests"));
//    try {
//      pmDB.updateMessage(msg);
//    } catch (ParticipationManagementDatabaseException e) {
//      fail("Error during message update: " + e.getMessage());
//    }
//
//    // Count all messages
//    try {
//      assertEquals(6, pmDB.countMessagesSent());
//    } catch (ParticipationManagementDatabaseException e) {
//      fail("Error during execution of message count query");
//    }
//
//    // Count daily messages
//    try {
//      int dailySum = 4;
//      if (today.getDayOfYear() == startQuarter.getDayOfYear())
//        dailySum += 2;
//      assertEquals(dailySum, pmDB.countDailyMessagesSent());
//    } catch (ParticipationManagementDatabaseException e) {
//      fail("Error during execution of message count query:" + e.getMessage());
//    }
//
//    // Count messages sent for this quarter
//    try {
//      assertTrue(2 <= pmDB.countQuarterMessagesSent());
//    } catch (ParticipationManagementDatabaseException e) {
//      fail("Error during execution of message count query:" + e.getMessage());
//    }
//
//    // Count messages with error
//    try {
//      assertEquals(1, pmDB.countMessageErrors());
//    } catch (ParticipationManagementDatabaseException e) {
//      fail("Error during execution of message count query:" + e.getMessage());
//    }
//  }

//  @Test
//  public void testMessageBySeries() {
//    Recording recording = createRecording();
//
//    User user = new JaxbUser("test@email.ch", null, "teste", "test@email.ch", "test", new DefaultOrganization(),
//            new HashSet<JaxbRole>());
//
//    Person person = Person.fromUser(user);
//    DateTime today = new DateTime();
//    DateTime startQuarter = getDateForQuarter().plusDays(1);
//
//    MessageTemplate tmplInvitation = new MessageTemplate("Invitation", user, "test", "test");
//    MessageSignature messageSignature = MessageSignature.messageSignature("Test", user,
//            EmailAddress.emailAddress(user.getEmail(), user.getName()), "Test signature");
//
//    Message msg = createAndPersistMessage(person, tmplInvitation, messageSignature, option(today.toDate()));
//    recording.addMessage(msg);
//    recording.addMessage(createAndPersistMessage(person, tmplInvitation, messageSignature, option(today.toDate())));
//    recording.addMessage(createAndPersistMessage(person, tmplInvitation, messageSignature, option(today.toDate())));
//    recording.addMessage(createAndPersistMessage(person, tmplInvitation, messageSignature, option(today.toDate())));
//    recording.addMessage(createAndPersistMessage(person, tmplInvitation, messageSignature,
//            option(startQuarter.toDate())));
//    recording.addMessage(createAndPersistMessage(person, tmplInvitation, messageSignature,
//            option(startQuarter.toDate())));
//
//    msg.addError(new Error("Test error", "Simple error for tests", "unit tests"));
//    try {
//      pmDB.updateRecording(recording);
//    } catch (ParticipationManagementDatabaseException e) {
//      fail("Error during recording update: " + e.getMessage());
//    }
//
//    try {
//      List<Message> messages = pmDB.getMessagesBySeriesId("uui");
//      assertEquals(0, messages.size());
//    } catch (ParticipationManagementDatabaseException e) {
//      fail("Error during execution of message by series query:" + e.getMessage());
//    }
//
//    try {
//      List<Message> messages = pmDB.getMessagesBySeriesId("uuid");
//      assertEquals(6, messages.size());
//    } catch (ParticipationManagementDatabaseException e) {
//      fail("Error during execution of message by series query:" + e.getMessage());
//    }
//  }

//  @Test
//  public void testRecordingCountQueries() {
//    DateTime today = new DateTime();
//    DateTime startQuarter = getDateForQuarter();
//    Recording rec1 = createRecording();
//    Recording rec2 = createRecording();
//    Recording rec3 = createRecording();
//    Recording rec4 = createRecording();
//    rec1.setStart(today.plusMinutes(1).toDate());
//    rec1.setStop(today.plusMinutes(2).toDate());
//    rec1.setReviewDate(some(today.toDate()));
//    rec2.setStart(today.plusMinutes(3).toDate());
//    rec2.setStop(today.plusMinutes(4).toDate());
//    rec2.setReviewDate(some(today.toDate()));
//    rec3.setStart(today.plusMinutes(5).toDate());
//    rec3.setStop(today.plusMinutes(6).toDate());
//    rec3.setReviewDate(some(today.toDate()));
//    rec4.setStart(startQuarter.plusMinutes(7).toDate());
//    rec4.setStop(startQuarter.plusMinutes(8).toDate());
//    rec4.setReviewDate(some(startQuarter.plusDays(1).toDate()));
//
//    try {
//      rec1 = pmDB.updateRecording(rec1);
//      rec2 = pmDB.updateRecording(rec2);
//      rec3 = pmDB.updateRecording(rec3);
//      rec4 = pmDB.updateRecording(rec4);
//    } catch (ParticipationManagementDatabaseException e) {
//      fail("Not able to save recording:" + e.getMessage());
//    }
//
//    // Count all messages
//    try {
//      assertEquals(4, pmDB.countTotalRecordings());
//    } catch (ParticipationManagementDatabaseException e) {
//      fail("Error during execution of recording count query");
//    }
//
//    // Count daily messages
//    try {
//      rec4 = pmDB.updateRecording(rec4);
//    } catch (ParticipationManagementDatabaseException e) {
//      fail("Not able to save recording:" + e.getMessage());
//    }
//
//    try {
//      int dailySum = 3;
//      if (today.getDayOfYear() == startQuarter.getDayOfYear())
//        dailySum++;
//      assertEquals(dailySum, pmDB.countDailyRecordings());
//    } catch (ParticipationManagementDatabaseException e) {
//      fail("Error during execution of recording count query");
//    }
//
//    // Count weekly recording
//    try {
//      int weeklySum = 3;
//      if (today.getWeekOfWeekyear() == startQuarter.getWeekOfWeekyear())
//        weeklySum++;
//      assertEquals(weeklySum, pmDB.countWeekRecordings());
//    } catch (ParticipationManagementDatabaseException e) {
//      fail("Error during execution of recording count query");
//    }
//
//    // Count confirmed recording
//    rec1.setReviewStatus(ReviewStatus.CONFIRMED);
//    rec4.setReviewStatus(ReviewStatus.CONFIRMED);
//    rec2.setReviewStatus(ReviewStatus.UNCONFIRMED);
//    rec3.setReviewStatus(ReviewStatus.OPTED_OUT);
//    try {
//      rec1 = pmDB.updateRecording(rec1);
//      rec4 = pmDB.updateRecording(rec4);
//      rec2 = pmDB.updateRecording(rec2);
//      rec3 = pmDB.updateRecording(rec3);
//    } catch (ParticipationManagementDatabaseException e) {
//      fail("Not able to save recording:" + e.getMessage());
//    }
//
//    try {
//      assertEquals(2, pmDB.countConfirmedResponses());
//    } catch (ParticipationManagementDatabaseException e) {
//      fail("Error during execution of recording count query");
//    }
//
//    // Count daily confirmed recording
//    try {
//      int dailySum = 1;
//      if (today.getDayOfYear() == startQuarter.getDayOfYear()
//              || startQuarter.plusDays(1).getDayOfYear() == today.getDayOfYear())
//        dailySum++;
//      assertEquals(dailySum, pmDB.countDailyConfirmedResponses());
//    } catch (ParticipationManagementDatabaseException e) {
//      fail("Error during execution of recording count query");
//    }
//
//    // Count confirmed recording for this quarter
//    try {
//      assertTrue(1 <= pmDB.countQuarterConfirmedResponses());
//    } catch (ParticipationManagementDatabaseException e) {
//      fail("Error during execution of recording count query");
//    }
//
//    // Count unconfirmed recording
//    try {
//      assertEquals(1, pmDB.countUnconfirmedResponses());
//    } catch (ParticipationManagementDatabaseException e) {
//      fail("Error during execution of recording count query");
//    }
//
//    // Count opted-out recording
//    try {
//      assertEquals(1, pmDB.countOptedOutResponses());
//    } catch (ParticipationManagementDatabaseException e) {
//      fail("Error during execution of recording count query");
//    }
//  }

  @Test
  public void testOptedOutCourse() {
    DateTime today = new DateTime();

    Recording rec1 = createRecording();
    Recording rec2 = createRecording();
    rec1.setStart(today.plusMinutes(1).toDate());
    rec1.setStop(today.plusMinutes(2).toDate());
    rec1.setReviewDate(some(today.toDate()));
    rec2.setStart(today.plusMinutes(3).toDate());
    rec2.setStop(today.plusMinutes(4).toDate());
    rec2.setReviewDate(some(today.toDate()));

    Course course = new Course("test");
    try {
      pmDB.updateCourse(course);
      rec1.setCourse(Option.<Course> some(course));
      rec2.setCourse(Option.<Course> some(course));
      pmDB.updateRecording(rec1);
      pmDB.updateRecording(rec2);
    } catch (ParticipationManagementDatabaseException e) {
      fail("Not able to save course or recording.");
    }

    course.setOptedOut(true);

    assertTrue(course.isOptedOut());
//    assertEquals(Recording.RecordingStatus.OPTED_OUT, rec1.getRecordingStatus());

    try {
      pmDB.updateCourse(course);
    } catch (ParticipationManagementDatabaseException e) {
      fail("Not able to save course or recording.");
    }

    RecordingQuery query = RecordingQuery.create();
    query.withCourse(course);
    try {
      List<Recording> recordings = pmDB.findRecordings(query);
      assertEquals(2, recordings.size());

      for (Recording r : recordings) {
//        assertEquals(Recording.RecordingStatus.OPTED_OUT, r.getRecordingStatus());
      }
    } catch (ParticipationManagementDatabaseException e) {
      fail("Not able to find the course recordings.");
    }

    course.setOptedOut(false);

    assertTrue(!course.isOptedOut());
    assertEquals(Recording.RecordingStatus.READY, rec1.getRecordingStatus(false));
  }

  @Test
  public void testCourseRequirements() {
    Course course = new Course("test");
    List<String> requirements = new ArrayList<String>();
    requirements.add(Course.REQUIREMENT_RECORD);
    requirements.add(Course.REQUIREMENT_CAPTIONS);
    course.setRequirements(requirements);

    try {
      pmDB.updateCourse(course);
    } catch (ParticipationManagementDatabaseException e) {
      fail("Not able to save course.");
    }

    EntityManager em = pmDB.emf.createEntityManager();
//    Option<CourseDto> courseOption = ParticipationManagementPersistenceUtil.findCourse(option(course.getId()), course.getCourseId(), course.getSeriesId(), em);
//    CourseDto dto = courseOption.get();
//    assertTrue(dto.getRequirements().contains(Course.REQUIREMENT_RECORD));
//    assertTrue(dto.getRequirements().contains(Course.REQUIREMENT_CAPTIONS));
  }


  @Test
  public void testRequiredOptedOutCourse() {
    DateTime today = new DateTime();

    Course course = new Course("test");
    List<String> requirements = new ArrayList<String>();
    requirements.add(Course.REQUIREMENT_RECORD);
    requirements.add(Course.REQUIREMENT_CAPTIONS);
    course.setRequirements(requirements);

    Recording rec1 = createRecording();
    Recording rec2 = createRecording();
    rec1.setStart(today.plusMinutes(1).toDate());
    rec1.setStop(today.plusMinutes(2).toDate());
    rec1.setReviewDate(some(today.toDate()));
    rec2.setStart(today.plusMinutes(3).toDate());
    rec2.setStop(today.plusMinutes(4).toDate());
    rec2.setReviewDate(some(today.toDate()));

    try {
      pmDB.updateCourse(course);
      rec1.setCourse(Option.<Course> some(course));
      rec2.setCourse(Option.<Course> some(course));
      pmDB.updateRecording(rec1);
      pmDB.updateRecording(rec2);
    } catch (ParticipationManagementDatabaseException e) {
      fail("Not able to save course or recording.");
    }

    RecordingQuery query = RecordingQuery.create();
    query.withCourse(course);
    try {
      List<Recording> recordings = pmDB.findRecordings(query);
      assertEquals(2, recordings.size());

      for (Recording r : recordings) {
        Boolean requiredRecording = r.getCourse().get().getRequirements().contains(Course.REQUIREMENT_RECORD);
        assertEquals(Recording.RecordingStatus.READY, r.getRecordingStatus(requiredRecording));
      }
    } catch (ParticipationManagementDatabaseException e) {
      fail("Not able to find the course recordings.");
    }
  }

  @Test
  public void testEditRecording() throws Exception {
    final Recording rec = pmDB.updateRecording(createRecording());
    final long recId = rec.getId().get();
    assertFalse("Recording should not be edited initially", pmDB.getRecording(recId).isEdit());
    // update via Recording object
    rec.setEdit(true);
    pmDB.updateRecording(rec);
    assertTrue("Recording should be edited", pmDB.getRecording(recId).isEdit());
    // update via persistence service method
    pmDB.trimRecording(recId, false);
    assertFalse("Recording should not be edited", pmDB.getRecording(recId).isEdit());
    pmDB.trimRecording(recId, true);
    assertTrue("Recording should be edited", pmDB.getRecording(recId).isEdit());
  }
}
