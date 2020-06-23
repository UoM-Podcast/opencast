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
package org.opencastproject.archive.aws.s3.endpoint;

import static org.opencastproject.util.RestUtil.R.noContent;
import static org.opencastproject.util.RestUtil.R.notFound;
import static org.opencastproject.util.RestUtil.R.ok;
import static org.opencastproject.util.RestUtil.R.serverError;

import org.opencastproject.archive.api.Archive;
import org.opencastproject.archive.api.ArchiveException;
import org.opencastproject.archive.api.Query;
import org.opencastproject.archive.api.ResultItem;
import org.opencastproject.archive.api.ResultSet;
import org.opencastproject.archive.api.UriRewriter;
import org.opencastproject.archive.api.Version;
import org.opencastproject.archive.aws.s3.AwsS3AssetStore;
import org.opencastproject.archive.base.QueryBuilder;
import org.opencastproject.archive.base.StoragePath;
import org.opencastproject.archive.base.storage.ElementStoreException;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@RestService(name = "archive-aws-s3", title = "AWS S3 Archive",
        notes = {
          "All paths are relative to the REST endpoint base (something like http://your.server/files)",
          "If you notice that this service is not working as expected, there might be a bug! "
          + "You should file an error report with your server logs from the time when the error occurred: "
          + "<a href=\"http://opencast.jira.com\">Opencast Issue Tracker</a>"
        },
        abstractText = "This service handles AWS S3 archived assets")
public class AwsS3RestEndpoint {

  private static final Logger logger = LoggerFactory.getLogger(AwsS3RestEndpoint.class);

  private AwsS3AssetStore awsS3AssetStore = null;
  private Archive archive = null;
  private SecurityService securityService = null;

