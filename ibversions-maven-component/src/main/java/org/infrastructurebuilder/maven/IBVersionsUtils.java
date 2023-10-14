package org.infrastructurebuilder.maven;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.maven.plugin.logging.Log;

public interface IBVersionsUtils {
  public static int copyTree(final File sourceDirectory, final File destinationDirectory,
      final File rootDestinationDirectory, Log log) throws IOException {
    requireNonNull(sourceDirectory, "Source dir cannot be null");
    requireNonNull(destinationDirectory, "Destination dir cannot be null");

    AtomicInteger copied = new AtomicInteger();

    if (sourceDirectory.equals(destinationDirectory))
      throw new IOException("source and destination are the same directory.");

    if (!sourceDirectory.exists())
      throw new IOException("Source directory doesn't exists (" + sourceDirectory.getAbsolutePath() + ").");
    Path sPath = sourceDirectory.toPath();
    Path dPath = destinationDirectory.toPath();
    log.info("Copying " + sPath + " to " + dPath);
    Files.walkFileTree(sPath, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        Path newPath = dPath.resolve(dir);
        log.debug("Creating " + newPath);
        Files.createDirectories(newPath);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        log.debug("visitFile " + file);
        Files.copy(sPath.resolve(file), dPath.resolve(file));
        copied.incrementAndGet();
        return FileVisitResult.CONTINUE;
      }

    });
    return copied.get();
  }

}
