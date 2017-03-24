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
import org.opencastproject.requirement.api.RequirementService.Requirement;
import org.opencastproject.requirement.api.RequirementService.Resource;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.List;


public class AbstractDassRequirementServiceTest {
  private static ComboPooledDataSource pooledDataSource;
  private static Connection connection;
  private static RequirementService reqService;

  @BeforeClass
  public static void setUpClass() throws Exception {
    pooledDataSource = new ComboPooledDataSource();
    pooledDataSource.setDriverClass("org.h2.Driver");
    pooledDataSource.setJdbcUrl("jdbc:h2:./target/db" + System.currentTimeMillis());
    pooledDataSource.setUser("sa");
    pooledDataSource.setPassword("sa");

    connection = pooledDataSource.getConnection();
    Statement statem = connection.createStatement();

    statem.execute("CREATE TABLE Podcast_Registration_Number as SELECT * FROM CSVREAD('./src/test/resources/test_dass_user.csv')");
    statem.execute("CREATE TABLE Podcast_Course_Id as SELECT * FROM CSVREAD('./src/test/resources/test_dass_course.csv')");

    reqService = new AbstractDassRequirementService() {
      @Override
      public ComboPooledDataSource getDataSource() {
        return pooledDataSource;
      }
    };
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    pooledDataSource.close();
  }

  /** Test that setUp has populated the database
   *
   * @throws Exception
   */
  @Test
  @Ignore
  public void testTestData() throws Exception {
    Statement statem = connection.createStatement();
    ResultSet result;

    result = statem.executeQuery("SELECT count(*) as n FROM dass_user");

    Assert.assertTrue(result.next());
    Assert.assertEquals(result.getInt(1), 4);

    result.close();

    result = statem.executeQuery("SELECT count(*) as n FROM dass_course");

    Assert.assertTrue(result.next());
    Assert.assertEquals(result.getInt(1), 4);

    result.close();
  }

  @Test
  public void testGetCourses() throws Exception {
    List courseIds = reqService.getIds(Resource.SERIES, Requirement.RECORDING);
    Assert.assertEquals(courseIds.size(), 4);
  }

  @Test
  public void testCheckUsers() throws Exception {
    String validUserId = "0000003";
    String invalidUserId = "NOT_VALID_ID";

    try {
      Boolean res = reqService.checkId(validUserId, Resource.USER, Requirement.RECORDING);
      Assert.assertTrue(res);
    } catch (Exception e) {
      throw e;
    }

    try {
      Boolean res = reqService.checkId(invalidUserId, Resource.USER, Requirement.RECORDING);
      Assert.assertFalse(res);
    } catch (Exception e) {
      throw e;
    }
  }

  @Test
  public void testUnsupportedRequirement() throws Exception {
    String validUserId = "0000003";

    try {
      reqService.getIds(Resource.SERIES, Requirement.CAPTIONS);
      Assert.fail();
    } catch (Exception e) {
      // do nothing
    }

    try {
      reqService.checkId(validUserId, Resource.USER, Requirement.CAPTIONS);
      Assert.fail();
    } catch (Exception e) {
      // do nothing
    }
  }

  @Test
  public void testUnsupportedRequirementTarget() throws Exception {
    String epId = "UNSUPPORTED_TARGET_ID";

    try {
      reqService.getIds(Resource.EVENT, Requirement.CAPTIONS);
      Assert.fail();
    } catch (Exception e) {
      // do nothing
    }

    try {
      reqService.checkId(epId, Resource.EVENT, Requirement.CAPTIONS);
      Assert.fail();
    } catch (Exception e) {
      // do nothing
    }
  }
}
