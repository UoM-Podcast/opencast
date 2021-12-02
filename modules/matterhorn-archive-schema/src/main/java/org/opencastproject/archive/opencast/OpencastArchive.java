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

package org.opencastproject.archive.opencast;

import static java.lang.String.format;
import static org.opencastproject.archive.base.StoragePath.spath;
import static org.opencastproject.archive.base.storage.Source.source;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Option.some;

import org.opencastproject.archive.api.ArchiveException;
import org.opencastproject.archive.api.ArchivedMediaPackageElement;
import org.opencastproject.archive.api.Query;
import org.opencastproject.archive.api.ResultItem;
import org.opencastproject.archive.api.UriRewriter;
import org.opencastproject.archive.api.Version;
import org.opencastproject.archive.base.ArchiveBase;
import org.opencastproject.archive.base.PartialMediaPackage;
import org.opencastproject.archive.base.Protected;
import org.opencastproject.archive.base.StoragePath;
import org.opencastproject.archive.base.persistence.ArchiveDb;
import org.opencastproject.archive.base.persistence.ArchiveDbException;
import org.opencastproject.archive.base.persistence.Asset;
import org.opencastproject.archive.base.persistence.Episode;
import org.opencastproject.archive.base.storage.DeletionSelector;
import org.opencastproject.archive.base.storage.ElementStore;
import org.opencastproject.archive.base.storage.RemoteElementStore;
import org.opencastproject.archive.base.storage.Source;
import org.opencastproject.archive.opencast.solr.SolrIndexManager;
import org.opencastproject.archive.opencast.solr.SolrRequester;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageSupport;
import org.opencastproject.message.broker.api.MessageReceiver;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.MimeTypeUtil;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Option;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.data.Opt;
import com.google.common.collect.Sets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Opencast specific implementation of the archive. */
public final class OpencastArchive extends ArchiveBase<OpencastResultSet> {
  public static final Set<MediaPackageElement.Type> MOVABLE_TYPES =
          Sets.newHashSet(MediaPackageElement.Type.Attachment, MediaPackageElement.Type.Catalog, MediaPackageElement.Type.Track);
  private static final String MANIFEST_DEFAULT_NAME = "manifest";
  private static final Logger logger = LoggerFactory.getLogger(OpencastArchive.class);
  private final SolrRequester solrRequester;
  private final SolrIndexManager solrIndex;
  private final Map<String, RemoteElementStore> remoteStores = new LinkedHashMap<String, RemoteElementStore>();
  // list of all the known stores starting with localSore followed by remoteStores
  private final Map<String, ElementStore> allStores = new LinkedHashMap<String, ElementStore>();
  private ElementStore localElementStore = null;
  private UriRewriter uriRewriter = null;

  public OpencastArchive(SolrIndexManager solrIndex, SolrRequester solrRequester, SecurityService secSvc,
          AuthorizationService authSvc, OrganizationDirectoryService orgDir, ServiceRegistry svcReg,
          WorkflowService workflowSvc, Workspace workspace, ArchiveDb persistence, ElementStore elementStore,
          String systemUserName, MessageSender messageSender, MessageReceiver messageReceiver) {
    super(secSvc, authSvc, orgDir, svcReg, workflowSvc, workspace, persistence, elementStore, systemUserName,
            messageSender, messageReceiver);
    this.solrIndex = solrIndex;
    this.solrRequester = solrRequester;
    localElementStore = elementStore;
    allStores.put(elementStore.getStoreType(), elementStore);
  }

  @Override
  public void add(final MediaPackage mp) throws ArchiveException {
    if (mp.getCatalogs(MediaPackageElements.EPISODE).length == 0)
      throw new ArchiveException("Archived Mediapackage didn't contain a necessary " + MediaPackageElements.EPISODE
              + " catalog.");
    super.add(mp);
  }

  @Override
  protected void index(MediaPackage mp, DublinCoreCatalog dc, AccessControlList acl, Date timestamp, Version version) {
    try {
      solrIndex.add(mp, dc, acl, timestamp, version);
    } catch (SolrServerException e) {
      throw new ArchiveException(e);
    }
  }

  @Override
  protected void index(MediaPackage mediaPackage, DublinCoreCatalog dc, AccessControlList acl, Version version, boolean deleted,
          Date modificationDate, boolean latestVersion) {
    try {
      solrIndex.add(mediaPackage, dc, acl, version, deleted, modificationDate, latestVersion);
    } catch (SolrServerException e) {
      throw new ArchiveException(e);
    }
  }

