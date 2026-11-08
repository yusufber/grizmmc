package com.grizmtech.perms.model;

public class RewardCommand {
    private String server = "first";
    private String command = "";

    public RewardCommand() {
    }

    public RewardCommand(String server, String command) {
        this.server = server != null ? server : "first";
        this.command = command != null ? command : "";
    }

    public String getServer() {
        return server == null || server.isBlank() ? "first" : server.trim().toLowerCase();
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getCommand() {
        return command == null ? "" : command.trim();
    }

    public void setCommand(String command) {
        this.command = command;
    }
}

