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

import org.opencastproject.pm.api.Recording.RecordingStatus;
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
import com.vaadin.shared.ui.label.ContentMode;
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class FutureRecordingsView extends RecordingsView {
  private static final Logger log = LoggerFactory.getLogger(FutureRecordingsView.class);

  private static final String OPT_RECORD = "record";
  private static final String OPT_RECORD_EDIT = "record.edit";
  private static final String OPT_OPT_OUT = "opt-out";

  private static final String COL_SELECT = "select";
  private static final String COL_STATUS = "status";
  private static final String COL_RECORD = "record";

  private static final String[] COLS = array(COL_SELECT, COL_TITLE, COL_START_DATE, COL_TIME, COL_ROOM, COL_STATUS,
                                             COL_RECORD);

  private boolean areAllChecked = false;

  public FutureRecordingsView(final I18N i18n,
                        final String email,
                        final TeacherPm teacherPm) {
    super(i18n, i18n.s("future.title"), i18n.s("future.text"),
            email, teacherPm, Option.none(WorkflowServiceUtils.class), true);

    log.debug("Create FutureRecordingsView");

    final Set<RecordingView> objectIds = new HashSet<>();

    final OptionGroup recordGroup = new OptionGroup();
    final Label recordInputsLabel = new Label(i18n.s("options.record.inputs"));
    final List<String> inputs = teacherPm.getCaptureAgentInputs().get();
    setOptGroupItems(recordGroup, i18n, "options.record.input", inputs.get(0), inputs);
    recordGroup.setMultiSelect(true);
    recordGroup.setNullSelectionAllowed(false); // FIXME seems to remember last selected but only after another event has been Listened

    final OptionGroup optOutGroup = new OptionGroup();
    setOptGroupItems(optOutGroup, i18n, "options", OPT_OPT_OUT, Arrays.asList(OPT_OPT_OUT, OPT_RECORD, OPT_RECORD_EDIT));

    Button applyButton = new Button("Apply");
    applyButton.addClickListener(new ClickListener() {
      @Override
      public void buttonClick(ClickEvent event) {
        for (RecordingView recording : objectIds) {
          ParticipationManagementDatabase pm = pmDb;
          try {
            final Object selected = optOutGroup.getValue();
            final String inputs = filterSelectedInputs(recording.getAgentInputs(),
                    (Set<String>)recordGroup.getValue());
            final ReviewStatus status;
            final Boolean edit;

            if (eq(selected, OPT_RECORD)) {
              status = ReviewStatus.CONFIRMED;
              edit = false;
            } else if (eq(selected, OPT_RECORD_EDIT)) {
              status = ReviewStatus.CONFIRMED;
              edit = true;
            } else { // if (eq(selected, OPT_OPT_OUT))
              status = ReviewStatus.OPTED_OUT;
              edit = false;
            }
            pm.updateRecordingOptOut(recording.getId(), status, edit, inputs);
            log.info(String.format("User: %s Recording: %d Status: %s Edit: %s Inputs: %s",
                    email, recording.getId(), status.toString(), edit.toString(), inputs));
          } catch (ParticipationManagementDatabaseException | NotFoundException e) {
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

    content.addComponents(vlayout(withMargin, optOutGroup, UiUtil.vspacer(),
            recordInputsLabel, recordGroup,
            UiUtil.vspacer(), applyButton, UiUtil.vspacer(), table));

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
        final Label statusLabel = new Label();
        final String statusLabelText;
        statusLabel.setContentMode(ContentMode.HTML);

        switch (recording.getStatus()) {
          case READY:
            if (recording.isEdit()) {
              statusLabelText = i18n.s("table.status.edit");
            } else {
              statusLabelText = i18n.s("table.status.ready");
            }
            break;
          case OPTED_OUT:
            statusLabelText = i18n.s("table.status.optedout");
            statusLabel.setPrimaryStyleName("pm_opted_out");
            break;
          case BLACKLISTED:
            statusLabelText = i18n.s("table.status.blacklisted");
            break;
          default:
            statusLabelText = recording.getStatus().toString();
            break;
        }

        if (recording.getStatus() == RecordingStatus.OPTED_OUT && !recording.requiresRecording()) {
          statusLabel.setValue(statusLabelText);
        } else {
          statusLabel.setValue(statusLabelText
                + getInputIcons(recording.getRecordingInput(), "The %s will be recorded"));
        }
        return statusLabel;
      }
    });
    table.setColumnWidth(COL_STATUS, COL_WIDTH_MEDIUM);
    table.addGeneratedColumn(COL_RECORD, new ColumnGenerator() {
      @Override
      public Object generateCell(Table t, final Object item, Object colId) {
        final RecordingView recording = (RecordingView) item;
        Boolean required = recording.requiresRecording();
        final Label recordedLabel = new Label();
        final String recordedLabelText;

        switch (recording.getStatus()) {
          case READY:
            if (required && recording.isEdit()) {
              recordedLabelText = i18n.s("table.record.override_edit");
            } else {
              recordedLabelText = i18n.s("table.record.yes");
            }
            recordedLabel.setPrimaryStyleName("pm_recording");
            break;
          case OPTED_OUT:
            if (required) {
              recordedLabelText = i18n.s("table.record.override");
              recordedLabel.setPrimaryStyleName("pm_recording");
            } else {
              recordedLabelText = i18n.s("table.record.no");
              recordedLabel.setPrimaryStyleName("pm_not_recording");
            }
            break;
          case BLACKLISTED:
            recordedLabelText = i18n.s("table.record.blacklisted");
            recordedLabel.setPrimaryStyleName("pm_not_recording");
            break;
          default:
            recordedLabelText = recording.getStatus().toString();
            break;
        }
        recordedLabel.setValue(recordedLabelText);
        return recordedLabel;
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
  private void setOptGroupItems(OptionGroup group, I18N i18n, String prefix, String selectId, List<String> optionIds) {
    for (String optionId : optionIds) {
      group.addItem(optionId);
      group.setItemCaption(optionId, i18n.s(prefix + "." + optionId));
    }
    group.select(selectId);
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

  /**
  *  filter selected inputs by what is available
  @param availableInputs
  @param selectedInputs
  @return filtered inputs
  */
  private String filterSelectedInputs(String availableInputs, Set<String> selectedInputs) {
    final Set<String> filteredInputs = new HashSet<>();
    for (String input : selectedInputs) {
      if (availableInputs.contains(input)) {
        filteredInputs.add(input);
      }
    }
    // the default is the first availale input
    if (filteredInputs.isEmpty()) {
      return availableInputs.split("\\|")[0];
    }
    return String.join("|", filteredInputs);
  }
}
