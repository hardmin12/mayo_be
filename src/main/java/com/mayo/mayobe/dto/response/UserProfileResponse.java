package com.mayo.mayobe.dto.response;

public class UserProfileResponse {

    private String email;
    private String nickname;
    private String provider;

    public UserProfileResponse(String email, String nickname, String provider) {
        this.email = email;
        this.nickname = nickname;
        this.provider = provider;
    }
}
