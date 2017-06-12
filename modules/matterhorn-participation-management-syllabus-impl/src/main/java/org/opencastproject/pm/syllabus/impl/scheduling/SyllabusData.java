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

package org.opencastproject.pm.syllabus.impl.scheduling;

import static java.util.Collections.sort;
import static org.opencastproject.pm.syllabus.impl.scheduling.AccessorFunctions.VActivityLocationF;
import static org.opencastproject.pm.syllabus.impl.scheduling.AccessorFunctions.VActivityParentsF;
import static org.opencastproject.pm.syllabus.impl.scheduling.AccessorFunctions.VActivityStaffF;
import static org.opencastproject.pm.syllabus.impl.scheduling.AccessorFunctions.VDepartmentF;
import static org.opencastproject.pm.syllabus.impl.scheduling.AccessorFunctions.VLocationF;
import static org.opencastproject.pm.syllabus.impl.scheduling.AccessorFunctions.VLocationSuitabilityF;
import static org.opencastproject.pm.syllabus.impl.scheduling.AccessorFunctions.VModuleF;
import static org.opencastproject.pm.syllabus.impl.scheduling.AccessorFunctions.VStaffF;
import static org.opencastproject.pm.syllabus.impl.scheduling.AccessorFunctions.VZonesF;
import static org.opencastproject.util.data.Collections.asMap;
import static org.opencastproject.util.data.Collections.groupBy;
import static org.opencastproject.util.data.Collections.grouped;
import static org.opencastproject.util.data.Collections.toSet;
import static org.opencastproject.util.data.Monadics.mlist;

import org.opencastproject.pm.syllabus.api.SyllabusService;
import org.opencastproject.pm.syllabus.api.VActivity;
import org.opencastproject.pm.syllabus.api.VActivityLocation;
import org.opencastproject.pm.syllabus.api.VActivityParents;
import org.opencastproject.pm.syllabus.api.VActivityStaff;
import org.opencastproject.pm.syllabus.api.VDepartment;
import org.opencastproject.pm.syllabus.api.VLocation;
import org.opencastproject.pm.syllabus.api.VLocationSuitability;
import org.opencastproject.pm.syllabus.api.VModule;
import org.opencastproject.pm.syllabus.api.VStaff;
import org.opencastproject.pm.syllabus.api.VZones;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.functions.Functions;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** This data type holds data fetched from the S+ database together with some helper functions. */
public abstract class SyllabusData {
  private static final Logger logger = LoggerFactory.getLogger(SyllabusData.class);

  public static final String ACTIVITY_TYPE_ANY = "**any**";

  private static final Map <String, Map<String,String>> AgentLocation = new HashMap<String, Map <String,String>>();

  private static final Map <String, Map<String,String>> ActivityType = new HashMap<String, Map <String,String>>();

  public abstract List<List<VActivity>> getActivityPartitioned();

  public abstract Map<String, VActivity> getActivity();

  public abstract Map<String, VLocation> getLocation();

  public abstract Multimap<String, VActivityLocation> getActivityLocation();

  public abstract Multimap<String, VActivityParents> getActivityParent();

  public abstract Map<String, VZones> getZones();

  public abstract Map<String, VStaff> getStaff();

  public abstract Multimap<String, VActivityStaff> getActivityStaff();

  public abstract Map<String, VDepartment> getDepartment();

  public abstract Map<String, VModule> getModule();

  public abstract String getSourceDescription();

  public abstract boolean hasCaptureAgent(VLocation location);

