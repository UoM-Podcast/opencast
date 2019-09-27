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

import static org.opencastproject.pm.syllabus.api.SyllabusService.ACTIVITY_TYPE_ANY;
import static org.opencastproject.util.OsgiUtil.getCfg;
import static org.opencastproject.util.OsgiUtil.getOptCfg;
import static org.opencastproject.util.OsgiUtil.getOptCfgAsBoolean;

import org.opencastproject.pm.syllabus.api.SyllabusCaptureFilter;
import org.opencastproject.pm.syllabus.api.SyllabusData;
import org.opencastproject.pm.syllabus.api.SyllabusDataService;
import org.opencastproject.pm.syllabus.api.SyllabusService;
import org.opencastproject.pm.syllabus.api.VActivityDateTime;
import org.opencastproject.pm.syllabus.api.VModule;
import org.opencastproject.util.data.Option;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.List;

public class SyllabusDataServiceImpl implements ManagedService, SyllabusDataService {

  private static final String LOCAL_PROPERTY = "local";
  private static final String REMOTE_URL_PROPERTY = "url";

  // room filter paramaters
  private static final String CAPTURE_ROOMS_PROPERTY = "capture.rooms";
  private static final String CAPTURE_ROOM_PROPERTY = "capture.room";
  private static final String[] CAPTURE_ROOM_PROPS = {"name", "id", "inputs"};

  private static final String CAPTURE_TYPES_PROPERTY = "capture.types";
  private static final String CAPTURE_TYPE_PROPERTY = "capture.type";
  private static final String[] CAPTURE_TYPE_PROPS = {"name", "id"};

  private SyllabusCaptureFilter syllabusCaptureFilter;

  private static final Logger logger = LoggerFactory.getLogger(SyllabusDataServiceImpl.class);

  // is SyllabusService (S+ database) local or remote
  private Boolean local = true;

  // Must be valid is local == true
  private SyllabusService syllabusService;

  // URI of the base remote SyllabusEntityService. eg https://example.com/syllabus
  // Must be valid is local == false
  private URL remoteServiceURL;

  // Remote connection when local == false
  private HttpClient httpClient;

  public void activate(final ComponentContext cc) {
    logger.info("Start Syllabus Data Service");
    syllabusCaptureFilter = new SyllabusCaptureFilterImpl();
  }

  @Override
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    Option<Boolean> bvalue = getOptCfgAsBoolean(properties, LOCAL_PROPERTY);
    if (bvalue.isSome()) {
      local = bvalue.get();
    }

    Option<String> value = getOptCfg(properties, REMOTE_URL_PROPERTY);
    if (value.isSome()) {
      try {
       remoteServiceURL = new URL(value.get());
      } catch (MalformedURLException e) {
        logger.error("Remote URL is invalid: ", e);
      }
    }


    setParticipationProperties(properties);
  }

  public void setParticipationProperties(Dictionary properties) throws ConfigurationException {
    final String rooms = getCfg(properties, CAPTURE_ROOMS_PROPERTY);

    for (String room : rooms.split(",")) {
      for (String prop : CAPTURE_ROOM_PROPS) {
        Option<String> value = getOptCfg(properties, CAPTURE_ROOM_PROPERTY + "." + room.trim() + "." + prop);
        if (value.isSome()) {
          syllabusCaptureFilter.addCaptureRoomProperty(room.trim(), prop, value.get());
        }
      }
    }
    final Option<String> typesOption = getOptCfg(properties, CAPTURE_TYPES_PROPERTY);
    if (typesOption.isSome()) {
      String types = typesOption.get();
      for (String type : types.split(",")) {
        for (String prop : CAPTURE_TYPE_PROPS) {
          Option<String> value = getOptCfg(properties, CAPTURE_TYPE_PROPERTY + "." + type.trim() + "." + prop);
          if (value.isSome()) {
            syllabusCaptureFilter.addCaptureActivityTypeProperty(type.trim(), prop, value.get());
          }
        }
      }
    } else {
      syllabusCaptureFilter.addCaptureActivityTypeProperty("ANY", "id", ACTIVITY_TYPE_ANY);
    }
  }

  @Override
  public SyllabusData fetch() {
    if (local) {
      if (syllabusService != null) {
        return fetchLocal();
      } else {
        logger.error("Can't fetch data as no local SyllabusService");

      }
    }
    return null;
  }

  @Override
  public SyllabusData fetchModules() {
    if (local) {
      if (syllabusService != null) {
        return fetchModulesLocal();
      } else {
        logger.error("Can't fetch data as no local SyllabusService");

      }
    }
    return null;
  }

  private SyllabusData fetchLocal() {
    SyllabusData data = new SyllabusDataImpl();
    data.fetch(syllabusService, syllabusCaptureFilter);

    return data;
  }

  private SyllabusData fetchModulesLocal() {
    SyllabusData data = new SyllabusDataImpl();
    data.fetchModules(syllabusService);

    return data;
  }

  private SyllabusData fetchRemote() {
    String url = remoteServiceURL.toString();

    logger.debug("Requesting all syllabus data");

    HttpResponse response = null;
    try {
      HttpGet get = new HttpGet(url);
      response = httpClient.execute(get);
      if (response != null) {
        final SyllabusData data;
        final HttpEntity entity = response.getEntity();

        if (entity != null) {
          try (InputStream stream = entity.getContent()) {
            ObjectInputStream objStream = new ObjectInputStream(stream);
            data = (SyllabusData) objStream.readObject();
            return data;
          } catch (IOException e) {
            logger.error("Can't reading object stream:", e);
            return null;
          } catch (ClassNotFoundException ee) {
            logger.error(ee.getMessage());
            return null;
          }
        }
      }
    } catch (IOException e) {
      logger.error("Can't connect to remote SyllabusEntityService");
    }

    return null;
  }

  public SyllabusData fetchModulesRemote() {
    String url = remoteServiceURL.toString() + "/modules";
    final SyllabusData data = null;
    logger.debug("Requesting syllabus module data");

    return data;
  }

  @Override
  public String getSourceDescription() {
    if (local && syllabusService != null) {
      return syllabusService.getSourceDescription();
    } else {
      // do remote thing
      logger.error("getModuleByCourseKey remote not implemented");
      return null;
    }
  }

  @Override
  public VModule getModuleByCourseKey(String courseKey) {
    if (local && syllabusService != null) {
      return syllabusService.getModuleByCourseKey(courseKey);
    } else {
      // do remote thing
      logger.error("getModuleByCourseKey remote not implemented");
      return null;
    }
  }

  @Override
  public List<String> findModuleActivityIdsByCourseKey(String courseKey) {
    if (local && syllabusService != null) {
      return syllabusService.findModuleActivityIdsByCourseKey(courseKey);
    } else {
      // do remote thing
      logger.error("getModuleByCourseKey remote not implemented");
      return null;
    }
  }

  @Override
  public List<VActivityDateTime> findActivityDateTimeByRange(String startActivityId, String endActivityId) {
    if (local && syllabusService != null) {
      return syllabusService.findActivityDateTimeByRange(startActivityId, endActivityId);
    } else {
      // do remote thing
      logger.error("getModuleByCourseKey remote not implemented");
      return null;
    }
  }

  @Override
  public SyllabusCaptureFilter getSyllabusCaptureFilter() {
    return syllabusCaptureFilter;
  }

  public void setRemoteServiceURL(URL remoteServiceURL) {
    this.remoteServiceURL = remoteServiceURL;
  }

  /** OSGi container callback. */
  public void setSyllabusService(SyllabusService syllabusService) {
    this.syllabusService = syllabusService;
  }
}
