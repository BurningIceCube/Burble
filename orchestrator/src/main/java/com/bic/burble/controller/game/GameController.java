package com.bic.burble.controller.game;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Example endpoint for the {@code game} module. Orchestrator hosts all
 * API endpoints; the game module itself is a library with no web layer.
 */
@RestController
@RequestMapping("/api/v1/game")
public class GameController {

  private static final Logger log = LoggerFactory.getLogger(GameController.class);

  @GetMapping("/foo")
  public Map<String, String> foo() {
    log.info("GET  /api/v1/game/foo");
    return Map.of(
        "module", "game",
        "status", "ok",
        "message", "bar"
    );
  }
}
