package com.bic.burble.shared;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Placeholder bean that confirms the {@code shared} module has been loaded
 * and wired into the application context by the orchestrator.
 */
@Component
public class SharedFoo {

  private static final Logger log = LoggerFactory.getLogger(SharedFoo.class);

  public SharedFoo() {
    log.info("Shared module loaded");
  }
}
