package com.mayo.mayobe.dto.request;

public class SocialLoginRequest {

    private String provider;
    private String idToken;

    public SocialLoginRequest(String provider, String idToken) {
        this.provider = provider;
        this.idToken = idToken;
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
}
