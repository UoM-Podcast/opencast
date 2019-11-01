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
package org.opencastproject.pm.syllabus.remote.scheduling;

import org.opencastproject.pm.syllabus.impl.scheduling.DassRequirementServicePublisher;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.LoggerFactory;

// TEST convience class to allow the remote version of DassRequirementServicePublisher to be run
// in the same node
public class DassRequirementServicePublisherRemote extends DassRequirementServicePublisher {
  @Override
  public void activate(final ComponentContext cc) throws ConfigurationException {
    logger = LoggerFactory.getLogger(DassRequirementServicePublisherRemote.class);
    super.activate(cc);
  }
}
