package io.github.oclay1st.wfdb.mocks.formatters;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.oclay1st.wfdb.HeaderSignal;
import io.github.oclay1st.wfdb.SignalFormat;
import io.github.oclay1st.wfdb.formatters.SignalFormatter;
import io.github.oclay1st.wfdb.formatters.SignalFormatter311;
import io.github.oclay1st.wfdb.mocks.MockHeaderSignal;

class SignalFormatter311Test {

    @Test
    @DisplayName("Should convert raw data to signal samples with format 311")
    void shouldConvertFromRawDataToFormat311() {
        byte[] source = { 1, 2, 3, 4 };
        HeaderSignal signal = new MockHeaderSignal.Builder()
                .format(SignalFormat.FORMAT_311)
                .initialValue(-511)
                .build();
        HeaderSignal[] headerSignals = { signal };
        SignalFormatter formatter = new SignalFormatter311();
        int[] formattedSamples = formatter.convertBytesToSamples(source, headerSignals);
        assertNotNull(formattedSamples);
        assertArrayEquals(new int[] { -511, 192, 64 }, formattedSamples);
    }

    @Test
    @DisplayName("Should convert signal samples with format 311 to raw data")
    void shouldConvertFromFormat311ToRawData() {
        int[] samples = { -511, 192, 64 };
        HeaderSignal signal = new MockHeaderSignal.Builder()
                .format(SignalFormat.FORMAT_311)
                .initialValue(-511)
                .build();
        HeaderSignal[] headerSignals = { signal };
        SignalFormatter formatter = new SignalFormatter311();
        byte[] source = formatter.convertSamplesToBytes(samples, headerSignals);
        assertNotNull(source);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, source);
    }

}
