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

package org.opencastproject.pm.syllabus.impl.snapcount;

import static org.opencastproject.pm.api.Course.EmailStatus;
import static org.opencastproject.util.OsgiUtil.getCfg;
import static org.opencastproject.util.OsgiUtil.getOptCfgAsInt;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.VCell.ocell;

import org.opencastproject.kernel.mail.EmailAddress;
import org.opencastproject.messages.MessageSignature;
import org.opencastproject.messages.MessageTemplate;
import org.opencastproject.pm.api.EmailSender;
import org.opencastproject.pm.api.Message;
import org.opencastproject.pm.api.ParticipationManagementException;
import org.opencastproject.pm.api.Person;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabaseException;
import org.opencastproject.pm.api.scheduling.ScheduleFeederService;
import org.opencastproject.pm.api.scheduling.SnapCountService;
import org.opencastproject.pm.api.util.RequirementManager;
import org.opencastproject.pm.syllabus.api.SyllabusDataService;
import org.opencastproject.pm.syllabus.impl.scheduling.DassRequirementManager;
import org.opencastproject.pm.syllabus.impl.scheduling.HarvestStats;
import org.opencastproject.pm.syllabus.impl.scheduling.ParticipationFeederRunner;
import org.opencastproject.requirement.api.RequirementService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityContext;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.VCell;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;

public class SnapCountServiceImpl implements ManagedService, SnapCountService {

  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(SnapCountService.class);

  private SyllabusDataService syllabusDataService;
  private ParticipationManagementDatabase participationDatabase;
  private SecurityService securityService;
  private ScheduleFeederService scheduleFeederService;
  private OrganizationDirectoryService organizationDirectoryService;
  private String systemUser;
  private HashMap<String, String> snapCountStats = new HashMap<String,String>();
  private RequirementService requirementService;
  private RequirementManager requirementManager;
  private ParticipationFeederRunner pmRunner;
  private final VCell<Option<SecurityContext>> secCtx = ocell();
  private EmailSender emailSenderService;

  private boolean initialHarvest = false;
  private boolean running = false;

  /** Configuration keys */
  public static final String RECORDINGS_THRESHOLD_CHANGED_CONFIG = "recordings.threshold.changed";
  public static final String RECORDINGS_THRESHOLD_DELETED_CONFIG = "recordings.threshold.deleted";
  public static final String ERROR_EMAIL_FROM_NAME_CONFIG = "error.email.from.name";
  public static final String ERROR_EMAIL_FROM_ADDRESS_CONFIG = "error.email.from.address";
  public static final String ERROR_EMAIL_TO_ADDRESS_CONFIG = "error.email.to.address";

  /** Configuration defaults */
  public static final int RECORDINGS_THRESHOLD_CHANGED_DEFAULT = 200;
  public static final int RECORDINGS_THRESHOLD_DELETED_DEFAULT = 100;

  /** Configuration values */
  private int recordingsThresholdChanged = RECORDINGS_THRESHOLD_CHANGED_DEFAULT;
  private int recordingsThresholdDeleted = RECORDINGS_THRESHOLD_DELETED_DEFAULT;
  private Person emailCreator;
  private EmailAddress emailSender;
  private List<EmailAddress> errorRecipients = new ArrayList<EmailAddress>();


  /** OSGi container callback. */
  public void setSyllabusDataService(SyllabusDataService syllabusDataService) {
    this.syllabusDataService = syllabusDataService;
  }

  /** OSGi container callback. */
  public void setPersistence(ParticipationManagementDatabase persistence) {
    this.participationDatabase = persistence;
  }

