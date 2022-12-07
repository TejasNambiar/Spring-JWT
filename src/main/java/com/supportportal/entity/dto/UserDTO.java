package com.supportportal.entity.dto;

import lombok.*;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Builder
@Component
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private String firstName;
    private String lastName;
    private String username;
    private String email;
}
