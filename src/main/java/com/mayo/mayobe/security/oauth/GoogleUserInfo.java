package com.mayo.mayobe.security.oauth;

import java.util.Map;

public class GoogleUserInfo extends OAuthUserInfo {

    public GoogleUserInfo(Map<String, Object> attributes) {

      super(
              (String) attributes.get("sub"),
              (String) attributes.get("email"),
              (String) attributes.get("name"),
              "Google"
      );

    }
}
