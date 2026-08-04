package com.lorenzosanz.uploadservice.validation.constants;

import java.util.Set;

public final class AllowedContentTypes {
  
  private AllowedContentTypes() {}

  public static final Set<String> VALUES = Set.of(
    "image/png",
    "image/jpeg",
    "image/webp",
    "image/gif"
  );
}
