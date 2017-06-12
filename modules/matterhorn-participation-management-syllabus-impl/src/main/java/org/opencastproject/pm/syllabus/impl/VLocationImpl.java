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

import org.opencastproject.pm.syllabus.api.VLocation;
import org.opencastproject.util.data.Option;

import net.jcip.annotations.ThreadSafe;

import org.joda.time.DateTime;

@ThreadSafe
public class VLocationImpl implements VLocation {
  private String hostKey;
  private String name;
  private String id;
  private DateTime lastChanged;
  private Option<String> zoneId;

  public VLocationImpl(String id, String name, String hostKey, DateTime lastChanged, Option<String> zoneId) {
    this.hostKey = hostKey;
    this.name = name;
    this.id = id;
    this.lastChanged = lastChanged;
    this.zoneId = zoneId;
  }

  @Override
  public String getHostKey() {
    return hostKey;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public DateTime getLastChanged() {
    return lastChanged;
  }

  @Override
  public Option<String> getZoneId() {
    return zoneId;
  }
}
