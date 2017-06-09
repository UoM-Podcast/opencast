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

import static org.opencastproject.pm.ui.common.util.UiUtil.vlayout;
import static org.opencastproject.pm.ui.common.util.UiUtil.withMargin;
import static org.opencastproject.util.EqualsUtil.eq;
import static org.opencastproject.util.data.Arrays.array;

import org.opencastproject.pm.api.Recording.ReviewStatus;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabaseException;
import org.opencastproject.pm.api.persistence.RecordingView;
import org.opencastproject.pm.ui.common.util.I18N;
import org.opencastproject.pm.ui.common.util.UiUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.Table.HeaderClickEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;


public class FutureRecordingsView extends RecordingsView {
  private static final Logger log = LoggerFactory.getLogger(FutureRecordingsView.class);

  private static final String OPT_RECORD_NO_TRIM = "record-no-trim";
  private static final String OPT_RECORD_TRIM = "record-trim";
  private static final String OPT_OPT_OUT = "opt-out";

  private static final String COL_SELECT = "select";
  private static final String COL_STATUS = "status";
  private static final String COL_RECORD = "record";

  private static final String[] COLS = array(COL_SELECT, COL_TITLE, COL_START_DATE, COL_TIME, COL_ROOM, COL_STATUS,
                                             COL_RECORD);

  private boolean areAllChecked = false;

