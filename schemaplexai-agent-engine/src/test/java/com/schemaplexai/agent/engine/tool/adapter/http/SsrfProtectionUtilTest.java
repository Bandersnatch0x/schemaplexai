package com.schemaplexai.agent.engine.tool.adapter.http;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;

class SsrfProtectionUtilTest {

    // --- IPv4 private ranges ---

    @Test
    void shouldBlockLoopbackIpv4() throws Exception {
        assertTrue(SsrfProtectionUtil.isPrivateAddress(InetAddress.getByName("127.0.0.1")));
        assertTrue(SsrfProtectionUtil.isPrivateAddress(InetAddress.getByName("127.255.255.255")));
    }

    @Test
    void shouldBlockPrivate10Range() throws Exception {
        assertTrue(SsrfProtectionUtil.isPrivateAddress(InetAddress.getByName("10.0.0.1")));
        assertTrue(SsrfProtectionUtil.isPrivateAddress(InetAddress.getByName("10.255.255.255")));
    }

    @Test
    void shouldBlockPrivate172Range() throws Exception {
        assertTrue(SsrfProtectionUtil.isPrivateAddress(InetAddress.getByName("172.16.0.1")));
        assertTrue(SsrfProtectionUtil.isPrivateAddress(InetAddress.getByName("172.31.255.255")));
    }

    @Test
    void shouldNotBlockPublic172Range() throws Exception {
        assertFalse(SsrfProtectionUtil.isPrivateAddress(InetAddress.getByName("172.15.0.1")));
        assertFalse(SsrfProtectionUtil.isPrivateAddress(InetAddress.getByName("172.32.0.1")));
    }

    @Test
    void shouldBlockPrivate192Range() throws Exception {
        assertTrue(SsrfProtectionUtil.isPrivateAddress(InetAddress.getByName("192.168.0.1")));
        assertTrue(SsrfProtectionUtil.isPrivateAddress(InetAddress.getByName("192.168.255.255")));
    }

    @Test
    void shouldNotBlockPublic192Range() throws Exception {
        assertFalse(SsrfProtectionUtil.isPrivateAddress(InetAddress.getByName("192.167.0.1")));
        assertFalse(SsrfProtectionUtil.isPrivateAddress(InetAddress.getByName("192.169.0.1")));
    }

    @Test
    void shouldBlockLinkLocal169254() throws Exception {
        assertTrue(SsrfProtectionUtil.isPrivateAddress(InetAddress.getByName("169.254.0.1")));
        assertTrue(SsrfProtectionUtil.isPrivateAddress(InetAddress.getByName("169.254.255.255")));
    }

    @Test
    void shouldBlockZeroNetwork() throws Exception {
        assertTrue(SsrfProtectionUtil.isPrivateAddress(InetAddress.getByName("0.0.0.0")));
        assertTrue(SsrfProtectionUtil.isPrivateAddress(InetAddress.getByName("0.255.255.255")));
    }

    @Test
    void shouldAllowPublicIpv4() throws Exception {
        assertFalse(SsrfProtectionUtil.isPrivateAddress(InetAddress.getByName("8.8.8.8")));
        assertFalse(SsrfProtectionUtil.isPrivateAddress(InetAddress.getByName("1.1.1.1")));
        assertFalse(SsrfProtectionUtil.isPrivateAddress(InetAddress.getByName("104.16.0.0")));
    }

    // --- IPv6 ---

    @Test
    void shouldBlockLoopbackIpv6() throws Exception {
        assertTrue(SsrfProtectionUtil.isPrivateAddress(InetAddress.getByName("::1")));
    }

    @Test
    void shouldBlockLinkLocalIpv6() throws Exception {
        assertTrue(SsrfProtectionUtil.isPrivateAddress(InetAddress.getByName("fe80::1")));
        assertTrue(SsrfProtectionUtil.isPrivateAddress(InetAddress.getByName("fe80::ffff")));
        assertTrue(SsrfProtectionUtil.isPrivateAddress(InetAddress.getByName("febf::1")));
    }

    @Test
    void shouldBlockUniqueLocalIpv6() throws Exception {
        assertTrue(SsrfProtectionUtil.isPrivateAddress(InetAddress.getByName("fc00::1")));
        assertTrue(SsrfProtectionUtil.isPrivateAddress(InetAddress.getByName("fd00::1")));
    }

    @Test
    void shouldBlockMulticastIpv6() throws Exception {
        assertTrue(SsrfProtectionUtil.isPrivateAddress(InetAddress.getByName("ff00::1")));
        assertTrue(SsrfProtectionUtil.isPrivateAddress(InetAddress.getByName("ff02::1")));
    }

    @Test
    void shouldAllowPublicIpv6() throws Exception {
        assertFalse(SsrfProtectionUtil.isPrivateAddress(InetAddress.getByName("2001:4860:4860::8888")));
        assertFalse(SsrfProtectionUtil.isPrivateAddress(InetAddress.getByName("2606:4700:4700::1111")));
    }

    // --- IPv4-mapped IPv6 ---

    @Test
    void shouldBlockIpv4MappedPrivate() throws Exception {
        assertTrue(SsrfProtectionUtil.isPrivateAddress(InetAddress.getByName("::ffff:10.0.0.1")));
        assertTrue(SsrfProtectionUtil.isPrivateAddress(InetAddress.getByName("::ffff:192.168.1.1")));
        assertTrue(SsrfProtectionUtil.isPrivateAddress(InetAddress.getByName("::ffff:127.0.0.1")));
        assertTrue(SsrfProtectionUtil.isPrivateAddress(InetAddress.getByName("::ffff:172.16.0.1")));
    }

    @Test
    void shouldAllowIpv4MappedPublic() throws Exception {
        assertFalse(SsrfProtectionUtil.isPrivateAddress(InetAddress.getByName("::ffff:8.8.8.8")));
    }

    // --- isIpv4MappedAddress ---

    @Test
    void shouldDetectIpv4MappedAddress() {
        byte[] mapped = new byte[16];
        mapped[10] = (byte) 0xFF;
        mapped[11] = (byte) 0xFF;
        assertTrue(SsrfProtectionUtil.isIpv4MappedAddress(mapped));
    }

    @Test
    void shouldRejectNonIpv4MappedAddress() {
        byte[] notMapped = new byte[16];
        assertFalse(SsrfProtectionUtil.isIpv4MappedAddress(notMapped));

        byte[] wrongLength = new byte[4];
        assertFalse(SsrfProtectionUtil.isIpv4MappedAddress(wrongLength));
    }

    @Test
    void shouldRejectIpv4MappedWithWrongPrefix() {
        byte[] wrongPrefix = new byte[16];
        wrongPrefix[10] = (byte) 0xFF;
        wrongPrefix[11] = (byte) 0xFE; // not FF
        assertFalse(SsrfProtectionUtil.isIpv4MappedAddress(wrongPrefix));
    }
}
