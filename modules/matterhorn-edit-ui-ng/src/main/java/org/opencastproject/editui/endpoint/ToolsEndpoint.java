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

import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.urlsigning.utils.UrlSigningServiceOsgiUtil;
import org.opencastproject.util.RestUtil.R;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Dictionary;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@RestService(name = "toolsService", title = "Tools API Service",
        abstractText = "Provides a location for the tools API.",
        notes = {"This service provides a location for the tools API for the admin UI.",
          "<strong>Important:</strong> "
          + "<em>This service is for exclusive use by the module matterhorn-admin-ui-ng. Its API might change "
          + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
          + "DO NOT use this for integration of third-party applications.<em>"})
public class ToolsEndpoint extends RemoteRestEndpoint implements ManagedService {

  /**
   * The logging facility
   */
  private static final Logger logger = LoggerFactory.getLogger(ToolsEndpoint.class);

  private long expireSeconds = UrlSigningServiceOsgiUtil.DEFAULT_URL_SIGNING_EXPIRE_DURATION;

  private Boolean signWithClientIP = UrlSigningServiceOsgiUtil.DEFAULT_SIGN_WITH_CLIENT_IP;

  private SecurityService securityService;

  /**
   * OSGi DI
   */
  void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Override
  public SecurityService getSecurityService() {
    return securityService;
  }

  /**
   * OSGi callback if properties file is present
   */
  @SuppressWarnings("rawtypes")
  @Override
  public void updated(Dictionary properties) throws ConfigurationException {
    expireSeconds = UrlSigningServiceOsgiUtil.getUpdatedSigningExpiration(properties, this.getClass().getSimpleName());
    signWithClientIP = UrlSigningServiceOsgiUtil.getUpdatedSignWithClientIP(properties,
            this.getClass().getSimpleName());
  }

  @GET
  @Path("{mediapackageid}.json")
  @RestQuery(name = "getAvailableTools", description = "Returns a list of tools which are currently available for the given media package.", returnDescription = "A JSON array with tools identifiers", pathParameters = {
    @RestParameter(name = "mediapackageid", description = "The id of the media package", isRequired = true, type = RestParameter.Type.STRING)}, reponses = {
    @RestResponse(description = "Available tools evaluated", responseCode = HttpServletResponse.SC_OK)})
  public Response getAvailableTools(@PathParam("mediapackageid") final String mediaPackageId) {
    return forwardRequest("/admin-ng/tools/" + mediaPackageId + ".json", "GET", null);
  }

  @GET
  @Path("editor/{mediapackageid}")
  @RestQuery(name = "getEditor", description = "Redirects to the editor for the mediapackage", returnDescription = "editor redirect", pathParameters = {
    @RestParameter(name = "mediapackageid", description = "The id of the media package", isRequired = true, type = RestParameter.Type.STRING)}, reponses = {
    @RestResponse(description = "Redirect to editor", responseCode = HttpServletResponse.SC_SEE_OTHER),
    @RestResponse(description = "Editor not found", responseCode = HttpServletResponse.SC_NOT_FOUND)})
          public Response getEditor(@PathParam("mediapackageid") final String mediaPackageId) {
    try {
      URI uri = new URI("../index.html#/events/events/" + mediaPackageId + "/tools/editor");
      return Response.seeOther(uri).build();
    } catch (URISyntaxException ex) {
      logger.error(null, ex);
    }
    return R.notFound();
  }

  @GET
  @Path("{mediapackageid}/editor.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getVideoEditor", description = "Returns all the information required to get the editor tool started", returnDescription = "JSON object", pathParameters = {
    @RestParameter(name = "mediapackageid", description = "The id of the media package", isRequired = true, type = RestParameter.Type.STRING)}, reponses = {
    @RestResponse(description = "Media package found", responseCode = HttpServletResponse.SC_OK),
    @RestResponse(description = "Media package not found", responseCode = HttpServletResponse.SC_NOT_FOUND)})
  public Response getVideoEditor(@PathParam("mediapackageid") final String mediaPackageId) {
    return forwardRequest("/admin-ng/tools/" + mediaPackageId + "/editor.json", "GET", null);
  }

  @POST
  @Path("{mediapackageid}/editor.json")
  @Consumes(MediaType.APPLICATION_JSON)
  @RestQuery(name = "editVideo", description = "Takes editing information from the client side and processes it", returnDescription = "", pathParameters = {
    @RestParameter(name = "mediapackageid", description = "The id of the media package", isRequired = true, type = RestParameter.Type.STRING)}, reponses = {
    @RestResponse(description = "Editing information saved and processed", responseCode = HttpServletResponse.SC_OK),
    @RestResponse(description = "Media package not found", responseCode = HttpServletResponse.SC_NOT_FOUND),
    @RestResponse(description = "The editing information cannot be parsed", responseCode = HttpServletResponse.SC_BAD_REQUEST)})
  public Response editVideo(@PathParam("mediapackageid") final String mediaPackageId,
          @Context HttpServletRequest request) {
    String details;
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      logger.error("Sleep interrupted: {}", ExceptionUtils.getStackTrace(e));
    }
    try (InputStream is = request.getInputStream()) {
      details = IOUtils.toString(is);
    } catch (IOException e) {
      logger.error("Error reading request body: {}", ExceptionUtils.getStackTrace(e));
      return R.serverError();
    }
    return forwardRequest("/admin-ng/tools/" + mediaPackageId + "/editor.json", "POST", details);
  }
}
