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
package org.opencastproject.pm.impl.persistence.id;

import java.io.Serializable;

/**
 *
 * @author ts23
 */
public class RecordingToMessagesId implements Serializable {

  private long recordingId;
  private long messageId;

  public RecordingToMessagesId() {

  }

  public RecordingToMessagesId(long recordingId, long messageId) {
    this.recordingId = recordingId;
    this.messageId = messageId;
  }

  public long getRecordingId() {
    return recordingId;
  }

  public long getMessageId() {
    return messageId;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
            + String.valueOf(recordingId).hashCode();
    result = prime * result + (String.valueOf(messageId).hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    RecordingToMessagesId other = (RecordingToMessagesId) obj;
    if (recordingId != other.recordingId) {
      return false;
    }
    if (messageId != other.messageId) {
      return false;
    }
    return true;
  }

}
