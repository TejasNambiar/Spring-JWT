package com.supportportal.security.enumeration;

import java.util.List;

import static com.supportportal.utility.constants.Authority.*;

public enum Role {
    ROLE_USER(USER_AUTHORITIES),
    ROLE_HR(HR_AUTHORITIES),
    ROLE_MANAGER(MANAGER_AUTHORITIES),
    ROLE_ADMIN(ADMIN_AUTHORITIES),
    ROLE_SUPER_ADMIN(SUPER_ADMIN_AUTHORITIES);

    private final String[] authorities;

    Role(String... authorities){
        this.authorities = authorities;
    }

    public List<String> getAuthorities(){
        return List.of(authorities);
    }
}
