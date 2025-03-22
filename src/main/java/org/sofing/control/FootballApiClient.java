package org.sofing.control;

import org.sofing.model.Match;

import java.util.List;

public interface FootballApiClient {
    List<Match> getMatches();
}
