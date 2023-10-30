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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.input.XmlStreamReader;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class ProjectStub extends MavenProject {
  public static MavenProject createProjectForITExample(final Path exampleName) throws IOException, XmlPullParserException, URISyntaxException {

    final Path load = exampleName.resolve("pom.xml");
    assertTrue(Files.exists(load));
    assertTrue(Files.isRegularFile(load));
    return new ProjectStub(exampleName);

  }

  private final File basedir;

  public ProjectStub(final Path basedir) {
    this.basedir = basedir.toFile();
    initiate();
  }

  @Override
  public File getBasedir() {
    return basedir;
  }

  private void initiate() {
    final MavenXpp3Reader pomReader = new MavenXpp3Reader();
    Model model;
    try {
      model = pomReader.read(new XmlStreamReader(new File(getBasedir(), "pom.xml")));
      setModel(model);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }

    final Build build = getBuild();
    build.setFinalName(model.getArtifactId());
    build.setDirectory(getBasedir() + "/target");
    build.setSourceDirectory(getBasedir() + "/src/main/java");
    build.setOutputDirectory(getBasedir() + "/target/classes");
    build.setTestSourceDirectory(getBasedir() + "/src/test/java");
    build.setTestOutputDirectory(getBasedir() + "/target/test-classes");

    final List<String> compileSourceRoots = new ArrayList<String>();
    compileSourceRoots.add(getBasedir() + "/src/main/java");
    setCompileSourceRoots(compileSourceRoots);

    final List<String> testCompileSourceRoots = new ArrayList<String>();
    testCompileSourceRoots.add(getBasedir() + "/src/test/java");
    setTestCompileSourceRoots(testCompileSourceRoots);


  }

}
