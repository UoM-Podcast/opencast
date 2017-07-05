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

package org.opencastproject.pm.syllabus.impl;

import static java.util.Collections.sort;
import static org.junit.Assert.assertEquals;
import static org.opencastproject.pm.syllabus.impl.SyllabusUtil.youngest;
import static org.opencastproject.util.IoSupport.loadPropertiesFromClassPath;
import static org.opencastproject.util.data.Collections.asMap;
import static org.opencastproject.util.data.Collections.groupBy;
import static org.opencastproject.util.data.Collections.grouped;
import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Collections.nil;
import static org.opencastproject.util.data.Collections.toSet;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.persistence.PersistenceEnvs.persistenceEnvironment;
import static org.opencastproject.util.persistence.PersistenceUtil.NO_PERSISTENCE_PROPS;
import static org.opencastproject.util.persistence.PersistenceUtil.newEntityManagerFactory;

import org.opencastproject.pm.api.Action;
import org.opencastproject.pm.api.CaptureAgent;
import org.opencastproject.pm.api.Course;
import org.opencastproject.pm.api.Course.EmailStatus;
import org.opencastproject.pm.api.Message;
import org.opencastproject.pm.api.Person;
import org.opencastproject.pm.api.Recording;
import org.opencastproject.pm.api.Recording.RecordingInput;
import org.opencastproject.pm.api.Recording.ReviewStatus;
import org.opencastproject.pm.api.Room;
import org.opencastproject.pm.syllabus.api.Occurrence;
import org.opencastproject.pm.syllabus.api.SyllabusService;
import org.opencastproject.pm.syllabus.api.VActivity;
import org.opencastproject.pm.syllabus.api.VActivityDateTime;
import org.opencastproject.pm.syllabus.api.VActivityLocation;
import org.opencastproject.pm.syllabus.api.VActivityStaff;
import org.opencastproject.pm.syllabus.api.VActivityStudentSet;
import org.opencastproject.pm.syllabus.api.VDepartment;
import org.opencastproject.pm.syllabus.api.VLocation;
import org.opencastproject.pm.syllabus.api.VLocationSuitability;
import org.opencastproject.pm.syllabus.api.VModule;
import org.opencastproject.pm.syllabus.api.VStaff;
import org.opencastproject.pm.syllabus.api.VStudentSet;
import org.opencastproject.pm.syllabus.api.VZones;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.persistence.PersistenceEnv;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import org.apache.commons.lang.StringUtils;

import org.eclipse.persistence.jpa.PersistenceProvider;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * THIS IS NOT A REAL UNIT TEST BUT AN INTEGRATION TEST RUNNER AND USAGE DEMO. ATTENTION: THIS CLASS IS OUTDATED SINCE
 * THE LAST REFACTORINGS todo turn into a real unit test by creating a database with some test data.
 */
@Ignore
public class AbstractSyllabusServiceTest {

  @Test
  public void testFindOccurrence() throws Exception {
    final List<Occurrence> ocs = syl().findOccurrences(new DateMidnight(2013, 7, 19).toDateTime(),
            new DateMidnight(2013, 10, 19).toDateTime());
    for (Occurrence oc : ocs) {
//      System.out.pri ntln(oc.getActivityId());
//      System.out.pri ntln(oc.getLastChanged());
//      System.out.pri ntln(oc.getLocation().getLastChanged());
    }
  }

  @Test
  public void testFindActivity() {
    final List<VActivity> as = syl().findActivities(new DateMidnight(2013, 7, 1).toDateTime());
    for (VActivity a : as) {
//      System.out.pri ntln(a.getId());
    }
  }

  @Test
  public void testFindLocation() {
    final List<VLocation> as = syl().findLocations(new DateMidnight(2013, 7, 1).toDateTime());
    for (VLocation a : as) {
//      System.out.pri ntln(a.getId());
    }
  }

  @Test
  public void testFindActivityDateTime() {
    final List<VActivityDateTime> as = syl().findActivityDateTime();
//    System.out.pri ntln(as.size());
  }

  @Test
  public void testFindActivityLocation() {
    final List<VActivityLocation> as = syl().findActivityLocation();
//    System.out.pri ntln(as.size());
  }

  private final Function<VActivityDateTime, String> getActivityIdDateTime = new Function<VActivityDateTime, String>() {
    @Override
    public String apply(VActivityDateTime a) {
      return a.getActivityId();
    }
  };

