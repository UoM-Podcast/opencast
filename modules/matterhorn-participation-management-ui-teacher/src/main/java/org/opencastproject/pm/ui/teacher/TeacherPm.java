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

import static org.opencastproject.util.OsgiUtil.getCfg;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.VCell.cell;
import static org.opencastproject.util.data.VCell.iocell;

import org.opencastproject.event.comment.EventCommentService;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.data.Cell;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.VCell;
import org.opencastproject.workflow.api.WorkflowService;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* OSGi component */
public class TeacherPm implements ManagedService {
  private static final String DEFAULT_CA_INPUT = "screen";
  private static final String DEFAULT_WORKFLOW_EDIT = "ng-schedule-upload";
  private static final String DEFAULT_WORKFLOW_RETRACT = "ng-retract";
  private static final String DEFAULT_WORKFLOW_EMAIL_PROPERTY = "emailAddresses";
  private static final Map<String, String>DEFAULT_CA_INPUT_DESCRIPTION = new HashMap<>();
  {
    DEFAULT_CA_INPUT_DESCRIPTION.put(DEFAULT_CA_INPUT, "Screen only");
  }

  private static final Logger logger = LoggerFactory.getLogger(TeacherPm.class);
  private final VCell<Option<ParticipationManagementDatabase>> pm = iocell(none(ParticipationManagementDatabase.class));
  private SchedulerService schedulerService;
  private SecurityService security;
  private WorkflowService workflowService;
  private ArchiveServiceUtils archiveServiceUtils;
  private EventCommentService eventCommentService;
  private String systemUserName;

  private final VCell<List<String>> captureAgentInputs = cell(Arrays.asList(DEFAULT_CA_INPUT));
  private final VCell<Map<String, String>>captureAgentInputDescriptions = cell(DEFAULT_CA_INPUT_DESCRIPTION);
  private final VCell<String> workflowEdit = cell(DEFAULT_WORKFLOW_EDIT);
  private final VCell<String> workflowRetract = cell(DEFAULT_WORKFLOW_RETRACT);
  private final VCell<HashMap<String, String>> workflowEditConfig = cell(new HashMap<String, String>());
  private final VCell<String> emailProperty = cell(DEFAULT_WORKFLOW_EMAIL_PROPERTY);
  private final VCell<String> editServer = cell("http://localhost:8080");

  public void activate(ComponentContext cc) {
    systemUserName = cc.getBundleContext().getProperty(SecurityUtil.PROPERTY_KEY_SYS_USER);
  }

  /* OSGi DI */
  public void setParticipationManagementDatabase(ParticipationManagementDatabase pm) {
    this.pm.set(Option.some(pm));
  }

  /* OSGi DI */
  public void unsetParticipationManagementDatabase(ParticipationManagementDatabase pm) {
    this.pm.set(Option.<ParticipationManagementDatabase>none());
  }

  /* OSGi container callback */
  public void setSchedulerService(SchedulerService schedulerService) {
    this.schedulerService = schedulerService;
  }

  public SchedulerService getSchedulerService() {
    return schedulerService;
  }

  public Cell<Option<ParticipationManagementDatabase>> getParticipationManagementDatabase() {
    return pm;
  }

  /* OSGi DI */
  public void setSecurityService(SecurityService securityService) {
    this.security = securityService;
  }

  public SecurityService getSecurityService() {
    return security;
  }

  /* OSGi DI */
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  public WorkflowService getWorkflowService() {
    return workflowService;
  }

  public String getSystemUserName() {
    return systemUserName;
  }

  /* OSGi DI */
  public void setArchiveServiceUtils(ArchiveServiceUtils archiveServiceUtils) {
    this.archiveServiceUtils = archiveServiceUtils;
  }

  public ArchiveServiceUtils getArchiveServiceUtils() {
    return archiveServiceUtils;
  }

  /* OSGi DI */
  public void setEventCommentService(EventCommentService eventCommentService) {
    this.eventCommentService = eventCommentService;
  }

  public EventCommentService getEventCommentService() {
    return eventCommentService;
  }

  @Override
  public void updated(Dictionary properties) throws ConfigurationException {
    if (properties == null) {
      return;
    }
    logger.info("reading properties");
    final String caInputs = getCfg(properties, "capture.room.__ANY__.inputs");
    captureAgentInputs.set(Arrays.asList(caInputs.split("\\|")));
    final Map<String, String>caInputDescrips = new HashMap<>();
    for (String input : captureAgentInputs.get()) {
      caInputDescrips.put("options.record.input." + input, getCfg(properties, "ui.input." + input));
    }
    captureAgentInputDescriptions.set(caInputDescrips);
    final String wfEdit = getCfg(properties, "workflow.edit.definition");
    final String wfRetract = getCfg(properties, "workflow.retract.definition");
    final HashMap<String, String> wfCfg = new HashMap<>(getWfCfgAsMap(properties, "workflow.edit.config"));
    final String emailProp = getCfg(properties, "workflow.property.email");
    final String editSrv = getCfg(properties, "edit.server");

    workflowEdit.set(wfEdit);
    workflowRetract.set(wfRetract);
    workflowEditConfig.set(wfCfg);
    emailProperty.set(emailProp);
    editServer.set(editSrv);
  }

  public static Map<String, String> getWfCfgAsMap(Dictionary<String, String> d, String key)
          throws ConfigurationException {
    HashMap<String, String> config = new HashMap<>();
    for (Enumeration<String> e = d.keys(); e.hasMoreElements();) {
      String dKey = e.nextElement();
      if (dKey.startsWith(key))
        config.put(dKey.substring(key.length() + 1), d.get(dKey));
    }
    return config;
  }

  public VCell<List<String>> getCaptureAgentInputs() {
    return captureAgentInputs;
  }

  public VCell<Map<String, String>> getCaptureAgentInputDescriptions() {
    return captureAgentInputDescriptions;
  }

  public VCell<String> getWorkflowEdit() {
    return workflowEdit;
  }

  public VCell<String> getWorkflowRetract() {
    return workflowRetract;
  }

  public VCell<HashMap<String, String>> getWorkflowEditConfig() {
    return workflowEditConfig;
  }

  public VCell<String> getEmailProperty() {
    return emailProperty;
  }

  public VCell<String> getEditServer() {
    return editServer;
  }
}
