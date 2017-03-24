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

package org.opencastproject.pm.syllabus.impl.scheduling;

/** Collects some statistics about the harvest process. */
public final class HarvestStats {
  private int statTotal = 0;
  private int statNew = 0;
  private int statUpdated = 0;
  private int statNotModified = 0;
  private int statDeleted = 0;

  public int incTotal() {
    return ++statTotal;
  }

  public int incNew() {
    return ++statNew;
  }

  public int incUpdated() {
    return ++statUpdated;
  }

  public int incNotModified() {
    return ++statNotModified;
  }

  public int incDeleted() {
    return ++statDeleted;
  }

  public int getTotal() {
    return statTotal;
  }

  public int getNew() {
    return statNew;
  }

  public int getUpdated() {
    return statUpdated;
  }

  public int getNotModified() {
    return statNotModified;
  }

  public int getDeleted() {
    return statDeleted;
  }
}
