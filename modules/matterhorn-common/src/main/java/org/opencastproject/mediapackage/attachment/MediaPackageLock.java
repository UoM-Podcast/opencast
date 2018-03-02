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

package org.opencastproject.mediapackage.attachment;

import org.opencastproject.mediapackage.AbstractMediaPackageElement;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 *
 * @author Tobias M Schiebeck
 */
public class MediaPackageLock extends AbstractMediaPackageElement implements Attachment {

  public MediaPackageLock(MediaPackageElementFlavor flavor) {
    super(Type.Attachment, flavor, null);
  }

  public static class Adapter extends XmlAdapter<MediaPackageLock, Attachment> {

    @Override
    public MediaPackageLock marshal(Attachment mp) throws Exception {
      return (MediaPackageLock) mp;
    }

    @Override
    public Attachment unmarshal(MediaPackageLock mp) throws Exception {
      return mp;
    }
  }
}
