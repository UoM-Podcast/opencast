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

import org.opencastproject.util.data.Option;

import org.joda.time.DateMidnight;

/** Representation of the S+ V_ACTIVITY table. */
public interface VActivity extends SyllabusEntity {
  /** Activity name. */
  String getName();

  /** Activity type ID. */
  String getActivityTypeId();

  /** The ID of the owning department. */
  String getDepartmentId();

  /** The ID of the module. */
  String getModuleId();

  /** Description. */
  Option<String> getDescription();

  /** Return the day of the first scheduled instance. May return <code>none</code> if no occurrences are scheduled. */
  Option<DateMidnight> getStartDate();

  /** Return the day of the last scheduled instance. May return <code>none</code> if no occurrences are scheduled. */
  Option<DateMidnight> getEndDate();

  /** Return if this activity is currently scheduled. */
  boolean isScheduled();
}
