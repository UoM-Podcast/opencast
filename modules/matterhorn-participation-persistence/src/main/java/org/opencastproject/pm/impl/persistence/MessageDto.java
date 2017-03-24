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

import org.opencastproject.messages.persistence.MessageSignatureDto;
import org.opencastproject.messages.persistence.MessageTemplateDto;
import org.opencastproject.pm.api.Message;
import org.opencastproject.security.api.UserDirectoryService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Entity object for messages.
 */
@Entity(name = "Message")
@Table(name = "mh_pm_message")
@NamedQueries({
        @NamedQuery(name = "Message.findAll", query = "SELECT m FROM Message m"),
        @NamedQuery(name = "Message.findByRecording", query = "SELECT m FROM Recording r INNER JOIN r.messages m WHERE r.id = :recording"),
        @NamedQuery(name = "Message.findBySeries", query = "SELECT m FROM Recording r INNER JOIN r.messages m WHERE r.course.seriesId = :series"),
        @NamedQuery(name = "Message.count", query = "SELECT COUNT(m) FROM Message m"),
        @NamedQuery(name = "Message.countByDateRange", query = "SELECT COUNT(m) FROM Message m WHERE m.creationDate >= :start AND m.creationDate < :end"),
        @NamedQuery(name = "Message.countErrors", query = "SELECT COUNT(m) FROM Message m WHERE m.errors IS NOT EMPTY"),
        @NamedQuery(name = "Message.clear", query = "DELETE FROM Message") })
public class MessageDto {

  @Id
  @GeneratedValue
  @Column(name = "id")
  private long id;

  @Column(name = "creation_date", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date creationDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "template", referencedColumnName = "id")
  private MessageTemplateDto template;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "signature", referencedColumnName = "id")
  private MessageSignatureDto signature;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "creator", referencedColumnName = "id")
  private PersonDto creator;

  @OneToMany(fetch = FetchType.LAZY, cascade = { CascadeType.REMOVE })
  private List<ErrorDto> errors = new ArrayList<ErrorDto>();

  /**
   * Default constructor
   */
  public MessageDto() {
  }

  /**
   * Creates a message
   * 
   * @param creationDate
   *          the creationDate
   * @param creator
   *          the creator of the message
   * @param template
   *          the template
   * @param signature
   *          the signature
   * @param errors
   *          the list of errors related to the message
   */
  public MessageDto(Date creationDate, PersonDto creator, MessageTemplateDto template, MessageSignatureDto signature,
          List<ErrorDto> errors) {
    this.creationDate = creationDate;
    this.creator = notNull(creator, "creator");
    this.template = template;
    this.signature = signature;
    this.errors = notNull(errors, "errors");
  }

  /**
   * Creates a message with current date as creation date
   * 
   * @param template
   *          the template
   * @param signature
   *          the signature
   */
  public MessageDto(MessageTemplateDto template, MessageSignatureDto signature) {
    this.creationDate = new Date();
    this.template = template;
    this.signature = signature;
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
   * Sets the creation date
   * 
   * @param creationDate
   *          the creation date
   */
  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }

  /**
   * Returns the creation date
   * 
   * @return the creation date
   */
  public Date getCreationDate() {
    return creationDate;
  }

  /**
   * Sets the creator
   * 
   * @param creator
   *          the creator of the message
   */
  public void setCreator(PersonDto creator) {
    this.creator = creator;
  }

  /**
   * Returns the creator
   * 
   * @returns the creator of the message
   */
  public PersonDto getCreator() {
    return this.creator;
  }

  /**
   * Sets the message template
   * 
   * @param template
   *          the message template
   */
  public void setTemplate(MessageTemplateDto template) {
    this.template = template;
  }

  /**
   * Returns the message template
   * 
   * @return the message template
   */
  public MessageTemplateDto getTemplate() {
    return template;
  }

  /**
   * Sets the message signature
   * 
   * @param signature
   *          the message signature
   */
  public void setSignature(MessageSignatureDto signature) {
    this.signature = signature;
  }

  /**
   * Returns the message signature
   * 
   * @return the message signature
   */
  public MessageSignatureDto getSignature() {
    return signature;
  }

  /**
   * Sets a message error list.
   * 
   * @param errors
   *          the error list
   */
  public void setErrors(List<ErrorDto> errors) {
    this.errors = notNull(errors, "errors");
  }

  /**
   * Returns the error list
   * 
   * @return the error list
   */
  public List<ErrorDto> getErrors() {
    return errors;
  }

  /**
   * Add an error to the message
   * 
   * @param error
   *          the error to add to this message
   * 
   * @return true if this collection changed as a result of the call
   */
  public boolean addError(ErrorDto error) {
    return errors.add(notNull(error, "error"));
  }

  /**
   * Remove an error from the message
   * 
   * @param error
   *          the error to remove from this message
   * 
   * @return true if this collection changed as a result of the call
   */
  public boolean removeError(ErrorDto error) {
    return errors.remove(notNull(error, "error"));
  }

  /**
   * Returns the business object of this message
   * 
   * @return the business object model of this message
   */
  public Message toMessage(UserDirectoryService userDirectoryService) {
    Message msg = new Message(creator.toPerson(), template.toMessageTemplate(userDirectoryService),
            signature.toMessageSignature(userDirectoryService));
    msg.setId(id);
    msg.setCreationDate(creationDate);
    for (ErrorDto error : errors) {
      msg.addError(error.toError());
    }
    return msg;
  }

}
