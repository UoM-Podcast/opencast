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

package org.opencastproject.archive.opencast.endpoint;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.opencastproject.util.RestUtil.R.badRequest;
import static org.opencastproject.util.RestUtil.R.noContent;
import static org.opencastproject.util.RestUtil.R.notFound;
import static org.opencastproject.util.RestUtil.R.ok;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.archive.api.Query;
import org.opencastproject.archive.api.Version;
import org.opencastproject.archive.base.endpoint.ArchiveRestEndpointBase;
import org.opencastproject.archive.opencast.JaxbResultSet;
import org.opencastproject.archive.opencast.OpencastArchive;
import org.opencastproject.archive.opencast.OpencastArchiveJobProducer;
import org.opencastproject.archive.opencast.OpencastQuery;
import org.opencastproject.archive.opencast.OpencastResultSet;
import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.RestUtil;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.functions.Strings;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Opencast specific REST endpoint of the archive.
 * <p>
 * No @Path annotation here since this class cannot be created by JAX-RS. Put it on implementations.
 */
public abstract class AbstractOpencastArchiveRestEndpoint extends ArchiveRestEndpointBase<OpencastResultSet> {
  public static final String SDF_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
  private static final Logger logger = LoggerFactory.getLogger(AbstractOpencastArchiveRestEndpoint.class);

  /** The constant used to switch the direction of the ordering query string parameter. */
  public static final String DESCENDING_SUFFIX = "_DESC";

  @Override public abstract OpencastArchive getArchive();
  public abstract OpencastArchiveJobProducer getJobProducer();

  @Override public Object convert(final OpencastResultSet source) {
    return JaxbResultSet.create(source);
  }

