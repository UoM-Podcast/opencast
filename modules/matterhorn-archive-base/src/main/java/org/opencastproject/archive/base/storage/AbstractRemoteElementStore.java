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

package org.opencastproject.archive.base.storage;

import static org.opencastproject.archive.base.storage.ElementStore.CONFIG_ARCHIVE_ROOT_DIR;

import org.opencastproject.util.ConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public abstract class AbstractRemoteElementStore implements RemoteElementStore {

  private static final Logger logger = LoggerFactory.getLogger(AbstractRemoteElementStore.class);

  /**
   * Creates the local cache for the remote element store.  This may or may not be required by the element store.
   * @param storeId The store's ID.  This will be the directory name for the cache.
   * @return A File pointing to the cache directory
   */
  public File createCacheDirectory(final ComponentContext cc, String storeId) throws ConfigurationException {
    String archiveRootDirectory = StringUtils.trimToNull(cc.getBundleContext().getProperty(CONFIG_ARCHIVE_ROOT_DIR));
    File cacheRoot = new File(archiveRootDirectory, RemoteElementStore.ELEMENT_STORE_CACHE_SLUG);
    File localCacheRoot = new File(cacheRoot, storeId);
    try {
      FileUtils.forceMkdir(localCacheRoot);
      logger.info("Local cache for {} created at {}", storeId, cacheRoot.getAbsolutePath());
      return localCacheRoot;
    } catch (IOException e) {
      throw new ConfigurationException("Unable to create " + localCacheRoot.getAbsolutePath());
    }
  }
}
