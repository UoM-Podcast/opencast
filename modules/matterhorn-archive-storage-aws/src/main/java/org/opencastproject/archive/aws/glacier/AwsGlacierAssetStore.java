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

package org.opencastproject.archive.aws.glacier;

import static org.opencastproject.util.IoSupport.file;

import org.opencastproject.archive.api.Version;
import org.opencastproject.archive.aws.AwsAbstractArchive;
import org.opencastproject.archive.aws.AwsUploadOperationResult;
import org.opencastproject.archive.aws.persistence.AwsAssetDatabaseException;
import org.opencastproject.archive.aws.persistence.AwsAssetMapping;
import org.opencastproject.archive.base.StoragePath;
import org.opencastproject.archive.base.storage.ElementStoreException;
import org.opencastproject.archive.base.storage.RemoteElementStore;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.OsgiUtil;
import org.opencastproject.util.data.Option;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.glacier.AmazonGlacierClient;
import com.amazonaws.services.glacier.AmazonGlacierClientBuilder;
import com.amazonaws.services.glacier.model.CreateVaultRequest;
import com.amazonaws.services.glacier.model.DeleteArchiveRequest;
import com.amazonaws.services.glacier.model.DescribeVaultOutput;
import com.amazonaws.services.glacier.model.ListVaultsRequest;
import com.amazonaws.services.glacier.model.ListVaultsResult;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManager;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManagerBuilder;
import com.amazonaws.services.glacier.transfer.UploadResult;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Dictionary;
import java.util.List;

public class AwsGlacierAssetStore extends AwsAbstractArchive {

  // Since Glacier does not do versioning the say S3 does, we use a static version string instead
  public static final String GLACIER_VERSION = "GLACIER";

  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(AwsGlacierAssetStore.class);

  // Service configuration
  public static final String AWS_GLACIER_ENABLED = "org.opencastproject.archive.aws.glacier.enabled";
  public static final String AWS_GLACIER_ACCESS_KEY_ID_CONFIG = "org.opencastproject.archive.aws.glacier.access.id";
  public static final String AWS_GLACIER_SECRET_ACCESS_KEY_CONFIG = "org.opencastproject.archive.aws.glacier.secret.key";
  public static final String AWS_GLACIER_REGION_CONFIG = "org.opencastproject.archive.aws.glacier.region";
  public static final String AWS_GLACIER_VAULT_CONFIG = "org.opencastproject.archive.aws.glacier.vault";

  /** The AWS client and transfer manager */
  private AmazonGlacierClient glacierClient = null;
  private AmazonSQSClient sqsClient = null;
  private AmazonSNSClient snsClient = null;
  private ArchiveTransferManager atm = null;

  /** The AWS Glacier vault name */
  private String vaultName = null;

  //The local cache of files which have been get()ed from Glacier
  private File localCacheRoot = null;

