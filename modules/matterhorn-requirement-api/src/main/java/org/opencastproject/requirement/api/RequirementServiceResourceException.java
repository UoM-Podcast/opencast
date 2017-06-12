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

package org.opencastproject.requirement.api;

/**
 * Exception to indicate that the specified requirement target was invalid or
 * not supported by the RequirementService implementation
 *
 */
public class RequirementServiceResourceException extends RequirementServiceException {

  /**
   * Creates an exception with an error message.
   *
   * @param message the error message
   */
  public RequirementServiceResourceException(String message) {
    super(message);
  }

  /**
   * Creates an exception with an error message.
   *
   * @param cause the original cause for failure
   */
  public RequirementServiceResourceException(Throwable cause) {
    super(cause);
  }

  /**
   * Creates an exception with an error message and a cause.
   *
   * @param message the error message
   * @param cause the original cause
   */
  public RequirementServiceResourceException(String message, Throwable cause) {
    super(message, cause);
  }
}
