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
import static org.opencastproject.util.osgi.SimpleServicePublisher.registerService;

import static org.osgi.framework.Constants.SERVICE_DESCRIPTION;
import static org.osgi.framework.Constants.SERVICE_PID;

import org.opencastproject.pm.syllabus.impl.RemoteObjectUtil;
import org.opencastproject.requirement.api.RequirementService;
import org.opencastproject.requirement.api.RequirementServiceException;
import org.opencastproject.requirement.impl.dass.AbstractDassRequirementService;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.osgi.SimpleServicePublisher;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Dictionary;
import java.util.List;


/** {@link uk.ac.manchester.requirement.RequirementService} publisher. */
public class DassRequirementServicePublisher extends SimpleServicePublisher {

  // service config properties
  private static final String LOCAL_PROPERTY = "local";
  private static final String REMOTE_URL_PROPERTY = "url";

  /** The logger */
  protected static Logger logger = LoggerFactory.getLogger(DassRequirementServicePublisher.class);

  private Boolean local = true;

  // Handle remote connections securely
  protected TrustedHttpClient client = null;

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
        final String driver = getOptCfg(p, "db.driver").getOrElse("com.mysql.jdbc.Driver");
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
        final URL remoteServiceURL;
        Option<String> value = getOptCfg(p, REMOTE_URL_PROPERTY);
        if (value.isSome()) {
          try {
            remoteServiceURL = new URL(value.get());
          } catch (MalformedURLException e) {
            throw new ConfigurationException(REMOTE_URL_PROPERTY, "Remote URL is invalid: ", e);
          }
        } else {
          throw new ConfigurationException(REMOTE_URL_PROPERTY, "Remote URL must be set for remote service");
        }

        srv = new RequirementService() {
          @Override
          public String getProviderName() {
            return providerName;
          }

          @Override
          public List<String> getIds(RequirementService.Resource resource, RequirementService.Requirement requirement) throws RequirementServiceException {
            String url = String.format("%s/%s/%s/%s/entities/ids", remoteServiceURL.toString(), providerName, resource, requirement);

            return RemoteObjectUtil.getResponseAsObject(client, url);
          }

          @Override
          public Boolean checkId(String id, RequirementService.Resource resource, RequirementService.Requirement requirement) throws RequirementServiceException {
            String url = String.format("%s/%s/%s/%s/entities/%s", remoteServiceURL.toString(), providerName, resource, requirement, id);

            try {
              HttpGet get = new HttpGet(url);
              HttpResponse response = client.execute(get);

              if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                return true;
              }
            } catch (IOException e) {
              logger.error("Can't connect to remote service at {}", url);
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
    } catch (Exception e) {
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
  public void setTrustedHttpClient(TrustedHttpClient client) {
    this.client = client;
  }
}
