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
import static org.opencastproject.util.data.Option.option;

import org.opencastproject.pm.syllabus.api.VStaff;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.functions.Strings;
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

@Entity(name = "VStaff")
@Table(name = "V_STAFF")
@NamedQueries({
        @NamedQuery(name = "VStaff.findAllSince", query = "select a from VStaff a where a.lastChanged > :since"),
        @NamedQuery(name = "VStaff.findAll", query = "select a from VStaff a"),
        @NamedQuery(name = "VStaff.getById", query = "select a from VStaff a where a.hostKey = :spotId")})
public final class VStaffDto {
  @Id
  private String id;
  private String hostKey;
  private String name;
  private String departmentId;
  private String email;
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastChanged;
  private String description;

  private VStaffDto() {
  }

  // -------------------------------------------------------------------------------------------------------------------

  public static final Function<VStaffDto, VStaff> toDomain = new Function<VStaffDto, VStaff>() {
    @Override
    public VStaff apply(VStaffDto dto) {
      final Option<String> email = option(dto.email).bind(Strings.trimToNone);
      final String name = dto.name;
      final String id = dto.id;
      final String hostKey = dto.hostKey;
      final Option<String> description = option(dto.description).bind(Strings.trimToNone);
      final Option<String> departmentId = option(dto.departmentId).bind(Strings.trimToNone);
      final DateTime lastChanged = toDateTime(dto.lastChanged);
      return new VStaff() {
        @Override
        public Option<String> getEmail() {
          return email;
        }

        @Override
        public String getName() {
          return name;
        }

        @Override
        public Option<String> getDescription() {
          return description;
        }

        @Override
        public String getHostKey() {
          return hostKey;
        }

        @Override
        public Option<String> getDepartmentId() {
          return departmentId;
        }

        @Override
        public String getId() {
          return id;
        }

        @Override
        public DateTime getLastChanged() {
          return lastChanged;
        }
      };
    }
  };

  // -------------------------------------------------------------------------------------------------------------------

  public static final Finder<VStaffDto> finder = new Finder<VStaffDto>("VStaff");

  public abstract static class StaffFinder<A> extends Finder {
    public StaffFinder(String entityName) {
      super(entityName);
    }

    /**
     * Get staff details given staff activity ID
     @param activityId
     @return
     */
    public abstract Function<EntityManager, List<VStaff>> findStaffByStaffActivityId(String activityId);
  }

  public static final Finder<VStaffDto> finderStaff = new VStaffDto.StaffFinder<VStaffDto>("VStaff") {
    @Override
    public Function<EntityManager, List<VStaff>> findStaffByStaffActivityId(String activityId) {
      return Queries.sql.findAll(
              "select sta.id, sta.Hostkey, sta.name, sta.email, sta.description from rdowner.V_Staff sta inner join rdowner.V_Activity_Staff acs on acs.StaffId = sta.Id "
                      + "where acs.ActivityId  = ? " , activityId);
    }
  };
}
