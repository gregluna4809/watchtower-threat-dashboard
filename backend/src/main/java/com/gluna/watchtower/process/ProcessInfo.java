package com.gluna.watchtower.process;

public record ProcessInfo(
        int pid,
        String name,
        String path,
        Boolean signed,
        String signer
) {
}

