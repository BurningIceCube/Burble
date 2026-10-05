package com.bic.burble.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Placeholder bean that confirms the {@code game} module has been loaded
 * and wired into the application context by the orchestrator.
 */
@Component
public class GameFoo {

  private static final Logger log = LoggerFactory.getLogger(GameFoo.class);

  public GameFoo() {
    log.info("Game module loaded");
  }
}
