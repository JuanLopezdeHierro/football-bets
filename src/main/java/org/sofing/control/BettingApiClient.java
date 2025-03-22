package org.sofing.control;

import org.sofing.model.Odd;

import java.util.List;

public interface BettingApiClient {
    List<Odd> getBets();
}
