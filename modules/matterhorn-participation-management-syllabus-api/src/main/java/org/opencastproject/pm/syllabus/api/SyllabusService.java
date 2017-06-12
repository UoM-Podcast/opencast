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

package org.opencastproject.pm.syllabus.api;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;

/** Provides access to a Syllabus+ data store. */
public interface SyllabusService {
  /**
   * Return a list of {@link Occurrence occurrences} matching the following criteria
   * <ul>
   * <li>created or modified after (&gt;) <code>since</code></li>
   * <li>starting before (&lt;=) <code>untilStartDate</code></li>
   * <li>does not belong to an unscheduled activity, i.e. activities whose <code>IsScheduled</code> flag is set to
   * <code>false</code></li>
   * </ul>
   */
  List<Occurrence> findOccurrences(DateTime since, DateTime untilStartDate);

  /** Return a list of {@link VActivity activities} modified after (&gt;) <code>since</code>. */
  List<VActivity> findActivities(DateTime since);

  /** Return a list of all {@link VActivity activities}. */
  List<VActivity> findActivities();

  /** Return a list of {@link VLocation locations} modified after (&gt;) <code>since</code>. */
  List<VLocation> findLocations(DateTime since);

  /** Return a list of all {@link VLocation locations}. */
  List<VLocation> findLocations();

  /** Return a list of {@link VStaff staff} modified after (&gt;) <code>since</code>. */
  List<VStaff> findStaff(DateTime since);

  /** Return a list of all the {@link VStaff staff}. */
  List<VStaff> findStaff();

  /** Return a list of {@link VDepartment department} modified after (&gt;) <code>since</code>. */
  List<VDepartment> findDepartment(DateTime since);

  /** Return a list of all the {@link VDepartment department}. */
  List<VDepartment> findDepartment();

  /** Return a list of {@link VZones zones} modified after (&gt;) <code>since</code>. */
  List<VZones> findZones(DateTime since);

  /** Return a list of all the {@link VZones zones}. */
  List<VZones> findZones();

  /**
   * Return a list of {@link VActivityDateTime entries in V_ACTIVITY_DATETIME} modified after (&gt;) <code>since</code>.
   */
  List<VActivityDateTime> findActivityDateTime(DateTime since);

  /**
   * Return a list of all {@link VActivityDateTime entries in V_ACTIVITY_DATETIME}. Use with care, this table will be
   * pretty large.
   */
  List<VActivityDateTime> findActivityDateTime();

  /** Return a list of all {@link VActivityDateTime entries in V_ACTIVITY_DATETIME} for a certain activity id. */
  List<VActivityDateTime> findActivityDateTime(String activityId);

  /**
   * Return a list of all {@link VActivityDateTime entries in V_ACTIVITY_DATETIME} whose activity ids are
   * lexicographically greater or equal to <code>startActivityId</code> and less than or equal than
   * <code>endActivityId</code>.
   */
  List<VActivityDateTime> findActivityDateTimeByRange(String startActivityId, String endActivityId);

  /** Return a list of {@link VCourse courses} modified after (&gt;) <code>since</code>. */
  List<VCourse> findCourses(DateTime since);

  /** Return a list of all {@link VCourse courses}. */
  List<VCourse> findCourses();

  /** Return a list of {@link VEquipment equipment} modified after (&gt;) <code>since</code>. */
  List<VEquipment> findEquipment(DateTime since);

  /** Return a list of all {@link VEquipment equipment}. */
  List<VEquipment> findEquipment();

  /** Return a list of {@link VModule modules} modified after (&gt;) <code>since</code>. */
  List<VModule> findModules(DateTime since);

  /** Return a list of all {@link VModule modules}. */
  List<VModule> findModules();

  /** Return a list of {@link VStudentSet stundent sets} modified after (&gt;) <code>since</code>. */
  List<VStudentSet> findStudentSets(DateTime since);

