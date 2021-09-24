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

import com.entwinemedia.fn.Stream;
import static com.entwinemedia.fn.Stream.$;
import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.fns.Strings;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import static org.opencastproject.workflow.handler.workflow.ExportWorkflowPropertiesWOH.DEFAULT_TARGET_FLAVOR;
import static org.opencastproject.workflow.handler.workflow.ExportWorkflowPropertiesWOH.KEYS_PROPERTY;
import static org.opencastproject.workflow.handler.workflow.ExportWorkflowPropertiesWOH.TARGET_FLAVOR_PROPERTY;
import static org.opencastproject.workflow.handler.workflow.ExportWorkflowPropertiesWOH.TARGET_TAGS_PROPERTY;
import static org.opencastproject.workflow.handler.workflow.ImportWorkflowPropertiesWOH.loadPropertiesElementFromMediaPackage;
import static org.opencastproject.workflow.handler.workflow.ImportWorkflowPropertiesWOH.loadPropertiesFromXml;

import org.opencastproject.archive.api.Archive;
import org.opencastproject.archive.api.ArchiveException;
import org.opencastproject.archive.api.HttpMediaPackageElementProvider;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.ConfiguredWorkflow;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workspace.api.Workspace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * This WOH starts a new workflow for given media package.
 */
public class StartWorkflowWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
  private static final Logger logger = LoggerFactory.getLogger(StartWorkflowWorkflowOperationHandler.class);

  /** Name of the configuration option that provides the media package ID */
  public static final String MEDIA_PACKAGE_ID = "media-package";

  /** Name of the configuration option that provides the workflow definition ID */
  public static final String WORKFLOW_DEFINITION = "workflow-definition";

  /** The archive service */
  private Archive<?> archiveService = null;

  private HttpMediaPackageElementProvider mpElementProvider;

  private WorkflowService workflowService;
  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param archiveService
   *          an instance of the archive service
   */
  public void setArchiveService(Archive<?> archiveService) {
    this.archiveService = archiveService;
  }

  /** OSGi DI */
  void setHttpMediaPackageElementProvider(HttpMediaPackageElementProvider mpElementProvider) {
    this.mpElementProvider = mpElementProvider;
  }

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param workflowService
   *          the workflow service
   */
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  /** The workspace */
  private Workspace workspace;

  /** OSGi DI */
  void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {

    final MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    final Set<String> keys = $(getOptConfig(workflowInstance, KEYS_PROPERTY)).bind(Strings.splitCsv).toSet();
    final String targetFlavorString = getOptConfig(workflowInstance, TARGET_FLAVOR_PROPERTY).or(DEFAULT_TARGET_FLAVOR);
    final Stream<String> targetTags = $(getOptConfig(workflowInstance, TARGET_TAGS_PROPERTY)).bind(Strings.splitCsv);
    final MediaPackageElementFlavor targetFlavor = MediaPackageElementFlavor.parseFlavor(targetFlavorString);

    final WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();
    final String configuredMediaPackageID = trimToEmpty(operation.getConfiguration(MEDIA_PACKAGE_ID));
    final String configuredWorkflowDefinition = trimToEmpty(operation.getConfiguration(WORKFLOW_DEFINITION));
    final ArrayList<String> mps = new ArrayList<>();
    mps.add(configuredMediaPackageID);
    // Get workflow parameter
    final Map<String, String> properties = new HashMap<>();
    for (String key : operation.getConfigurationKeys()) {
      if (MEDIA_PACKAGE_ID.equals(key) || WORKFLOW_DEFINITION.equals(key)) {
        continue;
      }
      properties.put(key, operation.getConfiguration(key));
    }

    Properties workflowProps = new Properties();
    Opt<Attachment> existingPropsElem = loadPropertiesElementFromMediaPackage(targetFlavor, workflowInstance);
    if (existingPropsElem.isSome()) {
      workflowProps = loadPropertiesFromXml(workspace, existingPropsElem.get().getURI());
      // Remove specified keys
      for (String key : keys)
        workflowProps.remove(key);
    }

    try {
      // Get workflow definition
      final WorkflowDefinition workflowDefinition = workflowService.getWorkflowDefinitionById(
              configuredWorkflowDefinition);

      // Start workflow
      logger.info("Starting '{}' workflow for media package '{}'", configuredWorkflowDefinition,
              configuredMediaPackageID);
      archiveService.applyWorkflow(ConfiguredWorkflow.workflow(workflowDefinition),
            mpElementProvider.getUriRewriter(), mps);
    } catch (ArchiveException e) {
        logger.warn("Unable to start workflow '{}' on archived media package '{}': {}",
            configuredWorkflowDefinition, configuredMediaPackageID, e.getStackTrace());
    } catch (NotFoundException e) {
      throw new WorkflowOperationException(format("Workflow Definition '%s' not found", configuredWorkflowDefinition));
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }

    return createResult(WorkflowOperationResult.Action.CONTINUE);
  }
}
