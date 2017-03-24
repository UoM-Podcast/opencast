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

package org.opencastproject.pm.syllabus.api;

import org.joda.time.DateTime;
import org.joda.time.Interval;

/**
 * A manifestation of an {@link VActivity} in space and time.
 * No representation in S+.
 */
public interface Occurrence {
  /** Return the ID of the activity. */
  String getActivityId();

  /** Return the name of the activity. */
  String getActivityName();

  /** Return the location where this occurrence takes place. */
  VLocation getLocation();

  /** Return the time span when this occurrence happens. */
  Interval getTime();

  /**
   * Return the time of latest modification. This time is calculated by taking into
   * account all relevant S+ database tables that may lead to a modification of this
   * particular occurrence. The time returned is the latest of those.
   */
  DateTime getLastChanged();
}
