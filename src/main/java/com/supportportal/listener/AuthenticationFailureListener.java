package com.supportportal.listener;

import com.supportportal.service.LoginAttemptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
public class AuthenticationFailureListener {
    private final LoginAttemptService loginAttemptService;

    @Autowired
    public AuthenticationFailureListener(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    /* AuthenticationFailureBadCredentialsEvent - event that gets triggered when
    * user provides bad credentials */
    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event){
        Object principle = event.getAuthentication().getPrincipal();
        /* In the controller we pass in UsernamePasswordAuthenticationToken a string
        * but setting an Object. Hence, we check here if that returned is an instance
        * of String */
        if(principle instanceof String){
            String username = (String) event.getAuthentication().getPrincipal();
            loginAttemptService.addUserToCache(username);
        }
    }
}
