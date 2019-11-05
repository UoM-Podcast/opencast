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

package org.opencastproject.ldapinfo.impl;

import static org.opencastproject.util.OsgiUtil.getOptCfg;
import static org.opencastproject.util.data.functions.Strings.toBool;

import org.opencastproject.ldapinfo.api.LdapInfoException;
import org.opencastproject.ldapinfo.api.LdapInfoService;
import org.opencastproject.util.data.Option;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

/**
 * An Opencast Service to query a local LDAP server to get limited user information from a username.
 */
public class LdapInfoServiceImpl implements ManagedService, LdapInfoService {

  /** The LDAP context factory */
  public static final String LDAP_CONTEXT_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";

  /** The configuration key to use to contact the LDAP server*/
  public static final String OPT_LDAP_INFO_SERVER_URL_ENABLED = "enabled";

  /** The configuration key to use to contact the LDAP server*/
  public static final String OPT_LDAP_INFO_SERVER_URL = "ldap.url.infoserver";

  /** The LDAP tag that contains the spotId */
  public static final String OPT_LDAP_SPOT_ID_TAG = "ldap.tag.spotId";

  /** The LDAP tag that contains the group membership information */
  public static final String OPT_LDAP_GROUP_MEMBERSHIP_TAG = "ldap.tag.group";

  /** The module specific logger */
  private static final Logger logger = LoggerFactory.getLogger(LdapInfoServiceImpl.class);

  private boolean ldapInfoServerEnabled = false;

  private String ldapInfoServer = "ldap://ldap.university.org";

  private String spotIdTag = "spotId";

  private String groupMembershipTag = "group";

  public void activate(final ComponentContext cc) {
    logger.info("Activating {}", this.getClass().getName());
    try {
      updated(cc.getProperties());
    } catch (ConfigurationException e) {
      logger.debug("Couldn't read properties");
    }
  }

  /** OSGi container called (ConfigurationAdmin). */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public synchronized void updated(Dictionary properties) throws ConfigurationException {
    if (properties == null) {
      return;
    }

    ldapInfoServerEnabled = getOptCfg(properties, OPT_LDAP_INFO_SERVER_URL_ENABLED).map(toBool).getOrElse(false);
    if (!ldapInfoServerEnabled) {
      logger.warn("The LDAP information server is not configured!");
      return;
    }

    // LDAP information server
    Option<String> ldapServer = getOptCfg(properties, OPT_LDAP_INFO_SERVER_URL);
    if (ldapServer.isSome()) {
      ldapInfoServer = ldapServer.get();
      logger.info("The LDAP information server is '{}'", ldapInfoServer);
    } else {
      logger.warn("No LDAP information server configured, using '{}'", ldapInfoServer);
    }

    // LDAP tag containing the spotId 
    Option<String> spotId = getOptCfg(properties, OPT_LDAP_SPOT_ID_TAG);
    if (spotId.isSome()) {
      spotIdTag = spotId.get();
      logger.info("The LDAP spotId tag is '{}'", spotIdTag);
    } else {
      logger.warn("No LDAP spotId tag configured, using '{}'", spotIdTag);
    }

    // LDAP tag containing the group membership information 
    Option<String> groups = getOptCfg(properties, OPT_LDAP_GROUP_MEMBERSHIP_TAG);
    if (groups.isSome()) {
      groupMembershipTag = groups.get();
      logger.info("The LDAP groupMembership tag is '{}'", groupMembershipTag);
    } else {
      logger.warn("No LDAP groupMembership tag configured, using '{}'", groupMembershipTag);
    }
  }

  private String queryLdap(String username, String attribute, String value) throws LdapInfoException {
    Hashtable env = new Hashtable();
    DirContext dctx = null;

    env.put(Context.INITIAL_CONTEXT_FACTORY, LDAP_CONTEXT_FACTORY);

    env.put(Context.PROVIDER_URL, ldapInfoServer);

    try {
      dctx = new InitialDirContext(env);
    } catch (NamingException e) {
      throw(new LdapInfoException(e));
    }

    SearchControls sc = new SearchControls();
    String[] attributeFilter = { attribute };
    sc.setReturningAttributes(attributeFilter);
    sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
    String base = "";
    String filter = "cn=" + username;
    String out = "";
    try {
      NamingEnumeration results = dctx.search(base, filter, sc);
      while (results.hasMore()) {
        SearchResult sr = (SearchResult) results.next();
        Attributes attrs = sr.getAttributes();
        Attribute attr = attrs.get(attribute);
        for (int i = 0; i < attr.size(); i++) {
          out = (String) attr.get(i);
          if (value.equals(out)) {
            return "true";
          }
        }
      }
    } catch (NamingException e) {
      throw(new LdapInfoException(e));
    } finally {
      try {
        dctx.close();
      } catch (NamingException ex) {
        logger.error("dctx.close threw", ex);
        throw(new LdapInfoException(ex));
      }
    }
    return out;
  }

  private String queryLdap(String username) throws LdapInfoException {
    return queryLdap(username, spotIdTag ,"");
  }

  /**
   * Get the spotId for the given user
   * @param username
   *          the username to look up
   * @return The spotId of the user
   * @throws org.opencastproject.ldapinfo.api.LdapInfoException
   */
  public String getSpotId(String username) throws LdapInfoException {
    if (!ldapInfoServerEnabled) {
      throw new LdapInfoException("LDAP Information Server disabled");
    }
    logger.debug("getSpotId for {}", username);
    String out = queryLdap(username);
    logger.debug("SpotId for {} : {} ", username, out);
    return out;
  }

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
  public boolean isMemberOf(String username, String group) throws LdapInfoException {
    if (!ldapInfoServerEnabled) {
      throw new LdapInfoException("LDAP Information Server disabled");
    }
    logger.debug("{} isMemberOf {}", username, group);
    String out = queryLdap(username, groupMembershipTag , group);
    return "true".equals(out);
  }

}
