package com.supportportal.utility;

public class SecurityConstant {

    private SecurityConstant() {
    }

    public static final long EXPIRATION_TIME = 432_000_000; // 5DAYS -> MILLISECONDS (_0) -> FOR READABILITY
    public static final String TOKEN_PREFIX = "Bearer "; // if received, no further checks required
    public static final String JWT_TOKEN_HEADER = "Jwt-Token";
    public static final String TOKEN_CANNOT_BE_VERIFIED = "Token could not be Verified";
    public static final String AUTHORITIES = "authorities";
    public static final String GET_LISTS_LLC = "Get Lists, LLC";
    public static final String GET_LISTS_ADMINISTRATION = "User Management Portal";
    public static final String FORBIDDEN_MESSAGE = "Please login to access this page";
    public static final String ACCESS_DENIED_MESSAGE = "You are not authorized to access this page";
    public static final String OPTIONS_HTTP_METHOD = "OPTIONS";
    public static final String[] PUBLIC_URLS = {"/user/login", "user/register", "user/resetPassword/**", "user/image/**"};
//    public static final String[] PUBLIC_URLS = {"**"};
}
