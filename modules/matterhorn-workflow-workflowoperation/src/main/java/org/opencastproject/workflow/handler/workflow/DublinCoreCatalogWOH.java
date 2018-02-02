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
package org.opencastproject.workflow.handler.workflow;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.selector.SimpleElementSelector;
import org.opencastproject.metadata.dublincore.DCMIPeriod;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;

import org.opencastproject.metadata.dublincore.Precision;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;


/**
 * Take look in specified catalog for specified term, if the value matches the specified value add the target-tags
 */
public class DublinCoreCatalogWOH extends ResumableWorkflowOperationHandlerBase {

  private static final Logger logger = LoggerFactory.getLogger(DublinCoreCatalogWOH.class);
  private static final String PLUS = "+";
  private static final String MINUS = "-";

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  /** Name of the configuration option that provides the source flavors we are looking for */
  public static final String SOURCE_FLAVORS_PROPERTY = "source-flavors";

  /** Name of the configuration option that provides the source tags we are looking for */
  public static final String SOURCE_TAGS_PROPERTY = "source-tags";

  /** Name of the configuration option that provides the target flavors we are looking for */
  public static final String TARGET_FLAVOR_PROPERTY = "target-flavor";

  /** Name of the configuration option that provides the target tags we are looking for */
  public static final String TARGET_TAGS_PROPERTY = "target-tags";

  /** episode or series **/
  private static final String DCCATALOG_PROPERTY = "dccatalog";

  /** set (overwrite) the available from date */
  private static final String SET_AVAILABLE_DATE_PROPERTY = "set-available-date";

  /** Name of the configuration option that provides the copy boolean we are looking for */
  public static final String COPY_PROPERTY = "copy";

  /** The dublincore catalog service */
  private DublinCoreCatalogService dcService;

  /** The local workspace */
  private Workspace workspace = null;

  static {
    CONFIG_OPTIONS = new TreeMap<>();
    CONFIG_OPTIONS.put(SOURCE_FLAVORS_PROPERTY,
            "Tagging any mediapackage elements with one of these (comma sparated) flavors.");
    CONFIG_OPTIONS.put(SOURCE_TAGS_PROPERTY,
            "Tagging any mediapackage elements with one of these (comma separated) tags.");
    CONFIG_OPTIONS.put(SET_AVAILABLE_DATE_PROPERTY,
            "Set (overwrite) the available from date");
    CONFIG_OPTIONS.put(TARGET_FLAVOR_PROPERTY, "Apply these flavor to any mediapackage elements");
    CONFIG_OPTIONS
            .put(TARGET_TAGS_PROPERTY,
                    "Apply these (comma separated) tags to any mediapackage elements. If a target-tag starts with a '-', "
                    + "tag will removed from preexisting tags, if starts with a '+', tag will added to preexisting tags.");
    CONFIG_OPTIONS.put(COPY_PROPERTY,
            "Indicates if any mediapackage elements should be copied 'true' or overridden 'false'");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#getConfigurationOptions()
   */
  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return CONFIG_OPTIONS;
  }

  public void setDublincoreService(DublinCoreCatalogService dcService) {
    this.dcService = dcService;
  }

