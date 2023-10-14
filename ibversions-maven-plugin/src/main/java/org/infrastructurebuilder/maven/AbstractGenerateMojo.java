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
import java.io.IOException;
import java.util.Objects;

import javax.inject.Named;

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

  @Parameter(property = "maven.resources.escapeString")
  protected String escapeString;

  @Parameter(defaultValue = "${session}", required = true, readonly = true)
  private MavenSession session;

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  private MavenProject project;

  @Parameter(required = false, readonly = false)
  private String overriddenGeneratedClassName = null;

  @Parameter(required = false, readonly = false)
  protected File overriddenTemplateFile = null;

  @Parameter(property = "apiVersionPropertyName", required = false)
  private String apiVersionPropertyName;

  private final BuildContext buildContext;

  protected final MavenResourcesFiltering mavenResourcesFiltering;

  public AbstractGenerateMojo(BuildContext b, @Named("default") MavenResourcesFiltering f) {
    this.buildContext = Objects.requireNonNull(b);
    this.mavenResourcesFiltering = Objects.requireNonNull(f);
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    GeneratorComponent comp = getComponent();
    comp.setMavenResourcesFiltering(this.mavenResourcesFiltering);
    comp.setBuildContext(this.buildContext);
    comp.setOverriddenGeneratedClassName(this.overriddenGeneratedClassName);
    comp.setOverriddenTemplateFile(this.overriddenTemplateFile);
    comp.setProject(this.project);
    comp.setSession(this.session);
    comp.setEncoding(this.encoding);
    comp.setApiVersionPropertyName(this.apiVersionPropertyName);
    try {
      comp.execute();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      throw new MojoExecutionException("FAiled to execute", e);
    }
  }

  abstract GeneratorComponent getComponent();

//  public abstract File getOutputDirectory();
//
//  abstract public Path getWorkDirectory();
//
//  abstract public void setWorkDirectory(File workDir);
//
  protected boolean isTestGeneration() {
    return false;
  }

}
