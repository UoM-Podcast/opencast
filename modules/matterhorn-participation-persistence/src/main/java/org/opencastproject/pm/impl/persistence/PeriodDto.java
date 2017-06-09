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

import static org.opencastproject.pm.api.Period.period;
import static org.opencastproject.util.RequireUtil.notNull;

import org.opencastproject.pm.api.Period;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Entity object for the period data.
 */
@Entity(name = "Period")
@Table(name = "mh_pm_period")
@NamedQueries({
        @NamedQuery(name = "Period.findAll", query = "SELECT p FROM Period p"),
        @NamedQuery(name = "Period.findPurposeByType", query = "SELECT p.purpose FROM Blacklist b INNER JOIN b.periods p WHERE b.type = :type AND p.purpose IS NOT NULL GROUP BY p.purpose"),
        @NamedQuery(name = "Period.count", query = "SELECT COUNT(p) FROM Period p"),
        @NamedQuery(name = "Period.clear", query = "DELETE FROM Period") })
public class PeriodDto {

  @Id
  @GeneratedValue
  @Column(name = "id")
  private long id;

  @Column(name = "start_date", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date start;

  @Column(name = "end_date", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date end;

  @Column(name = "purpose")
  private String purpose;

  @Column(name = "comment")
  private String comment;

  /**
   * Default constructor
   */
  public PeriodDto() {
  }

  /**
   * Creates a period
   * 
   * @param start
   *          the start date
   * @param end
   *          the end date
   */
  public PeriodDto(Date start, Date end) {
    this(start, end, null);
  }

  /**
   * Creates a period
   * 
   * @param start
   *          the start date
   * @param end
   *          the end date
   * @param purpose
   *          the purpose
   */
  public PeriodDto(Date start, Date end, String purpose) {
    this(start, end, purpose, null);
  }

  /**
   * Creates a period
   * 
   * @param start
   *          the start date
   * @param end
   *          the end date
   * @param purpose
   *          the purpose
   * @param comment
   *          the comment
   */
  public PeriodDto(Date start, Date end, String purpose, String comment) {
    this.start = notNull(start, "start");
    this.end = notNull(end, "end");
    this.purpose = purpose;
    this.comment = comment;
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
   * Sets the start date
   * 
   * @param start
   *          the start date
   */
  public void setStart(Date start) {
    this.start = start;
  }

  /**
   * Returns the start date
   * 
   * @return the start date
   */
  public Date getStart() {
    return start;
  }

  /**
   * Sets the end date
   * 
   * @param end
   *          the end date
   */
  public void setEnd(Date end) {
    this.end = end;
  }

  /**
   * Returns the end date
   * 
   * @return the end date
   */
  public Date getEnd() {
    return end;
  }

  /**
   * Sets the purpose
   * 
   * @param purpose
   *          the purpose
   */
  public void setPurpose(String purpose) {
    this.purpose = purpose;
  }

  /**
   * Returns the purpose
   * 
   * @return the purpose
   */
  public String getPurpose() {
    return purpose;
  }

  /**
   * Sets the comment
   * 
   * @param comment
   *          the comment
   */
  public void setComment(String comment) {
    this.comment = comment;
  }

  /**
   * Returns the comment
   * 
   * @return the comment
   */
  public String getComment() {
    return comment;
  }

  /**
   * Returns the business object of this period
   * 
   * @return the business object model of this period
   */
  public Period toPeriod() {
    return period(id, start, end, purpose, comment);
  }

}