  @Override
  protected boolean indexDelete(String mediaPackageId, Date timestamp) {
    try {
      return solrIndex.delete(mediaPackageId, timestamp);
    } catch (SolrServerException e) {
      throw new ArchiveException(e);
    }
  }

  @Override
  protected OpencastResultSet indexFind(Query q) {
    try {
      if (q instanceof OpencastQuery)
        return solrRequester.find((OpencastQuery) q);
      else
        return solrRequester.find(OpencastQueryBuilder.query(q));
    } catch (SolrServerException e) {
      throw new ArchiveException(e);
    }
  }

  @Override
  protected long indexSize() {
    try {
      return solrIndex.count();
    } catch (ArchiveDbException e) {
      throw new ArchiveException(e);
    }
  }

  @Override
  protected OpencastResultSet newResultSet(final List<? extends ResultItem> rs, final String query,
          final long totalSize, final long offset, final long limit, final long searchTime) {
    return new OpencastResultSet() {
      @SuppressWarnings("unchecked")
      @Override
      public List<OpencastResultItem> getItems() {
        return (List<OpencastResultItem>) rs;
      }

      @Override
      public String getQuery() {
        return query;
      }

      @Override
      public long getTotalSize() {
        return totalSize;
      }

      @Override
      public long getLimit() {
        return limit;
      }

      @Override
      public long getOffset() {
        return offset;
      }

      @Override
      public long getSearchTime() {
        return searchTime;
      }
    };
  }

  /* Begin Remote Asset Storage Overrides */
  // Override to get files from whichever store they're stored in, rather than assuming the local store
  @Override
  public Option<ArchivedMediaPackageElement> get(final String mpId, final String mpElemId, final Version version)
          throws ArchiveException {
    return handleException(new Function0.X<Option<ArchivedMediaPackageElement>>() {
      @Override
      public Option<ArchivedMediaPackageElement> xapply() throws Exception {
        for (final Protected<Episode> p : getPersistence().getEpisode(mpId, version).map(protectEpisode(READ_PERMISSION))) {
          if (p.isGranted()) {
            final MediaPackage mp = p.getGranted().getMediaPackage();
            for (MediaPackageElement mpe : option(mp.getElementById(mpElemId))) {
              for (ElementStore store : allStores.values()) {
                for (InputStream stream : store.get(spath(getOrgId(), mpId, version, mpElemId))) {
                  logger.debug("found for MPE: {} in Store: {}", mpElemId, store.getStoreType());
                  long wait = 0;
                  if (stream == ElementStore.streamNotReady) {
                    wait = store.getReadyEstimate(spath(getOrgId(), mpId, version, mpElemId));
                  }
                  return some(new ArchivedMediaPackageElement(stream, mpe.getMimeType(), mpe.getSize(), wait));
                }
              }
            }
            // mediapackage element does not exist
            return none();
          } else {
            // episode is protected
            throw p.getRejected();
          }
        }
        // episode cannot be found
        return none();
      }
    });
  }