  private final Function<VStaff, String> getStaffId = new Function<VStaff, String>() {
    @Override
    public String apply(VStaff a) {
      return a.getId();
    }
  };

  private final Function<VDepartment, String> getDepartmentId = new Function<VDepartment, String>() {
    @Override
    public String apply(VDepartment a) {
      return a.getId();
    }
  };

  private final Function<VZones, String> getZonesId = new Function<VZones, String>() {
    @Override
    public String apply(VZones a) {
      return a.getId();
    }
  };

  private final Function<VStudentSet, String> getStudentId = new Function<VStudentSet, String>() {
    @Override
    public String apply(VStudentSet a) {
      return a.getId();
    }
  };

  private final Function<VModule, String> getModuleId = new Function<VModule, String>() {
    @Override
    public String apply(VModule a) {
      return a.getId();
    }
  };

  private final Function<VLocation, String> getLocationId = new Function<VLocation, String>() {
    @Override
    public String apply(VLocation a) {
      return a.getId();
    }
  };

  private final Function<VActivityStudentSet, String> getActivityIdStudentSet = new Function<VActivityStudentSet, String>() {
    @Override
    public String apply(VActivityStudentSet a) {
      return a.getActivityId();
    }
  };

  private final Function<VActivityStaff, String> getActivityIdStaff = new Function<VActivityStaff, String>() {
    @Override
    public String apply(VActivityStaff a) {
      return a.getActivityId();
    }
  };

  private final Function<VActivityLocation, String> getActivityIdLocation = new Function<VActivityLocation, String>() {
    @Override
    public String apply(VActivityLocation a) {
      return a.getActivityId();
    }
  };

  // Works but needs some heap memory. Test did not pass with a heap size below -Xmx225m
  @Test
  public void testBuildActivityHierarchyBruteForce() {
    final SyllabusService syl = syl();
    //
    // Slurp everything into memory. This brute force approach requires some heap space.
    final List<VActivity> activities = syl.findActivities();
//    System.out.pri ntln("# activity " + activities.size());
    final List<VLocation> locationList = syl.findLocations();
//    System.out.pri ntln("# location " + locationList.size());
    final Map<String, VLocation> locationMap = asMap(locationList, getLocationId);
    final List<VActivityLocation> activityLocationList = syl.findActivityLocation();
//    System.out.pri ntln("# activityLocation " + activityLocationList.size());
    final Multimap<String, VActivityLocation> activityLocationMap = groupBy(
            ArrayListMultimap.<String, VActivityLocation> create(), activityLocationList, getActivityIdLocation);
    final List<VActivityDateTime> activityDateTimeList = syl.findActivityDateTime();
//    System.out.pri ntln("# activityDateTime " + activityDateTimeList.size());
    final Multimap<String, VActivityDateTime> activityDateTimeMap = groupBy(
            ArrayListMultimap.<String, VActivityDateTime> create(), activityDateTimeList, getActivityIdDateTime);
    //
    // create graph
    int i = 0;
    for (VActivity activity : activities) {
      final String aId = activity.getId();
      // just looking for a particular activity id
      // if (ne("EAF0151470EB2655C6303E13C954BE19", aId))
      // continue;
      for (VActivityDateTime dateTime : activityDateTimeMap.get(aId)) {
        for (VActivityLocation activityLocation : activityLocationMap.get(aId)) {
          final VLocation location = locationMap.get(activityLocation.getLocationId());
          final Date lastModification = youngest(activity.getLastChanged(), dateTime.getLastChanged(),
                  activityLocation.getLastChanged(), location.getLastChanged()).toDate();
          final Recording rec = Recording.recording(aId, activity.getName(), false, nil(Person.class), // todo
                  null, // todo
                  new Room(location.getName()), lastModification,
                  // think about adding a configurable offset to start
                  // and stop dates since they describe when the event
                  // actually happens. CA should probably capture a bigger
                  // time frame in order to not miss anything.
                  // This offset may be applied when creating the MH schedule.
                  // - or, the recording may hold the offsets
                  // - or, the recording may hold a capture start date, event start date
                  // and a capture end date and an event end date
                  dateTime.getStartDateTime().toDate(), dateTime.getEndDateTime().toDate(), nil(Person.class), // todo
                  nil(Message.class), // todo
                  some(0L), // todo
                  null, // todo
                  nil(Action.class), EmailStatus.UNSENT, ReviewStatus.UNCONFIRMED, null, false, false, RecordingInput.SCREEN);
          // todo store in database
//          System.out.pri ntln(String.format("%6d ", ++i) + "Recording " + rec.getActivityId() + " "
//                  + new DateTime(rec.getStart()) + " " + new DateTime(rec.getStop()) + " @ " + rec.getRoom().getName());
        }
      }
    }

    // for (VActivity activity : activities) {
    // Collection<VActivityLocation> locations = activityLocationMap.get(activity.getId());
    // if (locations.size() > 1) {
    // mlist(locations.iterator()).foldl(Collections.<String>set(), new Function2<Set<String>, VActivityLocation,
    // Set<String>>() {
    // @Override public Set<String> apply(Set<String> sum, VActivityLocation a) {
    // sum.add(a.getLocationId());
    // return sum;
    // }
    // });
    // System.out.pri ntln("Activity " + activity.getId() + " happens in " + locations.size() + " locations");
    // for (VActivityLocation location : locations) {
    // System.out.pri ntln("  " + location.getActivityId() + " -> " + location.getLocationId());
    // }
    // }
    // }

    // System.out.pri ntln("Activity B5BA6B3E7FA1CD9114286BA4ACEB9933");
    // for (VActivityLocation location : activityLocationMap.get("B5BA6B3E7FA1CD9114286BA4ACEB9933")) {
    // System.out.pri ntln(location.getLocationId());
    // }
  }

