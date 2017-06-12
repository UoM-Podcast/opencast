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

import org.opencastproject.pm.syllabus.api.Occurrence;
import org.opencastproject.pm.syllabus.api.VLocation;

import net.jcip.annotations.ThreadSafe;

import org.joda.time.DateTime;
import org.joda.time.Interval;

@ThreadSafe
public class OccurrenceImpl implements Occurrence {
  private final String activityId;
  private final String activityName;
  private final VLocation location;
  private final Interval time;
  private final DateTime lastModified;

  public OccurrenceImpl(String activityId,
                        String activityName,
                        VLocationImpl location,
                        Interval time,
                        DateTime lastChanged) {
    this.activityId = activityId;
    this.activityName = activityName;
    this.location = location;
    this.time = time;
    this.lastModified = lastChanged;
  }

  @Override public String getActivityId() {
    return activityId;
  }

  @Override public String getActivityName() {
    return activityName;
  }

  @Override public VLocation getLocation() {
    return location;
  }

  @Override public Interval getTime() {
    return time;
  }

  @Override public DateTime getLastChanged() {
    return lastModified;
  }
}
