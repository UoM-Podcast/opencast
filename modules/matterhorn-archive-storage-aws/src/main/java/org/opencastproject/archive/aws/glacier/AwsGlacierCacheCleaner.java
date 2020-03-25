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

package org.opencastproject.archive.aws.glacier;

import org.opencastproject.archive.base.AbstractArchiveScanner;
import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.util.Log;
import org.opencastproject.workflow.api.WorkflowService;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.quartz.CronExpression;
import org.quartz.JobExecutionContext;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.Dictionary;

public class AwsGlacierCacheCleaner extends AbstractArchiveScanner {
  private static final Log logger = new Log(LoggerFactory.getLogger(AwsGlacierCacheCleaner.class));

  public static final String PARAM_KEY_MAX_AGE = "max-age";
  public static final String JOB_NAME = "oc-archive-glacier-cache-cleaner-job";
  public static final String SCANNER_NAME = "Glacier cache cleaner";
  public static final String TRIGGER_NAME = "oc-archive-glacier-cache-cleaner-trigger";

  private AwsGlacierAssetStore assetStore;
  private WorkflowService workflowService;
  private long ageModifier;

  public AwsGlacierCacheCleaner() {
    mkJob(Runner.class);
  }

  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    String cronExpression;
    boolean enabled;

    unschedule();

    if (properties != null) {
      logger.debug("Updating configuration...");

      cronExpression = (String) properties.get(PARAM_KEY_CRON_EXPR);
      if (StringUtils.isBlank(cronExpression) || !CronExpression.isValidExpression(cronExpression)) {
        throw new ConfigurationException(PARAM_KEY_CRON_EXPR, "Cron expression must be valid");
      }
      setCronExpression(cronExpression);
      logger.debug("Timed media offload cron expression: '" + cronExpression + "'");

      try {
        ageModifier = Long.parseLong((String) properties.get(PARAM_KEY_MAX_AGE));
      } catch (NumberFormatException e) {
        throw new ConfigurationException(PARAM_KEY_MAX_AGE, "Invalid max age");
      }
      if (ageModifier < 0) {
        throw new ConfigurationException(PARAM_KEY_MAX_AGE, "Max age must be greater than zero");
      }
    }

    schedule();
  }

  @Override
  public void scan() {
    Date maxAge = Calendar.getInstance().getTime();
    maxAge.setTime(maxAge.getTime() - (ageModifier * CaptureParameters.HOURS * CaptureParameters.MILLISECONDS));

    assetStore.purgeCache(maxAge);
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

  public void setAssetStore(AwsGlacierAssetStore elementStore) {
    this.assetStore = elementStore;
  }

  /** Quartz job to which triggers a cache cleaning */
  public static class Runner extends AbstractRunner {

    public Runner() {
      super();
    }


    @Override
    protected void execute(final AbstractArchiveScanner parameters, JobExecutionContext ctx) {
      logger.debug("Starting " + parameters.getScannerName() + " job.");

      parameters.scan();

      logger.debug("Finished " + parameters.getScannerName() + " job.");
    }
  }
}
