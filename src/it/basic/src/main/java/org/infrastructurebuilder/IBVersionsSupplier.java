package org.infrastructurebuilder;

import java.util.function.Supplier;

public interface IBVersionsSupplier {
  default Supplier<String> getArtifactDependency() {
    return () -> getGroupId().get() + ":" + getArtifactId().get() + ":" + getVersion().get() + ":"
        + getExtension().get();
  }

  default Supplier<String> getVersionedArtifactDependency() {
    return () -> getGroupId().get() + ":" + getArtifactId().get() + ":" + getAPIVersion().get();
  }

  Supplier<String> getGroupId();

  Supplier<String> getArtifactId();

  Supplier<String> getVersion();

  Supplier<String> getExtension();

  Supplier<String> getAPIVersion();
}
