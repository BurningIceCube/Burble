package com.bic.burble.story;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Placeholder bean that confirms the {@code story} module has been loaded
 * and wired into the application context by the orchestrator.
 */
@Component
public class StoryFoo {

  private static final Logger log = LoggerFactory.getLogger(StoryFoo.class);

  public StoryFoo() {
    log.info("Story module loaded");
  }
}
