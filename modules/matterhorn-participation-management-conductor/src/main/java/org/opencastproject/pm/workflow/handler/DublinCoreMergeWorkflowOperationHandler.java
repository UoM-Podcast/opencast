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

package org.opencastproject.pm.workflow.handler;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Workflow operation for handling "clean" operations
 */
public class DublinCoreMergeWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(DublinCoreMergeWorkflowOperationHandler.class);

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put("source-flavors", "Merge Dublin Core catalogs of these flavors");
  }

  /** The Dublin Core catalog service */
  private DublinCoreCatalogService dcService;

  /** The local workspace */
  private Workspace workspace;

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#getConfigurationOptions()
   */
  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return CONFIG_OPTIONS;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    MediaPackage mediaPackageFromWorkflow = workflowInstance.getMediaPackage();

    // Check which tags have been configured
    String flavors = workflowInstance.getCurrentOperation().getConfiguration("source-flavors");
    if (StringUtils.trimToNull(flavors) == null) {
      logger.warn("No flavors have been specified, so nothing will be merged");
      return createResult(mediaPackageFromWorkflow, Action.CONTINUE);
    }

    // Iterate over all flavors and merge the catalogs
    for (String flavor : asList(flavors)) {
      try {
        logger.info("Merging catalogs of type '{}'", flavor);
        MediaPackageElementFlavor f = MediaPackageElementFlavor.parseFlavor(flavor);
        mediaPackageFromWorkflow = merge(mediaPackageFromWorkflow, f);
      } catch (IllegalArgumentException e) {
        throw new WorkflowOperationException("Unable to parse '" + flavor + "' into a media package element flavor");
      }
    }

    return createResult(mediaPackageFromWorkflow, Action.CONTINUE);
  }

  /**
   * Merges the Dublin Core catalogs of the given flavor into a single catalog.
   *
   * @param mediapackage
   *          the mediapackage
   * @param flavor
   *          the catalog flavor
   * @return the updated mediapackage
   * @throws WorkflowOperationException
   *           if merging fails
   */
  protected MediaPackage merge(MediaPackage mediapackage, MediaPackageElementFlavor flavor)
          throws WorkflowOperationException {

    List<MediaPackageElement> catalogs = new ArrayList<MediaPackageElement>();

    // Keep those elements that have been identified in the tags
    catalogs.addAll(Arrays.asList(mediapackage.getElementsByFlavor(flavor)));
    if (catalogs.size() <= 1) {
      logger.info("No multiple Dublin Core catalog instances with flavor '{}' found, nothing to merge", flavor);
      return mediapackage;
    }

    // Select the one to keep, which is the first one in the list
    Catalog masterCatalog = (Catalog) catalogs.get(0);
    logger.info("Selecting Dublin Core catalog '{}' as the master copy", masterCatalog.getIdentifier());

    // Load the master dc catalog contents
    DublinCoreCatalog masterDc = null;
    while (masterDc == null) {
      try {
        masterDc = loadCatalog(masterCatalog.getURI());
      } catch (NotFoundException e) {
        logger.warn("Dublin Core catalog {} not found", masterCatalog.getURI());
      } catch (IOException e) {
        logger.warn("Unable to read Dublin Core catalog contents from {}", masterCatalog.getURI(), e);
      }

      // Sometimes the catalogs are empty and can't be loaded
      if (masterDc == null) {
        catalogs.remove(0);
        mediapackage.remove(masterCatalog);
        if (catalogs.size() == 1) {
          logger.info("No more catalogs of type {} remaining to merge, giving up", flavor.toString());
          return mediapackage;
        } else {
          masterCatalog = (Catalog) catalogs.get(0);
          logger.info("Selecting Dublin Core catalog '{}' as the new master copy", masterCatalog.getIdentifier());
        }
      }
    }

    // Do we have anything left to merge=
    if (catalogs.size() == 1) {
      logger.info("There is only one catalog of type {} left, therefore nothing to merge", flavor.toString());
      return mediapackage;
    }

    // Merge everything into the master catalog and remove the rest (skip the first one)
    Iterator<MediaPackageElement> ci = catalogs.iterator();
    ci.next();

    while (ci.hasNext()) {
      Catalog catalog = (Catalog) ci.next();

      try {
        DublinCoreCatalog dc = loadCatalog(catalog.getURI());
        for (EName property : dc.getValues().keySet()) {
          logger.info("Merging property '{}' from {} into {}",
                  new String[] { property.getLocalName(), catalog.getIdentifier(), masterCatalog.getIdentifier() });
          masterDc.set(property, dc.get(property));
        }
      } catch (NotFoundException e) {
        logger.warn("Dublin Core catalog {} of type {} not found", catalog.getURI(), flavor.toString());
      } catch (IOException e) {
        logger.warn("Unable to read Dublin Core catalog contents from {}", catalog.getURI(), e);
      }

      // Finally, remove the catalog from the mediapackage and from the workspace
      mediapackage.remove(catalog);

      // Remove the catalog from the workspace
      try {
        logger.info("Deleting superfluous Dublin Core catalog '{}'", catalog.getURI());
        workspace.delete(catalog.getURI());
      } catch (NotFoundException e) {
        logger.debug("Dublin Core catalog {} not found in the workspace", catalog.getURI());
      } catch (IOException e) {
        logger.debug("Unable to delete Dublin Core catalog contents from {}", catalog.getURI(), e);
      }

      // Store the updated version of the catalog
      InputStream is = null;
      try {
        is = dcService.serialize(masterDc);
        String mediaPackageId = mediapackage.getIdentifier().toString();
        String catalogId = masterCatalog.getIdentifier();
        String filename = FilenameUtils.getName(masterCatalog.getURI().getPath());
        workspace.put(mediaPackageId, catalogId, filename, is);
      } catch (IOException e) {
        logger.warn("Error serializing the updatd master Dublin Core catalog to {}");
      }

    }

    return mediapackage;
  }

  /**
   * Loads the Dublin Core catalog from the given URI.
   *
   * @param uri
   *          the catalog's location
   * @return the catalog contents
   * @throws NotFoundException
   *           if the workspace can't find the URI
   * @throws IOException
   *           if the catalog file can't be opened or parsed
   */
  protected DublinCoreCatalog loadCatalog(URI uri) throws NotFoundException, IOException {
    DublinCoreCatalog masterDc = null;
    File masterDcFile = null;
    FileInputStream fis = null;
    try {
      masterDcFile = workspace.get(uri);
      masterDc = dcService.load(FileUtils.openInputStream(masterDcFile));
      return masterDc;
    } finally {
      IOUtils.closeQuietly(fis);
    }
  }

  /**
   * Callback for declarative services configuration that will introduce us to the Dublin Core service. Implementation
   *
   * @param dcService
   *          an instance of the Dublin Core service
   */
  public void setDublinCoreService(DublinCoreCatalogService dcService) {
    this.dcService = dcService;
  }

  /**
   * Callback for declarative services configuration that will introduce us to the local workspace service.
   * Implementation assumes that the reference is configured as being static.
   *
   * @param workspace
   *          an instance of the workspace
   */
  void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

}
