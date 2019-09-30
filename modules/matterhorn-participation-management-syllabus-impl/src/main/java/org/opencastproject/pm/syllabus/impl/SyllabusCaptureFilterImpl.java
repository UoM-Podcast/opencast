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
import static org.opencastproject.util.OsgiUtil.getCfg;
import static org.opencastproject.util.OsgiUtil.getOptCfg;

import org.opencastproject.pm.syllabus.api.SyllabusCaptureFilter;
import org.opencastproject.pm.syllabus.api.VActivity;
import org.opencastproject.pm.syllabus.api.VLocationSuitability;
import org.opencastproject.util.data.Option;

import org.osgi.service.cm.ConfigurationException;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SyllabusCaptureFilterImpl implements SyllabusCaptureFilter {
  // filter properties
  private static final String CAPTURE_ROOMS_PROPERTY = "capture.rooms";
  private static final String CAPTURE_ROOM_PROPERTY = "capture.room";
  private static final String[] CAPTURE_ROOM_PROPS = {"name", "id", "inputs"};

  private static final String CAPTURE_TYPES_PROPERTY = "capture.types";
  private static final String CAPTURE_TYPE_PROPERTY = "capture.type";
  private static final String[] CAPTURE_TYPE_PROPS = {"name", "id"};

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

  @Override
  public void updateProperties(Dictionary properties) throws ConfigurationException {
    final String rooms = getCfg(properties, CAPTURE_ROOMS_PROPERTY);

    for (String room : rooms.split(",")) {
      for (String prop : CAPTURE_ROOM_PROPS) {
        Option<String> value = getOptCfg(properties, CAPTURE_ROOM_PROPERTY + "." + room.trim() + "." + prop);
        if (value.isSome()) {
          addCaptureRoomProperty(room.trim(), prop, value.get());
        }
      }
    }
    final Option<String> typesOption = getOptCfg(properties, CAPTURE_TYPES_PROPERTY);
    if (typesOption.isSome()) {
      String types = typesOption.get();
      for (String type : types.split(",")) {
        for (String prop : CAPTURE_TYPE_PROPS) {
          Option<String> value = getOptCfg(properties, CAPTURE_TYPE_PROPERTY + "." + type.trim() + "." + prop);
          if (value.isSome()) {
            addCaptureActivityTypeProperty(type.trim(), prop, value.get());
          }
        }
      }
    } else {
      addCaptureActivityTypeProperty("ANY", "id", ACTIVITY_TYPE_ANY);
    }
  }
}
