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
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.sonatype.plexus.build.incremental.BuildContext;

@Mojo(name = "generate-java-test-version", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES, threadSafe = true)
public class GenerateTestMojo extends AbstractGenerateMojo {

  private GeneratorComponent component;

  @Parameter(defaultValue = "${project.build.directory}/generated-test-sources/generated-test-version")
  private File testOutputDirectory;

  @Parameter(defaultValue = "${project.build.directory}/generate-test-version", required = true, readonly = false)
  private File workDirectory;

  @Override
  GeneratorComponent getComponent() {
    component.setOutputDirectory(this.testOutputDirectory);
    component.setWorkDirectory(this.workDirectory);
    return component;
  }

  @Inject
  public GenerateTestMojo(BuildContext b, @Named("default") MavenResourcesFiltering f,
      @Named(JavaTestGeneratorComponent.JAVA_TEST) GeneratorComponent c)
  {
    super(b, f);
    this.component = Objects.requireNonNull(c);
  }
}
