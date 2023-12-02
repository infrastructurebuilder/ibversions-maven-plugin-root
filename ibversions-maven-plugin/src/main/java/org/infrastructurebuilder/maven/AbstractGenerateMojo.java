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

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.util.StringUtils;
import org.infrastructurebuilder.util.core.DefaultGAV;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.vdurmont.semver4j.Semver;

public abstract class AbstractGenerateMojo extends AbstractMojo {

  private static final String CLASS_FROM_PROJECT_ARTIFACT_ID = "classFromProjectArtifactId";

  @Parameter(defaultValue = "${project.build.sourceEncoding}")
  private String encoding;

  @Parameter(property = "maven.resources.escapeString") // TODO Maybe?
  protected String escapeString;

  @Parameter(defaultValue = "${session}", required = true, readonly = true)
  private MavenSession session;

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  private MavenProject project;

  @Parameter(required = false, readonly = false)
  private String overriddenGeneratedClassName = null;

  @Parameter(required = false, readonly = false)
  protected File overriddenTemplateFile = null;

  @Parameter(required = false, readonly = true)
  private String hint;

  @Parameter(property = "apiVersionPropertyName", required = false)
  private String apiVersionPropertyName;

  @Parameter(property = "apiVersionPropertyNameSafe", required = false)
  private String apiVersionPropertyNameSafe;

  private final BuildContext buildContext;

  protected final MavenResourcesFiltering mavenResourcesFiltering;

  private final GeneratorComponent component;

  private Optional<IBVersionsComponentExecutionResult> result;


  public AbstractGenerateMojo(BuildContext b, MavenResourcesFiltering f, Map<String, GeneratorComponent> components) {
    this.buildContext = requireNonNull(b);
    this.mavenResourcesFiltering = requireNonNull(f);
    // IF there is a hint, use that to lookup component. Otherwise type
    getLog().debug("Looking for hint = " + this.hint + " or type = " + getComponentHint());

    this.component = requireNonNull(components.get(this.hint == null ? getComponentHint() : this.hint),
        "No viable components");
  }

  protected abstract String getComponentHint();
  
  public void setApiVersionPropertyName(String apiVersionPropertyName) {
    getLog().info("Setting property name to " + apiVersionPropertyName);
    this.apiVersionPropertyName = apiVersionPropertyName;
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (!StringUtils.isBlank(this.apiVersionPropertyName)) {
      Semver s = new Semver(project.getVersion());
      Properties p = project.getProperties();
      p.setProperty(apiVersionPropertyName, s.getMajor() + "." + s.getMinor());
      getLog().debug(String.format("Property %s set to %s", this.apiVersionPropertyName,
          p.getProperty(this.apiVersionPropertyName)));
    }
    if (!StringUtils.isBlank(this.apiVersionPropertyNameSafe)) {
      Semver s = new Semver(project.getVersion());
      Properties p = project.getProperties();
      p.setProperty(apiVersionPropertyNameSafe, s.getMajor() + "_" + s.getMinor());
      getLog().debug(String.format("Property %s set to %s", this.apiVersionPropertyNameSafe,
          p.getProperty(this.apiVersionPropertyNameSafe)));
      
    }
    if ("pom".equals(project.getPackaging())) {
      getLog().info("Skipping a POM project type.");
      return;
    }

    buildContext.removeMessages(getWorkDirectory());

    try {
      this.component.setGAV(new DefaultGAV(project.getGroupId(), project.getArtifactId(), project.getVersion()));

      this.component.setWorkDirectory(getWorkDirectory().toPath());
      this.component.setOverriddenGeneratedClassName(this.overriddenGeneratedClassName);
      this.component.setOverriddenTemplateFile(IBVersionsUtils.pathOrNull(this.overriddenTemplateFile));
      this.result = this.component.execute();
      if (this.result.isEmpty()) {
        getLog().info("ibversions component returned no result");
        return;
      }
      var r = this.result.get();
      // Time to filter
      if (r.getAddedSourcesCount() > 0) {
        filterSourceToFilteredOutputDir(r, null, getOutputDirectory().toPath());
        r.getSources().ifPresent(source -> {
          this.project.addCompileSourceRoot(getOutputDirectory().toString());
        });
        r.getTestSources().ifPresent(source -> {
          this.project.addTestCompileSourceRoot(getOutputDirectory().toString());
        });
        this.buildContext.refresh(getOutputDirectory());
        logInfo("Copied %d files to output directory: %s", r.getAddedSourcesCount(), getOutputDirectory());
      } else {
        logInfo("No files need to be copied to output directory. Up to date: %s", getOutputDirectory());
      }
    } catch (IOException e) {
      throw new MojoExecutionException("Failed to execute", e);
    }
  }

  private void filterSourceToFilteredOutputDir(IBVersionsComponentExecutionResult result, Path workingDirectory,
      final Path filteredOutputDirectory) throws IOException {
    Files.createDirectories(filteredOutputDirectory);
    Path source = result.getSources().orElseGet(() -> result.getTestSources().get());
    if (source == null) {
      getLog().warn("No results for filtering");
      return;
    }
    final Resource resource = new Resource();
    resource.setFiltering(true);
    getLog().debug(String.format("Source absolute path: %s", source.toAbsolutePath()));
    resource.setDirectory(source.toAbsolutePath().toString());

    project.getProperties().setProperty(CLASS_FROM_PROJECT_ARTIFACT_ID, result.getClassFromArtifactId());
    final MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution(List.of(resource),
        filteredOutputDirectory.toFile(), project, encoding, emptyList(), emptyList(), session);
    mavenResourcesExecution.setInjectProjectBuildFilters(true);
    mavenResourcesExecution.setEscapeString(escapeString);
    mavenResourcesExecution.setOverwrite(true);
    final LinkedHashSet<String> delims = new LinkedHashSet<String>();
    delims.add("@");
    mavenResourcesExecution.setDelimiters(delims);
    try {
      mavenResourcesFiltering.filterResources(mavenResourcesExecution);
      project.getProperties().remove(CLASS_FROM_PROJECT_ARTIFACT_ID);
    } catch (final MavenFilteringException e) {
      buildContext.addMessage(/* getWorkDirectory() */ workingDirectory.toFile(), 1, 1, "Filtering Exception",
          BuildContext.SEVERITY_ERROR, e);
      throw new IOException(e.getMessage(), e);
    }
  }

  private void logInfo(final String format, final Object... args) {
    if (getLog().isInfoEnabled()) {
      getLog().info(String.format(format, args));
    }
  }

  /**
   * IS this execution type a test generation? Override to return true if a test mojo
   *
   * @return true if this is a test generation mojo
   */
  abstract protected boolean isTestGeneration();

  /**
   * Override for other language types with other components
   *
   * @return
   */
  abstract protected String getMojoHint();
  /* These two have defaults set (by Maven) in the real mojos */

  abstract protected File getWorkDirectory();

  abstract protected File getOutputDirectory();

}
