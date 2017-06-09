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

package org.opencastproject.pm.ui.teacher;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.opencastproject.archive.api.Archive.JOB_TYPE;

import org.opencastproject.archive.api.ArchiveException;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageImpl;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.serviceregistry.api.RemoteBase;
import org.opencastproject.util.NotFoundException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author james
 * This is a embedded archive service facade because there is no archive-remote
 * this class only makes read-only calls to the archive service using the REST
 * endpoints.
 */
public class ArchiveServices extends RemoteBase {
  private static final Logger logger = LoggerFactory.getLogger(ArchiveServices.class);

  public ArchiveServices() {
    super(JOB_TYPE);
  }

  MediaPackage getMediaPackage(String mediapackageId) throws ArchiveException, UnauthorizedException, NotFoundException {
    HttpGet get = new HttpGet("archive/mediapackage/" + mediapackageId);
    HttpResponse response = getResponse(get, SC_OK, SC_NOT_FOUND, SC_UNAUTHORIZED);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          throw new NotFoundException("Mediapackage" + mediapackageId + " not found in remote archive!");
        } else if (SC_UNAUTHORIZED == response.getStatusLine().getStatusCode()) {
          throw new UnauthorizedException("Not authorized to get archive " + mediapackageId);
        } else {
          MediaPackage mediaPackage = MediaPackageImpl.valueOf(response.getEntity().getContent());
          logger.debug("Successfully received mediapackage {} from the remote archive", mediapackageId);
          return mediaPackage;
        }
      }
    } catch (UnauthorizedException e) {
      throw e;
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new ArchiveException("Unable to parse mediapackage from remote archive: " + e);
    } finally {
      closeConnection(response);
    }
    throw new ArchiveException("Unable to get mediapackage from remote archive");
  }

}
