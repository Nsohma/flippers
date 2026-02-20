package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

    @GetMapping({"/", "/pos", "/pos/", "/handy", "/handy/", "/catalog", "/catalog/"})
    public String forwardSpaRoutes() {
        return "forward:/index.html";
    }
}
