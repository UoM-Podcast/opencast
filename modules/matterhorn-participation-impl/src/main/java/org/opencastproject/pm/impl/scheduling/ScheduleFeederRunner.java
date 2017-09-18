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

import static java.lang.String.format;
import static org.opencastproject.capture.CaptureParameters.INGEST_WORKFLOW_DEFINITION;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Tuple.tuple;
import static org.opencastproject.util.data.functions.Strings.toLong;

import org.opencastproject.metadata.dublincore.DCMIPeriod;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.Precision;
import org.opencastproject.pm.api.Course;
import org.opencastproject.pm.api.Person;
import org.opencastproject.pm.api.Recording;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabaseException;
import org.opencastproject.pm.api.scheduling.ParticipationManagementSchedulingException;
import org.opencastproject.pm.api.scheduling.Schedule;
import org.opencastproject.pm.api.scheduling.ScheduleProvider;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.scheduler.api.SchedulerQuery;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.util.SecurityContext;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Cell;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * The schedule feeder feeds scheduling data from external data sources into Matterhorn.
 * <p/>
 * The current approach is as follows:
 * <ul>
 * <li>query external data source, then if successful:</li>
 * <li>drop <em>all</em> scheduling data in Matterhorn</li>
 * <li>feed new scheduling data into Matterhorn</li>
 * <li>update series and their ACLs</li>
 * </ul>
 *
 * @see org.opencastproject.scheduler.api.SchedulerService
 */
public class ScheduleFeederRunner {

  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(ScheduleFeederRunner.class);

  private static final String WORKFLOW_CONFIG_PREFIX = "org.opencastproject.workflow.config.";
  private static final String JOB_NAME = "feeder";
  private static final String JOB_GROUP = "mh-schedule-feeder";
  private static final String TRIGGER_NAME = "cron";
  private static final String TRIGGER_GROUP = "mh-schedule-feeder";
  private static final String JOB_PARAM_PARENT = "parent";

  private final ScheduleFeederServiceImpl scheduleFeederService;
  private final SchedulerService schedulerService;
  private final ScheduleProvider scheduleProvider;
  private final ParticipationManagementDatabase participationManagementDB;
  private final Cell<Option<SecurityContext>> secCtx;
  private final Cell<String> workflow;
  private final Cell<HashMap<String, String>> workflowConfigs;
  private final Cell<Integer> preEditAvailDelay;
  private final Cell<String> editProperty;
  private final Cell<String> emailProperty;
  private final Cell<HashMap<String, String>> inputProperties;
  private final Cell<HashMap<String, String>> inputCANames;
  private final Scheduler scheduler;

