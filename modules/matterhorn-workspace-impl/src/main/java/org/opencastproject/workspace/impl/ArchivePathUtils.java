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

package org.opencastproject.workspace.impl;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;

public final class ArchivePathUtils {

  private static final Logger logger = LoggerFactory.getLogger(ArchivePathUtils.class);

  /** Key defining the archive root directory */
  private static final String CONFIG_ARCHIVE_ROOT = "org.opencastproject.episode.rootdir";

  /** Key defining the main storage directory */
  private static final String CONFIG_STORAGE_DIR = "org.opencastproject.storage.dir";

  private ArchivePathUtils() {
  }

  /**
   * Get the local mount point of the archive if it exists.
   *
   * @param componentContext
   *        The OSGI component context
   * @return Path to the local archive directory if it exists
   */
  public static String getArchivePath(final ComponentContext componentContext) {
    if (componentContext == null || componentContext.getBundleContext() == null) {
      return null;
    }
    final BundleContext bundleContext = componentContext.getBundleContext();

    String archiveDir = StringUtils.trimToNull(bundleContext.getProperty(CONFIG_ARCHIVE_ROOT));
    if (archiveDir == null) {
      archiveDir = StringUtils.trimToNull(bundleContext.getProperty(CONFIG_STORAGE_DIR));
      if (archiveDir != null) {
        archiveDir = new File(archiveDir, "archive").getAbsolutePath();
      }
    }

    // Is the archive available locally?
    if (archiveDir != null && new File(archiveDir).isDirectory()) {
      logger.debug("Found local archive directory at {}", archiveDir);
      return archiveDir;
    }

    return null;
  }

  /**
   * Splits up an archive URI and returns a local path instead.
   *
   * @param localPath
   *          Path to the local archive directory
   * @param organizationId
   *          Organization identifier
   * @param uri
   *          URI to the asset
   * @return Local file
   */
  public static File getLocalFile(final String localPath, final String organizationId, final URI uri) {
    if (localPath == null
            || organizationId == null
            || !uri.getScheme().startsWith("http")
            || !uri.getPath().startsWith("/archive/archive/")) {
      return null;
    }

    final String[] assetPath = uri.getPath().split("/");
    // /archive/archive/mediapackage/{mediaPackageID}/{mediaPackageElementID}/{version}/{filenameIgnore}
    if (assetPath.length != 7) {
      return null;
    }

    final String mediaPackageID = assetPath[3];
    final String mediaPackageElementID = assetPath[4];
    final String version = assetPath[5];
    final String filename = mediaPackageElementID + '.' + FilenameUtils.getExtension(assetPath[6]);
    final File file = Paths.get(localPath, organizationId, mediaPackageID, version, filename).toFile();
    if (file.isFile()) {
      logger.debug("Converted {} to local file at {}", uri, file);
      return file;
    }
    logger.debug("Local file for {} not available. {} does not exist.", uri, file);
    return null;
  }

}