  /** Fetches data from the S+ database. */
  public static SyllabusData fetch(final SyllabusService syl) {
    final List<List<VActivity>> activityPartitioned;
    {
      // Slurp some tables into memory. Then partition the activity list and fetch V_ACTIVITY_DATETIME
      // in partitions.
      //
      // Put this in a separate method or like this so that the garbage collector can
      // dispose the original list to safe heap space.
      // Drain list into new list since the returned one is immutable
      final List<VActivity> a = new ArrayList<VActivity>(syl.findActivities());
      // Sort activities by id ascending. Sorting is crucial otherwise the
      // partition approach does not work. Matching dateTime entities are fetched
      // based an lexicographic id ordering
      sort(a, new Comparator<VActivity>() {
        @Override public int compare(VActivity a, VActivity b) {
          return a.getId().compareTo(b.getId());
        }
      });
      logger.debug("# activity " + a.size());
      activityPartitioned = grouped(a, 1000);
    }
    final Map<String, VActivity> activity = asMap(
            mlist(activityPartitioned).bind(Functions.<List<VActivity>>identity()).value(),
            AccessorFunctions.VActivityF.getId);
    final Map<String, VLocation> location = mapById(syl.findLocations(), VLocationF.getId, "location");
    final Set<String> locationHasCaptureAgent;
    {
      List<VLocationSuitability> caLocations = new ArrayList<VLocationSuitability>();
      for (String roomIdWithCa : getAgentLocationID()) {
        caLocations.addAll(syl.findLocationSuitability(roomIdWithCa));
      }
      locationHasCaptureAgent = toSet(mlist(caLocations).map(VLocationSuitabilityF.getLocationId).value());
    }
    logger.debug("# locations featuring capture agents " + locationHasCaptureAgent.size());
    final Multimap<String, VActivityLocation> activityLocation =
            multimapById(syl.findActivityLocation(), VActivityLocationF.getActivityId, "activityLocation");
    final Multimap<String, VActivityParents> activityParent =
            multimapById(syl.findActivityParents(), VActivityParentsF.getActivityId, "activityParent");
    final Map<String, VZones> zones = mapById(syl.findZones(), VZonesF.getId, "zones");
    final Map<String, VStaff> staff = mapById(syl.findStaff(), VStaffF.getId, "staff");
    final Multimap<String, VActivityStaff> activityStaff =
            multimapById(syl.findActivityStaff(), VActivityStaffF.getActivityId, "activityStaff");
    final Map<String, VDepartment> department = mapById(syl.findDepartment(), VDepartmentF.getId, "department");
    final Map<String, VModule> module = mapById(syl.findModules(), VModuleF.getId, "module");
    final String sourceDescription = syl.getSourceDescription();
    //
    return new SyllabusData() {
      @Override public List<List<VActivity>> getActivityPartitioned() {
        return activityPartitioned;
      }

      @Override public Map<String, VActivity> getActivity() {
        return activity;
      }

      @Override public Map<String, VLocation> getLocation() {
        return location;
      }

      @Override public Multimap<String, VActivityLocation> getActivityLocation() {
        return activityLocation;
      }

      @Override public Multimap<String, VActivityParents> getActivityParent() {
        return activityParent;
      }

      @Override public Map<String, VZones> getZones() {
        return zones;
      }

      @Override public Map<String, VStaff> getStaff() {
        return staff;
      }

      @Override public Multimap<String, VActivityStaff> getActivityStaff() {
        return activityStaff;
      }

      @Override public Map<String, VDepartment> getDepartment() {
        return department;
      }

      @Override public Map<String, VModule> getModule() {
        return module;
      }

      @Override  public String getSourceDescription() {
        return sourceDescription;
      };

      @Override public boolean hasCaptureAgent(VLocation location) {
        return locationHasCaptureAgent.contains(location.getId());
      }
    };
  }

    public static List<String> getAgentLocationID() {
        List<String> locIds = new ArrayList<String>();
        for (Map<String,String> location : AgentLocation.values()) {
            String locId = location.get("id");
            if (null != locId) {
                locIds.add(locId);
            }
        }
        return locIds;
    }

    public static List<String> getActivityTypeID() {
        List<String> activityIds = new ArrayList<String>();
        for (Map<String,String> activity : ActivityType.values()) {
            String acId = activity.get("id");
            if (null != acId) {
                activityIds.add(acId);
            }
        }
        return activityIds;
    }

