package com.bic.burble.controller.story;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Example endpoint for the {@code story} module. Orchestrator hosts all
 * API endpoints; the story module itself is a library with no web layer.
 */
@RestController
@RequestMapping("/api/v1/story")
public class StoryController {

  private static final Logger log = LoggerFactory.getLogger(StoryController.class);

  @GetMapping("/foo")
  public Map<String, String> foo() {
    log.info("GET  /api/v1/story/foo");
    return Map.of(
        "module", "story",
        "status", "ok",
        "message", "bar"
    );
  }
}
