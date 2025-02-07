package com.mayo.mayobe.controller;

import com.mayo.mayobe.dto.request.SocialLoginRequest;
import com.mayo.mayobe.dto.response.TokenResponse;
import com.mayo.mayobe.dto.response.UserProfileResponse;
import com.mayo.mayobe.entity.SocialUserEntity;
import com.mayo.mayobe.repository.UserRepository;
import com.mayo.mayobe.service.JwtService;
import com.mayo.mayobe.service.OAuthService;;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final OAuthService oAuthService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthController(OAuthService oAuthService, JwtService jwtService, UserRepository userRepository) {
        this.oAuthService = oAuthService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    //소셜 로그인 API
    @PostMapping("/social-login")
    public ResponseEntity<TokenResponse> socialLogin(@RequestBody SocialLoginRequest request) {
        System.out.println("소셜 로그인 요청 - provider: " + request.getProvider() + ", idToken: " + request.getIdToken()); // ✅ 로그 출력
        TokenResponse response = oAuthService.socialLogin(request);
        return ResponseEntity.ok(response);
    }

    //로그인한 사용자 정보 조회 API
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getUserProfile(@RequestHeader("Authorization") String token) {
        String jwt = token.substring(7); //"Bearer" 제거
        Long userId = jwtService.extractUserId(jwt);
        SocialUserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        UserProfileResponse response = new UserProfileResponse(user.getEmail(), user.getNickname(), user.getProvider());
        return ResponseEntity.ok(response);
    }


    //카카오 OAuth 로그인 콜백 API
    @GetMapping("/kakao/callback")
    public ResponseEntity<String> kakaoCallback(@RequestParam("code") String code) {
        String accessToken = oAuthService.getKakaoAccessToken(code);
        return ResponseEntity.ok("카카오 액세스 토큰:" + accessToken);
    }
}
