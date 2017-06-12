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

package org.opencastproject.pm.impl;

import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.pm.api.TemplateRenderer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.io.StringWriter;

import freemarker.template.Configuration;
import freemarker.template.Template;


/** Template renderer based on the Freemarker template engine. */
public class FreemarkerTemplateRenderer implements TemplateRenderer<String, Object> {

  private static final Logger logger = LoggerFactory.getLogger(FreemarkerTemplateRenderer.class);
  private final Configuration cfg = new Configuration();

  /** Takes as data everything freemarker allows. */
  @Override
  public String render(String template, Object data) {
    final StringWriter out = new StringWriter();
    cfg.setWhitespaceStripping(true);
    try {
      final Template t = new Template("template", new StringReader(template), cfg);
      t.process(data, out);
    } catch (Exception e) {
      logger.warn("Error rendering template for data " + data, e);
      return chuck(e);
    }
    return out.toString();
  }
}
