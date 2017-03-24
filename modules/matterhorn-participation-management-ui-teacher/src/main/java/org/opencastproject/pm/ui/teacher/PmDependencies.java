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

import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.VCell.iocell;

import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.data.Cell;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.VCell;
import org.opencastproject.workflow.api.WorkflowService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** OSGi component */
public class PmDependencies {
  private static final Logger logger = LoggerFactory.getLogger(PmDependencies.class);
  private final VCell<Option<ParticipationManagementDatabase>> pm = iocell(none(ParticipationManagementDatabase.class));
  private SecurityService security;
  private WorkflowService workflowService;
  private ArchiveServices archiveServices;

  /** OSGi DI */
  public void setParticipationManagementDatabase(ParticipationManagementDatabase pm) {
    this.pm.set(Option.some(pm));
  }

  /** OSGi DI */
  public void unsetParticipationManagementDatabase(ParticipationManagementDatabase pm) {
    this.pm.set(Option.<ParticipationManagementDatabase>none());
  }

  public Cell<Option<ParticipationManagementDatabase>> getParticipationManagementDatabase() {
    return pm;
  }

  /** OSGi DI */
  public void setSecurityService(SecurityService securityService) {
    this.security = securityService;
  }

  public SecurityService getSecurityService() {
    return security;
  }

  /** OSGi DI */
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  public WorkflowService getWorkflowService() {
    return workflowService;
  }

  /** OSGi DI */
  public void setArchiveServices(ArchiveServices archiveServices) {
    this.archiveServices = archiveServices;
  }

  public ArchiveServices getArchiveServices() {
    return archiveServices;
  }
}
