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

package org.opencastproject.ldapinfo.impl.endpoint;

import org.opencastproject.ldapinfo.api.LdapInfoException;
import org.opencastproject.ldapinfo.api.LdapInfoService;

import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * The REST endpoint for the {@link LdapInfoService} service
 */
@Path("/")
@RestService(name = "LdapInfoServiceEndpoint",
    title = "LDAP Information Service Endpoint",
    abstractText = "This is a service endpoint that contacts an LDAP server to provide information about a user",
    notes = {"All paths above are relative to the REST endpoint base (something like http://your.server/ldapinfo)",
        "If the service is not working it will return a status 503, this means that the LDAP server that is providing the information"
                + "does not respond.",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated."
                + "In other words, there is a bug! You should file an error report with your server logs from the time"
                + "when the error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>" })
public class LdapInfoRestEndpoint {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(LdapInfoRestEndpoint.class);

  /** The rest docs */
  protected String docs;

  /** The service */
  protected LdapInfoService ldapInfoService;

  /**
   * Get the spotId for the given user
   * @param username
   *          the username to look up
   * @return The spotId of the user
   */
  @GET
  @Path("users/{username}/spotid")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "ldapInfo", description = "Get the spotId for the given user",
      pathParameters = { @RestParameter(description = "The username that should be queried", isRequired = true, name = "username", type = RestParameter.Type.STRING) },
      reponses = {
        @RestResponse(description = "The request was processed succesfully.", responseCode = HttpServletResponse.SC_OK),
        @RestResponse(description = "LDAP Server not available", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE),
        @RestResponse(description = "User not found", responseCode = HttpServletResponse.SC_NOT_FOUND) },
      returnDescription = "University of Manchester SpotId.")
  public Response getSpotid(@PathParam("username") String username) {
    logger.debug("REST call for ldapInfo user: '{}'", username);
    String spotId;
    try {
      spotId = ldapInfoService.getSpotId(username);
      if ("".equals(spotId)) {
        return Response.status(Status.NOT_FOUND).build();
      }
    } catch (LdapInfoException ex) {
      logger.error("ldapinfo get SpotId threw: {}", ex.getMessage());
      logger.debug("Stacktrace:",ex);
      return Response.status(Status.SERVICE_UNAVAILABLE).build();
    }
    return Response.ok().entity(spotId).build();
  }

  @GET
  @Path("users/{spotid}/directoryInfo.json")
  @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
  @RestQuery(name = "ldapInfo", description = "Get the ldap Directory information for the given spotid",
      pathParameters = { @RestParameter(description = "The spotId that should be queried", isRequired = true, name = "spotid", type = RestParameter.Type.STRING) },
      reponses = {
        @RestResponse(description = "The request was processed succesfully.", responseCode = HttpServletResponse.SC_OK),
        @RestResponse(description = "LDAP server not available", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE),
        @RestResponse(description = "spotId not found", responseCode = HttpServletResponse.SC_NOT_FOUND) },
      returnDescription = "JSON map of directory information for the spotId.")
  public Response getEduPersonEntitlements(@PathParam("spotid") String spotId) {
    Map dirInfo;
    try {
      dirInfo = ldapInfoService.getDirectoryInformation(spotId);
      if (null == dirInfo) {
        return Response.status(Status.NOT_FOUND).build();
      }
    } catch (LdapInfoException ex) {
      logger.error("ldapinfo get SpotId threw: {}", ex.getMessage());
      logger.debug("Stacktrace:",ex);
      return Response.status(Status.SERVICE_UNAVAILABLE).build();
    }
    String json = new Gson().toJson(dirInfo);
    return Response.ok(json).type(MediaType.APPLICATION_JSON).build();
  }

  /**
   * Check the group membership of a given user.
   *
   * @param username
   *          the username to look up
   * @param groupname
   *          the LDAP group to be tested for the user
   * @return Status.OK          if the user is a member of the group
   *         Status.NOT_FOUND   otherwise
   */
  @GET
  @Path("groups/{groupname}")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "ldapInfo", description = "Check the group membership of a given user.",
      pathParameters = { @RestParameter(description = "The group that should be queried", isRequired = true, name = "groupname", type = RestParameter.Type.STRING) },
      restParameters = { @RestParameter(description = "user to find in group", isRequired = true, name = "user", type = RestParameter.Type.STRING) },
      reponses = {
        @RestResponse(description = "User has been found in this group", responseCode = HttpServletResponse.SC_OK),
        @RestResponse(description = "LDAP Server not available", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE),
        @RestResponse(description = "User not found in this group", responseCode = HttpServletResponse.SC_NOT_FOUND) },
      returnDescription = "")
  public Response memberOf(@PathParam("groupname") String groupname, @FormParam("user") String username) {
    logger.debug("REST call for ldapInfo memberOf user: '{}' group: '{}'", username, groupname);
    try {
      if (!ldapInfoService.isMemberOf(username, groupname)) {
        return Response.status(Status.NOT_FOUND).build();
      }
    } catch (LdapInfoException ex) {
      logger.error("ldap info get SpotId threw: {}", ex.getMessage());
      logger.debug("Stacktrace:",ex);
      return Response.status(Status.SERVICE_UNAVAILABLE).build();
    }
    return Response.ok().build();
  }

  public void setLdapInfoService(LdapInfoService service) {
    this.ldapInfoService = service;
  }
}
