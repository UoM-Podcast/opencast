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
package org.opencastproject.editui.endpoint;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;

import org.opencastproject.rest.RestConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.systems.MatterhornConstants;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.parser.JSONParser;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * The event endpoint acts as a facade for WorkflowService and Archive providing a unified query interface and result
 * set.
 * <p>
 * This first implementation uses the {@link org.opencastproject.archive.opencast.OpencastArchive}. In a later iteration
 * the endpoint may abstract over the concrete archive.
 */
@Path("/")
@RestService(name = "eventservice", title = "Event Service",
        abstractText = "Provides resources and operations related to the events",
        notes = {"This service offers the event CRUD Operations for the admin UI.",
          "<strong>Important:</strong> "
          + "<em>This service is for exclusive use by the module matterhorn-admin-ui-ng. Its API might change "
          + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
          + "DO NOT use this for integration of third-party applications.<em>"})
public abstract class AbstractEventEndpoint extends RemoteRestEndpoint {

  /**
   * The logging facility
   */
  static final Logger logger = LoggerFactory.getLogger(AbstractEventEndpoint.class);

  protected static final String URL_SIGNING_EXPIRES_DURATION_SECONDS_KEY = "url.signing.expires.seconds";

  /**
   * The default time before a piece of signed content expires. 2 Hours.
   */
  protected static final long DEFAULT_URL_SIGNING_EXPIRE_DURATION = 2 * 60 * 60;

  public abstract SecurityService getSecurityService();

  /**
   * Default server URL
   */
  protected String serverUrl = "http://localhost:8080";

  /**
   * Service url
   */
  protected String serviceUrl = null;

  /**
   * A parser for handling JSON documents inside the body of a request. *
   */
  private final JSONParser parser = new JSONParser();

  /**
   * Activates REST service.
   *
   * @param cc ComponentContext
   */
  public void activate(ComponentContext cc) {
    if (cc != null) {
      String ccServerUrl = cc.getBundleContext().getProperty(MatterhornConstants.SERVER_URL_PROPERTY);
      if (StringUtils.isNotBlank(ccServerUrl)) {
        this.serverUrl = ccServerUrl;
      }
      serviceUrl = (String) cc.getProperties().get(RestConstants.SERVICE_PATH_PROPERTY);
    }
  }

  @GET
  @Path("{eventId}/general.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "geteventgeneral", description = "Returns all the data related to the general tab in the event details modal as JSON", returnDescription = "All the data related to the event general tab as JSON", pathParameters = {
    @RestParameter(name = "eventId", description = "The event id (mediapackage id).", isRequired = true, type = RestParameter.Type.STRING)}, reponses = {
    @RestResponse(description = "Returns all the data related to the event general tab as JSON", responseCode = HttpServletResponse.SC_OK),
    @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND)})
  public Response getEventGeneralTab(@PathParam("eventId") String id) {
    return forwardRequest("/admin-ng/event/" + id + "/general.json", "GET", null);
  }

  @GET
  @Path("{eventId}/comments")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "geteventcomments", description = "Returns all the data related to the comments tab in the event details modal as JSON", returnDescription = "All the data related to the event comments tab as JSON", pathParameters = {
    @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING)}, reponses = {
    @RestResponse(description = "Returns all the data related to the event comments tab as JSON", responseCode = HttpServletResponse.SC_OK),
    @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND)})
  public Response getEventComments(@PathParam("eventId") String eventId) {
    return forwardRequest("/admin-ng/event/" + eventId + "/comments", "GET", null);
  }

  @GET
  @Path("{eventId}/participation.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "geteventparticipationinformation", description = "Get the particition information of a event", returnDescription = "The participation information", pathParameters = {
    @RestParameter(name = "eventId", isRequired = true, description = "The event identifier", type = RestParameter.Type.STRING)}, reponses = {
    @RestResponse(responseCode = SC_BAD_REQUEST, description = "The required form params were missing in the request."),
    @RestResponse(responseCode = SC_NOT_FOUND, description = "If the event has not been found."),
    @RestResponse(responseCode = SC_OK, description = "The access information ")})
  public Response getEventParticipation(@PathParam("eventId") String eventId) {
    return forwardRequest("/admin-ng/event/" + eventId + "/participation.json", "GET", null);
  }

  @GET
  @Path("{eventId}/metadata.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "geteventmetadata", description = "Returns all the data related to the metadata tab in the event details modal as JSON", returnDescription = "All the data related to the event metadata tab as JSON", pathParameters = {
    @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING)}, reponses = {
    @RestResponse(description = "Returns all the data related to the event metadata tab as JSON", responseCode = HttpServletResponse.SC_OK),
    @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND)})
  public Response getEventMetadata(@PathParam("eventId") String eventId) {
    return forwardRequest("/admin-ng/event/" + eventId + "/metadata.json", "GET", null);
  }

  @PUT
  @Path("{eventId}/metadata")
  @RestQuery(name = "updateeventmetadata", description = "Update the passed metadata for the event with the given Id", pathParameters = {
    @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING)}, restParameters = {
    @RestParameter(name = "metadata", isRequired = true, type = RestParameter.Type.TEXT, description = "The list of metadata to update")}, reponses = {
    @RestResponse(description = "The metadata have been updated.", responseCode = HttpServletResponse.SC_OK),
    @RestResponse(description = "Could not parse metadata.", responseCode = HttpServletResponse.SC_BAD_REQUEST),
    @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND)}, returnDescription = "No content is returned.")
  public Response updateEventMetadata(@PathParam("eventId") String id, @FormParam("metadata") String metadataJSON) {
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("metadata", metadataJSON));
    return forwardRequest("/admin-ng/event/" + id + "/metadata", "PUT", null, params);
  }

  @GET
  @Path("{eventId}/asset/assets.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getAssetList", description = "Returns the number of assets from each types as JSON", returnDescription = "The number of assets from each types as JSON", pathParameters = {
    @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING)}, reponses = {
    @RestResponse(description = "Returns the number of assets from each types as JSON", responseCode = HttpServletResponse.SC_OK),
    @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND)})
  public Response getAssetList(@PathParam("eventId") String id) {
    return forwardRequest("/admin-ng/event/" + id + "/asset/assets.json", "GET", null);
  }

  @GET
  @Path("{eventId}/workflows.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "geteventworkflows", description = "Returns all the data related to the workflows tab in the event details modal as JSON", returnDescription = "All the data related to the event workflows tab as JSON", pathParameters = {
    @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = RestParameter.Type.STRING)}, reponses = {
    @RestResponse(description = "Returns all the data related to the event workflows tab as JSON", responseCode = HttpServletResponse.SC_OK),
    @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND)})
  public Response getEventWorkflows(@PathParam("eventId") String id) {
    return forwardRequest("/admin-ng/event/" + id + "/workflows.json", "GET", null);
  }

  @GET
  @Path("{eventId}/access.json")
  @SuppressWarnings("unchecked")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getEventAccessInformation", description = "Get the access information of an event", returnDescription = "The access information", pathParameters = {
    @RestParameter(name = "eventId", isRequired = true, description = "The event identifier", type = RestParameter.Type.STRING)}, reponses = {
    @RestResponse(responseCode = SC_BAD_REQUEST, description = "The required form params were missing in the request."),
    @RestResponse(responseCode = SC_NOT_FOUND, description = "If the event has not been found."),
    @RestResponse(responseCode = SC_OK, description = "The access information ")})
  public Response getEventAccessInformation(@PathParam("eventId") String eventId) {
    return forwardRequest("/admin-ng/event/" + eventId + "/access.json", "GET", null);
  }

}