  @GET
  @Path("episode.{format:xml|json}")
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  @RestQuery(name = "episodes",
             description = "Search for episodes matching the query parameters.",
             pathParameters = {
                     @RestParameter(description = "The output format (json or xml) of the response body.", isRequired = true, name = "format", type = RestParameter.Type.STRING)
             },
             restParameters = {
                     @RestParameter(name = "id", type = RestParameter.Type.STRING, description = "The ID of the single episode to be returned, if it exists.", isRequired = false),
                     @RestParameter(name = "q", description = "Any episode that matches this free-text query.", isRequired = false, type = RestParameter.Type.STRING),
                     @RestParameter(name = "creator", isRequired = false, description = "Filter results by the mediapackage's creator", type = STRING),
                     @RestParameter(name = "contributor", isRequired = false, description = "Filter results by the mediapackage's contributor", type = STRING),
                     @RestParameter(name = "language", isRequired = false, description = "Filter results by mediapackage's language.", type = STRING),
                     @RestParameter(name = "series", isRequired = false, description = "Filter results by mediapackage's series identifier.", type = STRING),
                     @RestParameter(name = "license", isRequired = false, description = "Filter results by mediapackage's license.", type = STRING),
                     @RestParameter(name = "title", isRequired = false, description = "Filter results by mediapackage's title.", type = STRING),
                     @RestParameter(name = "episodes", type = RestParameter.Type.STRING, defaultValue = "false", description = "Whether to include this series episodes. This can be used in combination with \"id\" or \"q\".", isRequired = false),
                     @RestParameter(name = "limit", type = RestParameter.Type.STRING, defaultValue = "0", description = "The maximum number of items to return per page.", isRequired = false),
                     @RestParameter(name = "offset", type = RestParameter.Type.STRING, defaultValue = "0", description = "The page number.", isRequired = false),
                     @RestParameter(name = "admin", type = RestParameter.Type.STRING, defaultValue = "false", description = "Whether this is an administrative query", isRequired = false),
                     @RestParameter(name = "sort", type = RestParameter.Type.STRING, description = "The sort order.  May include any "
                             + "of the following: DATE_CREATED, TITLE, SERIES_TITLE, SERIES_ID, MEDIA_PACKAGE_ID, WORKFLOW_DEFINITION_ID, CREATOR, "
                             + "CONTRIBUTOR, LANGUAGE, LICENSE, SUBJECT.  Add '_DESC' to reverse the sort order (e.g. TITLE_DESC).", isRequired = false),
                     @RestParameter(name = "onlyLatest", type = Type.BOOLEAN, defaultValue = "false", description = "Filter results by only latest version of the archive", isRequired = false)
             },
             reponses = {
                     @RestResponse(description = "The request was processed succesfully.", responseCode = HttpServletResponse.SC_OK)
             },
             returnDescription = "The search results, expressed as xml or json.")
  // CHECKSTYLE:OFF -- more than 7 parameters
  public Response findEpisode(@QueryParam("id") final String id,
                              @QueryParam("q") final String text,
                              @QueryParam("creator") final String creator,
                              @QueryParam("contributor") final String contributor,
                              @QueryParam("language") final String language,
                              @QueryParam("series") final String series,
                              @QueryParam("license") final String license,
                              @QueryParam("title") final String title,
                              @QueryParam("limit") final Integer limit,
                              @QueryParam("offset") final Integer offset,
                              @QueryParam("sort") final String order,
                              @QueryParam("onlyLatest") @DefaultValue("true") final boolean onlyLatest,
                              @PathParam("format") final String format) {
    // CHECKSTYLE:ON
    return handleException(new Function0<Response>() {
      @Override public Response apply() {
        final Query q = new OpencastQuery() {
          @Override public Option<String> getDcTitle() { return option(title).bind(Strings.trimToNone); }

          @Override public Option<String> getDcCreator() { return option(creator).bind(Strings.trimToNone); }

          @Override public Option<String> getDcContributor() { return option(contributor).bind(Strings.trimToNone); }

          @Override public Option<String> getDcLanguage() { return option(language).bind(Strings.trimToNone); }

          @Override public Option<String> getDcLicense() { return option(license).bind(Strings.trimToNone); }

          @Override public Option<String> getSeriesTitle() { return option(title).bind(Strings.trimToNone); }

          @Override public Option<String> getFullText() { return option(text).bind(Strings.trimToNone); }

          @Override public Option<String> getMediaPackageId() { return option(id).bind(Strings.trimToNone); }

          @Override public Option<String> getSeriesId() { return option(series).bind(Strings.trimToNone); }

          @Override public Option<String> getOrganizationId() { return none(); }

          @Override public boolean isOnlyLastVersion() { return onlyLatest; }

          @Override public Option<Integer> getLimit() { return option(limit); }

          @Override public Option<Integer> getOffset() { return option(offset); }

          @Override public Option<Date> getArchivedAfter() { return none();}

          @Override public Option<Date> getArchivedBefore() { return none(); }

          @Override public boolean isIncludeDeleted() { return false; }

          @Override public Option<Date> getDeletedAfter() { return none(); }

          @Override public Option<Date> getDeletedBefore() { return none(); }

          @Override public Option<Order> getOrder() {
            return option(order).bind(new Function<String, Option<Order>>() {
              @Override public Option<Order> apply(String o) {
                final String stripped = o.endsWith(DESCENDING_SUFFIX)
                        ? o.substring(0, o.length() - DESCENDING_SUFFIX.length())
                        : o;
                try {
                  return some(Order.valueOf(stripped));
                } catch (IllegalArgumentException e) {
                  logger.warn("Unknown order " + order);
                  return none();
                }
              }
            });
          }

          @Override public boolean isOrderAscending() {
            return option(order).map(new Function<String, Boolean>() {
              @Override public Boolean apply(String o) {
                return !o.endsWith(DESCENDING_SUFFIX);
              }
            }).getOrElse(false);

          }
        };
        // Return the results using the requested format
        final OpencastResultSet sr = getArchive().find(q, getUriRewriter());
        return Response.ok(convert(sr))
                .type(RestUtil.getResponseFormat(format))
                .build();
      }
    });
  }

  @DELETE
  @Path("delete/{id}")
  @RestQuery(name = "remove",
             description = "Remove an episode from the archive.",
             pathParameters = {@RestParameter(name = "id", isRequired = true,
                                              type = RestParameter.Type.STRING, description = "The media package ID to remove from the archive.")},
             reponses = {@RestResponse(description = "The mediapackage was removed, no content to return.", responseCode = HttpServletResponse.SC_NO_CONTENT),
                     @RestResponse(description = "There has been an internal error and the mediapackage could not be deleted", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR)},
             returnDescription = "No content is returned.")
  public Response delete(@PathParam("id") final String mediaPackageId) {
    return handleException(new Function0.X<Response>() {
      @Override public Response xapply() throws NotFoundException {
        if (mediaPackageId != null && getArchive().delete(mediaPackageId))
          return noContent();
        else
          return notFound();
      }
    });
  }