  public FutureRecordingsView(final I18N i18n,
                        String email,
                        final ParticipationManagementDatabase pmDb) {
    super(i18n, i18n.s("future.title"), i18n.s("future.text"),
            email, pmDb, Option.none(WorkflowServices.class), true);

    log.debug("Create FutureRecordingsView");

    final Set<RecordingView> objectIds = new HashSet<RecordingView>();

    final OptionGroup optOutGroup = new OptionGroup();
    setOptGroupItems(optOutGroup, i18n, OPT_OPT_OUT, OPT_OPT_OUT, OPT_RECORD_NO_TRIM, OPT_RECORD_TRIM);

    Button applyButton = new Button("Apply");
    applyButton.addClickListener(new ClickListener() {
      @Override
      public void buttonClick(ClickEvent event) {
        for (RecordingView recording : objectIds) {
          ParticipationManagementDatabase pm = pmDb;
          try {
            final Object selected = optOutGroup.getValue();
            final long recId = recording.getId();
            if (eq(selected, OPT_OPT_OUT)) {
              pm.reviewRecording(recId, ReviewStatus.OPTED_OUT);
              pm.trimRecording(recording.getId(), false);
            } else if (eq(selected, OPT_RECORD_NO_TRIM)) {
              pm.reviewRecording(recId, ReviewStatus.CONFIRMED);
              pm.trimRecording(recording.getId(), false);
            } else if (eq(selected, OPT_RECORD_TRIM)) {
              pm.reviewRecording(recId, ReviewStatus.CONFIRMED);
              pm.trimRecording(recording.getId(), true);
            }
          } catch (ParticipationManagementDatabaseException e) {
            throw new RuntimeException(e);
          } catch (NotFoundException e) {
            throw new RuntimeException(e);
          }
        }
        objectIds.clear();
        recordingContainer.refresh();
        table.sort();
        areAllChecked = false;
        checkAndUpdateSelectIcon(table);
      }
    });

    content.addComponents(vlayout(withMargin, optOutGroup, UiUtil.vspacer(), applyButton, UiUtil.vspacer(), table));

    table.addGeneratedColumn(COL_SELECT, new Table.ColumnGenerator() {
      @Override
      public Object generateCell(Table source, final Object item, Object colId) {
        CheckBox checkBox = new CheckBox();
        checkBox.setValue(areAllChecked);
        checkBox.addValueChangeListener(new ValueChangeListener() {
          @Override
          public void valueChange(ValueChangeEvent event) {
            boolean value = (Boolean) event.getProperty().getValue();
            if (value) {
              objectIds.add((RecordingView) item);
            } else {
              objectIds.remove((RecordingView) item);
            }
          }
        });
        return checkBox;

      }
    });

    table.addGeneratedColumn(COL_STATUS, new ColumnGenerator() {
      @Override
      public Object generateCell(Table t, final Object item, Object colId) {
        final RecordingView recording = (RecordingView) item;
        Label optoutLabel;
        switch (recording.getStatus()) {
          case READY:
            if (recording.isTrim()) {
              optoutLabel = new Label(i18n.s("table.status.trim"));
            } else {
              optoutLabel = new Label(i18n.s("table.status.ready"));
            }
            break;
          case OPTED_OUT:
            optoutLabel = new Label(i18n.s("table.status.optedout"));
            optoutLabel.setPrimaryStyleName("pm_opted_out");
            break;
          case BLACKLISTED:
            optoutLabel = new Label(i18n.s("table.status.blacklisted"));
            break;
          default:
            optoutLabel = new Label(recording.getStatus().toString());
            break;
        }
        return optoutLabel;
      }
    });
    table.setColumnWidth(COL_STATUS, COL_WIDTH_MEDIUM);
    table.addGeneratedColumn(COL_RECORD, new ColumnGenerator() {
      @Override
      public Object generateCell(Table t, final Object item, Object colId) {
        final RecordingView recording = (RecordingView) item;
        Boolean required = recording.requiresRecording();
        Label optoutLabel;

        switch (recording.getStatus()) {
          case READY:
            if (required && recording.isTrim()) {
              optoutLabel = new Label(i18n.s("table.record.override_edit"));
            } else {
              optoutLabel = new Label(i18n.s("table.record.yes"));
            }
            optoutLabel.setPrimaryStyleName("pm_recording");
            break;
          case OPTED_OUT:
            if (required) {
              optoutLabel = new Label(i18n.s("table.record.override"));
              optoutLabel.setPrimaryStyleName("pm_recording");
            } else {
              optoutLabel = new Label(i18n.s("table.record.no"));
              optoutLabel.setPrimaryStyleName("pm_not_recording");

            }
            break;
          case BLACKLISTED:
            optoutLabel = new Label(i18n.s("table.record.blacklisted"));
            optoutLabel.setPrimaryStyleName("pm_not_recording");
            break;
          default:
            optoutLabel = new Label(recording.getStatus().toString());
            break;
        }
        return optoutLabel;
      }
    });
    table.setColumnWidth(COL_RECORD, COL_WIDTH_LARGE);

    table.setVisibleColumns(COLS);
    table.setSortAscending(true);
    super.setTableColHeaders(i18n, COLS);

    // Add listener for select/unselect all items action
    table.addHeaderClickListener(new Table.HeaderClickListener() {
      @Override
      public void headerClick(HeaderClickEvent event) {
        String column = (String) event.getPropertyId();
        if (COL_SELECT.equals(column)) {
          areAllChecked = !areAllChecked;
          for (Object itemId : table.getContainerDataSource().getItemIds()) {
            if (areAllChecked) {
              objectIds.add((RecordingView) itemId);
            } else {
              objectIds.remove(itemId);
            }
          }
          checkAndUpdateSelectIcon(table);
        }
      }
    });

    table.setColumnHeader(COL_SELECT, "");
    checkAndUpdateSelectIcon(table);
  }

  /**
   * @param selectId
   *         the id of the option to be selected by default
   */
  private void setOptGroupItems(OptionGroup optOutGroup, I18N i18n, String selectId, String... optionIds) {
    for (String optionId : optionIds) {
      optOutGroup.addItem(optionId);
      optOutGroup.setItemCaption(optionId, i18n.s("options." + optionId));
    }
    optOutGroup.select(selectId);
  }

  /**
   * Checks and updates the icon used for the selection column
   *
   * @param table
   *         The current table
   */
  private void checkAndUpdateSelectIcon(Table table) {
    table.refreshRowCache();
    if (areAllChecked) {
      table.setColumnIcon(COL_SELECT, new ThemeResource("img/checked-icon.png"));
    } else {
      table.setColumnIcon(COL_SELECT, new ThemeResource("img/unchecked-icon.png"));
    }
  }

}
