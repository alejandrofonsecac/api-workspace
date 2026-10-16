package com.workspace.api.ping;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public keep-alive target for an external monitor that stops Render from idling the instance.
 * It reveals nothing and touches no database or external service, so it costs no more than a rejected request.
 */
@RestController
public class PingController {
    @GetMapping("/ping")
    public ResponseEntity<Void> ping() {
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
