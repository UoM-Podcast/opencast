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

package org.opencastproject.pm.ui.admin.jobs;

import static org.opencastproject.util.EqualsUtil.eqObj;

import java.util.Date;

/** Describes a Job (Workflow). */
public final class Job {
  public static final String PROP_TITLE = "title";
  public static final String PROP_PRESENTER = "presenter";
  public static final String PROP_DATE = "date";
  public static final String PROP_SERIES = "series";
  public static final String PROP_STATUS = "status";

  private long id;
  private String title;
  private String presenter;
  private String series;
  private Date date;
  private String status;

  public Job(long id, String title, String presenter, String series, Date date, String status) {
    this.id = id;
    this.title = title;
    this.presenter = presenter;
    this.series = series;
    this.date = date;
    this.status = status;
  }

  public String getTitle() {
    return title;
  }

  public String getPresenter() {
    return presenter;
  }

  public String getSeries() {
    return series;
  }

  public Date getDate() {
    return date;
  }

  public String getStatus() {
    return status;
  }

  @Override
  public boolean equals(Object that) {
    return (this == that) || (that instanceof Job && eqFields((Job) that));
  }

  private boolean eqFields(Job that) {
    return eqObj(this.id, that.id);
  }

  @Override
  public int hashCode() {
    return (int) id;
  }
}
