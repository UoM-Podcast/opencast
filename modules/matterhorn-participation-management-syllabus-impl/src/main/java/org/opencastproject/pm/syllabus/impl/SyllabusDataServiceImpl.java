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
  // service config properties
  private static final String LOCAL_PROPERTY = "local";
  private static final String REMOTE_URL_PROPERTY = "url";

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

    syllabusCaptureFilter.updateProperties(properties);
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

  /** OSGi container callback. */
  public void setSyllabusService(SyllabusService syllabusService) {
    this.syllabusService = syllabusService;
  }
}
