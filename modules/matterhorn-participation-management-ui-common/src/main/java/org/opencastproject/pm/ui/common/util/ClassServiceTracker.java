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

import static org.opencastproject.util.data.Option.option;

import org.opencastproject.util.data.Option;

import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

/** Simplification of {@link org.osgi.util.tracker.ServiceTracker} to track just by class. */
public final class ClassServiceTracker<T> {
  private final ServiceTracker t;

  /** Create a service tracker looking for a service of class <code>clazz</code>. */
  public ClassServiceTracker(Class<T> clazz) {
    t = new ServiceTracker(FrameworkUtil.getBundle(clazz).getBundleContext(),
                           clazz.getName(),
                           null);
    t.open();
  }

  /** Get the service. */
  public Option<T> get() {
    return option((T) t.getService());
  }

  @Override
  protected void finalize() throws Throwable {
    super.finalize();
    t.close();
  }
}
