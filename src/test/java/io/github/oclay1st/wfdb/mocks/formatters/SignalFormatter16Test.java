package io.github.oclay1st.wfdb.mocks.formatters;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.oclay1st.wfdb.HeaderSignal;
import io.github.oclay1st.wfdb.SignalFormat;
import io.github.oclay1st.wfdb.formatters.SignalFormatter;
import io.github.oclay1st.wfdb.formatters.SignalFormatter16;
import io.github.oclay1st.wfdb.mocks.MockHeaderSignal;

class SignalFormatter16Test {

    @Test
    @DisplayName("Should convert raw data to signal samples with format 16")
    void shouldConvertFromRawDataToFormat16() {
        byte[] source = { 1, 2, 3, 4 };
        HeaderSignal signal = new MockHeaderSignal.Builder()
                .format(SignalFormat.FORMAT_16)
                .initialValue(513)
                .build();
        HeaderSignal[] headerSignals = { signal };
        SignalFormatter formatter = new SignalFormatter16();
        int[] formattedSamples = formatter.convertBytesToSamples(source, headerSignals);
        assertNotNull(formattedSamples);
        assertArrayEquals(new int[] { 513, 1027 }, formattedSamples);
    }

    @Test
    @DisplayName("Should convert signal samples with format 16 to raw data")
    void shouldConvertFromFormat16ToRawData() {
        int[] samples = new int[] { 513, 1027 };
        HeaderSignal signal = new MockHeaderSignal.Builder()
                .format(SignalFormat.FORMAT_16)
                .initialValue(513)
                .build();
        HeaderSignal[] headerSignals = { signal };
        SignalFormatter16 formatter = new SignalFormatter16();
        byte[] source = formatter.convertSamplesToBytes(samples, headerSignals);
        assertNotNull(source);
        assertArrayEquals(new byte[]{ 1, 2, 3, 4 }, source);
    }

}
