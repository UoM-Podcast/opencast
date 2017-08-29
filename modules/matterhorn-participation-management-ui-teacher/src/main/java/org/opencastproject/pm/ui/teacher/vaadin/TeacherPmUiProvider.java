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

package org.opencastproject.pm.ui.teacher.vaadin;

import org.opencastproject.pm.ui.common.util.ClassServiceTracker;
import org.opencastproject.pm.ui.teacher.TeacherPm;
import org.opencastproject.pm.ui.teacher.TeacherPmUI;

import com.vaadin.server.UIClassSelectionEvent;
import com.vaadin.server.UICreateEvent;
import com.vaadin.server.UIProvider;
import com.vaadin.ui.UI;

/**
 * Created and managed by Vaadin. Bridges between Vaadin and OSGi DI.
 */
public class TeacherPmUiProvider extends UIProvider {

  private final ClassServiceTracker<TeacherPm> teacherPmTracker;

  public TeacherPmUiProvider() {
    teacherPmTracker = new ClassServiceTracker<>(TeacherPm.class);
  }

  @Override
  public Class<? extends UI> getUIClass(UIClassSelectionEvent event) {
    return TeacherPmUI.class;
  }

  @Override
  public UI createInstance(UICreateEvent event) {
    for (TeacherPm teacherPm : teacherPmTracker.get()) {
      return new TeacherPmUI(teacherPm);
    }
    // todo maybe answer with an error UI
    throw new RuntimeException("No Participation Management Teacher UI available");
  }
}
