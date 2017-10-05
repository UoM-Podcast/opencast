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

package org.opencastproject.pm.ui.admin.components;

import static org.opencastproject.pm.ui.common.util.UiUtil.vlayout;

import org.opencastproject.pm.api.Synchronization;
import org.opencastproject.pm.api.SynchronizedRecording;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.pm.api.scheduling.ParticipationFeederService;
import org.opencastproject.pm.api.scheduling.ScheduleFeederService;
import org.opencastproject.pm.api.scheduling.SnapCountService;
import org.opencastproject.pm.ui.common.util.I18N;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.data.Cell;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.util.data.Option;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;

import java.util.HashMap;

public class SnapCountSummaryComponent extends AbstractSummaryComponent {

  public static final String TITLE = "SnapCount";

  private final Cell<Option<ParticipationManagementDatabase>> pm;
  private final Cell<Option<ParticipationFeederService>> syllabusSyncService;
  private final Cell<Option<ScheduleFeederService>> matterhornSyncService;
  private final Cell<Option<SnapCountService>> snapCountService;
  private final Label lastModified;
  private final Label nextPoll;
  private final Label newReservations;
  private final Label totalRecordings;
  private final Label newRecordings;
  private final Label deletedRecordings;
  private final Label updatedRecordings;
  private final Label unchangedRecordings;
  private final Label errors;
  private final Label blacklistWeek;
  private HashMap<String,String> snapCountStats = new HashMap<>();

  private Option<Synchronization> currentSynchronization = Option.<Synchronization> none();

  public SnapCountSummaryComponent(final Cell<Option<ParticipationManagementDatabase>> pm,
          Cell<Option<ParticipationFeederService>> syllabusSyncService,
          Cell<Option<ScheduleFeederService>> matterhornSyncService,
          Cell<Option<SnapCountService>> snapCountService,
          final SecurityService securityService, I18N i18n) {
    super(TITLE, pm, securityService, i18n);
    this.pm = pm;

    this.syllabusSyncService = syllabusSyncService;
    this.matterhornSyncService = matterhornSyncService;
    this.snapCountService = snapCountService;

    this.totalRecordings = new Label();
    this.lastModified = new Label();
    this.nextPoll = new Label();
    this.newReservations = new Label();
    this.newRecordings = new Label();
    this.deletedRecordings = new Label();
    this.unchangedRecordings = new Label();
    this.updatedRecordings = new Label();
    this.errors = new Label();
    this.blacklistWeek = new Label();

    addButton(createSnapCountSynchronizationButton(), Alignment.MIDDLE_RIGHT);
    //addButton(createSyllabusSynchronizationButton(), Alignment.MIDDLE_RIGHT);

    setContent(vlayout(
            row(valueDisplay(i18n.s("tab.dashboard.snap.last.title"), lastModified)),
            row(valueDisplay(i18n.s("tab.dashboard.snap.stats.total"), totalRecordings)),
            row(valueDisplay(i18n.s("tab.dashboard.snap.stats.new"), newRecordings),
                valueDisplay(i18n.s("tab.dashboard.snap.stats.deleted"), deletedRecordings)),
            row(valueDisplay(i18n.s("tab.dashboard.snap.stats.updated"), updatedRecordings),
                valueDisplay(i18n.s("tab.dashboard.snap.stats.unchanged"), unchangedRecordings)),
            row(valueDisplay(i18n.s("tab.dashboard.sync.pull.title"), nextPoll),
                valueDisplay(i18n.s("tab.dashboard.sync.new.title"), newReservations)),
            row(valueDisplay(i18n.s("tab.dashboard.sync.errors.title"), errors))));

    update();
  }

  protected Button createSyllabusSynchronizationButton() {
    return new Button("Sync Syllabus", new Button.ClickListener() {
      @Override
      public void buttonClick(Button.ClickEvent event) {
        update();
        for (ParticipationFeederService sync : syllabusSyncService.get()) {
          sync.harvest();
        }
      }
    });
  }

  protected Button createSnapCountSynchronizationButton() {
    return new Button("Synchronize", new Button.ClickListener() {
      @Override
      public void buttonClick(Button.ClickEvent event) {
        update();
        for (SnapCountService snap : snapCountService.get()) {
          snap.harvestSync();
          snapCountStats = snap.getStats();
        }

      }
    });
  }

  @Override
  protected void updateValues(ParticipationManagementDatabase pm) throws Exception {
    final Synchronization sync = pm.getLastSynchronization();
    final long totalRecs = pm.countTotalRecordings();
    invokeUIChange(new Effect0() {
      @Override
      protected void run() {
        if (sync != null && (currentSynchronization.isNone() || !sync.equals(currentSynchronization.get()))) {
          int news = 0;
          for (SynchronizedRecording rec : sync.getSynchronizedRecordings()) {
            if (rec.getStatus() == SynchronizedRecording.SynchronizationStatus.NEW)
              news++;
          }
          lastModified.setValue(dateFormater.format(sync.getDate()));
          totalRecordings.setValue(Long.toString(totalRecs));
          newReservations.setValue(Integer.toString(news));
          errors.setValue(Integer.toString(sync.getErrors().size()));
          currentSynchronization = Option.some(sync);
          updatedRecordings.setValue(snapCountStats.get("tab.dashboard.snap.stats.updated"));
          unchangedRecordings.setValue(snapCountStats.get("tab.dashboard.snap.stats.unchanged"));
          newRecordings.setValue(snapCountStats.get("tab.dashboard.snap.stats.new"));
          deletedRecordings.setValue(snapCountStats.get("tab.dashboard.snap.stats.deleted"));
        } else if (currentSynchronization.isNone()) {
          lastModified.setValue(i18n.s("tab.dashboard.sync.last.empty"));
          newReservations.setValue("0");
          errors.setValue("0");
        }

        // TODO add a way to get information pull
        nextPoll.setValue(i18n.s("tab.dashboard.sync.pull.empty"));
      }
    });
  }
}
