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

import static org.opencastproject.util.OsgiUtil.getCfg;
import static org.opencastproject.util.OsgiUtil.getOptCfg;
import static org.opencastproject.util.data.Collections.map;
import static org.opencastproject.util.data.Tuple.tuple;
import static org.opencastproject.util.osgi.SimpleServicePublisher.ServiceReg.reg;

import org.opencastproject.pm.syllabus.api.SyllabusService;
import org.opencastproject.util.data.Effect;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.osgi.SimpleServicePublisher;
import org.opencastproject.util.persistence.PersistenceEnv;
import org.opencastproject.util.persistence.PersistenceUtil;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Dictionary;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
/**
 * {@link org.opencastproject.pm.syllabus.api.SyllabusService} publisher.
 */
public class SyllabusServicePublisher extends SimpleServicePublisher {

  /**
   * The logger
   */
  private static final Logger logger = LoggerFactory.getLogger(SyllabusServicePublisher.class);

  public static final String PERSISTENCE_UNIT = "org.opencastproject.pm.syllabus";

  @Override
  public ServiceReg registerService(Dictionary p, ComponentContext cc) throws ConfigurationException {
    try {
      final String identity = getOptCfg(p, "db.identity").getOrElse("Syllabus+");
      final String vendor = getOptCfg(p, "db.vendor").getOrElse("SQLServer");
      final String driver = getOptCfg(p, "db.driver").getOrElse("net.sourceforge.jtds.jdbc.Driver");
      final String url = getOptCfg(p, "db.url").orError(new ConfigurationException("db.url", "Syllabus+ database URL not specified")).get();
      final String user = getCfg(p, "db.user");
      final String pwd = getCfg(p, "db.password");
      final Option<String> schema = getOptCfg(p, "db.schema");

      logger.info("Set up connection to Syllabus database at {} with user {}", url, user);

      final ComboPooledDataSource ds = new ComboPooledDataSource();
      ds.setDescription(identity);
      ds.setDriverClass(driver);
      ds.setJdbcUrl(url);
      ds.setUser(user);
      ds.setPassword(pwd);
      try {
        ds.getConnection();
      } catch (SQLException e) {
        logger.error("Cannot connect to Syllabus database with url {} and user {}", url, user);
        ds.close();
        throw e;
      }
      if (schema.isSome()) {
        SyllabusSessionCustomizer.setSchemaName(schema.get());
        logger.info("Setting database schema to {}", schema.get());
      }

      PersistenceUtil.testConnection(ds).map(new Effect.X<SQLException>() {
        @Override
        protected void xrun(SQLException e) throws Exception {
          logger.error("Cannot connect to Syllabus database with url {} and user {}", url, user);
          ds.close();
          throw e;
        }
      });

      logger.info("Connection set to Syllabus database at {} with user {}", url, user);

      // config params taken from org.opencastproject.db.Activator
      final Map persistenceProps = map(tuple("javax.persistence.nonJtaDataSource", ds),
              tuple("eclipselink.target-database", vendor), tuple("eclipselink.logging.logger", "JavaLogger"),
              tuple("eclipselink.cache.shared.default", "false"),
              tuple("eclipselink.session.customizer", SyllabusSessionCustomizer.class.getName())
      );
      // create DB
      final EntityManagerFactory emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT, persistenceProps);
      final PersistenceEnv penv = PersistenceUtil.newPersistenceEnvironment(emf);

      final SyllabusService srv = new AbstractSyllabusService() {

        @Override
        protected PersistenceEnv getPenv() {
          return penv;
        }

        @Override
        public String getSourceDescription() {
          return identity;
        }

        @Override
        protected void closePenv() {
          penv.close();
        }
      };
      return reg(registerService(cc, srv, SyllabusService.class, "Syllabus+ database connector service"),
              new Effect0() {
        @Override
        protected void run() {
        }
      });
    } catch (Exception e) {
      logger.error("Cannot set up Syllabus database connection");
      throw new ConfigurationException("?", "see exception", e);
    }
  }

  @Override
  public boolean needConfig() {
    return true;
  }
}
