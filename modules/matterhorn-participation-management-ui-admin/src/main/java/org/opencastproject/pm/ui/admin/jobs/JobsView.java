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

import static org.opencastproject.pm.ui.common.util.UiUtil.htmlLabel;
import static org.opencastproject.pm.ui.common.util.UiUtil.join;
import static org.opencastproject.pm.ui.common.util.UiUtil.label;
import static org.opencastproject.pm.ui.common.util.UiUtil.sizeUndefined;
import static org.opencastproject.pm.ui.common.util.UiUtil.tableDateColGen;
import static org.opencastproject.pm.ui.common.util.UiUtil.vspacer;
import static org.opencastproject.util.data.Arrays.array;

import org.opencastproject.pm.ui.common.util.I18N;
import org.opencastproject.pm.ui.common.util.UiUtil;
import org.opencastproject.util.data.Cell;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workspace.api.Workspace;

import com.vaadin.event.FieldEvents;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import java.util.Date;


public class JobsView extends CustomComponent {
  private static final String[] COLS = array(Job.PROP_DATE, Job.PROP_TITLE, Job.PROP_PRESENTER, Job.PROP_SERIES,
                                             Job.PROP_STATUS);

  public JobsView(I18N i18n, String caption, final Cell<Option<WorkflowService>> wfSvc,
                  final Cell<Option<Workspace>> wsSvc) {
    if (wfSvc.get().isNone() || wsSvc.get().isNone()) {
      setCompositionRoot(getErrorLabel());
      return;
    }

    Label workspaceLabel = label(join(i18n.s("workspace_total"),
                                      wsSvc.get().get().getTotalSpace().map(UiUtil.<Long>byteSize()).getOrElse("?")));

    final JobContainer jc = new JobContainer(wfSvc);
    // jobs table
    final Table table = new Table(caption, jc);
    final TextField search = new TextField();
    final VerticalLayout p = new VerticalLayout(workspaceLabel, vspacer(), search, table);

    table.setWidth(100, Unit.PERCENTAGE);
    table.setPageLength(Math.min(jc.size(), 10));
    table.setVisibleColumns(COLS);
    table.setColumnWidth(Job.PROP_DATE, 150);
    table.addGeneratedColumn(Job.PROP_DATE, tableDateColGen(new Function<Job, Date>() {
      @Override
      public Date apply(Job job) {
        return job.getDate();
      }
    }));
    table.setColumnCollapsingAllowed(true);
    table.setSortContainerPropertyId(Job.PROP_DATE);
    table.setSortAscending(false);
    // search field
    search.setInputPrompt("Search title");
    search.addTextChangeListener(new FieldEvents.TextChangeListener() {
      @Override
      public void textChange(FieldEvents.TextChangeEvent event) {
        if (wfSvc.get().isNone()) {
          p.removeAllComponents();
          p.addComponent(getErrorLabel());
          return;
        }

        WorkflowQuery query = new WorkflowQuery().withTitle(event.getText());
        table.getSortContainerPropertyId();
        table.isSortAscending();
        jc.refresh(query);
      }
    });
    // don't forget to set the root component
    setCompositionRoot(p);
  }

  private Label getErrorLabel() {
    return label("Kein Workspace or Workflow Service vorhanden", htmlLabel, sizeUndefined);
  }

}
