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

package org.opencastproject.archive.opencast;

import org.opencastproject.archive.api.ArchiveException;
import org.opencastproject.archive.base.AbstractArchiveScanner;
import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.security.api.Organization;
import org.opencastproject.util.Log;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.workflow.api.WorkflowService;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.quartz.CronExpression;
import org.quartz.JobExecutionContext;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.Dictionary;

public class TimedMediaArchiver extends AbstractArchiveScanner implements ManagedService {
  private static final Log logger = new Log(LoggerFactory.getLogger(TimedMediaArchiver.class));

  public static final String PARAM_KEY_STORE_TYPE = "store-type";
  public static final String PARAM_KEY_ARCHIVE_MAX_AGE = "max-age";
  public static final String JOB_NAME = "oc-archive-timed-storage-job";
  public static final String SCANNER_NAME = "Timed media archive offloader";
  public static final String TRIGGER_NAME = "oc-archive-timed-storage-trigger";

  private OpencastArchive archive;
  private OpencastArchiveJobProducer jp;
  private WorkflowService workflowService;
  private String storeType;
  private long ageModifier;

  public TimedMediaArchiver() {
    mkJob(Runner.class);
  }

  @Override
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    String cronExpression;
    boolean enabled;

    unschedule();

    if (properties != null) {
      logger.debug("Updating configuration...");

      enabled = BooleanUtils.toBoolean((String) properties.get(PARAM_KEY_ENABLED));
      setEnabled(enabled);
      logger.info("Timed media offload enabled: " + enabled);
      if (!isEnabled()) {
        return;
      }

      cronExpression = (String) properties.get(PARAM_KEY_CRON_EXPR);
      if (StringUtils.isBlank(cronExpression) || !CronExpression.isValidExpression(cronExpression)) {
        throw new ConfigurationException(PARAM_KEY_CRON_EXPR, "Cron expression must be valid");
      }
      setCronExpression(cronExpression);
      logger.debug("Timed media offload cron expression: '" + cronExpression + "'");

      storeType = (String) properties.get(PARAM_KEY_STORE_TYPE);
      if (StringUtils.isBlank(storeType)) {
        throw new ConfigurationException(PARAM_KEY_STORE_TYPE, "Store type is missing");
      }
      logger.debug("Remote media store type: " + storeType);

      try {
        ageModifier = Long.parseLong((String) properties.get(PARAM_KEY_ARCHIVE_MAX_AGE));
      } catch (NumberFormatException e) {
        throw new ConfigurationException(PARAM_KEY_ARCHIVE_MAX_AGE, "Invalid max age");
      }
      if (ageModifier < 0) {
        throw new ConfigurationException(PARAM_KEY_ARCHIVE_MAX_AGE, "Max age must be greater than zero");
      }
    }

    schedule();
  }

  @Override
  public void scan() {
    Date maxAge = Calendar.getInstance().getTime();
    maxAge.setTime(maxAge.getTime() - (ageModifier * CaptureParameters.HOURS * CaptureParameters.MILLISECONDS));
    if (!archive.getRemoteElementStoreIds().contains(storeType)) {
      throw new RuntimeException("Store type " + storeType + " is not available to the archive");
    }

    try {
      // Hardcoded date of zero.  Assumption: there is nothing with a date older than 0 which needs to be auto moved.
      jp.moveByDate(new Date(0), maxAge, storeType);
    } catch (ArchiveException e) {
      throw new RuntimeException("Unable to offload archive data", e);
    }
  }

  @Override
  public String getJobName() {
    return JOB_NAME;
  }

  @Override
  public String getTriggerName() {
    return TRIGGER_NAME;
  }

  @Override
  public String getScannerName() {
    return SCANNER_NAME;
  }

  public void setArchive(OpencastArchive ar) {
    this.archive = ar;
  }

  public void setJobproducer(OpencastArchiveJobProducer jp) {
    this.jp = jp;
  }

  /** Quartz job to which offloads old mediapackages from the archive to remote storage */
  public static class Runner extends AbstractRunner {

    public Runner() {
      super();
    }

    @Override
    protected void execute(final AbstractArchiveScanner parameters, JobExecutionContext ctx) {
      logger.debug("Starting " + parameters.getScannerName() + " job.");

      // iterate all organizations
      for (final Organization org : parameters.getOrganizationDirectoryService().getOrganizations()) {
        // set the organization on the current thread
        parameters.getAdminContextFor(org.getId()).runInContext(new Effect0() {
          @Override
          protected void run() {
            parameters.scan();
          }
        });
      }

      logger.debug("Finished " + parameters.getScannerName() + " job.");
    }
  }
}
