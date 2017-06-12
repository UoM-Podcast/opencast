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

package org.opencastproject.pm.syllabus.impl.scheduling;

import org.opencastproject.pm.api.Course;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.pm.api.util.RequirementManager;
import org.opencastproject.requirement.api.RequirementService;

import org.apache.commons.lang.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 *
 * @author james
 */
public class DassRequirementManager implements RequirementManager {

  private RequirementService requirementService;
  private Logger logger = LoggerFactory.getLogger(DassRequirementManager.class);
  private ParticipationManagementDatabase pmDB;

  @Override
  public void updateRequirements() {
    updateCourseRequirements(requirementService);
  }

  public DassRequirementManager(RequirementService requirementService, ParticipationManagementDatabase pmDB) {
    this.requirementService = requirementService;
    this.pmDB = pmDB;
  }

  private void updateCourseRequirements(RequirementService requirementService) {
    try {
      List<String> requiredCourses = requirementService.getIds(RequirementService.Resource.SERIES, RequirementService.Requirement.RECORDING);
      List<Course> courses = pmDB.getCourses();
      Integer addedDass = 0;
      Integer removedDass = 0;

      for (Course course : courses) {
        if (StringUtils.isNotEmpty(course.getExternalCourseKey())) {
          // courseKey will be a concatenate string for combined courses
          Set<String> courseKeys = new HashSet<String>(Arrays.asList(course.getExternalCourseKey().split("_")));
          courseKeys.retainAll(requiredCourses);

          if (course.getRequirements().contains(Course.REQUIREMENT_RECORD)) {
            if (courseKeys.size() == 0) {
              course.getRequirements().remove(Course.REQUIREMENT_RECORD);
              course.setEmailStatus(Course.EmailStatus.UNSENT);
              pmDB.updateCourse(course);
              logger.info("DASS requirement removed from {}, {}", course, course.getName());
              removedDass++;
            }
          } else {
            if (courseKeys.size() != 0) {
              course.getRequirements().add(Course.REQUIREMENT_RECORD);
              course.setEmailStatus(Course.EmailStatus.UNSENT);
              pmDB.updateCourse(course);
              logger.info("DASS requirement added to {}, {}", course, course.getName());
              addedDass++;
            }
          }
        }
      }

      logger.info("DASS requirement added to {} courses and removed from {} courses", addedDass, removedDass);
    } catch (Exception e) {
      logger.error("Can not check course requirements: {}", e.getMessage());
    }
  }
}
