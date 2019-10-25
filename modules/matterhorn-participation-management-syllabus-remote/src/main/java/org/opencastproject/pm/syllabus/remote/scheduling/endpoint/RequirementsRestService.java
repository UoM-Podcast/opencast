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
package org.opencastproject.pm.syllabus.remote.scheduling.endpoint;

import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;


import org.opencastproject.pm.syllabus.impl.RemoteObjectUtil;
import org.opencastproject.requirement.api.RequirementService;
import org.opencastproject.requirement.api.RequirementServiceException;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("/")
@RestService(name = "requirements_service", title = "Requirements Service", abstractText = "Provide remote access to requiremnts data", notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is "
                + "not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
                + "other words, there is a bug! You should file an error report with your server logs from the time when the "
                + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>" })
public class RequirementsRestService {

  private static transient Logger logger = LoggerFactory.getLogger(RequirementsRestService.class);

  private final Map<String, RequirementService> requirementServices = new HashMap();

  @GET
  @Path("/providers/{provider}/resources/{resource}/requirements/{requirement}/entities")
  @RestQuery(name = "get_entity_ids", description = "Get all entity ids from the provider have the requirement on the given resource",
          returnDescription = "List<String> serialized object",
          pathParameters = {
            @RestParameter(name = "provider", type = STRING, isRequired = true, description = "Requirements provider name "),
            @RestParameter(name = "resource", type = STRING, isRequired = true, description = "Resource name"),
            @RestParameter(name = "requirement", type = STRING, isRequired = true, description = "Requirement name"),
          },
          reponses = {
            @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Ids that have the resource requirement"),
            @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "The resource and/or are requirement invalid"),
            @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to get the ids"),
            @RestResponse(responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE, description = "The provider can not be found")
          })
  public Response getEntityIds(@PathParam("provider") String provider, @PathParam("resource") String resource, @PathParam("requirement") String requirement) {
    if (!requirementServices.containsKey(provider)) {
      return Response.status(Status.SERVICE_UNAVAILABLE).build();
    }

    RequirementService service = requirementServices.get(provider);

    try {
      RequirementService.Resource resourceEnum = RequirementService.Resource.valueOf(StringUtils.upperCase(resource));
      RequirementService.Requirement requirementEnum = RequirementService.Requirement.valueOf(StringUtils.upperCase(requirement));

      try {
        final List<String> entityIds = service.getIds(resourceEnum, requirementEnum);

        return RemoteObjectUtil.writeObjectResponse(entityIds);
      } catch (RequirementServiceException | IOException ex) {
        return Response.status(Status.SERVICE_UNAVAILABLE).build();
      }
    } catch (IllegalArgumentException ea) {
      return Response.status(Status.BAD_REQUEST).build();
    }
  }

  @GET
  @Path("/providers/{provider}/resources/{resource}/requirements/{requirement}/entities/{id}")
  @RestQuery(name = "check_entity_requirement", description = "Check whether an entity has the specified requirement",
          returnDescription = "no content, see status code",
          pathParameters = {
            @RestParameter(name = "provider", type = STRING, isRequired = true, description = "Requirements provider name "),
            @RestParameter(name = "resource", type = STRING, isRequired = true, description = "Resource name"),
            @RestParameter(name = "requirement", type = STRING, isRequired = true, description = "Requirement name"),
            @RestParameter(name = "id", type = STRING, isRequired = true, description = "Entity id")
          },
          reponses = {
            @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "The entity has the requirement"),
            @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "The resource and/or requirement are invalid"),
            @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "The entity does not have the requirement"),
            @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to get the requirements"),
            @RestResponse(responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE, description = "The provider can not be found")
          })
  public Response checkEntityId(@PathParam("provider") String provider, @PathParam("resource") String resource, @PathParam("requirement") String requirement, @PathParam("id") String id) {
    if (!requirementServices.containsKey(provider)) {
      return Response.status(Status.SERVICE_UNAVAILABLE).build();
    }

    RequirementService service = requirementServices.get(provider);

    try {
      RequirementService.Resource resourceEnum = RequirementService.Resource.valueOf(StringUtils.upperCase(resource));
      RequirementService.Requirement requirementEnum = RequirementService.Requirement.valueOf(StringUtils.upperCase(requirement));

      try {
        if (service.checkId(id, resourceEnum, requirementEnum)) {
          return Response.ok().build();
        } else {
          return Response.status(Status.NOT_FOUND).build();
        }
      } catch (RequirementServiceException ex) {
        return Response.status(Status.SERVICE_UNAVAILABLE).build();
      }
    } catch (IllegalArgumentException ea) {
      return Response.status(Status.BAD_REQUEST).build();
    }
  }

  /* OSGI callback */
  public void addRequirementService(RequirementService service) {
    requirementServices.put(service.getProviderName(), service);
  }
}
