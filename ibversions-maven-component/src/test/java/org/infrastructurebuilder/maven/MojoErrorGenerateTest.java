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

import java.io.File;

public class MojoErrorGenerateTest {
  public File failingParam = new File("/non/existing/path");

//  @Test(expected = NullPointerException.class)
//  public void testBadDirectoryDoesNotAddSourceFolder() throws MojoExecutionException, MavenFilteringException {
//    final GenerateJavaMojo filterSourcesMojo = new GenerateJavaMojo() {
//      @Override
//      protected void addSourceFolderToProject(final MavenProject mavenProject) {
//        throw new IllegalArgumentException();
//      }
//    };
//    filterSourcesMojo.setWorkDirectory(failingParam);
//    MavenProject project = new MavenProject();
//    Model model =  new Model();
//    model.setGroupId("x.w");
//    model.setArtifactId("z");
//    model.setVersion("1.0.0");
//    project.setModel(model);
//    Build build = new Build();
//    build.setDirectory(Paths.get(".").toAbsolutePath().toString());
//    project.setBuild(build);
//    BuildContext bc = new DefaultBuildContext();
//    filterSourcesMojo.setBuildContext(bc);
//    filterSourcesMojo.setProject(project);
//
//    final MavenResourcesFiltering mock = mock(MavenResourcesFiltering.class);
//    filterSourcesMojo.mavenResourcesFiltering = mock;
//
//    filterSourcesMojo.execute();
//    verifyZeroInteractions(mock);
//  }
}