  /** Store all assets of <code>mp</code> under the given version.  This may mean fetching elements from external storage. */
  @Override
  protected void storeAssets(final PartialMediaPackage pmp, final Version version) throws Exception {
    final String mpId = pmp.getMediaPackage().getIdentifier().toString();
    final String orgId = getOrgId();
    for (final MediaPackageElement e : pmp.getPartial()) {
      logger.info(format("Archiving %s %s %s", e.getFlavor(), e.getMimeType(), e.getURI()));
      final StoragePath storagePath = spath(orgId, mpId, version, e.getIdentifier());
      final Version lastVersion = new Version(version.value() - 1);
      findAssetByChecksumAndMediaPackageId(e.getChecksum().toString(), mpId).fold(new Option.EMatch<StoragePath>() {

        @Override
        public void esome(final StoragePath found) {
          final String currentStoreId = getEpisodeStorageLocation(lastVersion, mpId).getOrElse(new Function0<String>() {
            @Override
            public String apply() {
              throw new ArchiveException(format("Error finding storage location for %s@%s", mpId, version.value() - 1));
            }
          });
          if (!isLocalStore(currentStoreId)) {
            try {
              //Note that we're copying the *previous* version here
              Option<Episode> lastVersionEpisode = getLatestEpisodeById(mpId);
              final PartialMediaPackage lastVersionMP = mkPartial(lastVersionEpisode.get().getMediaPackage());
              copyElementsToStore(lastVersionMP, orgId, lastVersion, localElementStore);
              getPersistence().setStorageLocation(mpId, lastVersion, localElementStore.getStoreType());
            } catch (IOException ex) {
              throw new ArchiveException(ex);
            }
          }
          boolean result = false;
          // trying to copy from the store it is in to the localStore
          // assuming allStores as [ localStore, remoteStores[] ]
          for (ElementStore store: allStores.values()) {
            if (store.copy(found, storagePath)) {
              result = true;
              logger.debug("The asset {} is located on store ({})", found, store.getStoreType());
              break;
            }
          }
          if (!result) {
            logger.error("The asset {} is not available on any of the connected filestores {}",
                  found, localElementStore.getStoreType(), allStores.keySet());
            throw new ArchiveException("An asset with checksum " + e.getChecksum().toString() + " has"
                    + " already been archived but trying to copy or link asset " + found + " failed");
          }
        }

        @Override
        public void enone() {
          Option<Long> size;
          if (e.getSize() > 0) {
            size = Option.some(e.getSize());
          } else {
            size = Option.<Long> none();
          }
          localElementStore.put(storagePath, source(e.getURI(), size, option(e.getMimeType())));
        }

      });
    }
  }

  @Override
  public boolean delete(final String mediaPackageId) throws ArchiveException {
    // Try to delete archive from active remotes
    DeletionSelector selector = DeletionSelector.delAll(getOrgId(), mediaPackageId);
    for (Map.Entry<String, RemoteElementStore> remote: remoteStores.entrySet()) {
      logger.info("Attempting to remove mediapackage {} from remote store {}", mediaPackageId, remote.getKey());
      remote.getValue().delete(selector);
    }

    // Remove archive from opencast
    return super.delete(mediaPackageId);
  }

  /* End Remote Asset Storage Overrides */

  /* Begin New Remote Asset Storage Code */
  public Set<String> getRemoteElementStoreIds() {
    return remoteStores.keySet();
  }

  public void addRemoteElementStore(RemoteElementStore elementStore) {
    if (!ElementStore.DISABLED_STORE_TYPE.equals(elementStore.getStoreType())) {
      remoteStores.put(elementStore.getStoreType(), elementStore);
      allStores.put(elementStore.getStoreType(),elementStore);
    }
  }

  public void removeRemoteElementStore(RemoteElementStore elementStore) {
    remoteStores.remove(elementStore.getStoreType());
    allStores.remove(elementStore.getStoreType());
  }

  public Option<ElementStore> getRemoteElementStore(String id) {
    if (remoteStores.containsKey(id)) {
      return Option.some((ElementStore) remoteStores.get(id));
    } else {
      return Option.none();
    }
  }

  public Option<ElementStore> getElementStore(String id) {
    if (isLocalStore(id)) {
      return Option.some(localElementStore);
    } else {
      return getRemoteElementStore(id);
    }
  }

  private boolean isLocalStore(String id) {
    return localElementStore.getStoreType().equals(id);
  }

