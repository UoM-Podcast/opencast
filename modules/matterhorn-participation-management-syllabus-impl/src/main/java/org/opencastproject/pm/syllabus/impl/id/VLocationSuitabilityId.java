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
public class VLocationSuitabilityId implements Serializable {

  private String locationId;
  private String suitabilityId;

  public VLocationSuitabilityId() {

  }

  public VLocationSuitabilityId(String locationId, String suitabilityId) {
    this.locationId = locationId;
    this.suitabilityId = suitabilityId;
  }

  public String getLocationId() {
    return locationId;
  }

  public String getSuitabilityId() {
    return suitabilityId;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
            + ((locationId == null) ? 0 : locationId.hashCode());
    result = prime * result + ((suitabilityId == null) ? 0 : suitabilityId.hashCode());
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
    VLocationSuitabilityId other = (VLocationSuitabilityId) obj;
    if (locationId == null) {
      if (other.locationId != null) {
        return false;
      }
    } else if (!locationId.equals(other.locationId)) {
      return false;
    }
    if (suitabilityId == null) {
      if (other.suitabilityId != null) {
        return false;
      }
    } else if (!suitabilityId.equals(other.suitabilityId)) {
      return false;
    }
    return true;
  }

}