  /**
   * Service activator, called via declarative services configuration.
   *
   * @param cc
   *          the component context
   */
  public void activate(final ComponentContext cc) throws IllegalStateException, IOException {
    // Get the configuration
    if (cc != null) {
      @SuppressWarnings("rawtypes") Dictionary properties = cc.getProperties();

      boolean enabled = Boolean.parseBoolean(StringUtils.trimToEmpty((String) properties.get(AWS_GLACIER_ENABLED)));
      if (!enabled) {
        logger.info("AWS Glacier asset store is disabled");
        return;
      }

      // Store type: "aws-glacier"
      storeType = StringUtils.trimToEmpty((String) properties.get(RemoteElementStore.STORE_TYPE_PROPERTY));
      if (StringUtils.isEmpty(storeType)) {
        throw new ConfigurationException("Invalid store type value");
      }
      logger.info("{} is: {}", RemoteElementStore.STORE_TYPE_PROPERTY, storeType);

      localCacheRoot = createCacheDirectory(cc, storeType);

      // AWS Glacier vault name
      vaultName = getAWSConfigKey(cc, AWS_GLACIER_VAULT_CONFIG);
      logger.info("AWS Glacier vault name is {}", vaultName);

      // AWS region
      regionName = getAWSConfigKey(cc, AWS_GLACIER_REGION_CONFIG);
      logger.info("AWS region is {}", regionName);

      // Explicit credentials are optional.
      AWSCredentialsProvider provider = null;
      Option<String> accessKeyIdOpt = OsgiUtil.getOptCfg(cc.getProperties(), AWS_GLACIER_ACCESS_KEY_ID_CONFIG);
      Option<String> accessKeySecretOpt = OsgiUtil.getOptCfg(cc.getProperties(), AWS_GLACIER_SECRET_ACCESS_KEY_CONFIG);

      // Keys not informed so use default credentials provider chain, which
      // will look at the environment variables, java system props, credential files, and instance
      // profile credentials
      if (accessKeyIdOpt.isNone() && accessKeySecretOpt.isNone())
        provider = new DefaultAWSCredentialsProviderChain();
      else
        provider = new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(accessKeyIdOpt.get(), accessKeySecretOpt.get()));

      glacierClient = (AmazonGlacierClient) AmazonGlacierClientBuilder.standard()
              .withRegion(regionName)
              .withCredentials(provider)
              .build();


      sqsClient = (AmazonSQSClient) AmazonSQSClientBuilder.standard()
              .withRegion(regionName)
              .withCredentials(provider)
              .build();

      snsClient = (AmazonSNSClient) AmazonSNSClientBuilder.standard()
              .withRegion(regionName)
              .withCredentials(provider)
              .build();

      ArchiveTransferManagerBuilder atmb = new ArchiveTransferManagerBuilder();
      atmb.setGlacierClient(glacierClient);
      atmb.setSnsClient(snsClient);
      atmb.setSqsClient(sqsClient);
      atm = atmb.build();

      logger.info("AwsGlacierAssetStore activated with key id beginning {}!", provider.getCredentials().getAWSAccessKeyId().substring(0, 5));
      createAWSVault();
    }

  }

  /**
   * Creates the AWS Glacier vault if it doesn't exist yet.
   */
  private void createAWSVault() {
    //List all the vaults
    ListVaultsRequest listVaultsRequest = new ListVaultsRequest();
    ListVaultsResult listVaultsResult = glacierClient.listVaults(listVaultsRequest);

    logger.debug("Listing all vaults");
    List<DescribeVaultOutput> vaultList = listVaultsResult.getVaultList();
    //Does the vault exist?
    for (DescribeVaultOutput output : vaultList) {
      if (vaultName.equals(output.getVaultName())) {
        logger.debug("Configured vault has been found");
        return;
      }
    }

    logger.debug("Attempting to create vault {}", vaultName);
    CreateVaultRequest createVaultRequest = new CreateVaultRequest()
            .withVaultName(vaultName);
    glacierClient.createVault(createVaultRequest);
  }

  protected AwsUploadOperationResult uploadObject(File origin, String objectName, Option<MimeType> mimeType) {
    String archiveId;
    try {
      logger.info("Uploading {} to Glacier vault {}...", objectName, vaultName);
      UploadResult upload = atm.upload(vaultName, objectName, origin);
      archiveId = upload.getArchiveId();
    } catch (FileNotFoundException e) {
      throw new ElementStoreException("Unable to find " + origin.getAbsolutePath() + " on disk!", e);
    } catch (Exception e) {
      throw new ElementStoreException("Unexpected exception!", e);
    }
    return new AwsUploadOperationResult(archiveId, GLACIER_VERSION);
  }

  public InputStream getObject(AwsAssetMapping map) throws ElementStoreException {
    StoragePath localPath = new StoragePath(map.getOrganizationId(), map.getMediaPackageId(), new Version(map.getVersion()), map.getMediaPackageElementId());
    File localFile = getLocalCacheFile(localPath);

    if (database.isLocallyCached(localPath)) {
      logger.debug("{} is already locally cached, returning cached copy", localPath);
      return IOUtils.toInputStream(localFile.getAbsolutePath());
    }

    logger.info("{} not found locally, fetching from Glacier", map.getPrintablePath());
    logger.trace("Object key: {}", map.getObjectKey());
    //Ensure the root exists
    try {
      FileUtils.forceMkdirParent(localFile);
      FileUtils.touch(localFile);
    } catch (IOException e) {
      throw new ElementStoreException("Unable to create " + localCacheRoot, e);
    }

    //Download the file.  This may block for upwards of 12 hours depending on retrival class and account settings.
    atm.download(vaultName, map.getObjectKey(), localFile);

    try {
      logger.debug("{} downloaded, marking as locally cached at {}", map.getPrintablePath(), localFile.getAbsolutePath());
      database.addLocallyCachedFile(localPath);
    } catch (AwsAssetDatabaseException e) {
      throw new ElementStoreException("Unable to add cached copy to database, element " + localFile.getAbsolutePath() + " will not be automatically cleaned up!", e);
    }

    return IOUtils.toInputStream(localFile.getAbsolutePath());
  }

  public void deleteObject(AwsAssetMapping map) throws ElementStoreException {
    logger.info("Deleting {} from vault {}", map.getPrintablePath(), vaultName);
    glacierClient.deleteArchive(new DeleteArchiveRequest()
            .withVaultName(vaultName)
            .withArchiveId(map.getObjectKey()));
  }

  public void purgeCache(Date olderThan) throws ElementStoreException {
    try {
      logger.info("Purging AWS Glacier cache...");
      List<StoragePath> purge = database.getLocallyCachedFiles(getStoreType(), olderThan);
      for (StoragePath path : purge) {
        File purgeMe = getLocalCacheFile(path);
        FileUtils.deleteQuietly(purgeMe);
        database.deleteCacheMapping(path);
        logger.debug("Purged {}/{}@{}", path.getMediaPackageId(), path.getAssetId(), path.getVersion());
      }
    } catch (AwsAssetDatabaseException e) {
      throw new ElementStoreException("Unable to purge cache due to database error", e);
    }
  }

  public File getLocalCacheFile(StoragePath path) {
    return file(localCacheRoot.getAbsolutePath(), buildFilename(path, ""));
  }

  //Used for testing
  public void setVaultName(String vaultName) {
    this.vaultName = vaultName;
  }

  //Used for testing
  public void setLocalCacheRoot(File root) {
    this.localCacheRoot = root;
  }

  //Used for testing
  public void setArchiveTransferManager(ArchiveTransferManager atm) {
    this.atm = atm;
  }

  //Used for testing
  public void setGlacierClient(AmazonGlacierClient client) {
    this.glacierClient = client;
  }

  public void setStoreType(String storeType) {
    this.storeType = storeType;
  }
}
