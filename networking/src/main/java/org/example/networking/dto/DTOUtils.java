package org.example.networking.dto;

import org.example.model.*;

import java.util.Arrays;

public class DTOUtils {
    // Referee conversions
    public static RefereeDTO getDTO(Referee referee) {
        return new RefereeDTO(
                referee.getId(),
                referee.getName(),
                referee.getUsername(),
                referee.getPassword(),
                referee.getEvent().getId(),
                referee.getEvent().getName()
        );
    }

    public static Referee getFromDTO(RefereeDTO refereeDto) {
        return new Referee(
                refereeDto.getId(),
                refereeDto.getName(),
                new Event(refereeDto.getEventId(), refereeDto.getEventName()),
                refereeDto.getUsername(),
                refereeDto.getPassword()
        );
    }

    // Participant conversions
    public static ParticipantDTO getDTO(Participant participant) {
        return new ParticipantDTO(
                participant.getId(),
                participant.getName()
        );
    }

    public static Participant getFromDTO(ParticipantDTO participantDto) {
        return new Participant(
                participantDto.getId(),
                participantDto.getName()
        );
    }

    // Event conversions
    public static EventDTO getDTO(Event event) {
        return new EventDTO(
                event.getId(),
                event.getName()
        );
    }

    public static Event getFromDTO(EventDTO eventDto) {
        return new Event(
                eventDto.getId(),
                eventDto.getName()
        );
    }

    // Result conversions
    public static ResultDTO getDTO(Result result) {
        return new ResultDTO(
                result.getId(),
                result.getEvent().getId(),
                result.getEvent().getName(),
                result.getParticipant().getId(),
                result.getParticipant().getName(),
                result.getPoints()
        );
    }

    public static Result getFromDTO(ResultDTO resultDto) {
        Event event = new Event(resultDto.getEventId(), resultDto.getEventName());
        Participant participant = new Participant(resultDto.getParticipantId(), resultDto.getParticipantName());
        return new Result(
                resultDto.getId(),
                event,
                participant,
                resultDto.getPoints()
        );
    }

    // ParticipantResult conversions
    public static ParticipantResultDTO getDTO(ParticipantResult participantResult) {
        return new ParticipantResultDTO(
                participantResult.getId(),
                participantResult.getParticipantID(),
                participantResult.getParticipantName(),
                participantResult.getPoints()
        );
    }

    public static ParticipantResult getFromDTO(ParticipantResultDTO participantResultDto) {
        ParticipantResult result = new ParticipantResult(
                participantResultDto.getId(),
                participantResultDto.getParticipantName(),
                participantResultDto.getPoints()
        );
        result.setParticipantID(participantResultDto.getParticipantId());
        return result;
    }

    // Array conversions
    public static ParticipantDTO[] getDTO(Participant[] participants) {
        return Arrays.stream(participants)
                .map(DTOUtils::getDTO)
                .toArray(ParticipantDTO[]::new);
    }

    public static Participant[] getFromDTO(ParticipantDTO[] participantDtos) {
        return Arrays.stream(participantDtos)
                .map(DTOUtils::getFromDTO)
                .toArray(Participant[]::new);
    }

    public static ParticipantResultDTO[] getDTO(ParticipantResult[] participantResults) {
        return Arrays.stream(participantResults)
                .map(DTOUtils::getDTO)
                .toArray(ParticipantResultDTO[]::new);
    }
}