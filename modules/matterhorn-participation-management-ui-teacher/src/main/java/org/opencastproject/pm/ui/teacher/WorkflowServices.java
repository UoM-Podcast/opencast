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

import static java.lang.String.format;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/** Some code borrowed from module entwine-integration-tests. */
public final class WorkflowServices {
  private static final Logger log = LoggerFactory.getLogger(WorkflowServices.class);
  private static final long POLLING_INTERVAL = 30000;

  private final WorkflowService svc;

  public WorkflowServices(WorkflowService svc) {
    this.svc = svc;
  }

  /**
   * Start a particular workflow and await its termination.
   *
   * @return either the final state of the workflow instance or none if the workflow definition cannot be found
   */
  public Option<WorkflowInstance> runWorkflow(String workflowDefinitionId, MediaPackage mp) {
    try {
      final WorkflowDefinition wd = svc.getWorkflowDefinitionById(workflowDefinitionId);
      WorkflowInstance wi = svc.start(wd, mp);
      log.info(format("Started workflow %d@%s on media package %s", wi.getId(), workflowDefinitionId,
              mp.getIdentifier()));
      while (notFinished(wi)) {
        log.info(format("Waiting for workflow %d to complete", wi.getId()));
        Thread.sleep(POLLING_INTERVAL);
        wi = svc.getWorkflowById(wi.getId());
      }
      return some(wi);
    } catch (NotFoundException e) {
      return none();
    } catch (Exception e) {
      return chuck(e);
    }
  }

  /**
   * Start a workflow of type <code>workflowDefinitionId</code> on a given media package. Do not wait for the workflow
   * to complete.
   *
   * @return the start state of the workflow
   */
  public Option<WorkflowInstance> startWorkflow(String workflowDefinitionId, MediaPackage mp, Map<String, String> options) {
    try {
      final WorkflowDefinition wd = svc.getWorkflowDefinitionById(workflowDefinitionId);
      final WorkflowInstance wi = svc.start(wd, mp, options);
      log.info(format("Started workflow %d@%s on media package %s", wi.getId(), workflowDefinitionId,
              mp.getIdentifier()));
      return some(wi);
    } catch (NotFoundException e) {
      return none();
    } catch (Exception e) {
      return chuck(e);
    }
  }

  public static boolean notFinished(WorkflowInstance wi) {
    switch (wi.getState()) {
      case SUCCEEDED:
      case FAILED:
      case STOPPED:
        return false;
      default:
        return true;
    }
  }

  public static boolean isSucceeded(WorkflowInstance wi) {
    switch (wi.getState()) {
      case SUCCEEDED:
        return true;
      default:
        return false;
    }
  }

  public static boolean isPaused(WorkflowInstance wi) {
    switch (wi.getState()) {
      case PAUSED:
      case INSTANTIATED:
        return true;
      default:
        return false;
    }
  }

  public static boolean isRunning(WorkflowInstance wi) {
    return (wi.getState() == WorkflowState.RUNNING);
  }

  private static final Function<WorkflowInstance, Boolean> notFinishedFn = new Function<WorkflowInstance, Boolean>() {
    @Override
    public Boolean apply(WorkflowInstance wi) {
      return notFinished(wi);
    }
  };

  /** Wait for all workflows processing a given media package to complete. */
  public void waitForWorkflowsOnMediaPackage(MediaPackage mp) {
    waitForWorkflowsOnMediaPackage(mp.getIdentifier().toString());
  }

  /** Wait for all workflows processing a given media package to complete. */
  public void waitForWorkflowsOnMediaPackage(String mpId) {
    final WorkflowQuery q = new WorkflowQuery().withMediaPackage(mpId);
    try {
      while (true) {
        boolean loop = true;
        for (WorkflowInstance wi : getSvc().getWorkflowInstances(q).getItems()) {
          loop = loop && notFinished(wi);
        }
        if (loop) {
          log.info(format("Waiting for workflows on media package %s to complete", mpId));
          Thread.sleep(POLLING_INTERVAL);
        } else {
          break;
        }
      }
    } catch (Exception e) {
      chuck(e);
    }
  }

  /** Check if running workflows for the given media package exist. */
  public boolean workflowsRunningOnMediaPackage(MediaPackage mp) {
    final WorkflowQuery q = new WorkflowQuery().withMediaPackage(mp.getIdentifier().toString());
    try {
      return mlist(getSvc().getWorkflowInstances(q).getItems()).exists(notFinishedFn);
    } catch (WorkflowDatabaseException e) {
      return (Boolean) chuck(e);
    }
  }

  public WorkflowService getSvc() {
    return svc;
  }

  public boolean pauseWorkflow(Long id) {
    try {
      svc.suspend(id);
      return true;
    } catch (NotFoundException e) {
      return false;
    } catch (Exception e) {
      return chuck(e);
    }
  }
}
