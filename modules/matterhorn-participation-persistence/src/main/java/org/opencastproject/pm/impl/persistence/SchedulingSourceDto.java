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

import static org.opencastproject.util.RequireUtil.notEmpty;

import org.opencastproject.pm.api.SchedulingSource;
import org.opencastproject.util.data.Function;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * Entity object for the sources.
 */
@Entity(name = "SchedulingSource")
@Table(name = "mh_pm_scheduling_source")
@NamedQueries({
  @NamedQuery(name = "SchedulingSource.findAll", query = "SELECT c FROM SchedulingSource c"),
  @NamedQuery(name = "SchedulingSource.clear", query = "DELETE FROM SchedulingSource"),
  @NamedQuery(name = "SchedulingSource.findById", query = "SELECT c FROM SchedulingSource c WHERE c.source= :id"),
  @NamedQuery(name = "SchedulingSource.findByDescription", query = "SELECT c FROM SchedulingSource c WHERE c.source= :description")})
public class SchedulingSourceDto {

  @Id
  @GeneratedValue
  @Column(name = "id")
  private long id;

  @Column(name = "source", nullable = false)
  private String source;

  /**
   * Default constructor
   */
  public SchedulingSourceDto() {
  }

  /**
   * Creates a source
   * 
   * @param source
   *          the source
   */
  public SchedulingSourceDto(String source) {
    this.source = notEmpty(source, "source");
  }

  /**
   * Returns the id of this entity
   * 
   * @return the id as long
   */
  public long getId() {
    return id;
  }

  /**
   * Sets the source
   * 
   * @param source
   *          the source
   */
  public void setSource(String source) {
    this.source = source;
  }

  /**
   * Returns the source
   * 
   * @return the source
   */
  public String getSource() {
    return source;
  }

  /**
   * Returns the business object of this source
   * 
   * @return the business object model of this source
   */
  public SchedulingSource toSchedulingSource() {
    SchedulingSource schedulingSource = new SchedulingSource(source);
    schedulingSource.setId(id);
    return schedulingSource;
  }

  public static final Function<SchedulingSourceDto, SchedulingSource> toSchedulingSource = new Function<SchedulingSourceDto, SchedulingSource>() {
    @Override public SchedulingSource apply(SchedulingSourceDto dto) {
      return dto.toSchedulingSource();
    }
  };
}
