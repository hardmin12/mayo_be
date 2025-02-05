package com.mayo.mayobe.security.jwt;

import com.mayo.mayobe.service.JwtService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

            //요청에서 Authorization 헤더를 가져오기
            String token = request.getHeader("Authorization");

            //JWT 검증 및 사용자 정보 추출
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                if (jwtService.validateToken(token)) {
                    Long userId = jwtService.extractUserId(token);
                    //TODO: SecurityContext에 사용자 정보 설정 가능(추후 추가)
                }
            }

            //다음 필터로 요청을 넘김
            filterChain.doFilter(request, response);


        }

}