  @POST
  @Path("moveById")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "moveById", description = "Move a mediapackage based on its ID.",
          restParameters = {
                  @RestParameter(
                          name = "id",
                          isRequired = true,
                          type = RestParameter.Type.STRING,
                          defaultValue = "",
                          description = "The mediapackage ID to move."),
                  @RestParameter(
                          name = "target",
                          isRequired = true,
                          type = RestParameter.Type.STRING,
                          defaultValue = "",
                          description = "The target storage to move the mediapackage to.")},
          reponses = {
                  @RestResponse(
                          description = "The job created to move the snapshot.",
                          responseCode = SC_OK),
                  @RestResponse(
                          description = "Invalid parameters, and the job was not created",
                          responseCode = SC_BAD_REQUEST),
                  @RestResponse(
                          description = "There has been an internal error, and the job was not created",
                          responseCode = SC_INTERNAL_SERVER_ERROR)},
          returnDescription = "The Job created")
  public Response moveById(@FormParam("id") final String id, @FormParam("target") final String target) {
    final String mpid = StringUtils.trimToNull(id);
    if (null == mpid) {
      return badRequest("Invalid mediapackage ID: " + mpid);
    }

    final String trimmedTarget = StringUtils.trimToNull(target);
    if (null == trimmedTarget) {
      return badRequest("Invalid target store ID: " + trimmedTarget);
    } else if (!getJobProducer().datastoreExists(trimmedTarget)) {
      return badRequest("Target store " + trimmedTarget + " not found");
    }

    return handleException(new Function0<Response>() {
      @Override public Response apply() {
        Job j = getJobProducer().moveById(mpid, trimmedTarget);
        return Response.ok(new JaxbJob(j)).build();
      }
    });
  }

  @POST
  @Path("moveByIdAndVersion")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "moveByIdAndVersion", description = "Move a mediapackage based on its ID and version.",
          restParameters = {
                  @RestParameter(
                          name = "id",
                          isRequired = true,
                          type = RestParameter.Type.STRING,
                          defaultValue = "",
                          description = "The mediapackage ID to move."),
                  @RestParameter(
                          name = "version",
                          isRequired = true,
                          type = RestParameter.Type.STRING,
                          defaultValue = "",
                          description = "The version to move."),
                  @RestParameter(
                          name = "target",
                          isRequired = true,
                          type = RestParameter.Type.STRING,
                          defaultValue = "",
                          description = "The target storage to move the mediapackage to.")},
          reponses = {
                  @RestResponse(
                          description = "The job created to move the snapshot.",
                          responseCode = SC_OK),
                  @RestResponse(
                          description = "Invalid parameters, and the job was not created",
                          responseCode = SC_BAD_REQUEST),
                  @RestResponse(
                          description = "There has been an internal error, and the job was not created",
                          responseCode = SC_INTERNAL_SERVER_ERROR)},
          returnDescription = "The Job created")
  public Response moveByIdAndVersion(@FormParam("id") final String id, @FormParam("version") final String version, @FormParam("target") final String target) {
    final String mpid = StringUtils.trimToNull(id);
    if (null == mpid) {
      return badRequest("Invalid mediapackage ID: " + mpid);
    }

    final Version v;
    try {
      Long versionNumber = Long.parseLong(version);
      v = Version.version(versionNumber);
    } catch (NumberFormatException e) {
      return badRequest("Invalid version number format");
    }

    final String trimmedTarget = StringUtils.trimToNull(target);
    if (null == trimmedTarget) {
      return badRequest("Invalid target store ID: " + trimmedTarget);
    } else if (!getJobProducer().datastoreExists(trimmedTarget)) {
      return badRequest("Target store " + trimmedTarget + " not found");
    }

    try {
      Job j = getJobProducer().moveByIdAndVersion(v, mpid, trimmedTarget);
      return ok(new JaxbJob(j));
    } catch (Exception e) {
      logger.error("Problem handling moveByIdAndVersion", e);
      return Response.serverError().build();

    }
  }

  @POST
  @Path("moveByDate")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "moveByDate", description = "Move all snapshots between two dates.",
          restParameters = {
                  @RestParameter(
                          name = "start",
                          isRequired = true,
                          type = RestParameter.Type.STRING,
                          defaultValue = "",
                          description = "The start date, in the format yyyy-MM-dd'T'HH:mm:ss'Z'."),
                  @RestParameter(
                          name = "end",
                          isRequired = true,
                          type = RestParameter.Type.STRING,
                          defaultValue = "",
                          description = "The end date, in the format yyyy-MM-dd'T'HH:mm:ss'Z'."),
                  @RestParameter(
                          name = "target",
                          isRequired = true,
                          type = RestParameter.Type.STRING,
                          defaultValue = "",
                          description = "The target storage to move the mediapackage to.")},
          reponses = {
                  @RestResponse(
                          description = "The job created to move the snapshots.",
                          responseCode = SC_OK),
                  @RestResponse(
                          description = "Invalid parameters, and the job was not created",
                          responseCode = SC_BAD_REQUEST),
                  @RestResponse(
                          description = "There has been an internal error, and the job was not created",
                          responseCode = SC_INTERNAL_SERVER_ERROR)},
          returnDescription = "The Job created")
  public Response moveByDate(@FormParam("start") final String startString, @FormParam("end") final String endString, @FormParam("target") final String target) {
    DateFormat sdf = new SimpleDateFormat(SDF_FORMAT);
    Date start;
    Date end;
    try {
      start = sdf.parse(startString);
      end = sdf.parse(endString);
    } catch (ParseException e) {
      return badRequest("Invalid start or end date format");
    }

    if (end.before(start)) {
      return badRequest("Start date " + start + " must be before end date " + end);
    }

    final String trimmedTarget = StringUtils.trimToNull(target);
    if (null == trimmedTarget) {
      return badRequest("Invalid target store ID: " + trimmedTarget);
    } else if (!getJobProducer().datastoreExists(trimmedTarget)) {
      return badRequest("Target store " + trimmedTarget + " not found");
    }

    try {
      Job j = getJobProducer().moveByDate(start, end, trimmedTarget);
      return ok(new JaxbJob(j));
    } catch (Exception e) {
      logger.error("Problem handling moveByDate", e);
      return Response.serverError().build();

    }
  }

  @POST
  @Path("moveByIdAndDate")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "moveByIdAndDate", description = "Move all snapshots for a given mediapackage taken between two dates.",
          restParameters = {
                  @RestParameter(
                          name = "id",
                          isRequired = true,
                          type = RestParameter.Type.STRING,
                          defaultValue = "",
                          description = "The mediapackage ID to move."),
                  @RestParameter(
                          name = "start",
                          isRequired = true,
                          type = RestParameter.Type.STRING,
                          defaultValue = "",
                          description = "The start date, in the format yyyy-MM-dd'T'HH:mm:ss'Z'."),
                  @RestParameter(
                          name = "end",
                          isRequired = true,
                          type = RestParameter.Type.STRING,
                          defaultValue = "",
                          description = "The end date, in the format yyyy-MM-dd'T'HH:mm:ss'Z'."),
                  @RestParameter(
                          name = "target",
                          isRequired = true,
                          type = RestParameter.Type.STRING,
                          defaultValue = "",
                          description = "The target storage to move the mediapackage to.")},
          reponses = {
                  @RestResponse(
                          description = "The job created to move the snapshots.",
                          responseCode = SC_OK),
                  @RestResponse(
                          description = "Invalid parameters, and the job was not created",
                          responseCode = SC_BAD_REQUEST),
                  @RestResponse(
                          description = "There has been an internal error, and the job was not created",
                          responseCode = SC_INTERNAL_SERVER_ERROR)},
          returnDescription = "The Job created")
  public Response moveByIdAndDate(@FormParam("id") final String id, @FormParam("start") final String startString, @FormParam("end") final String endString, @FormParam("target") final String target) {
    DateFormat sdf = new SimpleDateFormat(SDF_FORMAT);
    final String mpid = StringUtils.trimToNull(id);
    if (null == mpid) {
      return badRequest("Invalid mediapackage ID: " + mpid);
    }

    Date start;
    Date end;
    try {
      start = sdf.parse(startString);
      end = sdf.parse(endString);
    } catch (ParseException e) {
      return badRequest("Invalid start or end date format");
    }

    if (end.before(start)) {
      return badRequest("Start date " + start + " must be before end date " + end);
    }

    final String trimmedTarget = StringUtils.trimToNull(target);
    if (null == trimmedTarget) {
      return badRequest("Invalid target store ID: " + trimmedTarget);
    } else if (!getJobProducer().datastoreExists(trimmedTarget)) {
      return badRequest("Target store " + trimmedTarget + " not found");
    }

    try {
      Job j = getJobProducer().moveByIdAndDate(mpid, start, end, trimmedTarget);
      return ok(new JaxbJob(j));
    } catch (Exception e) {
      logger.error("Problem handling moveByIdAndDate", e);
      return Response.serverError().build();

    }
  }
}
