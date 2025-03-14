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
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import com.google.api.client.json.jackson2.JacksonFactory;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import jakarta.persistence.EntityManager;

@Service
public class OAuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Value("${oauth.google.client-id}") // 환경 변수에서 Google Client ID 가져오기
    private String googleClientId;

    @Value("${oauth.kakao.rest-api-key}")
    private String kakaoClientId;

    @Value("${oauth.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    public OAuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
    }

    @PersistenceContext //
    private EntityManager entityManager;


    @Transactional
    public TokenResponse socialLogin(SocialLoginRequest request) {
        SocialUserEntity user;

        System.out.println("🔹 [소셜 로그인 요청] Provider: " + request.getProvider());

        if ("kakao".equalsIgnoreCase(request.getProvider())) {
            Map<String, Object> kakaoUserInfo = getKakaoUserInfo(request.getAccessToken());
            String providerId = kakaoUserInfo.get("id").toString();
            String email = kakaoUserInfo.containsKey("email") ? kakaoUserInfo.get("email").toString() : "no-email";

            System.out.println("📌 가져온 이메일: " + email);
            System.out.println("📌 Provider ID: " + providerId);

            Optional<SocialUserEntity> existingUser = userRepository.findByProviderId(providerId);

            if (existingUser.isPresent()) {
                user = existingUser.get();
                System.out.println("✅ 기존 사용자 로그인: " + user);
            } else {
                System.out.println("✅ 새로운 사용자 저장 시도...");
                user = new SocialUserEntity(providerId, email, "카카오 유저", "KAKAO");
                System.out.println("🔹 저장할 사용자 정보: " + user);

                // **사용자 저장**
                user = userRepository.save(user);
                userRepository.flush(); // 🚀 강제 DB 반영
                System.out.println("✅ 저장 완료된 사용자 정보: " + user);
            }
        } else {
            // ✅ 기존 Google 로그인 로직 유지
            OAuthUserInfo userInfo = getUserInfo(request.getProvider(), request.getAccessToken());
            Optional<SocialUserEntity> existingUser = userRepository.findByProviderId(userInfo.getProviderId());

            if (existingUser.isPresent()) {
                user = existingUser.get();
                System.out.println("✅ 기존 사용자 로그인: " + user);
            } else {
                System.out.println("✅ 새로운 사용자 저장 시도...");
                user = new SocialUserEntity(
                        userInfo.getProviderId(),
                        userInfo.getEmail(),
                        userInfo.getNickname(),
                        userInfo.getProvider()
                );
                user = userRepository.save(user);
                userRepository.flush(); // 🚀 강제 DB 반영
                System.out.println("✅ 저장 완료된 사용자 정보: " + user);
            }
        }

        // ✅ 기존 Refresh Token 삭제 후 새로 저장
        refreshTokenRepository.deleteByUser(user);

        // ✅ JWT Access & Refresh Token 발급
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        System.out.println("🔹 [토큰 발급] AccessToken: " + accessToken);
        System.out.println("🔹 [토큰 발급] RefreshToken: " + refreshToken);

        // ✅ Refresh Token 저장 전에 user_id 유효성 체크
        if (!userRepository.existsById(user.getId())) {
            throw new IllegalStateException("❌ Refresh Token 저장 불가! 유효하지 않은 user_id: " + user.getId());
        }
        System.out.println("🔹 Refresh Token 저장 시도! user_id=" + user.getId());

        // ✅ Refresh Token 저장
        RefreshTokenEntity refreshTokenEntity = new RefreshTokenEntity(user, refreshToken, LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(refreshTokenEntity);

        System.out.println("✅ Refresh Token 저장 완료!");

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
     *  Kakao ID 토큰 검증 및 사용자 정보 추출
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

    public String getKakaoAccessToken(String code) {
        System.out.println("카카오 로그인 콜백 - 인증 코드: " + code);

        String tokenUrl = "https://kauth.kakao.com/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        System.out.println("카카오 REST API 키: " + kakaoClientId);
        System.out.println("카카오 Redirect URI: " + kakaoRedirectUri);
        System.out.println("전달된 code 값: " + code);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoClientId);
        params.add("redirect_uri", kakaoRedirectUri);
        params.add("code", code);

        System.out.println("카카오 토큰 요청 파라미터: " + params);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        RestTemplate restTemplate = new RestTemplate();

        // 카카오 API 응답 받기
        ResponseEntity<Map> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, request, Map.class);

        if (response.getBody() == null) {
            throw new IllegalArgumentException("카카오 API 응답이 비어 있습니다.");
        }

        // 카카오 응답 데이터 확인
        System.out.println("카카오 응답 데이터: " + response.getBody());

        // 응답에서 access_token을 가져오기
        String accessToken = (String) response.getBody().get("access_token");

        if (accessToken == null) {
            throw new IllegalArgumentException("카카오에서 액세스 토큰을 받아올 수 없습니다.");
        }

        return accessToken;
    }

//    public Map<String, Object> getKakaoUserInfo(String accessToken) {
//        String userInfoUrl = "https://kapi.kakao.com/v2/user/me";
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.set("Authorization", "Bearer " + accessToken);
//        headers.set("Content-Type", "application/x-www-form-urlencoded");
//
//        HttpEntity<String> request = new HttpEntity<>(headers);
//        RestTemplate restTemplate = new RestTemplate();
//
//        ResponseEntity<Map> response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, request, Map.class);
//
//        return response.getBody();
//    }


    public Map<String, Object> getKakaoUserInfo(String accessToken) {
        String userInfoUrl = "https://kapi.kakao.com/v2/user/me";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Content-Type", "application/x-www-form-urlencoded");

        HttpEntity<String> request = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<Map> response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, request, Map.class);
        Map<String, Object> responseBody = response.getBody();

        // ✅ 응답 데이터 전체 확인 로그 추가
        System.out.println("🔹 카카오 응답 데이터: " + responseBody);

        if (responseBody == null || !responseBody.containsKey("id")) {
            throw new IllegalArgumentException("❌ 카카오 사용자 정보가 올바르지 않습니다.");
        }

        // ✅ `kakao_account` 존재 여부 확인 (널 체크 및 타입 변환 오류 방지)
        Object kakaoAccountObj = responseBody.get("kakao_account");
        if (kakaoAccountObj == null || !(kakaoAccountObj instanceof Map)) {
            System.out.println("⚠️ 'kakao_account' 필드가 없음. 사용자 정보 확인 필요!");
            return Map.of(
                    "id", responseBody.get("id"),
                    "email", "no-email"
            );
        }

        // ✅ `kakao_account`를 `Map<String, Object>`로 변환 (형 변환 예외 방지)
        Map<String, Object> kakaoAccount = (Map<String, Object>) kakaoAccountObj;

        // ✅ 이메일 정보 존재 여부 확인
        String email = kakaoAccount.getOrDefault("email", "no-email").toString();

        return Map.of(
                "id", responseBody.get("id"),
                "email", email
        );
    }
}