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

import org.opencastproject.pm.syllabus.api.VActivityStaff;
import org.opencastproject.pm.syllabus.impl.id.VActivityStaffId;
import org.opencastproject.util.data.Function;

import org.joda.time.DateTime;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity(name = "VActivityStaff")
@Table(name = "V_ACTIVITY_STAFF")
@IdClass(VActivityStaffId.class)
@NamedQueries({
        @NamedQuery(name = "VActivityStaff.findAllSince", query = "select a from VActivityStaff a where a.lastChanged > :since"),
        @NamedQuery(name = "VActivityStaff.findAll", query = "select a from VActivityStaff a") })
public final class VActivityStaffDto {
  @Id
  // not the ID but a foreign key
  private String activityId;
  @Id
  // not the ID but a foreign key
  private String staffId;
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastChanged;

  private VActivityStaffDto() {
  }

  // -------------------------------------------------------------------------------------------------------------------

  public static final Function<VActivityStaffDto, VActivityStaff> toDomain = new Function<VActivityStaffDto, VActivityStaff>() {
    @Override
    public VActivityStaff apply(VActivityStaffDto dto) {
      final String activityId = dto.activityId;
      final String staffId = dto.staffId;
      final DateTime lastChanged = toDateTime(dto.lastChanged);
      return new VActivityStaff() {
        @Override
        public String getActivityId() {
          return activityId;
        }

        @Override
        public String getStaffId() {
          return staffId;
        }

        @Override
        public DateTime getLastChanged() {
          return lastChanged;
        }
      };
    }
  };

  // -------------------------------------------------------------------------------------------------------------------

  public static final Finder<VActivityStaffDto> finder = new Finder<VActivityStaffDto>("VActivityStaff");
}
