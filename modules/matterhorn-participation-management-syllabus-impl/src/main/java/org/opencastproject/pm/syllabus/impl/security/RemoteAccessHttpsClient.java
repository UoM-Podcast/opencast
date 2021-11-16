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
package org.opencastproject.pm.syllabus.impl.security;

import static org.opencastproject.kernel.security.DelegatingAuthenticationEntryPoint.DIGEST_AUTH;
import static org.opencastproject.kernel.security.DelegatingAuthenticationEntryPoint.REQUESTED_AUTH_HEADER;
import static org.opencastproject.util.OsgiUtil.getCfg;
import static org.opencastproject.util.OsgiUtil.getOptCfg;

import org.opencastproject.security.api.SecurityConstants;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Dictionary;

import javax.net.ssl.SSLContext;

/*
 * Implements a HttpClient which makes requests to a specified host
 * using a client SSL certificate and Digest authentication
 */
public class RemoteAccessHttpsClient {
  protected static Logger logger = LoggerFactory.getLogger(RemoteAccessHttpsClient.class);

  public static final String REMOTE_HOST_PROPERTY = "remote.host";
  public static final String REMOTE_USER_PROPERTY = "remote.user";
  public static final String REMOTE_PASS_PROPERTY = "remote.password";
  public static final String KEY_FILENAME_PROPERTY = "key.filename";
  public static final String KEY_PASSPHRASE_PROPERTY = "key.passphrase";
  public static final String SSL_PROTOCOL_PROPERTY = "ssl.protocol";

  public static final String SSL_PROTOCOL_DEFAULT = "TLSv1.2";

  private String remoteHost;

  private final HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
  private final SSLContextBuilder sslContextBuilder = SSLContextBuilder.create();
  private final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

  private AuthScope authScope;
  private UsernamePasswordCredentials credentials;
  private SSLContext sslContext;

  public void activate(ComponentContext cc) throws ConfigurationException {
    modified(cc);
  }

  public void modified(ComponentContext cc) throws ConfigurationException {
    Dictionary<String,?> properties = cc.getProperties();

    remoteHost = getCfg(properties, REMOTE_HOST_PROPERTY);
    String user = getCfg(properties, REMOTE_USER_PROPERTY);
    String pass = getCfg(properties, REMOTE_PASS_PROPERTY);
    String protocol = getOptCfg(properties, SSL_PROTOCOL_PROPERTY).getOrElse(SSL_PROTOCOL_DEFAULT);
    String keyFilename = getCfg(properties, KEY_FILENAME_PROPERTY);
    String keyPassphrase = getCfg(properties, KEY_PASSPHRASE_PROPERTY);

    authScope = new AuthScope(remoteHost, AuthScope.ANY_PORT, AuthScope.ANY_REALM, AuthSchemes.DIGEST);
    credentials = new UsernamePasswordCredentials(user, pass);
    credentialsProvider.setCredentials(authScope, credentials);

    try {
      KeyStore keyStore = KeyStore.getInstance("PKCS12");
      keyStore.load(new FileInputStream(keyFilename), keyPassphrase.toCharArray());

      sslContext = sslContextBuilder.useProtocol(protocol).loadKeyMaterial(keyStore, keyPassphrase.toCharArray()).build();
    } catch (KeyStoreException | UnrecoverableKeyException | KeyManagementException | NoSuchAlgorithmException | CertificateException ex) {
      throw new ConfigurationException(KEY_FILENAME_PROPERTY, "Can't process client SSL certificate", ex);
    } catch (IOException ex) {
      throw new ConfigurationException(KEY_FILENAME_PROPERTY, "Can't open client SSL certificate", ex);
    }
  }

  private CloseableHttpClient makeClient() {
    return httpClientBuilder.setSSLContext(sslContext).setDefaultCredentialsProvider(credentialsProvider).build();
  }

  public HttpResponse execute(HttpUriRequest request) throws IOException {
    CloseableHttpClient client = makeClient();

    request.setHeader(REQUESTED_AUTH_HEADER, DIGEST_AUTH);
    request.setHeader(SecurityConstants.AUTHORIZATION_HEADER, "true");

    return client.execute(request);
  }

  public String getRemoteBaseAddress() {
    return "https://" + remoteHost;
  }
}