  /** Return a list of all {@link VStudentSet stundent sets}. */
  List<VStudentSet> findStudentSets();

  /**
   * Return a list of all {@link VActivityLocation entries in V_ACTIVITY_LOCATION} modified after (&gt;)
   * <code>since</code>.
   */
  List<VActivityLocation> findActivityLocation(DateTime since);

  /**
   * Return a list of all {@link VActivityLocation entries in V_ACTIVITY_LOCATION}. Use with care, this table may grow
   * large.
   */
  List<VActivityLocation> findActivityLocation();

  /** Return a list of all {@link VActivityLocation entries in V_ACTIVITY_LOCATION} for a certain activity id. */
  List<VActivityLocation> findActivityLocation(String activityId);

  /**
   * Return a list of all {@link VActivityParents entries in V_ACTIVITY_PARENTS}.
   */
  List<VActivityParents> findActivityParents();

  /** Return a list of all {@link VActivityParents entries in V_ACTIVITY_PARENTS} for a certain activity id. */
  List<VActivityParents> findActivityParents(String activityId);

  /**
   * Return a list of all {@link VLocationSuitability entries in V_LOCATION_SUITABILITY} modified after (&gt;)
   * <code>since</code>.
   */
  List<VLocationSuitability> findLocationSuitability(DateTime since);

  /** Return a list of all {@link VLocationSuitability entries in V_LOCATION_SUITABILITY}. */
  List<VLocationSuitability> findLocationSuitability();

  /** Return a list of all {@link VLocationSuitability entries in V_LOCATION_SUITABILITY}. */
  List<VLocationSuitability> findLocationSuitability(String suitabilityId);

  /**
   * Return a list of {@link VActivityEquipment all entries in V_ACTIVITY_EQUIPMENT} modified after (&gt;)
   * <code>since</code>.
   */
  List<VActivityEquipment> findActivityEquipment(DateTime since);

  /** Return a list of {@link VActivityEquipment all entries in V_ACTIVITY_EQUIPMENT}. */
  List<VActivityEquipment> findActivityEquipment();

  /**
   * Return a list of {@link VActivityStudentSet all entries in V_ACTIVITY_STUDENTSET} modified after (&gt;)
   * <code>since</code>.
   */
  List<VActivityStudentSet> findActivityStudentSet(DateTime since);

  /** Return a list of {@link VActivityStudentSet all entries in V_ACTIVITY_STUDENTSET}. */
  List<VActivityStudentSet> findActivityStudentSet();

  /** Return a list of {@link VActivityStaff all entries in V_ACTIVITY_STAFF} modified after (&gt;) <code>since</code>. */
  List<VActivityStaff> findActivityStaff(DateTime since);

  /** Return a list of {@link VActivityStaff all entries in V_ACTIVITY_STAFF}. */
  List<VActivityStaff> findActivityStaff();

  /**
   * Check if all given activities still exist.
   *
   * @param activityIds
   *          a list of activity ids to check for existence
   * @return a list of activity ids that do not exist anymore
   */
  List<String> findDeletedActivities(List<String> activityIds);

  /**
   * Find child activity Ids associated with a Module's MLEID (UserText4/courseKey)
   * @return List of activities that reference the module identified by the course key
  */
  List<String> findModuleActivityIdsByCourseKey(String courseKey);

  /**
   * Get the time zone of the data in the S+ store. For example the S+ store at Manchester returns UTC or UTC+1 (BST).
   * <p/>
   * The time zone information will be important if the system that creates the schedule is not located in the same time
   * zone as the database and dates in the db do not have any time zone information.
   */
  DateTimeZone getTimeZone();

  /**
   * Get the description of the S+ data source. For example "Syllabus+:Central"
   * @return source description
   */
  String getSourceDescription();

  /**
   * Get a module my its courseKey (UserText4)
   * @return source description
   */
  VModule getModuleByCourseKey(String courseKey);
}
