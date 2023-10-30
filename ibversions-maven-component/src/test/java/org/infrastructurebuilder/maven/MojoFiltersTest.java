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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Test;

public class MojoFiltersTest {
  private final org.codehaus.plexus.logging.Logger logger = new ConsoleLogger();
  @Test
  public void dontAddSourceFolder() throws IOException {
    final StringBuilder placeholder = new StringBuilder();
    final JavaGeneratorComponent jc = new JavaGeneratorComponent();
    jc.enableLogging(logger);
    jc.setOutputDirectory(Paths.get("."));
    Path f = jc.getOutputDirectory();

    final MavenProject mock = new MavenProject();

    jc.addSourceFolderToProject(mock);
    assertFalse(mock.getCompileSourceRoots().contains(f));


  }
}
