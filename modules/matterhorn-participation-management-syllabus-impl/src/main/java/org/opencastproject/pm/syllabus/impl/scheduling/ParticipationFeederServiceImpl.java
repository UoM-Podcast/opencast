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

import static org.opencastproject.util.OsgiUtil.getCfg;
import static org.opencastproject.util.OsgiUtil.getOptCfg;
import static org.opencastproject.util.OsgiUtil.showConfig;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.Tuple.tuple;
import static org.opencastproject.util.data.VCell.ocell;
import static org.opencastproject.util.data.functions.Strings.toBool;

import org.opencastproject.pm.api.Course;
import org.opencastproject.pm.api.Synchronization;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabaseException;
import org.opencastproject.pm.api.scheduling.ParticipationFeederService;
import org.opencastproject.pm.api.util.RequirementManager;
import org.opencastproject.pm.syllabus.api.SyllabusService;
import org.opencastproject.pm.syllabus.api.VActivity;
import org.opencastproject.pm.syllabus.api.VModule;
import org.opencastproject.requirement.api.RequirementService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.util.SecurityContext;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.VCell;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.logging.Level;

import javax.ws.rs.core.Response;

/** Connects the {@link ParticipationFeederRunner} to the OSGi environment. */
public class ParticipationFeederServiceImpl implements ManagedService, ParticipationFeederService {

  private static final String CAPTURE_ROOMS_PROPERTY = "capture.rooms";
  private static final String CAPTURE_ROOM_PROPERTY = "capture.room";
  private static final String[] CAPTURE_ROOM_PROPS = {"name","id","inputs"};

  private static final String CAPTURE_TYPES_PROPERTY = "capture.types";
  private static final String CAPTURE_TYPE_PROPERTY = "capture.type";
  private static final String[] CAPTURE_TYPE_PROPS = {"name","id"};

  public static final boolean DEFAULT_RUN_ON_START = false;
  public static final boolean DEFAULT_SCHEDULE = false;

  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(ParticipationFeederRunner.class);

  // Dependencies

  private SyllabusService syllabusService;
  private ParticipationManagementDatabase persistence;
  private SecurityService securityService;
  private OrganizationDirectoryService organizationDirectoryService;
  private RequirementService requirementService = null;
  private RequirementManager requirementManager;

  private String systemUser;

  private final VCell<Option<SecurityContext>> secCtx = ocell();

  private ParticipationFeederRunner runner;

  /** OSGi container callback. */
  public void setSyllabusService(SyllabusService syllabusService) {
    this.syllabusService = syllabusService;
  }

  /** OSGi container callback. */
  public void setPersistence(ParticipationManagementDatabase persistence) {
    this.persistence = persistence;
  }

