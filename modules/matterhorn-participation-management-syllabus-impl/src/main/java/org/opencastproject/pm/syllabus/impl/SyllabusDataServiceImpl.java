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

import static org.opencastproject.util.OsgiUtil.getOptCfg;
import static org.opencastproject.util.OsgiUtil.getOptCfgAsBoolean;

import org.opencastproject.pm.syllabus.api.Activities;
import org.opencastproject.pm.syllabus.api.SyllabusCaptureFilter;
import org.opencastproject.pm.syllabus.api.SyllabusData;
import org.opencastproject.pm.syllabus.api.SyllabusDataService;
import org.opencastproject.pm.syllabus.api.SyllabusService;
import org.opencastproject.pm.syllabus.api.VActivityDateTime;
import org.opencastproject.pm.syllabus.api.VActivityLocation;
import org.opencastproject.pm.syllabus.api.VLocation;
import org.opencastproject.pm.syllabus.api.VModule;
import org.opencastproject.pm.syllabus.api.VStaff;
import org.opencastproject.pm.syllabus.impl.security.RemoteAccessHttpsClient;
import org.opencastproject.util.data.Option;

import org.joda.time.DateTime;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

public class SyllabusDataServiceImpl implements ManagedService, SyllabusDataService {
  // service config properties
  private static final String LOCAL_PROPERTY = "local";
  private static final String REMOTE_ENDPOINT_PROPERTY = "remote.endpoint";
  private static final String REMOTE_ENDPOINT_DEFAULT = "/syllabus";

  protected SyllabusCaptureFilter syllabusCaptureFilter;

  protected static Logger logger = LoggerFactory.getLogger(SyllabusDataServiceImpl.class);

  // is SyllabusService (S+ database) local or remote
  private Boolean local = true;

  // Must be valid is local == true
  private SyllabusService syllabusService;

  // Keep our own copy of this unchanging value;
  private String sourceDescription = null;

  // Handle remote connections securely
  protected RemoteAccessHttpsClient client = null;

  // Remote endpoint, default REMOTE_ENDPOINT_DEFAULT
  private String remoteEndpoint;

  public void activate(final ComponentContext cc) {
    logger.info("Activating {}", this.getClass().getName());
    syllabusCaptureFilter = new SyllabusCaptureFilterImpl();
  }

  @Override
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    Option<Boolean> bvalue = getOptCfgAsBoolean(properties, LOCAL_PROPERTY);
    if (bvalue.isSome()) {
      local = bvalue.get();
      logger.info("Setting service to {} mode", local ? "local" : "remote");
    }

    remoteEndpoint = getOptCfg(properties, REMOTE_ENDPOINT_PROPERTY).getOrElse(REMOTE_ENDPOINT_DEFAULT);

    if ('/' != remoteEndpoint.charAt(0)) {
      remoteEndpoint = "/" + remoteEndpoint;
    }

