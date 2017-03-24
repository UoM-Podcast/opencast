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

package org.opencastproject.requirement.impl.dass;

import org.opencastproject.requirement.api.RequirementService;
import org.opencastproject.requirement.api.RequirementServiceException;
import org.opencastproject.requirement.api.RequirementServiceResourceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

public abstract class AbstractDassRequirementService implements RequirementService {

  private static final String DASS_SQL_SELECT_USERS = "SELECT * FROM Podcast_Registration_Number";
  private static final String DASS_SQL_SELECT_COURSES = "SELECT * FROM Podcast_Course_Id";
  private static final String DASS_SQL_SELECT_USER = DASS_SQL_SELECT_USERS + " where spotid = ?";
  private static final String DASS_SQL_SELECT_COURSE = DASS_SQL_SELECT_COURSES + " where mleid = ?";

  protected abstract DataSource getDataSource();
  private static final Logger logger = LoggerFactory.getLogger(AbstractDassRequirementService.class);

  @Override
  public List<String> getIds(Resource target, Requirement requirement) throws
          RequirementServiceException {
    final String query;

    switch (requirement) {
      case RECORDING:
        switch (target) {
          case USER:
            query = DASS_SQL_SELECT_USERS;
            break;
          case SERIES:
            query = DASS_SQL_SELECT_COURSES;
            break;
          case EVENT:
          default:
            throw new RequirementServiceResourceException(
                    String.format("Requirement target {} is not supported", target.toString()));
        }
        break;
      case CAPTIONS:
      default:
        throw new RequirementServiceException(
                String.format("Requirement {} is not supported", requirement.toString()));
    }

    try (
            Connection sql = getDataSource().getConnection();
            Statement statem = sql.createStatement();
            ResultSet res = statem.executeQuery(query);) {

      List<String> ids = new ArrayList<String>();
      while (res.next()) {
        ids.add(res.getString(1));
      }

      return ids;
    } catch (SQLException e) {
      logger.error("Unable to retrieve ids from DASS DB");
      throw new RequirementServiceException("Unable to retrieve ids: " + e.getMessage());
    }
  }

  @Override
  public Boolean checkId(String id, Resource target, Requirement requirement) throws
          RequirementServiceException {
    final String query;

    switch (requirement) {
      case RECORDING:
        switch (target) {
          case USER:
            query = DASS_SQL_SELECT_USER;
            break;
          case SERIES:
            query = DASS_SQL_SELECT_COURSE;
            break;
          case EVENT:
          default:
            throw new RequirementServiceResourceException(
                    String.format("Requirement target {} is not supported", target.toString()));
        }
        break;
      case CAPTIONS:
      default:
        throw new RequirementServiceException(
                String.format("Requirement {} is not supported", requirement.toString()));
    }

    try (
            Connection sql = getDataSource().getConnection();
            PreparedStatement statem = sql.prepareStatement(query);) {
      statem.setString(1, id);
      ResultSet res = statem.executeQuery();

      return res.next();
    } catch (SQLException e) {
      logger.error("Unable to check id in DASS DB");
      throw new RequirementServiceException("Unable to check id: " + e.getMessage());

    }

  }
}
