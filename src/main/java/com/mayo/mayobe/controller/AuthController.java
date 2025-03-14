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

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
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
    @PostMapping("/auth/kakao")
    public ResponseEntity<Map<String, String>> kakaoLogin(@RequestHeader("Authorization") String authorizationHeader) {

        System.out.println("Authorization 헤더 값: " + authorizationHeader);

        // Authorization 헤더에서 "Bearer " 부분을 제외한 토큰만 추출
        String accessToken = authorizationHeader != null && authorizationHeader.startsWith("Bearer ")
                ? authorizationHeader.substring(7)
                : null;
        System.out.println(" 프론트에서 받은 액세스 토큰:" + accessToken);

        if (accessToken == null || accessToken.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "액세스 토큰이 제공되지않음"));

        }
//        //응답을 JSON으로 반환
//        Map<String, String> response = Map.of(
//                "message", "카카오 로그인 성공",
//                "status", "success",
//                "receivedAccessToken", accessToken
//        );
        //액세스 토큰으로 카카오 사용자 정보 가져오기
        Map<String, Object> kakaoUserInfo = oAuthService.getKakaoUserInfo(accessToken);
        String providerId = kakaoUserInfo.get("id").toString();
        String email = kakaoUserInfo.containsKey("email") ? kakaoUserInfo.get("email").toString() : "no-email";

        //사용자 정보가 db에 있는지 확인
        Optional<SocialUserEntity> existingUser = userRepository.findByProviderId(providerId);

        SocialUserEntity user;
        if(existingUser.isPresent()) {
            user = existingUser.get();
            System.out.println("기존 사용자 로그인:" + user);
        } else {
           // 새 유저 등록
             user = new SocialUserEntity(providerId, email, "카카오 유저", "KAKAO");
             user = userRepository.save(user);
            System.out.println("새 사용자 저장 완료:" + user);
        }

        //JWT 발급
        String accessJwtToken = jwtService.generateAccessToken(user);
        String refreshJwtToken = jwtService.generateRefreshToken(user);

        System.out.println("발급된 AccessToken:" + accessJwtToken);
        System.out.println("발급된 RefreshToken" + refreshJwtToken);

        // JWT와 Refresh Tokne을 응답으로 변환
        Map<String, String> response = Map.of(
                "accessToken", accessJwtToken,
                "refreshToken", refreshJwtToken
        );

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

//
//    //카카오 OAuth 로그인 콜백 API
//    @GetMapping("/kakao/callback")
//    public ResponseEntity<String> kakaoCallback(@RequestParam("code") String code) {
//        String accessToken = oAuthService.getKakaoAccessToken(code);
//        return ResponseEntity.ok("카카오 액세스 토큰:" + accessToken);
//    }
//


   @GetMapping("/kakao/callback")
    public ResponseEntity<Map<String, String>> kakaoCallback(@RequestParam("code") String code) {
        String accessToken = oAuthService.getKakaoAccessToken(code); // 프론트에서 받은 액세스 토큰


       if (accessToken == null || accessToken.isEmpty()) {
           return ResponseEntity.badRequest().body(Map.of("error", "액세스 토큰을 받을 수 없습니다"));
       }

       return ResponseEntity.ok(Map.of("accessToken", accessToken));
    }
}
