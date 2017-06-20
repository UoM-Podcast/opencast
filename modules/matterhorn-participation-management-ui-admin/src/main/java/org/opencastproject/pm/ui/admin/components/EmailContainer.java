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

import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.pm.api.Course.EmailStatus;
import org.opencastproject.pm.api.persistence.EmailView;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabaseException;
import org.opencastproject.pm.api.persistence.RecordingQuery;
import org.opencastproject.util.data.Cell;
import org.opencastproject.util.data.Option;

import com.vaadin.data.util.BeanItemContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;

public class EmailContainer extends BeanItemContainer<EmailView> implements Serializable {
  private static final Logger log = LoggerFactory.getLogger(EmailContainer.class);
  private static final int ROWS_MAX = 100;
  private final Cell<Option<ParticipationManagementDatabase>> pm;
  private final EmailStatus emailStatus;

  public EmailContainer(Cell<Option<ParticipationManagementDatabase>> pm, EmailStatus emailStatus) {
    super(EmailView.class);
    this.pm = pm;
    this.emailStatus = emailStatus;

    if (pm.get().isSome()) {
      try {
        List<EmailView> emailViews = pm.get().get().findRecordingsAsEmailView(this.emailStatus);
        emailViews.addAll(pm.get().get().findCoursesAsEmailView(EmailStatus.valueOf(this.emailStatus.toString())));
        int r = 0;

        for (EmailView emailView : emailViews) {
          addBean(emailView);
          if (++r >= ROWS_MAX) {
            break;
          }
        }
      } catch (ParticipationManagementDatabaseException e) {
        chuck(e);
      }
    }
  }

  public void refresh(final RecordingQuery query) {
    log.trace("Refresh");
    if (pm.get().isNone()) {
      removeAllItems();
    } else {
      try {
        removeAllItems();
        final List<EmailView> emailViews = pm.get().get().findRecordingsAsEmailView(this.emailStatus);
        int r = 0;

        for (EmailView emailView : emailViews) {
          addBean(emailView);
          if (++r >= ROWS_MAX) {
            break;
          }
        }
      } catch (ParticipationManagementDatabaseException e) {
        chuck(e);
      }
    }
  }

  public EmailStatus getEmailStatus() {
    return emailStatus;
  }
}
