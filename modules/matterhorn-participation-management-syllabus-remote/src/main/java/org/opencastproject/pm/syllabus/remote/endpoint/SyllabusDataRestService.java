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

import org.opencastproject.pm.syllabus.api.SyllabusData;
import org.opencastproject.pm.syllabus.api.SyllabusDataService;
import org.opencastproject.pm.syllabus.api.VActivityDateTime;
import org.opencastproject.pm.syllabus.api.VModule;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.google.common.base.Strings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

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
      return writeObjectResponse(syllabusData);
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
      return writeObjectResponse(module);
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
      return writeObjectResponse(ids);
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
      return writeObjectResponse(datetimes);
    } catch (IOException e) {
      logger.error("Unable to write datetimes response", e);
      return Response.serverError().build();
    }
  }

  // Create a serialized object as response
  private Response writeObjectResponse(final Object obj) throws IOException {
    StreamingOutput stream = new StreamingOutput() {
      @Override
      public void write(OutputStream os) throws IOException, WebApplicationException {
        ObjectOutputStream objectStream = new ObjectOutputStream(os);
        objectStream.writeObject(obj);
        objectStream.flush();
      }
    };

    return Response.ok(stream).build();
  }

/** OSGi container callback. */
  public void setSyllabusDataService(SyllabusDataService syllabusDataService) {
    this.syllabusDataService = syllabusDataService;
  }
}
