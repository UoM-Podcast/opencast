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

import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.pm.ui.common.util.I18N;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.data.Cell;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.util.data.Option;

import com.vaadin.ui.Label;

public class ResponseSummaryComponent extends AbstractSummaryComponent {
  private final Label total;
  private final Label today;
  private final Label quarter;
  private final Label unconfirmed;

  public ResponseSummaryComponent(final Cell<Option<ParticipationManagementDatabase>> pm,
          final SecurityService securityService, I18N i18n) {
    super("Responses", pm, securityService, i18n);
    this.total = new Label();
    this.today = new Label();
    this.quarter = new Label();
    this.unconfirmed = new Label();
    setContent(vlayout(row(valueDisplay("Total", total), valueDisplay("Unconfirmed", unconfirmed)),
            row(valueDisplay("Confirmed Today", today), valueDisplay("Confirmed Quarter", quarter))));
    update();
  }

  @Override
  protected void updateValues(ParticipationManagementDatabase pm) throws Exception {
    final String totalValue = format(pm.countConfirmedResponses());
    final String todayValue = format(pm.countDailyConfirmedResponses());
    final String quarterValue = format(pm.countQuarterConfirmedResponses());
    final String unconfirmedValue = format(pm.countUnconfirmedResponses());

    invokeUIChange(new Effect0() {
      @Override
      protected void run() {
        total.setValue(totalValue);
        today.setValue(todayValue);
        quarter.setValue(quarterValue);
        unconfirmed.setValue(unconfirmedValue);
      }
    });
  }
}
