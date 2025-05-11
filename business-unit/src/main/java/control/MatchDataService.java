package control;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import model.MatchEvent;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MatchDataService {

    private List<MatchEvent> matchEvents = new ArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @PostConstruct
    public void init() {
        String jsonData = "datalake/eventstore/Match_Topic/default/20250414.events";
        try {
            List<MatchEventDTO> dtos = objectMapper.readValue(jsonData, new TypeReference<>() {});
            this.matchEvents = dtos.stream()
                    .map(dto -> new MatchEvent(
                            dto.timeStamp,
                            dto.oddsDraw,
                            dto.teamAway,
                            dto.oddsAway,
                            dto.teamHome,
                            dto.source,
                            dto.oddsHome
                    )).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            this.matchEvents = new ArrayList<>();
        }
    }

    public List<MatchEvent> getAllMatchEvents() {
        return new ArrayList<>(matchEvents);
    }

    public Optional<MatchEvent> getMatchEventById(String id) {
        return matchEvents.stream().filter(m -> m.getId().equals(id)).findFirst();
    }

    private static class MatchEventDTO {
        public ZonedDateTime timeStamp;
        public double oddsDraw;
        public String teamAway;
        public double oddsAway;
        public String teamHome;
        public String source;
        public double oddsHome;
    }
}
