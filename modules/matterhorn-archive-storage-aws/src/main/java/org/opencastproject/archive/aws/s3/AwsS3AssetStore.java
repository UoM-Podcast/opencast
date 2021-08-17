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

package org.opencastproject.archive.aws.s3;

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

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingResult;
import com.amazonaws.services.s3.model.ObjectTagging;
import com.amazonaws.services.s3.model.RestoreObjectRequest;
import com.amazonaws.services.s3.model.RestoreObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.SetBucketVersioningConfigurationRequest;
import com.amazonaws.services.s3.model.SetObjectTaggingRequest;
import com.amazonaws.services.s3.model.StorageClass;
import com.amazonaws.services.s3.model.Tag;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.List;

public class AwsS3AssetStore extends AwsAbstractArchive implements RemoteElementStore {

  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(AwsS3AssetStore.class);

  private static final Tag freezable = new Tag("Freezable", "true");
  private static final Integer RESTORE_MIN_WAIT = 1080000; // 3h
  private static final Integer RESTORE_POLL = 900000; // 15m

  // Service configuration
  public static final String AWS_S3_ENABLED = "org.opencastproject.archive.aws.s3.enabled";
  public static final String AWS_S3_ACCESS_KEY_ID_CONFIG = "org.opencastproject.archive.aws.s3.access.id";
  public static final String AWS_S3_SECRET_ACCESS_KEY_CONFIG = "org.opencastproject.archive.aws.s3.secret.key";
  public static final String AWS_S3_REGION_CONFIG = "org.opencastproject.archive.aws.s3.region";
  public static final String AWS_S3_BUCKET_CONFIG = "org.opencastproject.archive.aws.s3.bucket";
  public static final String AWS_S3_GLACIER_RESTORE_DAYS = "org.opencastproject.archive.aws.s3.glacier.restore.days";

  public static final Integer AWS_S3_GLACIER_RESTORE_DAYS_DEFAULT = 2;

  /** The AWS client and transfer manager */
  private AmazonS3 s3 = null;
  private TransferManager s3TransferManager = null;

  /** The AWS S3 bucket name */
  private String bucketName = null;

  /** The Glacier storage class, restore period **/
  private Integer restorePeriod;

  private boolean bucketCreated = false;

