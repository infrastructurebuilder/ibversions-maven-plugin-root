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

import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.newOutputStream;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.zip.CRC32;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.util.FileUtils;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.vdurmont.semver4j.Semver;

public abstract class AbstractGenerateMojo extends AbstractMojo {
  public static void cleanupTemporaryDirectory(final File temporaryDirectory) throws MojoExecutionException {
    try {
      FileUtils.forceDelete(temporaryDirectory);
    } catch (IOException | NullPointerException ex) {
      throw new MojoExecutionException(ex.getMessage(), ex);
    }
  }

  public static void copy(final InputStream source, final OutputStream sink) throws IOException {
    final byte[] buffer = new byte[10240];
    for (int n = 0; (n = requireNonNull(source, "source").read(buffer)) > 0;) {
      requireNonNull(sink, "sink").write(buffer, 0, n);
    }
    return;
  }

  public static int copyDirectoryStructureWithIO(final File sourceDirectory, final File destinationDirectory,
      final File rootDestinationDirectory) throws IOException {
    int copied = 0;
    if (sourceDirectory == null)
      throw new IOException("source directory can't be null.");

    if (destinationDirectory == null)
      throw new IOException("destination directory can't be null.");

    if (sourceDirectory.equals(destinationDirectory))
      throw new IOException("source and destination are the same directory.");

    if (!sourceDirectory.exists())
      throw new IOException("Source directory doesn't exists (" + sourceDirectory.getAbsolutePath() + ").");
    File[] files = sourceDirectory.listFiles();
    files = files == null ? new File[0] : files;
    final String sourcePath = sourceDirectory.getAbsolutePath();

    for (final File file : files) {
      if (file.equals(rootDestinationDirectory)) {

        continue;
      }

      String dest = file.getAbsolutePath();

      dest = dest.substring(sourcePath.length() + 1);

      File destination = new File(destinationDirectory, dest);

      if (file.isFile()) {
        destination = destination.getParentFile();

        if (isFileDifferent(file, destination)) {
          copied++;
          FileUtils.copyFileToDirectory(file, destination);
        }
      } else if (file.isDirectory()) {
        if (!destination.exists() && !destination.mkdirs())
          throw new IOException("Could not create destination directory '" + destination.getAbsolutePath() + "'.");

        copied += copyDirectoryStructureWithIO(file, destination, rootDestinationDirectory);
      } else
        throw new IOException("Unknown file type: " + file.getAbsolutePath());
    }
    return copied;
  }

  private static boolean isFileDifferent(final File file, final File directory) throws IOException {
    return true;
  }

  private static File resolve(final File file, final String... subfile) {
    final StringBuilder path = new StringBuilder();
    path.append(file.getPath());
    for (final String fi : subfile) {
      path.append(File.separator);
      path.append(fi);
    }
    return new File(path.toString());
  }

  private int copied = 0;

  @Component
  private BuildContext buildContext;

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

  public void setApiVersionPropertyName(String apiVersionPropertyName) {
    this.apiVersionPropertyName = apiVersionPropertyName;
  }

  @Component(hint = "default")
  protected MavenResourcesFiltering mavenResourcesFiltering;

  @Override
  public void execute() throws MojoExecutionException {

    if (this.apiVersionPropertyName != null) {
      Semver s = new Semver(project.getVersion());
      project.getProperties().setProperty(apiVersionPropertyName, s.getMajor() + "." + s.getMinor());
      Properties test = project.getProperties();
      String test2 = test.getProperty(apiVersionPropertyName);
    }

    final File sourceDirectory = getSourceDirectory();

    if ("pom".equals(project.getPackaging())) {
      logInfo("Skipping a POM project type.");
      return;
    }
    logDebug("source=%s target=%s", sourceDirectory, getOutputDirectory());
    if (!(sourceDirectory != null && sourceDirectory.exists())) {
      logInfo("Request to add '%s' folder. Not added since it does not exist.", sourceDirectory);
      throw new MojoExecutionException("Source " + sourceDirectory + " does not exist");
    }

    final Path p = getResourcePathString();
    if (!Files.exists(p))
      throw new MojoExecutionException("Path " + p.toAbsolutePath() + " does not exist");

    buildContext.removeMessages(sourceDirectory);

    copied = 0;
    project.getProperties().put("classFromProjectArtifactId", getClassNameFromArtifactId());
    final File temporaryDirectory = getTemporaryDirectory(sourceDirectory);
    logInfo("Copying files with filtering to temporary directory.");
    logDebug("Temporary director for filtering is: %s", temporaryDirectory);
    filterSourceToTemporaryDir(sourceDirectory, temporaryDirectory);

    try {
      copied += copyDirectoryStructureWithIO(temporaryDirectory, getOutputDirectory(), getOutputDirectory());
    } catch (final IOException e) {
      throw new MojoExecutionException("Failed to copy directory struct", e);
    }
    cleanupTemporaryDirectory(temporaryDirectory);
    if (isSomethingBeenUpdated()) {
      buildContext.refresh(getOutputDirectory());
      logInfo("Copied %d files to output directory: %s", copied, getOutputDirectory());
    } else {
      logInfo("No files needs to be copied to output directory. Up to date: %s", getOutputDirectory());
    }

    addSourceFolderToProject(project);
    logInfo("Source directory: %s added.", getOutputDirectory());
  }

  public abstract File getOutputDirectory();

  abstract public Path getWorkDirectory();