  // Seems to issue too many requests so that it hangs reproducible.
  @Test
  public void testBuildActivityHierarchyLessMemory() {
    final SyllabusService syl = syl();
    //
    // Slurp everything into memory except the V_ACTIVITY_DATETIME table. Uses less memory than
    // the previous brute force approach but issues one additional request per activity which
    // is currently 22273! Hangs after creating the 1031th recording.
    final List<VActivity> activities = syl.findActivities();
//    System.out.pri ntln("# activity " + activities.size());
    final List<VLocation> locationList = syl.findLocations();
//    System.out.pri ntln("# location " + locationList.size());
    final Map<String, VLocation> locationMap = asMap(locationList, getLocationId);
    final List<VActivityLocation> activityLocationList = syl.findActivityLocation();
//    System.out.pri ntln("# activityLocation " + activityLocationList.size());
    final Multimap<String, VActivityLocation> activityLocationMap = groupBy(
            ArrayListMultimap.<String, VActivityLocation> create(), activityLocationList, getActivityIdLocation);
    //
    // create graph
    int i = 0;
    for (VActivity activity : activities) {
      final String aId = activity.getId();
      // if (ne("EAF0151470EB2655C6303E13C954BE19", aId))
      // continue;
      // issue a new database request for each activity
      for (VActivityDateTime dateTime : syl.findActivityDateTime(aId)) {
        for (VActivityLocation activityLocation : activityLocationMap.get(aId)) {
          final VLocation location = locationMap.get(activityLocation.getLocationId());
          final Date lastModification = youngest(activity.getLastChanged(), dateTime.getLastChanged(),
                  activityLocation.getLastChanged(), location.getLastChanged()).toDate();
          final Recording rec = Recording.recording(aId, activity.getName(), false, nil(Person.class), // todo
                  null, // todo
                  new Room(location.getName()), lastModification,
                  // think about adding a configurable offset to start
                  // and stop dates since they describe when the event
                  // actually happens. CA should probably capture a bigger
                  // time frame in order to not miss anything.
                  // This offset may be applied when creating the MH schedule.
                  // - or, the recording may hold the offsets
                  // - or, the recording may hold a capture start date, event start date
                  // and a capture end date and an event end date
                  dateTime.getStartDateTime().toDate(), dateTime.getEndDateTime().toDate(), nil(Person.class), // todo
                  nil(Message.class), // todo
                  some(0L), // todo
                  null, // todo
                  nil(Action.class), EmailStatus.UNSENT, ReviewStatus.UNCONFIRMED, null, false, false, RecordingInput.SCREEN);
          // todo store in database
//          System.out.pri ntln(String.format("%6d ", ++i) + "Recording " + rec.getActivityId() + " "
//                  + new DateTime(rec.getStart()) + " " + new DateTime(rec.getStop()) + " @ " + rec.getRoom().getName());
        }
      }
    }
  }