  /**
   * Service activator, called via declarative services configuration.
   *
   * @param cc
   *          the component context
   */
  public void activate(final ComponentContext cc) throws IllegalStateException, IOException, ConfigurationException {
    // Get the configuration
    if (cc != null) {
      @SuppressWarnings("rawtypes")
      Dictionary properties = cc.getProperties();

      boolean enabled = Boolean.parseBoolean(StringUtils.trimToEmpty((String) properties.get(AWS_S3_ENABLED)));
      if (!enabled) {
        logger.info("AWS S3 asset store is disabled");
        return;
      }

      // Store type: "aws-s3"
      storeType = StringUtils.trimToEmpty((String) properties.get(RemoteElementStore.STORE_TYPE_PROPERTY));
      if (StringUtils.isEmpty(storeType)) {
        throw new ConfigurationException("Invalid store type value");
      }
      logger.info("{} is: {}", RemoteElementStore.STORE_TYPE_PROPERTY, storeType);

      // AWS S3 bucket name
      bucketName = getAWSConfigKey(cc, AWS_S3_BUCKET_CONFIG);
      logger.info("AWS S3 bucket name is {}", bucketName);

      // AWS region
      regionName = getAWSConfigKey(cc, AWS_S3_REGION_CONFIG);
      logger.info("AWS region is {}", regionName);

      // Glacier storage class restore period
      restorePeriod = OsgiUtil.getOptCfgAsInt(cc.getProperties(), AWS_S3_REGION_CONFIG).getOrElse(AWS_S3_GLACIER_RESTORE_DAYS_DEFAULT);

      // Explicit credentials are optional.
      AWSCredentialsProvider provider = null;
      Option<String> accessKeyIdOpt = OsgiUtil.getOptCfg(cc.getProperties(), AWS_S3_ACCESS_KEY_ID_CONFIG);
      Option<String> accessKeySecretOpt = OsgiUtil.getOptCfg(cc.getProperties(), AWS_S3_SECRET_ACCESS_KEY_CONFIG);

      // Keys not informed so use default credentials provider chain, which
      // will look at the environment variables, java system props, credential files, and instance
      // profile credentials
      if (accessKeyIdOpt.isNone() && accessKeySecretOpt.isNone()) {
        provider = new DefaultAWSCredentialsProviderChain();
      } else {
        provider = new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(accessKeyIdOpt.get(), accessKeySecretOpt.get()));
      }
      // Create AWS client.
      s3 = AmazonS3ClientBuilder.standard()
              .withRegion(regionName)
              .withCredentials(provider)
              .build();

      s3TransferManager = TransferManagerBuilder.standard().withS3Client(s3).build();

      logger.info("AwsS3ArchiveAssetStore activated with key id beginning {}!", provider.getCredentials().getAWSAccessKeyId().substring(0, 5));
    }

  }

  /**
   * Creates the AWS S3 bucket if it doesn't exist yet.
   */
  private void createAWSBucket() {
    // Does bucket exist?
    try {
      s3.listObjects(bucketName);
    } catch (AmazonServiceException e) {
      if (e.getStatusCode() == 404) {
        // Create the bucket
        try {
          s3.createBucket(bucketName);
          // Enable versioning
          BucketVersioningConfiguration configuration = new BucketVersioningConfiguration().withStatus("Enabled");
          SetBucketVersioningConfigurationRequest configRequest = new SetBucketVersioningConfigurationRequest(
                  bucketName, configuration);
          s3.setBucketVersioningConfiguration(configRequest);
          logger.info("AWS S3 ARCHIVE bucket {} created and versioning enabled", bucketName);
        } catch (Exception e2) {
          throw new IllegalStateException("ARCHIVE bucket " + bucketName + " cannot be created: " + e2.getMessage(), e2);
        }
      } else {
        throw new IllegalStateException("ARCHIVE bucket " + bucketName + " exists, but we can't access it: "
                + e.getMessage(), e);
      }
    }
    // Bucket already existed or was just created
    bucketCreated = true;
  }

  /**
   * Returns the aws s3 object id created by aws
   */
  protected AwsUploadOperationResult uploadObject(File origin, String objectName, Option<MimeType> mimeType) throws ElementStoreException {
    // Check first if bucket is there.
    if (!bucketCreated) {
      createAWSBucket();
    }

    // Upload file to AWS S3
    // Use TransferManager to take advantage of multipart upload.
    // TransferManager processes all transfers asynchronously, so this call will return immediately.
    logger.info("Uploading {} to S3 bucket {}...", objectName, bucketName);
    Upload upload = s3TransferManager.upload(bucketName, objectName, origin);
    long start = System.currentTimeMillis();

    S3Object obj = null;
    try {
      // Block and wait for the upload to finish
      upload.waitForCompletion();
      logger.info("Upload of {} to archive bucket {} completed in {} seconds",
              new Object[] { objectName, bucketName, (System.currentTimeMillis() - start) / 1000 });
      obj = s3.getObject(bucketName, objectName);

      // Tag objects that are suitable for Glacier storage class
      // NOTE: Use of S3TransferManager means that tagging has to be done as a separate request
      if (mimeType.isSome()) {
        switch (mimeType.get().getType()) {
          case "audio":
          case "image":
          case "video":
            logger.debug("Tagging S3 object {} as Freezable", objectName);
            List<Tag> tags = new ArrayList<>();
            tags.add(freezable);
            s3.setObjectTagging(new SetObjectTaggingRequest(bucketName, objectName, new ObjectTagging(tags)));
            break;
          default:
            break;
        }
      }

      //If bucket versioning is disabled the versionId is null, so return a -1 to indicate no version
      String versionId = obj.getObjectMetadata().getVersionId();
      //FIXME: We need to do better checking this, what if versioning is just suspended?
      if (null == versionId) {
        return new AwsUploadOperationResult(objectName, "-1");
      }
      return new AwsUploadOperationResult(objectName, versionId);
    } catch (InterruptedException e) {
      throw new ElementStoreException("Operation interrupted", e);
    } finally {
      try {
        obj.close();
      } catch (IOException e) {
        //Swallow and ignore
      }
    }
  }

  /**
   * Return the object key of the asset in S3
   * @param storagePath asset storage path
   */
  public String getAssetObjectKey(StoragePath storagePath) throws ElementStoreException {
    try {
      AwsAssetMapping map = database.findMapping(getStoreType(), storagePath);
      return map.getObjectKey();
    } catch (AwsAssetDatabaseException e) {
      throw new ElementStoreException(e);
    }
  }

  /**
   * Return the storage class of the asset in S3
   * @param storagePath asset storage path
   */
  public String getAssetStorageClass(StoragePath storagePath) throws ElementStoreException {
    try {
      AwsAssetMapping map = database.findMapping(getStoreType(), storagePath);
      return getObjectStorageClass(map.getObjectKey());
    } catch (AwsAssetDatabaseException e) {
      throw new ElementStoreException(e);
    }
  }

  private String getObjectStorageClass(String objectName) throws ElementStoreException {
    try {
      String storageClass = s3.getObjectMetadata(bucketName, objectName).getStorageClass();
      return storageClass == null ? "STANDARD" : storageClass;
    } catch (SdkClientException e) {
      throw new ElementStoreException(e);
    }
  }

  /**
   * Change the storage class of the object if possible
   * @param storagePath asset storage path
   * @param storageClassId metadata storage class id
   * @see https://aws.amazon.com/s3/storage-classes/
   */
  public String modifyAssetStorageClass(StoragePath storagePath, String storageClassId) throws ElementStoreException {
    try {
      StorageClass storageClass = StorageClass.fromValue(storageClassId);
      AwsAssetMapping map = database.findMapping(getStoreType(), storagePath);
      return modifyObjectStorageClass(map.getObjectKey(), storageClass).toString();
    } catch (AwsAssetDatabaseException | IllegalArgumentException e) {
      throw new ElementStoreException(e);
    }
  }

  private StorageClass modifyObjectStorageClass(String objectName, StorageClass storageClass) throws ElementStoreException {
    try {
      String storageClassId = getObjectStorageClass(objectName);
      StorageClass objectStorageClass = StorageClass.fromValue(storageClassId);

      if (storageClass != objectStorageClass) {
        /* objects can only be retrived from Glacier not moved */
        if (objectStorageClass == StorageClass.Glacier || objectStorageClass == StorageClass.DeepArchive) {
          logger.warn("S3 Object {} can not be moved from storage class {}", objectStorageClass);
          return objectStorageClass;
        }

        /* Only put suitable objects in Glacier */
        if (storageClass == StorageClass.Glacier || objectStorageClass == StorageClass.DeepArchive) {
          GetObjectTaggingResult objectTaggingRequest = s3.getObjectTagging(new GetObjectTaggingRequest(bucketName, objectName));
          if (!objectTaggingRequest.getTagSet().contains(freezable)) {
            logger.info("S3 object {} is not suitable for storage class {}", objectName, storageClass);
            return objectStorageClass;
          }
        }

        CopyObjectRequest copyRequest = new CopyObjectRequest(bucketName, objectName, bucketName, objectName).withStorageClass(storageClass);
        s3.copyObject(copyRequest);
        logger.info("S3 object {} moved to storage class {}", objectName, storageClass);
      } else {
        logger.info("S3 object {} already in storage class {}", objectName, storageClass);
      }

      return storageClass;
    } catch (SdkClientException e) {
      throw new ElementStoreException(e);
    }
  }

  /**
   *
   */
  @Override
  protected InputStream getObject(AwsAssetMapping map) {
    return getObject(map.getObjectKey()).getObjectContent();
  }

  private S3Object getObject(String objectName) {
    String storageClassId = getObjectStorageClass(objectName);

    if ("GLACIER".equals(storageClassId)) {
      // restore object and wait until available if necessary
      restoreGlacierObject(objectName, restorePeriod, true);
    }

    return s3.getObject(bucketName, objectName);
  }

  public String getAssetRestoreStatusString(StoragePath storagePath) {
    try {
      AwsAssetMapping map = database.findMapping(getStoreType(), storagePath);

      Date expirationTime = s3.getObjectMetadata(bucketName, map.getObjectKey()).getRestoreExpirationTime();
      if (expirationTime != null) {
        return String.format("RESTORED,%s", expirationTime.toString());
      }

      Boolean prevOngoingRestore = s3.getObjectMetadata(bucketName, map.getObjectKey()).getOngoingRestore();
      if (prevOngoingRestore != null && prevOngoingRestore) {
        return "RESTORING";
      }

      return "NONE";
    } catch (AwsAssetDatabaseException | IllegalArgumentException e) {
      throw new ElementStoreException(e);
    }
  }

  /*
   * Restore a frozen asset from deep archive
   * @param storagePath asset storage path
   * @param assetRestorePeriod number of days to restore assest for
   * @see https://aws.amazon.com/s3/storage-classes/
   */
  public void initiateRestoreAsset(StoragePath storagePath, Integer assetRestorePeriod) throws ElementStoreException {
    try {
      AwsAssetMapping map = database.findMapping(getStoreType(), storagePath);
      restoreGlacierObject(map.getObjectKey(), assetRestorePeriod, false);
    } catch (AwsAssetDatabaseException | IllegalArgumentException e) {
      throw new ElementStoreException(e);
    }
  }

  private void restoreGlacierObject(String objectName, Integer objectRestorePeriod, Boolean wait) {
    Boolean prevOngoingRestore = s3.getObjectMetadata(bucketName, objectName).getOngoingRestore();
    RestoreObjectResult restoreResult;

    // Check the restoration status of the object.
    if (prevOngoingRestore == null || !prevOngoingRestore) {
      // if the object had already been restored the restore request will just
      // increase the expiration time
      RestoreObjectRequest requestRestore = new RestoreObjectRequest(bucketName, objectName, objectRestorePeriod);
      restoreResult = s3.restoreObjectV2(requestRestore);
      logger.debug("Requesting restore result {}", restoreResult.toString());
    }

    if (s3.getObjectMetadata(bucketName, objectName).getRestoreExpirationTime() == null) {
      logger.info("Restoring object {} from Glacier class storage", objectName);

      // Just initiate restore?
      if (!wait) {
        return;
      }

      // Wait min restore time and then poll ofter that
      try {
        // Check as restore might have already been initiated
        if (prevOngoingRestore != null && !prevOngoingRestore) {
          Thread.sleep(RESTORE_MIN_WAIT);
        }

        while (s3.getObjectMetadata(bucketName, objectName).getOngoingRestore()) {
          Thread.sleep(RESTORE_POLL);
        }

        logger.info("Object {} has been restored from Glacier class storage, for {} days", objectName, objectRestorePeriod);
      } catch (InterruptedException e) {
        logger.error("Object {} has not yet been restored from Glacier class storage", objectName);
      }
    } else {
      logger.info("Object {} has already been restored, further extended by {} days", objectName, objectRestorePeriod);
    }
  }

  /**
   *
   */
  @Override
  protected void deleteObject(AwsAssetMapping map) {
    s3.deleteObject(bucketName, map.getObjectKey());
  }

  // Used by restore service
  public String getBucketName() {
    return this.bucketName;
  }

  public Integer getRestorePeriod() {
    return restorePeriod;
  }

  // For running tests
  void setS3(AmazonS3 s3) {
    this.s3 = s3;
  }

  void setS3TransferManager(TransferManager s3TransferManager) {
    this.s3TransferManager = s3TransferManager;
  }

  void setBucketName(String bucketName) {
    this.bucketName = bucketName;
  }

  void setStoreType(String storeType) {
    this.storeType = storeType;
  }
}
