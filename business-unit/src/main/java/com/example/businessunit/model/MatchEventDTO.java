package com.example.businessunit.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.ZonedDateTime;

@Getter
@Setter
@NoArgsConstructor // Constructor por defecto requerido por Jackson
public class MatchEventDTO {

    @JsonProperty("timeStamp")
    private ZonedDateTime timeStamp;

    @JsonProperty("dateTime")
    private String dateTime; // Este será el dateTimeString en MatchEvent

    @JsonProperty("oddsDraw")
    private Double oddsDraw;

    @JsonProperty("teamAway")
    private String teamAway;

    @JsonProperty("oddsAway")
    private Double oddsAway;

    @JsonProperty("teamHome")
    private String teamHome;

    @JsonProperty("source")
    private String source;

    @JsonProperty("oddsHome")
    private Double oddsHome;
}