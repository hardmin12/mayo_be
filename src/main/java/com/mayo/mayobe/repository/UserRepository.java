package com.mayo.mayobe.repository;

import com.mayo.mayobe.entity.SocialUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<SocialUserEntity, Long> {

    //이메일이 있을 경우, 이메일로 사용자 조회
    Optional<SocialUserEntity> findByEmail(String email);

    //소셜 로그인 providerId로 사용자 조회(이메일 없을 수 있음)
    Optional<SocialUserEntity> findByProviderId(String providerId);

}
