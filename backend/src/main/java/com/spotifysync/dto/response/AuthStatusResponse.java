package com.spotifysync.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthStatusResponse {
    private AccountInfo source;
    private AccountInfo destination;
}
