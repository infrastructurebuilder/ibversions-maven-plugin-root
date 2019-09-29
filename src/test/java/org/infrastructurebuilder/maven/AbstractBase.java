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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.apache.maven.shared.utils.io.FileUtils;
import org.infrastructurebuilder.maven.AbstractGenerateMojo;
import org.infrastructurebuilder.maven.GenerateJavaMojo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sonatype.plexus.build.incremental.BuildContext;

@RunWith(MockitoJUnitRunner.class)
public abstract class AbstractBase {

  protected static class MockCopyAnswer implements Answer<Void> {

    @Override
    public Void answer(final InvocationOnMock invocation) throws Throwable {
      final MavenResourcesExecution arg = MavenResourcesExecution.class.cast(invocation.getArguments()[0]);
      final File source = new File(arg.getResources().iterator().next().getDirectory());
      assertTrue(source.exists());
      assertTrue(source.isDirectory());
      final File destination = arg.getOutputDirectory();
      assertFalse(destination.exists());
      FileUtils.copyDirectoryStructure(source, destination);
      return null;
    }
  }

  protected static File resolve(final File file, final String... paths) {
    final StringBuilder sb = new StringBuilder(file.getPath());
    for (final String path : paths) {
      sb.append(File.separator).append(path);
    }
    return new File(sb.toString());
  }

  @Mock
  private MavenSession session;
  @Mock
  protected MavenResourcesFiltering mavenResourcesFiltering;
  @Spy
  private final MavenProject _project = ProjectStub.createProjectForITExample("test1");
  @Spy
  private final File _workDirectory = resolve(_project.getBasedir(), "target", "generate-version");
  @Spy
  protected File outputDirectory = resolve(new File("target"), "generated-sources", "generated-version-templates");
  @Mock
  protected BuildContext buildContext;

  @InjectMocks
  protected AbstractGenerateMojo mojo = new GenerateJavaMojo();

  public AbstractBase() {
    super();
  }

  @Before
  public void before() throws IOException {
    final File target = resolve(getProject().getBasedir(), outputDirectory.getPath());
    mojo.setApiVersionPropertyName("apiVersion");
    FileUtils.forceDelete(target);
  }

  @Test(expected = MojoExecutionException.class)
  public void testCleanupFail() throws MojoExecutionException {
    AbstractGenerateMojo.cleanupTemporaryDirectory(null);
  }

  @Test(expected = IOException.class)
  public void testCopyDirectoryStructionWithIO() throws IOException {
    AbstractGenerateMojo.copyDirectoryStructureWithIO(null, null, null);
  }

  @Test(expected = IOException.class)
  public void testCopyDirectoryStructionWithIO2() throws IOException {
    AbstractGenerateMojo.copyDirectoryStructureWithIO(new File("X"), null, null);
  }

  @Test(expected = IOException.class)
  public void testCopyDirectoryStructionWithIO3() throws IOException {
    AbstractGenerateMojo.copyDirectoryStructureWithIO(new File("X"), new File("X"), null);
  }

  @Test(expected = IOException.class)
  public void testCopyDirectoryStructionWithIO4() throws IOException {
    AbstractGenerateMojo.copyDirectoryStructureWithIO(new File("X"), new File("Z"), new File("Y"));
  }

  abstract protected MavenProject getProject();

}