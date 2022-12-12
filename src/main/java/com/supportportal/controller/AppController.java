package com.supportportal.controller;

import com.supportportal.entity.User;
import com.supportportal.entity.UserPrinciple;
import com.supportportal.entity.dto.UserDTO;
import com.supportportal.exception.ExceptionHandling;
import com.supportportal.exception.domain.EmailExistException;
import com.supportportal.exception.domain.UserNotFoundException;
import com.supportportal.exception.domain.UsernameExistException;
import com.supportportal.security.JwtTokenProvider;
import com.supportportal.service.interfaces.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.mail.MessagingException;

import static com.supportportal.utility.constants.SecurityConstant.JWT_TOKEN_HEADER;

@RestController
@RequestMapping(path = {"/","/supportPortal"})
public class AppController extends ExceptionHandling {

    @Autowired
    IUserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @GetMapping(value = "/home")
    public String home(){
        return "Home Page";
    }

    @PostMapping(value = "/register")
    public ResponseEntity<User> registerUser(@RequestBody UserDTO userDTO) throws UserNotFoundException, EmailExistException, UsernameExistException, MessagingException {
        User registeredUser = userService.register(userDTO.getFirstName(), userDTO.getLastName(),
                userDTO.getUsername(), userDTO.getEmail());
        return new ResponseEntity<>(registeredUser, HttpStatus.OK);
    }

    @PostMapping(value = "/login")
    public ResponseEntity<User> login(@RequestBody UserDTO userDTO){
        authenticate(userDTO.getUsername(),userDTO.getPassword());
        User authenticatedUser = userService.findByUsername(userDTO.getUsername());
        // this will allow generation of JWT and its headers to be passed to UI
        UserPrinciple userPrinciple = new UserPrinciple(authenticatedUser);
        HttpHeaders jwtHeaders = getJwtHeader(userPrinciple);
        return new ResponseEntity<>(authenticatedUser, jwtHeaders, HttpStatus.OK);
    }

    private HttpHeaders getJwtHeader(UserPrinciple user) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(JWT_TOKEN_HEADER, jwtTokenProvider.generateJwtToken(user));
        return headers;
    }

    private void authenticate(String username, String password) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
        authenticationManager.authenticate(authenticationToken);
    }
}