  private void processOperation(Episode ep, String targetStoreId, UriRewriter rewriter) {
    if (StringUtils.isBlank(ep.getStoreId())) {
      logger.warn("Blank store ID for " + ep.getMediaPackage().getIdentifier().toString() + "@" + ep.getVersion().toString());
      return;
    }

    if (ep.getStoreId().equals(targetStoreId)) {
      //We're already in the right place
      return;
    }

    ElementStore currentStore = getElementStore(ep.getStoreId()).getOrElseNull();
    ElementStore targetStore = getElementStore(targetStoreId).getOrElseNull();

    try {
      if (null == currentStore) {
        Thread.sleep(10000);
        logger.error("Unknown store for current store: " + ep.getStoreId());
        return;
      }
      if (null == targetStore) {
        Thread.sleep(10000);
        logger.error("Unknown store for target store: " + targetStoreId);
        return;
      }
    } catch (InterruptedException ex) {
        //
    }

    if (isLocalStore(ep.getStoreId()) || isLocalStore(targetStoreId)) {
      //Content is local, or is going to be local after the move
      logger.debug("Moving {} from {} to {}", ep , ep.getStoreId(), targetStoreId);

      try {
        PartialMediaPackage pmp = mkPartial(MediaPackageSupport.copy(ep.getMediaPackage()));
        for (MediaPackageElement mpe : pmp.getPartial()) {
          mpe.setURI(rewriter.apply(ep.getVersion(), mpe));
        }
        copyElementsToStore(pmp, ep.getOrganization(), ep.getVersion(), targetStore);
        logger.debug("Done moving elements for {} from {} to {}", ep, ep.getStoreId(), targetStoreId);
        copyManifest(ep, targetStore);
        logger.debug("Done moving manifest for {} from {} to {}", ep, ep.getStoreId(), targetStoreId);
      } catch (IOException | NotFoundException | ArchiveException e) {
        //Rollback the action?
        logger.error("Error moving elements or manifest for {} from {} to {}: {}", ep, ep.getStoreId(), targetStoreId, e.getMessage());
        deleteElementsFromStore(ep, targetStore);
        logger.error("Deleting any elements of {} from {}", ep, targetStoreId);
        return;
      }
      getPersistence().setStorageLocation(ep.getMediaPackage().getIdentifier().toString(), ep.getVersion(), targetStoreId);
      logger.debug("Setting storage location for {} to {}", ep, targetStoreId);
      deleteElementsFromStore(ep, currentStore);
      logger.debug("Deleting elements of {} from {}", ep, currentStore);
    } else {
      //Content is not local, thus download to local and then upload to new remote
      String intermediate = localElementStore.getStoreType();
      logger.debug("Moving {} from {} to {}, then to {}", ep, ep.getStoreId(), intermediate, targetStoreId);

      try {
        moveToStore(ep.getVersion(), ep.getMediaPackage().getIdentifier().toString(), intermediate, rewriter);
      } catch (NotFoundException e) {
        logger.error("Unable to move {} to {}", ep, intermediate);
      }
      try {
        moveToStore(ep.getVersion(), ep.getMediaPackage().getIdentifier().toString(), targetStoreId, rewriter);
      } catch (NotFoundException e) {
        logger.error("Unable to move {} to {}", ep, targetStoreId);
      }
    }
  }

  private void copyElementsToStore(PartialMediaPackage mp, String orgId, Version version, ElementStore store) throws ArchiveException, IOException {
    final String mpId = mp.getMediaPackage().getIdentifier().toString();
    final String prettyMpId = mpId + "@v" + version;
    logger.debug(format("Moving assets for snapshot %s to store %s", prettyMpId, store.getStoreType()));
    for (final MediaPackageElement e : mp.getPartial()) {
      if (!MOVABLE_TYPES.contains(e.getElementType())) {
        logger.debug(format("Skipping %s because type is %s", e.getIdentifier(), e.getElementType()));
        continue;
      }
      logger.debug(format("Moving %s to store %s", e.getIdentifier(), store.getStoreType()));
      final StoragePath storagePath = StoragePath.spath(orgId, mpId, version, e.getIdentifier());
      if (store.contains(storagePath)) {
        logger.debug(format("Element %s (version %s) is already in store %s so skipping it", e.getIdentifier(),
                version.toString(),
                store.getStoreType()));
        continue;
      }
      final Option<StoragePath> existingAssetOpt = findAssetByChecksumAndStore(e.getChecksum().toString(), store.getStoreType());
      if (existingAssetOpt.isSome()) {
        final StoragePath existingAsset = existingAssetOpt.get();
        logger.debug("Content of asset {} with checksum {} already exists in {}",
                existingAsset.getAssetId(), e.getChecksum(), store.getStoreType());
        boolean result = false;
        // trying to copy from the store it is in to the localStore
        // assuming allStores as [ localStore, remoteStores[] ]
        for (ElementStore elemStore : allStores.values()) {
          if (elemStore.copy(existingAsset, storagePath)) {
            logger.debug("The asset {} is located on store ({})", existingAsset, elemStore.getStoreType());
            result = true;
            break;
          }
        }
        if (!result) {
          logger.error("The asset {} is not available on any of the connected filestores {}",
                existingAsset, allStores.keySet());
          throw new ArchiveException(
              format("An element with checksum %s has already been archived but trying to copy or link asset %s to it failed",
                      e.getChecksum(), existingAsset));
        }
      } else {
        try {
          URI elementUri = e.getURI();
          //Get where the asset is currently stored, if it exists.  This asset is null in the case of *new* assets being added for the first time
          Asset a = getPersistence().findAssetByChecksumAndMediaPackageId(e.getChecksum().toString(), mpId).getOrElseNull();
          if (null != a) {
            String currentStoreId = a.getStoreId();
            // If it's not the local store
            if (!isLocalStore(currentStoreId)) {
              ElementStore currentStore = getElementStore(currentStoreId).getOrElseNull();
              //Get a handle on the found asset's file and put it in the workspace
              //NB: FileSystemElementStore assumes that anything you put() into it is already in the workspace...
              Option<InputStream> stream = currentStore.get(a.getStoragePath());
              //Archived in remote store create appropiate workspace uri
              //FIXME: is there functionality to do this better?
              String filename;
              if (StringUtils.contains(e.getFlavor().getSubtype(), "xacml+series")) {
                filename = "xacml." + Option.option(e.getMimeType()).bind(MimeTypeUtil.suffix).getOrElse("unknown");
              } else {
                filename = e.getElementType().toString().toLowerCase() + "." + Option.option(e.getMimeType()).bind(MimeTypeUtil.suffix).getOrElse("unknown");
              }
              //Stash it in the workspace
              elementUri = getWorkspace().put(mpId, e.getIdentifier(), filename, stream.get());
            }
          }
          final Option<Long> size = e.getSize() > 0 ? Option.some(e.getSize()) : Option.<Long>none();
          final Option<MimeType> mimetype = e.getMimeType() != null ? Option.some(e.getMimeType()) : Option.<MimeType>none();
          //Push it to the target store
          store.put(storagePath, Source.source(elementUri, size, mimetype));
        } catch (MalformedURLException | ArchiveDbException ex) {
          throw new ArchiveException(ex);
        }
      }
    }
  }