  // Seems to issue too many requests so that it hangs reproducible.
  @Test
  public void testBuildActivityHierarchyEvenLessMemory() {
    final SyllabusService syl = syl();
    //
    // Slurp everything into memory except the V_ACTIVITY_DATETIME and V_ACTIVITY_LOCATION table.
    // Issues probably far too many requests. Hangs like the previous approach after
    // the creation of the 1031th recording.
    final List<VActivity> activities = syl.findActivities();
//    System.out.pri ntln("# activity " + activities.size());
    final List<VLocation> locationList = syl.findLocations();
//    System.out.pri ntln("# location " + locationList.size());
    final Map<String, VLocation> locationMap = asMap(locationList, getLocationId);
    //
    // create graph
    int i = 0;
    for (VActivity activity : activities) {
      final String aId = activity.getId();
      // limit to a certain activity id
      // if (ne("EAF0151470EB2655C6303E13C954BE19", aId))
      // continue;
      // issue a new database request for each activity to keep memory consumption low
      final List<VActivityDateTime> activityDateTimeList = syl.findActivityDateTime(aId);
      final List<VActivityLocation> activityLocationList = syl.findActivityLocation(aId);
      for (VActivityDateTime dateTime : activityDateTimeList) {
        // issue a new database request for each activity
        for (VActivityLocation activityLocation : activityLocationList) {
          final VLocation location = locationMap.get(activityLocation.getLocationId());
          final Date lastModification = youngest(activity.getLastChanged(), dateTime.getLastChanged(),
                  activityLocation.getLastChanged(), location.getLastChanged()).toDate();
          final Recording rec = Recording.recording(aId, activity.getName(), false, nil(Person.class), // todo
                  null, // todo
                  new Room(location.getName()), lastModification,
                  // think about adding a configurable offset to start
                  // and stop dates since they describe when the event
                  // actually happens. CA should probably capture a bigger
                  // time frame in order to not miss anything.
                  // This offset may be applied when creating the MH schedule.
                  // - or, the recording may hold the offsets
                  // - or, the recording may hold a capture start date, event start date
                  // and a capture end date and an event end date
                  dateTime.getStartDateTime().toDate(), dateTime.getEndDateTime().toDate(), nil(Person.class), // todo
                  nil(Message.class), // todo
                  some(0L), // todo
                  null, // todo
                  nil(Action.class), EmailStatus.UNSENT, ReviewStatus.UNCONFIRMED, null, false, false, RecordingInput.SCREEN);
          // todo store in database
//          System.out.pri ntln(String.format("%6d ", ++i) + "Recording " + rec.getActivityId() + " "
//                  + new DateTime(rec.getStart()) + " " + new DateTime(rec.getStop()) + " @ " + rec.getRoom().getName());
        }
      }
    }
  }

