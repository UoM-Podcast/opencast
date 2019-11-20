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

package org.opencastproject.enrolments.endpoint;

import org.opencastproject.pm.syllabus.impl.RemoteObjectUtil;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.google.gson.Gson;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import uk.ac.manchester.integration.enrolments.Enrolments;
import uk.ac.manchester.its.media.enrolments.ClassEnrolmentsException;
import uk.ac.manchester.its.media.enrolments.ClassEnrolmentsService;

/**
 * The REST endpoint for the {@link ClassEnrolmentsService} service
 */
@Path("/")
@RestService(name = "EnrolmentsRest",
    title = "Enrolments Endpoint",
    abstractText = "This is a service endpoint that contacts server to provide information about a user's enrolments",
    notes = {"All paths above are relative to the REST endpoint base (something like http://your.server/enrolments)",
        "If the service is not working it will return a status 503, this means that the webmethod that is providing the information"
                + "does not respond.",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated."
                + "In other words, there is a bug! You should file an error report with your server logs from the time"
                + "when the error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>" })
public class EnrolmentsRest {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(EnrolmentsRest.class);

  /** The service */
  protected ClassEnrolmentsService classEnrolmentsService;

  @GET
  @Path("/classes")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "class enrolments", description = "Get the class enrolments for the user",
      restParameters = {
        @RestParameter(description = "User's unique identifier", isRequired = true, name = "spotid", type = RestParameter.Type.STRING),
        @RestParameter(description = "Format default serialized Java List<String>, options json", isRequired = false, name = "format", type = RestParameter.Type.STRING)
      },reponses = {
        @RestResponse(description = "The request was processed succesfully.", responseCode = HttpServletResponse.SC_OK),
        @RestResponse(description = "Enrolments Server not available", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE),
        @RestResponse(description = "User not found", responseCode = HttpServletResponse.SC_NOT_FOUND) },
      returnDescription = "User's class enrolments")
  public Response getClassEnrolments(@QueryParam("spotid") String spotId,
          @QueryParam("format") String format) {
    if (classEnrolmentsService == null) {
      return Response.status(Status.SERVICE_UNAVAILABLE).build();
    }

    try {
      List<Enrolments> enrolments = classEnrolmentsService.getClassEnrolments(spotId);

      if (format != null && StringUtils.equalsIgnoreCase(format, "json")) {
        String json = new Gson().toJson(enrolments);
        return Response.ok(json).build();
      } else {
        try {
          return RemoteObjectUtil.writeObjectResponse(enrolments);
        } catch (IOException e) {
          logger.error("Could not write class enrolments reponse", e);
          return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
      }
    } catch (ClassEnrolmentsException ex) {
      return Response.status(Status.SERVICE_UNAVAILABLE).build();
    }
  }

  /* OSGI callback */
  public void setClassEnrolmentsService(ClassEnrolmentsService service) {
    classEnrolmentsService = service;
  }
}

