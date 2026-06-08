package eu.client.pingbypass.protocol;

import eu.client.pingbypass.protocol.packets.C2SSettingChangePacket;
import eu.client.pingbypass.protocol.packets.S2CSettingStatePacket;
import io.netty.buffer.Unpooled;
import net.jqwik.api.*;
import net.minecraft.network.PacketByteBuf;
import org.junit.jupiter.api.Assertions;

/**
 * Property-based test for setting serialization round-trip.
 *
 * <p><b>Validates: Requirements 9.6</b></p>
 *
 * <p>Feature: pingbypass-proxy, Property 6: Setting serialization round-trip</p>
 *
 * <p>For any valid setting value (BooleanSetting, NumberSetting, StringSetting,
 * ModeSetting, ColorSetting), serializing the value to a PacketByteBuf using the
 * custom channel format and then deserializing it back SHALL produce a value
 * equivalent to the original.</p>
 *
 * <p>Settings are transported as strings in C2SSettingChangePacket and
 * S2CSettingStatePacket. This test generates random string representations for
 * each setting type, writes them into a packet, serializes to buffer, deserializes,
 * and verifies the value string matches.</p>
 */
class SettingSerializationPropertyTest {

    // --- Providers for setting value strings ---

    @Provide
    Arbitrary<String> booleanSettingValues() {
        return Arbitraries.of("true", "false");
    }

    @Provide
    Arbitrary<String> numberSettingValues() {
        return Arbitraries.doubles()
                .between(-1_000_000, 1_000_000)
                .ofScale(4)
                .map(d -> {
                    if (d == Math.floor(d) && !Double.isInfinite(d)) {
                        return String.valueOf((long) Math.floor(d));
                    }
                    return String.valueOf(d);
                });
    }

    @Provide
    Arbitrary<String> stringSettingValues() {
        return Arbitraries.strings()
                .ascii()
                .ofMinLength(0)
                .ofMaxLength(100)
                .filter(s -> !s.contains("\0"));
    }