  public void setOverriddenGeneratedClassName(final String overriddenGeneratedClassName) {
    this.overriddenGeneratedClassName = overriddenGeneratedClassName;
  }

  abstract public void setWorkDirectory(File workDir);

  private void filterSourceToTemporaryDir(final File sourceDirectory, final File temporaryDirectory)
      throws MojoExecutionException {
    final List<Resource> resources = new ArrayList<Resource>();
    final Resource resource = new Resource();
    resource.setFiltering(true);
    logDebug("Source absolute path: %s", sourceDirectory.getAbsolutePath());
    resource.setDirectory(sourceDirectory.getAbsolutePath());
    resources.add(resource);

    final MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution(resources, temporaryDirectory,
        project, encoding, emptyList(), emptyList(), session);
    mavenResourcesExecution.setInjectProjectBuildFilters(true);
    mavenResourcesExecution.setEscapeString(escapeString);
    mavenResourcesExecution.setOverwrite(true);
    setDelimitersForExecution(mavenResourcesExecution);
    try {
      mavenResourcesFiltering.filterResources(mavenResourcesExecution);
    } catch (final MavenFilteringException e) {
      buildContext.addMessage(getSourceDirectory(), 1, 1, "Filtering Exception", BuildContext.SEVERITY_ERROR, e);
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  private File getSourceDirectory() throws MojoExecutionException {
    final Path wd = getWorkDirectory();
    if (wd == null)
      throw new MojoExecutionException("Null work directory");
    if (!Files.exists(wd)) {
      try {
        Files.createDirectories(wd);
      } catch (final IOException e) {
        throw new MojoExecutionException("Failed to create  " + wd, e);
      }
    }
    return getWorkDirectory().toFile();
  }

  private File getTemporaryDirectory(final File sourceDirectory) throws MojoExecutionException {
    final File basedir = project.getBasedir();
    final File target = new File(project.getBuild().getDirectory());
    final StringBuilder label = new StringBuilder("templates-tmp");
    final CRC32 crcMaker = new CRC32();
    crcMaker.update(sourceDirectory.getPath().getBytes());
    label.append(crcMaker.getValue());
    final String subfile = label.toString();
    return target.isAbsolute() ? resolve(target, subfile) : resolve(basedir, target.getPath(), subfile);
  }

  private boolean isSomethingBeenUpdated() {
    return copied > 0;
  }

  private void logDebug(final String format, final Object... args) {
    if (getLog().isDebugEnabled()) {
      getLog().debug(String.format(format, args));
    }
  }

  private void logInfo(final String format, final Object... args) {
    if (getLog().isInfoEnabled()) {
      getLog().info(String.format(format, args));
    }
  }

  private void setDelimitersForExecution(final MavenResourcesExecution mavenResourcesExecution) {
    final LinkedHashSet<String> delims = new LinkedHashSet<String>();
    delims.add("@");
    mavenResourcesExecution.setDelimiters(delims);
  }

  protected abstract void addSourceFolderToProject(MavenProject mavenProject);

  protected int countCopiedFiles() {
    return copied;
  }

  protected String getClassNameFromArtifactId() {
    if (overriddenGeneratedClassName != null)
      return overriddenGeneratedClassName;

    String ver = project.getVersion();
    if (ver.toLowerCase().endsWith("-snapshot"))
      ver = ver.substring(0, ver.length() - 9); // Remove snapshotting
    final String nonJavaMethodName = project.getArtifactId();
    final StringBuilder nameBuilder = new StringBuilder();
    boolean capitalizeNextChar = true;
    boolean first = true;

    for (int i = 0; i < nonJavaMethodName.length(); i++) {
      char c = nonJavaMethodName.charAt(i);
      if (c == '.')
        c = '_';
      if (c != '_' && !Character.isLetterOrDigit(c)) {
        if (!first) {
          capitalizeNextChar = true;
        }
      } else {
        nameBuilder.append(capitalizeNextChar ? Character.toUpperCase(c) : Character.toLowerCase(c));
        capitalizeNextChar = false;
        first = false;
      }
    }

    nameBuilder.append("Versioning");
    return nameBuilder.toString();

  }

  protected Path getResourcePathString() throws MojoExecutionException {
    final Path filePath = Paths
        .get(getWorkDirectory().toString(), requireNonNull(project.getGroupId()).split("\\."))
        .resolve((isTestGeneration() ? "Test" : "") + getClassNameFromArtifactId() + "." + getType());
    final Path tPath = getWorkDirectory().resolve(filePath).toAbsolutePath();
    getLog().info("writing template to " + tPath.toAbsolutePath());

    try {
      final Path parents = tPath.getParent();
      if (!Files.exists(parents)) {
        Files.createDirectories(parents);
      }
      if (overriddenTemplateFile != null) {
        try (InputStream ins = newInputStream(overriddenTemplateFile.toPath());
            OutputStream os = newOutputStream(tPath)) {
          copy(ins, os);
        }
      } else {
        final String rPath = "/" + (isTestGeneration() ? "test-" : "") + "templates/" + "template." + getType();
        getLog().info("Target path for copied resource is " + tPath);
        try (InputStream res = getClass().getResourceAsStream(rPath); OutputStream os = newOutputStream(tPath)) {
          copy(res, os);
        }
      }
    } catch (final IOException e) {
      throw new MojoExecutionException("Failed to copy files", e);
    }
    return tPath;
  }

  protected String getType() {
    return "java";
  }

  protected boolean isTestGeneration() {
    return false;
  }

}
