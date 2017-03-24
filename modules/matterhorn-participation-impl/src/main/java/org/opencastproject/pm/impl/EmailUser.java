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

package org.opencastproject.pm.impl;

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.User;

import java.util.Set;

/**
 * The EmailUser is a dummy user that can be used to transfer Name and emailAddress
 * through an EmailTemplate
 * 
 * @author Tobias M Schiebeck
 */
public class EmailUser implements User {

    private String name;

    private String email;

    EmailUser(String name, String email) {
        this.name = name;
        this.email = email;
    }

    @Override
    public String getUsername() {
        throw new UnsupportedOperationException("EmailUser does not support getUsername().");
    }

    @Override
    public String getPassword() {
        throw new UnsupportedOperationException("EmailUser does not support getPassword().");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getProvider() {
        throw new UnsupportedOperationException("EmailUser does not support getProvider().");
    }

    @Override
    public boolean isManageable() {
        return false;
    }

    @Override
    public boolean canLogin() {
        return false;
    }

    @Override
    public Organization getOrganization() {
        throw new UnsupportedOperationException("EmailUser does not support getOrganization().");
    }

    @Override
    public Set<Role> getRoles() {
        throw new UnsupportedOperationException("EmailUser does not support getRoles().");
    }

    @Override
    public boolean hasRole(String role) {
        return false;
    }

}
