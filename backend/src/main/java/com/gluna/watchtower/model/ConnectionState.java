package com.gluna.watchtower.model;

public enum ConnectionState {
    ESTABLISHED,
    LISTENING,
    TIME_WAIT,
    CLOSE_WAIT,
    SYN_SENT,
    SYN_RECV,
    FIN_WAIT_1,
    FIN_WAIT_2,
    LAST_ACK,
    CLOSED,
    UNKNOWN
}

