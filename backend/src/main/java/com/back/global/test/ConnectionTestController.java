package com.back.global.test;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConnectionTestController {

    @GetMapping("/api/connection-test")
    public String connectionTest() {
        return "backend connected";
    }
}