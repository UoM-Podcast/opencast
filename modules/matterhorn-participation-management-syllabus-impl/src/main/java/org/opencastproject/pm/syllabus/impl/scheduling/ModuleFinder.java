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

import static java.lang.String.format;
import static org.opencastproject.pm.syllabus.impl.scheduling.AccessorFunctions.VActivityParentsF;
import static org.opencastproject.util.data.Collections.cons;
import static org.opencastproject.util.data.Collections.getMap;
import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

import org.opencastproject.pm.syllabus.api.VActivity;
import org.opencastproject.pm.syllabus.api.VActivityParents;
import org.opencastproject.pm.syllabus.api.VModule;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

import com.google.common.collect.Multimap;

import org.apache.commons.lang.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/** Search the activity hierarchy to find a module where the activity belongs to. */
final class ModuleFinder {
  private static final Logger logger = LoggerFactory.getLogger(ModuleFinder.class);
  // activityId -> VModule
  private final Map<String, VModule> moduleMap;
  // activityId -> VActivityParents
  private final Multimap<String, VActivityParents> parentsMap;
  // activityId -> VActivity
  private final Map<String, VActivity> activityMap;
//  private final Cache<String, List<VModule>> cache = Caches.lru(100, 30 * 60000);

  ModuleFinder(Map<String, VModule> moduleMap,
               Multimap<String, VActivityParents> parentsMap,
               Map<String, VActivity> activityMap) {
    this.moduleMap = moduleMap;
    this.parentsMap = parentsMap;
    this.activityMap = activityMap;
  }

//  /** Find all modules the activity belongs to. */
//  public List<VModule> findModules(final VActivity activity, SLogger log) {
//    return cache.get(activity.getId(), findModulesF(log, activity, activity));
//  }
//
//  /** Return a module if the activity belongs to exactly one module. */
//  public Option<VModule> findModule(VActivity activity, SLogger log) {
//    return mlist(cache.get(activity.getId(), findModulesF(log, activity, activity))).headOpt();
//  }
//
//  private Function<String, List<VModule>> findModulesF(final SLogger log, final VActivity current,
//                                                       final VActivity base) {
//    return new Function<String, List<VModule>>() {
//      @Override public List<VModule> apply(String activityId) {
//        final List<VModule> modules = findModules(log, current, base);
//        if (modules.size() > 1) {
//          log.log().warn(format("Activity %s is associated with multiple modules (%d) which is not supported.",
//                                activityId, modules.size()));
//        }
//        return modules;
//      }
//    };
//  }
//
//  /** Find all module the activity belongs to. */
//  private List<VModule> findModules(final SLogger log, final VActivity current, final VActivity base) {
//    final String mId = current.getModuleId();
//    if (mId != null) {
//      // activity belongs to a module
//      final VModule module = moduleMap.get(mId);
//      if (module != null && !StringUtils.isBlank(module.getName())) {
//        if (eq(base.getId(), current.getId())) {
//          log.log().info(format("Activity %s belongs to module %s.", base.getId(), module.getName()));
//        } else {
//          log.log().info(format("Activity %s -> %s belongs to module %s.",
//                                base.getId(), current.getId(), module.getName()));
//        }
//        return list(module);
//      } else {
//        // module cannot be found in module map
//        log.logInconsistency("module", mId);
//        return nil();
//      }
//    } else {
//      // activity does not belong to a module
//      final Collection<VActivityParents> parents = parentsMap.get(current.getId());
//      if (!parents.isEmpty()) {
//        // activity has parents
//        log.log().debug(format("Activity %s -> %s has %d parent/s.",
//                               base.getId(), current.getId(), parents.size()));
//        return mlist(parents).map(AccessorFunctions.VActivityParentsF.getParentId)
//                .bind(new Function<String, List<VModule>>() {
//                  @Override public List<VModule> apply(String aId) {
//                    final VActivity parent = activityMap.get(aId);
//                    if (parent != null) {
//                      return findModules(log, parent, base);
//                    } else {
//                      log.logInconsistency("activity", aId);
//                      return nil();
//                    }
//                  }
//                }).value();
//      } else {
//        log.log().warn(format("Activity %s -> %s is not associated to a module and does not have any parents.",
//                              base.getId(), current.getId()));
//        return nil();
//      }
//    }
//  }

  public List<VModule> getModules(List<VActivity> activities) {
    return mlist(activities).bind(getModuleF).value();
  }

  /**
   * Collect all activities up the hierarchy until a parent activity associated to a module is found.
   * The original activity is also included in the list as the very first element so that at least a list
   * with one activity is returned.
   */
  public List<VActivity> collectUntilModule(final VActivity a) {
    if (hasModule(a)) {
      return list(a);
    } else {
      final List<VActivity> parents = mlist(parentsMap.get(a.getId()))
              .map(VActivityParentsF.getParentId)
              .bind(getMap(activityMap))
              .bind(collectUntilModuleF).value();
      if (parents.size() > 0) {
        logger.debug(format("Collected %d parent activities of activity %s", parents.size(), a.getId()));
      }
      return cons(a, parents);
    }
  }

  public Option<VModule> getModule(VActivity a) {
    final VModule m = moduleMap.get(a.getModuleId());
    if (m != null && StringUtils.isNotBlank(m.getName())) {
      return some(m);
    } else {
      return none();
    }
  }

  private final Function<VActivity, Option<VModule>> getModuleF = new Function<VActivity, Option<VModule>>() {
    @Override public Option<VModule> apply(VActivity activity) {
      return getModule(activity);
    }
  };

  /** Check if activity <code>a</code> is associated to a module. */
  public boolean hasModule(VActivity a) {
    return getModule(a).isSome();
  }

  private final Function<VActivity, List<VActivity>> collectUntilModuleF = new Function<VActivity, List<VActivity>>() {
    @Override public List<VActivity> apply(VActivity a) {
      return collectUntilModule(a);
    }
  };
}
