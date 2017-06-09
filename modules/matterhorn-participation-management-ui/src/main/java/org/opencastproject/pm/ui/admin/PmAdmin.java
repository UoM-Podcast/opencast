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

package org.opencastproject.pm.ui.admin;

import static org.opencastproject.pm.ui.common.util.UiUtil.i18n;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.pm.ui.admin.components.AbstractSummaryComponent;
import org.opencastproject.pm.ui.admin.components.EmailPane;
import org.opencastproject.pm.ui.admin.components.MessageSummaryComponent;
import org.opencastproject.pm.ui.admin.components.RecordingSummaryComponent;
import org.opencastproject.pm.ui.admin.components.ResponseSummaryComponent;
import org.opencastproject.pm.ui.admin.components.SnapCountSummaryComponent;
import org.opencastproject.pm.ui.admin.components.SummaryComponentsPane;
import org.opencastproject.pm.ui.admin.components.SynchronizationSummaryComponent;
import org.opencastproject.pm.ui.common.components.MainLayout;
import org.opencastproject.pm.ui.common.util.I18N;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.UI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;

/** Created by {@link org.opencastproject.pm.ui.admin.vaadin.PmAdminUiProvider}. */
@Theme("PM")
public class PmAdmin extends UI {
  private static final Logger logger = LoggerFactory.getLogger(PmAdmin.class);

  private final PmDependencies dep;

  public PmAdmin(PmDependencies dep) {
    this.dep = dep;
  }

  @Override
  protected void init(VaadinRequest request) {
    logger.info("Init HQ " + this);
    // Set the window or tab title
    getPage().setTitle("Opencast Participation Management - Admin UI");

    final I18N i18n = i18n(ResourceBundle.getBundle("messages", request.getLocale()));
    final EmailPane emailPane =  new EmailPane(
            dep.getSecurityService(), dep.getParticipationManagementDatabase(),
            dep.getEmailSenderService(), dep.getSystemUserName(), i18n);
    final MainLayout mainLayout = new MainLayout("Participation Management - Admin UI", i18n).addTabs(
            tuple(i18n.s("tab.dashboard"),
                    new SummaryComponentsPane(
                            new SnapCountSummaryComponent(dep.getParticipationManagementDatabase(),
                                    dep.getParticipationFeederService(),
                                    dep.getScheduleFeederService(), dep.getSnapCountService(), i18n, dep.getSecurityService()),
                            AbstractSummaryComponent.ZERO,
                            new SynchronizationSummaryComponent(dep.getParticipationManagementDatabase(),
                                    dep.getParticipationFeederService(),
                                    dep.getScheduleFeederService(), i18n, dep.getSecurityService()),
                            new RecordingSummaryComponent(dep.getParticipationManagementDatabase(), dep
                                    .getSecurityService()),
                            new MessageSummaryComponent(dep.getParticipationManagementDatabase(), dep
                                    .getSecurityService()),
                            new ResponseSummaryComponent(dep
                                    .getParticipationManagementDatabase(), dep.getSecurityService()))),
            tuple(i18n.s("tab.email"), emailPane));

    mainLayout.addSelectedTabChangeListener(new TabSheet.SelectedTabChangeListener() {
      @Override
      public void selectedTabChange(TabSheet.SelectedTabChangeEvent event) {
        if (event.getTabSheet().getSelectedTab() == emailPane) {
          emailPane.update();
        }
      }
    });

    setContent(mainLayout);
  }
}
