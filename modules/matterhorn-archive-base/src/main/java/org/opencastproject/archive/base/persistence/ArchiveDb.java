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

package org.opencastproject.archive.base.persistence;

import org.opencastproject.archive.api.Version;
import org.opencastproject.archive.base.PartialMediaPackage;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.util.data.Option;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

/** API that defines persistent storage of episodes. */
public interface ArchiveDb {

  /**
   * Returns the number of episodes in persistent storage.
   *
   * @return The count of episodes.
   * @throws ArchiveDbException
   *           if exception occurs
   */
  int countAllEpisodes() throws ArchiveDbException;

  /**
   * Returns all episodes in persistence storage
   *
   * @return {@link Episode} iterator representing stored episodes
   * @throws ArchiveDbException
   *         if exception occurs
   */
  Iterator<Episode> getAllEpisodes() throws ArchiveDbException;

  Version claimVersion(String mpId) throws ArchiveDbException;

  /**
   * Returns the media package from the selected episode
   *
   * @param mediaPackageId
   *         the media package id to select
   * @param version
   *         the archive version to select
   * @return the media package
   * @throws ArchiveDbException
   *         if an error occurs
   */
  Option<Episode> getEpisode(String mediaPackageId, Version version) throws ArchiveDbException;

  Option<Episode> getLatestEpisode(String mediaPackageId) throws ArchiveDbException;

  /**
   * Returns the latest version flag of the selected episode
   *
   * @param mediaPackageId
   *         the media package id to select
   * @param version
   *         the archive version to select
   * @return the latest version flag
   * @throws ArchiveDbException
   *         if an error occurs
   */
  Option<Boolean> isLatestVersion(String mediaPackageId, Version version) throws ArchiveDbException;

  /**
   * Returns the deletion date from the selected episode.
   *
   * @param mediaPackageId
   *         the media package id to select
   * @return the deletion date
   * @throws ArchiveDbException
   *         if an error occurs
   */
  Option<Date> getDeletionDate(String mediaPackageId) throws ArchiveDbException;

  /**
   * Sets a deletion date to the selected episode.
   *
   * @param mediaPackageId
   *         the media package id to select
   * @param deletionDate
   *         the deletion date to set
   * @return <code>false</code> if the requested media package does not exist
   * @throws ArchiveDbException
   *         if an error occurs
   */
  boolean deleteEpisode(String mediaPackageId, Date deletionDate) throws ArchiveDbException;

  /**
   * Stores a new version of a media package
   *
   * @param pmp
   *         the media package to store
   * @param dublinCore
   *         the dublinCore of the media package
   * @param acl
   *         the acl of the media package
   * @param now
   *         the store date
   * @param version
   *         the new version from the archive
   * @param storeId
   *         the id of the store where the episode currently resides
   * @throws ArchiveDbException
   *         if an error occurs
   */
  void storeEpisode(PartialMediaPackage pmp,
                    DublinCoreCatalog dublinCore,
                    AccessControlList acl,
                    Date now,
                    Version version,
                    String storeId) throws ArchiveDbException;

  /**
   * Updates the Dublin Core of a media package
   *
   * @param mediaPackageId
   *         the media package id to select
   * @param version
   *         the Version of the media package
   * @param dublinCoreXml
   *         the dublinCore of the media package
   * @return <code>true</code> if the DublincoreXml has been inserted in the database
   * @throws ArchiveDbException
   *         if an error occurs
   */
  boolean updateEpisodeDC(String mediaPackageId, Version version, String dublinCoreXml)
    throws ArchiveDbException;

  /**
   * Find an asset by its checksum
   *
   * @param checksum
   *          the checksum of the asset
   * @return
   *          an Option containing the asset.  Maybe.
   * @throws ArchiveDbException
   */
  Option<Asset> findAssetByChecksum(String checksum) throws ArchiveDbException;

  /**
   * Find an asset by its checksum and store
   *
   * @param checksum
   *          the checksum of the asset
   * @param storeId
   *          the id of the store to search
   * @return
   *          an Option containing the asset.  Maybe.
   * @throws ArchiveDbException
   */
  Option<Asset> findAssetByChecksumAndStore(String checksum, String storeId) throws ArchiveDbException;

  /**
   * Set the storage location for an episode
   *
   * @param mediaPackageId
   *          the mediapackage id to modify
   * @param version
   *          the version of the mediapackaeg to modify
   * @param targetStoreId
   *          the id of the store where the episode should be stored
   * @return
   *          true if the update succeeded (or is a noop), false otherwise
   */
  boolean setStorageLocation(String mediaPackageId, Version version, String targetStoreId);

  /**
   * Return a list of all versions for an episode.
   *
   * @param mediaPackageId
   *          the mediapackage id to select
   * @return
   *          the (potentially empty) list of versions for that episode
   * @throws ArchiveDbException
   */
  List<Episode> getEpisode(String mediaPackageId) throws ArchiveDbException;

  /**
   * Return a list of all versions and episodes archived between the start and end dates
   *
   * @param start
   *          the start date for the window
   * @param end
   *          the end date for the window
   * @return
   *          the (potentially empty) list of versions and episodes
   * @throws ArchiveDbException
   */
  List<Episode> getEpisodes(Date start, Date end) throws ArchiveDbException;

  /**
   * Return a list of all versions and episodes archived between the start and end dates
   *
   * @param mediaPackageId
   *          the mediapackage id to select
   * @param start
   *          the start date for the window
   * @param end
   *          the end date for the window
   * @return
   *          the (potentially empty) list of versions and episodes
   * @throws ArchiveDbException
   */
  List<Episode> getEpisodes(String mediaPackageId, Date start, Date end) throws ArchiveDbException;
}
