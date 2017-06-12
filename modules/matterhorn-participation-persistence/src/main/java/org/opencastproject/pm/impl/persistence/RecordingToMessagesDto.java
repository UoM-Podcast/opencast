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

package org.opencastproject.pm.impl.persistence;

import org.opencastproject.pm.impl.persistence.id.RecordingToMessagesId;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Entity to map recordings to messages.
 */
@Entity(name = "RecordingToMessages")
@Table(name = "mh_pm_recording_messages", uniqueConstraints = { @UniqueConstraint(columnNames = { "recording_id", "message_id"}) })
@IdClass(RecordingToMessagesId.class)
@NamedQueries({
  @NamedQuery(name = "RecordingToMessage.findByRecordingId", query = "SELECT r FROM RecordingToMessages r WHERE r.recordingId = :recordingId")})
public class RecordingToMessagesDto {
  @Id
  @Column(name = "recording_id", nullable = false)
  private long recordingId;

  @Id
  @Column(name = "message_id", nullable = false)
  private long messageId;

  public long getRecordingId() {
    return recordingId;
  }

  public void setRecordingId(long recordingId) {
    this.recordingId = recordingId;
  }

  public long getMessageId() {
    return messageId;
  }

  public void setMessageId(long messageId) {
    this.messageId = messageId;
  }
}
