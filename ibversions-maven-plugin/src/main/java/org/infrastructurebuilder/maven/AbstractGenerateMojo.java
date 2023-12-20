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
import static java.util.Objects.requireNonNullElse;
import static org.infrastructurebuilder.maven.IBVersionsUtils.pathOrNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.util.StringUtils;
import org.infrastructurebuilder.util.versions.DefaultGAVBasic;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.Semver.SemverType;

public abstract class AbstractGenerateMojo extends AbstractMojo {

  public static final String PACKAGE_NAME_PROPERTY = "__targetVersionsPackageName";

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

  @Parameter(defaultValue = "${project.groupId}", required = false, readonly = false)
  private String targetPackageName;

  @Parameter(required = false, readonly = false)
  private String hint;

  @Parameter(property = "apiVersionPropertyName", required = false)
  private String apiVersionPropertyName;

  @Parameter(property = "apiVersionPropertyNameSafe", required = false)
  private String apiVersionPropertyNameSafe;

  private final BuildContext buildContext;

  protected final MavenResourcesFiltering mavenResourcesFiltering;

  private final GeneratorComponent component;

  private Optional<IBVersionsComponentExecutionResult> result;

  public AbstractGenerateMojo(BuildContext b, //
      MavenResourcesFiltering f, // 
//      MavenFileFilter ff, //
      Map<String, GeneratorComponent> components)
  {
    this.buildContext = requireNonNull(b);
    this.mavenResourcesFiltering = requireNonNull(f);
    // IF there is a hint, use that to lookup component. Otherwise type
    var theHint = this.hint == null ? getComponentHint() : this.hint;
    logDebug("Looking for hint = %s", theHint);
    this.component = requireNonNull(components.get(theHint), "No viable components");
    logDebug("Got component %s", this.component.getHint());
  }

  protected abstract String getComponentHint();

  public void setApiVersionPropertyName(String apiVersionPropertyName) {
    logInfo("Setting property name to %s", apiVersionPropertyName);
    this.apiVersionPropertyName = apiVersionPropertyName;
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    String tpn = this.targetPackageName;
    Semver s = new Semver(project.getVersion() + ".0", SemverType.LOOSE);
    String apiVersion = s.getMajor() + "." + s.getMinor();
    String safeVersion = s.getMajor() + "_" + s.getMinor();
    Properties p = project.getProperties();
    if (p.containsKey(PACKAGE_NAME_PROPERTY))
      throw new MojoFailureException("Cannot generate versions due to conflicting target package name property");
    if (!StringUtils.isBlank(this.apiVersionPropertyName)) {

      p.setProperty(apiVersionPropertyName, apiVersion);
      logDebug("Property %s set to %s", this.apiVersionPropertyName, apiVersion);
      tpn = filter(tpn, this.apiVersionPropertyName, apiVersion);
    }
    if (!StringUtils.isBlank(this.apiVersionPropertyNameSafe)) {
      p.setProperty(apiVersionPropertyNameSafe, safeVersion);
      logDebug("Property %s set to %s", this.apiVersionPropertyNameSafe, safeVersion);
      tpn = filter(tpn, this.apiVersionPropertyNameSafe, safeVersion);
    }

    if ("pom".equals(project.getPackaging())) {
      logInfo("Skipping a POM project type.");
      return;
    }
    buildContext.removeMessages(getWorkDirectory());

    try {
      this.component.setGAV(new DefaultGAVBasic(project.getGroupId(), project.getArtifactId(), project.getVersion()));
      this.component.setOverriddenPackageName(tpn);
      this.component.setWorkDirectory(getWorkDirectory().toPath());
      this.component.setOverriddenGeneratedClassName(this.overriddenGeneratedClassName);
      this.component.setOverriddenTemplateFile(pathOrNull(this.overriddenTemplateFile));
      this.result = this.component.execute();
      if (this.result.isEmpty()) {
        logInfo("ibversions component returned no result");
        return;
      }
      var r = this.result.get();
      logDebug("Target package name is ", r.getTargetPackageName());
      p.setProperty(PACKAGE_NAME_PROPERTY, r.getTargetPackageName());
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
    } finally {
      p.remove(PACKAGE_NAME_PROPERTY);
    }
  }

  // Arbitrary delimiters
  private String filter(String existing, String name, String sub) {
    String one = "@" + name + "@";
    String two = "${" + name + "}";
    existing = existing.replace(one, sub);
    existing = existing.replace(two,sub);
    return existing;
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
    logDebug("Source absolute path: %s", source.toAbsolutePath());
    resource.setDirectory(source.toAbsolutePath().toString());

    project.getProperties().setProperty(CLASS_FROM_PROJECT_ARTIFACT_ID, result.getClassFromArtifactId());
    var resources = List.of(resource);
    final MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution( //
        resources, // Resources list
        filteredOutputDirectory.toFile(), // target
        project, encoding, //
        emptyList(), // filefilters
        emptyList(), // Non filtered extensions
        session);
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

  private void logDebug(final String format, final Object... args) {
    if (getLog().isDebugEnabled()) {
      getLog().debug(String.format(format, args));
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
