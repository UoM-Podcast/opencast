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

package org.opencastproject.pm.ui.common.components;

import static org.opencastproject.pm.ui.common.util.UiUtil.tabs;
import static org.opencastproject.pm.ui.common.util.UiUtil.vlayout;
import static org.opencastproject.pm.ui.common.util.UiUtil.withMargin;
import static org.opencastproject.pm.ui.common.util.UiUtil.withoutMargin;

import org.opencastproject.pm.ui.common.util.I18N;
import org.opencastproject.util.data.Tuple;

import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HasComponents;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;


/**
 * Main Layout for the participation management UI
 */
public class MainLayout extends CustomComponent {

  private I18N i18n;

  // Layout elements
  private MenuBar menu;
  private MenuBar.MenuItem title;
  private TabSheet tabSheet;
  private VerticalLayout header;
  private VerticalLayout content;

  /**
   * Constructor for a participation management main layout
   *
   * @param name
   *         The title of the page
   * @param i18n
   */
  public MainLayout(String name, I18N i18n) {

    this.i18n = i18n;

    // Create and configure menu
    menu = new MenuBar();
    menu.setHtmlContentAllowed(true);
    menu.setWidth(100.0f, Unit.PERCENTAGE);
    menu.setSizeFull();
    title = menu.addItem("<b>" + name + "</b>", null);
    title.setEnabled(false);
    title.setStyleName("main-title");
    title.addSeparator();
    tabSheet = tabs();

    header = vlayout(withoutMargin, menu);
    // default use tabs
    content = vlayout(withMargin, tabSheet);
    setCompositionRoot(vlayout(header, content));
  }

  /**
   * Add tab(s) to the main layout
   *
   * @param tabs
   *         A comma separated list of tabs to add
   * @return a reference to the current main layout
   */
  public MainLayout addTabs(Tuple<String, ? extends HasComponents>... tabs) {
    for (Tuple<String, ? extends HasComponents> tab : tabs) {
      tabSheet.addTab(tab.getB(), tab.getA());
    }
    return this;
  }

  /**
   * Remove tab(s) from the main layout
   *
   * @param tabComponents
   *         A comma separated list of tabs to remove
   * @return a reference to the current main layout
   */
  public MainLayout removeTabs(Component... tabComponents) {
    for (Component tabComponent : tabComponents) {
      tabSheet.removeComponent(tabComponent);
    }
    return this;
  }

  public MainLayout addSelectedTabChangeListener(TabSheet.SelectedTabChangeListener listener) {
    tabSheet.addSelectedTabChangeListener(listener);

    return this;
  }

  /**
   * Add item(s)/link(s) to the menu
   *
   * @param items
   *         A comma separated list of item to add
   * @return a reference to the current main layout
   */
  public MainLayout addMenuItem(Tuple<String, Command>... items) {
    for (Tuple<String, Command> item : items) {
      menu.addItem(item.getA(), item.getB());
    }
    return this;
  }

    /**
   * Add replace tabs withs single page
   *
   * @param page
   *         A comma separated list of tabs to add
   * @return a reference to the current main layout
   */
  public MainLayout addPage(Component page) {
    this.content = vlayout(withMargin, page);
    setCompositionRoot(vlayout(header, content));
    return this;
  }
}
