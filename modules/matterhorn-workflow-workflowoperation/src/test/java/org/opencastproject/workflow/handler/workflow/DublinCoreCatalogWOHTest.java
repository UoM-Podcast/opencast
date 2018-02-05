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
package org.opencastproject.workflow.handler.workflow;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workspace.api.Workspace;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Test class for {@link TagWorkflowOperationHandler}
 */
public class DublinCoreCatalogWOHTest {

  private DublinCoreCatalogWOH operationHandler;
  private WorkflowInstanceImpl instance;
  private WorkflowOperationInstanceImpl operation;
  private MediaPackage mp;
  private Workspace workspace;
  private DublinCoreCatalogService dublincore;
  private Capture<DublinCoreCatalog> dc = new Capture();

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    mp = builder.loadFromXml(this.getClass().getResourceAsStream("/archive_mediapackage.xml"));
    mp.getCatalog("catalog-1").setURI(this.getClass().getResource("/dublincore.xml").toURI());

    // set up the handler
    operationHandler = new DublinCoreCatalogWOH();

    // Initialize the workflow
    instance = new WorkflowInstanceImpl();
    operation = new WorkflowOperationInstanceImpl("test", OperationState.INSTANTIATED);
    List<WorkflowOperationInstance> ops = new ArrayList<>();
    ops.add(operation);
    instance.setOperations(ops);
    instance.setMediaPackage(mp);

    workspace = EasyMock.createNiceMock(Workspace.class);
    URI uri = this.getClass().getResource("/dublincore.xml").toURI();
    EasyMock.expect(workspace.read((URI) EasyMock.anyObject())).andReturn(
            new File(uri));
    EasyMock.expect(workspace.put(EasyMock.anyString(), EasyMock.anyString(),
            EasyMock.anyString(), (InputStream)EasyMock.anyObject())).andReturn(uri);
    EasyMock.replay(workspace);
    operationHandler.setWorkspace(workspace);

    InputStream mockInputStream =  EasyMock.createMock(InputStream.class);
    dublincore =  EasyMock.createNiceMock(DublinCoreCatalogService.class);
    EasyMock.expect(dublincore.serialize(EasyMock.capture(dc))).andReturn(mockInputStream);
    EasyMock.replay(dublincore);
    operationHandler.setDublincoreService(dublincore);
  }

  @Test
  public void testSetAvailable() throws Exception {
    operation.setConfiguration(DublinCoreCatalogWOH.SOURCE_FLAVORS_PROPERTY, "dublincore/episode");
    operation.setConfiguration(DublinCoreCatalogWOH.SET_AVAILABLE_DATE_PROPERTY, "true");

    operationHandler.start(instance, null);
    DublinCoreCatalog dccatalog = dc.getValue();

    Assert.assertTrue(dccatalog.hasValue(DublinCore.PROPERTY_AVAILABLE));
  }
}