  // This test partitions the list of activities into chunks of 1000 and then fetches the matching
  // V_ACTIVITY_DATETIME entities. This test runs with a heap size of -Xmx75m and has a reasonable
  // performance.
  @Test
  public void testBuildActivityHierarchyPartitioned() {
    final SyllabusService syl = syl();
    //
    // Slurp some tables into memory. Then partition the activity list and fetch V_ACTIVITY_DATETIME
    // in partitions.
    final List<List<VActivity>> activitiesPartitioned;
    {
      // Put this in a separate method or like this so that the garbage collector can
      // dispose the original list to safe heap space.
      // Drain list into new list since the returned one is immutable
      final List<VActivity> activities = new ArrayList<VActivity>(syl.findActivities());
      // Sort activities by id ascending. Sorting is crucial otherwise the
      // partition approach does not work. Matching dateTime entities are fetched
      // based an lexicographic id ordering
      sort(activities, new Comparator<VActivity>() {
        @Override
        public int compare(VActivity a, VActivity b) {
          return a.getId().compareTo(b.getId());
        }
      });
//      System.out.pri ntln("# activity " + activities.size());
      activitiesPartitioned = grouped(activities, 1000);
    }
    final List<VLocation> locationList = syl.findLocations();
//    System.out.pri ntln("# location " + locationList.size());
    final Map<String, VLocation> locationMap = asMap(locationList, getLocationId);
    final List<VActivityLocation> activityLocationList = syl.findActivityLocation();
//    System.out.pri ntln("# activityLocation " + activityLocationList.size());
    final Multimap<String, VActivityLocation> activityLocationMap = groupBy(
            ArrayListMultimap.<String, VActivityLocation> create(), activityLocationList, getActivityIdLocation);

    List<VModule> modules = syl.findModules();
//    System.out.pri ntln("# modules " + modules.size());
    final Map<String, VModule> moduleMap = asMap(modules, getModuleId);

    List<VDepartment> department = syl.findDepartment();
//    System.out.pri ntln("# departments " + department.size());
    final Map<String, VDepartment> departmentMap = asMap(department, getDepartmentId);

    List<VZones> zones = syl.findZones();
//    System.out.pri ntln("# zones " + zones.size());
    final Map<String, VZones> zonesMap = asMap(zones, getZonesId);

    List<VStaff> vStaff = syl.findStaff();
//    System.out.pri ntln("# staff " + vStaff.size());
    final Map<String, VStaff> staffMap = asMap(vStaff, getStaffId);
    final List<VActivityStaff> activityStaffList = syl.findActivityStaff();
//    System.out.pri ntln("# activityStaff " + activityStaffList.size());
    final Multimap<String, VActivityStaff> activityStaffMap = groupBy(
            ArrayListMultimap.<String, VActivityStaff> create(), activityStaffList, getActivityIdStaff);

    List<VStudentSet> studentSet = syl.findStudentSets();
//    System.out.pri ntln("# studentSet " + studentSet.size());
    final Map<String, VStudentSet> studentMap = asMap(studentSet, getStudentId);
    final List<VActivityStudentSet> activityStudentSet = syl.findActivityStudentSet();
//    System.out.pri ntln("# activityStudentSet " + activityStudentSet.size());
    final Multimap<String, VActivityStudentSet> activityStudentSetMap = groupBy(
            ArrayListMultimap.<String, VActivityStudentSet> create(), activityStudentSet, getActivityIdStudentSet);
    final Set<String> locationHasCaptureAgent;
    {
      final List<VLocationSuitability> al = syl.findLocationSuitability();
//      System.out.pri ntln("# locationSuitability " + al.size());
      final Set<String> as = toSet(mlist(al).bind(new Function<VLocationSuitability, Option<String>>() {
        @Override
        public Option<String> apply(VLocationSuitability a) {
          return syl.getCaptureRoomTypeIDs().contains(a.getSuitabilityId()) ? some(a.getLocationId()) : none(String.class);
        }
      }).value());
//      System.out.pri ntln("# locations featuring capture agents " + as.size());
      locationHasCaptureAgent = as;
    }

    //
    // create graph
    int i = 0;
    // iterate activity partitions
    for (List<VActivity> activityPartition : activitiesPartitioned) {
      // fetch matching V_ACTIVITY_DATETIME entities
      final String firstActivityId = activityPartition.get(0).getId();
      final String lastActivityId = activityPartition.get(activityPartition.size() - 1).getId();
      final List<VActivityDateTime> activityDateTimeList = syl.findActivityDateTimeByRange(firstActivityId,
              lastActivityId);
//      System.out.pri ntln("# activityDateTime from " + firstActivityId + " to " + lastActivityId + " "
//              + activityDateTimeList.size());
      final Multimap<String, VActivityDateTime> activityDateTimeMap = groupBy(
              ArrayListMultimap.<String, VActivityDateTime> create(), activityDateTimeList, getActivityIdDateTime);
      // iterate activity partition
      for (VActivity activity : activityPartition) {
        final String aId = activity.getId();
        if (!syl.isCaptureActivityType(activity))
          continue;
        for (VActivityDateTime dateTime : activityDateTimeMap.get(aId)) {
          for (VActivityLocation activityLocation : activityLocationMap.get(aId)) {
            final VLocation location = locationMap.get(activityLocation.getLocationId());
            if (location == null) {
//              System.err.pri ntln("Location " + activityLocation.getLocationId() + " not found! Inconsistend data!");
              continue;
            }
            // CHECKSTYLE:OFF
            if (locationHasCaptureAgent.contains(location.getId())) {
              // CHECKSTYLE:ON
              VModule module = moduleMap.get(activity.getModuleId());

              if (location.getZoneId().isSome()) {
                VZones zone = zonesMap.get(location.getZoneId().get());
              }

              Collection<VActivityStaff> staffList = activityStaffMap.get(aId);
              if (staffList == null) {
//                System.err.pri ntln("ActivityStaffList for activity id" + aId + " not found! Inconsistend data!");
                continue;
              }

              List<Person> staff = new ArrayList<Person>();
              for (VActivityStaff activityStaff : staffList) {
                VStaff s = staffMap.get(activityStaff.getStaffId());
                if (s == null) {
//                  System.err.pri ntln("Staff " + activityStaff.getStaffId() + " not found! Inconsistend data!");
                  continue;
                }
                if (s.getEmail().isSome())
                  staff.add(Person.person(s.getName(), s.getEmail().get()));
              }

              Collection<VActivityStudentSet> participationList = activityStudentSetMap.get(aId);
              if (participationList == null) {
//                System.err.pri ntln("ActivityStudentSetList for activity id" + aId + " not found! Inconsistend data!");
                continue;
              }
              List<Person> participation = new ArrayList<Person>();
              for (VActivityStudentSet activityStudent : participationList) {
                VStudentSet s = studentMap.get(activityStudent.getStudentSetId());
                if (s == null) {
//                  System.err.pri ntln("Student " + activityStudent.getStudentSetId() + " not found! Inconsistend data!");
                  continue;
                }

                if (s.getHostKey() != null && !s.getHostKey().startsWith("#") && s.getEmail().isSome()) {
                  participation.add(Person.person(s.getName(), s.getEmail().get()));
                }
              }

              if (staff.size() < 1 || module == null || StringUtils.isBlank(module.getName()))
                continue;

              Course course = new Course(module.getId(), null, null);

              final Room room = new Room(location.getName());

              final Recording rec = Recording.recording(
                      aId,
                      module.getName(),
                      staff,
                      course,
                      room,
                      new Date(),
                      // think about adding a configurable offset to start
                      // and stop dates since they describe when the event
                      // actually happens. CA should probably capture a bigger
                      // time frame in order to not miss anything.
                      // This offset may be applied when creating the MH schedule.
                      // - or, the recording may hold the offsets
                      // - or, the recording may hold a capture start date, event start date
                      // and a capture end date and an event end date
                      dateTime.getStartDateTime().toDate(), dateTime.getEndDateTime().toDate(), participation,
                      new CaptureAgent(room, CaptureAgent.getMhAgentIdFromRoom(room)));
              // todo store in database
//              System.out.pri ntln(String.format("%6d ", ++i) + "Recording " + rec.getActivityId() + " "
//                      + new DateTime(rec.getStart()) + " " + new DateTime(rec.getStop()) + " @ "
//                      + rec.getRoom().getName());
            }
          }
        }
      }
    }
  }

