package com.mayo.mayobe.dto.request;

public class NicknameRequest {

    private String email;
    private String nickname;

    private String providerId;

    public NicknameRequest(String email, String nickname, String providerId) {
        this.email = email;
        this.nickname = nickname;
        this.providerId = providerId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
}
