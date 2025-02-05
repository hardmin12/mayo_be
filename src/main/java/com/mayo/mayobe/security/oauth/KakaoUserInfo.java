package com.mayo.mayobe.security.oauth;

import com.sun.jdi.ObjectReference;

import java.util.Map;

public class KakaoUserInfo extends OAuthUserInfo{

    public KakaoUserInfo(Map<String, Object> attributes) {
        super(
                String.valueOf(attributes.get("id")),
                (String) ((Map<String, Object>) attributes.get("kakao_account")).get("email"),
                (String) ((Map<String, Object>) attributes.get("properties")).get("nickname"),
                "Kakao"

        );

    }
}
