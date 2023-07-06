package com.foodclicker.game_service.game.social.player_profile.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.text.SimpleDateFormat;
import java.util.Date;

@Getter
@Setter
public class PlayerProfileModel {
    private String registrationDate;
    private Long userId;
    public PlayerProfileModel(Long userId, Date registrationDate) {
        this.userId = userId;
        this.registrationDate = new SimpleDateFormat("yyyy.MM.dd").format(registrationDate);
    }
}
