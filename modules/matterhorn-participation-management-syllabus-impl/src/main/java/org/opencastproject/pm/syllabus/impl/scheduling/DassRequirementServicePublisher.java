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

import static org.opencastproject.util.OsgiUtil.getCfg;
import static org.opencastproject.util.OsgiUtil.getOptCfg;
import static org.opencastproject.util.OsgiUtil.getOptCfgAsBoolean;
import static org.opencastproject.util.data.Collections.dict;
import static org.opencastproject.util.data.Tuple.tuple;
import static org.opencastproject.util.osgi.SimpleServicePublisher.ServiceReg.reg;

import static org.osgi.framework.Constants.SERVICE_DESCRIPTION;
import static org.osgi.framework.Constants.SERVICE_PID;

import org.opencastproject.pm.syllabus.impl.RemoteObjectUtil;
import org.opencastproject.pm.syllabus.impl.security.RemoteAccessHttpsClient;
import org.opencastproject.requirement.api.RequirementService;
import org.opencastproject.requirement.api.RequirementServiceException;
import org.opencastproject.requirement.impl.dass.AbstractDassRequirementService;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.osgi.SimpleServicePublisher;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyVetoException;
import java.sql.SQLException;
import java.util.Dictionary;
import java.util.List;


/** {@link uk.ac.manchester.requirement.RequirementService} publisher. */
public class DassRequirementServicePublisher extends SimpleServicePublisher {

  // service config properties
  private static final String LOCAL_PROPERTY = "local";
  private static final String REMOTE_ENDPOINT_PROPERTY = "remote.endpoint";
  private static final String REMOTE_ENDPOINT_DEFAULT = "/requirements";

  /** The logger */
  protected static Logger logger = LoggerFactory.getLogger(DassRequirementServicePublisher.class);

  private Boolean local = true;

  // Handle remote connections securely
  protected RemoteAccessHttpsClient client = null;
  protected String remoteEndpoint;
  @Override
  public SimpleServicePublisher.ServiceReg registerService(Dictionary p, ComponentContext cc) throws ConfigurationException {
    try {

      final String providerName = cc.getServiceReference().getProperty(RequirementService.REQUIREMENTS_PROVIDER_PROPERTY).toString();

      Option<Boolean> bvalue = getOptCfgAsBoolean(p, LOCAL_PROPERTY);
      if (bvalue.isSome()) {
        local = bvalue.get();
        logger.info("Setting service to {} mode", local ? "local" : "remote");
      }

      // Either create a local or a remotely implmentation of the Requirement Service
      final RequirementService srv;

      if (local) {
        final String identity = getOptCfg(p, "db.identity").getOrElse("dass");
        final String driver = getOptCfg(p, "db.driver").getOrElse("net.sourceforge.jtds.jdbc.Driver");
        final String url = getOptCfg(p, "db.url").orError(new ConfigurationException("db.url", "DASS database URL not specified")).get();
        final String user = getCfg(p, "db.user");
        final String pwd = getCfg(p, "db.password");

        final ComboPooledDataSource dataSource = new ComboPooledDataSource();
        dataSource.setDescription(identity);
        dataSource.setDriverClass(driver);
        dataSource.setJdbcUrl(url);
        dataSource.setUser(user);
        dataSource.setPassword(pwd);

        logger.info("Setting up DASS Database connection");

        try {
          dataSource.getConnection();
          logger.info("Connected to DASS database");
        } catch (SQLException e) {
          logger.error("Cannot connect to DASS database with url {} and user {}", url, user);
          dataSource.close();
          throw e;
        }

        srv = new AbstractDassRequirementService() {
          @Override
          public ComboPooledDataSource getDataSource() {
            return dataSource;
          }

          @Override
          public String getProviderName() {
            return providerName;
          }
        };


      } else {
        remoteEndpoint = getOptCfg(p, REMOTE_ENDPOINT_PROPERTY).getOrElse(REMOTE_ENDPOINT_DEFAULT);

        if ('/' != remoteEndpoint.charAt(0)) {
          remoteEndpoint = "/" + remoteEndpoint;
        }

        final String remoteEndpointURL = client.getRemoteBaseAddress() + remoteEndpoint;

        srv = new RequirementService() {
          @Override
          public String getProviderName() {
            return providerName;
          }

          @Override
          public List<String> getIds(RequirementService.Resource resource, RequirementService.Requirement requirement) throws RequirementServiceException {
            String url = String.format("%s%s/providers/%s/resources/%s?requirement=%s",
                client.getRemoteBaseAddress(), remoteEndpoint, providerName, resource, requirement);

            return RemoteObjectUtil.getResponseAsObject(client, url);
          }

          @Override
          public Boolean checkId(String id, RequirementService.Resource resource, RequirementService.Requirement requirement) throws RequirementServiceException {
            String url = String.format("%s%s/providers/%s/resources/%s?requirement=%s&id=%s",
                client.getRemoteBaseAddress(), remoteEndpoint, providerName, resource, requirement, id);

            List<String> resources = RemoteObjectUtil.getResponseAsObject(client, url);
            if (resources != null && !resources.isEmpty()) {
              return true;
            }

            return false;
          }
        };
      }

      ServiceRegistration sr = registerService(cc, srv, RequirementService.class, "DASS database connector service");
      // FIXME: Hack to add our own properties
      sr.setProperties(dict(
              tuple(SERVICE_PID, srv.getClass().getName()),
              tuple(SERVICE_DESCRIPTION, "DASS database connector service"),
              tuple("opencast.service.local", local.toString())));
      return reg(sr, new Effect0() {
        @Override
        protected void run() {
        }
      });
    } catch (PropertyVetoException | SQLException | ConfigurationException e) {
      logger.error("Cannot set up DASS requirement service");
      throw new ConfigurationException("?", "see exception", e);
    }
  }

  @Override
  public boolean needConfig() {
    return true;
  }

  /**
   * Sets the trusted http client
   *
   * @param client
   */
  public void setRemoteAccessHttpsClient(RemoteAccessHttpsClient client) {
    this.client = client;
  }
}
