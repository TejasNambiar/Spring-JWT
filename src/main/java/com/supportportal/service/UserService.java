package com.supportportal.service;

import com.supportportal.entity.User;
import com.supportportal.entity.UserPrinciple;
import com.supportportal.repository.UserRepository;
import com.supportportal.service.interfaces.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@Qualifier("UserDetailsService")
public class UserService implements IUserService, UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> user = Optional.ofNullable(userRepository.findUserByUsername(username));
        if(user.isEmpty()) {
            log.error("User not found by username: " + username);
            throw new UsernameNotFoundException("User not found by username: " + username);
        }else{
            user.get().setLastLoginDateDisplay(user.get().getLastLoginDate());
            user.get().setLastLoginDate(new Date());
            userRepository.save(user.get());
            UserPrinciple userPrinciple = new UserPrinciple(user.get());
            log.info("Returning user by username: "+username);
            return  userPrinciple;
        }
    }
}
