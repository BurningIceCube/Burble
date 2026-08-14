package com.bic.burble.story;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/story")
public class StoryController {

  @GetMapping("/foo")
  public Map<String, String> foo() {
    return Map.of(
        "module", "story",
        "status", "ok",
        "message", "bar"
    );
  }
}
