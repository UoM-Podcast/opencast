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

import org.opencastproject.pm.syllabus.api.VActivityLocation;
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

@Entity(name = "VActivityLocation")
@Table(name = "V_ACTIVITY_LOCATION")
@NamedQueries({@NamedQuery(name = "VActivityLocation.findAllSince",
                           query = "select a from VActivityLocation a where a.lastChanged > :since"),
                      @NamedQuery(name = "VActivityLocation.findAll",
                                  query = "select a from VActivityLocation a"),
                      @NamedQuery(name = "VActivityLocation.findByActivityId",
                                  query = "select a from VActivityLocation a where a.activityId = :activityId")})
public final class VActivityLocationDto {
  @Id // not the ID but a foreign key
  private String activityId;
  @Id // not the ID but a foreign key
  private String locationId;
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastChanged;

  private VActivityLocationDto() {
  }

  // -------------------------------------------------------------------------------------------------------------------

  public static final Function<VActivityLocationDto, VActivityLocation> toDomain = new Function<VActivityLocationDto, VActivityLocation>() {
    @Override public VActivityLocation apply(VActivityLocationDto dto) {
      final String activityId = dto.activityId;
      final String locationId = dto.locationId;
      final DateTime lastChanged = toDateTime(dto.lastChanged);
      return new VActivityLocation() {
        @Override public String getActivityId() {
          return activityId;
        }

        @Override public String getLocationId() {
          return locationId;
        }

        @Override public DateTime getLastChanged() {
          return lastChanged;
        }
      };
    }
  };

  // -------------------------------------------------------------------------------------------------------------------

  public static final Finder<VActivityLocationDto> finder = new Finder<VActivityLocationDto>("VActivityLocation");

  public static Function<EntityManager, List<VActivityLocationDto>> findByActivityId(String activityId) {
    return Queries.named.findAll("VActivityLocation.findByActivityId", tuple("activityId", activityId));
  }
}
