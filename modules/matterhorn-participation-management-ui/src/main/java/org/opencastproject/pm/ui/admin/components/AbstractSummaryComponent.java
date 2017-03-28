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

import static org.opencastproject.pm.ui.common.util.UiUtil.dateTimeFormatSecond;
import static org.opencastproject.pm.ui.common.util.UiUtil.hlayout;
import static org.opencastproject.pm.ui.common.util.UiUtil.label;
import static org.opencastproject.pm.ui.common.util.UiUtil.spacing;
import static org.opencastproject.pm.ui.common.util.UiUtil.vlayout;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.VCell.cell;

import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.util.SecurityContext;
import org.opencastproject.util.data.Cell;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.util.data.Option;

import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.VerticalLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AbstractSummaryComponent extends CustomComponent {
  public static final AbstractSummaryComponent ZERO = new AbstractSummaryComponent("",
          cell(none(ParticipationManagementDatabase.class)), null) {
    @Override
    protected void updateValues(ParticipationManagementDatabase pm) throws Exception {
    }
  };

  private static final Logger logger = LoggerFactory.getLogger(AbstractSummaryComponent.class);

  private final VerticalLayout content;
  private final Label lastUpdated;
  private final Button refreshButton;
  private final ProgressIndicator indicator;
  private final HorizontalLayout buttonsContainer;
  private final Cell<Option<ParticipationManagementDatabase>> pm;
  private final SecurityService securityService;

  /** Thread pool to run the background workers. */
  private final ExecutorService executorService = Executors.newCachedThreadPool();

  public AbstractSummaryComponent(String title, final Cell<Option<ParticipationManagementDatabase>> pm,
          final SecurityService securityService) {
    this.pm = pm;
    this.securityService = securityService;
    lastUpdated = new Label();
    buttonsContainer = hlayout();
    buttonsContainer.setSizeFull();
    indicator = new ProgressIndicator(new Float(0.0));
    indicator.setIndeterminate(true);
    indicator.setPollingInterval(500);
    buttonsContainer.addComponent(indicator);
    buttonsContainer.setComponentAlignment(indicator, Alignment.MIDDLE_LEFT);
    refreshButton = createRefreshButton();
    addButton(refreshButton, Alignment.MIDDLE_RIGHT);

    final Panel main = new Panel(title);
    main.setWidth(400, Unit.PIXELS);
    content = new VerticalLayout();
    final VerticalLayout mainContent = vlayout(buttonsContainer, hlayout(spacing, label("Updated"), lastUpdated),
            content);
    main.setContent(mainContent);
    setCompositionRoot(main);
  }

  protected void invokeUIChange(Effect0 effect) {
    VaadinSession session = getSession();
    if (session != null)
      session.lock();
    try {
      effect.apply();
    } finally {
      if (session != null)
        session.unlock();
    }
  }

  protected static Component row(Component... cs) {
    return hlayout(cs);
  }

  protected static Component valueDisplay(String title, Label value) {
    final VerticalLayout a = vlayout(label(title), value);
    a.setWidth(200, Unit.PIXELS);
    a.setMargin(true);
    return a;
  }

  protected static String format(long value) {
    return String.format("%,d", value);
  }

  public void setContent(Component... cs) {
    content.removeAllComponents();
    content.addComponents(cs);
  }

  /**
   * Add a new button at the top of the summary component
   */
  protected AbstractSummaryComponent addButton(Button button, Alignment alignement) {
    buttonsContainer.addComponentAsFirst(button);
    buttonsContainer.setComponentAlignment(button, alignement);
    return this;
  }

  /**
   * Create the refresh button. The default implementation creates a button labelled "Refresh" calling {@link #update()}
   * on click. Overwrite to customize.
   */
  protected Button createRefreshButton() {
    return new Button("Refresh", new Button.ClickListener() {
      @Override
      public void buttonClick(Button.ClickEvent event) {
        update();
      }
    });
  }

  /** Update custom sub components. */
  protected abstract void updateValues(ParticipationManagementDatabase pm) throws Exception;

  /** Update the component. Custom code goes into {@link #updateValues}. */
  public final void update() {
    // Create an indicator that makes you look busy
    indicator.setVisible(true);
    indicator.setEnabled(true);
    refreshButton.setEnabled(false);
    if (securityService == null) {
      logger.warn("No security context available");
      return;
    }
    Organization org = securityService.getOrganization();
    if (org == null) {
      org = new DefaultOrganization();
      securityService.setOrganization(org);
    }
    final SecurityContext sctx = new SecurityContext(securityService, org, securityService.getUser());
    executorService.execute(new Runnable() {
      @Override
      public void run() {
        sctx.runInContext(new Effect0() {
          @Override
          protected void run() {
            try {
              for (ParticipationManagementDatabase a : pm.get()) {
                updateValues(a);
                invokeUIChange(new Effect0() {
                  @Override
                  protected void run() {
                    lastUpdated.setValue(dateTimeFormatSecond().format(new Date()));
                  }
                });
              }
              // todo database currently unavailable
            } catch (Exception e) {
              // todo
            } finally {
              invokeUIChange(new Effect0() {
                @Override
                protected void run() {
                  indicator.setEnabled(false);
                  indicator.setVisible(false);
                  refreshButton.setEnabled(true);
                }
              });
            }
          }
        });
      }
    });
  }
}
