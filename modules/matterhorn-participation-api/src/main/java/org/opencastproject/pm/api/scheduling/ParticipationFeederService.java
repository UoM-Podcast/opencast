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

package org.opencastproject.pm.api.scheduling;

import org.opencastproject.pm.api.Course;
import org.opencastproject.util.NotFoundException;

import java.util.List;

import javax.ws.rs.core.Response;

/**
 * Represents the external participation feeder service
 */
public interface ParticipationFeederService {

  /**
   * provides information if the harvest process can be initiated
   */
  Response permitHarvest();

  /**
   * Initiates an harvest process on the external feeder service
   */
  void harvest();

  /**
   * provides information if the harvest process can be initiated
   */
  Response getHarvestStats() throws NotFoundException;

  /**
  * returns a Course object that represents the given activity,
  * the course may be virtual module
  *
  @param activityId
  @return
  @throws NotFoundException
  */
  Course getCourseByActivityId(String activityId) throws NotFoundException;

  /**
  * returns a Course object that represent the module with the given courseKey
  * NOTE: This should only be used if there are no activities associated with
  * the module
  *
  @param courseKey
  @return
  @throws NotFoundException
  */
  Course getCourseByCourseKey(String courseKey) throws NotFoundException;

  /**
  * returns child activity Ids that of activities that reference a module
  * identified by the courseKey
  *
  @param courseKey
  @return
  @throws NotFoundException
  */
  List<String> getActivityIds(String courseKey) throws NotFoundException;
}
