package com.schemaplexai.agent.engine.tool.adapter.http;

import java.net.InetAddress;

/**
 * SSRF (Server-Side Request Forgery) protection utilities.
 *
 * <p>Provides IP address validation to prevent outbound requests to
 * private/internal networks from tool adapters.</p>
 */
public final class SsrfProtectionUtil {

    private SsrfProtectionUtil() {
        // utility class
    }

    /**
     * Check if an IP address is in a private/internal range.
     *
     * <p>Blocks: 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16, 127.0.0.0/8,
     * 169.254.0.0/16, 0.0.0.0/8, fc00::/7, fe80::/10, ff00::/8,
     * and IPv4-mapped variants of private ranges.</p>
     *
     * @param address the resolved IP address
     * @return true if the address is private/internal and should be blocked
     */
    public static boolean isPrivateAddress(InetAddress address) {
        if (address.isLoopbackAddress() || address.isLinkLocalAddress()) {
            return true;
        }
        byte[] octets = address.getAddress();
        if (octets.length == 4) {
            return isPrivateIpv4(octets);
        } else if (octets.length == 16) {
            return isPrivateIpv6(octets);
        }
        return false;
    }

    private static boolean isPrivateIpv4(byte[] octets) {
        int first = octets[0] & 0xFF;
        int second = octets[1] & 0xFF;
        // 10.0.0.0/8
        if (first == 10) return true;
        // 172.16.0.0/12
        if (first == 172 && second >= 16 && second <= 31) return true;
        // 192.168.0.0/16
        if (first == 192 && second == 168) return true;
        // 127.0.0.0/8 (loopback, already caught by isLoopbackAddress for IPv4)
        if (first == 127) return true;
        // 169.254.0.0/16 (link-local, already caught by isLinkLocalAddress)
        if (first == 169 && second == 254) return true;
        // 0.0.0.0/8
        if (first == 0) return true;
        return false;
    }

    private static boolean isPrivateIpv6(byte[] octets) {
        int firstByte = octets[0] & 0xFF;
        int secondByte = octets[1] & 0xFF;
        // fc00::/7 (Unique Local Addresses) — first byte is 1111_110x = 0xFC or 0xFD
        if ((firstByte & 0xFE) == 0xFC) return true;
        // fe80::/10 (Link-Local) — first byte is 1111_1110_10 = 0xFE80
        if (firstByte == 0xFE && (secondByte & 0xC0) == 0x80) return true;
        // ::1 (loopback) — already caught by isLoopbackAddress
        // ff00::/8 (Multicast) — not private, but blocked for safety
        if (firstByte == 0xFF) return true;
        // ::ffff:0:0/96 (IPv4-mapped) — check underlying IPv4
        if (isIpv4MappedAddress(octets)) {
            int mapped = octets[12] & 0xFF;
            int mapped2 = octets[13] & 0xFF;
            if (mapped == 10) return true;
            if (mapped == 172 && mapped2 >= 16 && mapped2 <= 31) return true;
            if (mapped == 192 && mapped2 == 168) return true;
            if (mapped == 127) return true;
            if (mapped == 0) return true;
        }
        return false;
    }

    /**
     * Check if the given 16-byte address is an IPv4-mapped IPv6 address
     * ({@code ::ffff:0:0/96}).
     *
     * @param octets the raw address bytes
     * @return true if this is an IPv4-mapped address
     */
    public static boolean isIpv4MappedAddress(byte[] octets) {
        return octets.length == 16
                && octets[10] == (byte) 0xFF && octets[11] == (byte) 0xFF
                && octets[0] == 0 && octets[1] == 0 && octets[2] == 0
                && octets[3] == 0 && octets[4] == 0 && octets[5] == 0
                && octets[6] == 0 && octets[7] == 0 && octets[8] == 0;
    }
}
