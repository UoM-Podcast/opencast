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

import static org.opencastproject.pm.ui.common.util.UiUtil.i18n;
import static org.opencastproject.pm.ui.common.util.UiUtil.vlayout;

import org.opencastproject.pm.api.util.Security;
import org.opencastproject.pm.ui.common.components.MainLayout;
import org.opencastproject.pm.ui.common.util.I18N;
import org.opencastproject.pm.ui.common.util.UiUtil;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.util.Crypt;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;

/** Created by {@link org.opencastproject.pm.ui.teacher.vaadin.PmTeacherUiProvider}. */
@Theme("PM")
public class PmTeacher extends UI {

  private static final Logger logger = LoggerFactory.getLogger(PmTeacher.class);

  private final PmDependencies dep;

  public PmTeacher(PmDependencies dep) {
    this.dep = dep;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void init(VaadinRequest request) {

    logger.debug("Init HQ " + this);
    final I18N i18n = i18n(ResourceBundle.getBundle("messages", request.getLocale()));

    getPage().setTitle(i18n.s("title"));

    SecurityService securityService = dep.getSecurityService();
    User user = securityService.getUser();
    logger.info("User {} is accessing the participation management page for teaching staff", user);

    String teacherEMail;
    try {
      teacherEMail = Crypt.decrypt(Security.TEACHER_EMAIL_KEY, request.getParameter("teacher"));
    } catch (Exception e) {
      Notification.show("Bad request", "Unable to parse the requested teacher: " + request.getParameter("teacher"),
                        Type.ERROR_MESSAGE);
      return;
    }

    if (dep.getParticipationManagementDatabase().get().isNone()) {
      setContent(getErrorLabel(i18n.s("error.db")));
      return;
    } else if (StringUtils.isBlank(teacherEMail)) {
      setContent(getErrorLabel(i18n.s("error.mail")));
      return;
    }

    RecordingsView futureRecordingsView = new FutureRecordingsView(i18n,
                                                       teacherEMail,
                                                       dep.getParticipationManagementDatabase().get().get());

    RecordingsView pastRecordingsView = new PastRecordingsView(i18n,
                                                       teacherEMail,
                                                       dep.getParticipationManagementDatabase().get().get(),
                                                       dep.getSecurityService(),
                                                       new WorkflowServices(dep.getWorkflowService()),
                                                       dep.getArchiveServices());

    final MainLayout mainLayout = new MainLayout(i18n.s("title"), i18n).addPage(
            vlayout(futureRecordingsView, UiUtil.vspacerBig() ,pastRecordingsView));

    setContent(mainLayout);
  }

  private Label getErrorLabel(String error) {
    return UiUtil.label(error, UiUtil.bold, UiUtil.htmlLabel, UiUtil.sizeUndefined);
  }
}
