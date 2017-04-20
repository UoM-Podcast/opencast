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

import org.opencastproject.pm.syllabus.api.VLocationSuitability;
import org.opencastproject.pm.syllabus.impl.id.VLocationSuitabilityId;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.persistence.Queries;

import org.joda.time.DateTime;

import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity(name = "VLocationSuitability")
@Table(name = "V_LOCATION_SUITABILITY")
@IdClass(VLocationSuitabilityId.class)
@NamedQueries({
        @NamedQuery(name = "VLocationSuitability.findAllSince", query = "select a from VLocationSuitability a where a.lastChanged > :since"),
        @NamedQuery(name = "VLocationSuitability.findAll", query = "select a from VLocationSuitability a"),
        @NamedQuery(name = "VLocationSuitability.findBySuitabilityId", query = "select a from VLocationSuitability a where a.suitabilityId = :suitabilityId") })
public final class VLocationSuitabilityDto {
  @Id // not the ID but a foreign key
  private String locationId;
  @Id // not the ID but a foreign key
  private String suitabilityId;
  private String type;
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastChanged;

  private VLocationSuitabilityDto() {
  }

  // -------------------------------------------------------------------------------------------------------------------

  public static final Function<VLocationSuitabilityDto, VLocationSuitability> toDomain = new Function<VLocationSuitabilityDto, VLocationSuitability>() {
    @Override
    public VLocationSuitability apply(VLocationSuitabilityDto dto) {
      final String locationId = dto.locationId;
      final String suitabilityId = dto.suitabilityId;
      final DateTime lastChanged = toDateTime(dto.lastChanged);
      final String type = dto.type;
      return new VLocationSuitability() {
        @Override public String getLocationId() {
          return locationId;
        }

        @Override public String getSuitabilityId() {
          return suitabilityId;
        }

        @Override public String getType() {
          return type;
        }

        @Override
        public DateTime getLastChanged() {
          return lastChanged;
        }
      };
    }
  };

  // -------------------------------------------------------------------------------------------------------------------

  public static final Finder<VLocationSuitabilityDto> finder = new Finder<VLocationSuitabilityDto>("VLocationSuitability");

  /** A JPA named query <code><i>EntityName</i>.findAllSince</code> must exist. */
  public static Function<EntityManager, List<VLocationSuitabilityDto>> findBySuitabilityId(String suitabilityId) {
    return Queries.named.findAll("VLocationSuitability.findBySuitabilityId", tuple("suitabilityId", suitabilityId));
  }
}
