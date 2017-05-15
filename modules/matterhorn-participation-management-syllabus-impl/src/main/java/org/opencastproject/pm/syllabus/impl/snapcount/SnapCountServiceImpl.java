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

import static org.opencastproject.kernel.mail.EmailAddress.emailAddress;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.VCell.ocell;

import org.opencastproject.messages.MessageSignature;
import org.opencastproject.messages.MessageTemplate;
import org.opencastproject.pm.api.EmailSender;
import org.opencastproject.pm.api.Message;
import org.opencastproject.pm.api.Person;
import org.opencastproject.pm.api.Synchronization;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabaseException;
import org.opencastproject.pm.api.scheduling.ScheduleFeederService;
import org.opencastproject.pm.api.scheduling.SnapCountService;
import org.opencastproject.pm.api.util.RequirementManager;
import org.opencastproject.pm.syllabus.api.SyllabusService;
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

import java.util.Dictionary;
import java.util.HashMap;

public class SnapCountServiceImpl implements ManagedService, SnapCountService {

  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(SnapCountService.class);

  private SyllabusService syllabusService;
  private ParticipationManagementDatabase participationDatabase;
  private SecurityService securityService;
  private ScheduleFeederService matterhornSyncService;
  private OrganizationDirectoryService organizationDirectoryService;
  private String systemUser;
  private HashMap<String, String> snapCountStats = new HashMap<String,String>();
  private RequirementService requirementService = null;
  private RequirementManager requirementManager = null;
  private ParticipationFeederRunner pmRunner;
  private final VCell<Option<SecurityContext>> secCtx = ocell();
  private EmailSender emailSenderService;

 /** OSGi container callback. */
  public void setSyllabusService(SyllabusService syllabusService) {
    this.syllabusService = syllabusService;
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
    this.matterhornSyncService = scheduleFeederService;
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
    this.matterhornSyncService = null;
  }

  /** OSGi container callback. */
  public void setRequirementService(RequirementService requirementService) {
    this.requirementService = requirementService;
    if (requirementService != null) {
      requirementManager = new DassRequirementManager(requirementService, participationDatabase);
    }
    pmRunner = new ParticipationFeederRunner(syllabusService, participationDatabase, requirementManager, secCtx);
  }

  /** OSGi container callback. */
  public void unsetRequirementService(RequirementService requirementService) {
    this.requirementService = null;
    this.requirementManager = null;
    pmRunner = new ParticipationFeederRunner(syllabusService, participationDatabase, requirementManager, secCtx);
  }

  /** OSGi container callback. */
  public synchronized void activate(final ComponentContext cc) {
    logger.info("Start participation management orchestrator");
    systemUser = cc.getBundleContext().getProperty(SecurityUtil.PROPERTY_KEY_SYS_USER);
    if (requirementService != null) {
      requirementManager = new DassRequirementManager(requirementService, participationDatabase);
    }
    pmRunner = new ParticipationFeederRunner(syllabusService, participationDatabase, requirementManager, secCtx);
  }

  /** OSGi container callback. */
  public synchronized void deactivate(ComponentContext cc) {
    logger.info("Stop participation management orchestrator");
  //  runner.shutdown();
  }

  protected void waitForSync(final Synchronization sync) {
//    SecurityContext securityContext = secCtx.get().get();
//    securityContext.runInContext(new Effect0() {
//      @Override
//      public void run() {
        while (sync != null && (pmRunner.isRunning())) {
          try {
            logger.info("in sync wait");
            Thread.sleep(0);
          } catch (InterruptedException ignore) {
          }
        }
//      }
//    });
  }

  @Override
  public void harvestSync() {
    logger.info("Participation management orchestrator update");
    final Organization org;

    org = securityService.getOrganization();
    secCtx.set(some(new SecurityContext(securityService, org, SecurityUtil.createSystemUser(systemUser, org))));

    try {
      final Synchronization sync = participationDatabase.getLastSynchronization();
      if (null == sync) {
        logger.info("Start initial harvest");

        // "Can't trigger inital harvest externally, try harvesting through UI!"
      }
      logger.info("Start S+ Sync");
      pmRunner.setSnapCountService(this);
      pmRunner.trigger();
    } catch (ParticipationManagementDatabaseException ex) {
      logger.error("DB Problem");
    }
  }

  @Override
  public void verifyParticipationFeeder() {
      User user = SecurityUtil.createSystemUser(systemUser, new DefaultOrganization());

    Person creator = Person.person("Lecture Capture Service", "lecture-capture@university.org");
    MessageSignature msgSign = MessageSignature.messageSignature("admin", user,
      emailAddress(creator.getEmail(), creator.getName()), "Send by admin");

    final Organization org;

    org = organizationDirectoryService.getOrganizations().get(0);
    secCtx.set(some(new SecurityContext(securityService, org, SecurityUtil.createSystemUser(systemUser, org))));
    try {

      logger.info("Verify S+ Sync");
      HarvestStats stats = pmRunner.getLastHarvestStats();

      long storedTotalRecordings = participationDatabase.countTotalRecordings();
      snapCountStats.put("tab.dashboard.snap.stats.total", Long.toString(stats.getTotal()));
      snapCountStats.put("tab.dashboard.snap.stats.new", Integer.toString(stats.getNew()));
      snapCountStats.put("tab.dashboard.snap.stats.updated", Integer.toString(stats.getUpdated()));
      snapCountStats.put("tab.dashboard.snap.stats.unchanged", Integer.toString(stats.getNotModified()));
      snapCountStats.put("tab.dashboard.snap.stats.deleted", Integer.toString(stats.getDeleted()));
      logger.info("stats of S+ sync :" + snapCountStats);
      int totalSplusRecordings = stats.getTotal();
      if ((storedTotalRecordings - totalSplusRecordings) > 100) {
        String body = " The  number of recordings in the S+ database is significantly \n "
                + "smaller that the number of recordings in the Matterhorn Database:\n\n"
                + "stored total recordings: " + storedTotalRecordings + "\n\n" //471
                + "total S+ Recordings: " + totalSplusRecordings + "\n\n" //475
                + "Please run MH update manually!";
        logger.error("Error Syncing Syllabus - not enough entries in S+ DB");
        logger.error(body);
        MessageTemplate tmpl = new MessageTemplate("invitation", user,
            "Error Syncing Syllabus - not enough entries in S+ DB",
            body);
        Message errorMessage = new Message(creator, tmpl, msgSign);
//        emailSenderService.sendErrorMessage(errorMessage);
        return;
      }
      if (stats.getDeleted() > 200) {
        // send email and exit
        // respb.type("Trying to delete too many Recordings -- please verify");
        String body = "The Synchronization with S+ attempts to delete a large number \n "
                + "of recordings in the Matterhorn Database:\n\n"
                + "stored total recordings: " + storedTotalRecordings + "\n\n"
                + "total S+ Recordings: " + totalSplusRecordings + "\n\n"
                + "Please run MH update manually!";
        logger.error("Error Syncing Syllabus - Trying to delete too many Recordings");
        logger.error(body);
      }
      logger.info("Start Matterhorn Scheduler Sync");
      matterhornSyncService.synchronize();
    } catch (ParticipationManagementDatabaseException ex) {
      logger.error("DB Problem");
    }

    }



  /** OSGi container called (ConfigurationAdmin). */
  @Override
  public synchronized void updated(Dictionary properties) throws ConfigurationException {
  }

  @Override
  public HashMap<String, String> getStats() {
    return snapCountStats;
  }



}
