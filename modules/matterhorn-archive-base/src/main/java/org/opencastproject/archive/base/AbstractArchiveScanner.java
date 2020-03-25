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

package org.opencastproject.archive.base;

import static org.opencastproject.util.data.Option.some;

import org.opencastproject.kernel.scanner.AbstractScanner;
import org.opencastproject.util.Log;
import org.opencastproject.util.NeedleEye;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

public abstract class AbstractArchiveScanner extends AbstractScanner implements ManagedService {
  private static final Log logger = new Log(LoggerFactory.getLogger(AbstractArchiveScanner.class));

  public static final String JOB_GROUP = "oc-archive-job-group";
  public static final String TRIGGER_GROUP = "oc-archive-trigger-group";

  public void mkJob(Class<? extends AbstractRunner> foo) {
    try {
      quartz = new StdSchedulerFactory().getScheduler();
      quartz.start();
      // create and set the job. To actually run it call schedule(..)
      final JobDetail job = new JobDetail(getJobName(), getJobGroup(), foo);
      job.setDurability(false);
      job.setVolatility(true);
      job.getJobDataMap().put(JOB_PARAM_PARENT, this);
      quartz.addJob(job, true);
    } catch (org.quartz.SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  public abstract void updated(Dictionary<String, ?> properties) throws ConfigurationException;

  @Override
  public String getJobGroup() {
    return JOB_GROUP;
  }

  @Override
  public String getTriggerGroupName() {
    return TRIGGER_GROUP;
  }

  public abstract static class AbstractRunner extends TypedQuartzJob<AbstractArchiveScanner> {
    private static final NeedleEye eye = new NeedleEye();

    public AbstractRunner() {
      super(some(eye));
    }

    @Override
    protected abstract void execute(final AbstractArchiveScanner parameters, JobExecutionContext ctx);
  }
}
