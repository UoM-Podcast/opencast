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

import static org.opencastproject.util.RequireUtil.notNull;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Entity object for the blacklist data.
 */
@Entity(name = "Blacklist")
@Table(name = "mh_pm_blacklist", uniqueConstraints = { @UniqueConstraint(columnNames = { "blacklisted" }) })
@NamedQueries({ @NamedQuery(name = "Blacklist.findAll", query = "SELECT b FROM Blacklist b"),
        @NamedQuery(name = "Blacklist.findById", query = "SELECT b FROM Blacklist b WHERE b.id = :id"),
        @NamedQuery(name = "Blacklist.clear", query = "DELETE FROM Blacklist") })
public class BlacklistDto {

  @Id
  @GeneratedValue
  @Column(name = "id")
  private long id;

  @Column(name = "type", nullable = false)
  private String type;

  @Column(name = "blacklisted", nullable = false)
  private long blacklistedId;

  @OneToMany(fetch = FetchType.EAGER, cascade = { CascadeType.REMOVE })
  private List<PeriodDto> periods = new ArrayList<PeriodDto>();

  /**
   * Default constructor
   */
  public BlacklistDto() {
  }

  /**
   * Creates a building
   * 
   * @param blacklisted
   *          the blacklisted object
   * @param periods
   *          the period list
   */
  public BlacklistDto(BlacklistableDto blacklisted, List<PeriodDto> periods) {
    this.blacklistedId = notNull(blacklisted, "blacklisted").getId();
    this.periods = notNull(periods, "periods list");
    this.type = blacklisted.getType();
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
   * Sets the blacklisted object
   * 
   * @param blacklisted
   *          the blacklisted object
   */
  public void setBlacklisted(BlacklistableDto blacklisted) {
    this.blacklistedId = notNull(blacklisted, "blacklisted").getId();
    this.type = blacklisted.getType();
  }

  /**
   * Sets the blacklisted object id
   * 
   * @return the blacklisted object id
   */
  public void setBlacklistedId(long blacklistedId) {
    this.blacklistedId = blacklistedId;
  }

  /**
   * Returns the blacklisted object id
   * 
   * @return the blacklisted object id
   */
  public long getBlacklistedId() {
    return blacklistedId;
  }

  /**
   * Returns the type
   * 
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the periods list
   * 
   * @param periods
   *          the periods list
   */
  public void setPeriods(List<PeriodDto> periods) {
    this.periods = notNull(periods, "periods");
  }

  /**
   * Returns the list of periods
   * 
   * @return the periods list
   */
  public List<PeriodDto> getPeriods() {
    return periods;
  }

  /**
   * Add a time period to the blacklist
   * 
   * @param period
   *          the time period to add to the blacklist
   * 
   * @return true if this collection changed as a result of the call
   */
  public boolean addPeriod(PeriodDto period) {
    return periods.add(notNull(period, "period"));
  }

  /**
   * Remove the given time period from the blacklist
   * 
   * @param period
   *          the time period to remove
   * 
   * @return true if this collection changed as a result of the call
   */
  public boolean removePeriod(PeriodDto period) {
    return periods.remove(notNull(period, "period"));
  }

}
