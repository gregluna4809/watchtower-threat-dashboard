package com.gluna.watchtower.enrichment;

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class IpUtils {

    private IpUtils() {
    }

    public static boolean isPublicAddress(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(ip);
            byte[] bytes = address.getAddress();
            return !address.isAnyLocalAddress()
                    && !address.isLoopbackAddress()
                    && !address.isLinkLocalAddress()
                    && !address.isSiteLocalAddress()
                    && !address.isMulticastAddress()
                    && !isCarrierGradeNat(bytes);
        } catch (UnknownHostException ex) {
            return false;
        }
    }

    private static boolean isCarrierGradeNat(byte[] bytes) {
        if (bytes.length != 4) {
            return false;
        }
        int first = bytes[0] & 0xff;
        int second = bytes[1] & 0xff;
        return first == 100 && second >= 64 && second <= 127;
    }
}
