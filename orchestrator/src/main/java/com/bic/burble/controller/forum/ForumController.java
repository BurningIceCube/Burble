package com.bic.burble.controller.forum;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Example endpoint for the {@code forum} module. Orchestrator hosts all
 * API endpoints; the forum module itself is a library with no web layer.
 */
@RestController
@RequestMapping("/api/v1/forum")
public class ForumController {

  @GetMapping("/foo")
  public Map<String, String> foo() {
    return Map.of(
        "module", "forum",
        "status", "ok",
        "message", "bar"
    );
  }
}
