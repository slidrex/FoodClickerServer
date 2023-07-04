package com.foodclicker.game_service.food_id.identity_service.repository;

import com.foodclicker.game_service.food_id.identity_service.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
}

