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

import org.codehaus.plexus.logging.Logger;

public interface IBVersionsUtils {
  public static int copyTree(final Path sPath, final Path dPath, Logger log)
      throws IOException {
    requireNonNull(sPath, "Source dir cannot be null");
    requireNonNull(dPath, "Destination dir cannot be null");

    AtomicInteger copied = new AtomicInteger();

    if (sPath.equals(dPath))
      throw new IOException("source and destination are the same directory.");

    if (!Files.exists(sPath) || !Files.isDirectory(sPath))
      throw new IOException("Source isn't a directory (" + sPath + ").");
    Path rootPath = sPath.getParent();
    log.info("rootPath is " + rootPath);
    log.info("Copying " + sPath + " to " + dPath);
    Files.walkFileTree(sPath, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        super.postVisitDirectory(dir, exc);
        // Otherwise...
        Path relDir = rootPath.relativize(dir);
        Path newPath = dPath.resolve(relDir);
        log.info("Creating " + newPath);
        Files.createDirectories(newPath);
        return FileVisitResult.CONTINUE;
      }


      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        super.visitFile(file, attrs);
        // then
        log.info("visitFile " + file);
        Path relFile = sPath.relativize(file);
        log.info("relFile is " + relFile);
        Path sFile = sPath.resolve(relFile);
        log.info("copyFile  " + sFile);
        Path target = dPath.resolve(relFile);
        log.info(" to       " + target);
        if (!Files.isDirectory(target.getParent()))
          Files.createDirectories(target.getParent());
        Files.copy(sFile, target);
        copied.incrementAndGet();
        return FileVisitResult.CONTINUE;
      }

    });
    return copied.get();
  }

  public static Path pathOrNull(File file) {
    return (file != null) ? file.toPath() : null;
  }

}
