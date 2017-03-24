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

import org.opencastproject.pm.syllabus.api.VLocation;
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

@Entity(name = "VLocation")
@Table(name = "V_LOCATION")
@NamedQueries({
        @NamedQuery(name = "VLocation.findAllSince", query = "select a from VLocation a where a.lastChanged > :since"),
        @NamedQuery(name = "VLocation.findAll", query = "select a from VLocation a") })
public final class VLocationDto {
  @Id
  private String id;
  private String hostKey;
  private String name;
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastChanged;
  private String zoneId;

  private VLocationDto() {
  }

  // -------------------------------------------------------------------------------------------------------------------

  public static final Function<VLocationDto, VLocation> toDomain = new Function<VLocationDto, VLocation>() {
    @Override
    public VLocation apply(VLocationDto dto) {
      final String id = dto.id;
      final String name = dto.name;
      final String hostKey = dto.hostKey;
      final DateTime lastChanged = toDateTime(dto.lastChanged);
      final Option<String> zoneId = option(dto.zoneId).bind(Strings.trimToNone);
      return new VLocation() {
        @Override
        public String getHostKey() {
          return hostKey;
        }

        @Override
        public String getName() {
          return name;
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
        public Option<String> getZoneId() {
          return zoneId;
        }
      };
    }
  };

  // -------------------------------------------------------------------------------------------------------------------

  public static final Finder<VLocationDto> finder = new Finder<VLocationDto>("VLocation");
}
