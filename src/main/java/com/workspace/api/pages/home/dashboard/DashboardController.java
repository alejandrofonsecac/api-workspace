package com.workspace.api.pages.home.dashboard;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController
@RequiredArgsConstructor
@RequestMapping("/home/dashboard")
public class DashboardController {
    private final DashboardService dashboard;
    @GetMapping public DashboardService.Dashboard get() { return dashboard.get(); }
}

