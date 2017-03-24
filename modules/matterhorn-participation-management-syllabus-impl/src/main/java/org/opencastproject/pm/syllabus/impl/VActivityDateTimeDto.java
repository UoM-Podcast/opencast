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

import static org.opencastproject.pm.syllabus.impl.SyllabusUtil.toDateTime;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.pm.syllabus.api.VActivityDateTime;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.persistence.Queries;

import org.joda.time.DateTime;

import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity(name = "VActivityDateTime")
@Table(name = "V_ACTIVITY_DATETIME")
@NamedQueries({@NamedQuery(name = "VActivityDateTime.findAllSince",
                           query = "select a from VActivityDateTime a where a.lastChanged > :since"),
                      @NamedQuery(name = "VActivityDateTime.findAll",
                                  query = "select a from VActivityDateTime a"),
                      @NamedQuery(name = "VActivityDateTime.findByActivityId",
                                  query = "select a from VActivityDateTime a where a.activityId = :activityId"),
                      @NamedQuery(name = "VActivityDateTime.findByActivityIdRange",
                                  query = "select a from VActivityDateTime a where a.activityId >= :startId and a.activityId <= :endId")})
public final class VActivityDateTimeDto {
  @Id // not the ID but a foreign key
  private String activityId;
  @Id // not really part of the ID but needed by JPA to distinguish table rows. absolutely needed otherwise JPA will miss entities!
  @Temporal(TemporalType.TIMESTAMP)
  private Date startDateTime;
  @Id // not really part of the ID but needed by JPA to distinguish table rows. absolutely needed otherwise JPA will miss entities!
  @Temporal(TemporalType.TIMESTAMP)
  private Date endDateTime;
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastChanged;

  private VActivityDateTimeDto() {
  }

  // -------------------------------------------------------------------------------------------------------------------

  public static final Function<VActivityDateTimeDto, VActivityDateTime> toDomain = new Function<VActivityDateTimeDto, VActivityDateTime>() {
    @Override public VActivityDateTime apply(VActivityDateTimeDto dto) {
      final String activityId = dto.activityId;
      final DateTime startDateTime = toDateTime(dto.startDateTime);
      final DateTime endDateTime = toDateTime(dto.endDateTime);
      final DateTime lastChanged = toDateTime(dto.lastChanged);
      return new VActivityDateTime() {
        @Override public String getActivityId() {
          return activityId;
        }

        @Override public DateTime getStartDateTime() {
          return startDateTime;
        }

        @Override public DateTime getEndDateTime() {
          return endDateTime;
        }

        @Override public DateTime getLastChanged() {
          return lastChanged;
        }
      };
    }
  };

  // -------------------------------------------------------------------------------------------------------------------

  public static final Finder<VActivityDateTimeDto> finder = new Finder<VActivityDateTimeDto>("VActivityDateTime");

  public static Function<EntityManager, List<VActivityDateTimeDto>> findByActivityId(String activityId) {
    return Queries.named.findAll("VActivityDateTime.findByActivityId", tuple("activityId", activityId));
  }

  public static Function<EntityManager, List<VActivityDateTimeDto>> findByActivityIdRange(String startId, String endId) {
    return Queries.named.findAll("VActivityDateTime.findByActivityIdRange", tuple("startId", startId), tuple("endId", endId));
  }
}
