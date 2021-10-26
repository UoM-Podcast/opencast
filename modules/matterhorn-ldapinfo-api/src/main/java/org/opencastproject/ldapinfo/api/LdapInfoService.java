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

package org.opencastproject.ldapinfo.api;

import java.util.List;
import java.util.Map;

/**
 * Api for the LDAP Information Service
 * 
 * An Opencast Service to query a local LDAP server to get limited user information from a username.
 */
public interface LdapInfoService {

  /**
   * Get the spotId for the given user
   * @param username
   *          the username to look up
   * @return The spotId of the user
   * @throws org.opencastproject.ldapinfo.api.LdapInfoException
   */
  String getSpotId(String username) throws LdapInfoException;

  /**
   * Get the LDAP directory information for a user by spotId the fields to be
   * returned are configured in the config file (regex matching is permitted)
   * @param spotId
   *          the spotId to look up
   * @return map of directory information
   * @throws org.opencastproject.ldapinfo.api.LdapInfoException
   */
  Map<String,List<String>> getDirectoryInformation(String spotId) throws LdapInfoException;

  /**
   * Check the group membership of a given user.
   *
   * @param username
   *          the username to look up
   * @param group
   *          the LDAP group to be tested for the user
   * @return true   if the user is a member of the group
   *         false  otherwise
   * @throws org.opencastproject.ldapinfo.api.LdapInfoException
   */
  boolean isMemberOf(String username, String group) throws LdapInfoException;
}
