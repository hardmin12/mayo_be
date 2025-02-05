package com.mayo.mayobe.repository;

import com.mayo.mayobe.entity.RefreshTokenEntity;
import com.mayo.mayobe.entity.SocialUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    //특정 사용자의 리프레시 토큰 조회
    Optional<RefreshTokenEntity> findByUser(SocialUserEntity user);

    //리프레시 토큰 삭제
    void deleteByUser(SocialUserEntity user);


}
