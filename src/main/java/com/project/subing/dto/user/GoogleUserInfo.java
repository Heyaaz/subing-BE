package com.project.subing.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GoogleUserInfo {

    private String sub;
    private String email;
    private String name;
    private String picture;

    @JsonProperty("email_verified")
    private Boolean emailVerified;
}
