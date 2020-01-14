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

import java.util.List;

/**
 *
 * @author james
 */
public interface SyllabusDataService {

  /**
   * fetch()
   *
   * fetch all the data
   * @return all the syllabus data
   */
  SyllabusData fetch();

  /**
   * fetchModules()
   *
   * etch only the data relating to Modules
   * @return the module syllabus data
   */
  SyllabusData fetchModules();

   /**
   * Find child activity Ids associated with a Module's MLEID (UserText4/courseKey)
   * @return List of activities that reference the module identified by the course key
  */
  List<String> findModuleActivityIdsByCourseKey(String courseKey);

  /**
   * Return a list of all {@link VActivityDateTime entries in V_ACTIVITY_DATETIME} whose activity ids are
   * lexicographically greater or equal to <code>startActivityId</code> and less than or equal than
   * <code>endActivityId</code>.
   */
  List<VActivityDateTime> findActivityDateTimeByRange(String startActivityId, String endActivityId);

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

  VModule getModuleById(String id);

  List<VModule> getAllModule();

  SyllabusCaptureFilter getSyllabusCaptureFilter();

  List<Activities> findActivityByStaffId(String staffId);

  List<Activities> findActivityByModule(String moduleName);

  List<Activities> findChildActivity(String activityId);

  VStaff findStaffById(String spotId);

  List<VStaff> findStaffByStaffActivityId(String activityId);

  List<VActivityLocation> findByActivityLocationId(String activityId);

  List<VActivityDateTime> findActivityDateTime(String activityId);

  List<VLocation> findSuitabilityByLocationId(String locationId);

  List<Activities> findActivitiesByLocation(String location, DateTime start, DateTime end);

  List<Activities> findParentActivities(String activityId);
}
