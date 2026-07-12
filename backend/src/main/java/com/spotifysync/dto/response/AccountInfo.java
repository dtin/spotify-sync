package com.spotifysync.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountInfo {
    private String displayName;
    private String email;
    private String profileImageUrl;
    private boolean connected;
}