  @Test
  public void testActivityLocation() {
    for (VActivityLocation al : syl().findActivityLocation()) {
      if ("B5BA6B3E7FA1CD9114286BA4ACEB9933".equals(al.getActivityId())) {
//        System.out.pri ntln(al.getLocationId());
      }
    }
  }

  @Test
  public void testSortDateTime() {
    final List<DateTime> dates = list(new DateTime(2015, 1, 1, 0, 0, 0, 0), new DateTime(2013, 11, 15, 0, 0, 0, 0));
    // sort ascending
    sort(dates);
    assertEquals(new DateTime(2013, 11, 15, 0, 0, 0, 0), dates.get(0));
  }

  private SyllabusService syl() {
    try {
      Class.forName("net.sourceforge.jtds.jdbc.Driver");
    } catch (ClassNotFoundException e) {
      throw new Error(
              "MSSQL JDBC driver cannot be found. See pom.xml for information on the MSSQL JDBC driver installation.");
    }
    final Properties credentials = loadPropertiesFromClassPath("/syllabus-credentials.properties");
    final String url = "jdbc:jtds:sqlserver://localhost:1433/ScientiaProdRDB";
    final String user = credentials.getProperty("user");
    final String password = credentials.getProperty("password");
//    System.out.pri ntln("Connecting to DB server at " + url);
//    System.out.pri ntln("DB user " + user);
    final PersistenceEnv penv = persistenceEnvironment(newEntityManagerFactory(
            SyllabusServicePublisher.PERSISTENCE_UNIT, "SQLServer", "net.sourceforge.jtds.jdbc.Driver", url, user,
            password, NO_PERSISTENCE_PROPS, new PersistenceProvider()));
    return new AbstractSyllabusService() {
      protected PersistenceEnv getPenv() {
        return penv;
      }

      public String getSourceDescription() {
        return "Syllabus+:Test";
      }

      @Override
      protected void closePenv() {
      }
    };
  }
}
