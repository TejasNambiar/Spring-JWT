package com.supportportal.service;

import com.supportportal.entity.User;
import com.supportportal.entity.UserPrinciple;
import com.supportportal.exception.domain.*;
import com.supportportal.repository.UserRepository;
import com.supportportal.security.enumeration.Role;
import com.supportportal.service.interfaces.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.mail.MessagingException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static com.supportportal.security.enumeration.Role.ROLE_USER;
import static com.supportportal.utility.constants.Constants.*;
import static com.supportportal.utility.constants.FileConstant.*;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.springframework.util.MimeTypeUtils.*;

@Slf4j
@Service
@Transactional
@Qualifier("UserDetailsService")
public class UserService implements IUserService, UserDetailsService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final EmailService emailService;

    @Autowired
    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, LoginAttemptService loginAttemptService, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
        this.emailService = emailService;
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
    public User register(String fName, String lName, String username, String email) throws UserNotFoundException, EmailExistException, UsernameExistException, MessagingException {
        validateNewUsernameAndEmail(EMPTY, username, email);
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
                .profileImageUrl(getTemporaryProfilePicUrl(username))
                .build();

        log.info("\nNew User Password: "+password);
//        emailService.sendNewPasswordEmail(fName,password,email);
        return userRepository.save(buildUser);
    }

    private String getTemporaryProfilePicUrl(String username) {
        return ServletUriComponentsBuilder
                .fromCurrentContextPath().path(DEFAULT_USER_IMAGE_PATH+username).toUriString();
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
    private User validateNewUsernameAndEmail(String currentUsername, String newUsername, String newEmail) throws UserNotFoundException, UsernameExistException, EmailExistException {

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
    public User addNewUser(String fName, String lName, String username, String email, String role, boolean isNonLocked, boolean isActive, MultipartFile profileImage) throws UserNotFoundException, UsernameExistException, EmailExistException, IOException, NotAnImageFileException {
        validateNewUsernameAndEmail(EMPTY, username, email);
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
                .role(getRoleEnumName(role).name())
                .authorities(getRoleEnumName(role).getAuthorities())
                .profileImageUrl(getTemporaryProfilePicUrl(username))
                .build();

        userRepository.save(buildUser);
        saveProfileImage(buildUser, profileImage);
        log.info("New user password: " + password);
        return buildUser;
    }

    private void saveProfileImage(User user, MultipartFile profileImage) throws IOException, NotAnImageFileException {
        if (profileImage != null) {
            if(!Arrays.asList(IMAGE_JPEG_VALUE, IMAGE_PNG_VALUE, IMAGE_GIF_VALUE).contains(profileImage.getContentType())) {
                throw new NotAnImageFileException(profileImage.getOriginalFilename() + NOT_AN_IMAGE_FILE);
            }
            Path userFolder = Paths.get(USER_FOLDER + user.getUsername()).toAbsolutePath().normalize();
            if(!Files.exists(userFolder)) {
                Files.createDirectories(userFolder);
                log.info(DIRECTORY_CREATED + userFolder);
            }
            Files.deleteIfExists(Paths.get(userFolder + user.getUsername() + DOT + JPG_EXTENSION));
            Files.copy(profileImage.getInputStream(), userFolder.resolve(user.getUsername() + DOT + JPG_EXTENSION), REPLACE_EXISTING);
            user.setProfileImageUrl(setProfileImageUrl(user.getUsername()));
            userRepository.save(user);
            log.info(FILE_SAVED_IN_FILE_SYSTEM + profileImage.getOriginalFilename());
        }
    }

    private String setProfileImageUrl(String username) {
        return ServletUriComponentsBuilder
                .fromCurrentContextPath().path(USER_IMAGE_PATH+username).toUriString();
    }

    private Role getRoleEnumName(String role) {
        return Role.valueOf(role.toUpperCase());
    }


    @Override
    public User updateUser(String currentUsername, String newFirstName, String newLastName, String newUsername, String newEmail, String role, boolean isNonLocked, boolean isActive, MultipartFile profileImage) throws UserNotFoundException, UsernameExistException, EmailExistException, IOException, NotAnImageFileException {
        Optional<User> validUser = Optional.ofNullable(validateNewUsernameAndEmail(currentUsername, newUsername, newEmail));
        if(validUser.isPresent()){
            User currentUser = validUser.get();
            currentUser.setFirstName(newFirstName);
            currentUser.setLastName(newLastName);
            currentUser.setUsername(newUsername);
            currentUser.setEmail(newEmail);
            currentUser.setActive(isActive);
            currentUser.setNotLocked(isNonLocked);
            currentUser.setRole(getRoleEnumName(role).name());
            currentUser.setAuthorities(getRoleEnumName(role).getAuthorities());
            userRepository.save(currentUser);
            saveProfileImage(currentUser, profileImage);
            return currentUser;
        }
        throw new UserNotFoundException(NO_USER_FOUND_BY_USERNAME +currentUsername);
    }

    @Override
    public void deleteUser(String username) throws IOException {
        User user = userRepository.findUserByUsername(username);
        Path userFolder = Paths.get(USER_FOLDER + user.getUsername()).toAbsolutePath().normalize();
        FileUtils.deleteDirectory(new File(userFolder.toString()));
        userRepository.deleteById(user.getId());
    }

    @Override
    public void resetPassword(String email) throws MessagingException, EmailNotFoundException {
        User user = userRepository.findUserByEmail(email);
        if (user == null) {
            throw new EmailNotFoundException(NO_USER_FOUND_BY_EMAIL + email);
        }
        String password = generatePassword();
        user.setPassword(encodedPassword(password));
        userRepository.save(user);
        log.info("New user password: " + password);
        //emailService.sendNewPasswordEmail(user.getFirstName(), password, user.getEmail());
    }

    @Override
    public User updateProfileImage(String username, MultipartFile profileImage) throws UserNotFoundException, UsernameExistException, EmailExistException, IOException, NotAnImageFileException {
        User user = validateNewUsernameAndEmail(username, null, null);
        saveProfileImage(user, profileImage);
        return user;
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