  /**
   * Callback for declarative services configuration that will introduce us to the local workspace service.
   * Implementation assumes that the reference is configured as being static.
   *
   * @param workspace
   *          an instance of the workspace
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {

    MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    WorkflowOperationInstance currentOperation = workflowInstance.getCurrentOperation();

    String configuredSourceFlavors = StringUtils
            .trimToEmpty(currentOperation.getConfiguration(SOURCE_FLAVORS_PROPERTY));
    String configuredSourceTags = StringUtils.trimToEmpty(currentOperation.getConfiguration(SOURCE_TAGS_PROPERTY));
    boolean setAvailableDate = BooleanUtils.toBoolean(currentOperation.getConfiguration(SET_AVAILABLE_DATE_PROPERTY));
    String configuredTargetFlavor = StringUtils.trimToNull(currentOperation.getConfiguration(TARGET_FLAVOR_PROPERTY));
    String configuredTargetTags = StringUtils.trimToEmpty(currentOperation.getConfiguration(TARGET_TAGS_PROPERTY));
    boolean copy = BooleanUtils.toBoolean(currentOperation.getConfiguration(COPY_PROPERTY));

    String[] sourceTags = StringUtils.split(configuredSourceTags, ",");
    String[] targetTags = StringUtils.split(configuredTargetTags, ",");
    String[] sourceFlavors = StringUtils.split(configuredSourceFlavors, ",");

    SimpleElementSelector elementSelector = new SimpleElementSelector();
    for (String flavor : sourceFlavors) {
      elementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor));
    }
    for (String tag : sourceTags) {
      elementSelector.addTag(tag);
    }

    List<String> removeTags = new ArrayList<>();
    List<String> addTags = new ArrayList<>();
    List<String> overrideTags = new ArrayList<>();

    for (String tag : targetTags) {
      if (tag.startsWith(MINUS)) {
        removeTags.add(tag);
      } else if (tag.startsWith(PLUS)) {
        addTags.add(tag);
      } else {
        overrideTags.add(tag);
      }
    }

    Collection<MediaPackageElement> elements = elementSelector.select(mediaPackage, false);
    for (MediaPackageElement el : elements) {
      MediaPackageElement element = el;

      if (copy) {
        element = (MediaPackageElement) el.clone();
        element.setIdentifier(null);
        element.setURI(el.getURI()); // use the same URI as the original
      }

      if (element.getElementType() == MediaPackageElement.Type.Catalog) {
        Catalog catalog = (Catalog) element;

        if (setAvailableDate) {
          DublinCoreCatalog dc = DublinCoreUtil.loadDublinCore(workspace, catalog);

          DCMIPeriod period;
          Date now = new Date();

          if (dc.hasValue(DublinCore.PROPERTY_AVAILABLE)) {
            DCMIPeriod prevPeriod = EncodingSchemeUtils.decodePeriod(dc.getFirstVal(DublinCore.PROPERTY_AVAILABLE));
            if (prevPeriod.hasEnd()) { // DCMIPeriod has no set methods
              period = new DCMIPeriod(now, prevPeriod.getEnd());
            } else {
              period = new DCMIPeriod(now, null);
            }
            dc.set(DublinCore.PROPERTY_AVAILABLE, EncodingSchemeUtils.encodePeriod(period, Precision.Minute));
          } else {
            period = new DCMIPeriod(now, null);
            dc.add(DublinCore.PROPERTY_AVAILABLE, EncodingSchemeUtils.encodePeriod(period, Precision.Minute));
          }

          try {
            workspace.put(mediaPackage.getIdentifier().toString(), catalog.getIdentifier(),
                    FilenameUtils.getName(catalog.getURI().getPath()),
                    dcService.serialize(dc));
            catalog.setURI(workspace.getURI(mediaPackage.getIdentifier().toString(), catalog.getIdentifier()));
            logger.info("Updated catalog {} dcterm:available", catalog.getIdentifier());
          } catch (IOException e) {
            logger.error("Couldn't save updated catalog {}", catalog.getIdentifier());
            throw new WorkflowOperationException(e);
          }
        }
      }

      if (configuredTargetFlavor != null) {
        element.setFlavor(MediaPackageElementFlavor.parseFlavor(configuredTargetFlavor));
      }

      if (overrideTags.size() > 0) {
        element.clearTags();
        for (String tag : overrideTags) {
          element.addTag(tag);
        }
      } else {
        for (String tag : removeTags) {
          element.removeTag(tag.substring(MINUS.length()));
        }
        for (String tag : addTags) {
          element.addTag(tag.substring(PLUS.length()));
        }
      }

      if (copy) {
        mediaPackage.addDerived(element, el);
        logger.info("Retagging mediapackage elements as a copy");
      } else {
        logger.info("Retagging mediapackage elements");
      }
    }

    return createResult(mediaPackage, Action.CONTINUE);
  }
}
