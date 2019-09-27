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

import org.opencastproject.pm.syllabus.api.SyllabusCaptureFilter;
import org.opencastproject.pm.syllabus.api.SyllabusData;
import org.opencastproject.pm.syllabus.api.SyllabusService;
import org.opencastproject.pm.syllabus.api.VActivity;
import org.opencastproject.pm.syllabus.api.VLocationSuitability;
import org.opencastproject.pm.syllabus.impl.scheduling.AccessorFunctions;
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
public class SyllabusDataImpl extends SyllabusData {
  private static final Logger logger = LoggerFactory.getLogger(SyllabusDataImpl.class);

  /** Fetches data from the S+ database. */
  public void fetch(final SyllabusService syllabus, final SyllabusCaptureFilter rooms) {

    // get Actvities, ActivityParents, and Modules
    fetchModules(syllabus);

    // location id and suitability id, filtered by capture agent room type ids
    this.setLocation(mapById(syllabus.findLocations(), VLocationF.getId, "location"));
    Map<String, String> locationSuitability = new HashMap<>();
    {
      List<VLocationSuitability> caLocations = new ArrayList<>();
      for (String roomIdWithCa : rooms.getCaptureRoomTypeIDs()) {
        caLocations.addAll(syllabus.findLocationSuitability(roomIdWithCa));
      }
      makeMap(locationSuitability, caLocations, toLocationSuitabilityIds);
    }
    this.setLocationSuitability(locationSuitability);

    logger.debug("# locations featuring capture agents " + locationSuitability.size());

    this.setActivityLocation(multimapById(syllabus.findActivityLocation(),
            VActivityLocationF.getActivityId, "activityLocation"));
    this.setZones(mapById(syllabus.findZones(), VZonesF.getId, "zones"));
    this.setStaff(mapById(syllabus.findStaff(), VStaffF.getId, "staff"));
    this.setActivityStaff(multimapById(syllabus.findActivityStaff(),
            VActivityStaffF.getActivityId, "activityStaff"));
    this.setDepartment(mapById(syllabus.findDepartment(), VDepartmentF.getId, "department"));
    this.setSourceDescription(syllabus.getSourceDescription());
  }

  // fetch only data relating modules and activities
  public void fetchModules(final SyllabusService syllabus) {
    {
      // Partition the activity list and fetch V_ACTIVITY_DATETIME
      // in partitions.
      //
      // Put this in a separate method or like this so that the garbage collector can
      // dispose the original list to safe heap space.
      // Drain list into new list since the returned one is immutable
      final List<VActivity> a = new ArrayList<>(syllabus.findActivities());
      // Sort activities by id ascending. Sorting is crucial otherwise the
      // partition approach does not work. Matching dateTime entities are fetched
      // based an lexicographic id ordering
      sort(a, new Comparator<VActivity>() {
        @Override public int compare(VActivity a, VActivity b) {
          return a.getId().compareTo(b.getId());
        }
      });
      logger.debug("# activity " + a.size());
      this.setActivityPartitioned(grouped(a, 1000));
    }
    this.setActivity(asMap(
            mlist(this.getActivityPartitioned()).bind(Functions.<List<VActivity>>identity()).value(),
            AccessorFunctions.VActivityF.getId));
    this.setActivityParent(multimapById(syllabus.findActivityParents(),
            VActivityParentsF.getActivityId, "activityParent"));
    this.setModule(mapById(syllabus.findModules(), VModuleF.getId, "module"));
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
