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
import org.opencastproject.kernel.mail.SmtpService;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * WOH to notify staff about an awaiting trimming step
 */
public class StaffNotificationWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  private static final String CONFIG_OPTION_TO = "to";
  private static final String CONFIG_OPTION_CC = "cc";
  private static final String CONFIG_OPTION_BCC = "bcc";
  private static final String CONFIG_OPTION_FROM = "from";
  private static final String CONFIG_OPTION_SUBJECT = "subject";
  private static final String CONFIG_OPTION_BODY = "body";

  private static final String ADMIN_URL_PROPERTY = "org.opencastproject.admin.ui.url";
  private static final String ENGAGE_URL_PROPERTY = "org.opencastproject.engage.ui.url";
  private static final String LINE_SEPERATOR = "\r\n";

  /** Reference to a SMTP service */
  private SmtpService smtpService;

  /** Reference to the security service */
  private SecurityService securityService;

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(StaffNotificationWorkflowOperationHandler.class);

  public StaffNotificationWorkflowOperationHandler() {
    addConfigurationOption(CONFIG_OPTION_TO, "Mail 'to' field");
    addConfigurationOption(CONFIG_OPTION_CC, "Mail 'cc' field");
    addConfigurationOption(CONFIG_OPTION_BCC, "Mail 'bcc' field");
    addConfigurationOption(CONFIG_OPTION_FROM, "Mail 'from' field");
    addConfigurationOption(CONFIG_OPTION_SUBJECT, "Mail 'subject' field");
    addConfigurationOption(CONFIG_OPTION_BODY, "Mail body");
  }

  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {

    // The current workflow operation instance
    WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();

    logger.debug("Start notifying staff about workflow instance '{}'", operation.getId());

    // MediaPackage from previous workflow operations
    MediaPackage mp = workflowInstance.getMediaPackage();

    // Lookup the name of the to, from, and subject
    String to = operation.getConfiguration(CONFIG_OPTION_TO);
    String cc = operation.getConfiguration(CONFIG_OPTION_CC);
    String bcc = operation.getConfiguration(CONFIG_OPTION_BCC);
    String from = operation.getConfiguration(CONFIG_OPTION_FROM);
    String subject = operation.getConfiguration(CONFIG_OPTION_SUBJECT);
    String body = operation.getConfiguration(CONFIG_OPTION_BODY);

    String adminHostname = null;
    String engageHostname = null;

    try {
      adminHostname = securityService.getOrganization().getProperties().get(ADMIN_URL_PROPERTY);
      if (adminHostname == null)
        adminHostname = "http://localhost";

      engageHostname = securityService.getOrganization().getProperties().get(ENGAGE_URL_PROPERTY);
      if (engageHostname == null)
        engageHostname = "http://localhost";
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }

    if (StringUtils.isBlank(body)) {
      // use static fallback
      String url = UrlSupport.concat(adminHostname, "/admin/trim.html?id=").concat(String.valueOf(workflowInstance.getId()));

      StringBuilder bodyBldr = new StringBuilder();
      bodyBldr.append("Your podcast ");
      bodyBldr.append(mp.getTitle());
      bodyBldr.append(" is now ready for editing and/or review before release to students. Instructions for using the editing / review system can be found at the following address[1].");
      bodyBldr.append(LINE_SEPERATOR);
      bodyBldr.append(LINE_SEPERATOR);
      bodyBldr.append("Please click on the following link:").append(LINE_SEPERATOR);
      bodyBldr.append(url).append(LINE_SEPERATOR);
      bodyBldr.append(LINE_SEPERATOR);
      bodyBldr.append("login as:").append(LINE_SEPERATOR);
      bodyBldr.append("username: editor").append(LINE_SEPERATOR);
      bodyBldr.append("password: ScfHdREd").append(LINE_SEPERATOR);
      bodyBldr.append(LINE_SEPERATOR);
      bodyBldr.append("[1] http://servicedesk.manchester.ac.uk/portal/app/portlets/results/viewsolution.jsp?solutionid=041601104380179");
      bodyBldr.append(LINE_SEPERATOR);
      bodyBldr.append(LINE_SEPERATOR);
      bodyBldr.append("If you are editing / reviewing from home or are not on the University network you will need to use the VPN service to access the interface. ");
      bodyBldr.append(LINE_SEPERATOR);
      bodyBldr.append("http://www.itservices.manchester.ac.uk/vpn/");
      body = bodyBldr.toString();
    } else {
      // do some simple string replacement
      body = body.replace("[[ mediapackage_title ]]", mp.getTitle());
      body = body.replace("[[ mediapackage_id ]]", mp.getIdentifier().toString());
      body = body.replace("[[ series_title ]]", StringUtils.isNotEmpty(mp.getSeriesTitle()) ? mp.getSeriesTitle() : "");
      body = body.replace("[[ series_id ]]", StringUtils.isNotEmpty(mp.getSeries()) ? mp.getSeries() : "");
      body = body.replace("[[ workflow_id ]]", String.valueOf(workflowInstance.getId()));
      body = body.replace("[[ admin_hostname ]]", adminHostname);
      body = body.replace("[[ engage_hostname ]]", engageHostname);
    }

    // Create the mail message
    MimeMessage message = smtpService.createMessage();

    try {
      if (StringUtils.isNotBlank(to))
        message.addRecipients(RecipientType.TO, to);
      if (StringUtils.isNotBlank(cc))
        message.addRecipients(RecipientType.CC, cc);
      if (StringUtils.isNotBlank(bcc))
        message.addRecipients(RecipientType.BCC, bcc);
      if (StringUtils.isNotBlank(subject))
        message.setSubject(subject);
      if (StringUtils.isNotBlank(from)) {
        try {
          message.setFrom(new InternetAddress(from, "Podcast Service"));
        } catch (UnsupportedEncodingException e) {
          throw new WorkflowOperationException(e);
        }
      }
      message.setText(body);

      message.saveChanges();

      smtpService.send(message);
      logger.info("E-mail notification successfully sent (to: {} | cc: {} | bcc: {})", new String[] { to, cc, bcc });

    } catch (MessagingException e) {
      throw new WorkflowOperationException(e);
    }

    return createResult(mp, Action.CONTINUE);
  }

  /**
   * OSGi callback to set smtp service reference
   */
  protected void setSmtpService(SmtpService smtpService) {
    this.smtpService = smtpService;
  }

  /** OSGi callback to set security service */
  protected void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

}
