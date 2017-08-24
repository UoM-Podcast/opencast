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
package org.opencastproject.editui.endpoint;

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.TrustedHttpClientException;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.systems.MatterhornConstants;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 *
 * @author Tobias M Schiebeck
 */
abstract class RemoteRestEndpoint {

  /**
   * The logger
   */
  private static final Logger logger = LoggerFactory.getLogger(RemoteRestEndpoint.class);

  public abstract SecurityService getSecurityService();

  protected TrustedHttpClient trustedClient;

  public void setTrustedHttpClient(TrustedHttpClient client) {
    this.trustedClient = client;
  }

  public Response forwardRequest(String request, String method, String json) {
    return forwardRequest(request, method, json, null);
  }

  public Response forwardRequest(String request, String method, String json, List<BasicNameValuePair> params) {
    SecurityService secService = getSecurityService();
    Organization org = getSecurityService().getOrganization();
    secService.setUser(SecurityUtil.createSystemUser("admin", org));
    HttpResponse httpResponse = null;
    HttpRequestBase httpRequest = null;
    Response response = null;
    String adminHost = org.getProperties().get(MatterhornConstants.ADMIN_URL_ORG_PROPERTY);
    String url = adminHost + request;
    switch (method) {
      case "GET": {
        httpRequest = new HttpGet(url);
        break;
      }
      case "POST": {
        HttpPost httpPost = new HttpPost(url);
        if (null != json) {
          httpPost.setEntity(new StringEntity(json, ContentType.create("application/json")));
        } else {
          try {
            httpPost.setEntity(new UrlEncodedFormEntity(params));
          } catch (UnsupportedEncodingException ex) {
            logger.error("Can't encode parameters:" + url, ex);
          }
        }
        httpRequest = httpPost;
        break;
      }
      case "PUT": {
        HttpPut httpPut = new HttpPut(url);
        if (null != json) {
          httpPut.setEntity(new StringEntity(json, ContentType.create("application/json")));
        } else {
          try {
            httpPut.setEntity(new UrlEncodedFormEntity(params));
          } catch (UnsupportedEncodingException ex) {
            logger.error("Can't encode parameters:" + url, ex);
          }
        }
        httpRequest = httpPut;
        break;
      }
      default: {
        return response;// do nothing
      }
    }
    try {
      httpResponse = trustedClient.execute(httpRequest);
      int status = httpResponse.getStatusLine().getStatusCode();
      HttpEntity httpEntity = httpResponse.getEntity();
      if (httpEntity != null) {
        String type = httpEntity.getContentType().getValue();
        String data = IOUtils.toString(httpEntity.getContent());
        return Response.status(Status.fromStatusCode(status)).entity(data).type(type).build();
      }
    } catch (TrustedHttpClientException ex) {
      logger.error("Can't forward Rest Call:" + url, ex);
    } catch (IOException ex) {
      logger.error("Can't read Result content:" + url, ex);
    } finally {
      trustedClient.close(httpResponse);
    }
    return response;
  }
}
