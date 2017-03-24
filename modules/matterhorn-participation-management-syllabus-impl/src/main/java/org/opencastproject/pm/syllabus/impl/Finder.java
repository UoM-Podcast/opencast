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

package org.opencastproject.pm.syllabus.impl;

import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.util.data.Function;
import org.opencastproject.util.persistence.Queries;

import org.joda.time.DateTime;

import java.util.List;

import javax.persistence.EntityManager;

/** Basic find operations on a JPA entity. */
public class Finder<A> {
  private final String entityName;

  /**
   * Name of the JPA entity.
   *
   * @see javax.persistence.Entity#name()
   */
  public Finder(String entityName) {
    this.entityName = entityName;
  }

  /** A JPA named query <code><i>EntityName</i>.findAllSince</code> must exist. */
  public Function<EntityManager, List<A>> findAllSince(DateTime since) {
    return Queries.named.findAll(entityName + ".findAllSince", tuple("since", since));
  }

  /** A JPA named query <code><i>EntityName</i>.findAll</code> must exist. */
  public Function<EntityManager, List<A>> findAll() {
    return Queries.named.findAll(entityName + ".findAll");
  }

}
