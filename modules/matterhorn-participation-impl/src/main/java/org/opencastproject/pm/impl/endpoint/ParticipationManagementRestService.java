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

package org.opencastproject.pm.impl.endpoint;


import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.pm.api.Course;
import org.opencastproject.pm.api.Recording.ReviewStatus;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabaseException;
import org.opencastproject.pm.api.scheduling.ParticipationFeederService;
import org.opencastproject.pm.api.scheduling.ScheduleFeederService;
import org.opencastproject.pm.api.scheduling.SnapCountService;
import org.opencastproject.pm.api.util.Security;
import org.opencastproject.pm.impl.scheduling.ParticipationManagementProvider;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.Crypt;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Rest endpoint for participation management
 */
@Path("/")
@RestService(name = "participationmanagementservice", title = "Participation Management Service", abstractText = "This service provides help for managing participation mangement", notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is "
                + "not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
                + "other words, there is a bug! You should file an error report with your server logs from the time when the "
                + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>" })
public class ParticipationManagementRestService {

  /**
   * The participation statuses to set
   */
  private static final String TRIM = "TRIM";
  private static final String NO_TRIM = "NO_TRIM";
  private static final String OPT_OUT = "OPT_OUT";

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(ParticipationManagementRestService.class);

  private ParticipationFeederService feederService;
  private ScheduleFeederService scheduleFeederService;
  private ParticipationManagementDatabase persistence;
  private SecurityService securityService;
  private SeriesService seriesService;
  private SnapCountService snapCountService;

  /**
   * Callback from the OSGi declarative services to set the participation management persistence
   *
   * @param persistence
   *          the participation management persistence
   */
  public void setPersistence(ParticipationManagementDatabase persistence) {
    this.persistence = persistence;
  }

  /**
   * Callback from the OSGi declarative services to set the feeder service.
   *
   * @param feederService
   *          the feeder service
   */
  public void setFeederService(ParticipationFeederService feederService) {
    this.feederService = feederService;
  }

  /**
   * Callback from the OSGi declarative services to unset the feeder service.
   *
   * @param feederService
   *          the feeder service
   */
  public void unsetFeederService(ParticipationFeederService feederService) {
    this.feederService = null;
  }

  /**
   * Callback from the OSGi declarative services to set the feeder service.
   *
   * @param scheduleFeederService
   *          the schedule feeder service
   */
  public void setScheduleFeederService(ScheduleFeederService scheduleFeederService) {
    this.scheduleFeederService = scheduleFeederService;
  }

  /**
   * Callback from the OSGi declarative services to set the snap count service.
   *
   * @param snapCountService
   *          the snap count service
   */
  public void setSnapCountService(SnapCountService snapCountService) {
    this.snapCountService = snapCountService;
  }

  /** OSGi container callback. */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi container callback. */
  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  // FIXME: Should be GET /activity/{id}/seriesid
  @POST
  @Path("/activity/{id}/series")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "activity_series", description = "Get the seriesid and create it if required for the specified activity", returnDescription = "", pathParameters = {
          @RestParameter(name = "id", isRequired = false, description = "S+ Activity Id", type = STRING) }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_CREATED, description = "Series created"),
          @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Activity id not found"),
          @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Series could not be found or created"),
          @RestResponse(responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE, description = "An external service not available")})
  public Response activitySeriesId(@PathParam("id") String activityId) throws Exception {
    if (feederService == null) {
      logger.info("Can't harvest from external feeder service: service not available!");
      return Response.status(Status.SERVICE_UNAVAILABLE).build();
    }

    return internalActivitySeriesId(activityId, true);
  }

  @GET
  @Path("/module/{id}/seriesid")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "activity_series", description = "Get the seriesid and create it if required for the specified activity", returnDescription = "", pathParameters = {
          @RestParameter(name = "id", isRequired = false, description = "Module Course MLE Id", type = STRING) }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_CREATED, description = "Series created"),
          @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Activity id not found"),
          @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Series could not be found or created"),
          @RestResponse(responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE, description = "An external service not available")})
  public Response moduleSeriesId(@PathParam("id") String mleId) throws Exception {
    if (feederService == null) {
      logger.info("Can't harvest from external feeder service: service not available!");
      return Response.status(Status.SERVICE_UNAVAILABLE).build();
    }

    List<String> activityIds = feederService.getActivityIds(mleId);

    if (activityIds.isEmpty()) {
      // if no activities create series from supplied MELID
      return internalCourseKeySeriesId(mleId, false);
    }

    // Just need one activity id correctly determine series id
    return internalActivitySeriesId(activityIds.get(0), false);
  }

  @GET
  @Path("/optout/id")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "optout_id", description = "Get OptOut URI for described person", returnDescription = "", restParameters = {
          @RestParameter(name = "email", isRequired = true, description = "Person's email address", type = STRING) }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Optout ID return"),
          @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Series could not be found or created")})
  public Response optoutID(@QueryParam("email") String email) throws Exception {
    String id = Crypt.encrypt(Security.TEACHER_EMAIL_KEY, email);

    return Response.ok().entity(id).build();
  }

  @POST
  @Path("/harvest")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "harvest", description = "Harvest external data in the participation management module", returnDescription = "", reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = ""),
          @RestResponse(responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE, description = "External feeder service not available") })
  public Response synchronize() throws Exception {
    if (feederService == null) {
      logger.info("Can't harvest from external feeder service: service not available!");
      return Response.status(Status.SERVICE_UNAVAILABLE).build();
    }
    feederService.harvest();
    return Response.ok().build();
  }

  @POST
  @Path("/synchronizeScheduler")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "synchronizeMatterhornScheduler", description = "Synchronize the recordings from particiaptation management module with the Matterhorn Scheduler", returnDescription = "", reponses = { @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "") })
  public Response schedule() throws Exception {
    scheduleFeederService.synchronize();
    return Response.ok().build();
  }

  @POST
  @Path("/resetPM")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "reset", description = "Reset only the participation management database", returnDescription = "", reponses = { @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "") })
  public Response resetParticipationManagement() throws Exception {
    persistence.clearDatabase();
    return Response.ok().build();
  }

  @POST
  @Path("/resetMatterhorn")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "reset", description = "WARNING this will DELETE data. Delete the participation management related events and series created in Matterhorn. WARNING: This will delete schedule", returnDescription = "", reponses = { @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "") })
  public Response resetMatterhorn() throws Exception {
    scheduleFeederService.clearScheduledEvents();
    scheduleFeederService.clearSeries();
    return Response.ok().build();
  }

  @POST
  @Path("/resetAll")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "reset", description = "WARNING this will DELETE data. Reset the participation management database and delete the related events and series created in Matterhorn", returnDescription = "", reponses = { @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "") })
  public Response reset() throws Exception {
    scheduleFeederService.clearScheduledEvents();
    scheduleFeederService.clearSeries();
    persistence.clearDatabase();
    return Response.ok().build();
  }

  @POST
  @Path("/harvestSync")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "harvestSync", description = "Harvest external data in the participation management and synchronize all events with the Matterhorn scheduler",
          returnDescription = "", reponses = {
            @RestResponse(responseCode = HttpServletResponse.SC_PRECONDITION_FAILED , description = "The participation management module does not allow a combined harvest"),
            @RestResponse(responseCode = HttpServletResponse.SC_PARTIAL_CONTENT , description = "The participation management module did not complete in a way that a schedule update is possible"),
            @RestResponse(responseCode = HttpServletResponse.SC_OK, description = ""),
            @RestResponse(responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE, description = "An external service not available")})
  public Response harvestSynchronize() throws Exception {
    if (snapCountService == null) {
      logger.info("Can't harvest from external feeder service: service not available!");
      return Response.status(Status.SERVICE_UNAVAILABLE).build();
    }
    Response response = feederService.permitHarvest();
    logger.info("permit Harvest response: {}", response.getStatus());
    if (Response.ok().build().getStatus() != response.getStatus()) {
      logger.info("Can't harvest from external feeder service: harvest not possible try initial harvest through UI!");
      return response;
    }
    snapCountService.harvestSync();
    return Response.ok().build();
  }


  @PUT
  @Path("/recording/{id}/status")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "reset", description = "Modify the recording status", returnDescription = "", pathParameters = { @RestParameter(name = "id", isRequired = true, description = "The recording identifier", type = Type.INTEGER) }, restParameters = { @RestParameter(name = "status", isRequired = true, description = "The recording status: OPT_OUT, NO_TRIM, TRIM", type = STRING) }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "Unkown recording status"),
          @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "The recording has not been found"),
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Recording status has been changed") })
  public Response modifyRecordingStatus(@PathParam("id") long recordingId, @FormParam("status") String status)
          throws NotFoundException {
    String lowerCaseStatus = status.toLowerCase();
    try {
      if (OPT_OUT.toLowerCase().equals(lowerCaseStatus)) {
        persistence.reviewRecording(recordingId, ReviewStatus.OPTED_OUT);
        persistence.trimRecording(recordingId, false);
      } else if (NO_TRIM.toLowerCase().equals(lowerCaseStatus)) {
        persistence.reviewRecording(recordingId, ReviewStatus.CONFIRMED);
        persistence.trimRecording(recordingId, false);
      } else if (TRIM.toLowerCase().equals(lowerCaseStatus)) {
        persistence.reviewRecording(recordingId, ReviewStatus.CONFIRMED);
        persistence.trimRecording(recordingId, true);
      } else {
        logger.warn("Unknown participation status: {} ", status);
        return Response.status(Status.BAD_REQUEST).build();
      }
    } catch (ParticipationManagementDatabaseException e) {
      logger.error("Unable to modify the participation status of recording {} to {}", recordingId, status);
      throw new WebApplicationException(e);
    }
    return Response.ok().build();
  }

  private Response internalActivitySeriesId(String activityId, Boolean post) {
    try {
      Course course = feederService.getCourseByActivityId(activityId);

      return createCourseSeries(course, post);

    } catch (NotFoundException e) {
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (WebApplicationException e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  private Response internalCourseKeySeriesId(String courseKey, Boolean post) {
    try {
      Course course = feederService.getCourseByCourseKey(courseKey);

      return createCourseSeries(course, post);

    } catch (NotFoundException e) {
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (WebApplicationException e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  private Response createCourseSeries(Course course, Boolean post) {
    String seriesId = course.getSeriesId();
    try {
      seriesService.getSeries(seriesId);
    } catch (NotFoundException e) {
      logger.debug("Creating series {} in Matterhorn", seriesId);
      DublinCoreCatalog seriesDC = ParticipationManagementProvider.toSeriesCatalog(course);
      AccessControlList acl = ParticipationManagementProvider.createACL(course, seriesService, securityService);

      try {
        seriesService.updateSeries(seriesDC);
        seriesService.updateAccessControl(seriesId, acl);
      } catch (SeriesException ee) {
        logger.error("Error creating series {}", seriesId);
        throw new WebApplicationException();
      } catch (NotFoundException ee) {
        logger.error("Previous created series '{}' could not be found!", seriesId);
        throw new WebApplicationException();
      } catch (UnauthorizedException ee) {
        logger.error("Does not have right to create a new series: {}", ee.getMessage());
        throw new WebApplicationException();
      }
    } catch (SeriesException e) {
      logger.error("Error searching for series {}", seriesId);
      throw new WebApplicationException();
    } catch (UnauthorizedException ee) {
      logger.error("Does not have right to access series: {}", ee.getMessage());
      throw new WebApplicationException();
    }

    if (post) {
      return Response.status(Response.Status.CREATED).entity(seriesId).build();
    } else {
      return Response.ok().entity(seriesId).build();
    }
  }
}
