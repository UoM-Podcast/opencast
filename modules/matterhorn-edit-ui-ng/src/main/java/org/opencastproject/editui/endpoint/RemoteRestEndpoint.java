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
import org.opencastproject.serviceregistry.api.ServiceRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 *
 * @author Tobias M Schiebeck
 */
abstract class RemoteRestEndpoint {
  private static final Logger logger = LoggerFactory.getLogger(RemoteRestEndpoint.class);

  private ServiceRegistry serviceRegistry;
  private String adminHost;

  public abstract SecurityService getSecurityService();

  public ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  public void setAdminHostFromRegistry(ServiceRegistry serviceRegistry) {
    try {
      List<ServiceRegistration> regs = serviceRegistry.getServiceRegistrationsByType("org.opencastproject.adminui.endpoint.tools");
      if (regs.size() > 0) {
        adminHost = regs.get(0).getHost();
      }
    } catch (ServiceRegistryException e) {
      logger.error("Can't set admin host", e.getMessage());
    }
    this.serviceRegistry = serviceRegistry;
  }

  protected TrustedHttpClient trustedClient;

  public void setTrustedHttpClient(TrustedHttpClient client) {
    this.trustedClient = client;
  }

  public Response forwardRequest(String uri, HttpServletRequest request, String json) {
    return forwardRequest(uri, request, json, null);
  }

  public Response forwardRequest(String uri, HttpServletRequest request, String json, List<BasicNameValuePair> params) {
    SecurityService secService = getSecurityService();
    Organization org = getSecurityService().getOrganization();
    // FIXME: this should be set once at the implementation level
    secService.setUser(SecurityUtil.createSystemUser("admin", org));
    HttpResponse httpResponse = null;
    HttpRequestBase httpRequest = null;
    Response response = null;

    // There is a chance that adminHost is unset is this service starts before
    // adminui.enpoint service is registered
    if (adminHost.isEmpty()) {
      setAdminHostFromRegistry(getServiceRegistry());
    }

    String url = adminHost + uri;
    String sessionId = request.getRequestedSessionId();
    logger.debug("Forwarding request: {} {}", request.getMethod(), url);
    switch (request.getMethod()) {
      case "DELETE": {
        httpRequest = new HttpDelete(url);
        break;
      }
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
            logger.error("Can't encode parameters", ex);
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
            logger.error("Can't encode parameters", ex);
          }
        }
        httpRequest = httpPut;
        break;
      }
      default: {
        logger.error("Can't handle request method: {}", request.getMethod());
        return response; // do nothing
      }
    }
    try {
      httpRequest.addHeader("X-Forwarded-SessionId", sessionId);
      httpResponse = trustedClient.execute(httpRequest);
      int status = httpResponse.getStatusLine().getStatusCode();
      HttpEntity httpEntity = httpResponse.getEntity();
      if (httpEntity != null) {
        String type = httpEntity.getContentType().getValue();
        String data = IOUtils.toString(httpEntity.getContent());
        return Response.status(Status.fromStatusCode(status)).entity(data).type(type).build();
      }
    } catch (TrustedHttpClientException ex) {
      logger.error("Can't forward rest call: {}", url, ex);
    } catch (IOException ex) {
      logger.error("Can't read repsonse content: {}", url, ex);
    } finally {
      trustedClient.close(httpResponse);
    }
    return response;
  }
}
