package org.infrastructurebuilder.maven;

import static java.nio.file.Files.newOutputStream;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.infrastructurebuilder.maven.IBVersionsUtils.copyTree;

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
import java.util.Objects;

import javax.inject.Inject;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.io.RawInputStreamFacade;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.vdurmont.semver4j.Semver;

abstract public class GeneratorComponent {

  private static final String CLASS_FROM_PROJECT_ARTIFACT_ID = "classFromProjectArtifactId";
  private static final String SNAPSHOT = "-snapshot";
  private File workDirectory;
  private File outputDirectory;
  @Inject
  private MavenSession session;
  private BuildContext buildContext;
  @Inject
  private MavenProject project;
  @Inject
  private Log log;
  private String overriddenGeneratedClassName;
  @Inject
  private MavenResourcesFiltering mavenResourcesFiltering;
  private String apiVersionPropertyName;
  private File overriddenTemplateFile;
  private String encoding = "UTF-8";
  private String escapeString = "\\";
  private int copied = 0;

  public void execute() throws MojoExecutionException, IOException {

    if (this.apiVersionPropertyName != null) {
      Semver s = new Semver(project.getVersion());
      project.getProperties().setProperty(apiVersionPropertyName, s.getMajor() + "." + s.getMinor());
    }

    final File workDirectory = getWorkDirectory();

    if ("pom".equals(project.getPackaging())) {
      logInfo("Skipping a POM project type.");
      return;
    }
    logDebug("source=%s target=%s", workDirectory, getOutputDirectory());

    final Path p = getResourcePathString();
    if (!Files.exists(p))
      throw new MojoExecutionException("Path " + p.toAbsolutePath() + " does not exist");

    buildContext.removeMessages(workDirectory);

    copied = 0;

    project.getProperties().put(CLASS_FROM_PROJECT_ARTIFACT_ID, getClassNameFromArtifactId());

    final File temporaryDirectory = getTemporaryDirectory(workDirectory);

    logInfo("Copying files with filtering to temporary directory %s.", temporaryDirectory);

    filterSourceToTemporaryDir(workDirectory, temporaryDirectory);

    try {
      copied += copyTree(temporaryDirectory, getOutputDirectory(), getOutputDirectory(), getLog());
    } catch (final IOException e) {
      throw new MojoExecutionException("Failed to copy directory struct", e);
    }
//    FileUtils.deleteDirectory(temporaryDirectory);
    if (isSomethingBeenUpdated()) {
      buildContext.refresh(getOutputDirectory());
      logInfo("Copied %d files to output directory: %s", copied, getOutputDirectory());
    } else {
      logInfo("No files needs to be copied to output directory. Up to date: %s", getOutputDirectory());
    }
    project.getProperties().remove(CLASS_FROM_PROJECT_ARTIFACT_ID);
    addSourceFolderToProject(project);
    logInfo("Source directory: %s added.", getOutputDirectory());
  }

  protected Path getResourcePathString() throws MojoExecutionException {
    Path wp = this.workDirectory.toPath().toAbsolutePath();
    final Path filePath = Paths //
        .get(wp.toString(), requireNonNull(project.getGroupId()).split("\\.")) // source directory/the/group/id/expanded
        .resolve((isTestGeneration() ? "Test" : "") + getClassNameFromArtifactId() + "." + getType()); // final filename
    final Path templatePath = wp.resolve(filePath).toAbsolutePath();
    getLog().info("writing template to " + templatePath.toAbsolutePath());

    try {
      final Path parents = templatePath.getParent();
      if (!Files.exists(parents)) {
        Files.createDirectories(parents);
      }
      if (overriddenTemplateFile != null) {
        FileUtils.copyFile(overriddenTemplateFile, templatePath.toFile());
      } else {
        final String rPath = "/" + (isTestGeneration() ? "test-" : "") + "templates/" + "template." + getType();
        getLog().info("Target path for copied resource is " + templatePath);
        try (InputStream res = getClass().getResourceAsStream(rPath); OutputStream os = newOutputStream(templatePath)) {
          FileUtils.copyStreamToFile(new RawInputStreamFacade(res), templatePath.toFile());
        }
      }
    } catch (final IOException e) {
      throw new MojoExecutionException("Failed to copy files", e);
    }
    return templatePath;
  }

