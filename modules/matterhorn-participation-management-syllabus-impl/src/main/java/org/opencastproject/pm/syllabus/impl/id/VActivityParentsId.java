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
public class VActivityParentsId implements Serializable {

  private String activityId;
  private String parentId;

  public VActivityParentsId() {

  }

  public VActivityParentsId(String activityId, String parentId) {
    this.activityId = activityId;
    this.parentId = parentId;
  }

  public String getActivityId() {
    return activityId;
  }

  public String getParentId() {
    return parentId;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
            + ((activityId == null) ? 0 : activityId.hashCode());
    result = prime * result + ((parentId == null) ? 0 : parentId.hashCode());
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
    VActivityParentsId other = (VActivityParentsId) obj;
    if (activityId == null) {
      if (other.activityId != null) {
        return false;
      }
    } else if (!activityId.equals(other.activityId)) {
      return false;
    }
    if (parentId == null) {
      if (other.parentId != null) {
        return false;
      }
    } else if (!parentId.equals(other.parentId)) {
      return false;
    }
    return true;
  }

}
