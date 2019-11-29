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

import org.opencastproject.pm.syllabus.impl.security.RemoteAccessHttpsClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

/**
 * Util class to send and receive objects via http
 */
public final class RemoteObjectUtil {
  private RemoteObjectUtil() {
  }

  private static Logger logger = LoggerFactory.getLogger(RemoteObjectUtil.class);

  // Get object stream from url and deserialize
  public static <T> T getResponseAsObject(RemoteAccessHttpsClient client, String url) {

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
      logger.error("Can't connect to remote service at {}", url);
    }
    return null;
  }

  // Create a serialized object as response
  public static Response writeObjectResponse(final Object obj) throws IOException {
    StreamingOutput stream = new StreamingOutput() {
      @Override
      public void write(OutputStream os) throws IOException, WebApplicationException {
        ObjectOutputStream objectStream = new ObjectOutputStream(os);
        objectStream.writeObject(obj);
        objectStream.flush();
      }
    };

    return Response.ok(stream).type(MediaType.APPLICATION_OCTET_STREAM).build();
  }

    public static Response writeJson(Object object) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String moduleJson = mapper.writeValueAsString(object);
        return Response.ok(moduleJson).build();
    }
}
