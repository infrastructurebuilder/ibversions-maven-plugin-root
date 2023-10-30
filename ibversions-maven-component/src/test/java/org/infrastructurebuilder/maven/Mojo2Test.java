/**
 * Copyright © 2019 admin (admin@infrastructurebuilder.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.infrastructurebuilder.maven;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

public class Mojo2Test extends AbstractBase {

  public Mojo2Test() {
    super();
  }

  @Override
  public Path getProject() throws Throwable {
    return getCopyOfTestingWorkingPath(Paths.get("test2"));
  }

  @Test
  public void testExecute() throws IOException {
    this.component.setOverriddenTemplateFile(null);
    var res = this.component.execute();
    assertEquals(1, res.get().getAddedSourcesCount());
    final Path file = this.component.getWorkDirectory();
    assertNotNull(file);
//    assertTrue(file.isDirectory());

//    verify(mavenResourcesFiltering, times(1)).filterResources(any(MavenResourcesExecution.class));
//    verify(buildContext, times(1)).refresh(outputDirectory);
//    verify(getProject(), times(1)).addCompileSourceRoot(outputDirectory.getAbsolutePath());
  }

  @Override
  protected JavaTestGeneratorComponent getComponent() {
    return new JavaTestGeneratorComponent();
  }

}
