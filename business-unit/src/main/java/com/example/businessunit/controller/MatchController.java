package com.example.businessunit.controller;

import com.example.businessunit.model.MatchEvent;
import com.example.businessunit.model.MatchStatus;
import com.example.businessunit.service.MatchDataService;
import com.example.businessunit.service.MatchSseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/laliga")
public class MatchController {

    private final MatchDataService matchDataService;
    private final MatchSseService sseService;

    @Autowired
    public MatchController(MatchDataService matchDataService, MatchSseService sseService) {
        this.matchDataService = matchDataService;
        this.sseService = sseService;
    }

    @GetMapping("/matches")
    public String listAllMatchesOverview(Model model) {
        model.addAttribute("liveMatches", matchDataService.getLiveMatches());
        model.addAttribute("upcomingMatches", matchDataService.getUpcomingMatches());
        model.addAttribute("historicalMatches", matchDataService.getHistoricalMatches());
        return "matches_overview";
    }

    @GetMapping("/matches/stream")
    public SseEmitter streamMatches() {
        return sseService.createEmitter();
    }

    @GetMapping("/match/{matchId}")
    public String viewMatchDetails(@PathVariable("matchId") String idDelPartido, Model model) {
        Optional<MatchEvent> selectedMatchOpt = matchDataService.getMatchEventById(idDelPartido);

        if (selectedMatchOpt.isPresent()) {
            MatchEvent selectedMatch = selectedMatchOpt.get();
            model.addAttribute("selectedMatch", selectedMatch);

            // Lógica para generar URLs de logo para el selectedMatch
            // Esta lógica se ejecutará siempre para el selectedMatch, asegurando
            // que 'homeLogo' y 'awayLogo' se añaden al modelo.
            if (selectedMatch.getTeamHome() != null) {
                String homeLogoFileName = selectedMatch.getTeamHome().replace(" ", "_") + ".png";
                model.addAttribute("homeLogo", "/images/logos/" + homeLogoFileName);
            } else {
                model.addAttribute("homeLogo", null); // O una URL de logo por defecto
            }

            if (selectedMatch.getTeamAway() != null) {
                String awayLogoFileName = selectedMatch.getTeamAway().replace(" ", "_") + ".png";
                model.addAttribute("awayLogo", "/images/logos/" + awayLogoFileName);
            } else {
                model.addAttribute("awayLogo", null); // O una URL de logo por defecto
            }

            // Lógica para "Otros Partidos" (esto ya debería funcionar si los datos son correctos)
            List<MatchEvent> allPotentiallyRelevantMatches = matchDataService.getAllMatchEvents();
            List<MatchEvent> otherMatchesList = allPotentiallyRelevantMatches.stream()
                    .filter(match -> !match.getId().equals(idDelPartido))
                    .filter(match -> {
                        MatchStatus status = match.getMatchStatus();
                        return status == MatchStatus.LIVE ||
                                status == MatchStatus.UPCOMING ||
                                status == MatchStatus.SCHEDULED;
                    })
                    .collect(Collectors.toList());
            model.addAttribute("otherMatchesList", otherMatchesList);

        } else {
            model.addAttribute("selectedMatch", null);
            model.addAttribute("otherMatchesList", List.of());
            model.addAttribute("errorMessage", "Partido no encontrado con ID: " + idDelPartido);
        }
        return "match_details";
    }
}