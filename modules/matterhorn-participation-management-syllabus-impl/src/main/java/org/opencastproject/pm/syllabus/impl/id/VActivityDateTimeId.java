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
import java.util.Date;

/**
 *
 * @author ts23
 */
public class VActivityDateTimeId implements Serializable {

  private String activityId;
  private Date startDateTime;
  private Date endDateTime;

  public VActivityDateTimeId() {

  }

  public VActivityDateTimeId(String activityId, Date startDateTime, Date endDateTime) {
    this.activityId = activityId;
    this.startDateTime = startDateTime;
    this.endDateTime = endDateTime;
  }

  public String getActivityId() {
    return activityId;
  }

  public Date getStartDateTime() {
    return startDateTime;
  }

  public Date getEndDateTime() {
    return endDateTime;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
            + ((activityId == null) ? 0 : activityId.hashCode());
    result = prime * result + ((startDateTime == null) ? 0 : startDateTime.hashCode());
    result = prime * result + ((endDateTime == null) ? 0 : endDateTime.hashCode());
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
    VActivityDateTimeId other = (VActivityDateTimeId) obj;
    if (activityId == null) {
      if (other.activityId != null) {
        return false;
      }
    } else if (!activityId.equals(other.activityId)) {
      return false;
    }
    if (startDateTime == null) {
      if (other.startDateTime != null) {
        return false;
      }
    } else if (!startDateTime.equals(other.startDateTime)) {
      return false;
    }
    if (endDateTime == null) {
      if (other.endDateTime != null) {
        return false;
      }
    } else if (!endDateTime.equals(other.endDateTime)) {
      return false;
    }
    return true;
  }

}
