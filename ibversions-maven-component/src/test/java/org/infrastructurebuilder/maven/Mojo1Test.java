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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.junit.Test;

public class Mojo1Test extends AbstractBase {
  private MavenProject project;
  private Path tests;

  @Override
  public MavenProject getProject() throws Throwable {
    tests = getCopyOfTestingWorkingPath(Paths.get("test1"));
    project = ProjectStub.createProjectForITExample(tests);
    return project;
  }

  public File getWorkDirectory() {
    return tests.resolve("target").resolve("generate-version").toFile();
  }

  @Test
  public void testExecute() throws Throwable {
    this.component.setOverriddenTemplateFile(null);
    this.component.execute();
    assertEquals(1, this.component.countCopiedFiles());

//    verify(filtering, times(1)).filterResources(any(MavenResourcesExecution.class));
//    verify(buildContext, times(1)).refresh(outputDirectory);
//    verify(getProject(), times(1)).addCompileSourceRoot(outputDirectory.getAbsolutePath());
  }

  @Test(expected = NullPointerException.class)
  public void testExecuteSetNullSource() throws MojoExecutionException, MavenFilteringException, IOException {
    this.component.setWorkDirectory(null);
  }

  @Test
  public void testGetOutputDirectory() {

    final Path file = this.component.getOutputDirectory();
    assertNotNull(file);
    assertTrue(file.toString().contains("generated-version-templates"));
  }

  @Override
  protected GeneratorComponent getComponent() {
    JavaGeneratorComponent jc = new JavaGeneratorComponent();
    jc.enableLogging(getLog());
    return jc;
  }
}