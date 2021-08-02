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

import org.apache.commons.lang3.StringUtils;

import org.joda.time.DateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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
  @Path("/modules/activities/ids")
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

    try {
      return RemoteObjectUtil.writeObjectResponse(ids);
    } catch (IOException e) {
      logger.error("Unable to write activity ids response", e);
      return Response.serverError().build();
    }
  }

  @GET
  @Path("/modules/all")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "module_all", description = "get all modules",
          returnDescription = "List of Module",
          reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "All modules"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Modules not found"),
                  @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to query S+ database")
          })
  public Response getAllModule() {
    try {
      List<VModule> modules = syllabusDataService.getAllModule();
      if (modules == null) {
        return Response.status(Status.NOT_FOUND).build();
      }
      return RemoteObjectUtil.writeJson(modules);
    } catch (Exception e) {
      logger.warn("Could not get modules");
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
  @Path("/activities")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "activities_by_parameter", description = "Search activities by parameters. Parameters modulename, staffid and locationid are mutually exclusive",
          returnDescription = "List of activities",
          restParameters = {
                  @RestParameter(name = "modulename", type = STRING, isRequired = false, description = "Module name"),
                  @RestParameter(name = "staffid", type = STRING, isRequired = false, description = "S+ Staff ID"),
                  @RestParameter(name = "locationid", type = STRING, isRequired = false, description = "Location id"),
                  @RestParameter(name = "start", type = STRING, isRequired = false, description = "Start date"),
                  @RestParameter(name = "end", type = STRING, isRequired = false, description = "End date")
          },
          reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Activities for module"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Activities not found"),
                  @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to query S+ database")
          })
  public Response findActivityByParameter(@QueryParam("modulename") String moduleName, @QueryParam("staffid") String staffId,
                                          @QueryParam("locationid") String locationId, @QueryParam("start") String startDate, @QueryParam("end") String endDate) {
    List<Activities> activities = null;
    String parameter = null;
    String parameterValue = null;

    try {
      if (StringUtils.isNotBlank(moduleName)) {
        activities = syllabusDataService.findActivityByModule('%' + moduleName + '%');
        parameter = "module name";
        parameterValue = moduleName;
      } else if (StringUtils.isNotBlank(staffId)) {
        activities = syllabusDataService.findActivityByStaffId(staffId);
        parameter = "staff id";
        parameterValue = staffId;
      } else if (StringUtils.isNotBlank(locationId)) {
        DateTime sdate = new DateTime(startDate);
        DateTime edate = new DateTime(endDate);
        activities = syllabusDataService.findActivitiesByLocation(locationId, sdate, edate);
        parameter = "location id";
        parameterValue = locationId;
      } else {
        logger.warn("No parameter provided");
        return Response.serverError().build();
      }

      if (activities.isEmpty()) {
        return Response.status(Status.NOT_FOUND).build();
      }
      return RemoteObjectUtil.writeJson(activities);
    } catch (Exception e) {
      logger.warn("Could not get activities for" + parameter + ": '{}'", parameterValue);
      return Response.serverError().build();
    }
  }

  @GET
  @Path("/activities/{id}/child")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "activities_by_child_activity_id", description = "Get child activities by activity id",
          returnDescription = "List of child activities",
          pathParameters = { @RestParameter(name = "id", type = STRING, isRequired = true, description = "Activity ID") },
          reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Child Activities matching activity ID"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Activities not found"),
                  @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to query S+ database")
          })
  public Response findChildActivity(@PathParam("id") String activityId) {
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
  @Path("/modules/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "module_by_id", description = "Find module details matching id",
          returnDescription = "Module",
          pathParameters = {
                  @RestParameter(name = "id", type = STRING, isRequired = true, description = "Module ID")
          },
          reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "module matching module ID"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "module not found"),
                  @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to query S+ database")
          })
  public Response findModuleById(@PathParam("id") String id) {
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
  @Path("/staff/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "find_staff_by_id", description = "Find staff by spotId",
          returnDescription = "Staff",
          pathParameters = {
                  @RestParameter(name = "id", type = STRING, isRequired = true, description = "staff spotId")
          },
          reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "staff matching spot ID"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "staff not found"),
                  @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to query S+ database")
          })
  public Response findStaffById(@PathParam("id") String spotId) {
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
  @Path("/staff")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "find_staff_by_activity", description = "Find staff by activity",
          returnDescription = "Staff",
          restParameters = {
                  @RestParameter(name = "activityid", type = STRING, isRequired = true, description = "activity ID")
          },
          reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "staff matching activity ID"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "staff not found"),
                  @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to query S+ database")
          })
  public Response findStaffByStaffActivityId(@QueryParam("activityid") String activityId) {
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
  @Path("/locations")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "find_location_by_activity", description = "Find location by activity",
          returnDescription = "Location details",
          restParameters = {
                  @RestParameter(name = "activityid", type = STRING, isRequired = true, description = "activity ID")
          },
          reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "location matching activity ID"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "location not found"),
                  @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to query S+ database")
          })
  public Response findByActivityLocationId(@QueryParam("activityid") String activityId) {
    try {
      List<VActivityLocation> location = syllabusDataService.findByActivityLocationId(activityId);
      if (location == null) {
        return Response.status(Status.NOT_FOUND).build();
      }
      return RemoteObjectUtil.writeJson(location);
    } catch (Exception e) {
      logger.warn("Could not find location with activity id : '{}'", activityId);
      return Response.serverError().build();
    }
  }

  @GET
  @Path("/locations/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "location_by_name", description = "Find location details matching name",
          returnDescription = "Location",
          pathParameters = {
                  @RestParameter(name = "name", type = STRING, isRequired = true, description = "Location name")
          },
          reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "location matching location name"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "location not found"),
                  @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to query S+ database")
          })
  public Response findLocationByName(@PathParam("name") String name) {
    try {
      VLocation location = syllabusDataService.findLocationByName(name);
      if (location == null) {
        return Response.status(Status.NOT_FOUND).build();
      }
      return RemoteObjectUtil.writeJson(location);
    } catch (Exception e) {
      logger.warn("Could not find location with name : '{}'", name);
      return Response.serverError().build();
    }
  }

  @GET
  @Path("/activities/{id}/datetime")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "find_activity_start_and_end", description = "Find activity start and end",
          returnDescription = "List of activities",
          pathParameters = {
                  @RestParameter(name = "id", type = STRING, isRequired = true, description = "activity ID")
          },
          reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Find activities start and end date by ID"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "activities start and end date not found"),
                  @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to query S+ database")
          })
  public Response findActivityDateTime(@PathParam("id") String activityId) {
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
  @Path("/suitabilities")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "find_suitability_by_location", description = "Find suitability by location",
          returnDescription = "Suitability details",
          restParameters = {
                  @RestParameter(name = "locationid", type = STRING, isRequired = true, description = "location ID")
          },
          reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "suitabilities matching location ID"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "suitability not found"),
                  @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to query S+ database")
          })
  public Response findSuitabilityByLocationid(@QueryParam("locationid") String locationId) {
    try {
      List<VLocation> suitability = syllabusDataService.findSuitabilityByLocationId(locationId);
      if (suitability == null) {
        return Response.status(Status.NOT_FOUND).build();
      }
      return RemoteObjectUtil.writeJson(suitability);
    } catch (Exception e) {
      logger.warn("Could not find suitabilities with location id : '{}'", locationId);
      return Response.serverError().build();
    }
  }


  @GET
  @Path("/activities/{id}/parents")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "activities_parents", description = "Get parents activities by activity id",
          returnDescription = "List of parent activities",
          pathParameters = { @RestParameter(name = "id", type = STRING, isRequired = true, description = "Activity id") },
          reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Find parents activities"),
                  @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "Activities not found"),
                  @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to query S+ database")
          })
  public Response findParentActivities(@PathParam("id") String activityId) {
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