  private void copyManifest(Episode ep, ElementStore targetStore) throws NotFoundException, IOException {
    final String mpId = ep.getMediaPackage().getIdentifier().toString();
    final String orgId = ep.getOrganization();
    final Version version = ep.getVersion();

    ElementStore currentStore = getElementStore(ep.getStoreId()).get();
    Opt<String> manifestOpt = findManifestBaseName(ep, MANIFEST_DEFAULT_NAME, currentStore);
    if (manifestOpt.isNone())
      return; // Nothing to do, already moved to long-term storage

    // Copy the manifest file
    String manifestBaseName = manifestOpt.get();
    StoragePath pathToManifest = new StoragePath(orgId, mpId, version, manifestBaseName);

    // Already copied?
    if (!targetStore.contains(pathToManifest)) {
      Option<InputStream> inputStreamOpt;
      InputStream inputStream = null;
      String manifestFileName = null;
      try {
        inputStreamOpt = currentStore.get(pathToManifest);
        if (inputStreamOpt.isNone()) { // This should never happen because it has been tested before
          throw new NotFoundException(
                  format("Unexpected error. Manifest %s not found in current asset store", manifestBaseName));
        }
        inputStream = inputStreamOpt.get();
        manifestFileName = UUID.randomUUID().toString() + ".xml";
        URI manifestTmpUri = getWorkspace().putInCollection("archive", manifestFileName, inputStream);
        targetStore.put(pathToManifest, Source.source(manifestTmpUri, Option.<Long> none(), Option.some(MimeTypes.XML)));
      } finally {
        IOUtils.closeQuietly(inputStream);
        try {
          // Make sure to clean up the temporary file
          getWorkspace().deleteFromCollection("archive", manifestFileName);
        } catch (NotFoundException e) {
          // This is OK, we are deleting it anyway
        } catch (IOException e) {
          // This usually happens when the collection directory cannot be deleted
          // because another process is running at the same time and wrote a file there
          // after it was tested but before it was actually deleted. We will consider this ok.
          // Does the error message mention the manifest file name?
          if (e.getMessage().indexOf(manifestFileName) > -1) {
            logger.warn("The manifest file {} didn't get deleted from the archive collection: {}",
                    manifestBaseName, e);
          }
          // Else the error is related to the file-archive collection, which is fine
        }
      }
    }
  }