  protected int countCopiedFiles() {
    return copied;
  }

  protected String getClassNameFromArtifactId() {
    if (overriddenGeneratedClassName != null)
      return overriddenGeneratedClassName;

    String ver = project.getVersion();
    if (ver.toLowerCase().endsWith(SNAPSHOT))
      ver = ver.substring(0, ver.length() - SNAPSHOT.length()); // Remove snapshotting
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

  private void filterSourceToTemporaryDir(final File workingDirectory, final File temporaryDirectory)
      throws MojoExecutionException {
    final List<Resource> resources = new ArrayList<Resource>();
    final Resource resource = new Resource();
    resource.setFiltering(true);
    logDebug("Source absolute path: %s", workingDirectory.getAbsolutePath());
    resource.setDirectory(workingDirectory.getAbsolutePath());
    resources.add(resource);

    final MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution(resources, temporaryDirectory,
        project, encoding, emptyList(), emptyList(), session);
    mavenResourcesExecution.setInjectProjectBuildFilters(true);
    mavenResourcesExecution.setEscapeString(escapeString);
    mavenResourcesExecution.setOverwrite(true);
    final LinkedHashSet<String> delims = new LinkedHashSet<String>();
    delims.add("@");
    mavenResourcesExecution.setDelimiters(delims);
    try {
      mavenResourcesFiltering.filterResources(mavenResourcesExecution);
    } catch (final MavenFilteringException e) {
      buildContext.addMessage(getWorkDirectory(), 1, 1, "Filtering Exception", BuildContext.SEVERITY_ERROR, e);
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  private File getWorkDirectory() throws MojoExecutionException {
    final Path wd = this.workDirectory.toPath().toAbsolutePath();
    if (wd == null)
      throw new MojoExecutionException("Null work directory");
    if (!Files.exists(wd)) {
      try {
        Files.createDirectories(wd);
      } catch (final IOException e) {
        throw new MojoExecutionException("Failed to create  " + wd, e);
      }
    }
    return wd.toFile();
  }

  private File getTemporaryDirectory(final File sourceDirectory) throws MojoExecutionException {
    final Path basedir = project.getBasedir().toPath();
    Path target = Paths.get(project.getBuild().getDirectory());
    target = target.isAbsolute() ? target : basedir.resolve(target);
    try {
      return Files.createTempDirectory(target, "templates-tmp").toFile();
    } catch (IOException e) {
      throw new MojoExecutionException("Cannot create temp dir", e);
    }
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

  protected String getType() {
    return "java";
  }

  public void setProject(MavenProject project) {
    this.project = project;
  }

  public void setBuildContext(BuildContext buildContext) {
    this.buildContext = buildContext;
  }

  public void setSession(MavenSession session2) {
    this.session = session2;
  }

  public File getOutputDirectory() {
    return outputDirectory;
  }

  public Log getLog() {
    return log;
  }

  public void setOverriddenGeneratedClassName(final String overriddenGeneratedClassName) {
    this.overriddenGeneratedClassName = overriddenGeneratedClassName;
  }

  protected boolean isTestGeneration() {
    return false;
  }

  public void setMavenResourcesFiltering(MavenResourcesFiltering mavenResourcesFiltering) {
    this.mavenResourcesFiltering = mavenResourcesFiltering;
  }

  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }

  public void setApiVersionPropertyName(String apiVersionPropertyName) {
    this.apiVersionPropertyName = apiVersionPropertyName;
  }

  public void setOverriddenTemplateFile(File overriddenTemplateFile) {
    this.overriddenTemplateFile = overriddenTemplateFile;
  }

  public void setOutputDirectory(File outputDirectory) {
    getLog().info("Setting outputDirectory to " + outputDirectory.toPath().toAbsolutePath().toString());
    this.outputDirectory = outputDirectory;
  }

  public void setWorkDirectory(File workDirectory) {
    this.workDirectory = Objects.requireNonNull(workDirectory, "Work directory cannot be null");
  }

  public void setLog(Log log) {
    this.log = log;
  }

  protected abstract void addSourceFolderToProject(MavenProject mavenProject);
}
