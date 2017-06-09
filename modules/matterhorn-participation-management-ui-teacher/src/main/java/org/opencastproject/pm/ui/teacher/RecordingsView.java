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

import static org.opencastproject.pm.api.Person.person;
import static org.opencastproject.pm.ui.common.util.UiUtil.tableDateColGen;
import static org.opencastproject.pm.ui.common.util.UiUtil.tableStringColGen;
import static org.opencastproject.pm.ui.common.util.UiUtil.vlayout;
import static org.opencastproject.pm.ui.common.util.UiUtil.withMargin;

import org.opencastproject.pm.api.Person;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.pm.api.persistence.RecordingView;
import org.opencastproject.pm.ui.common.util.I18N;
import org.opencastproject.pm.ui.common.util.UiUtil;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class RecordingsView extends CustomComponent {
  private static final Logger log = LoggerFactory.getLogger(RecordingsView.class);

  protected static final String COL_TITLE = "title";
  // the column id must match the bean property name (here RecordingView#getStartDate), otherwise sorting does not work!
  protected static final String COL_START_DATE = "startDate";
  protected static final String COL_TIME = "time";
  protected static final String COL_ROOM = "room";

  protected static final Integer COL_WIDTH_SMALL = 100;
  protected static final Integer COL_WIDTH_MEDIUM = 200;
  protected static final Integer COL_WIDTH_LARGE = 300;

  protected final ParticipationManagementDatabase pmDb;
  protected final String email;
  protected VerticalLayout content;
  protected Table table;
  protected RecordingContainer recordingContainer;

  public RecordingsView(final I18N i18n, String viewTitle, String viewCaption,
                        final String email,
                        final ParticipationManagementDatabase pmDb,
                        final Option<WorkflowServices> workflowServices,
                        final Boolean future)
 {
    log.debug("Create RecordingsView");
    this.pmDb = pmDb;
    this.email = email;
    final Person teacher = person("", email);
    recordingContainer = new RecordingContainer(pmDb, workflowServices, teacher, future);
    table = new Table();
    table.setContainerDataSource(recordingContainer);
    table.setPageLength(Math.min(recordingContainer.size(), 20));
    Label description = UiUtil.label(viewCaption, UiUtil.htmlLabel);

    content = vlayout(withMargin, description, UiUtil.vspacer());

    final Panel p = new Panel(viewTitle);
    p.setWidth("100%");
    p.setContent(content);

    table.setColumnWidth(COL_TITLE, COL_WIDTH_MEDIUM);
    table.addGeneratedColumn(COL_START_DATE, tableDateColGen(new Function<RecordingView, Date>() {
      @Override
      public Date apply(RecordingView recording) {
        return recording.getStartDate();
      }
    }));
    table.setColumnWidth(COL_START_DATE, COL_WIDTH_SMALL);
    table.addGeneratedColumn(COL_TIME, tableStringColGen(new Function<RecordingView, String>() {
      @Override
      public String apply(RecordingView recording) {
        return UiUtil.timeFormat().format(recording.getStartDate()) + " - "
                + UiUtil.timeFormat().format(recording.getEndDate());
      }
    }));
    table.setColumnWidth(COL_TIME, COL_WIDTH_SMALL);
    table.setColumnWidth(COL_ROOM, COL_WIDTH_MEDIUM);

    table.setSortContainerPropertyId(COL_START_DATE);
    table.setSortAscending(true);

    super.setCompositionRoot(p);
  }

  /** Set the column headers.
   * @param i18n
   * @param colIds */
  protected void setTableColHeaders(I18N i18n, String[] colIds) {
    for (String colId : colIds) {
      table.setColumnHeader(colId, i18n.s("table.column." + colId));
    }
  }

}
