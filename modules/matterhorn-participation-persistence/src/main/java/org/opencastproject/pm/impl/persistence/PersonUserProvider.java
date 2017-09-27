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

package org.opencastproject.pm.impl.persistence;

import org.opencastproject.pm.api.Person;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabaseException;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserProvider;
import org.opencastproject.util.data.Function2;
import org.opencastproject.util.data.Monadics;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * A UserProvider that reads users from the participation management person table.
 */
public class PersonUserProvider implements UserProvider {

  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(PersonUserProvider.class);

  public static final String PROVIDER_NAME = "participation";

  private ParticipationManagementDatabase database;

  private SecurityService securityService;

  /**
   * OSGi callback to set the participation database
   * 
   * @param database
   *          the participation database
   */
  public void setDatabase(ParticipationManagementDatabase database) {
    this.database = database;
  }

  /**
   * OSGi callback to set security service
   * 
   * @param securityService
   *          the security service
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * OSGi activation method
   * 
   * @param cc
   */
  public void activate(ComponentContext cc) {
    logger.info("Activating participation management's person user provider");
  }

  @Override
  public String getName() {
    return PROVIDER_NAME;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Iterator<User> getUsers() {
    try {
      return Monadics.mlist(database.getPersons())
              .map(toUser.curry(JaxbOrganization.fromOrganization(securityService.getOrganization()))).value()
              .iterator();
    } catch (ParticipationManagementDatabaseException e) {
      return Collections.EMPTY_LIST.iterator();
    }
  }

  @Override
  public User loadUser(String userName) {
    try {
      return toUser.apply(JaxbOrganization.fromOrganization(securityService.getOrganization()),
              database.getPerson(userName));
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public long countUsers() {
    try {
      return database.countPersons();
    } catch (ParticipationManagementDatabaseException e) {
      return 0;
    }
  }

  @Override
  public String getOrganization() {
    return ALL_ORGANIZATIONS;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Iterator<User> findUsers(String query, int offset, int limit) {
    try {
      List<Person> persons = database.findPersons(query, offset, limit);
      return Monadics.mlist(persons)
              .map(toUser.curry(JaxbOrganization.fromOrganization(securityService.getOrganization()))).iterator();
    } catch (ParticipationManagementDatabaseException e) {
      return Collections.EMPTY_LIST.iterator();
    }

  }

  private Function2<JaxbOrganization, Person, User> toUser = new Function2<JaxbOrganization, Person, User>() {
    @Override
    public User apply(JaxbOrganization o, Person person) {
      return new JaxbUser(person.getEmail(), null, person.getName(), person.getEmail(), PROVIDER_NAME, false, o,
              new HashSet<JaxbRole>());
    }
  };

  @Override
  public void invalidate(String userName) {
    // do nothing 
  }

}
