package dev.sh3lby.minegate.monitoring;

import java.util.UUID;

public class MonitoringEvent {
    private final Type type;
    private final UUID playerId;
    private final String playerName;
    private final String message;

    private MonitoringEvent(Type type, UUID playerId, String playerName, String message) {
        this.type = type;
        this.playerId = playerId;
        this.playerName = playerName;
        this.message = message;
    }

    public static MonitoringEvent playerJoin(String playerName) {
        return new MonitoringEvent(Type.PLAYER_JOIN, null, playerName, null);
    }

    public static MonitoringEvent playerQuit(String playerName) {
        return new MonitoringEvent(Type.PLAYER_QUIT, null, playerName, null);
    }

    public static MonitoringEvent playerDeath(String message) {
        return new MonitoringEvent(Type.PLAYER_DEATH, null, null, message);
    }

    public static MonitoringEvent playerChat(UUID playerId, String playerName, String message) {
        return new MonitoringEvent(Type.PLAYER_CHAT, playerId, playerName, message);
    }

    public static MonitoringEvent serverStart() {
        return new MonitoringEvent(Type.SERVER_START, null, null, null);
    }

    public static MonitoringEvent serverStop() {
        return new MonitoringEvent(Type.SERVER_STOP, null, null, null);
    }

    public Type getType() {
        return type;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getMessage() {
        return message;
    }

    public enum Type {
        PLAYER_JOIN,
        PLAYER_QUIT,
        PLAYER_DEATH,
        PLAYER_CHAT,
        SERVER_START,
        SERVER_STOP
    }
}
