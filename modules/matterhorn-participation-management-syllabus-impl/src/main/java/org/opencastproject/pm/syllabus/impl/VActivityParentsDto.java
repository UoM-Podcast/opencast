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

import org.opencastproject.pm.syllabus.api.VActivityParents;
import org.opencastproject.pm.syllabus.impl.id.VActivityParentsId;
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

@Entity(name = "VActivityParents")
@Table(name = "V_ACTIVITY_PARENTS")
@IdClass(VActivityParentsId.class)
@NamedQueries({@NamedQuery(name = "VActivityParents.findAllSince",
                           query = "select a from VActivityParents a where a.lastChanged > :since"),
                      @NamedQuery(name = "VActivityParents.findAll",
                                  query = "select a from VActivityParents a"),
                      @NamedQuery(name = "VActivityParents.findByActivityId",
                                  query = "select a from VActivityParents a where a.activityId = :activityId")})
public final class VActivityParentsDto {
  @Id // not the ID but a foreign key
  private String activityId;
  @Id // not the ID but a foreign key
  private String parentId;
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastChanged;

  private VActivityParentsDto() {
  }

  // -------------------------------------------------------------------------------------------------------------------

  public static final Function<VActivityParentsDto, VActivityParents> toDomain = new Function<VActivityParentsDto, VActivityParents>() {
    @Override public VActivityParents apply(VActivityParentsDto dto) {
      final String activityId = dto.activityId;
      final String parentId = dto.parentId;
      final DateTime lastChanged = toDateTime(dto.lastChanged);
      return new VActivityParents() {
        @Override public String getActivityId() {
          return activityId;
        }

        @Override public String getParentId() {
          return parentId;
        }

        @Override public DateTime getLastChanged() {
          return lastChanged;
        }
      };
    }
  };

  // -------------------------------------------------------------------------------------------------------------------

  public static final Finder<VActivityParentsDto> finder = new Finder<VActivityParentsDto>("VActivityParents");

  public static Function<EntityManager, List<VActivityParentsDto>> findByActivityId(String activityId) {
    return Queries.named.findAll("VActivityParents.findByActivityId", tuple("activityId", activityId));
  }
}
