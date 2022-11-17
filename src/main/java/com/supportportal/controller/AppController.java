package com.supportportal.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/supportPortal")
public class AppController {
    @GetMapping(value = "/home")
    public String home(){
        return "Home Page";
    }
}
