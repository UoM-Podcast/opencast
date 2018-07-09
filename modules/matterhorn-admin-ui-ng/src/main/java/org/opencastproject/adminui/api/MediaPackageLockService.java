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
package org.opencastproject.adminui.api;

import org.opencastproject.index.service.impl.index.event.Event;

/**
 *
 * @author Tobias M Schiebeck
 */
public interface MediaPackageLockService {
  /**
   * Minimum duration the media package is locked for.
   */
  long MIN_LOCK_DURATION = 3600000;

  /**
   * Try to acquire a lock for the event's media package, if already held
   * by the user refresh the lock.
   * @param event
   * @param sessionId identifier of potential holder
   * @return -1 if lock acquired else the millisecs before the lock expires
   */
  long getMediaPackageLock(Event event, String sessionId);

  /**
   * Try to release a lock for the event's media package.
   * @param event
   * @param sessionId identifier of potential holder
   */
  void releaseMediaPackageLock(final Event event, String sessionId);

  /**
   * Remove expired locks.
   */
  void cleanUp();
}
