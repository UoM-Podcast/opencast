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
public class VActivityEquipmentId implements Serializable {

  private String activityId;
  private String equipmentId;

  public VActivityEquipmentId() {

  }

  public VActivityEquipmentId(String activityId, String equipmentId) {
    this.activityId = activityId;
    this.equipmentId = equipmentId;
  }

  public String getActivityId() {
    return activityId;
  }

  public String getEquipmentId() {
    return equipmentId;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
            + ((activityId == null) ? 0 : activityId.hashCode());
    result = prime * result + ((equipmentId == null) ? 0 : equipmentId.hashCode());
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
    VActivityEquipmentId other = (VActivityEquipmentId) obj;
    if (activityId == null) {
      if (other.activityId != null) {
        return false;
      }
    } else if (!activityId.equals(other.activityId)) {
      return false;
    }
    if (equipmentId == null) {
      if (other.equipmentId != null) {
        return false;
      }
    } else if (!equipmentId.equals(other.equipmentId)) {
      return false;
    }
    return true;
  }

}
