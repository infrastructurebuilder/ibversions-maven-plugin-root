package org.infrastructurebuilder;

import java.util.function.Supplier;

public interface IBVersionsSupplier  {
  Supplier<String> getArtifact();
  Supplier<String> getAPIVersion();
}
