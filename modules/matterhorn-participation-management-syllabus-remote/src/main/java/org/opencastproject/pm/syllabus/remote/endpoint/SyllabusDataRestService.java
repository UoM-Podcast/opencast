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
package org.opencastproject.pm.syllabus.remote.endpoint;

import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.pm.syllabus.api.Activities;
import org.opencastproject.pm.syllabus.api.SyllabusData;
import org.opencastproject.pm.syllabus.api.SyllabusDataService;
import org.opencastproject.pm.syllabus.api.VActivityDateTime;
import org.opencastproject.pm.syllabus.api.VActivityLocation;
import org.opencastproject.pm.syllabus.api.VLocation;
import org.opencastproject.pm.syllabus.api.VModule;
import org.opencastproject.pm.syllabus.api.VStaff;
import org.opencastproject.pm.syllabus.impl.RemoteObjectUtil;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.google.common.base.Strings;

import org.joda.time.DateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("/")
@RestService(name = "syllabus_data_service", title = "Syllabus Data Service", abstractText = "Provide remote access to S+ data", notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is "
                + "not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
                + "other words, there is a bug! You should file an error report with your server logs from the time when the "
                + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>" })
public class SyllabusDataRestService {
  private static transient Logger logger = LoggerFactory.getLogger(SyllabusDataRestService.class);

  private transient SyllabusDataService syllabusDataService;

  @GET
  @Path("/")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @RestQuery(name = "fetch", description = "Get a SyllabusData object stream populated with data",
          returnDescription = "SyllabusData serialized object",
          restParameters = {
            @RestParameter(name = "subset", type = STRING, isRequired = false, description = "Return a subset of the complete data, valid values are:\n 'modules'")
          },
          reponses = {
            @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "SyllabusData returned"),
            @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "subset is not a valid value"),
            @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "SyllabusData can not be generated")})
  public Response fetch(@QueryParam("subset") String subset) {
    SyllabusData syllabusData;

    if (Strings.isNullOrEmpty(subset)) {
      syllabusData = syllabusDataService.fetch();
    } else if ("modules".equalsIgnoreCase(subset)) {
      syllabusData = syllabusDataService.fetchModules();
    } else {
      return Response.status(Status.BAD_REQUEST).build();
    }

    if (syllabusData == null) {
      return Response.serverError().build();
    }

    try {
      return RemoteObjectUtil.writeObjectResponse(syllabusData);
    } catch (IOException e) {
      logger.error("Unable to write fetch SyllabusData response", e);
      return Response.serverError().build();
    }
  }

  @GET
  @Path("/description")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "source_description", description = "String that descibes this source",
          returnDescription = "String description",
          reponses = {
            @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Source desciption returned"),
            @RestResponse(responseCode = HttpServletResponse.SC_FOUND, description = "Source desciption not set")
          })
  public Response getSourceDescription() {
    return Response.ok().entity(syllabusDataService.getSourceDescription()).build();
  }

  @GET
  @Path("/modules")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @RestQuery(name = "modules", description = "Module matching specified coursekey",
          returnDescription = "VModule serialized object",
          restParameters = {
            @RestParameter(name = "coursekey", type = STRING, isRequired = true, description = "Course MELID")
          },
          reponses = {
            @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Module for coursekey"),
            @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Module not found"),
            @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to query S+ database")
          })
  public Response getModuleByCourseKey(@QueryParam("coursekey") String courseKey) {
    VModule module = syllabusDataService.getModuleByCourseKey(courseKey);

    if (module == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    try {
      logger.info("module found");
      return RemoteObjectUtil.writeObjectResponse(module);
    } catch (IOException e) {
      logger.error("Unable to write modules response", e);
      return Response.serverError().build();
    }
  }

  @GET
  @Path("/modules/activites/ids")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @RestQuery(name = "module_activtiy_ids", description = "Module activity ids matching specified coursekey",
          returnDescription = "List<String> serialized object",
          restParameters = {
            @RestParameter(name = "coursekey", type = STRING, isRequired = true, description = "Course MELID")
          },
          reponses = {
            @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Activity Ids for module coursekey"),
            @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Activities not found"),
            @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to query S+ database")
          })
  public Response findModuleActivityIdsByCourseKey(@QueryParam("coursekey") String courseKey) {
    List<String> ids = syllabusDataService.findModuleActivityIdsByCourseKey(courseKey);

    if (ids.isEmpty()) {
      return Response.status(Status.NOT_FOUND).build();
    }

    try {
      return RemoteObjectUtil.writeObjectResponse(ids);
    } catch (IOException e) {
      logger.error("Unable to write activity ids response", e);
      return Response.serverError().build();
    }
  }

  @GET
  @Path("/activites/datetime")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @RestQuery(name = "activties_datetime_by_range", description = "Activities datetime by id range",
          returnDescription = "List<VActivityDateTime> serialized object",
          restParameters = {
            @RestParameter(name = "startid", type = STRING, isRequired = true, description = "Start activity id"),
            @RestParameter(name = "endid", type = STRING, isRequired = true, description = "End activity id")
          },
          reponses = {
            @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Activity Ids for module coursekey"),
            @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Activities not found"),
            @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to query S+ database")
          })
  public Response findActivityDateTimeByRange(@QueryParam("startid") String startActivityId, @QueryParam("endid") String endActivityId) {
    List<VActivityDateTime> datetimes = syllabusDataService.findActivityDateTimeByRange(startActivityId, endActivityId);

    if (datetimes == null || datetimes.isEmpty()) {
      return Response.status(Status.NOT_FOUND).build();
    }

    try {
      return RemoteObjectUtil.writeObjectResponse(datetimes);
    } catch (IOException e) {
      logger.error("Unable to write datetimes response", e);
      return Response.serverError().build();
    }
  }

  @GET
  @Path("/activities/staff")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "activties_by_staff", description = "Get activities by staff id",
          returnDescription = "List of staff activities",
          restParameters = { @RestParameter(name = "staffid", type = STRING, isRequired = true, description = "S+ Staff ID") },
          reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Activities for staff"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Activities not found"),
                  @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to query S+ database")
          })
  public Response findActivityByStaffId(@QueryParam("staffid") String staffId) {
    try {
      List<Activities> activities = syllabusDataService.findActivityByStaffId(staffId);
      if (activities.isEmpty()) {
        return Response.status(Status.NOT_FOUND).build();
      }
      return RemoteObjectUtil.writeJson(activities);
    } catch (Exception e) {
      logger.warn("Could not get staff activities for staff ID: '{}'", staffId);
      return Response.serverError().build();
    }
  }

  @GET
  @Path("/activities/module")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "activities_by_module_name", description = "Search activities by module name",
          returnDescription = "List of activities",
          restParameters = { @RestParameter(name = "name", type = STRING, isRequired = true, description = "Module name") },
          reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Activities for module"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Activities not found"),
                  @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to query S+ database")
          })
  public Response findActivityByModule(@QueryParam("name") String moduleName) {
    try {
      List<Activities> activities = syllabusDataService.findActivityByModule('%' + moduleName + '%');
      if (activities.isEmpty()) {
        return Response.status(Status.NOT_FOUND).build();
      }
      return RemoteObjectUtil.writeJson(activities);
    } catch (Exception e) {
      logger.warn("Could not get activities for module name: '{}'", moduleName);
      return Response.serverError().build();
    }
  }

  @GET
  @Path("/activities/child")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "activtiy_by_child_activity_id", description = "Get child activities by activity id",
          returnDescription = "List of child activities",
          restParameters = { @RestParameter(name = "activityid", type = STRING, isRequired = true, description = "Activity ID") },
          reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Child Activities for activity"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Activities not found"),
                  @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to query S+ database")
          })
  public Response findChildActivity(@QueryParam("activityid") String activityId) {
    try {
      List<Activities> activities = syllabusDataService.findChildActivity(activityId);
      if (activities.isEmpty()) {
        return Response.status(Status.NOT_FOUND).build();
      }
      return RemoteObjectUtil.writeJson(activities);
    } catch (Exception e) {
      logger.warn("Could not get child activities for activity: '{}'", activityId);
      return Response.serverError().build();
    }
  }

  @GET
  @Path("/module/find")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "module_by_id", description = "Find module details matching id",
          returnDescription = "Module",
          restParameters = {
                  @RestParameter(name = "id", type = STRING, isRequired = true, description = "Module ID")
          },
          reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "module matching module id"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "module not found"),
                  @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to query S+ database")
          })
  public Response findModuleById(@QueryParam("id") String id) {
    try {
      VModule module = syllabusDataService.getModuleById(id);
      if (module == null) {
        return Response.status(Status.NOT_FOUND).build();
      }
      return RemoteObjectUtil.writeJson(module);
    } catch (Exception e) {
      logger.warn("Could not get module with id : '{}'", id);
      return Response.serverError().build();
    }
  }

  @GET
  @Path("/staff/find")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "find_staff_by_id", description = "Find staff by spotId",
          returnDescription = "Staff",
          restParameters = {
                  @RestParameter(name = "id", type = STRING, isRequired = true, description = "staff spotId")
          },
          reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "staff matching spot ID"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "staff not found"),
                  @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to query S+ database")
          })
  public Response findStaffById(@QueryParam("id") String spotId) {
    try {
      VStaff staff = syllabusDataService.findStaffById(spotId);
      if (staff == null) {
        return Response.status(Status.NOT_FOUND).build();
      }
      return RemoteObjectUtil.writeJson(staff);
    } catch (Exception e) {
      logger.warn("Could not get staff with spotId : '{}'", spotId);
      return Response.serverError().build();
    }
  }

  @GET
  @Path("/staff/activity")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "find_staff_by_activity", description = "Find staff by activity",
          returnDescription = "Staff",
          restParameters = {
                  @RestParameter(name = "activityId", type = STRING, isRequired = true, description = "activity ID")
          },
          reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "staff matching activity ID"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "staff not found"),
                  @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to query S+ database")
          })
  public Response findStaffByStaffActivityId(@QueryParam("activityId") String activityId) {
    try {
      List<VStaff> staff = syllabusDataService.findStaffByStaffActivityId(activityId);
      if (staff == null) {
        return Response.status(Status.NOT_FOUND).build();
      }
      return RemoteObjectUtil.writeJson(staff);
    } catch (Exception e) {
      logger.warn("Could not get staff with spotId : '{}'", activityId);
      return Response.serverError().build();
    }
  }

  @GET
  @Path("/activity/location")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "find_location_by_activity", description = "Find location by activity",
          returnDescription = "Location details",
          restParameters = {
                  @RestParameter(name = "activityId", type = STRING, isRequired = true, description = "activity ID")
          },
          reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "location matching activity ID"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "location not found"),
                  @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to query S+ database")
          })
  public Response findByActivityLocationId(@QueryParam("activityId") String activityId) {
    try {
      List<VActivityLocation> location = syllabusDataService.findByActivityLocationId(activityId);
      if (location == null) {
        return Response.status(Status.NOT_FOUND).build();
      }
      return RemoteObjectUtil.writeJson(location);
    } catch (Exception e) {
      logger.warn("Could not location with activity id : '{}'", activityId);
      return Response.serverError().build();
    }
  }

  @GET
  @Path("/activity/datetime")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "find_activity_start_and_end", description = "Find activity start and end",
          returnDescription = "List of activities",
          restParameters = {
                  @RestParameter(name = "activityId", type = STRING, isRequired = true, description = "activity ID")
          },
          reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Find activity start and end by ID"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "location not found"),
                  @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to query S+ database")
          })
  public Response findActivityDateTime(@QueryParam("activityId") String activityId) {
    try {
      List<VActivityDateTime> activity = syllabusDataService.findActivityDateTime(activityId);
      if (activity == null) {
        return Response.status(Status.NOT_FOUND).build();
      }
      return RemoteObjectUtil.writeJson(activity);
    } catch (Exception e) {
      logger.warn("Could not find start and end with activity id : '{}'", activityId);
      return Response.serverError().build();
    }
  }

  @GET
  @Path("/location/suitability")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "find_location_by_activity", description = "Find location by activity",
          returnDescription = "Location details",
          restParameters = {
                  @RestParameter(name = "locationId", type = STRING, isRequired = true, description = "location ID")
          },
          reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "location matching activity ID"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "location not found"),
                  @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to query S+ database")
          })
  public Response findSuitabilityByLocationId(@QueryParam("locationId") String activityId) {
    try {
      List<VLocation> suitability = syllabusDataService.findSuitabilityByLocationId(activityId);
      if (suitability == null) {
        return Response.status(Status.NOT_FOUND).build();
      }
      return RemoteObjectUtil.writeJson(suitability);
    } catch (Exception e) {
      logger.warn("Could not location with activity id : '{}'", activityId);
      return Response.serverError().build();
    }
  }

  @GET
  @Path("/activities/location")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "activties_by_location_and_datetime_range", description = "Activities by location and datetime range",
          returnDescription = "List<Activities> object",
          restParameters = {
                  @RestParameter(name = "locationId", type = STRING, isRequired = true, description = "Location id"),
                  @RestParameter(name = "start", type = STRING, isRequired = true, description = "Start date"),
                  @RestParameter(name = "end", type = STRING, isRequired = true, description = "End date")
          },
          reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Activities in given location and date range"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Activities not found"),
                  @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to query S+ database")
          })
  public Response findActivitiesByLocation(@QueryParam("locationId") String locationId, @QueryParam("start") String startDate, @QueryParam("end") String endDate) {
    DateTime sdate = new DateTime(startDate);
    DateTime edate = new DateTime(endDate);
    try {
      List<Activities> activities = syllabusDataService.findActivitiesByLocation(locationId, sdate, edate);
      if (activities  == null) {
        return Response.status(Status.NOT_FOUND).build();
      }
      return RemoteObjectUtil.writeJson(activities);
    } catch (Exception e) {
      logger.warn("Could not find activities with location id : '{}'", locationId);
      return Response.serverError().build();
    }
  }

  @GET
  @Path("/activity/parent")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "activity_parent", description = "Get parent activities by activity id",
          returnDescription = "List of parent activities",
          restParameters = { @RestParameter(name = "id", type = STRING, isRequired = true, description = "Activity id") },
          reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Find parent activities"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Activities not found"),
                  @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to query S+ database")
          })
  public Response findParentActivities(@QueryParam("id") String activityId) {
    try {
      List<Activities> activities = syllabusDataService.findParentActivities(activityId);
      if (activities.isEmpty()) {
        return Response.status(Status.NOT_FOUND).build();
      }
      return RemoteObjectUtil.writeJson(activities);
    } catch (Exception e) {
      logger.warn("Could not find parents activities for activity id: '{}'", activityId);
      return Response.serverError().build();
    }
  }

/** OSGi container callback. */
  public void setSyllabusDataService(SyllabusDataService syllabusDataService) {
    this.syllabusDataService = syllabusDataService;
  }
}
