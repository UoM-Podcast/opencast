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
import static org.opencastproject.util.osgi.SimpleServicePublisher.ServiceReg.reg;
import static org.opencastproject.util.osgi.SimpleServicePublisher.registerService;

import org.opencastproject.requirement.api.RequirementService;
import org.opencastproject.requirement.impl.dass.AbstractDassRequirementService;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.util.osgi.SimpleServicePublisher;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Dictionary;


/** {@link uk.ac.manchester.requirement.RequirementService} publisher. */
public class DassRequirementServicePublisher extends SimpleServicePublisher {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(DassRequirementServicePublisher.class);

@Override
  public SimpleServicePublisher.ServiceReg registerService(Dictionary p, ComponentContext cc) throws ConfigurationException {
    try {
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

      // create DassService
      final RequirementService srv = new AbstractDassRequirementService() {
        @Override
        public ComboPooledDataSource getDataSource() {
          return dataSource;
        }
      };

      return reg(registerService(cc, srv, RequirementService.class, "DASS database connector service"),
              new Effect0() {
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
}
