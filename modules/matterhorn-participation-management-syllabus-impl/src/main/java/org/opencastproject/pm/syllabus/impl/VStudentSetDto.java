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

import org.opencastproject.pm.syllabus.api.VStudentSet;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.functions.Strings;

import org.joda.time.DateTime;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity(name = "VStudentSet")
@Table(name = "V_STUDENTSET")
@NamedQueries({
        @NamedQuery(name = "VStudentSet.findAllSince", query = "select s from VStudentSet s where s.lastChanged > :since"),
        @NamedQuery(name = "VStudentSet.findAll", query = "select s from VStudentSet s") })
public final class VStudentSetDto {
  @Id
  private String id;
  private String name;
  private String hostKey;
  private String departmentId;
  private String userText2;
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastChanged;
  private String description;

  private VStudentSetDto() {
  }

  // -------------------------------------------------------------------------------------------------------------------

  public static final Function<VStudentSetDto, VStudentSet> toDomain = new Function<VStudentSetDto, VStudentSet>() {
    @Override
    public VStudentSet apply(VStudentSetDto dto) {
      final String id = dto.id;
      final String hostKey = dto.hostKey;
      final Option<String> description = option(dto.description).bind(Strings.trimToNone);
      final String departmentId = dto.departmentId;
      final String name = dto.name;
      final Option<String> email = option(dto.userText2).bind(Strings.trimToNone);
      final DateTime lastChanged = toDateTime(dto.lastChanged);
      return new VStudentSet() {
        @Override
        public String getHostKey() {
          return hostKey;
        }

        @Override
        public String getDepartmentId() {
          return departmentId;
        }

        @Override
        public Option<String> getDescription() {
          return description;
        }

        @Override
        public String getId() {
          return id;
        }

        @Override
        public DateTime getLastChanged() {
          return lastChanged;
        }

        @Override
        public String getName() {
          return name;
        }

        @Override
        public Option<String> getEmail() {
          return email;
        }
      };
    }
  };

  // -------------------------------------------------------------------------------------------------------------------

  public static final Finder<VStudentSetDto> finder = new Finder<VStudentSetDto>("VStudentSet");
}
