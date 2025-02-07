package com.mayo.mayobe.dto.request;

public class SocialLoginRequest {

    private String provider;
    private String idToken;

    private String accessToken;

    public SocialLoginRequest(String provider, String idToken, String accessToken) {
        this.provider = provider;
        this.idToken = idToken;
        this.accessToken = accessToken;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
