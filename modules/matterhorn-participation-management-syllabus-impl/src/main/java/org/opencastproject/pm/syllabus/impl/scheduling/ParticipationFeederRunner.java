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
import static org.opencastproject.util.EqualsUtil.eq;
import static org.opencastproject.util.data.Collections.diff;
import static org.opencastproject.util.data.Collections.groupBy;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.pm.api.Building;
import org.opencastproject.pm.api.CaptureAgent;
import org.opencastproject.pm.api.Course;
import org.opencastproject.pm.api.Error;
import org.opencastproject.pm.api.Person;
import org.opencastproject.pm.api.Recording;
import org.opencastproject.pm.api.Room;
import org.opencastproject.pm.api.SchedulingSource;
import org.opencastproject.pm.api.Synchronization;
import org.opencastproject.pm.api.SynchronizedRecording;
import org.opencastproject.pm.api.SynchronizedRecording.SynchronizationStatus;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabaseException;
import org.opencastproject.pm.api.persistence.RecordingQuery;
import org.opencastproject.pm.api.scheduling.SnapCountService;
import org.opencastproject.pm.api.util.RequirementManager;
import org.opencastproject.pm.syllabus.api.SyllabusService;
import org.opencastproject.pm.syllabus.api.VActivity;
import org.opencastproject.pm.syllabus.api.VActivityDateTime;
import org.opencastproject.pm.syllabus.api.VActivityLocation;
import org.opencastproject.pm.syllabus.api.VActivityStaff;
import org.opencastproject.pm.syllabus.api.VLocation;
import org.opencastproject.pm.syllabus.api.VModule;
import org.opencastproject.pm.syllabus.api.VStaff;
import org.opencastproject.pm.syllabus.api.VZones;
import org.opencastproject.pm.syllabus.impl.scheduling.AccessorFunctions.VActivityDateTimeF;
import org.opencastproject.security.util.SecurityContext;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.ChecksumType;
import org.opencastproject.util.EqualsUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Cell;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Monadics;
import org.opencastproject.util.data.Monadics.ListMonadic;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.functions.Booleans;
import org.opencastproject.util.data.functions.Strings;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.joda.time.DateTime;
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
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

/** Feeding Syllabus+ data into the Manchester participation management system. */
public class ParticipationFeederRunner {
  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(ParticipationFeederRunner.class);

  private static final String JOB_NAME = "participation-feeder";
  private static final String JOB_GROUP = "mh-participation-feeder";
  private static final String TRIGGER_NAME = "cron";
  private static final String TRIGGER_GROUP = "mh-participation-feeder";
  private static final String JOB_PARAM_PARENT = "parent";

  private final SyllabusService syllabusService;
  private final ParticipationManagementDatabase persistence;
  private final RequirementManager requirementManager;
  private final Cell<Option<SecurityContext>> secCtx;
  private static HarvestStats lastHarvestStats = new HarvestStats();
  private static SnapCountService snapCountService = null;

  private final Scheduler scheduler;

