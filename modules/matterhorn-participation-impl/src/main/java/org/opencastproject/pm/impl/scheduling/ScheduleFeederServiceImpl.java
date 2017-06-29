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

package org.opencastproject.pm.impl.scheduling;

import static org.opencastproject.util.DateTimeSupport.toNextWeekDay;
import static org.opencastproject.util.OsgiUtil.getCfg;
import static org.opencastproject.util.OsgiUtil.getOptCfg;
import static org.opencastproject.util.OsgiUtil.showConfig;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.Tuple.tuple;
import static org.opencastproject.util.data.VCell.cell;
import static org.opencastproject.util.data.VCell.ocell;
import static org.opencastproject.util.data.functions.Strings.toBool;
import static org.opencastproject.util.data.functions.Strings.toInt;

import org.opencastproject.pm.api.Course;
import org.opencastproject.pm.api.Recording;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabaseException;
import org.opencastproject.pm.api.persistence.RecordingQuery;
import org.opencastproject.pm.api.scheduling.ParticipationManagementSchedulingException;
import org.opencastproject.pm.api.scheduling.ScheduleFeederService;
import org.opencastproject.pm.api.scheduling.ScheduleProvider;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.util.SecurityContext;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Cell;
import org.opencastproject.util.data.Collections;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.data.VCell;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Connects the {@link ScheduleFeederRunner} to the OSGi environment. */
public class ScheduleFeederServiceImpl implements ManagedService, ScheduleFeederService {

  public static final boolean DEFAULT_RUN_ON_START = false;
  public static final boolean DEFAULT_SCHEDULE = false;
  public static final Integer DEFAULT_START_MARGIN = 0; // Default margin in minutes for the start of the schedule
  public static final Integer DEFAULT_END_MARGIN = 0; // Default margin in minutes for the end of the schedule
  public static final boolean DEFAULT_CREATE_NEW_SERIES = true;
  public static final String DEFAULT_WORKFLOW = "ng-schedule-upload";

  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(ScheduleFeederRunner.class);

  /** The configuration key to use for determining the workflow definition to use */
  public static final String WORKFLOW_DEFINITION = "workflow.definition";

  /** The configuration key to use for determining the workflow configuration to use */
  public static final String WORKFLOW_CONFIG = "workflow.config";

  /** Name for the configuration option for upcoming days */
  public static final String OPT_UPCOMING_DAYS = "upcoming.days";

  /** Name for the configuration option for rounding of upcoming days */
  public static final String OPT_UPCOMING_ROUND = "upcoming.round";

  /** Name for the configuration option for when to run the upcoming recordings scheduler */
  public static final String OPT_UPCOMING_UDPATE_SCHEDULE = "upcoming.update";

  /** Name for the configuration option for what the start date is of the sync interval */
  public static final String OPT_SYNC_PAST = "sync.past";

  /** The default number of upcoming days */
  public static final int UPCOMING_DAYS_DEFAULT = -1;

  /** The default setting for whether to sync past recordings or not */
  public static final boolean SYNC_PAST_DEFAULT = false;

  /** The default number of upcoming days */
  public static final boolean UPCOMING_ROUNDING_DEFAULT = true;

  /** The default number of hours that a pre-edit published recording's availability will be delayed by */
  private static final Integer DEFAULT_PREEDIT_AVAILABILITY_DELAY = 24;

  /** The number of days to show in the "upcoming" section of the admin ui */
  protected int upcomingIntervalInDays = UPCOMING_DAYS_DEFAULT;

  /** Whether to include all days up until the following Sunday */
  protected boolean roundUpcoming = UPCOMING_ROUNDING_DEFAULT;

  /** Whether to sync past recordings or only upcoming ones */
  protected boolean syncPast = false;

  // Dependencies
  private SchedulerService schedulerService;
  private SecurityService securityService;
  private OrganizationDirectoryService organizationDirectoryService;
  private ScheduleProvider scheduleProvider;
  private SeriesService seriesService;
  private ParticipationManagementDatabase participationManagementDB;

  private String systemUser;

  private final VCell<Option<SecurityContext>> secCtx = ocell();
  private final VCell<String> workflow = cell(DEFAULT_WORKFLOW);
  private final VCell<HashMap<String, String>> workflowConfigs = cell(new HashMap<String, String>());
  private final VCell<Integer> startMargin = cell(DEFAULT_START_MARGIN);
  private final VCell<Integer> endMargin = cell(DEFAULT_END_MARGIN);
  private final VCell<Boolean> hasToCreateNewSeries = cell(DEFAULT_CREATE_NEW_SERIES);
  private final VCell<Integer> preEditAvailDelay = cell(DEFAULT_PREEDIT_AVAILABILITY_DELAY);
  private final VCell<String> editProperty = cell("");
  private final VCell<String> emailProperty = cell("");
  private Cell<String> testCell;

  private ScheduleFeederRunner runner;

