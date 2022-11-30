package com.supportportal.exception.domain;

public class UserNameExistsException extends Exception{

    public UserNameExistsException(String message) {
        super(message);
    }
}
