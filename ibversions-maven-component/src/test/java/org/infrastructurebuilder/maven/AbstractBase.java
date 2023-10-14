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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.DefaultMavenFileFilter;
import org.apache.maven.shared.filtering.DefaultMavenResourcesFiltering;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.infrastructurebuilder.exceptions.IBException;
import org.infrastructurebuilder.util.core.TestingPathSupplier;
import org.infrastructurebuilder.util.logging.LoggingMavenComponent;
import org.joor.Reflect;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.plexus.build.incremental.BuildContext;
import org.sonatype.plexus.build.incremental.DefaultBuildContext;

public abstract class AbstractBase {

  private final static TestingPathSupplier tps = new TestingPathSupplier();

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final Log log = new LoggingMavenComponent(logger);
  @Mock
  private MavenSession session;
  protected MavenFileFilter filter = new DefaultMavenFileFilter();
  protected MavenResourcesFiltering filtering = new DefaultMavenResourcesFiltering();
  private MavenProject _project;
  private File _workDirectory;
  protected File outputDirectory;

  protected BuildContext buildContext = new DefaultBuildContext();


  protected GeneratorComponent component;

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

  @Before
  public void before() throws Throwable {
    org.codehaus.plexus.logging.Logger pLogger = new ConsoleLogger();
    MockitoAnnotations.initMocks(this);
    Reflect.on(filter) //
    .set("logger", pLogger) //
    .set("buildContext", buildContext);
    Reflect.on(filtering) //
    .set("mavenFileFilter", filter) //
    .set("buildContext", buildContext) //
    .set("logger", pLogger);
    _project = getProject();
    Path targetDir = Paths.get(_project.getBuild().getOutputDirectory());

    this.component = getComponent();
    _workDirectory = targetDir.resolve("generate-version").toFile();
    outputDirectory = targetDir.resolve("generated-sources").resolve("generated-version-templates").toFile();


    final File target = _project.getBasedir().toPath().resolve(outputDirectory.toPath()).toFile();
    FileUtils.deleteQuietly(target);

    MavenExecutionRequest req  = new DefaultMavenExecutionRequest();
    session = new MavenSession(null, req  , null, _project);

    this.component.setProject(_project);
    this.component.setSession(session);
    this.component.setBuildContext(buildContext);
    this.component.setMavenResourcesFiltering(filtering);
    this.component.setApiVersionPropertyName("apiVersion");
    this.component.setWorkDirectory(_workDirectory);
    this.component.setOutputDirectory(outputDirectory);
    this.component.setLog(log);
  }

  @Test(expected = NullPointerException.class)
  public void testCopyDirectoryStructionWithIO() throws IOException {
    copyTree(null, null, null, log);
  }

  @Test(expected = NullPointerException.class)
  public void testCopyDirectoryStructionWithIO2() throws IOException {
    copyTree(new File("X"), null, null, log);
  }

  @Test(expected = IOException.class)
  public void testCopyDirectoryStructionWithIO3() throws IOException {
    copyTree(new File("X"), new File("X"), null, log);
  }

  @Test(expected = IOException.class)
  public void testCopyDirectoryStructionWithIO4() throws IOException {
    copyTree(new File("X"), new File("Z"), new File("Y"), log);
  }

  public Log getLog() {
    return log;
  }

  abstract protected MavenProject getProject() throws Throwable;


  abstract protected GeneratorComponent getComponent();

}