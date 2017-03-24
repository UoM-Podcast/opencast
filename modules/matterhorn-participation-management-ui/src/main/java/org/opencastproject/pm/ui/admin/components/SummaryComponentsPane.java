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

import static org.opencastproject.pm.ui.common.util.UiUtil.hlayout;
import static org.opencastproject.pm.ui.common.util.UiUtil.spacing;
import static org.opencastproject.pm.ui.common.util.UiUtil.vlayout;
import static org.opencastproject.pm.ui.common.util.UiUtil.vmargins;
import static org.opencastproject.pm.ui.common.util.UiUtil.withMargin;
import static org.opencastproject.util.data.Arrays.cons;
import static org.opencastproject.util.data.Collections.grouped;
import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Collections.toArray;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.functions.Booleans.ne;
import static org.opencastproject.util.data.functions.Functions.all;

import org.opencastproject.util.data.Function;

import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;

import java.util.List;

public class SummaryComponentsPane extends CustomComponent {
  public SummaryComponentsPane(final AbstractSummaryComponent... cs) {
    final List<HorizontalLayout> rows =
            mlist(grouped(list(cs), 2)).map(new Function<List<AbstractSummaryComponent>, HorizontalLayout>() {
              @Override public HorizontalLayout apply(List<AbstractSummaryComponent> row) {
                return hlayout(all(withMargin(vmargins), spacing),
                               toArray(Component.class,
                                       mlist(row).filter(ne(AbstractSummaryComponent.ZERO)).value()));
              }
            }).value();
    setCompositionRoot(
            vlayout(withMargin,
                    cons(Component.class,
                         new Button("Refresh All", updater(cs)),
                         toArray(HorizontalLayout.class, rows))));
  }

  private Button.ClickListener updater(final AbstractSummaryComponent... cs) {
    return new Button.ClickListener() {
      @Override public void buttonClick(Button.ClickEvent event) {
        for (AbstractSummaryComponent a : cs) {
          a.update();
        }
      }
    };
  }
}
