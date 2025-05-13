package com.example.businessunit.controller;


import com.example.businessunit.model.MatchEvent;
import com.example.businessunit.service.MatchDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/laliga") // Ruta base para este controlador, como en tus ejemplos HTML
public class MatchController {

    private final MatchDataService matchDataService;
    private final DateTimeFormatter timeFormatter; // Inyectar el formateador

    @Autowired
    public MatchController(MatchDataService matchDataService, DateTimeFormatter timeFormatter) {
        this.matchDataService = matchDataService;
        this.timeFormatter = timeFormatter;
    }

    @GetMapping("/matches")
    public String listAllMatches(Model model) {
        List<MatchEvent> matches = matchDataService.getAllMatchEvents();
        model.addAttribute("matches", matches);
        // El nombre del template debe coincidir con el archivo en src/main/resources/templates/
        return "matches_overview"; // Vista para la lista de partidos
    }

    @GetMapping("/match/{matchId}") // El nombre aquí es "matchId"
    public String viewMatchDetails(@PathVariable("matchId") String idDelPartido, Model model) { // Especifica el nombre aquí
        List<MatchEvent> allMatches = matchDataService.getAllMatchEvents();
        // Usa 'idDelPartido' para buscar el partido
        Optional<MatchEvent> selectedMatchOpt = matchDataService.getMatchEventById(idDelPartido);

        model.addAttribute("allMatches", allMatches);
        model.addAttribute("timeFormatter", timeFormatter);

        if (selectedMatchOpt.isPresent()) {
            MatchEvent selectedMatch = selectedMatchOpt.get();
            model.addAttribute("selectedMatch", selectedMatch);

            String homeLogoFileName = selectedMatch.getTeamHome()
                    .toLowerCase()
                    .replaceAll("\\s+", "")
                    // .replaceAll("[^a-z0-9]", "") // Opcional, si lo necesitas
                    + ".png";
            String awayLogoFileName = selectedMatch.getTeamAway()
                    .toLowerCase()
                    .replaceAll("\\s+", "")
                    // .replaceAll("[^a-z0-9]", "") // Opcional
                    + ".png";

            model.addAttribute("homeLogo", "/images/logos/" + homeLogoFileName);
            model.addAttribute("awayLogo", "/images/logos/" + awayLogoFileName);
        } else {
            model.addAttribute("selectedMatch", null);
            model.addAttribute("errorMessage", "Partido no encontrado con ID: " + idDelPartido); // Mensaje más específico
        }
        return "match_details";
    }
}
