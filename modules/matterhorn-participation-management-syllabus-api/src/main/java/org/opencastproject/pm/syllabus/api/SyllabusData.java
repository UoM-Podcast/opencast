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

import org.apache.commons.collections.MultiMap;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/** This data type holds data fetched from the S+ database together with some helper functions. */
public abstract class SyllabusData implements Serializable {

  // Data from S+
  private List<List<VActivity>> activityPartitioned;
  private Map<String, VActivity> activity;
  private Map<String, VLocation> location;
  // location id and suitability id, filtered by capture agent room type ids
  private Map<String, String> locationSuitability;

  // MAT-329, Guava Multimap won't deserialize due to ClassLoader issues
  // Apache Commons Collection 3.2.2 doesn't use generic
  private MultiMap activityLocation; //<String, VActivityLocation>
  private MultiMap activityParent; //<String, VActivityParents>
  private Map<String, VZones> zones;
  private Map<String, VStaff> staff;
  private MultiMap activityStaff; // <String, VActivityStaff>
  private Map<String, VDepartment> department;
  private Map<String, VModule> module;
  private String sourceDescription;

  public void setActivityPartitioned(List<List<VActivity>> activityPartitioned) {
    this.activityPartitioned = activityPartitioned;
  }

  public void setActivity(Map<String, VActivity> activity) {
    this.activity = activity;
  }

  public void setLocation(Map<String, VLocation> location) {
    this.location = location;
  }

  public void setLocationSuitability(Map<String, String> locationSuitability) {
    this.locationSuitability = locationSuitability;
  }

  public void setActivityLocation(MultiMap activityLocation) {
    this.activityLocation = activityLocation;
  }

  public void setActivityParent(MultiMap activityParent) {
    this.activityParent = activityParent;
  }

  public void setActivityStaff(MultiMap activityStaff) {
    this.activityStaff = activityStaff;
  }

  public void setZones(Map<String, VZones> zones) {
    this.zones = zones;
  }

  public void setStaff(Map<String, VStaff> staff) {
    this.staff = staff;
  }


  public void setDepartment(Map<String, VDepartment> department) {
    this.department = department;
  }

  public void setModule(Map<String, VModule> module) {
    this.module = module;
  }

  public void setSourceDescription(String sourceDescription) {
    this.sourceDescription = sourceDescription;
  }

  public List<List<VActivity>> getActivityPartitioned() {
    return activityPartitioned;
  }

  public Map<String, VActivity> getActivity() {
    return activity;
  }

  public Map<String, VLocation> getLocation() {
    return location;
  }

  public MultiMap getActivityLocation() {
    return activityLocation;
  }

  public MultiMap getActivityParent() {
    return activityParent;
  }

  public MultiMap getActivityStaff() {
    return activityStaff;
  }

  public Map<String, VZones> getZones() {
    return zones;
  }

  public Map<String, VStaff> getStaff() {
    return staff;
  }

  public Map<String, VDepartment> getDepartment() {
    return department;
  }

  public Map<String, VModule> getModule() {
    return module;
  }

  public String getSourceDescription() {
    return sourceDescription;
  }

  public String getCaptureAgentSuitabilityId(VLocation location) {
    return locationSuitability.get(location.getId());
  }

  // Harvest functions
  public abstract void fetch(SyllabusService service, SyllabusCaptureFilter rooms);

  public abstract void fetchModules(SyllabusService service);
}
