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
package org.opencastproject.terminationstate.endpoint.aws;

import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;

import org.opencastproject.terminationstate.api.TerminationStateService;
import org.opencastproject.terminationstate.endpoint.api.TerminationStateRestService;
import org.opencastproject.util.Log;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.json.simple.JSONObject;
import org.slf4j.LoggerFactory;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@RestService(name = "terminationstateservice", title = "Termination State Service: AWS Auto Scalomg",
        abstractText = "This service responds to notifications that the underlying server may be terminating."
                + " It stops the node accepting further jobs and can inform when those jobs have completed are"
                + " the node is ready to be terminated.",
        notes  = {"It does not actually shut down the node."})
public class AutoScalingTerminationStateRestService implements TerminationStateRestService {
  private static final Log logger = new Log(LoggerFactory.getLogger(AutoScalingTerminationStateRestService.class));

  private TerminationStateService service;

  @Override
  @GET
  @Path("/state")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "stateasjson", description = "Returns the Termination State as JSON", returnDescription = "A JSON representation of the termination state.",
          reponses = {
            @RestResponse(responseCode = SC_OK, description = "A JSON representation of the termination state."),
            @RestResponse(responseCode = SC_SERVICE_UNAVAILABLE, description = "The AWS Autoscaling Termination State Service is disabled or unavailable")
          })
  public Response getState() {
    if (service != null) {
      JSONObject json  = new JSONObject();
      String state = service.getState().toString();
      json.put("state", state);
      return Response.ok(json.toJSONString()).build();
    } else {
      logger.error("TerminationStateSerice is not available");
      return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
    }
  }

  @Override
  @DELETE
  @Path("/state")
  @RestQuery(name = "terminate", description = "Instruct the node to prepare for termination", returnDescription = "Whether the termination state was set successfully",
          reponses = {
            @RestResponse(responseCode = SC_NO_CONTENT, description = "The node is preparing to terminate"),
            @RestResponse(responseCode = SC_SERVICE_UNAVAILABLE, description = "The AWS Autoscaling Termination State Service is disabled or unavailable"),
          })
  public Response terminate() {
    if (service != null) {
      service.setState(TerminationStateService.TerminationState.WAIT);

      // check is state has changed (ie service is working)
      if (service.getState() != TerminationStateService.TerminationState.NONE) {
        return Response.noContent().build();
      }
    }

    logger.error("AWS Autoscaling Termination State Serice is not available");
    return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
  }

  public void setService(TerminationStateService service) {
    this.service = service;
  }
}
