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

package org.opencastproject.pm.workflow.handler;

import static org.easymock.EasyMock.anyObject;
import static org.junit.Assert.assertEquals;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class DublinCoreMergeWorkflowOperationHandlerTest {

  /** The operation handler to test */
  private DublinCoreMergeWorkflowOperationHandler operationHandler;

  /** The sample media package */
  private MediaPackage mediaPackage;

  /** The master catalog */
  private DublinCoreCatalog masterCatalog = null;

  /** The Dublin Core service */
  private DublinCoreCatalogService dcService = null;

  private Workspace workspace = null;

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();

    // test resources
    URI mediaPackageURI = DublinCoreMergeWorkflowOperationHandler.class.getResource("/manifest.xml").toURI();
    mediaPackage = builder.loadFromXml(mediaPackageURI.toURL().openStream());
    mediaPackage.setTitle("Land and Vegetation: Key players on the Climate Scene");

    // Instantiate the Dublin Core catalog service
    dcService = new DublinCoreCatalogService();

    // Mock the workspace
    workspace = EasyMock.createMock(Workspace.class);
    EasyMock.expect(workspace.put((String)anyObject(), (String)anyObject(), (String)anyObject(), (InputStream)anyObject())).andReturn(mediaPackageURI).once();
    workspace.delete((URI) anyObject());
    EasyMock.expectLastCall().once();
    EasyMock.replay(workspace);

    // set up the operation handler
    operationHandler = new DublinCoreMergeWorkflowOperationHandler() {
      protected DublinCoreCatalog loadCatalog(URI uri) throws NotFoundException, IOException {
        return DublinCoreMergeWorkflowOperationHandlerTest.this.loadCatalog(uri);
      }
    };

    // Connect it to the service
    operationHandler.setDublinCoreService(dcService);
    operationHandler.setWorkspace(workspace);
  }

  @Test
  public void testMerge() throws Exception {
    MediaPackage mp = operationHandler.merge(mediaPackage, MediaPackageElements.EPISODE);

    // Check that there is only one catalog of the given type
    assertEquals("Found more than one catalog after merge", 1, mp.getCatalogs(MediaPackageElements.EPISODE).length);

    // Check that this one catalog contains nothing but the merged content
    assertEquals("Found an unexpected number of elements", 4, masterCatalog.getValues().size());

    assertEquals("Found more than one title", 1, masterCatalog.get(DublinCore.PROPERTY_TITLE).size());
    assertEquals("Expected title does not match", "New title", masterCatalog.getFirst(DublinCore.PROPERTY_TITLE));

    assertEquals("Found more than one subject", 1, masterCatalog.get(DublinCore.PROPERTY_SUBJECT).size());
    assertEquals("Expected subjects does not match", "climate, land, vegetation", masterCatalog.getFirst(DublinCore.PROPERTY_SUBJECT));

    assertEquals("Found more than one identifier", 1, masterCatalog.get(DublinCore.PROPERTY_IDENTIFIER).size());
    assertEquals("Expected identifier does not match", "abc", masterCatalog.getFirst(DublinCore.PROPERTY_IDENTIFIER));

    assertEquals("Found more than one spatial", 1, masterCatalog.get(DublinCore.PROPERTY_SPATIAL).size());
    assertEquals("Expected spatial does not match", "HGE12", masterCatalog.getFirst(DublinCore.PROPERTY_SPATIAL));

    EasyMock.verify(workspace);
}

  /**
   * Loads the Dublin Core catalog from the given URI.
   *
   * @param uri
   *          the catalog's location
   * @return the catalog contents
   * @throws NotFoundException
   *           if the workspace can't find the URI
   * @throws IOException
   *           if the catalog file can't be opened or parsed
   */
  private DublinCoreCatalog loadCatalog(URI uri) throws NotFoundException, IOException {
    InputStream is = null;
    try {
      is = getClass().getResourceAsStream("/" + uri.getPath());
      DublinCoreCatalog dc = dcService.load(is);
      if ("dc-master.xml".equals(uri.getPath()))
        masterCatalog = dc;
      return dc;
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

}