  /** OSGi container callback. */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi container callback. */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectory) {
    this.organizationDirectoryService = organizationDirectory;
  }

  /** OSGi container callback. */
  public void setScheduleFeederService(ScheduleFeederService scheduleFeederService) {
    this.scheduleFeederService = scheduleFeederService;
  }

  /** OSGi DI */
  public void setEmailSenderService(EmailSender emailSenderService) {
    this.emailSenderService = emailSenderService;
  }

  /** OSGi DI */
  public void unsetEmailSenderService(EmailSender emailSenderService) {
    this.emailSenderService = null;
  }

  /** OSGi container callback. */
  public void unsetScheduleFeederService(ScheduleFeederService scheduleFeederService) {
    this.scheduleFeederService = null;
  }

  /** OSGi container callback. */
  public void setRequirementService(RequirementService requirementService) {
    this.requirementService = requirementService;
  }

  /** OSGi container callback. */
  public synchronized void activate(final ComponentContext cc) {
    logger.info("Starting 'Snapcount' participation management orchestrator service");
    systemUser = cc.getBundleContext().getProperty(SecurityUtil.PROPERTY_KEY_SYS_USER);
    requirementManager = new DassRequirementManager(requirementService, participationDatabase);
    pmRunner = new ParticipationFeederRunner(syllabusDataService, participationDatabase, requirementManager, secCtx);
  }

  /** OSGi container callback. */
  public synchronized void deactivate(ComponentContext cc) {
    logger.info("Stopping 'Snapcount' participation management orchestrator service");
  }

  @Override
  public void harvestSync() {
    logger.info("START: Participation management orchestrator synchronizations ####");
    final Organization org;

    running = true;
    org = securityService.getOrganization();
    secCtx.set(some(new SecurityContext(securityService, org, SecurityUtil.createSystemUser(systemUser, org))));

    try {
      initialHarvest = participationDatabase.getLastSynchronization() == null;
      pmRunner.setSnapCountService(this);
      pmRunner.trigger();
    } catch (ParticipationManagementDatabaseException ex) {
      logger.error("Database problem", ex);
    }
  }

  @Override
  public void verifyParticipationFeeder() {
    logger.info("START: Verify participation harvest ####################################");

    HarvestStats stats = pmRunner.getLastHarvestStats();
    snapCountStats.put("tab.dashboard.snap.stats.total", Long.toString(stats.getTotal()));
    snapCountStats.put("tab.dashboard.snap.stats.new", Integer.toString(stats.getNew()));
    snapCountStats.put("tab.dashboard.snap.stats.updated", Integer.toString(stats.getUpdated()));
    snapCountStats.put("tab.dashboard.snap.stats.unchanged", Integer.toString(stats.getNotModified()));
    snapCountStats.put("tab.dashboard.snap.stats.deleted", Integer.toString(stats.getDeleted()));
    logger.info("Syllabus+ sync stats:" + snapCountStats);

    if (!initialHarvest) {
      Boolean error = false;
      String body = "<div>There are large descrepencies between the current and previous Syllabus+ synchronizations to Participation Management databases."
          + "Please review and run the Participation Management to Opencast Schedule synchronization manually if required.</div>";

      if (stats.getUpdated() > recordingsThresholdChanged) {
        error = true;
        body += "<div>The number of recordings in the Participation Management database has changed by more than " + recordingsThresholdChanged + "</div>"
            + "<ul><li>Changed recordings: " + stats.getUpdated() + "</li>"
            + "<li>Total recordings: " + stats.getTotal() + "</li></ul>";
        logger.error("Syllabus+ to PM synchronization: too many were recording changed. Opencast Schedule synchronization not run");
      }

      if (stats.getDeleted() > recordingsThresholdDeleted) {
        error = true;
        body = "<div>The number of recordings <b>marked</b> as deleted in the Participation Management database is more than " + recordingsThresholdDeleted + "</div>"
            + "<ul><li>Deleted recordings: " + stats.getDeleted() + "</li>"
            + "<li>Total recordings: " + stats.getTotal() + "</li></ul>";
        logger.error("Syllabus+ to PM synchronization: too many were recordings marked deleted. Opencast Schedule synchronization not run");
      }

      if (error)  {
        User user = SecurityUtil.createSystemUser(systemUser, new DefaultOrganization());
        MessageSignature msgSign = MessageSignature.messageSignature("admin", user, emailSender, "Sent by admin");
        MessageTemplate tmpl = new MessageTemplate("synchronization warning", user,
            "ATTENTION: Syllabus+ synchronization descrepencies", body);
        Message errorMessage = new Message(emailCreator, tmpl, msgSign);
        emailSenderService.sendErrorMessage(errorMessage, errorRecipients);

        running = false;
        return;
      }
    }

    logger.info("END: Verify participation harvest ####################################");

    // Synchronize PM to OC schedule
    scheduleFeederService.synchronize();
  }

  @Override
  public void sendOptOutEmails() {
    try {
      emailSenderService.sendMessages(systemUser, EmailStatus.UNSENT, true);
    } catch (ParticipationManagementException e) {
      logger.error(e.getMessage());
    } finally {
      // this is the end of the snapchat process
      logger.info("END: Participation management orchestrator synchronizations ######");
      running = false;
    }
  }

  /** OSGi container called (ConfigurationAdmin). */
  @Override
  public synchronized void updated(Dictionary properties) throws ConfigurationException {
    if (properties != null) {
      // thresholds
      recordingsThresholdChanged = getOptCfgAsInt(properties, RECORDINGS_THRESHOLD_CHANGED_CONFIG).getOrElse(RECORDINGS_THRESHOLD_CHANGED_DEFAULT);
      recordingsThresholdDeleted = getOptCfgAsInt(properties, RECORDINGS_THRESHOLD_DELETED_CONFIG).getOrElse(RECORDINGS_THRESHOLD_DELETED_DEFAULT);
      // email from
      String sender = getCfg(properties, ERROR_EMAIL_FROM_NAME_CONFIG);
      String email = getCfg(properties, ERROR_EMAIL_FROM_ADDRESS_CONFIG);
      emailCreator = Person.person(sender, email);
      emailSender = EmailAddress.emailAddress(email, sender);
      // email to
      email = getCfg(properties, ERROR_EMAIL_TO_ADDRESS_CONFIG);
      errorRecipients.clear();
      errorRecipients.add(new EmailAddress(email, ""));
    }
  }

  @Override
  public HashMap<String, String> getStats() {
    return snapCountStats;
  }

  @Override
  public boolean isRunning() {
    return running;
  }

}
