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
import static org.opencastproject.pm.syllabus.impl.scheduling.AccessorFunctions.VModuleF;
import static org.opencastproject.pm.syllabus.impl.scheduling.AccessorFunctions.VStaffF;
import static org.opencastproject.pm.syllabus.impl.scheduling.AccessorFunctions.VZonesF;
import static org.opencastproject.util.data.Collections.asMap;
import static org.opencastproject.util.data.Collections.groupBy;
import static org.opencastproject.util.data.Collections.grouped;
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
import org.opencastproject.util.data.Tuple;
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

/** This data type holds data fetched from the S+ database together with some helper functions. */
public abstract class SyllabusData {
  private static final Logger logger = LoggerFactory.getLogger(SyllabusData.class);

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

  public abstract String getCaptureAgentSuitabilityId(VLocation location);

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
    // location id and suitability id, filtered by capture agent room type ids
    final Map<String, String> locationSuitability = new HashMap<>();
    {
      List<VLocationSuitability> caLocations = new ArrayList<>();
      for (String roomIdWithCa : syl.getCaptureRoomTypeIDs()) {
        caLocations.addAll(syl.findLocationSuitability(roomIdWithCa));
      }
      makeMap(locationSuitability, caLocations, toLocationSuitabilityIds);
     }

    logger.debug("# locations featuring capture agents " + locationSuitability.size());
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

      @Override public String getCaptureAgentSuitabilityId(VLocation location) {
        return locationSuitability.get(location.getId());
      }
    };
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

      @Override public String getCaptureAgentSuitabilityId(VLocation location) {
        return null;
      }
    };
  }

  private static <A> Map<String, A> mapById(List<A> as, Function<A, String> id, String name) {
    logger.debug("# " + name + " " + as.size());
    return asMap(as, id);
  }

  private static <A> Multimap<String, A> multimapById(List<A> as, Function<A, String> id, String name) {
    logger.debug("# " + name + " " + as.size());
    return groupBy(ArrayListMultimap.<String, A>create(), as, id);
  }

  public static <K, V, X> Map<K, V> makeMap(Map<K, V> map,
                                            Iterable<? extends X> values,
                                            Function<? super X, Tuple<K, V>> group) {
    for (X value : values) {
      final Tuple<K, V> entry = group.apply(value);
      map.put(entry.getA(), entry.getB());
    }
    return map;
  }

  private static final Function<VLocationSuitability, Tuple<String, String>> toLocationSuitabilityIds =
          new Function<VLocationSuitability, Tuple<String, String>>() {
    @Override
    public Tuple<String, String> apply(VLocationSuitability s) {
      return new Tuple(s.getLocationId(), s.getSuitabilityId());
    }
  };
}
