package com.mayo.mayobe.security.oauth;

public abstract class OAuthUserInfo {

    protected String providerId;
    protected String email;
    protected String nickname;
    protected String provider;

    public OAuthUserInfo(String providerId, String email, String nickname, String provider) {
        this.providerId = providerId;
        this.email = email;
        this.nickname = nickname;
        this.provider = provider;
    }

    public String getProviderId() {return providerId; }
    public String getEmail() {return email; }
    public String getNickname() {return nickname; }
    public String getProvider() {return provider; }

}
