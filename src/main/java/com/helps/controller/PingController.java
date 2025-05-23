package com.helps.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class PingController {

    @MessageMapping("/ping")
    @SendTo("/topic/pong")
    public String ping(String message) {
        return "pong";
    }
}