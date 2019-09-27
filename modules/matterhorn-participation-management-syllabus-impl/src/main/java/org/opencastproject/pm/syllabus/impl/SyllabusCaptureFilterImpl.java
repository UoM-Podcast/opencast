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

import static org.opencastproject.pm.syllabus.api.SyllabusService.ACTIVITY_TYPE_ANY;

import org.opencastproject.pm.syllabus.api.SyllabusCaptureFilter;
import org.opencastproject.pm.syllabus.api.VActivity;
import org.opencastproject.pm.syllabus.api.VLocationSuitability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SyllabusCaptureFilterImpl implements SyllabusCaptureFilter {

  // Data to filter S+
  private final Map<String, Map<String, String>> captureRooms = new HashMap<>();
  private final Map<String, Map<String, String>> captureActivityTypes = new HashMap<>();

  @Override
  public void addCaptureRoomProperty(final String captureRoom, String property, String value) {
    Map<String, String> loc = captureRooms.get(captureRoom);
    if (null == loc) {
      loc = new HashMap<>();
      captureRooms.put(captureRoom, loc);
    }
    loc.put(property, value);
  }

  @Override
  public void addCaptureActivityTypeProperty(final String activityType, String property, String value) {
    Map<String, String> type = captureActivityTypes.get(activityType);
    if (null == type) {
      type = new HashMap<>();
      captureActivityTypes.put(activityType, type);
    }
    type.put(property, value);
  }


  @Override
  public Map<String, Map<String, String>> getCaptureRooms() {
    return captureRooms;
  }

  @Override
  public List<String> getCaptureRoomTypeIDs() {
    List<String> locIds = new ArrayList<>();
    for (Map<String, String> location : captureRooms.values()) {
      String locId = location.get("id");
      if (null != locId) {
        locIds.add(locId);
      }
    }
    return locIds;
  }

  @Override
  public List<String> getCaptureActivityTypeIDs() {
    List<String> activityIds = new ArrayList<>();
    for (Map<String, String> activity : captureActivityTypes.values()) {
      String acId = activity.get("id");
      if (null != acId) {
        activityIds.add(acId);
      }
    }
    return activityIds;
  }


  @Override
  public boolean isCaptureActivityType(VActivity activity) {
    if (getCaptureActivityTypeIDs().contains(ACTIVITY_TYPE_ANY)) {
        return true;
    }
    return getCaptureActivityTypeIDs().contains(activity.getActivityTypeId());
  }

  @Override
  public boolean hasCaptureAgent(VLocationSuitability suitability) {
    return getCaptureRoomTypeIDs().contains(suitability.getSuitabilityId());
  }

}
