package com.mayo.mayobe.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;

import com.mayo.mayobe.dto.request.SocialLoginRequest;
import com.mayo.mayobe.dto.response.TokenResponse;
import com.mayo.mayobe.entity.RefreshTokenEntity;
import com.mayo.mayobe.entity.SocialUserEntity;
import com.mayo.mayobe.repository.RefreshTokenRepository;
import com.mayo.mayobe.repository.UserRepository;
import com.mayo.mayobe.security.oauth.GoogleUserInfo;
import com.mayo.mayobe.security.oauth.KakaoUserInfo;
import com.mayo.mayobe.security.oauth.OAuthUserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.google.api.client.json.jackson2.JacksonFactory;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Service
public class OAuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Value("${oauth.google.client-id}") // 환경 변수에서 Google Client ID 가져오기
    private String googleClientId;

    public OAuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
    }

    public TokenResponse socialLogin(SocialLoginRequest request) {
        OAuthUserInfo userInfo = getUserInfo(request.getProvider(), request.getIdToken());

        // 기존 사용자 확인
        Optional<SocialUserEntity> existingUser = userRepository.findByProviderId(userInfo.getProviderId());
        SocialUserEntity user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
        } else {
            // 새로운 사용자 저장
            user = new SocialUserEntity(
                    userInfo.getProviderId(),
                    userInfo.getEmail(),
                    userInfo.getNickname(),
                    userInfo.getProvider()
            );
            userRepository.save(user);
        }

        // Refresh Token 삭제 (기존 것 제거 후 새로 저장)
        refreshTokenRepository.deleteByUser(user);

        // JWT Access & Refresh Token 발급
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Refresh Token 저장
        RefreshTokenEntity refreshTokenEntity = new RefreshTokenEntity(user, refreshToken, LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(refreshTokenEntity);

        return new TokenResponse(accessToken, refreshToken);
    }

    /**
     * OAuth 제공자로부터 사용자 정보를 가져오는 메서드
     */
    private OAuthUserInfo getUserInfo(String provider, String idToken) {
        if ("Google".equalsIgnoreCase(provider)) {
            Map<String, Object> googleAttributes = verifyGoogleIdToken(idToken);
            return new GoogleUserInfo(googleAttributes);
        } else if ("Kakao".equalsIgnoreCase(provider)) {
            Map<String, Object> kakaoAttributes = verifyKakaoIdToken(idToken);
            return new KakaoUserInfo(kakaoAttributes);
        } else {
            throw new IllegalArgumentException("지원하지 않는 OAuth 제공자입니다.");
        }
    }

    /**
     * Google ID 토큰 검증 및 사용자 정보 추출
     */
    private Map<String, Object> verifyGoogleIdToken(String idToken) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId)) // 환경 변수에서 불러오기
                    .build();

            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                throw new IllegalArgumentException("유효하지 않은 Google ID 토큰입니다.");
            }

            GoogleIdToken.Payload payload = googleIdToken.getPayload();

            return Map.of(
                    "sub", payload.getSubject(),
                    "email", payload.getEmail(),
                    "name", (String) payload.get("name"),
                    "picture", (String) payload.get("picture")
            );

        } catch (Exception e) {
            throw new IllegalArgumentException("Google ID 토큰 검증 중 오류 발생", e);
        }
    }

    /**
     * ✅ Kakao ID 토큰 검증 및 사용자 정보 추출
     */
    private Map<String, Object> verifyKakaoIdToken(String accessToken) {
        String url = "https://kapi.kakao.com/v2/user/me";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Content-Type", "application/x-www-form-urlencoded");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

        return response.getBody();
    }
}