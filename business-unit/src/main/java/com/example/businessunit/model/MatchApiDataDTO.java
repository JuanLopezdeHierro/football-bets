package com.example.businessunit.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class MatchApiDataDTO {

    @JsonProperty("round")
    private String round;

    @JsonProperty("stadium")
    private String stadium;

    @JsonProperty("homeTeam")
    private String homeTeam;

    @JsonProperty("awayTeam")
    private String awayTeam;

    @JsonProperty("referee")
    private String referee;

    @JsonProperty("dateTime")
    private String dateTime;
}