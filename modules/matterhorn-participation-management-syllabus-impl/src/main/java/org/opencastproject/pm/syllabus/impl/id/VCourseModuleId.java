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
package org.opencastproject.pm.syllabus.impl.id;

import java.io.Serializable;

/**
 *
 * @author ts23
 */
public class VCourseModuleId implements Serializable {

  private String courseId;
  private String moduleId;

  public VCourseModuleId() {

  }

  public VCourseModuleId(String courseId, String moduleId) {
    this.courseId = courseId;
    this.moduleId = moduleId;
  }

  public String getCourseId() {
    return courseId;
  }

  public String getModuleId() {
    return moduleId;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
            + ((courseId == null) ? 0 : courseId.hashCode());
    result = prime * result + ((moduleId == null) ? 0 : moduleId.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    VCourseModuleId other = (VCourseModuleId) obj;
    if (courseId == null) {
      if (other.courseId != null) {
        return false;
      }
    } else if (!courseId.equals(other.courseId)) {
      return false;
    }
    if (moduleId == null) {
      if (other.moduleId != null) {
        return false;
      }
    } else if (!moduleId.equals(other.moduleId)) {
      return false;
    }
    return true;
  }

}
