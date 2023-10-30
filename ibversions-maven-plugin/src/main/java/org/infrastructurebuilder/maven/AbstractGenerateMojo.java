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

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.sonatype.plexus.build.incremental.BuildContext;

public abstract class AbstractGenerateMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project.build.sourceEncoding}")
  private String encoding;

//  @Parameter(property = "maven.resources.escapeString") // TODO Maybe?
//  protected String escapeString;

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

  private final BuildContext buildContext;

  protected final MavenResourcesFiltering mavenResourcesFiltering;

  private final GeneratorComponent component;

  public AbstractGenerateMojo(BuildContext b, MavenResourcesFiltering f, Map<String, GeneratorComponent> components) {
    this.buildContext = requireNonNull(b);
    this.mavenResourcesFiltering = requireNonNull(f);
    // IF there is a hint, use that to lookup component. Otherwise type
    getLog().debug("Looking for hint = " + this.hint + " or type = " + getType());

    this.component = requireNonNull(components.get(this.hint == null ? getType() : this.hint), "No viable components");
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      this.component.setOutputDirectory(getOutputDirectory().toPath());
      this.component.setWorkDirectory(getWorkDirectory().toPath());
      this.component.setMavenResourcesFiltering(this.mavenResourcesFiltering);
      this.component.setBuildContext(this.buildContext);
      this.component.setOverriddenGeneratedClassName(this.overriddenGeneratedClassName);
      this.component.setOverriddenTemplateFile(IBVersionsUtils.pathOrNull(this.overriddenTemplateFile));
      this.component.setProject(this.project);
      this.component.setSession(this.session);
      this.component.setEncoding(this.encoding);
      this.component.setApiVersionPropertyName(this.apiVersionPropertyName);
      this.component.execute();
    } catch (IOException e) {
      throw new MojoExecutionException("Failed to execute", e);
    }
  }

  protected boolean isTestGeneration() {
    return false;
  }

  /**
   * Override for other language types with other components
   *
   * @return
   */
  protected String getType() {
    return JavaGeneratorComponent.JAVA;
  }

  /* These two have defaults set (by Maven) in the real mojos */

  abstract protected File getWorkDirectory();

  abstract protected File getOutputDirectory();

}
