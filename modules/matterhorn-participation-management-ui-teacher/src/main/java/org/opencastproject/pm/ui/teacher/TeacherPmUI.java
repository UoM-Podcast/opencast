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

import org.opencastproject.pm.api.util.Security;
import org.opencastproject.pm.ui.common.components.MainLayout;
import org.opencastproject.pm.ui.common.util.I18N;
import org.opencastproject.pm.ui.common.util.UiUtil;
import org.opencastproject.util.Crypt;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.VCell;
import org.opencastproject.util.data.functions.Functions;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.ResourceBundle;

/** Created by {@link org.opencastproject.pm.ui.teacher.vaadin.PmTeacherUiProvider}. */
@Theme("PM")
public class TeacherPmUI extends UI {

  private static final Logger logger = LoggerFactory.getLogger(TeacherPmUI.class);

  private final TeacherPm teacherPm;

  public TeacherPmUI(TeacherPm teacherPm) {
    this.teacherPm = teacherPm;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void init(VaadinRequest request) {

    logger.debug("Init HQ " + this);
    final I18N i18n = new I18N(ResourceBundle.getBundle("messages", request.getLocale()), customMessages(teacherPm.getCaptureAgentInputDescriptions()));

    getPage().setTitle(i18n.s("title"));

    String teacherEMail;
    try {
      teacherEMail = Crypt.decrypt(Security.TEACHER_EMAIL_KEY, request.getParameter("teacher"));
    } catch (Exception e) {
      Notification.show("Bad request", "Unable to parse the requested teacher: " + request.getParameter("teacher"),
                        Type.ERROR_MESSAGE);
      return;
    }

    if (teacherPm.getParticipationManagementDatabase().get().isNone()) {
      setContent(getErrorLabel(i18n.s("error.db")));
      return;
    } else if (StringUtils.isBlank(teacherEMail)) {
      setContent(getErrorLabel(i18n.s("error.mail")));
      return;
    }

    RecordingsView futureRecordingsView = new FutureRecordingsView(i18n,
                                                       teacherEMail,
                                                       teacherPm);

    RecordingsView pastRecordingsView = new  PastRecordingsView(i18n,
                                                       teacherEMail,
                                                       teacherPm,
                                                       new WorkflowServiceUtils(teacherPm.getWorkflowService()));

    final MainLayout mainLayout = new MainLayout(i18n.s("title"), i18n).addPage(
            vlayout(futureRecordingsView, UiUtil.vspacerBig() ,pastRecordingsView));

    setContent(mainLayout);
  }

  private Label getErrorLabel(String error) {
    return UiUtil.label(error, UiUtil.bold, UiUtil.htmlLabel, UiUtil.sizeUndefined);
  }

  private Function<String, String> customMessages(final VCell<Map<String, String>> messages) {
    return new Function<String, String>() {
      @Override
      public String apply(String a) {
        if (messages.get().containsKey(a)) {
          return messages.get().get(a);
        } else {
          return Functions.<String>identity().apply(a);
        }
      }
    };
  }

}