  /** OSGi container callback. */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi container callback. */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectory) {
    this.organizationDirectoryService = organizationDirectory;
  }

  /** OSGi container callback. */
  public void setRequirementService(RequirementService requirementService) {
    this.requirementService = requirementService;
  }

  /** OSGi container callback. */
  public synchronized void activate(final ComponentContext cc) {
    logger.info("Start participation management feeder");
    requirementManager = new DassRequirementManager(requirementService, persistence);
    systemUser = cc.getBundleContext().getProperty(SecurityUtil.PROPERTY_KEY_SYS_USER);
    runner = new ParticipationFeederRunner(syllabusService, persistence, requirementManager, secCtx);
  }

  /** OSGi container callback. */
  public synchronized void deactivate(ComponentContext cc) {
    logger.info("Stop participation management feeder");
    runner.shutdown();
  }

  @Override
  public Response permitHarvest() {
    try {
      final Synchronization sync = persistence.getLastSynchronization();
      if (null == sync) {
        Response.ResponseBuilder respb = Response.status(Response.Status.PRECONDITION_FAILED);
        respb.type("Can't trigger initial harvest externally, try harvesting through UI!");
        return respb.build();
      }

    } catch (ParticipationManagementDatabaseException ex) {
      java.util.logging.Logger.getLogger(ParticipationFeederServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
      Response.ResponseBuilder respb = Response.status(Response.Status.SERVICE_UNAVAILABLE);
      respb.type("DB Problem");
      return respb.build();
    }
    return Response.ok().build();
  }

  @Override
  public void harvest() {
    runner.trigger();
  }

  protected void waitForSync(final Synchronization sync) {
    SecurityContext securityContext = secCtx.get().get();
    securityContext.runInContext(new Effect0() {
      @Override
      public void run() {
        while (sync != null && (runner.isRunning())) {
          try {
            Thread.sleep(0);
          } catch (InterruptedException ignore) {
          }
        }
      }
    });
  }

  @Override
  public Response getHarvestStats() throws NotFoundException {

    try {
      final Synchronization sync = persistence.getLastSynchronization();
      waitForSync(sync);
      HarvestStats stats = runner.getLastHarvestStats();
      long storedTotalRecordings = persistence.countTotalRecordings();
      int totalSplusRecordings = stats.getTotal();
      if ((storedTotalRecordings - totalSplusRecordings) > -100) {
        Response.ResponseBuilder respb = Response.status(Response.Status.PRECONDITION_FAILED);
        respb.type("Trying to delete too many Recordings -- please verify");
        return respb.build();
      }

    } catch (ParticipationManagementDatabaseException ex) {
      java.util.logging.Logger.getLogger(ParticipationFeederServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
    }
    return Response.ok().build();
  }

  @Override
  public Course getCourseByActivityId(String activityId) throws NotFoundException {
    final SyllabusData data = SyllabusData.fetchModules(syllabusService);
    final ModuleFinder moduleFinder = new ModuleFinder(data.getModule(), data.getActivityParent(), data.getActivity());
    final VActivity activity = data.getActivity().get(activityId);

    if (activity == null) {
      throw(new NotFoundException());
    }

    // Assume activity is a child node
    final List<VActivity> activityHierarchy = moduleFinder.collectUntilModule(activity);
    final List<VModule> modules = moduleFinder.getModules(activityHierarchy);
    Course course = ParticipationFeederRunner.mergeModules(modules);
    course.setSeriesId(course.createSeriesId());

    return course;
  }

  public void setParticipationProperties(Dictionary properties) throws ConfigurationException {
        final String rooms = getCfg(properties, CAPTURE_ROOMS_PROPERTY);

        for (String room : rooms.split(",")) {
            for (String prop : CAPTURE_ROOM_PROPS) {
                Option<String> value = getOptCfg(properties, CAPTURE_ROOM_PROPERTY + "." + room.trim() + "." + prop);
                if (value.isSome()) {
                    syllabusService.addCaptureRoomProperty(room.trim(), prop, value.get());
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
                        syllabusService.addCaptureActivityTypeProperty(type.trim(), prop, value.get());
                    }
                }
            }
        } else {
            syllabusService.addCaptureActivityTypeProperty("ANY", "id", syllabusService.ACTIVITY_TYPE_ANY);
        }
  }

  @Override
  public Course getCourseByCourseKey(String courseKey) throws NotFoundException {
    final List<VModule> modules = new ArrayList<VModule>();
    final VModule module = syllabusService.getModuleByCourseKey(courseKey);

    if (module != null) {
      modules.add(module);
    } else {
      throw (new NotFoundException());
    }

    Course course = ParticipationFeederRunner.mergeModules(modules);
    course.setSeriesId(course.createSeriesId());

    return course;
  }

  @Override
  public List<String> getActivityIds(String courseKey) throws NotFoundException {
    return syllabusService.findModuleActivityIdsByCourseKey(courseKey);
  }

  /** OSGi container called (ConfigurationAdmin). */
  @Override
  public synchronized void updated(Dictionary properties) throws ConfigurationException {
    if (properties != null) {
      // read config
      final String cron = getCfg(properties, "cron");
      final String orgId = getCfg(properties, "organization");
      final boolean runOnStart = getOptCfg(properties, "run-on-start").map(toBool).getOrElse(DEFAULT_RUN_ON_START);
      final boolean schedule = getOptCfg(properties, "schedule").map(toBool).getOrElse(DEFAULT_SCHEDULE);

      setParticipationProperties(properties);

      // create security context
      final Organization org;
      try {
        org = organizationDirectoryService.getOrganization(orgId);
      } catch (NotFoundException e) {
        throw new ConfigurationException("organization", "not found", e);
      }
      secCtx.set(some(new SecurityContext(securityService, org, SecurityUtil.createSystemUser(systemUser, org))));
      //
      if (schedule)
        runner.schedule(cron);

      if (runOnStart)
        runner.trigger();
      //
      logger.info(showConfig(tuple("cron", cron), tuple("organization", org), tuple("run-on-start", runOnStart),
              tuple("schedule", schedule)));
    }
  }
}
