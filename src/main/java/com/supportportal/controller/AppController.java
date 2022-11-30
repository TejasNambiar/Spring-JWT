package com.supportportal.controller;

import com.supportportal.exception.ExceptionHandling;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/supportPortal")
public class AppController extends ExceptionHandling {
    @GetMapping(value = "/home")
    public String home(){
        return "Home Page";
    }
}
