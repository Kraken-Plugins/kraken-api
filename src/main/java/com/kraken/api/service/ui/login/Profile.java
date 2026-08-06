package com.kraken.api.service.ui.login;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = {"password", "sessionId", "characterId", "bankPin"})
public class Profile {

    @Builder.Default
    private String identifier = "";

    @Builder.Default
    private boolean isJagexAccount = true;

    @Builder.Default
    private String username = "";

    @Builder.Default
    private String password = "";

    @Builder.Default
    private String characterName = "";

    @Builder.Default
    private String sessionId = "";

    @Builder.Default
    private String characterId = "";

    @Builder.Default
    private String bankPin = "";
}
