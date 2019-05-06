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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class Mojo1Test extends AbstractBase {
  @Spy
  private final MavenProject project = ProjectStub.createProjectForITExample("test1");

  @Spy
  private final File workDirectory = resolve(project.getBasedir(), "target", "generate-version");

  @Override
  public MavenProject getProject() {
    return project;
  }

  public File getWorkDirectory() {
    return workDirectory;
  }

  @Test
  public void testExecute() throws MojoExecutionException, MavenFilteringException {

    mojo.overriddenTemplateFile = null;

    doAnswer(new MockCopyAnswer()).when(mavenResourcesFiltering).filterResources(any(MavenResourcesExecution.class));

    mojo.execute();
    assertEquals(1, mojo.countCopiedFiles());

    verify(mavenResourcesFiltering, times(1)).filterResources(any(MavenResourcesExecution.class));
    verify(buildContext, times(1)).refresh(outputDirectory);
    verify(getProject(), times(1)).addCompileSourceRoot(outputDirectory.getAbsolutePath());
  }

  @Test(expected = NullPointerException.class)
  public void testExecuteSetNullSource() throws MojoExecutionException, MavenFilteringException {
    mojo.setWorkDirectory(null);
  }

  @Test
  public void testGetOutputDirectory() {

    final File file = mojo.getOutputDirectory();
    assertNotNull(file);
    assertTrue(file.getPath().contains("generated-version-templates"));
  }

}