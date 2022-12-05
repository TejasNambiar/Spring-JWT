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

@Slf4j
@Service
@Transactional
@Qualifier("UserDetailsService")
public class UserService implements IUserService, UserDetailsService {

    private final UserRepository userRepository;
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

    @Override
    public User register(String fName, String lName, String username, String email) throws UserNotFoundException, EmailExistException, UsernameExistException {
        User validUser = validateNewUserNameAndEmail(StringUtils.EMPTY, username, email);
        String password = generatePassword();
        String encodedPassword = encodedPassword(password);
        User buildUser = User.builder()
                .userId(generateUserId())
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
        userRepository.save(buildUser);
        log.info("\nNew User Password: "+password);
        return null;
    }

    private String getTemporaryProfilePicUrl() {
        return ServletUriComponentsBuilder
                .fromCurrentContextPath().path("/user/image/profile/temp").toUriString();
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
        if(StringUtils.isNotBlank(currentUsername)){
            Optional<User> currentUser = Optional.ofNullable(findByUsername(currentUsername));

            if(currentUser.isEmpty())
                throw new UserNotFoundException("No User found by Username: "+currentUsername);

            Optional<User> userByUsername = Optional.ofNullable(findByUsername(newUsername));
            Predicate<User> checkUserById = user -> !user.getId().equals(userByUsername.get().getId());

            if(userByUsername.isPresent() && checkUserById.test(currentUser.get())){
                throw new UsernameExistException("Username already exists");
            }

            Optional<User> userByEmail = Optional.ofNullable(findByEmail(newEmail));
            Predicate<User> checkEmailById = user -> !user.getId().equals(userByEmail.get().getId());

            if(userByEmail.isPresent() && checkEmailById.test(currentUser.get())){
                throw new EmailExistException("Email already exists");
            }
            return currentUser.get();
        }else{
            Optional<User> userByUsername = Optional.ofNullable(findByUsername(newUsername));
            Optional<User> userByEmail = Optional.ofNullable(findByEmail(newEmail));

            if(userByUsername.isPresent()){
                throw new UsernameExistException("Username already exists");
            }

            if(userByEmail.isPresent()){
                throw new EmailExistException("Email already exists");
            }
            return null;
        }
    }

    @Override
    public List<User> getUsers() {
        return null;
    }

    @Override
    public User findByUsername(String username) {
        return null;
    }

    @Override
    public User findByEmail(String email) {
        return null;
    }
}
