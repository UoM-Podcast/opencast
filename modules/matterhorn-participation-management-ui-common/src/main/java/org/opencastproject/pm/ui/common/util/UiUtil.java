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

package org.opencastproject.pm.ui.common.util;

import static org.opencastproject.util.data.functions.Functions.all;
import static org.opencastproject.util.data.functions.Functions.noop;

import org.opencastproject.util.data.Arrays;
import org.opencastproject.util.data.Effect;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.data.functions.Functions;

import com.vaadin.server.Sizeable;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;

public final class UiUtil {

  public static final String PAGE_TITLE_CLASS = "page-title";
  public static final String PAGE_SUB_TITLE_CLASS = "page-sub-title";

  private UiUtil() {
  }

  public static DateFormat timeFormat() {
    return new SimpleDateFormat("HH:mm");
  }

  public static DateFormat dateFormat() {
    return new SimpleDateFormat("yyyy-MM-dd");
  }

  public static DateFormat dateTimeFormat() {
    return new SimpleDateFormat("yyyy-MM-dd HH:mm");
  }

  public static DateFormat dateTimeFormatSecond() {
    return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  }

  public static <A> Table.ColumnGenerator tableDateColGen(final Function<A, Date> dateFromItem) {
    return new Table.ColumnGenerator() {
      @Override
      public Object generateCell(Table source, Object itemId, Object columnId) {
        return new Label(dateFormat().format(dateFromItem.apply((A) itemId)));

      }
    };
  }

  public static <A> Table.ColumnGenerator tableStringColGen(final Function<A, String> stringFromItem) {
    return new Table.ColumnGenerator() {
      @Override
      public Object generateCell(Table source, Object itemId, Object columnId) {
        return stringFromItem.apply((A) itemId);

      }
    };
  }

  public static I18N i18n(ResourceBundle bundle) {
    return new I18N(bundle, Functions.<String>identity());
  }

  public static String join(String... strs) {
    return Arrays.mkString(strs, " ");
  }

  public static TabSheet tabs(Tuple<String, ? extends ComponentContainer>... tabs) {
    final TabSheet ts = new TabSheet();
    ts.setSizeFull();
    for (Tuple<String, ? extends ComponentContainer> c : tabs) {
      ts.addTab(c.getB(), c.getA());
    }
    return ts;
  }

  public static VerticalLayout vlayout(Effect<? super AbstractOrderedLayout> modifier, Component... cs) {
    final VerticalLayout v = new VerticalLayout(cs);
    modifier.apply(v);
    return v;
  }

  public static VerticalLayout vlayout(Component... cs) {
    return vlayout(noop(), cs);
  }

  public static HorizontalLayout hlayout(Component... cs) {
    return hlayout(noop(), cs);
  }

  public static HorizontalLayout hlayout(Effect<? super AbstractOrderedLayout> modifier, Component... cs) {
    final HorizontalLayout v = new HorizontalLayout(cs);
    modifier.apply(v);
    return v;
  }

  public static Label label(String key, Effect<? super Label>... configs) {
    final Label l = new Label(key);
    all(configs).apply(l);
    return l;
  }

  public static Label vspacer() {
    final Label l = new Label(" ");
    l.setHeight("10px");
    return l;
  }

  public static Label hspacer() {
    final Label l = new Label(" ");
    l.setWidth("10px");
    return l;
  }

  public static Label vspacerBig() {
    final Label l = new Label(" ");
    l.setHeight("20px");
    return l;
  }

  private static final Double kb = 1024d;
  private static final Double mb = kb * kb;
  private static final Double gb = mb * kb;
  private static final Double tb = gb * kb;

  public static <A extends Number> Function<A, String> byteSize() {
    return new Function<A, String>() {
      @Override
      public String apply(A s) {
        final double size = s.doubleValue();
        if (size < kb)
          return String.format("%f", size);
        if (size < mb)
          return String.format("%.1f KB", size / kb);
        if (size < gb)
          return String.format("%.1f MB", size / mb);
        if (size < tb)
          return String.format("%.1f GB", size / gb);
        return String.format("%.1f TB", size / tb);
      }
    };
  }

  public static MarginInfo margins(boolean top, boolean right, boolean bottom, boolean left) {
    return new MarginInfo(top, right, bottom, left);
  }

  public static final MarginInfo hmargins = new MarginInfo(false, true, false, true);
  public static final MarginInfo vmargins = new MarginInfo(true, false, true, false);

  public static final Effect<Component> enable = new Effect<Component>() {
    @Override
    protected void run(Component component) {
      component.setEnabled(true);
    }
  };

  public static final Effect<Sizeable> sizeFull = new Effect<Sizeable>() {
    @Override
    protected void run(Sizeable s) {
      s.setSizeFull();
    }
  };

  public static final Effect<Component> bold = new Effect<Component>() {
    @Override
    protected void run(Component component) {
      component.setStyleName("bold");
    }
  };

  public static final Effect<Component> pageTitle = new Effect<Component>() {
    @Override
    protected void run(Component component) {
      component.setStyleName(PAGE_TITLE_CLASS);
    }
  };

  public static final Effect<Component> pageSubTitle = new Effect<Component>() {
    @Override
    protected void run(Component component) {
      component.setStyleName(PAGE_SUB_TITLE_CLASS);
    }
  };

  public static final Effect<Sizeable> sizeUndefined = new Effect<Sizeable>() {
    @Override
    protected void run(Sizeable s) {
      s.setSizeUndefined();
    }
  };

  public static final Effect<Label> htmlLabel = new Effect<Label>() {
    @Override
    protected void run(Label a) {
      a.setContentMode(ContentMode.HTML);
    }
  };

  public static final Effect<Layout.MarginHandler> withMargin = new Effect<Layout.MarginHandler>() {
    @Override
    protected void run(Layout.MarginHandler a) {
      a.setMargin(true);
    }
  };

  public static final Effect<Layout.MarginHandler> withoutMargin = new Effect<Layout.MarginHandler>() {
    @Override
    protected void run(Layout.MarginHandler a) {
      a.setMargin(false);
    }
  };

  public static final Effect<Component> topLeft = new Effect<Component>() {
    @Override
    protected void run(Component component) {
    }
  };

  public static final Effect<AbstractOrderedLayout> spacing = new Effect<AbstractOrderedLayout>() {
    @Override
    protected void run(AbstractOrderedLayout a) {
      a.setSpacing(true);
    }
  };

  public static Effect<Layout.MarginHandler> withMargin(final MarginInfo info) {
    return new Effect<Layout.MarginHandler>() {
      @Override
      protected void run(Layout.MarginHandler a) {
        a.setMargin(info);
      }
    };
  }
}