  public ParticipationFeederRunner(SyllabusService syllabusService, ParticipationManagementDatabase persistence,
          RequirementManager requirementManager, Cell<Option<SecurityContext>> secCtx) {
    this.syllabusService = syllabusService;
    this.persistence = persistence;
    this.requirementManager = requirementManager;
    this.secCtx = secCtx;
    try {
      Properties properties = new Properties();
      properties.setProperty("org.quartz.threadPool.threadCount", "1");
      properties.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
      properties.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
      scheduler = new StdSchedulerFactory(properties).getScheduler();
      scheduler.start();
    } catch (org.quartz.SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean isRunning() {
    return Feeder.running;
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

  // old implementation of hasCaptureAgent()
  // private static final Set<String> LOCATIONS_WITH_CA = set(
  // "Chemistry_G.53", "Chemistry_G.54",
  // "Crawford House_TH 1",
  // "Ellen Wilk Wing C_C5.1",
  // "HumBridgeSt_CORDINGLEY THEATRE",
  // "Kilburn_TH 1.1", "Kilburn_TH 1.3", "Kilburn_TH 1.4", "Kilburn_TH 1.5",
  // "Mansfield Cooper_G.20",
  // "Renold_C16", "Renold_C2", "Renold_C9",
  // "Roscoe_TH A",
  // "Sackville Street_C53",
  // "Sam Alex East Wing_LG12%",
  // "Schuster_BLACKETT TH", "Schuster_MOSELEY TH",
  // "Stopford_TH 1", "Stopford_TH 2", "Stopford_TH 3", "Stopford_TH 4", "Stopford_TH 5", "Stopford_TH 6",
  // "Zochonis_TH A", "Zochonis_TH B");
  //
  // /** Check if a location features a capture agent. */
  // public static boolean hasCaptureAgent(VLocation location) {
  // return LOCATIONS_WITH_CA.contains(location.getName());
  // }

  @Override
  protected void finalize() throws Throwable {
    super.finalize();
    shutdown();
  }

  public void setSnapCountService(SnapCountService snapCountService) {
    logger.info("setSnapCountService");
    this.snapCountService = snapCountService;
  }

  /** Quartz job implementation to poll the data source for schedule data. */
  public static class Feeder implements Job {
    private static volatile boolean running = false;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
      if (!running) {
        running = true;
        try {
          executeInSecurityCtx((ParticipationFeederRunner) jobExecutionContext.getJobDetail().getJobDataMap()
                  .get(JOB_PARAM_PARENT));
        } catch (Exception e) {
          throw new JobExecutionException("An error occurred while harvesting schedule", e);
        } finally {
          running = false;
        }
      } else {
        logger.warn("Feeder still running... Not executing");
      }
    }

    public void verify(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    if (!running) {
        running = true;
        try {
          verifyInSecurityCtx((ParticipationFeederRunner) jobExecutionContext.getJobDetail().getJobDataMap()
                  .get(JOB_PARAM_PARENT));
        } catch (Exception e) {
          throw new JobExecutionException("An error occurred while harvesting schedule", e);
        } finally {
          running = false;
        }
      } else {
        logger.warn("Feeder still running... Not executing");
      }
    }

    /** Run the harvest verification inside a security context. */
    private void verifyInSecurityCtx(final ParticipationFeederRunner parent) {
      for (final SecurityContext secCtx : parent.secCtx.get()) {
        logger.info("participation Harvest Verification start");
        secCtx.runInContext(new Effect0() {
          @Override
          public void run() {
            final Synchronization synchronization = new Synchronization(new Date());
            try {
              verifyHarvest(synchronization, parent);
            } catch (Exception e) {
              final String stackTrace = ExceptionUtils.getStackTrace(e);
              synchronization.addError(new Error("uncaught-exception", "An uncaught exception occurred", stackTrace));
              logger.error(stackTrace);
            } finally {
              logger.info("Persisted {} recordings with {} errors", synchronization.getSynchronizedRecordings().size(),
                      synchronization.getErrors().size());
              try {
                parent.persistence.storeLatestSynchronization(synchronization);
              } catch (ParticipationManagementDatabaseException e) {
                logger.error("Unable to store synchronization {}", synchronization);
              }
            }
          }
        });
        logger.info("Participation Harvest Verification  end");
      }
    }

    /** Run the harvest inside a security context. */
    private void executeInSecurityCtx(final ParticipationFeederRunner parent) {
      for (final SecurityContext secCtx : parent.secCtx.get()) {
        logger.info("Harvesting participation start");
        secCtx.runInContext(new Effect0() {
          @Override
          public void run() {
            final Synchronization synchronization = new Synchronization(new Date());
            try {
              harvest(synchronization, parent);
            } catch (Exception e) {
              final String stackTrace = ExceptionUtils.getStackTrace(e);
              synchronization.addError(new Error("uncaught-exception", "An uncaught exception occurred", stackTrace));
              logger.error(stackTrace);
            } finally {
              logger.info("Persisted {} recordings with {} errors", synchronization.getSynchronizedRecordings().size(),
                      synchronization.getErrors().size());
              try {
                parent.persistence.storeLatestSynchronization(synchronization);
              } catch (ParticipationManagementDatabaseException e) {
                logger.error("Unable to store synchronization {}", synchronization);
              }
            }
            logger.info("Updating the the Course's DASS requirements");
            parent.requirementManager.updateRequirements();
          }
        });
        logger.info("Harvesting participation end");
        if (null !=  parent.snapCountService) {
          logger.info("Start verify participation harvest");
          parent.snapCountService.verifyParticipationFeeder();
        }
      }
    }

    /** Run the harvest Verification. */
    private void verifyHarvest(final Synchronization synchronization, final ParticipationFeederRunner parent) {
      final SyllabusService syl = parent.syllabusService;
      final SyllabusData data = SyllabusData.fetch(syl);
      final Map<String, Set<Room>> buildingMap = new HashMap<String, Set<Room>>();
      final Set<Long> treatedRecordings = new HashSet<Long>();
      final ModuleFinder moduleFinder = new ModuleFinder(data.getModule(), data.getActivityParent(), data.getActivity());
      final String sourceDescription = data.getSourceDescription();
      Option<SchedulingSource> schedulingSource;
      if (StringUtils.isNotBlank(sourceDescription)) {
        schedulingSource = Option.some(new SchedulingSource(sourceDescription));
      } else {
        schedulingSource = none();
      }
      final HarvestStats stats = new HarvestStats();
      // limit the amount of harvested recordings for testing
      final Option<Integer> limitRecordings = none();
    }

    /** Run the actual harvest. */
    private void harvest(final Synchronization synchronization, final ParticipationFeederRunner parent) {
      final SyllabusService syl = parent.syllabusService;
      final SyllabusData data = SyllabusData.fetch(syl);
      final Map<String, Set<Room>> buildingMap = new HashMap<String, Set<Room>>();
      final Set<Long> treatedRecordings = new HashSet<Long>();
      final ModuleFinder moduleFinder = new ModuleFinder(data.getModule(), data.getActivityParent(), data.getActivity());
      final String sourceDescription = data.getSourceDescription();
      Option<SchedulingSource> schedulingSource;
      logger.info("start harvesting from S+");
      if (StringUtils.isNotBlank(sourceDescription)) {
        schedulingSource = Option.some(new SchedulingSource(sourceDescription));
      } else {
        schedulingSource = none();
      }
      final HarvestStats stats = new HarvestStats();
      // limit the amount of harvested recordings for testing
      final Option<Integer> limitRecordings = none();
      // iterate activity partitions
      ACTIVITIES: for (List<VActivity> activityPartition : data.getActivityPartitioned()) {
        // fetch matching V_ACTIVITY_DATETIME entities
        final String firstActivityId = activityPartition.get(0).getId();
        final String lastActivityId = activityPartition.get(activityPartition.size() - 1).getId();
        final List<VActivityDateTime> activityDateTimeList = fetch(syl, firstActivityId, lastActivityId, 4);
        logger.debug("# activityDateTime from " + firstActivityId + " to " + lastActivityId + " "
                + activityDateTimeList.size());
        if (activityDateTimeList.size() == 0) {
          logger.warn("ActivityDateTime range query did non yield any results. That may not be correct. Be"
                  + " aware that all recordings of that range will be deleted.");
        }
        final Multimap<String, VActivityDateTime> activityDateTimeMap = groupBy(
                ArrayListMultimap.<String, VActivityDateTime> create(), activityDateTimeList,
                VActivityDateTimeF.getActivityId);
        // iterate activity partition
        for (final VActivity activity : activityPartition) {
          final String aId = activity.getId();
          if (!SyllabusData.isSubjectToSchedule(activity)) {
            continue;
          }
          logger.info(format("********** Handling activity %s", aId));
          if (activityDateTimeMap.get(aId).isEmpty()) {
            logger.info(format("ActivityDateTime does not contain entry for activity %s. Skipping...", aId));
            continue;
          }
          if (data.getActivityLocation().get(aId).isEmpty()) {
            logger.info(format("ActivityLocationMap does not contain entry for activity %s. Skipping...", aId));
            continue;
          }
          final List<VActivity> activityHierarchy = moduleFinder.collectUntilModule(activity);
          final List<VModule> modules = moduleFinder.getModules(activityHierarchy);
          if (modules.isEmpty()) {
            logger.warn(format("Activity %s does not belong to any module. Skipping...", aId));
            continue;
          }
          if (activityHierarchy.size() > 1) {
            logger.info(format("Merging staff members of activity hierarchy (%d) of activity %s",
                    activityHierarchy.size(), aId));
          }
          final List<Person> staff = mergeStaff(data, activityHierarchy);
          if (staff.size() < 1) {
            logger.info(format("Activity %s has no staff members associated. Skipping...", aId));
            continue;
          }
          // we do not store course participation in PM anymore
          final List<Person> participation = new ArrayList<Person>();
          if (modules.size() > 1) {
            logger.info(format("Activity %s belongs to %d modules. Merging.", aId, modules.size()));
          }
          final Course course = mergeModules(modules);
          course.setSchedulingSource(schedulingSource);

          // iterate locations
          for (final VActivityLocation activityLocation : data.getActivityLocation().get(aId)) {
            final VLocation location = data.getLocation().get(activityLocation.getLocationId());
            if (location == null) {
              logInconsistency("location", activityLocation.getLocationId());
              continue;
            }
            // only process if location features a capture agent
            if (!data.hasCaptureAgent(location)) {
              logger.info(format("Location %s does not have a capture agent. Skipping...", location.getName()));
              continue;
            }
            // iterate dates
            for (final VActivityDateTime dateTime : activityDateTimeMap.get(aId)) {
              final Room room = new Room(location.getName());
              final Recording newRecording = Recording.recording(
                      // no need to create a virtual id for the recording
                      aId,
                      // MP-284
                      createRecordingTitle(modules, dateTime.getStartDateTime().toDate()),
                      staff,
                      course,
                      room,
                      new Date(),
                      // think about adding a configurable offset to start
                      // and stop dates since they describe when the event
                      // actually happens. CA should probably capture a bigger
                      // time frame in order to not miss anything.
                      // This offset may be applied when creating the MH schedule.
                      // - or, the recording may hold the offsets
                      // - or, the recording may hold a capture start date, event start date
                      // and a capture end date and an event end date
                      dateTime.getStartDateTime().toDate(), dateTime.getEndDateTime().toDate(), participation,
                      new CaptureAgent(room, CaptureAgent.getMhAgentIdFromRoom(room)));
              newRecording.setSchedulingSource(schedulingSource);
              try {
                List<Recording> recordings = parent.persistence.findRecordings(RecordingQuery.create()
                        .withActivityId(newRecording.getActivityId()).withRoom(newRecording.getRoom()).withStartDate(newRecording.getStart())
                        .withEndDate(newRecording.getStop()));
                Recording updatedRecording = null;
                if (recordings.size() == 0) {
                  logger.debug("New recording.");

                  // Find activity's recordings that might have been scheduled in another room
                  Recording oldRecording = findByFingerPrint(parent.persistence, newRecording.getActivityId(),
                          newRecording.getStart(), newRecording.getStop());
                  if (oldRecording != null) {
                    newRecording.setReviewStatus(oldRecording.getReviewStatus());
                    newRecording.setReviewDate(oldRecording.getReviewDate());
                    newRecording.setTrim(oldRecording.isTrim());
                  }

                  updatedRecording = parent.persistence.updateRecording(newRecording);
                  stats.incNew();
                  treatedRecordings.add(updatedRecording.getId().getOrElseNull());
                  synchronization.addSynchronizedRecording(new SynchronizedRecording(SynchronizationStatus.NEW,
                          updatedRecording));
                } else if (recordings.size() == 1) {
                  Recording existingRecording = recordings.get(0);
                  treatedRecordings.add(existingRecording.getId().getOrElseNull());
                  // Check if the recording has to be updated
                  final List<String> diff = equalsUpdateDiff(newRecording, existingRecording);
                  if (diff.size() > 0) {
                    if (!existingRecording.isDeleted()) {
                      logger.debug(format(
                              "A recording %s for activity %s with matching unique constraint already exists. Updating.",
                              existingRecording.getId().get(), existingRecording.getActivityId()));
                      newRecording.setId(existingRecording.getId());
                      newRecording.setAction(existingRecording.getAction());
                      newRecording.setBlacklisted(existingRecording.isBlacklisted());
                      newRecording.setEventId(existingRecording.getEventId().getOrElseNull());
                      newRecording.setMessages(existingRecording.getMessages());
                      newRecording.setReviewDate(existingRecording.getReviewDate());
                      newRecording.setReviewStatus(existingRecording.getReviewStatus());
                      newRecording.setTrim(existingRecording.isTrim());
                      newRecording.setFingerprint(existingRecording.getFingerprint());
                      newRecording.setSchedulingSource(existingRecording.getSchedulingSource());
                      if (existingRecording.getCourse().isSome()) {
                        course.setSeriesId(existingRecording.getCourse().get().getSeriesId());
                        newRecording.setCourse(Option.some(course));
                      }
                    } else {
                      logger.debug(format(
                              "A deleted recording %s for activity %s with matching unique constraint already exists. Overwriting.",
                              existingRecording.getId().get(), existingRecording.getActivityId()));
                      newRecording.setId(existingRecording.getId());
                    }
                    if (newRecording.getStart().before(DateTime.now().minusDays(1).toDate())) {
                      newRecording.setReviewDate(existingRecording.getReviewDate());
                      newRecording.setReviewStatus(existingRecording.getReviewStatus());
                    }
                    logger.debug("Diff: " + mlist(diff).mkString(","));
                    updatedRecording = parent.persistence.updateRecording(newRecording);
                    stats.incUpdated();
                    synchronization.addSynchronizedRecording(new SynchronizedRecording(SynchronizationStatus.UPDATE,
                            updatedRecording));
                  } else {
                    stats.incNotModified();
                    logger.debug(format(
                            "A recording %s for activity %s with matching unique constraint already exists but needs no update.",
                            existingRecording.getId().get(), existingRecording.getActivityId()));
                  }
                } else {
                  throw new IllegalStateException("Multiple recordings with the same unique identifiers found!");
                }

                // Add room to building
                if (location.getZoneId().isSome() && updatedRecording != null) {
                  final VZones zone = data.getZones().get(location.getZoneId().get());
                  Set<Room> rooms = buildingMap.get(zone.getName());
                  if (rooms == null) {
                    rooms = new HashSet<Room>();
                    rooms.add(updatedRecording.getRoom());
                  } else {
                    rooms.add(updatedRecording.getRoom());
                  }
                  buildingMap.put(zone.getName(), rooms);
                }
              } catch (ParticipationManagementDatabaseException e) {
                logger.error("Unable to store recording {}", newRecording);
                synchronization.addError(new Error("database", "Unable to store recording", e.getMessage()));
              }
              logger.info(format("%6d Recording for activity %s [%s %s] @ %s", stats.incTotal(),
                      newRecording.getActivityId(), new DateTime(newRecording.getStart()),
                      new DateTime(newRecording.getStop()), newRecording.getRoom().getName()));
              // it the limit of recordings is reached, end processing of activities
              if (limitRecordings.map(Booleans.lt(stats.getTotal() + 1)).getOrElse(false)) {
                break ACTIVITIES;
              }
            }
          }
        } // /iterate activity partition
      } // /iterate activity partitions
      //
      // deletion
      List<Recording> recordings = new ArrayList<Recording>();
      try {
        logger.debug("Preparing deletion of untreated recordings. Fetching all recordings from database.");
        recordings = parent.persistence.findRecordings(RecordingQuery.createWithoutDeleted());
      } catch (ParticipationManagementDatabaseException e) {
        logger.error("Unable to get all recording! Synchronization failed");
      }
      for (Recording r : recordings) {
        if (!treatedRecordings.contains(r.getId().get())) {
          try {
            logger.info(format("Deleting untreated recording %s of activityId %s", r.getId(), r.getActivityId()));
            parent.persistence.deleteRecording(r.getId().getOrElseNull());
            synchronization.addSynchronizedRecording(new SynchronizedRecording(SynchronizationStatus.DELETION, r));
            stats.incDeleted();
          } catch (ParticipationManagementDatabaseException e) {
            logger.error("Unable to delete recording {}", r);
            synchronization.addError(new Error("database", "Unable to delete recording", e.getMessage()));
          } catch (NotFoundException e) {
            // todo Throw an exception here? Wouldn't it be better to just log?
            throw new IllegalStateException("Previous found recording not found for deletion anymore!");
          }
        }
      }
      // update buildings
      for (Entry<String, Set<Room>> entry : buildingMap.entrySet()) {
        final Building building = new Building(entry.getKey(), new ArrayList<Room>(entry.getValue()));
        try {
          logger.info("Updating building " + building.getName());
          parent.persistence.updateBuilding(building);
        } catch (ParticipationManagementDatabaseException e) {
          logger.error("Unable to store building {}", building);
          synchronization.addError(new Error("database", "Unable to store building", e.getMessage()));
        }
      }
      logger.info(format("%d recordings total, %d new, %d updated, %d not modified, %d deleted", stats.getTotal(),
              stats.getNew(), stats.getUpdated(), stats.getNotModified(), stats.getDeleted()));
      lastHarvestStats =  stats;
    }

    private Recording findByFingerPrint(ParticipationManagementDatabase persistence, String activityId, Date startDate,
            Date stopDate) throws ParticipationManagementDatabaseException {
      DateTime start = new DateTime(startDate.getTime()).withHourOfDay(0);
      DateTime end = new DateTime(stopDate.getTime()).withHourOfDay(0).plusDays(2);

      List<Recording> recordings = persistence.findRecordings(RecordingQuery.createWithoutDeleted()
              .withActivityId(activityId).withStartDateRange(start.toDate()).withEndDateRange(end.toDate()));

      List<Recording> oldRecordings = new ArrayList<Recording>();
      for (Recording recording : recordings) {
        if (start.getDayOfYear() == new DateTime(recording.getStart().getTime()).getDayOfYear())
          oldRecordings.add(recording);
      }
      return getMostRecentRecording(oldRecordings);
    }

    private Recording getMostRecentRecording(List<Recording> oldRecordings) {
      if (oldRecordings.size() == 0)
        return null;

      Recording mostRecent = oldRecordings.get(0);
      for (Recording recording : oldRecordings) {
        if (recording.getModificationDate().after(mostRecent.getModificationDate()))
          mostRecent = recording;
      }
      return mostRecent;
    }

    /** Fetch a partition from the VActivityDateTime table. */
    private List<VActivityDateTime> fetch(SyllabusService syl, String firstActivityId, String lastActivityId,
            int retries) {
      final List<VActivityDateTime> r = syl.findActivityDateTimeByRange(firstActivityId, lastActivityId);
      if (r.size() > 0) {
        return r;
      } else {
        if (retries > 0) {
          logger.debug("ActivityDateTime range query did not yield any result. Retrying in one second...");
          try {
            Thread.sleep(1000);
          } catch (InterruptedException ignore) {
          }
          return fetch(syl, firstActivityId, lastActivityId, retries - 1);
        } else {
          return r;
        }
      }
    }

    private List<String> equalsUpdateDiff(Recording rec, Recording db) {
      return mlist(
              !rec.getActivityId().equals(db.getActivityId()) ? format("activityId %s -> %s", db.getActivityId(),
                      rec.getActivityId()) : "",
              !rec.getTitle().equals(db.getTitle()) ? format("title %s -> %s", db.getTitle(), rec.getTitle()) : "",
              !EqualsUtil.eqListUnsorted(rec.getStaff(), db.getStaff()) ? format("staff %s",
                      biDiff(rec.getStaff(), db.getStaff())) : "",
              !equalsCourseUpdate(rec.getCourse(), db.getCourse()) ? format("course %s -> %s", db.getCourse(),
                      rec.getCourse()) : "",
              !rec.getRoom().equals(rec.getRoom()) ? format("room %s -> %s", db.getRoom(), rec.getRoom()) : "",
              !rec.getStart().equals(db.getStart()) ? format("start %s -> %s", db.getStart(), rec.getStart()) : "",
              !rec.getStop().equals(db.getStop()) ? format("stop %s -> %s", db.getStop(), rec.getStop()) : "",
              !EqualsUtil.eqListUnsorted(rec.getParticipation(), db.getParticipation()) ? format("participation %s",
                      biDiff(rec.getParticipation(), db.getParticipation())) : "",
              !equalsCAUpdate(rec.getCaptureAgent(), db.getCaptureAgent()) ? format("capture agent %s -> %s",
                      db.getCaptureAgent(), rec.getCaptureAgent()) : "").filter(Strings.notBlank).value();
    }

    /** Calculate a bidirectional diff of two lists as a printable string. */
    private static String biDiff(List<?> newL, List<?> oldL) {
      final List<?> plus = diff(newL, oldL);
      final List<?> minus = diff(oldL, newL);
      return (plus.size() > 0 ? "[+ " + mlist(plus).mkString(";") + "]" : "")
              + (minus.size() > 0 ? "[- " + mlist(minus).mkString(";") + "]" : "");
    }

    private static boolean equalsCAUpdate(CaptureAgent agent, CaptureAgent db) {
      return agent == null && db == null || agent != null && eq(agent.getRoom(), db.getRoom());
    }

    private static boolean equalsCourseUpdate(Option<Course> course, Option<Course> db) {
      if (course.isNone() && db.isNone()) {
        return true;
      } else if (course.isSome() && db.isSome()) {
        boolean courseId = course.get().getCourseId().equals(db.get().getCourseId());
        boolean name = EqualsUtil.eq(course.get().getName(), course.get().getName());
        boolean description = EqualsUtil.eq(course.get().getDescription(), course.get().getDescription());
        boolean externalCourseKey = EqualsUtil.eq(course.get().getExternalCourseKey(), course.get()
                .getExternalCourseKey());
        return courseId && name && description && externalCourseKey;
      } else {
        return false;
      }
    }
  }

  private static List<Person> mergeStaff(final SyllabusData data, final List<VActivity> activities) {
    return mlist(activities).bind(new Function<VActivity, Iterable<VActivityStaff>>() {
      @Override
      public Iterable<VActivityStaff> apply(VActivity a) {
        return data.getActivityStaff().get(a.getId());
      }
    }).bind(new Function<VActivityStaff, Iterable<VStaff>>() {
      @Override
      public Iterable<VStaff> apply(VActivityStaff a) {
        return option(data.getStaff().get(a.getStaffId()));
      }
    }).filter(new Function<VStaff, Boolean>() {
      @Override
      public Boolean apply(VStaff a) {
        return a.getEmail().isSome();
      }
    }).map(new Function<VStaff, Person>() {
      @Override
      public Person apply(VStaff a) {
        return Person.person(a.getName(), a.getEmail().get());
      }
    }).value();
  }

  /** Concat the names of a list of modules. */
  private static String concatModuleNames(final ListMonadic<VModule> modules) {
    return modules.map(AccessorFunctions.VModuleF.getName).mkString(" / ");
  }

  private static ListMonadic<VModule> sortModulesById(final List<VModule> modules) {
    return mlist(modules).sort(new Comparator<VModule>() {
      @Override
      public int compare(VModule a, VModule b) {
        return a.getId().compareToIgnoreCase(b.getId());
      }
    });
  }

  public static Course mergeModules(final List<VModule> modules) {
    // sort modules to get a stable virtual course id in case it's needed
    final Monadics.ListMonadic<VModule> modulesSorted = sortModulesById(modules);
    final String name;
    final String description;
    final String id;
    final String externalCourseKey;
    if (modulesSorted.value().size() == 1) {
      final String baseName = concatModuleNames(modulesSorted);
      description = modulesSorted.value().get(0).getDescription().getOrElseNull();
      if (description != null) {
        name = baseName.concat(" - ").concat(description);
      } else {
        name = baseName;
      }
      id = modulesSorted.value().get(0).getId();
      externalCourseKey = modulesSorted.value().get(0).getExternalCourseKey().getOrElseNull();
    } else {
      name = concatModuleNames(modulesSorted);
      description = name;
      id = createVirtualId(modulesSorted.map(AccessorFunctions.VModuleF.getId).mkString("_"));
      logger.debug("Create virtual course id " + id);
      externalCourseKey = modulesSorted.map(AccessorFunctions.VModuleF.getExternalCourseKey).mkString("_");
    }
    return new Course(id, null, name, description, externalCourseKey);
  }

  public HarvestStats getLastHarvestStats() {
    return lastHarvestStats;
  }

  private static void logInconsistency(String dataName, String dataId) {
    logger.warn(format("Data inconsistency. Cannot find %s with id %s. Skipping...", dataName, dataId));
  }

  /** Create an id for virtual (aka merged) activities and modules. */
  private static String createVirtualId(String s) {
    try {
      return "V_" + Checksum.createFor(ChecksumType.DEFAULT_TYPE, s).getValue();
    } catch (IOException e) {
      return chuck(e);
    }
  }

  /**
   * Create a title for a recording after the requirement of MP-284. <module-name(s)> - <date:dd-MMM>
   */
  private static String createRecordingTitle(List<VModule> modules, Date recordingStart) {
    return format("%1$s - %2$td-%2$tb", concatModuleNames(mlist(modules)), recordingStart);
  }
}