    @Provide
    Arbitrary<String> modeSettingValues() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(30);
    }

    @Provide
    Arbitrary<String> colorSettingValues() {
        Arbitrary<Integer> rgb = Arbitraries.integers().between(0, 255);
        Arbitrary<Boolean> bools = Arbitraries.of(true, false);
        return Combinators.combine(rgb, rgb, rgb, rgb, bools, bools)
                .as((r, g, b, a, sync, rainbow) ->
                        r + "," + g + "," + b + "," + a + "," + sync + "," + rainbow);
    }

    @Provide
    Arbitrary<String> moduleNames() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(30);
    }

    @Provide
    Arbitrary<String> settingNames() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(30);
    }

    // --- Round-trip via C2SSettingChangePacket ---

    /**
     * <b>Property 6: Setting serialization round-trip — BooleanSetting via C2SSettingChangePacket</b>
     *
     * <p><b>Validates: Requirements 9.6</b></p>
     */
    @Property(tries = 100)
    void booleanSettingRoundTripC2S(
            @ForAll("moduleNames") String moduleName,
            @ForAll("settingNames") String settingName,
            @ForAll("booleanSettingValues") String value
    ) {
        assertC2SRoundTrip(moduleName, settingName, value);
    }

    /**
     * <b>Property 6: Setting serialization round-trip — NumberSetting via C2SSettingChangePacket</b>
     *
     * <p><b>Validates: Requirements 9.6</b></p>
     */
    @Property(tries = 100)
    void numberSettingRoundTripC2S(
            @ForAll("moduleNames") String moduleName,
            @ForAll("settingNames") String settingName,
            @ForAll("numberSettingValues") String value
    ) {
        assertC2SRoundTrip(moduleName, settingName, value);
    }

    /**
     * <b>Property 6: Setting serialization round-trip — StringSetting via C2SSettingChangePacket</b>
     *
     * <p><b>Validates: Requirements 9.6</b></p>
     */
    @Property(tries = 100)
    void stringSettingRoundTripC2S(
            @ForAll("moduleNames") String moduleName,
            @ForAll("settingNames") String settingName,
            @ForAll("stringSettingValues") String value
    ) {
        assertC2SRoundTrip(moduleName, settingName, value);
    }

    /**
     * <b>Property 6: Setting serialization round-trip — ModeSetting via C2SSettingChangePacket</b>
     *
     * <p><b>Validates: Requirements 9.6</b></p>
     */
    @Property(tries = 100)
    void modeSettingRoundTripC2S(
            @ForAll("moduleNames") String moduleName,
            @ForAll("settingNames") String settingName,
            @ForAll("modeSettingValues") String value
    ) {
        assertC2SRoundTrip(moduleName, settingName, value);
    }

    /**
     * <b>Property 6: Setting serialization round-trip — ColorSetting via C2SSettingChangePacket</b>
     *
     * <p><b>Validates: Requirements 9.6</b></p>
     */
    @Property(tries = 100)
    void colorSettingRoundTripC2S(
            @ForAll("moduleNames") String moduleName,
            @ForAll("settingNames") String settingName,
            @ForAll("colorSettingValues") String value
    ) {
        assertC2SRoundTrip(moduleName, settingName, value);
    }

    // --- Round-trip via S2CSettingStatePacket ---

    /**
     * <b>Property 6: Setting serialization round-trip — BooleanSetting via S2CSettingStatePacket</b>
     *
     * <p><b>Validates: Requirements 9.6</b></p>
     */
    @Property(tries = 100)
    void booleanSettingRoundTripS2C(
            @ForAll("moduleNames") String moduleName,
            @ForAll("settingNames") String settingName,
            @ForAll("booleanSettingValues") String value
    ) {
        assertS2CRoundTrip(moduleName, settingName, value);
    }

    /**
     * <b>Property 6: Setting serialization round-trip — NumberSetting via S2CSettingStatePacket</b>
     *
     * <p><b>Validates: Requirements 9.6</b></p>
     */
    @Property(tries = 100)
    void numberSettingRoundTripS2C(
            @ForAll("moduleNames") String moduleName,
            @ForAll("settingNames") String settingName,
            @ForAll("numberSettingValues") String value
    ) {
        assertS2CRoundTrip(moduleName, settingName, value);
    }

    /**
     * <b>Property 6: Setting serialization round-trip — StringSetting via S2CSettingStatePacket</b>
     *
     * <p><b>Validates: Requirements 9.6</b></p>
     */
    @Property(tries = 100)
    void stringSettingRoundTripS2C(
            @ForAll("moduleNames") String moduleName,
            @ForAll("settingNames") String settingName,
            @ForAll("stringSettingValues") String value
    ) {
        assertS2CRoundTrip(moduleName, settingName, value);
    }

    /**
     * <b>Property 6: Setting serialization round-trip — ModeSetting via S2CSettingStatePacket</b>
     *
     * <p><b>Validates: Requirements 9.6</b></p>
     */
    @Property(tries = 100)
    void modeSettingRoundTripS2C(
            @ForAll("moduleNames") String moduleName,
            @ForAll("settingNames") String settingName,
            @ForAll("modeSettingValues") String value
    ) {
        assertS2CRoundTrip(moduleName, settingName, value);
    }

    /**
     * <b>Property 6: Setting serialization round-trip — ColorSetting via S2CSettingStatePacket</b>
     *
     * <p><b>Validates: Requirements 9.6</b></p>
     */
    @Property(tries = 100)
    void colorSettingRoundTripS2C(
            @ForAll("moduleNames") String moduleName,
            @ForAll("settingNames") String settingName,
            @ForAll("colorSettingValues") String value
    ) {
        assertS2CRoundTrip(moduleName, settingName, value);
    }

    // --- Helper methods ---

    private void assertC2SRoundTrip(String moduleName, String settingName, String value) {
        // 1. Create the packet with the original values
        C2SSettingChangePacket original = new C2SSettingChangePacket(moduleName, settingName, value);

        // 2. Serialize to PacketByteBuf
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        original.write(buf);

        // 3. Deserialize from the buffer
        C2SSettingChangePacket deserialized = new C2SSettingChangePacket(buf);

        // 4. Assert equivalence
        Assertions.assertEquals(moduleName, deserialized.getModuleName(),
                "C2S moduleName mismatch after round-trip");
        Assertions.assertEquals(settingName, deserialized.getSettingName(),
                "C2S settingName mismatch after round-trip");
        Assertions.assertEquals(value, deserialized.getValue(),
                "C2S setting value mismatch after round-trip");

        // 5. Clean up buffer
        buf.release();
    }

    private void assertS2CRoundTrip(String moduleName, String settingName, String value) {
        // 1. Create the packet with the original values
        S2CSettingStatePacket original = new S2CSettingStatePacket(moduleName, settingName, value);

        // 2. Serialize to PacketByteBuf
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        original.write(buf);

        // 3. Deserialize from the buffer
        S2CSettingStatePacket deserialized = new S2CSettingStatePacket(buf);

        // 4. Assert equivalence
        Assertions.assertEquals(moduleName, deserialized.getModuleName(),
                "S2C moduleName mismatch after round-trip");
        Assertions.assertEquals(settingName, deserialized.getSettingName(),
                "S2C settingName mismatch after round-trip");
        Assertions.assertEquals(value, deserialized.getValue(),
                "S2C setting value mismatch after round-trip");

        // 5. Clean up buffer
        buf.release();
    }
}
