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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.infrastructurebuilder.util.core.DefaultGAV;
import org.infrastructurebuilder.util.core.IBUtils;
import org.infrastructurebuilder.util.core.TestingPathSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractBase {
  public final static Logger pLogger = LoggerFactory.getLogger(AbstractBase.class);
  public static final String GENERATE_VERSION = "generate-version";
  public static final String GENERATE_TEST_VERSION = "generate-test-version";
  public static final String TARGET = "target";
  public static final String GENERATED_SOURCES = "generated-sources";
  public static final String GENERATED_VERSION_TEMPLATES = "generated-version-templates";
  public static final String API_VERSION = "apiVersion";
  private static final TestingPathSupplier tps = new TestingPathSupplier();

  private Path testWorkDirectory;
  protected Path testOutputDirectory;

  protected GeneratorComponent component;

  public AbstractBase() {
    super();
  }

  public Path getCopyOfTestingWorkingPath(Path name) throws IOException {

    Path p = getTps().getTestClasses().resolve(name);
    Path testPath = getTps().get();
    IBUtils.copyTree(p, testPath);
    return testPath;
  }

  public TestingPathSupplier getTps() {
    return tps;
  }

  @AfterEach
  public void after() {
    getTps().finalize();
  }

  @BeforeEach
  public void before() throws Throwable {
    Path tod = Paths.get(GENERATED_SOURCES).resolve(GENERATED_VERSION_TEMPLATES);
    Path testProject = getProject();
    Path targetDir = testProject.resolve(TARGET);// Paths.get(testProject.getBuild().getDirectory());

    this.component = getComponent();
    testOutputDirectory = targetDir.resolve(tod);
    testWorkDirectory = targetDir.resolve(this.component.isTestGeneration() ? GENERATE_TEST_VERSION : GENERATE_VERSION);

    final Path target = targetDir.resolve(tod); // testProject.getBasedir().toPath().resolve(testOutputDirectory);
    IBUtils.deletePath(target);

    this.component.setGAV(new DefaultGAV("org.sample:sample:1.0.0"));
    this.component.setWorkDirectory(testWorkDirectory);
    this.component.setOverriddenGeneratedClassName(null);
    this.component.setOverriddenTemplateFile(IBVersionsUtils.pathOrNull(null));
  }

  abstract protected GeneratorComponent getComponent();

  abstract public Path getProject() throws Throwable;
}
