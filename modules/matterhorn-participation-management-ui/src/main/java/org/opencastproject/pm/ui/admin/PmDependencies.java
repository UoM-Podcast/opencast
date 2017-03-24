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

package org.opencastproject.pm.ui.admin;

import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.VCell.iocell;

import org.opencastproject.pm.api.EmailSender;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.pm.api.scheduling.ParticipationFeederService;
import org.opencastproject.pm.api.scheduling.ScheduleFeederService;
import org.opencastproject.pm.api.scheduling.SnapCountService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.data.Cell;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.VCell;

import org.osgi.service.component.ComponentContext;

/** OSGi component */
public class PmDependencies {
  private final VCell<Option<ParticipationManagementDatabase>> pm = iocell(none(ParticipationManagementDatabase.class));
  private final VCell<Option<ParticipationFeederService>> participationFeederService = iocell(none(ParticipationFeederService.class));
  private final VCell<Option<SnapCountService>> snapCountService = iocell(none(SnapCountService.class));
  private final VCell<Option<ScheduleFeederService>> scheduleFeederService = iocell(none(ScheduleFeederService.class));
  private final VCell<Option<EmailSender>> emailSenderService = iocell(none(EmailSender.class));
  private final VCell<Option<SecurityService>> securityService = iocell(none(SecurityService.class));
  private String systemUserName;

  public void activate(ComponentContext cc) {
    systemUserName = cc.getBundleContext().getProperty(SecurityUtil.PROPERTY_KEY_SYS_USER);
  }

  /** OSGi DI */
  public void setParticipationManagementDatabase(ParticipationManagementDatabase pm) {
    this.pm.set(Option.some(pm));
  }

  /** OSGi DI */
  public void unsetParticipationManagementDatabase(ParticipationManagementDatabase pm) {
    this.pm.set(Option.<ParticipationManagementDatabase> none());
  }

  public Cell<Option<ParticipationManagementDatabase>> getParticipationManagementDatabase() {
    return pm;
  }

  /** OSGi DI */
  public void setParticipationFeederService(ParticipationFeederService participationFeederService) {
    this.participationFeederService.set(Option.some(participationFeederService));
  }

  /** OSGi DI */
  public void unsetParticipationFeederService(ParticipationFeederService participationFeederService) {
    this.participationFeederService.set(Option.<ParticipationFeederService> none());
  }

  public Cell<Option<ParticipationFeederService>> getParticipationFeederService() {
    return participationFeederService;
  }

  /** OSGi DI */
  public void setSnapCountService(SnapCountService snapCountService) {
    this.snapCountService.set(Option.some(snapCountService));
  }

  /** OSGi DI */
  public void unsetSnapCountService(SnapCountService snapCountService) {
    this.snapCountService.set(Option.<SnapCountService> none());
  }

  public Cell<Option<SnapCountService>> getSnapCountService() {
    return snapCountService;
  }

  /** OSGi DI */
  public void setScheduleFeederService(ScheduleFeederService scheduleFeederService) {
    this.scheduleFeederService.set(Option.some(scheduleFeederService));
  }

  /** OSGi DI */
  public void unsetScheduleFeederService(ScheduleFeederService scheduleFeederService) {
    this.scheduleFeederService.set(Option.<ScheduleFeederService> none());
  }

  public Cell<Option<ScheduleFeederService>> getScheduleFeederService() {
    return scheduleFeederService;
  }

  /** OSGi DI */
  public void setEmailSenderService(EmailSender emailSenderService) {
    this.emailSenderService.set(Option.some(emailSenderService));
  }

  /** OSGi DI */
  public void unsetEmailSenderService(EmailSender emailSenderService) {
    this.emailSenderService.set(Option.<EmailSender> none());
  }

  public Cell<Option<EmailSender>> getEmailSenderService() {
    return emailSenderService;
  }

  public String getSystemUserName() {
    return systemUserName;
  }

  /** OSGi DI */
  public void setSecurityService(SecurityService securityService) {
    this.securityService.set(Option.some(securityService));
  }

  /** OSGi DI */
  public void unsetSecurityService(SecurityService securityService) {
    this.securityService.set(Option.<SecurityService>none());
  }

  public Cell<Option<SecurityService>> getSecurityService() {
    return securityService;
  }
}
