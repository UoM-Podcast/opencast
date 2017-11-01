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

import static org.apache.commons.lang3.StringUtils.isBlank;

import org.opencastproject.job.api.JobContext;

import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.metadata.dublincore.DublinCoreValue;

import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;

import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The workflow definition for handling "episode ACL" operations
 */
public class AddEpisodeAclWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /**
   * The logging facility
   */
  private static final Logger logger = LoggerFactory.getLogger(AddEpisodeAclWorkflowOperationHandler.class);

  /**
   * The configuration options for this handler
   */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  /**
   * Name of the configuration option that provides the list of roles
   */
  public static final String LIST_OF_ROLES = "roles";

  /**
   * Role action
   */
  private static final String ROLE_ACTION = "read";

  /**
   * Role 'allow' property
   */
  private static final boolean ROLE_ALLOW = true;

  /**
   * The authorization service
   */
  private AuthorizationService authorizationService;

  /**
   * The workspace
   */
  private Workspace workspace;

  protected void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param workspace the workspace
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put(LIST_OF_ROLES, "List of roles for the episode ACL");
  }

  /**
   * {@inheritDoc}
   *
   * @see
   * org.opencastproject.workflow.api.WorkflowOperationHandler#getConfigurationOptions()
   */
  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return CONFIG_OPTIONS;
  }

  /**
   * {@inheritDoc}
   *
   * @see
   * org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
   * JobContext)
   */
  @Override
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.debug("Running series workflow operation");

    MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    WorkflowOperationInstance currentOperation = workflowInstance.getCurrentOperation();

    String optRoles = StringUtils.trimToEmpty(currentOperation.getConfiguration(LIST_OF_ROLES));

    List<String> roleListFromEpisode = new ArrayList<>();

    try {
      roleListFromEpisode = getRolesFromEpisode(mediaPackage);
    } catch (Exception e) {
      logger.error("Unable to get list of ACL from  Episode : {}", ExceptionUtils.getStackTrace(e));
      throw new WorkflowOperationException(e);
    }

    if (isBlank(optRoles) && roleListFromEpisode.isEmpty()) {
      logger.info("No Episode attachment ACL to add, skip operation");
      return createResult(mediaPackage, Action.SKIP);
    }

    List<String> roleListFromConfiguration = asList(optRoles);

    List<AccessControlEntry> entries = new ArrayList<>();

    // Add ACLcontrolList based on given configuration values
    if (!roleListFromConfiguration.isEmpty()) {
      for (String role : roleListFromConfiguration) {
        AccessControlEntry access = new AccessControlEntry(role, ROLE_ACTION, ROLE_ALLOW);
        entries.add(access);
      }
    }

    // Add ACLcontrolList based on episode
    if (!roleListFromEpisode.isEmpty()) {
      for (String role : roleListFromEpisode) {
        AccessControlEntry access = new AccessControlEntry(role, ROLE_ACTION, ROLE_ALLOW);
        entries.add(access);
      }
    }

    //Add list of ACLs to mediapackage
    AccessControlList acl = new AccessControlList(entries);
    try {
      authorizationService.setAcl(mediaPackage, AclScope.Episode, acl);
      logger.info("Episode ACLs added to mediapackage");
    } catch (Exception e) {
      logger.error("Unable to add Episode ACL: {}", ExceptionUtils.getStackTrace(e));
      throw new WorkflowOperationException(e);
    }

    return createResult(mediaPackage, Action.CONTINUE);
  }

  private List<String> getRolesFromEpisode(MediaPackage mediaPackage) throws Exception {
    List<String> aces = new ArrayList<>();
    for (Catalog episodeCatalog : mediaPackage.getCatalogs(MediaPackageElements.EPISODE)) {
      DublinCoreCatalog episodeDublinCore = DublinCoreUtil.loadDublinCore(workspace, episodeCatalog);
      List<DublinCoreValue> audience = episodeDublinCore.get(DublinCore.PROPERTY_AUDIENCE);
      if (!audience.isEmpty()) {
        for (DublinCoreValue aud : audience) {
          aces.add(aud.getValue());
        }
      }
    }
    return aces;
  }
}