  public static void addAgentLocationProperty(final String agentLocation, String name, String value) {
    HashMap<String,String>loc = (HashMap<String,String>) AgentLocation.get(agentLocation);
    if (null == loc) {
        loc = new HashMap<String,String>();
        AgentLocation.put(agentLocation, loc);
    }
    loc.put(name, value);
  }

  public static void addActivityTypeProperty(final String activityType, String name, String value) {
    HashMap<String,String>type = (HashMap<String,String>) ActivityType.get(activityType);
    if (null == type) {
        type = new HashMap<String,String>();
        ActivityType.put(activityType, type);
    }
    type.put(name, value);
  }

  // fetch only data relating modules and activities
  public static SyllabusData fetchModules(final SyllabusService syl) {
    final List<List<VActivity>> activityPartitioned;
    {
      // Slurp some tables into memory. Then partition the activity list and fetch V_ACTIVITY_DATETIME
      // in partitions.
      //
      // Put this in a separate method or like this so that the garbage collector can
      // dispose the original list to safe heap space.
      // Drain list into new list since the returned one is immutable
      final List<VActivity> a = new ArrayList<VActivity>(syl.findActivities());
      // Sort activities by id ascending. Sorting is crucial otherwise the
      // partition approach does not work. Matching dateTime entities are fetched
      // based an lexicographic id ordering
      sort(a, new Comparator<VActivity>() {
        @Override public int compare(VActivity a, VActivity b) {
          return a.getId().compareTo(b.getId());
        }
      });
      logger.debug("# activity " + a.size());
      activityPartitioned = grouped(a, 1000);
    }
    final Map<String, VActivity> activity = asMap(
            mlist(activityPartitioned).bind(Functions.<List<VActivity>>identity()).value(),
            AccessorFunctions.VActivityF.getId);
    final Multimap<String, VActivityParents> activityParent =
            multimapById(syl.findActivityParents(), VActivityParentsF.getActivityId, "activityParent");
    final Map<String, VModule> module = mapById(syl.findModules(), VModuleF.getId, "module");

    return new SyllabusData() {
      @Override public List<List<VActivity>> getActivityPartitioned() {
        return activityPartitioned;
      }

      @Override public Map<String, VActivity> getActivity() {
        return activity;
      }

      @Override public Map<String, VLocation> getLocation() {
        return null;
      }

      @Override public Multimap<String, VActivityLocation> getActivityLocation() {
        return null;
      }

      @Override public Multimap<String, VActivityParents> getActivityParent() {
        return activityParent;
      }

      @Override public Map<String, VZones> getZones() {
        return null;
      }

      @Override public Map<String, VStaff> getStaff() {
        return null;
      }

      @Override public Multimap<String, VActivityStaff> getActivityStaff() {
        return null;
      }

      @Override public Map<String, VDepartment> getDepartment() {
        return null;
      }

      @Override public Map<String, VModule> getModule() {
        return module;
      }

      @Override  public String getSourceDescription() {
        return null;
      };

      @Override public boolean hasCaptureAgent(VLocation location) {
        return false;
      }
    };
  }
  /** Check if a location features a capture agent. */
  public static boolean hasCaptureAgent(VLocationSuitability locationSuitability) {
    return getAgentLocationID().contains(locationSuitability.getSuitabilityId());
  }

  /** Check if an activity shall be recorded. */
  public static boolean isSubjectToSchedule(VActivity activity) {
    if (getActivityTypeID().contains(ACTIVITY_TYPE_ANY)) {
        return true;
    }
    return getActivityTypeID().contains(activity.getActivityTypeId());
  }

  private static <A> Map<String, A> mapById(List<A> as, Function<A, String> id, String name) {
    logger.debug("# " + name + " " + as.size());
    return asMap(as, id);
  }

  private static <A> Multimap<String, A> multimapById(List<A> as, Function<A, String> id, String name) {
    logger.debug("# " + name + " " + as.size());
    return groupBy(ArrayListMultimap.<String, A>create(), as, id);
  }
}
