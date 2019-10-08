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

import org.opencastproject.pm.syllabus.api.SyllabusCaptureFilter;
import org.opencastproject.pm.syllabus.api.SyllabusData;
import org.opencastproject.pm.syllabus.api.SyllabusDataService;
import org.opencastproject.pm.syllabus.api.SyllabusService;
import org.opencastproject.pm.syllabus.api.VActivityDateTime;
import org.opencastproject.pm.syllabus.api.VModule;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.util.data.Option;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
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
  // service config properties
  private static final String LOCAL_PROPERTY = "local";
  private static final String REMOTE_URL_PROPERTY = "url";

  protected SyllabusCaptureFilter syllabusCaptureFilter;

  protected static Logger logger = LoggerFactory.getLogger(SyllabusDataServiceImpl.class);

  // is SyllabusService (S+ database) local or remote
  private Boolean local = true;

  // Must be valid is local == true
  private SyllabusService syllabusService;

  // Keep our own copy of this unchanging value;
  private String sourceDescription = null;

  // Handle remote connections securely
  protected TrustedHttpClient client = null;

  // URI of the base remote SyllabusEntityService. eg https://example.com/syllabus
  // Must be valid is local == false
  private URL remoteServiceURL;

  // Remote connection when local == false
  private HttpClient httpClient =  HttpClients.createDefault();;

  public void activate(final ComponentContext cc) {
    logger.info("Activating {}", this.getClass().getName());
    syllabusCaptureFilter = new SyllabusCaptureFilterImpl();
    httpClient = HttpClients.createDefault();
    try {
      updated(cc.getProperties());
    } catch (ConfigurationException e) {
      logger.debug("Couldn't read properties");
    }
  }

  @Override
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    Option<Boolean> bvalue = getOptCfgAsBoolean(properties, LOCAL_PROPERTY);
    if (bvalue.isSome()) {
      local = bvalue.get();
      logger.info("Setting service to {} mode", local ? "local" : "remote");
    }

    Option<String> value = getOptCfg(properties, REMOTE_URL_PROPERTY);
    if (value.isSome()) {
      try {
        remoteServiceURL = new URL(value.get());
      } catch (MalformedURLException e) {
        logger.error("Remote URL is invalid: ", e);
      }
    }

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

      String url = remoteServiceURL.toString();
      data = getResponseAsObject(url);
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

      String url = remoteServiceURL.toString() + "?subset=modules";
      data = getResponseAsObject(url);
    }

    return data;
  }

  @Override
  public String getSourceDescription() {
    if (sourceDescription == null || sourceDescription.isEmpty()) {
      if (local && syllabusService != null) {
         sourceDescription = syllabusService.getSourceDescription();
      } else {
        String url = remoteServiceURL.toString() + "/description";
        sourceDescription = getResponseAsObject(url);
      }
    }

    return sourceDescription;
  }

  @Override
  public VModule getModuleByCourseKey(String courseKey) {
    if (local && syllabusService != null) {
      return syllabusService.getModuleByCourseKey(courseKey);
    } else {
      String url = remoteServiceURL.toString() + "/modules?coursekey=" + courseKey;
      final VModule module = getResponseAsObject(url);

      return module;
    }
  }

  @Override
  public List<String> findModuleActivityIdsByCourseKey(String courseKey) {
    if (local && syllabusService != null) {
      return syllabusService.findModuleActivityIdsByCourseKey(courseKey);
    } else {
      String url = remoteServiceURL.toString() + "/modules/activites/ids?coursekey=" + courseKey;
      final List<String> activityIds = getResponseAsObject(url);

      return activityIds;
    }
  }

  @Override
  public List<VActivityDateTime> findActivityDateTimeByRange(String startActivityId, String endActivityId) {
    if (local && syllabusService != null) {
      return syllabusService.findActivityDateTimeByRange(startActivityId, endActivityId);
    } else {
      String url = remoteServiceURL.toString() + "/activites/datetime"
              + "?startid=" + startActivityId
              + "&endid=" + endActivityId;
      final List<VActivityDateTime> datetimes = getResponseAsObject(url);

      return datetimes;
    }
  }

  @Override
  public SyllabusCaptureFilter getSyllabusCaptureFilter() {
    return syllabusCaptureFilter;
  }

  private <T> T getResponseAsObject(String url) {
    try {
      HttpGet get = new HttpGet(url);
      HttpResponse response = client.execute(get);
      T object = null;
      if (response != null && response.getStatusLine().getStatusCode() == 200) {
        final HttpEntity entity = response.getEntity();

        if (entity != null) {
          try (InputStream stream = entity.getContent()) {
            ObjectInputStream objStream = new ObjectInputStream(stream);
            object = (T) objStream.readObject();
            return object;
          } catch (IOException e) {
            logger.error("Can't read object stream:", e);
            return null;
          } catch (ClassNotFoundException ee) {
            logger.error("Class not found: {}", ee.getMessage());
            return object;
          }
        }
      }
    } catch (IOException e) {
      logger.error("Can't connect to remote SyllabusDataService");
    }
    return null;
  }

  /**
   * OSGi container callback.
   * @param syllabusService
   */
  public void setSyllabusService(SyllabusService syllabusService) {
    this.syllabusService = syllabusService;
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
