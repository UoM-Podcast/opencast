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

package org.opencastproject.composer.api;

/**
 * Exception that occurs while embedding.
 *
 */
public class EmbedderException extends Exception {

  /** serial version uid */
  private static final long serialVersionUID = -2355590632530442679L;

  /**
   * Creates exception with given error message.
   *
   * @param message
   */
  public EmbedderException(String message) {
    super(message);
  }

  /**
   * Creates exception with given error message and {@link Throwable} as cause.
   *
   * @param message
   * @param cause
   */
  public EmbedderException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates exception with {@link Throwable} as cause.
   *
   * @param cause
   */
  public EmbedderException(Throwable cause) {
    super(cause);
  }

  // TODO store embedder engine as well
}
