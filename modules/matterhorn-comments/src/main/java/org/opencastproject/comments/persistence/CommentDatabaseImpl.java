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
package org.opencastproject.comments.persistence;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManagerFactory;


/**
 * Implements {@link SeriesServiceDatabase}. Defines permanent storage for series.
 */
public class CommentDatabaseImpl extends AbstractCommentDatabase {

  /**
   * Logging utilities
   */
  private static final Logger logger = LoggerFactory.getLogger(CommentDatabaseImpl.class);

  private static final String PERSISTENCE_UNIT = "org.opencastproject.comments";

  /**
   * Factory used to create {@link javax.persistence.EntityManager}s for transactions
   */
  private EntityManagerFactory emf;

  /**
   * Creates {@link EntityManagerFactory} using persistence provider and properties passed via OSGi.
   *
   * @param cc
   */
  public void activate(ComponentContext cc) {
    logger.info("Activating persistence manager for comments");
  }

  /**
   * OSGi DI
   */
  void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Override
  public EntityManagerFactory getEmf() {
    return emf;
  }

}
