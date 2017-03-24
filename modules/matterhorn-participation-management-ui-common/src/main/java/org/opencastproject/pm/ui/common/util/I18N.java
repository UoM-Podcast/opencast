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

import org.opencastproject.util.data.Function;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class I18N {
  private final ResourceBundle b;
  private final Function<String, String> defaultStrategy;

  public I18N(ResourceBundle bundle, Function<String, String> defaultStrategy) {
    this.b = bundle;
    this.defaultStrategy = defaultStrategy;
  }

  public String s(String key) {
    try {
      return b.getString(key);
    } catch (MissingResourceException e) {
      return defaultStrategy.apply(key);
    }
  }
}
