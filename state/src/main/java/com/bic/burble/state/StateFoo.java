package com.bic.burble.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Placeholder bean that confirms the {@code state} module has been loaded
 * and wired into the application context by the orchestrator.
 */
@Component
public class StateFoo {

  private static final Logger log = LoggerFactory.getLogger(StateFoo.class);

  public StateFoo() {
    log.info("State module loaded");
  }
}
