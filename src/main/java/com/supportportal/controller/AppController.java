package com.supportportal.controller;

import com.supportportal.entity.User;
import com.supportportal.entity.dto.UserDTO;
import com.supportportal.exception.ExceptionHandling;
import com.supportportal.exception.domain.EmailExistException;
import com.supportportal.exception.domain.UserNotFoundException;
import com.supportportal.exception.domain.UsernameExistException;
import com.supportportal.service.interfaces.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = {"/","/supportPortal"})
public class AppController extends ExceptionHandling {

    @Autowired
    IUserService userService;

    @GetMapping(value = "/home")
    public String home(){
        return "Home Page";
    }

    @PostMapping(value = "/register")
    public ResponseEntity<User> registerUser(@RequestBody UserDTO userDTO) throws UserNotFoundException, EmailExistException, UsernameExistException {
        User registeredUser = userService.register(userDTO.getFirstName(), userDTO.getLastName(),
                userDTO.getUsername(), userDTO.getEmail());
        return new ResponseEntity<>(registeredUser, HttpStatus.OK);
    }
}
