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

package org.opencastproject.pm.impl.scheduling;

import static org.junit.Assert.fail;
import static org.opencastproject.pm.api.Person.person;
import static org.opencastproject.util.data.Collections.nil;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.VCell.cell;
import static org.opencastproject.util.persistence.PersistenceUtil.newTestEntityManagerFactory;

import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.pm.api.Action;
import org.opencastproject.pm.api.CaptureAgent;
import org.opencastproject.pm.api.Course;
import org.opencastproject.pm.api.Course.EmailStatus;
import org.opencastproject.pm.api.Message;
import org.opencastproject.pm.api.Person;
import org.opencastproject.pm.api.PersonType;
import org.opencastproject.pm.api.Recording;
import org.opencastproject.pm.api.Recording.RecordingInput;
import org.opencastproject.pm.api.Recording.ReviewStatus;
import org.opencastproject.pm.api.Room;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabaseException;
import org.opencastproject.pm.api.scheduling.ParticipationManagementSchedulingException;
import org.opencastproject.pm.api.scheduling.Schedule;
import org.opencastproject.pm.impl.persistence.ParticipationManagementDatabaseImpl;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.NotFoundException;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class ParticipationManagementProviderTest {

  private ComboPooledDataSource pooledDataSource = null;
  private ParticipationManagementDatabaseImpl pmDB = null;
  private ParticipationManagementProvider pmProvider = null;
  private SeriesService seriesService = null;
  private SecurityService securityService = null;
  private Date upcomingDate = new DateTime().plusHours(4).toDate();

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
    CaptureAgent captureAgent = new CaptureAgent(room, "mh_ncast1");
    Course course = new Course("mathe", "uuid", "Math", "Simple course about algebra.", "I1234-MATH-56789-1151-1YR-012345");

    return Recording.recording("activityId", "Test title", false, staff, some(course), room, new Date(), new DateTime()
            .plusHours(2).toDate(), new DateTime().plusHours(3).toDate(), students, nil(Message.class), some(0L),
            captureAgent, nil(Action.class), EmailStatus.UNSENT, ReviewStatus.UNCONFIRMED, none(Date.class), false, false, RecordingInput.SCREEN);
  }

  @Before
  public void setUp() throws Exception {
    // Set up the database
    pooledDataSource = new ComboPooledDataSource();
    pooledDataSource.setDriverClass("org.h2.Driver");
    pooledDataSource.setJdbcUrl("jdbc:h2:./target/db" + System.currentTimeMillis());
    pooledDataSource.setUser("sa");
    pooledDataSource.setPassword("sa");

    // Set up the persistence properties
    Map<String, Object> props = new HashMap<String, Object>();
    props.put("javax.persistence.nonJtaDataSource", pooledDataSource);
    props.put("eclipselink.ddl-generation", "create-tables");
    props.put("eclipselink.ddl-generation.output-mode", "database");

    User user = new JaxbUser("test", null, "Test User", "test@test.com", "test", new DefaultOrganization(),
            new HashSet<JaxbRole>());

    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.replay(securityService);

    UserDirectoryService userDirectoryService = EasyMock.createNiceMock(UserDirectoryService.class);
    EasyMock.expect(userDirectoryService.loadUser((String) EasyMock.anyObject())).andReturn(user).anyTimes();
    EasyMock.replay(userDirectoryService);

    // Set up the annotation service
    pmDB = new ParticipationManagementDatabaseImpl();
    pmDB.setEntityManagerFactory(newTestEntityManagerFactory(ParticipationManagementDatabaseImpl.PERSISTENCE_UNIT));
    pmDB.setSecurityService(securityService);
    pmDB.setUserDirectoryService(userDirectoryService);

    seriesService = EasyMock.createNiceMock(SeriesService.class);

    pmProvider = new ParticipationManagementProvider(pmDB, seriesService, securityService, true, cell(0), cell(0));
  }

  @After
  public void tearDown() throws Exception {
    pmDB.deactivate(null);
  }

  @Test
  public void testRecordingWithCourseAndWithoutSeries() {
    Recording recording = createRecording();
    final String seriesId = recording.getCourse().get().getCourseId().replaceAll(" ", "-");
    DublinCoreCatalog dc = ParticipationManagementProvider.toSeriesCatalog(recording.getCourse().get());

    try {
      EasyMock.expect(seriesService.getSeries(seriesId)).andReturn(dc).anyTimes();
      EasyMock.expect(seriesService.getSeries((String) EasyMock.anyObject())).andThrow(new NotFoundException())
              .anyTimes();
      EasyMock.expect(seriesService.updateSeries((DublinCoreCatalog) EasyMock.anyObject())).andReturn(dc).anyTimes();
      EasyMock.expect(
              seriesService.updateAccessControl((String) EasyMock.anyObject(), (AccessControlList) EasyMock.anyObject()))
              .andReturn(true).once();
      EasyMock.replay(seriesService);
    } catch (Exception e) {
      fail("Exception when describing mock behavior ");
    }

    Assert.assertEquals(dc.getFirst(DublinCore.PROPERTY_TITLE), "Math 2015/16");

    try {
      recording = pmDB.updateRecording(recording);
    } catch (ParticipationManagementDatabaseException e) {
      fail("Can not update recording");
    }

    // With course but no series (a new one is created)
    try {
      Schedule schedule = pmProvider.fetchSchedule(new Date(), upcomingDate);
      Assert.assertEquals(1, schedule.getEpisodes().size());
      Recording result = schedule.getEpisodes().get(0).getA();
      Assert.assertEquals(recording.getId(), result.getId());
      Assert.assertFalse(recording.getCourse().get().equals(result.getCourse().get()));
      Assert.assertEquals("I1234-MATH-56789-1151-1YR-012345", recording.getCourse().get().getExternalCourseKey());
    } catch (ParticipationManagementSchedulingException e) {
      fail("Cannot fetch recording: " + e.getMessage());
      e.printStackTrace();
    }

    EasyMock.verify(seriesService);
  }

  @Test
  public void testRecordingWithCourseAndWithoutSeriesNoCreation() {
    pmProvider = new ParticipationManagementProvider(pmDB, seriesService, securityService, false, cell(0), cell(0));

    Recording recording = createRecording();
    final String seriesId = recording.getCourse().get().getCourseId().replaceAll(" ", "-");
    DublinCoreCatalog dc = ParticipationManagementProvider.toSeriesCatalog(recording.getCourse().get());

    try {
      EasyMock.expect(seriesService.getSeries(seriesId)).andReturn(dc).anyTimes();
      EasyMock.expect(seriesService.getSeries((String) EasyMock.anyObject())).andThrow(new NotFoundException())
              .anyTimes();
      EasyMock.expect(seriesService.updateSeries((DublinCoreCatalog) EasyMock.anyObject())).andReturn(dc).anyTimes();
      EasyMock.replay(seriesService);
    } catch (Exception e) {
      fail("Exception when describing mock behavior ");
    }

    try {
      recording = pmDB.updateRecording(recording);
    } catch (ParticipationManagementDatabaseException e) {
      fail("Can not update recording");
    }

    try {
      pmProvider.fetchSchedule(new Date(), upcomingDate);
    } catch (ParticipationManagementSchedulingException e) {
      Assert.assertTrue(true);
    }
  }

  @Test
  public void testRecordingWithCourseAndSeries() {
    Recording recording = createRecording();
    final String seriesId = recording.getCourse().get().getSeriesId();
    DublinCoreCatalog dc = ParticipationManagementProvider.toSeriesCatalog(recording.getCourse().get());

    try {
      EasyMock.expect(seriesService.getSeries(seriesId)).andReturn(dc).once();
      EasyMock.expect(seriesService.updateSeries((DublinCoreCatalog) EasyMock.anyObject())).andReturn(dc).anyTimes();
      EasyMock.expect(
              seriesService.updateAccessControl((String) EasyMock.anyObject(), (AccessControlList) EasyMock.anyObject()))
              .andReturn(true).once();
      EasyMock.replay(seriesService);
    } catch (Exception e) {
      fail("Exception when describing mock behavior ");
    }

    try {
      recording = pmDB.updateRecording(recording);
    } catch (ParticipationManagementDatabaseException e) {
      fail("Can not update recording");
    }

    // With course but no series (a new one is created)
    try {
      Schedule schedule = pmProvider.fetchSchedule(new Date(), upcomingDate);
      Assert.assertEquals(1, schedule.getEpisodes().size());
      Recording result = schedule.getEpisodes().get(0).getA();
      Assert.assertEquals(recording.getId(), result.getId());
      Assert.assertEquals(recording.getCourse().get(), result.getCourse().get());
    } catch (ParticipationManagementSchedulingException e) {
      fail("Can not fetch recording: " + e.getMessage());
      e.printStackTrace();
    }

    EasyMock.verify(seriesService);
  }

  @Test
  public void testRecordingWithoutCourse() {
    Recording recording = createRecording();
    recording.setCourse(none(Course.class));

    try {
      recording = pmDB.updateRecording(recording);
    } catch (ParticipationManagementDatabaseException e) {
      fail("Can not update recording");
    }

    // With course but no series (a new one is created)
    try {
      Schedule schedule = pmProvider.fetchSchedule(new Date(), upcomingDate);
      Assert.assertEquals(1, schedule.getEpisodes().size());
      Recording result = schedule.getEpisodes().get(0).getA();
      Assert.assertEquals(recording.getId(), result.getId());
      Assert.assertTrue(result.getCourse().isNone());
    } catch (ParticipationManagementSchedulingException e) {
      fail("Can not fetch recording: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
