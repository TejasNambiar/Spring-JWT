package com.supportportal.service.interfaces;

import com.supportportal.entity.User;
import com.supportportal.exception.domain.EmailExistException;
import com.supportportal.exception.domain.UserNotFoundException;
import com.supportportal.exception.domain.UsernameExistException;

import java.util.List;

public interface IUserService {

    User register(String fName, String lName, String username, String email) throws UserNotFoundException, EmailExistException, UsernameExistException;

    List<User> getUsers();

    User findByUsername(String username);

    User findByEmail(String email);
}
