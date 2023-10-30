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

import static org.infrastructurebuilder.maven.IBVersionsUtils.copyTree;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.DefaultMavenFileFilter;
import org.apache.maven.shared.filtering.DefaultMavenResourcesFiltering;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.infrastructurebuilder.util.core.TestingPathSupplier;
import org.joor.Reflect;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonatype.plexus.build.incremental.BuildContext;
import org.sonatype.plexus.build.incremental.DefaultBuildContext;

public abstract class AbstractBase {

  public static final String API_VERSION = "apiVersion";

  private final static TestingPathSupplier tps = new TestingPathSupplier();

  protected MavenFileFilter filter = new DefaultMavenFileFilter();
  protected MavenResourcesFiltering filtering = new DefaultMavenResourcesFiltering();
  private MavenProject testProject;
  private Path testWorkDirectory;
  protected Path testOutputDirectory;

  protected BuildContext buildContext = new DefaultBuildContext();

  protected GeneratorComponent component;

  private ConsoleLogger pLogger;

  private String encoding = "UTF-8";

  public AbstractBase() {
    super();
  }

  public Path getCopyOfTestingWorkingPath(Path name) throws IOException {

    Path p = getTps().getTestClasses().resolve(name);
    Path testPath = getTps().get();
    FileUtils.copyDirectory(p.toFile(), testPath.toFile());
    return testPath;
  }

  public TestingPathSupplier getTps() {
    return tps;
  }

  @After
  public void after() {
    getTps().finalize();
  }

  @SuppressWarnings("deprecation")
  @Before
  public void before() throws Throwable {
    pLogger = new ConsoleLogger();
    Reflect.on(filter) //
        .set("logger", pLogger) //
        .set("buildContext", buildContext);
    Reflect.on(filtering) //
        .set("mavenFileFilter", filter) //
        .set("buildContext", buildContext) //
        .set("logger", pLogger);
    testProject = getProject();
    Path targetDir = Paths.get(testProject.getBuild().getDirectory());

    this.component = getComponent();
    testWorkDirectory = targetDir.resolve("generate-version");
    testOutputDirectory = targetDir.resolve("generated-sources").resolve("generated-version-templates");

    final Path target = testProject.getBasedir().toPath().resolve(testOutputDirectory);
    FileUtils.deleteQuietly(target.toFile());

    this.component.setOutputDirectory(testOutputDirectory);
    this.component.setWorkDirectory(testWorkDirectory);
    this.component.setMavenResourcesFiltering(filtering);
    this.component.setBuildContext(buildContext);
    this.component.setOverriddenGeneratedClassName(null);
    this.component.setOverriddenTemplateFile(IBVersionsUtils.pathOrNull(null));
    this.component.setProject(testProject);
    this.component.setSession(new MavenSession(null, new DefaultMavenExecutionRequest(), new DefaultMavenExecutionResult(), testProject));
    this.component.setEncoding(this.encoding);
    this.component.setApiVersionPropertyName(API_VERSION);
    this.component.enableLogging(pLogger);
  }

  @Test
  public void testCopy() throws IOException {
    Path source = getTps().get();
    Path c = Paths.get("A").resolve("B").resolve("C");
    Path e = Paths.get("A").resolve("D").resolve("E");
    Path one = source.resolve(c);
    Path two = source.resolve(e);

    Files.createDirectories(one);
    Files.createDirectories(two);

    Path tempX = Files.createTempFile(one, "X", ".y");

    Path nameX = tempX.getFileName();
    Path tempZ = Files.createTempFile(two, "Z", ".y");
    Path nameZ = tempZ.getFileName();

    Path dest = getTps().get();
    Path targetX = dest.resolve(c).resolve(nameX);
    Path targetZ = dest.resolve(e).resolve(nameZ);

    copyTree(source, dest, pLogger);

    assertTrue(Files.isRegularFile(targetX));
    assertTrue(Files.isRegularFile(targetZ));

  }

  @Test(expected = NullPointerException.class)
  public void testCopyDirectoryStructionWithIO() throws IOException {
    copyTree(null, null, pLogger);
  }

  @Test(expected = NullPointerException.class)
  public void testCopyDirectoryStructionWithIO2() throws IOException {
    copyTree(Paths.get("X"), null, pLogger);
  }

  @Test(expected = IOException.class)
  public void testCopyDirectoryStructionWithIO3() throws IOException {
    copyTree(Paths.get("X"), Paths.get("X"), pLogger);
  }

  @Test(expected = IOException.class)
  public void testCopyDirectoryStructionWithIO4() throws IOException {
    copyTree(Paths.get("X"), Paths.get("Z"), pLogger);
  }

  public ConsoleLogger getLog() {
    return pLogger;
  }

  abstract protected MavenProject getProject() throws Throwable;

  abstract protected GeneratorComponent getComponent();

}