  @GET
  @Path("{mediaPackageId}/assets/storageClass")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "getStorageClass",
          description = "Get the S3 Storage Class for each asset in the Media Package",
          pathParameters = {
            @RestParameter(
                    name = "mediaPackageId", isRequired = true,
                    type = RestParameter.Type.STRING,
                    description = "The media package indentifier.")},
          reponses = {
            @RestResponse(
                    description = "mediapackage found in S3",
                    responseCode = HttpServletResponse.SC_OK),
            @RestResponse(
                    description = "mediapackage not found or has no assets in S3",
                    responseCode = HttpServletResponse.SC_NOT_FOUND)
          },
          returnDescription = "List each asset's Object Key and S3 Storage Class ")
  public Response getStorageClass(@PathParam("mediaPackageId") final String mediaPackageId) {
    return handleException(new Function0<Response>() {
      private String getMediaPackageId() {
        return StringUtils.trimToNull(mediaPackageId);
      }
      @Override public Response apply() {
        final Query idQuery = QueryBuilder.query()
                .currentOrganization(securityService)
                .mediaPackageId(getMediaPackageId())
                .onlyLastVersion(true);
        final ResultSet result = archive.find(idQuery, uriRewriter);
        if (result.size() > 1)
          return serverError();
        if (result.size() == 0)
          return notFound();
        final ResultItem item = result.getItems().get(0);

        StringBuilder info = new StringBuilder();
        for (MediaPackageElement e : item.getMediaPackage().elements()) {
          if (e.getElementType() == MediaPackageElement.Type.Publication) {
            continue;
          }

          StoragePath storagePath = new StoragePath(securityService.getOrganization().getId(), getMediaPackageId(), item.getVersion(), e.getIdentifier());
          if (awsS3AssetStore.contains(storagePath)) {
            try {
              info.append(String.format("%s,%s\n", awsS3AssetStore.getAssetObjectKey(storagePath), awsS3AssetStore.getAssetStorageClass(storagePath)));
            } catch (ElementStoreException ex) {
              throw new ArchiveException(ex);
            }
          } else {
            info.append(String.format("%s,NONE\n", e.getURI()));
          }
        }
        return ok(info.toString());
      }
    });
  }

  @PUT
  @Path("{mediaPackageId}/assets")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "modifyStorageClass",
          description = "Move the Media Package assets to the specified S3 Storage Class if possible",
          pathParameters = {
            @RestParameter(
                    name = "mediaPackageId",
                    isRequired = true,
                    type = RestParameter.Type.STRING,
                    description = "The media package indentifier.")
          },
          restParameters = {
            @RestParameter(
                    name = "storageClass",
                    isRequired = true,
                    type = RestParameter.Type.STRING,
                    description = "The S3 storage class, valid terms STANDARD, STANDARD_IA, INTELLIGENT_TIERING, ONEZONE_IA, REDUCED_REDUNDANCY and GLACIER. See https://aws.amazon.com/s3/storage-classes/")
          },
          reponses = {
            @RestResponse(
                    description = "mediapackage found in S3",
                    responseCode = HttpServletResponse.SC_OK),
            @RestResponse(
                    description = "mediapackage not found or has no assets in S3",
                    responseCode = HttpServletResponse.SC_NOT_FOUND)
          },
          returnDescription = "List each asset's Object Key and new S3 Storage Class")
  public Response modifyStorageClass(@PathParam("mediaPackageId") final String mediaPackageId, @FormParam("storageClass") final String storageClass) {
    return handleException(new Function0<Response>() {
      private String getMediaPackageId() {
        return StringUtils.trimToNull(mediaPackageId);
      }

      private String getStorageClass() {
        return StringUtils.trimToNull(storageClass);
      }

      @Override public Response apply() {
        final Query idQuery = QueryBuilder.query()
                .currentOrganization(securityService)
                .mediaPackageId(getMediaPackageId())
                .onlyLastVersion(true);
        final ResultSet result = archive.find(idQuery, uriRewriter);
        if (result.size() > 1)
          return serverError();
        if (result.size() == 0)
          return notFound();
        final ResultItem item = result.getItems().get(0);

        StringBuilder info = new StringBuilder();
        for (MediaPackageElement e : item.getMediaPackage().elements()) {
          if (e.getElementType() == MediaPackageElement.Type.Publication) {
            continue;
          }

          StoragePath storagePath = new StoragePath(securityService.getOrganization().getId(), getMediaPackageId(), item.getVersion(), e.getIdentifier());
          if (awsS3AssetStore.contains(storagePath)) {
            try {
              info.append(String.format("%s,%s\n", awsS3AssetStore.getAssetObjectKey(storagePath), awsS3AssetStore.modifyAssetStorageClass(storagePath, getStorageClass())));
            } catch (ElementStoreException ex) {
              throw new ArchiveException(ex);
            }
          } else {
            info.append(String.format("%s,NONE\n", e.getURI()));
          }
        }
        return ok(info.toString());
      }

    });
  }

  @GET
  @Path("glacier/{mediaPackageId}/assets")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "restoreAssetsStatus",
          description = "Get the mediapackage asset's restored status",
          pathParameters = {
            @RestParameter(
                    name = "mediaPackageId",
                    isRequired = true,
                    type = RestParameter.Type.STRING,
                    description = "The media package indentifier.")
          },
          reponses = {
            @RestResponse(
                    description = "mediapackage found in S3 and assets in Glacier",
                    responseCode = HttpServletResponse.SC_OK),
            @RestResponse(
                    description = "mediapackage found in S3 but no assets in Glacier",
                    responseCode = HttpServletResponse.SC_NO_CONTENT),
            @RestResponse(
                    description = "mediapackage not found or has no assets in S3",
                    responseCode = HttpServletResponse.SC_NOT_FOUND)
          },
          returnDescription = "List each glacier asset's restoration status and expiration date")
  public Response restoreAssetsStatus(@PathParam("mediaPackageId") final String mediaPackageId) {
    return handleException(new Function0<Response>() {
      private String getMediaPackageId() {
        return StringUtils.trimToNull(mediaPackageId);
      }

      @Override public Response apply() {
        final Query idQuery = QueryBuilder.query()
                .currentOrganization(securityService)
                .mediaPackageId(getMediaPackageId())
                .onlyLastVersion(true);
        final ResultSet result = archive.find(idQuery, uriRewriter);
        if (result.size() > 1)
          return serverError();
        if (result.size() == 0)
          return notFound();
        final ResultItem item = result.getItems().get(0);
          StringBuilder info = new StringBuilder();

        for (MediaPackageElement e : item.getMediaPackage().elements()) {
          if (e.getElementType() == MediaPackageElement.Type.Publication) {
            continue;
          }

          StoragePath storagePath = new StoragePath(securityService.getOrganization().getId(), getMediaPackageId(), item.getVersion(), e.getIdentifier());
          String assetStorageClass = awsS3AssetStore.getAssetStorageClass(storagePath);
          if (awsS3AssetStore.contains(storagePath)
                  && ("GLACIER".equals(assetStorageClass) || "DEEP_ARCHIVE".equals(assetStorageClass))) {
            try {
              info.append(String.format("%s,%s\n", awsS3AssetStore.getAssetObjectKey(storagePath), awsS3AssetStore.getAssetRestoreStatusString(storagePath)));
            } catch (ElementStoreException ex) {
              throw new ArchiveException(ex);
            }
          }
        }
        if (info.length() == 0) {
          return noContent();
        }
        return ok(info.toString());
      }
    });
  }

  @PUT
  @Path("glacier/{mediaPackageId}/assets")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "restoreAssets",
          description = "Initiate the restore of any assets in Glacier storage class",
          pathParameters = {
            @RestParameter(
                    name = "mediaPackageId",
                    isRequired = true,
                    type = RestParameter.Type.STRING,
                    description = "The media package indentifier.")
          },
          restParameters = {
            @RestParameter(
                    name = "restorePeriod",
                    isRequired = false,
                    type = RestParameter.Type.INTEGER,
                    defaultValue = "2",
                    description = "Number of days to restore the assets for, default see service configuration")
          },
          reponses = {
            @RestResponse(
                    description = "restore of assets started",
                    responseCode = HttpServletResponse.SC_NO_CONTENT),
            @RestResponse(
                    description = "mediapackage not found or has no assets in S3",
                    responseCode = HttpServletResponse.SC_NOT_FOUND)
          },
          returnDescription = "Restore of assets initiated")
  public Response restoreAssets(@PathParam("mediaPackageId") final String mediaPackageId, @FormParam("restorePeriod") final Integer restorePeriod) {
    return handleException(new Function0<Response>() {
      private String getMediaPackageId() {
        return StringUtils.trimToNull(mediaPackageId);
      }

      private Integer getRestorePeriod() {
        return restorePeriod != null ? restorePeriod : awsS3AssetStore.getRestorePeriod();
      }

      @Override public Response apply() {
        final Query idQuery = QueryBuilder.query()
                .currentOrganization(securityService)
                .mediaPackageId(getMediaPackageId())
                .onlyLastVersion(true);
        final ResultSet result = archive.find(idQuery, uriRewriter);
        if (result.size() > 1)
          return serverError();
        if (result.size() == 0)
          return notFound();
        final ResultItem item = result.getItems().get(0);

        for (MediaPackageElement e : item.getMediaPackage().elements()) {
          if (e.getElementType() == MediaPackageElement.Type.Publication) {
            continue;
          }

          StoragePath storagePath = new StoragePath(securityService.getOrganization().getId(), getMediaPackageId(), item.getVersion(), e.getIdentifier());
          String assetStorageClass = awsS3AssetStore.getAssetStorageClass(storagePath);
          if (awsS3AssetStore.contains(storagePath)
                  && ("GLACIER".equals(assetStorageClass) || "DEEP_ARCHIVE".equals(assetStorageClass))) {
            try {
              // Initiate restore and return
              awsS3AssetStore.initiateRestoreAsset(storagePath, getRestorePeriod());
            } catch (ElementStoreException ex) {
              throw new ArchiveException(ex);
            }
          }
        }
        return noContent();
      }
    });
  }

  // Returns the S3 objectKey
  private final UriRewriter uriRewriter = new UriRewriter() {
      @Override public URI apply(Version version, MediaPackageElement mpe) {
         return mpe.getURI();
       }
  };

  /** Unify exception handling. */
  public static <A> A handleException(final Function0<A> f) {
    try {
      return f.apply();
    } catch (ArchiveException e) {
      if (e.isCauseNotAuthorized())
        throw new WebApplicationException(e, Response.Status.UNAUTHORIZED);
      if (e.isCauseNotFound())
        throw new WebApplicationException(e, Response.Status.NOT_FOUND);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    } catch (Exception e) {
      logger.error("Error calling archive REST method", e);
      if (e instanceof NotFoundException)
        throw new WebApplicationException(e, Response.Status.NOT_FOUND);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  void setAwsS3AssetStore(AwsS3AssetStore store) {
    awsS3AssetStore = store;
  }

   void setArchive(Archive service) {
     archive = service;
   }

  void setSecurityService(SecurityService service) {
    securityService = service;
  }
}
