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

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.File;

import org.apache.maven.project.MavenProject;
import org.infrastructurebuilder.maven.GenerateJavaMojo;
import org.junit.Test;
import org.mockito.Mockito;

public class MojoFiltersTest {
  @Test
  public void dontAddSourceFolder() {
    final StringBuilder placeholder = new StringBuilder();
    final GenerateJavaMojo filterSourcesMojo = new GenerateJavaMojo() {
      @Override
      protected void addSourceFolderToProject(final MavenProject mavenProject) {
        placeholder.append("called");
      }
    };
    filterSourcesMojo.setOutputDirectory(new File("."));

    final MavenProject mock = mock(MavenProject.class);
    Mockito.doThrow(IllegalArgumentException.class).when(mock).addCompileSourceRoot(anyString());
    Mockito.doThrow(IllegalArgumentException.class).when(mock).addTestCompileSourceRoot(anyString());

    filterSourcesMojo.addSourceFolderToProject(mock);

    assertTrue(placeholder.toString().equals("called"));
    verify(mock, never()).addTestCompileSourceRoot(anyString());
  }
}