  /** OSGi container callback. */
  public void setSchedulerService(SchedulerService schedulerService) {
    this.schedulerService = schedulerService;
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
  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  /** OSGi container callback. */
  public void setParticipationManagementDatabase(ParticipationManagementDatabase participationManagementDB) {
    this.participationManagementDB = participationManagementDB;
  }

  /** OSGi container callback. */
  public synchronized void activate(final ComponentContext cc) {
    scheduleProvider = new ParticipationManagementProvider(participationManagementDB, seriesService, securityService,
            hasToCreateNewSeries.get(), startMargin, endMargin);
    logger.info("Start schedule feeder using provider {}", scheduleProvider);
    systemUser = cc.getBundleContext().getProperty(SecurityUtil.PROPERTY_KEY_SYS_USER);
    runner = new ScheduleFeederRunner(this, schedulerService, participationManagementDB, scheduleProvider,
            secCtx, workflow, workflowConfigs, editProperty, emailProperty, preEditAvailDelay);
  }

  /** OSGi container callback. */
  public synchronized void deactivate(ComponentContext cc) {
    logger.info("Stop schedule feeder");
    runner.shutdown();
  }

  /** OSGi container called (ConfigurationAdmin). */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public synchronized void updated(Dictionary properties) throws ConfigurationException {
    if (properties != null) {
      // read configuration
      final String cron = getCfg(properties, "cron");
      final String wf = getCfg(properties, WORKFLOW_DEFINITION);
      final String orgId = getCfg(properties, "organization");
      final boolean hasToCreateNewSeries = getOptCfg(properties, "create-new-series").map(toBool).getOrElse(
              DEFAULT_CREATE_NEW_SERIES);
      final boolean runOnStart = getOptCfg(properties, "run-on-start").map(toBool).getOrElse(DEFAULT_RUN_ON_START);
      final boolean schedule = getOptCfg(properties, "schedule").map(toBool).getOrElse(DEFAULT_SCHEDULE);
      final Integer startMargin = toInt.apply(getCfg(properties, "start-margin")).getOrElse(DEFAULT_START_MARGIN);
      final Integer endMargin = toInt.apply(getCfg(properties, "end-margin")).getOrElse(DEFAULT_END_MARGIN);

      final HashMap<String, String> wfCfg = new HashMap<String, String>(getWfCfgAsMap(properties, WORKFLOW_CONFIG));

      final String editProp = getCfg(properties, "workflow.property.edit");
      final String emailProp = getCfg(properties, "workflow.property.email");

      // configure
      this.workflow.set(wf);
      this.workflowConfigs.set(wfCfg);
      this.startMargin.set(startMargin);
      this.endMargin.set(endMargin);
      this.hasToCreateNewSeries.set(hasToCreateNewSeries);

      this.editProperty.set(editProp);
      this.emailProperty.set(emailProp);

      // Configure upcoming days
      String optUpcomingDays = (String) properties.get(OPT_UPCOMING_DAYS);
      if (StringUtils.isNotBlank(optUpcomingDays)) {
        try {
          upcomingIntervalInDays = Integer.parseInt(optUpcomingDays);
          logger.info("The number of upcoming days has been set to {}", upcomingIntervalInDays);
        } catch (Exception e) {
          throw new ConfigurationException(OPT_UPCOMING_DAYS, e.getMessage());
        }
      } else {
        upcomingIntervalInDays = UPCOMING_DAYS_DEFAULT;
        logger.info("The number of upcoming days defaults to {}", upcomingIntervalInDays);
      }

      // Configure whether to round the number of upcoming days to the end of the respective week
      String optRoundUpcomingDays = (String) properties.get(OPT_UPCOMING_ROUND);
      if (StringUtils.isNotBlank(optRoundUpcomingDays)) {
        try {
          roundUpcoming = Boolean.parseBoolean(optRoundUpcomingDays);
          if (roundUpcoming)
            logger.info("The number of upcoming days will be rounded to the end of the next weekend");
          else
            logger.info("The number of upcoming days will not be rounded");
        } catch (Exception e) {
          throw new ConfigurationException(OPT_UPCOMING_DAYS, e.getMessage());
        }
      } else {
        roundUpcoming = UPCOMING_ROUNDING_DEFAULT;
        logger.info("The number of upcoming days is rounded by default to the end of the next weekend");
      }

      // Configure the synchronization start date
      syncPast = getOptCfg(properties, OPT_SYNC_PAST).map(toBool).getOrElse(SYNC_PAST_DEFAULT);
      logger.info("Past recordings {} be included in the synchronization", syncPast ? "will" : "won't");

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
      List<Tuple<String, ?>> config = new ArrayList<Tuple<String, ?>>();
      config.add(tuple("cron", cron));
      config.add(tuple("organization", org));
      config.add(tuple("run-on-start", runOnStart));
      config.add(tuple("create-new-series", hasToCreateNewSeries));
      config.add(tuple("schedule", schedule));
      config.add(tuple("start-margin", startMargin));
      config.add(tuple("end-margin", endMargin));
      config.add(tuple("workflow", workflow.get()));
      config.addAll(Collections.toList(wfCfg));
      logger.info(showConfig(config.toArray(new Tuple[config.size()])));
    }
  }

  /**
   * Trigger a new synchronization
   */
  @Override
  public void synchronize() {
    runner.trigger();
  }

  /**
   * Clear all the events scheduled from the participation management in Matterhorn
   *
   * @throws ParticipationManagementSchedulingException
   */
  @Override
  public void clearScheduledEvents() throws ParticipationManagementSchedulingException {
    RecordingQuery recordingQuery = RecordingQuery.createWithoutDeleted();
    recordingQuery.withDeleted();
    recordingQuery.withEvent();
    List<Recording> recordings = null;

    logger.info("Start clearing schedule Events from PM in Matterhorn");

    try {
      recordings = participationManagementDB.findRecordings(recordingQuery);
    } catch (ParticipationManagementDatabaseException e) {
      logger.error("Not able to find recordings!: {}", e);
      throw new ParticipationManagementSchedulingException(e);
    }

    for (Recording r : recordings) {
      if (r.getEventId().isSome()) {
        try {
          schedulerService.removeEvent(r.getEventId().get());
          logger.debug("Event {} deleted", r.getEventId().get());
        } catch (SchedulerException e) {
          logger.error("Not able to delete schedule Event {}: {}", r.getEventId().get(), e);
          throw new ParticipationManagementSchedulingException(e);
        } catch (NotFoundException e) {
          logger.warn("Schedule event {} can not be found in Matterhorn: {}", r.getEventId().get(), e);
          continue;
        } catch (UnauthorizedException e) {
          logger.error("Not able to delete schedule Event {}: {}", r.getEventId().get(), e);
          throw new ParticipationManagementSchedulingException(e);
        }
      }
    }

    logger.info("Clearing schedule Events from PM in Matterhorn finished. {} events deleted.", recordings.size());
  }

  /**
   * Clear all the series created from the participation management in Matterhorn
   *
   * @throws ParticipationManagementSchedulingException
   */
  @Override
  public void clearSeries() throws ParticipationManagementSchedulingException {
    logger.info("Start clearing series in Matterhorn");

    List<Course> courses;
    try {
      courses = participationManagementDB.getCourses();
    } catch (ParticipationManagementDatabaseException e) {
      logger.error("Not able to find courses!: {}", e);
      throw new ParticipationManagementSchedulingException(e);
    }

    for (Course c : courses) {
      if (c.getSeriesId() == null)
        continue;

      try {
        seriesService.deleteSeries(c.getSeriesId());
        logger.debug("Series {} deleted", c.getSeriesId());
      } catch (SeriesException e) {
        logger.error("Not able to delete series {}: {}", c.getSeriesId(), e);
        throw new ParticipationManagementSchedulingException(e);
      } catch (NotFoundException e) {
        logger.warn("Series {} can not be found in Matterhorn: {}", c.getSeriesId(), e);
        continue;
      } catch (UnauthorizedException e) {
        logger.error("Not able to delete series {}: {}", c.getSeriesId(), e);
        throw new ParticipationManagementSchedulingException(e);
      }
    }

    logger.info("Clearing series in Matterhorn finished. {} series deleted", courses.size());
  }

  /**
   * Returns <code>true</code> if the given date is within the configured range of upcoming days.
   *
   * @return <code>true</code> if the date is part of the upcoming date range
   */
  boolean isUpcoming(Date date) {
    return upcomingIntervalInDays <= 0 || date.before(getSynchronizationEndDate());
  }

  /**
   * Returns the instant (inclusive) that marks the beginning of the range for upcoming recordings or <code>null</code>
   * if no such limit has been configured.
   *
   * @return the end date of the upcoming period
   */
  public Date getSynchronizationStartDate() {
    return syncPast ? new Date(0) : new Date();
  }

  /**
   * Returns the instant (exclusive) that marks the end of the range for upcoming recordings or <code>null</code> if no
   * such limit has been configured.
   *
   * @return the end date of the upcoming period
   */
  public Date getSynchronizationEndDate() {
    if (upcomingIntervalInDays <= 0)
      return null;
    final DateTime end = DateTime.now().withTimeAtStartOfDay() // today's midnight
            .plusDays(1 + upcomingIntervalInDays); // +1 -> tomorrow's midnight + interval
    // Add days until the next weekend
    return roundUpcoming ? toNextWeekDay(end, DateTimeConstants.SUNDAY).toDate() : end.toDate();
  }

  public static Map<String, String> getWfCfgAsMap(Dictionary<String, String> d, String key)
          throws ConfigurationException {
    HashMap<String, String> config = new HashMap<String, String>();
    for (Enumeration<String> e = d.keys(); e.hasMoreElements();) {
      String dKey = e.nextElement();
      if (dKey.startsWith(key))
        config.put(dKey.substring(key.length() + 1), d.get(dKey));
    }
    return config;
  }

}