  Opt<String> findManifestBaseName(Episode ep, String manifestName, ElementStore store) {
    StoragePath path = new StoragePath(ep.getOrganization(), ep.getMediaPackage().getIdentifier().toString(),
            ep.getVersion(), manifestName);
    // If manifest_.xml, etc not found, return previous name (copied from the EpsiodeServiceImpl logic)
    if (!store.contains(path)) {
      // If first call, manifest is not found, which probably means it has already been moved
      if (MANIFEST_DEFAULT_NAME.equals(manifestName))
        return Opt.none(); // No manifest found in current store
      else
        return Opt.some(manifestName.substring(0, manifestName.length() - 1));
    }
    // This is the same logic as when building the manifest name: manifest, manifest_, manifest__, etc
    return findManifestBaseName(ep, manifestName + "_", store);
  }

  public void moveToStore(Version version, String mediaPackageId, String storeId, UriRewriter rewriter) throws NotFoundException {
    //Cache this for use in add()
    uriRewriter = rewriter;
    try {
      Option<Episode> result = getPersistence().getEpisode(mediaPackageId, version);
      if (result.isNone()) {
        throw new NotFoundException("Mediapackage " + mediaPackageId + "@" + version + " not found");
      }
      processOperation(result.get(), storeId, rewriter);
    } catch (ArchiveDbException e) {
      logger.error("Error finding " + mediaPackageId + "@" + version.toString(), e);
    }
  }

  private void deleteElementsFromStore(Episode ep, ElementStore store) {
    store.delete(DeletionSelector.del(ep.getOrganization(), ep.getMediaPackage().getIdentifier().toString(), ep.getVersion()));
  }

  /** Check if element <code>e</code> is already part of the history and in a specific store */
  private Option<StoragePath> findAssetByChecksumAndStore(final String checksum, final String storeId) {
    try {
      return getPersistence().findAssetByChecksumAndStore(checksum, storeId).map(new Function<Asset, StoragePath>() {
        @Override
        public StoragePath apply(Asset dto) {
          return dto.getStoragePath();
        }
      });
    } catch (ArchiveDbException e) {
      logger.error("Error finding " + checksum + " in store " + storeId, e);
      return Option.none();
    }
  }

  /** Check if element <code>e</code> is already part of the history and in a specific store */
  private Option<StoragePath> findAssetByChecksumAndMediaPackageId(final String checksum, final String mpId) {
    try {
      return getPersistence().findAssetByChecksumAndMediaPackageId(checksum, mpId).map(new Function<Asset, StoragePath>() {
        @Override
        public StoragePath apply(Asset dto) {
          return dto.getStoragePath();
        }
      });
    } catch (ArchiveDbException e) {
      logger.error("Error finding " + checksum + " in mediapackage " + mpId, e);
      return Option.none();
    }
  }
  public Option<String> getEpisodeStorageLocation(Version version, String mediaPackageId) {
    try {
      Option<Episode> result = getPersistence().getEpisode(mediaPackageId, version);
      if (result.isNone()) {
        return Option.none();
      }
      return Option.some(result.get().getStoreId());
    } catch (ArchiveDbException e) {
      logger.error("Error finding " + mediaPackageId + "@" + version.toString(), e);
      return Option.none();
    }
  }

  public List<Episode> getEpisodesById(String mediapackageId) {
    try {
      return getPersistence().getEpisode(mediapackageId);
    } catch (ArchiveDbException e) {
      logger.error("Error finding " + mediapackageId, e);
      return new LinkedList<>();
    }
  }

  public Option<Episode> getLatestEpisodeById(String mediapackageId) {
    try {
      return getPersistence().getLatestEpisode(mediapackageId);
    } catch (ArchiveDbException e) {
      logger.error("Error finding " + mediapackageId, e);
      return Option.none(Episode.class);
    }
  }
  public List<Episode> getEpisodesByDate(Date start, Date end) {
    try {
      return getPersistence().getEpisodes(start, end);
    } catch (ArchiveDbException e) {
      logger.error("Error finding episodes between " + start + " and " + end);
      return new LinkedList<>();
    }
  }

  public List<Episode> getEpisodesByIdAndDate(String mediapackageId, Date start, Date end) {
    try {
      return getPersistence().getEpisodes(start, end);
    } catch (ArchiveDbException e) {
      logger.error("Error finding episodes between " + start + " and " + end);
      return new LinkedList<>();
    }
  }
}
