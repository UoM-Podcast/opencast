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

package org.opencastproject.pm.ui.admin.jobs;

import static org.opencastproject.util.data.Option.option;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.util.data.Arrays;
import org.opencastproject.util.data.Cell;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowSet;

import com.vaadin.data.util.BeanItemContainer;

import java.io.Serializable;

public class JobContainer extends BeanItemContainer<Job> implements Serializable {
  private final Cell<Option<WorkflowService>> ws;

  public JobContainer(Cell<Option<WorkflowService>> wfSvc) {
    super(Job.class);
    this.ws = wfSvc;
    refresh(new WorkflowQuery());
  }

  public void refresh(WorkflowQuery query) {
    if (ws.get().isNone()) {
      removeAllItems();
    } else {
      final WorkflowSet workflowSet;
      try {
        workflowSet = ws.get().get().getWorkflowInstances(query);
      } catch (WorkflowDatabaseException e) {
        throw new RuntimeException(e);
      }
      removeAllItems();
      for (WorkflowInstance wfi : workflowSet.getItems()) {
        MediaPackage mp = wfi.getMediaPackage();
        addBean(new Job(wfi.getId(), mp.getTitle(), Arrays.mkString(mp.getCreators(), ", "), mp.getSeries(),
                        mp.getDate(), option(wfi.getState()).map(stateAsString).getOrElse("-")));
      }
    }
  }

  private Function<WorkflowOperationInstance, String> getDescription = new Function<WorkflowOperationInstance, String>() {
    @Override
    public String apply(WorkflowOperationInstance a) {
      return a.getDescription();
    }
  };

  private Function<WorkflowInstance.WorkflowState, String> stateAsString = new Function<WorkflowInstance.WorkflowState, String>() {
    @Override
    public String apply(WorkflowInstance.WorkflowState a) {
      return a.name().toLowerCase();
    }
  };
}
