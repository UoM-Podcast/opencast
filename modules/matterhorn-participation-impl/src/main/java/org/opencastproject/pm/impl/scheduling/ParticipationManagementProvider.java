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

import static org.opencastproject.util.data.Option.some;

import org.opencastproject.metadata.dublincore.DCMIPeriod;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.Precision;
import org.opencastproject.pm.api.Course;
import org.opencastproject.pm.api.Person;
import org.opencastproject.pm.api.Recording;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabaseException;
import org.opencastproject.pm.api.persistence.RecordingQuery;
import org.opencastproject.pm.api.scheduling.ParticipationManagementSchedulingException;
import org.opencastproject.pm.api.scheduling.Schedule;
import org.opencastproject.pm.api.scheduling.ScheduleProvider;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Cell;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParticipationManagementProvider implements ScheduleProvider {

  /** Log facility */
  protected static final Logger logger = LoggerFactory.getLogger(ParticipationManagementProvider.class);

  public static final String ACADEMIC_ROLE_PREFIX = "ACADEMIC_";
  public static final String STUDENT_ROLE_PREFIX = "STUDENT_";

  private final ParticipationManagementDatabase participationManagementDB;
  private final SeriesService seriesService;
  private final SecurityService securityService;
  private boolean hasToCreateNewSeries = true;

  private final Cell<Integer> startMargin;
  private final Cell<Integer> endMargin;

  // eg I3039-PHYS-64631-1141-1SE-027176
  private static final Pattern courseKeySessionPattern = Pattern.compile(".*-.*-.*-\\d(\\d\\d)\\d-.*-.*");

  /**
   * @param pmDB
   *          the participation management database service
   * @param seriesService
   *          the series service
   * @param securityService
   *          the security service
   * @param hasToCreateNewSeries
   *          define if the participation management provider has to create or not the missing series
   */
  public ParticipationManagementProvider(ParticipationManagementDatabase pmDB, SeriesService seriesService,
          SecurityService securityService, Boolean hasToCreateNewSeries, Cell<Integer> startMargin,
          Cell<Integer> endMargin) {
    this.participationManagementDB = pmDB;
    this.seriesService = seriesService;
    this.securityService = securityService;
    this.hasToCreateNewSeries = hasToCreateNewSeries;
    this.startMargin = startMargin;
    this.endMargin = endMargin;
  }

  @Override
  public Schedule fetchSchedule(Date from, Date until) throws ParticipationManagementSchedulingException {
    if (from == null)
      throw new IllegalArgumentException("from date must not be null");
    if (until == null)
      throw new IllegalArgumentException("until date must not be null");

    // Prepare query
    RecordingQuery q = RecordingQuery.create().withDeleted();
    q.withStartDateRange(from);
    q.withEndDateRange(until);

    final List<Tuple<Recording, DublinCoreCatalog>> episodes = new ArrayList<Tuple<Recording, DublinCoreCatalog>>();
    Map<String, List<Tuple<Recording, DublinCoreCatalog>>> courseWithMissingSeriesId = new HashMap<String, List<Tuple<Recording, DublinCoreCatalog>>>();
    Map<String, List<Tuple<Recording, DublinCoreCatalog>>> courseWithExistingSeriesId = new HashMap<String, List<Tuple<Recording, DublinCoreCatalog>>>();

    // Get recordings
    List<Recording> recordings = null;
    try {
      logger.info("Collecting recordings from {} until {}", from, until);
      recordings = participationManagementDB.findRecordings(q);
      logger.info("{} recording events have been retrieved for synchronization", recordings.size());
    } catch (ParticipationManagementDatabaseException e) {
      logger.error("Unable to find recordings from the particpation management service: {}", e.getMessage());
    }

    // Create metadata catalogs
    logger.info("Preparing metadata catalogs for {} recording events", recordings.size());
    for (Recording rec : recordings) {
      episodes.add(new Tuple<Recording, DublinCoreCatalog>(rec, toEpisodeCatalog(rec, courseWithMissingSeriesId,
              courseWithExistingSeriesId, startMargin.get(), endMargin.get())));
    }
    logger.info("Metadata catalogs have been prepared", recordings.size());

    // Check courses with existing series
    if (courseWithExistingSeriesId.size() > 0) {
      int coursesSize = courseWithExistingSeriesId.size();
      logger.info("Matching {} courses associated with recording events with series", coursesSize);
      int courseIndex = 0; // Course index to use in log
      // Check if the existing series Id are valid
      for (String c : courseWithExistingSeriesId.keySet()) {
        Course course = courseWithExistingSeriesId.get(c).get(0).getA().getCourse().get();
        String seriesId = course.getSeriesId();
        DublinCoreCatalog seriesCatalog = null;
        courseIndex++;

        logger.debug(String.format("Check series Id %s from course %d. (%d/%d)", seriesId, course.getId(), courseIndex,
                coursesSize));
        try {
          seriesCatalog = seriesService.getSeries(seriesId);
          logger.debug("Series {} found", seriesId);
        } catch (SeriesException e) {
          logger.error("Error happend when searching for series {}: {}", seriesId, e.getMessage());
          throw new ParticipationManagementSchedulingException(e);
        } catch (NotFoundException e) {
          logger.warn("Series with id {} does not exit", seriesId);
          handleMissingSeries(course, courseWithExistingSeriesId.get(c));
          // As the missing series has been handled, no more work for this course, move to the next one.
          continue;
        } catch (UnauthorizedException e) {
          logger.error("Not authorized to search for series: {}", e.getMessage());
          throw new ParticipationManagementSchedulingException(e);
        }

        DublinCoreCatalog newSeriesCatalog = toSeriesCatalog(course);
        String md5 = null;
        try {
          md5 = DigestUtils.md5Hex(newSeriesCatalog.toXmlString());
        } catch (IOException e) {
          logger.error("Error calculating md5 sum from event metadata", e);
        }

        // If a catalog has been found, update the series, otherwise create it
        if (seriesCatalog != null) {
          if (course.getFingerprint().isNone() || !course.getFingerprint().get().equals(md5)) {
            logger.info("Fingerprint does not match, therefore update series {}", seriesId);
            try {
              seriesService.updateSeries(newSeriesCatalog);
            } catch (SeriesException e) {
              logger.error("Error during update of series {}: {}", seriesId, e.getMessage());
              throw new ParticipationManagementSchedulingException(e);
            } catch (UnauthorizedException e) {
              logger.error("Does not have right to update the series {}: {}", seriesId, e.getMessage());
              throw new ParticipationManagementSchedulingException(e);
            }

            course.setFingerprint(Option.some(md5));

            try {
              participationManagementDB.updateCourse(course);
            } catch (ParticipationManagementDatabaseException e) {
              logger.error("Not able to update course {}: {}", course, e.getMessage());
            }
          }

          if (StringUtils.isNotBlank(course.getExternalCourseKey())) {
            String[] courseKeys = StringUtils.split(course.getExternalCourseKey(), '_');
            try {
              Option<AccessControlList> seriesAccessControl = Option.option(seriesService
                      .getSeriesAccessControl(seriesId));

              logger.debug("Updating series access control for series {} in Matterhorn", seriesId);
              List<AccessControlEntry> accessControlEntries = new ArrayList<AccessControlEntry>();
//              for (String key : courseKeys) {
//                accessControlEntries.add(new AccessControlEntry(STUDENT_ROLE_PREFIX.concat(key),
//                        SeriesService.READ_CONTENT_PERMISSION, true));
//                accessControlEntries.add(new AccessControlEntry(ACADEMIC_ROLE_PREFIX.concat(key),
//                        SeriesService.READ_CONTENT_PERMISSION, true));
//              }

              // Adds previous entries to updated ACL
              if (seriesAccessControl.isSome()) {
//                AccessControlEntry publicRole = new AccessControlEntry(securityService.getOrganization()
//                        .getAnonymousRole(), SeriesService.READ_CONTENT_PERMISSION, true);
                for (AccessControlEntry entry : seriesAccessControl.get().getEntries()) {
                  // Make sure public role is not added
//                  if (publicRole.equals(entry))
//                    continue;
                  // Make sure outdated STUDENT and ACADEMIC roles are not added
                  if (entry.getRole().startsWith(STUDENT_ROLE_PREFIX)
                          || entry.getRole().startsWith(ACADEMIC_ROLE_PREFIX))
                    continue;
                  accessControlEntries.add(entry);
                }
              }
              seriesService.updateAccessControl(seriesId, new AccessControlList(accessControlEntries));
            } catch (SeriesException e) {
              logger.error("Error during series access control update : {}", e.getMessage());
              throw new ParticipationManagementSchedulingException(e);
            } catch (UnauthorizedException e) {
              logger.error("Does not have right to update the series access control: {}", e.getMessage());
              throw new ParticipationManagementSchedulingException(e);
            } catch (NotFoundException e) {
              logger.error("Existing series '{}' could not be found!", seriesId);
            }
          }
        } else {
          handleMissingSeries(course, courseWithExistingSeriesId.get(c));
        }
      }
    } else {
      logger.info("No course has a related series created in Matterhorn.");
    }

    // Create series for those courses that don't have a series associated
    if (courseWithMissingSeriesId.size() > 0) {
      logger.info("Creating series for {} courses", courseWithMissingSeriesId.size());
      for (String s : courseWithMissingSeriesId.keySet()) {
        Course course = courseWithMissingSeriesId.get(s).get(0).getA().getCourse().get();
        handleMissingSeries(course, courseWithMissingSeriesId.get(s));
      }
    }

    logger.info("{} recordings between {} and {} ready to be synchronized with Matterhorn", new Object[] { episodes.size(), from, until });
    return new Schedule() {
      @Override
      public List<Tuple<Recording, DublinCoreCatalog>> getEpisodes() {
        return episodes;
      }
    };
  }

  /**
   * Handle the missing series following the configuration of the participation management: It would create a new series
   * or throw an exception.
   *
   * @param course
   *          The course without corresponding series
   * @param recordingsWithDC
   *          A list of tuple containing the recordings in the course and their related dublin core catalog
   * @throws ParticipationManagementSchedulingException
   */
  public void handleMissingSeries(Course course, List<Tuple<Recording, DublinCoreCatalog>> recordingsWithDC)
          throws ParticipationManagementSchedulingException {

    if (hasToCreateNewSeries) {
      // Create the new series
      final String seriesId = course.createSeriesId();
      course.setSeriesId(seriesId);
      logger.info("Creating Matterhorn series {} for course {}", seriesId, course.getCourseId());
      DublinCoreCatalog dc = toSeriesCatalog(course);

      try {
        logger.debug("Creating series {} in Matterhorn", seriesId);
        seriesService.updateSeries(dc);
        seriesService.updateAccessControl(seriesId, createACL(course, seriesService, securityService));
      } catch (SeriesException e) {
        logger.error("Error during series creation : {}", e.getMessage());
        throw new ParticipationManagementSchedulingException(e);
      } catch (UnauthorizedException e) {
        logger.error("Does not have right to create a new series: {}", e.getMessage());
        throw new ParticipationManagementSchedulingException(e);
      } catch (NotFoundException e) {
        logger.error("Previous created series '{}' could not be found!", seriesId);
      }

      // Update the course
      try {
        String md5 = DigestUtils.md5Hex(dc.toXmlString());

        logger.debug("Updating course {} with newly created series identifier", seriesId);
        course.setSeriesId(seriesId);
        course.setFingerprint(Option.some(md5));
        course = participationManagementDB.updateCourse(course);
      } catch (ParticipationManagementDatabaseException e) {
        logger.error("Not able to update course {}: {}", course, e.getMessage());
        throw new ParticipationManagementSchedulingException(e);
      } catch (IOException e) {
        logger.error("Error calculating md5 sum from event metadata", e);
      }

      // Update the recordings and catalogs
      logger.debug("Updating {} recordings in course {}", recordingsWithDC.size(), seriesId);
      for (Tuple<Recording, DublinCoreCatalog> t : recordingsWithDC) {
        // Update recording
        Recording recording = t.getA();
        DublinCoreCatalog catalog = t.getB();
        recording.setCourse(some(course));
        try {
          recording = participationManagementDB.updateRecording(recording);
        } catch (ParticipationManagementDatabaseException e) {
          logger.error("Unable to update recording {} with new course: {}", recording.getId().get(), e.getMessage());
          throw new ParticipationManagementSchedulingException(e);
        }
        // Update catalog
        catalog.set(DublinCore.PROPERTY_IS_PART_OF, seriesId);
      }
      logger.info("Creation of series {} in Matterhorn finished. All the related {} recordings have been updated",
              seriesId, recordingsWithDC.size());
    } else {
      throw new ParticipationManagementSchedulingException("Course " + course.getCourseId()
              + " does not exist and the participation management provider is configured to not create new series");
    }
  }

  /**
   * Create the ACL for the Course object Series
  @param course
            The {@link org.opencastproject.pm.api.Course course} to create an ACL for
  @param seriesService
            Series service provider
  @param securityService
            Security service provider
  @return
  */
  public static AccessControlList createACL(Course course, SeriesService seriesService, SecurityService securityService) {
    List<AccessControlEntry> accessControlEntries = new ArrayList<AccessControlEntry>();
//    accessControlEntries.add(new AccessControlEntry(securityService.getOrganization().getAdminRole(),
//            SeriesService.READ_CONTENT_PERMISSION, true));
//    accessControlEntries.add(new AccessControlEntry(securityService.getOrganization().getAdminRole(),
//            SeriesService.EDIT_SERIES_PERMISSION, true));

    if (StringUtils.isNotBlank(course.getExternalCourseKey())) {
      String[] courseKeys = StringUtils.split(course.getExternalCourseKey(), '_');
//      for (String key : courseKeys) {
//        accessControlEntries.add(new AccessControlEntry(STUDENT_ROLE_PREFIX.concat(key),
//                SeriesService.READ_CONTENT_PERMISSION, true));
//        accessControlEntries.add(new AccessControlEntry(ACADEMIC_ROLE_PREFIX.concat(key),
//                SeriesService.READ_CONTENT_PERMISSION, true));
//      }
    } else {
      // Make all Ad-hoc series public
//      accessControlEntries.add(new AccessControlEntry(securityService.getOrganization().getAnonymousRole(),
//            SeriesService.READ_CONTENT_PERMISSION, true));
    }

    return new AccessControlList(accessControlEntries);
  }

  /**
   * Convert an {@link org.opencastproject.pm.api.Course course} into a Dublin Core catalog describing the course.
   *
   * @param course
   *          The {@link org.opencastproject.pm.api.Course course} to convert
   * @return a Dublin Core catalog
   */
  public static DublinCoreCatalog toSeriesCatalog(Course course) {
    final String courseId = course.getCourseId();
    final String seriesId = course.getSeriesId();
    final String courseKey = course.getExternalCourseKey();

    logger.debug("Creating Dublin Core catalog for course {}", courseId);

    DublinCoreCatalog dc = DublinCores.mkOpencastEpisode().getCatalog();
    dc.set(DublinCore.PROPERTY_IDENTIFIER, seriesId);

    if (StringUtils.isNotEmpty(course.getName())) {
      int year = 0;
      String session = "";
      if (StringUtils.isNotEmpty(courseKey)) {
        Matcher m = courseKeySessionPattern.matcher(courseKey);
        if (m.matches()) {
          year = Integer.parseInt(m.group(1));
        }
      }

      if (year == 0) {
        Calendar d = Calendar.getInstance();
        year = d.get(Calendar.YEAR) - 2000;
        if (d.get(Calendar.MONTH) < Calendar.SEPTEMBER) {
          year--;
        }
      }
      session = String.format(" 20%02d/%02d", year, year + 1);

      dc.set(DublinCore.PROPERTY_TITLE, String.format("%s%s",course.getName(), session));
    }
    if (StringUtils.isNotEmpty(course.getDescription())) {
      dc.set(DublinCore.PROPERTY_DESCRIPTION, course.getDescription());
    }
    dc.set(DublinCore.PROPERTY_IS_PART_OF, courseId);

    // Add extra DC terms
    // Series identification in an organization's external data source
    if (StringUtils.isNotEmpty(courseKey)) {
      dc.set(DublinCore.PROPERTY_RELATION, courseKey);
    }

    // From where did this series come from
    String source = "org.opencast.pm";
    // Scheduling Source
    if (course.getSchedulingSource().isSome()) {
      source = source + ":" + course.getSchedulingSource().get().getSource();
    }
    dc.set(DublinCore.PROPERTY_SOURCE, source);

    logger.info("Dublin Core Catalog {} for course {} created", seriesId, courseId);
    return dc;
  }

  /**
   * Convert an {@link org.opencastproject.pm.api.Recording recording} into a Dublin Core catalog describing the recording.
   *
   * @param recording
   *          The {@link org.opencastproject.pm.api.Recording recording} to convert
   * @param courseWithMissingSeriesId
   *          A map containing the Id of course without series Id as keys and a list of related recording/Dublin core
   *          catalog as value.
   * @return a Dublin Core catalog
   */
  public static DublinCoreCatalog toEpisodeCatalog(Recording recording,
          Map<String, List<Tuple<Recording, DublinCoreCatalog>>> courseWithMissingSeriesId,
          Map<String, List<Tuple<Recording, DublinCoreCatalog>>> courseWithExistingSeriesId, Integer startMargin,
          Integer endMargin) {
    logger.debug("Creating Dublin Core catalog for recording {}", recording.getId().get());

    DublinCoreCatalog dc = DublinCores.mkOpencastEpisode().getCatalog();

    Calendar startDateWithMargin = new GregorianCalendar();
    Calendar endDateWithMargin = new GregorianCalendar();
    startDateWithMargin.setTime(recording.getStart());
    startDateWithMargin.add(Calendar.MINUTE, -startMargin);
    endDateWithMargin.setTime(recording.getStop());
    endDateWithMargin.add(Calendar.MINUTE, endMargin);

    final DublinCoreValue period = EncodingSchemeUtils.encodePeriod(new DCMIPeriod(startDateWithMargin.getTime(),
            endDateWithMargin.getTime()), Precision.Second);
    final DublinCoreValue modificationDate = EncodingSchemeUtils.encodeDate(recording.getStart(), Precision.Second);

    dc.set(DublinCore.PROPERTY_IDENTIFIER, recording.getId().get().toString());
    dc.set(DublinCore.PROPERTY_TEMPORAL, period);
    dc.set(DublinCore.PROPERTY_TITLE, recording.getTitle());
    dc.set(DublinCore.PROPERTY_CREATED, modificationDate);

    // Add contributors
    if (!recording.getStaff().isEmpty()) {
      String allContributors = "";
      for (Person p : recording.getStaff()) {
        if (!allContributors.isEmpty())
          allContributors.concat(", " + p.getName());
        else
          allContributors.concat(p.getName());
      }
      dc.set(DublinCore.PROPERTY_CONTRIBUTOR, allContributors);
    }

    // Add extra DC terms
    // From where did this recording come from
    String source = "org.opencast.pm";
    // Scheduling Source
    if (recording.getSchedulingSource().isSome()) {
      // FIXME: This is a bodge as videolounge uses the episode's source to
      // determine if recording is a course lecture
      if (recording.getCourse().isSome() && StringUtils.isNotEmpty(recording.getCourse().get().getExternalCourseKey())) {
        source = source + ":" + recording.getSchedulingSource().get().getSource();
      } else {
        source = "ad-hoc:" + source + ":" + recording.getSchedulingSource().get().getSource();
      }
    }
    dc.set(DublinCore.PROPERTY_SOURCE, source);

    // * Date the recording was (considered) available.
    // * Leave end date open ended as retraction/deletion policy may change
    dc.set(DublinCore.PROPERTY_AVAILABLE,
            EncodingSchemeUtils.encodePeriod(new DCMIPeriod(recording.getStart(), null), Precision.Day));

    // Add rooms
    if (recording.getCaptureAgent() != null)
      dc.set(DublinCore.PROPERTY_SPATIAL, recording.getCaptureAgent().getMhAgent());

    if (recording.getCourse().isSome()) {
      String seriesId = recording.getCourse().get().getSeriesId();
      String courseId = recording.getCourse().get().getCourseId();
      if (StringUtils.isEmpty(seriesId)) {
        // If the series id does not exist, the missing id list is checked for the current course id
        logger.debug("Recording {} is part of course {} which has no series associated", recording.getId().get(),
                courseId);
        if (courseWithMissingSeriesId.containsKey(courseId)) {
          // If the course id is already present, the new tuple is simple added
          courseWithMissingSeriesId.get(courseId).add(new Tuple<Recording, DublinCoreCatalog>(recording, dc));
        } else {
          // Otherwise a new entry with the new course id
          List<Tuple<Recording, DublinCoreCatalog>> recordingsAndCatalogs = new ArrayList<Tuple<Recording, DublinCoreCatalog>>();
          recordingsAndCatalogs.add(new Tuple<Recording, DublinCoreCatalog>(recording, dc));
          courseWithMissingSeriesId.put(courseId, recordingsAndCatalogs);
        }
      } else {
        logger.debug("Recording {} is in course {} which is already represented in Matterhorn",
                recording.getId().get(), courseId);
        dc.set(DublinCore.PROPERTY_IS_PART_OF, seriesId);
        if (courseWithExistingSeriesId.containsKey(courseId)) {
          courseWithExistingSeriesId.get(courseId).add(new Tuple<Recording, DublinCoreCatalog>(recording, dc));
        } else {
          List<Tuple<Recording, DublinCoreCatalog>> recordingsAndCatalogs = new ArrayList<Tuple<Recording, DublinCoreCatalog>>();
          recordingsAndCatalogs.add(new Tuple<Recording, DublinCoreCatalog>(recording, dc));
          courseWithExistingSeriesId.put(courseId, recordingsAndCatalogs);
        }
      }
    } else {
      // If the recording has no series
      logger.warn("Recording {} has no course", recording.getId().get());
    }

    logger.debug("Dublin Core catalog for recording {} created", recording.getId().get());
    return dc;
  }
}
