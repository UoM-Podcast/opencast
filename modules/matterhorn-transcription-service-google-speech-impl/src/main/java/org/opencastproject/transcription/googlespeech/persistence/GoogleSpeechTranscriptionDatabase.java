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
package org.opencastproject.transcription.googlespeech.persistence;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;

public class GoogleSpeechTranscriptionDatabase {

  /**
   * Logging utilities
   */
  private static final Logger logger = LoggerFactory.getLogger(GoogleSpeechTranscriptionDatabase.class);

  /**
   * Persistence provider set by OSGi
   */
  private PersistenceProvider persistenceProvider;

  /**
   * Factory used to create entity managers for transactions
   */
  protected EntityManagerFactory emf;

  private final long noProviderId = -1;

  /**
   * OSGi callback.
   */
  public void activate(ComponentContext cc) {
    logger.info("Activating persistence manager for transcription service");
  }

  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  /**
   * OSGi callback to set persistence provider.
   */
  public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }

  public GoogleSpeechTranscriptionJobControl storeJobControl(String mpId, String trackId, String jobId, String jobStatus,
          long trackDuration, String provider) throws GoogleSpeechTranscriptionDatabaseException {
    long providerId = getProviderId(provider);
    if (providerId != noProviderId) {
      GoogleSpeechTranscriptionJobControlDto dto = GoogleSpeechTranscriptionJobControlDto.store(emf.createEntityManager(), mpId, trackId, jobId,
              jobStatus, trackDuration, providerId);
      if (dto != null) {
        return dto.toTranscriptionJobControl();
      }
    }
    return null;
  }

  public TranscriptionProviderControl storeProviderControl(String provider) throws GoogleSpeechTranscriptionDatabaseException {
    TranscriptionProviderControlDto dto = TranscriptionProviderControlDto.storeProvider(emf.createEntityManager(), provider);
    if (dto != null) {
      logger.info("Transcription provider '{}' stored", provider);
      return dto.toTranscriptionProviderControl();
    }
    logger.warn("Unable to store transcription provider '{}'", provider);
    return null;
  }

  public void deleteJobControl(String jobId) throws GoogleSpeechTranscriptionDatabaseException {
    GoogleSpeechTranscriptionJobControlDto.delete(emf.createEntityManager(), jobId);
  }

  public void updateJobControl(String jobId, String jobStatus) throws GoogleSpeechTranscriptionDatabaseException {
    GoogleSpeechTranscriptionJobControlDto.updateStatus(emf.createEntityManager(), jobId, jobStatus);
  }

  public GoogleSpeechTranscriptionJobControl findByJob(String jobId) throws GoogleSpeechTranscriptionDatabaseException {
    GoogleSpeechTranscriptionJobControlDto dto = GoogleSpeechTranscriptionJobControlDto.findByJob(emf.createEntityManager(), jobId);
    if (dto != null) {
      return dto.toTranscriptionJobControl();
    }
    return null;
  }

  public List<GoogleSpeechTranscriptionJobControl> findByMediaPackage(String mpId) throws GoogleSpeechTranscriptionDatabaseException {
    List<GoogleSpeechTranscriptionJobControlDto> list = GoogleSpeechTranscriptionJobControlDto.findByMediaPackage(emf.createEntityManager(),
            mpId);
    List<GoogleSpeechTranscriptionJobControl> resultList = new ArrayList<GoogleSpeechTranscriptionJobControl>();
    for (GoogleSpeechTranscriptionJobControlDto dto : list) {
      resultList.add(dto.toTranscriptionJobControl());
    }
    return resultList;
  }

  public List<GoogleSpeechTranscriptionJobControl> findByStatus(String... status) throws GoogleSpeechTranscriptionDatabaseException {
    List<GoogleSpeechTranscriptionJobControlDto> list = GoogleSpeechTranscriptionJobControlDto.findByStatus(emf.createEntityManager(), status);
    List<GoogleSpeechTranscriptionJobControl> resultList = new ArrayList<GoogleSpeechTranscriptionJobControl>();
    for (GoogleSpeechTranscriptionJobControlDto dto : list) {
      resultList.add(dto.toTranscriptionJobControl());
    }
    return resultList;
  }

  public TranscriptionProviderControl findIdByProvider(String provider) throws GoogleSpeechTranscriptionDatabaseException {
    TranscriptionProviderControlDto dtoProvider = TranscriptionProviderControlDto.findIdByProvider(emf.createEntityManager(), provider);
    if (dtoProvider != null) {
      return dtoProvider.toTranscriptionProviderControl();
    } else {
      // store provider and retrieve id
      TranscriptionProviderControl dto = storeProviderControl(provider);
      if (dto != null) {
        dtoProvider = TranscriptionProviderControlDto.findIdByProvider(emf.createEntityManager(), provider);
        return dtoProvider.toTranscriptionProviderControl();
      }
      return null; // not found
    }
  }

  public TranscriptionProviderControl findProviderById(Long id) throws GoogleSpeechTranscriptionDatabaseException {
    TranscriptionProviderControlDto dtoProvider = TranscriptionProviderControlDto.findProviderById(emf.createEntityManager(), id);
    if (dtoProvider != null) {
      return dtoProvider.toTranscriptionProviderControl();
    }
    return null;
  }

  private long getProviderId(String provider) throws GoogleSpeechTranscriptionDatabaseException {
    TranscriptionProviderControl providerInfo = findIdByProvider(provider);
    if (providerInfo != null) {
      return providerInfo.getId();
    }
    return noProviderId;
  }
}