  public ScheduleFeederRunner(ScheduleFeederServiceImpl scheduleFeeder, SchedulerService schedulerService,
          ParticipationManagementDatabase participationManagementDB, ScheduleProvider scheduleProvider,
          Cell<Option<SecurityContext>> secCtx,
          Cell<String> workflow, Cell<HashMap<String, String>> workflowConfigs,
          Cell<String> editProperty, Cell<String> emailProperty,
          Cell<HashMap<String, String>> inputProperties, Cell<HashMap<String, String>> inputCANames,
          Cell<Integer> preEditAvailDelay) {
    this.scheduleFeederService = scheduleFeeder;
    this.schedulerService = schedulerService;
    this.participationManagementDB = participationManagementDB;
    this.scheduleProvider = scheduleProvider;
    this.secCtx = secCtx;
    this.workflow = workflow;
    this.workflowConfigs = workflowConfigs;
    this.editProperty = editProperty;
    this.emailProperty = emailProperty;
    this.inputProperties = inputProperties;
    this.inputCANames = inputCANames;
    this.preEditAvailDelay = preEditAvailDelay;


    try {
      scheduler = new StdSchedulerFactory().getScheduler();
      scheduler.start();
      // create and set the job. To actually run it call schedule(..)
      final JobDetail job = new JobDetail(JOB_NAME, JOB_GROUP, Feeder.class);
      job.getJobDataMap().put(JOB_PARAM_PARENT, this);
      scheduler.addJob(job, true);
    } catch (org.quartz.SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  public Properties getCaptureAgentConfig() {
    Map<String, String> map = getWorkflowProperties();
    Properties caConfig = new Properties();

    // Prepend with opencast-workflow namespace
    for (Map.Entry<String, String> entry : map.entrySet()) {
      caConfig.put(WORKFLOW_CONFIG_PREFIX.concat(entry.getKey()), entry.getValue());
    }

    caConfig.put(INGEST_WORKFLOW_DEFINITION, workflow.get());
    return caConfig;
  }

  public Map<String, String> getWorkflowProperties() {
    // copy as they may be modified
    Map<String, String> map = new HashMap<String, String>(workflowConfigs.get());
    return map;
  }

  public Cell<String> getEditProperty() {
    return editProperty;
  }

  public Cell<String> getEmailProperty() {
    return emailProperty;
  }

  public Cell<HashMap<String, String>> getInputProperties() {
    return inputProperties;
  }

  public Cell<HashMap<String, String>> getInputCANames() {
    return inputCANames;
  }

  public Cell<Integer> getPreEditAvailDelay() {
    return preEditAvailDelay;
  }
  /** Shutdown the runner. */
  public void shutdown() {
    try {
      scheduler.shutdown();
    } catch (org.quartz.SchedulerException ignore) {
    }
  }

  /**
   * Set the feeders schedule.
   *
   * @param cron
   *          a string suitable for {@link org.quartz.CronTrigger}
   */
  public void schedule(String cron) {
    logger.info("Run ScheduleFeederRunner with cron {}", cron);
    try {
      final CronTrigger trigger = new CronTrigger(TRIGGER_NAME, TRIGGER_GROUP, JOB_NAME, JOB_GROUP, cron);
      if (scheduler.getTriggersOfJob(JOB_NAME, JOB_GROUP).length == 0) {
        scheduler.scheduleJob(trigger);
      } else {
        scheduler.rescheduleJob(TRIGGER_NAME, TRIGGER_GROUP, trigger);
      }
    } catch (Exception e) {
      logger.error("Error scheduling Quartz job", e);
    }
  }

  /** Trigger the feeder once independent of it's actual schedule. */
  public void trigger() {
    try {
      if (scheduler.getJobDetail(JOB_NAME, JOB_GROUP) == null) {
        // If there is no (more) job in scheduler, we create a new one
        final JobDetail job = new JobDetail(JOB_NAME, JOB_GROUP, Feeder.class);
        job.getJobDataMap().put(JOB_PARAM_PARENT, this);
        job.setDurability(true);
        scheduler.addJob(job, true);
      }
      scheduler.triggerJobWithVolatileTrigger(JOB_NAME, JOB_GROUP);
    } catch (Exception e) {
      logger.error("Error triggering Quartz job", e);
    }
  }

  /**
   * Drop the part of the schedule from the Matterhorn service that has not yet been processed.
   *
   * @return true, if schedule could be successfully dropped, false otherwise
   */
  protected boolean dropSchedule() {
    try {
      SchedulerQuery q = new SchedulerQuery();
      q.setStartsFrom(new Date());
      List<DublinCoreCatalog> events = schedulerService.search(q).getCatalogList();
      logger.info("Dropping {} recordings with a start date of now or later", events.size());
      for (DublinCoreCatalog c : events) {
        for (Long id : option(c.getFirst(DublinCore.PROPERTY_IDENTIFIER)).flatMap(toLong)) {
          try {
            logger.debug("Dropping recording event {}", id);
            schedulerService.removeEvent(id);
          } catch (NotFoundException e) {
            logger.warn("Cannot remove event " + id + " since it cannot be found. Maybe the workflow database and "
                    + "the scheduler index are out of sync.");
          }
        }
      }
      logger.info("Finished dropping of recordings");
      return true;
    } catch (Exception e) {
      logger.error("Error dropping schedule", e);
      return false;
    }
  }

  /** Quartz job implementation to poll the data source for schedule data. */
  public static class Feeder implements Job {
    /** Log facility */
    private static final Logger logger = LoggerFactory.getLogger(Feeder.class);

    private static volatile boolean running = false;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
      if (!running) {
        running = true;
        try {
          execute((ScheduleFeederRunner) jobExecutionContext.getJobDetail().getJobDataMap().get(JOB_PARAM_PARENT));
        } catch (Exception e) {
          throw new JobExecutionException("An error occurred while harvesting schedule", e);
        } finally {
          running = false;
        }
      } else {
        logger.warn("Feeder still running... Not executing");
      }
    }

    private void execute(final ScheduleFeederRunner parent) {
      final Properties caConfig = parent.getCaptureAgentConfig();
      final Map<String, String> wfProperties = parent.getWorkflowProperties();
      for (final SecurityContext secCtx : parent.secCtx.get()) {
        secCtx.runInContext(new Effect0() {
          @Override
          public void run() {
            final Date from = parent.scheduleFeederService.getSynchronizationStartDate();
            final Date until = parent.scheduleFeederService.getSynchronizationEndDate();

            List<Tuple<Date, Date>> syncIntervals = getSyncIntervals(from, until);
            logger.info(
                    "Start scheduling of recordings between {} and {} (scheduling will be done in {} cycles)",
                    new Object[] { from, until, syncIntervals.size() });

            for (Tuple<Date, Date> interval : syncIntervals) {
              Schedule newSchedule = null;
              try {
                logger.info("Loading recordings between {} and {}", interval.getA(), interval.getB());
                newSchedule = parent.scheduleProvider.fetchSchedule(interval.getA(), interval.getB());
              } catch (ParticipationManagementSchedulingException e) {
                logger.error("Fetching of scheduled failed: {}", e);
                continue;
              }

              if (!newSchedule.getEpisodes().isEmpty()) {
                logger.info("Scheduling {} recordings between {} and {}", new Object[] { newSchedule.getEpisodes().size(), interval.getA(), interval.getB() });
                try {
                  scheduleRecordings(newSchedule.getEpisodes(), parent.schedulerService,
                          parent.participationManagementDB, caConfig, wfProperties,
                          parent.getEditProperty(), parent.getEmailProperty(),
                          parent.getInputProperties(), parent.getInputCANames(),
                          parent.getPreEditAvailDelay());
                } catch (ParticipationManagementSchedulingException e) {
                  logger.error("Scheduling of recordings from participation management failed.");
                }
              } else {
                logger.warn("No schedule data available between {} and {}", interval.getA(), interval.getB());
              }
            }

          }
        });
        logger.info("Participation Management scheduling finished");
      }
    }

    /**
     * Splits the given date range into day-aligned ranges. No resulting range will lap over more than one day.
     *
     * @param startDate
     *          the start date, must not be {@code null}
     * @param endDate
     *          the start date, must not be {@code null}, must be after {@code startDate}
     * @return a list of date ranges
     */
    List<Tuple<Date, Date>> getSyncIntervals(Date startDate, Date endDate) {
      if (startDate == null)
        throw new IllegalArgumentException("The start date must not be null");
      if (endDate == null)
        throw new IllegalArgumentException("The end date must not be null");

      final DateTime start = new DateTime(startDate);
      final DateTime end = new DateTime(endDate);

      if (end.isBefore(start))
        throw new IllegalArgumentException("The start date must be before the end date");

      DateTime startOfInterval = start;
      DateTime endOfInterval = startOfInterval.withMillisOfDay(DateTimeConstants.MILLIS_PER_DAY - 1);

      if (endOfInterval.isAfter(end))
        endOfInterval = new DateTime(end);

      List<Tuple<Date, Date>> syncIntervals = new ArrayList<Tuple<Date, Date>>();
      syncIntervals.add(Tuple.tuple(new Date(startOfInterval.getMillis()), new Date(endOfInterval.getMillis())));

      while (endOfInterval.isBefore(end)) {
        startOfInterval = endOfInterval.plusMillis(1);
        endOfInterval = endOfInterval.plusDays(1);

        if (endOfInterval.isAfter(end))
          endOfInterval = end;

        syncIntervals.add(Tuple.tuple(new Date(startOfInterval.getMillis()), new Date(endOfInterval.getMillis())));
      }

      return syncIntervals;
    }

    /**
     * Schedule a list of recordings. Also set properties for the capture agent.
     *
     * @throws ParticipationManagementSchedulingException
     */
    @SuppressWarnings("unchecked")
    private static void scheduleRecordings(List<Tuple<Recording, DublinCoreCatalog>> list,
            SchedulerService schedulerService, ParticipationManagementDatabase participationManagementDB,
            Properties parentCAConfig, Map<String, String> parentWFProperties,
            Cell<String>editProperty, Cell<String>emailProperty,
            Cell<HashMap<String, String>> inputProperties, Cell<HashMap<String, String>> inputCANames,
            Cell<Integer> preEditAvailDelay)
                    throws ParticipationManagementSchedulingException {

      int i = 0;
      Properties caConfig;
      Map<String, String> wfProperties;

      for (Tuple<Recording, DublinCoreCatalog> t : list) {
        // UOM: Reset the config and properties to that of the parent
        caConfig = (Properties) parentCAConfig.clone();
        wfProperties = new HashMap<>(parentWFProperties);

        i++;
        try {
          Recording rec = t.getA();
          DublinCoreCatalog dc = t.getB();

          wfProperties.put(editProperty.get(), Boolean.toString(rec.isEdit()));
          caConfig.put(WORKFLOW_CONFIG_PREFIX.concat(editProperty.get()), Boolean.toString(rec.isEdit()));

          String emailAddresses;
          List<String> staffMailList = new ArrayList<>();
          for (Person p : rec.getStaff()) {
            if (StringUtils.isNotBlank(p.getEmail())) {
              staffMailList.add(p.getEmail());
            }
          }

          if (staffMailList.size() > 0) {
            emailAddresses = StringUtils.join(staffMailList, ",");

            wfProperties.put(emailProperty.get(), emailAddresses);
            caConfig.put(WORKFLOW_CONFIG_PREFIX.concat(emailProperty.get()), emailAddresses);
          }

          // Set inputs to use in workflow
          // Should not matter as capture agent should only record those selected below
          for (Map.Entry<String, String> prop : inputProperties.get().entrySet()) {
            if (prop.getValue() != null) {
              final Boolean value;
              if (Arrays.asList(rec.getRecordingInputs().split("\\|")).contains(prop.getKey())) {
                value = true;
              } else {
                value = false;
              }

              wfProperties.put(prop.getValue(), value.toString());
              caConfig.put(WORKFLOW_CONFIG_PREFIX.concat(prop.getValue()), value.toString());
            }
          }

          // Set which Capture Agent input devices to record
          // Check if we are using the "defaults" ie CA's one and only input
          String captureAgentInputs = rec.getCaptureAgent().getInputs();
          if (captureAgentInputs.split("\\|").length == 1) {
            caConfig.put("capture.device.names", "defaults");
          } else {
            List<String> inputNames = new ArrayList<>();
            // MAT-131, add audio explicitly when not using defaults
            for (String input : (rec.getRecordingInputs() + "|audio").split("\\|")) {
              inputNames.add(inputCANames.get().get(input));
            }
            caConfig.put("capture.device.names", String.join(",", inputNames));
          }

          // Check if course is required to be recorded and optout should be ignored
          Boolean requiredRecording = false;

          if (rec.getCourse().isSome()) {
            requiredRecording = rec.getCourse().get().getRequirements().contains(Course.REQUIREMENT_RECORD);

            // Only mark add audience restricted if to be trimmed or opted out
            if (requiredRecording && (rec.isEdit() || rec.getReviewStatus() == Recording.ReviewStatus.OPTED_OUT)) {
              // if DASS student on course add them to audience
              dc.add(DublinCore.PROPERTY_AUDIENCE, "restricted");
            }

            if (!(rec.getCourse().get().isOptedOut() || rec.getReviewStatus() == Recording.ReviewStatus.OPTED_OUT)) {
              // if neither course or recording opted out add enrolled to audience
              dc.add(DublinCore.PROPERTY_AUDIENCE, "enrolled");
            }

            // Set availablility date
            if (requiredRecording && rec.isEdit())  {
              Calendar cal = Calendar.getInstance();
              cal.setTime(rec.getStop());
              cal.add(Calendar.HOUR, preEditAvailDelay.get());
              // overwrite default available period
              dc.set(DublinCore.PROPERTY_AVAILABLE,
                EncodingSchemeUtils.encodePeriod(new DCMIPeriod(cal.getTime(), null), Precision.Minute));
            }
          } else {
            logger.warn("Attempting to schedule recording with no course {}", rec.getId());

            // For completeness rather than likely occcurance
            if (rec.getReviewStatus() == Recording.ReviewStatus.OPTED_OUT) {
              // if neither course or recording opted out add enrolled to audience
              dc.add(DublinCore.PROPERTY_AUDIENCE, "enrolled");
            }
          }

          StringBuilder fingerprintData = new StringBuilder(dc.toXmlString());
          fingerprintData.append(caConfig.toString());
          String md5 = DigestUtils.md5Hex(fingerprintData.toString());

          if (rec.getEventId().isNone()
                  && rec.getRecordingStatus(requiredRecording) == Recording.RecordingStatus.READY
                  && !rec.isDeleted()) {
            // ADD EVENT
            // If the recording has still no eventId, we create an new schedule event and update the recording with the
            // new event id
            logger.debug("Checking status of recording " + rec.getId().get() + " ({}/{})", i, list.size());
            Long eventId = schedulerService.addEvent(dc, wfProperties);
            if (eventId == null) {
              logger.info("Skipping scheduling of future event {}", rec.getId().get());
              continue;
            }
            logger.info("Scheduling recording " + rec.getId().get() + " ({}/{})", i, list.size());
            rec.setEventId(eventId);
            rec.setFingerprint(Option.some(md5));
            schedulerService.updateCaptureAgentMetadata(caConfig, tuple(rec.getEventId().get(), dc));
            try {
              rec = participationManagementDB.updateRecording(rec);
            } catch (ParticipationManagementDatabaseException e) {
              logger.error("Not able to update the newly scheduled recording {}", rec.getId().get());
              throw new ParticipationManagementSchedulingException(e);
            }
          } else if (rec.getEventId().isSome()
                  && (rec.isDeleted() || rec.getRecordingStatus(requiredRecording) != Recording.RecordingStatus.READY)) {
            // DELETE EVENT
            // If eventId is present and delete flag is true or the recording status is blacklisted or opted-out, we
            // deleted the schedule event
            Long eventId = rec.getEventId().get();
            logger.info("Removing scheduled event " + eventId + " ({}/{})", i, list.size());
            try {
              schedulerService.removeEvent(eventId);
            } catch (NotFoundException e1) {
              logger.warn("The event {} can not be found in Matterhorn and therefore can not be removed.", eventId);
            }
            rec.setEventId(null);
            try {
              participationManagementDB.updateRecording(rec);
            } catch (ParticipationManagementDatabaseException e2) {
              throw new ParticipationManagementSchedulingException("Not able to update deleted event " + eventId, e2);
            }
          } else if (rec.getEventId().isSome() && rec.getRecordingStatus(requiredRecording) == Recording.RecordingStatus.READY
                  && !rec.isDeleted() && !rec.getStop().before(new Date())) {
            // UPDATE EVENT

            // Otherwise, if the recording status is ready and not recording and is not deleted, it can be updated
            Long eventId = rec.getEventId().get();

            if (rec.getFingerprint().isSome() && md5.equals(rec.getFingerprint().get())) {
              logger.debug("Scheduled event {} has not changed", eventId);
            } else {
              logger.info("Updating scheduled event " + eventId + " ({}/{})", i, list.size());
              try {
                schedulerService.updateEvent(eventId, dc, wfProperties);
                schedulerService.updateCaptureAgentMetadata(caConfig, tuple(eventId, t.getB()));
                rec.setFingerprint(Option.some(md5));
                participationManagementDB.updateRecording(rec);
              } catch (NotFoundException e) {
                logger.warn("The event {} cannot be found in Matterhorn and therefore can not be updated.", eventId);
              } catch (ParticipationManagementDatabaseException e) {
                logger.warn("Failed to update fingerprint of event {}: {}", eventId, e.getMessage());
              }
            }
          }
        } catch (SchedulerException e) {
          logger.error("Error scheduling event", e);
        } catch (UnauthorizedException e) {
          logger.error("Error scheduling event", e);
        } catch (NotFoundException e) {
          logger.error("Error scheduling event", e);
        } catch (IOException e) {
          logger.error("Error calculating md5 sum from event metadata", e);
        }
      }

      logger.debug(format("%d/%d recordings scheduled from Participation Management", i, list.size()));
    }
  }

  @Override
  protected void finalize() throws Throwable {
    super.finalize();
    shutdown();
  }

}