    logger.info("Setting service to {} mode", local ? "local" : "remote");
    syllabusCaptureFilter.updateProperties(properties);
  }

  @Override
  public SyllabusData fetch() {
    final SyllabusData data;

    if (local) {
      if (syllabusService != null) {
        data = new SyllabusDataImpl();
        data.fetch(syllabusService, syllabusCaptureFilter);
      } else {
        logger.error("Can't fetch data as no local SyllabusService");
        return null;
      }
    } else {
      logger.debug("Requesting all syllabus data");

      String url = client.getRemoteBaseAddress() + remoteEndpoint;
      data = RemoteObjectUtil.getResponseAsObject(client, url);
    }

    return data;
  }

  @Override
  public SyllabusData fetchModules() {
    final SyllabusData data;

    if (local) {
      if (syllabusService != null) {
        data = new SyllabusDataImpl();
        data.fetchModules(syllabusService);
      } else {
        logger.error("Can't fetch data as no local SyllabusService");
        return null;
      }
    } else {
      logger.debug("Requesting syllabus module data");

      String url = client.getRemoteBaseAddress() + remoteEndpoint + "?subset=modules";
      data = RemoteObjectUtil.getResponseAsObject(client, url);
    }

    return data;
  }

  @Override
  public String getSourceDescription() {
    if (sourceDescription == null || sourceDescription.isEmpty()) {
      if (local && syllabusService != null) {
         sourceDescription = syllabusService.getSourceDescription();
      } else {
        String url = client.getRemoteBaseAddress() + remoteEndpoint + "/description";
        sourceDescription = RemoteObjectUtil.getResponseAsObject(client, url);
      }
    }

    return sourceDescription;
  }

  @Override
  public VModule getModuleByCourseKey(String courseKey) {
    if (local && syllabusService != null) {
      return syllabusService.getModuleByCourseKey(courseKey);
    } else {
      String url = client.getRemoteBaseAddress() + remoteEndpoint + "/modules?coursekey=" + courseKey;
      final VModule module = RemoteObjectUtil.getResponseAsObject(client, url);

      return module;
    }
  }

  @Override
  public List<String> findModuleActivityIdsByCourseKey(String courseKey) {
    if (local && syllabusService != null) {
      return syllabusService.findModuleActivityIdsByCourseKey(courseKey);
    } else {
      String url = client.getRemoteBaseAddress() + remoteEndpoint + "/modules/activities/ids?coursekey=" + courseKey;
      List<String> activityIds = RemoteObjectUtil.getResponseAsObject(client, url);

      // Return the expected result if no activities found
      if (activityIds == null) {
        activityIds = new ArrayList<>();
      }
      return activityIds;
    }
  }

  @Override
  public List<VActivityDateTime> findActivityDateTimeByRange(String startActivityId, String endActivityId) {
    if (local && syllabusService != null) {
      return syllabusService.findActivityDateTimeByRange(startActivityId, endActivityId);
    } else {
      String url = client.getRemoteBaseAddress() + remoteEndpoint + "/activites/datetime"
              + "?startid=" + startActivityId
              + "&endid=" + endActivityId;
      final List<VActivityDateTime> datetimes = RemoteObjectUtil.getResponseAsObject(client, url);

      return datetimes;
    }
  }

  @Override
  public SyllabusCaptureFilter getSyllabusCaptureFilter() {
    return syllabusCaptureFilter;
  }

  @Override
  public List<Activities> findActivityByStaffId(String staffId) {
    if (local && syllabusService != null) {
      return syllabusService.findStaffActivities(staffId);
    } else {
      String url = client.getRemoteBaseAddress() + remoteEndpoint + "/activities/staff?staffid=" + staffId;
      final List<Activities> activities = RemoteObjectUtil.getResponseAsObject(client, url);

      return activities;
    }
  }

  @Override
  public List<Activities> findActivityByModule(String moduleName) {
    if (local && syllabusService != null) {
      return syllabusService.findModuleActivities(moduleName);
    } else {
      String url = client.getRemoteBaseAddress() + remoteEndpoint + "/activities/module?modulename=" + moduleName;
      final List<Activities> activities = RemoteObjectUtil.getResponseAsObject(client, url);

      return activities;
    }
  }

  @Override
  public List<Activities> findChildActivity(String activityId) {
    if (local && syllabusService != null) {
      return syllabusService.findChildActivities(activityId);
    } else {
      String url = client.getRemoteBaseAddress() + remoteEndpoint + "/activities/" + activityId + "/child";
      final List<Activities> activities = RemoteObjectUtil.getResponseAsObject(client, url);

      return activities;
    }
  }

  @Override
  public VStaff findStaffById(String spotId) {
    if (local && syllabusService != null) {
      return syllabusService.findStaffById(spotId);
    } else {
      String url = client.getRemoteBaseAddress() + remoteEndpoint + "/staff/" + spotId;
      final VStaff staff = RemoteObjectUtil.getResponseAsObject(client, url);

      return staff;
    }
  }

  @Override
  public List<VStaff> findStaffByStaffActivityId(String activityId) {
    if (local && syllabusService != null) {
      return syllabusService.findStaffByStaffActivityId(activityId);
    } else {
      String url = client.getRemoteBaseAddress() + remoteEndpoint + "/staff?activityid=" + activityId;
      final List<VStaff> staff = RemoteObjectUtil.getResponseAsObject(client, url);

      return staff;
    }
  }

  @Override
  public List<VActivityLocation> findByActivityLocationId(String activityId) {
    if (local && syllabusService != null) {
      return syllabusService.findByActivityLocationId(activityId);
    } else {
      String url = client.getRemoteBaseAddress() + remoteEndpoint + "/locations?activityid=" + activityId;
      final List<VActivityLocation> locations = RemoteObjectUtil.getResponseAsObject(client, url);

      return locations;
    }
  }

  @Override
  public List<VActivityDateTime> findActivityDateTime(String activityId) {
    if (local && syllabusService != null) {
      return syllabusService.findActivityDateTime(activityId);
    } else {
      String url = client.getRemoteBaseAddress() + remoteEndpoint + "/activities/" + activityId + "/datetime";
      final List<VActivityDateTime> activity = RemoteObjectUtil.getResponseAsObject(client, url);

      return activity;
    }
  }

  @Override
  public List<VLocation> findSuitabilityByLocationId(String locationId) {
    if (local && syllabusService != null) {
      return syllabusService.findSuitabilityByLocationId(locationId);
    } else {
      String url = client.getRemoteBaseAddress() + remoteEndpoint + "/suitabilities?locationid=" + locationId;
      final List<VLocation> suitability = RemoteObjectUtil.getResponseAsObject(client, url);

      return suitability;
    }
  }

  @Override
  public VLocation findLocationByName(String name) {
    if (local && syllabusService != null) {
      return syllabusService.findLocationByName(name);
    } else {
      String url = client.getRemoteBaseAddress() + remoteEndpoint + "/locations/" + name;
      final VLocation location = RemoteObjectUtil.getResponseAsObject(client, url);

      return location;
    }
  }

  @Override
  public List<Activities> findActivitiesByLocation(String location, DateTime start, DateTime end) {
    if (local && syllabusService != null) {
      return syllabusService.findActivitiesByLocation(location, start, end);
    } else {
      String url = client.getRemoteBaseAddress() + remoteEndpoint + "/activities/location?locationid=" + location + "&start=" + start + "&end=" + end;
      final List<Activities> activities = RemoteObjectUtil.getResponseAsObject(client, url);

      return activities;
    }
  }

  @Override
  public List<Activities> findParentActivities(String activityId) {
    if (local && syllabusService != null) {
      return syllabusService.findParentActivities(activityId);
    } else {
      String url = client.getRemoteBaseAddress() + remoteEndpoint + "/activities/" + activityId + "/parents";
      final List<Activities> activities = RemoteObjectUtil.getResponseAsObject(client, url);

      return activities;
    }
  }

  @Override
  public VModule getModuleById(String id) {
    if (local && syllabusService != null) {
      return syllabusService.getModuleById(id);
    } else {
      String url = client.getRemoteBaseAddress() + remoteEndpoint + "/modules/" + id;
      final VModule module = RemoteObjectUtil.getResponseAsObject(client, url);

      return module;
    }
  }

  @Override
  public List<VModule> getAllModule() {
    if (local && syllabusService != null) {
      return syllabusService.getAllModule();
    } else {
      String url = client.getRemoteBaseAddress() + remoteEndpoint + "/modules/all";
      final List<VModule> module = RemoteObjectUtil.getResponseAsObject(client, url);

      return module;
    }
  }

  /**
   * OSGi container callback.
   * @param syllabusService
   */
  public void setSyllabusService(SyllabusService syllabusService) {
    this.syllabusService = syllabusService;
  }

  /**
   * Sets the remote access https client
   *
   * @param client
   */
  public void setRemoteAccessHttpsClient(RemoteAccessHttpsClient client) {
    this.client = client;
  }

}
