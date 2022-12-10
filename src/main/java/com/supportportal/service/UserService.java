package com.supportportal.service;

import com.supportportal.entity.User;
import com.supportportal.entity.UserPrinciple;
import com.supportportal.exception.domain.EmailExistException;
import com.supportportal.exception.domain.UserNotFoundException;
import com.supportportal.exception.domain.UsernameExistException;
import com.supportportal.repository.UserRepository;
import com.supportportal.service.interfaces.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static com.supportportal.security.enumeration.Role.ROLE_USER;
import static com.supportportal.utility.constants.Constants.*;

@Slf4j
@Service
@Transactional
@Qualifier("UserDetailsService")
public class UserService implements IUserService, UserDetailsService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;

    @Autowired
    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException{
        Optional<User> user = Optional.ofNullable(userRepository.findUserByUsername(username));
        if(user.isEmpty()) {
            log.error(NO_USER_FOUND_BY_USERNAME + username);
            throw new UsernameNotFoundException(NO_USER_FOUND_BY_USERNAME + username);
        }else{
            validateLoginAttempt(user.get());
            user.get().setLastLoginDateDisplay(user.get().getLastLoginDate());
            user.get().setLastLoginDate(new Date());
            userRepository.save(user.get());
            UserPrinciple userPrinciple = new UserPrinciple(user.get());
            log.info("Returning user by username: "+username);
            return userPrinciple;
        }
    }

    private void validateLoginAttempt(User user){
        if(user.isNotLocked()){
            if(loginAttemptService.hasExceededMaxAttempt(user.getUsername())){
                user.setNotLocked(false);
            }
            else {
                user.setNotLocked(true);
            }
        }else{
            loginAttemptService.evictUserFromCache(user.getUsername());
        }
    }

    @Override
    public User register(String fName, String lName, String username, String email) throws UserNotFoundException, EmailExistException, UsernameExistException {
        User validUser = validateNewUserNameAndEmail(StringUtils.EMPTY, username, email);
        String password = generatePassword();
        String encodedPassword = encodedPassword(password);
        User buildUser = User.builder()
                .userId(generateUserId())
                .username(username)
                .firstName(fName)
                .lastName(lName)
                .email(email)
                .joinDate(new Date())
                .password(encodedPassword)
                .isActive(true)
                .isNotLocked(true)
                .role(ROLE_USER.name())
                .authorities(ROLE_USER.getAuthorities())
                .profileImageUrl(getTemporaryProfilePicUrl())
                .build();

        log.info("\nNew User Password: "+password);
        return userRepository.save(buildUser);
    }

    private String getTemporaryProfilePicUrl() {
        return ServletUriComponentsBuilder
                .fromCurrentContextPath().path(DEFAULT_USER_IMAGE_PATH).toUriString();
    }

    private String generateUserId() {
        return RandomStringUtils.randomNumeric(10);
    }

    private String generatePassword() {
        return RandomStringUtils.randomAlphanumeric(10);
    }

    private String encodedPassword(String password) {
        return passwordEncoder.encode(password);
    }

    // going to be used when user is trying to create or update account.
    // Hence, being written slightly generic
    private User validateNewUserNameAndEmail(String currentUsername, String newUsername, String newEmail) throws UserNotFoundException, UsernameExistException, EmailExistException {

        Optional<User> userByNewUsername = Optional.ofNullable(findByUsername(newUsername));
        Predicate<User> checkUserById = user -> !user.getId().equals(userByNewUsername.get().getId());

        Optional<User> userByNewEmail = Optional.ofNullable(findByEmail(newEmail));
        Predicate<User> checkEmailById = user -> !user.getId().equals(userByNewEmail.get().getId());

        if(StringUtils.isNotBlank(currentUsername)){
            Optional<User> currentUser = Optional.ofNullable(findByUsername(currentUsername));

            if(currentUser.isEmpty())
                throw new UserNotFoundException(NO_USER_FOUND_BY_USERNAME +currentUsername);

            if(userByNewUsername.isPresent() && checkUserById.test(currentUser.get())){
                throw new UsernameExistException(USERNAME_ALREADY_EXISTS);
            }

            if(userByNewEmail.isPresent() && checkEmailById.test(currentUser.get())){
                throw new EmailExistException(EMAIL_ALREADY_EXISTS);
            }
            return currentUser.get();
        }else{
            if(userByNewUsername.isPresent()){
                throw new UsernameExistException(USERNAME_ALREADY_EXISTS);
            }

            if(userByNewEmail.isPresent()){
                throw new EmailExistException(EMAIL_ALREADY_EXISTS);
            }
            return null;
        }
    }

    @Override
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findUserByUsername(username);
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findUserByEmail(email);
    }
}
