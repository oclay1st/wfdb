package dev.oclay.wfdb;

import static java.util.stream.Collectors.groupingBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public record SingleSegmentRecord(SingleSegmentHeader header, int[][] samplesPerSingal) {

    public static SingleSegmentRecord parse(Path recordPath) throws IOException, ParseException {
        Path headerFilePath = recordPath.resolveSibling(recordPath.getFileName() + ".hea");
        try (InputStream inputStream = Files.newInputStream(headerFilePath)) {
            // Parse info from the header file
            SingleSegmentHeader header = SingleSegmentHeader.parse(inputStream);
            int numberOfSignals = header.headerRecord().numberOfSignals();
            int numberOfSamplesPerSignal = header.headerRecord().numberOfSamples();
            int[][] samplesPerSignal = new int[numberOfSignals][numberOfSamplesPerSignal];
            int samplesPerSignalIndex = 0;
            // Group the signals by filename
            Map<String, List<HeaderSignal>> recordFileMap = Arrays.stream(header.headerSignals())
                    .collect(groupingBy(HeaderSignal::filename));
            // Parse the samples files given the signals
            for (Map.Entry<String, List<HeaderSignal>> entry : recordFileMap.entrySet()) {
                Path samplesFilePath = recordPath.resolveSibling(entry.getKey());
                int[][] samples = parseSamples(samplesFilePath, entry.getValue(), numberOfSamplesPerSignal);
                System.arraycopy(samples, 0, samplesPerSignal, samplesPerSignalIndex, samples.length);
                samplesPerSignalIndex += samples.length - 1;
            }
            return new SingleSegmentRecord(header, samplesPerSignal);
        }
    }

    private static int[][] parseSamples(Path samplesFilePath, List<HeaderSignal> signals, int numberOfSamplesPerSignal)
            throws IOException {
        try (InputStream samplesInputStream = Files.newInputStream(samplesFilePath)) {
            byte[] data = samplesInputStream.readAllBytes();
            int format = signals.get(0).format(); // All signals have the same format
            int signalSize = signals.size();
            int[] formattedSamples = toFormat(format, data);
            int[][] samplesPerSignal = new int[signalSize][numberOfSamplesPerSignal];
            int signalIndex = 0;
            for (int i = 0; i < signalSize; i++) {
                signalIndex = 0;
                for (int j = i; j < formattedSamples.length; j += signalSize) {
                    samplesPerSignal[i][signalIndex] = formattedSamples[j];
                    signalIndex++;
                }
            }
            return samplesPerSignal;
        }
    }

    private static int[] toFormat(int format, byte[] data) {
        return switch (format) {
            case 8 -> toFormat8(data);
            case 16 -> toFormat16(data);
            case 32 -> toFormat32(data);
            case 80 -> toFormat80(data);
            case 160 -> toFormat160(data);
            case 212 -> toFormat212(data);
            case 310 -> toFormat310(data);
            case 311 -> toFormat311(data);
            default -> throw new IllegalArgumentException("Unsupported bit reference : " + format);
        };
    }

    /**
     * Convert sampling data to format 8
     *
     * Each sample is represented as an 8-bit first difference; i.e., to get the
     * value of sample n, sum the first n bytes of the sample data file together
     * with the initial value from the header file. When format 8 files are created,
     * first differences which cannot be represented in 8 bits are represented
     * instead by the largest difference of the appropriate sign (-128 or +127), and
     * subsequent differences are adjusted such that the correct amplitude is
     * obtained as quickly as possible. Thus there may be loss of information if
     * signals in another of the formats listed below are converted to format 8.
     * Note that the first differences stored in multiplexed format 8 files are
     * always determined by subtraction of successive samples from the same signal
     * (otherwise signals with baselines which differ by 128 units or more could not
     * be represented this way)
     */
    public static int[] toFormat8(byte[] data) {
        throw new IllegalStateException("Not implemeted yet");
    }

    /**
     * Convert sampling data to format 16
     * 
     * Each sample is represented by a 16-bit two’s complement amplitude stored
     * least significant byte first. Any unused high-order bits are sign-extended
     * from the most significant bit.
     */
    public static int[] toFormat16(byte[] data) {
        int index = 0;
        int[] values = new int[data.length / 2];
        ShortBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        while (buffer.hasRemaining()) {
            values[index] = buffer.get();
            index++;
        }
        return values;
    }

    /**
     * Convert sampling data to format 32
     *
     * Each sample is represented by a 32-bit two’s complement amplitude stored
     * least significant byte first.
     */
    public static int[] toFormat32(byte[] data) {
        int[] samples = new int[data.length / 4];
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(samples);
        return samples;
    }

    /**
     * Convert sampling data to format 80
     *
     * Each sample is represented by an 8-bit amplitude in offset binary form (i.e.,
     * 128 must be subtracted from each unsigned byte to obtain a signed 8-bit
     * amplitude).
     */
    public static int[] toFormat80(byte[] data) {
        int[] samples = new int[data.length];
        for (int i = 0; i < data.length; i++) {
           samples[i] = (data[i] & 0xFF) - 128; 
        }
        return samples;
    }

    /**
     * Convert sampling data to format 160
     *
     * Each sample is represented by a 16-bit amplitude in offset binary form (i.e.,
     * 32,768 must be subtracted from each unsigned byte pair to obtain a signed
     * 16-bit amplitude). As for format 16, the least significant byte of each pair
     * is first.
     */
    public static int[] toFormat160(byte[] data) {
        throw new IllegalStateException("Not implemeted yet");
    }

    /**
     * Convert sampling data to format 212
     *
     * Each sample is represented by a 12-bit two’s complement amplitude. The first
     * sample is obtained from the 12 least significant bits of the first byte pair
     * (stored least significant byte first). The second sample is formed from the 4
     * remaining bits of the first byte pair (which are the 4 high bits of the
     * 12-bit sample) and the next byte (which contains the remaining 8 bits of the
     * second sample). The process is repeated for each successive pair of samples.
     * 
     * Given 3 unsigned bytes : represented as 244 15 78
     * 244 -> 1 1 1 1 0 1 0 0
     * 15 -> 1 1 1 1
     * 78 -> 1 0 0 1 1 1 0
     * Then :
     * First sample: 0 0 0 0 [ 1 1 1 1 | 1 1 1 1 0 1 0 0 ] -> 4084 or -12
     * Second sample: 0 0 0 0 | 1 0 0 1 1 1 0 -> 78
     *
     * Two complement form where values greater than 2^11 - 1 = 2047 are negative
     * 12 bits goes 0 to 4096 for unsigned and -2048 to 2047 for signed
     */
    public static int[] toFormat212(byte[] data) {
        int[] samples = new int[data.length - data.length / 3];
        int index = 0;
        for (int i = 0; i < data.length; i += 3) {
            // first sample
            int firstByteUnsigned = data[i] & 0xFF;
            int secondByteUnsigned = data[i + 1] & 0xFF;
            int lastFourBitsOfSecondByte = secondByteUnsigned & 0x0F;
            int firstSample = (lastFourBitsOfSecondByte << 8) + firstByteUnsigned;
            // Convert to two complement amplitude 
            samples[index] = firstSample > 2047 ? firstSample - 4096 : firstSample; 
            // second sample
            int thirdUnsigned = data[i + 2] & 0xFF;
            int firstFourBitsOfSecondByte = (secondByteUnsigned >> 4) & 0x0F;
            int secondSample = (firstFourBitsOfSecondByte << 8) + thirdUnsigned;
            // Convert to two complement amplitude 
            samples[index + 1] = secondSample > 2047 ? secondSample - 4096 : secondSample; 
            // increment array index
            index += 2;
        }
        return samples;
    }

    /**
     * Convert sampling data to format 310
     *
     * Each sample is represented by a 10-bit two’s-complement amplitude. The first
     * sample is obtained from the 11 least significant bits of the first byte pair
     * (stored least significant byte first), with the low bit discarded. The second
     * sample comes from the 11 least significant bits of the second byte pair, in
     * the same way as the first. The third sample is formed from the 5 most
     * significant bits of each of the first two byte pairs (those from the first
     * byte pair are the least significant bits of the third sample).
     */
    private static int[] toFormat310(byte[] data) {
        throw new IllegalStateException("Not implemeted yet");
    }

    /**
     * Convert sampling data to format 311
     *
     * Each sample is represented by a 10-bit two’s-complement amplitude. Three
     * samples are bit-packed into a 32-bit integer as for format 310, but the
     * layout is different. Each set of four bytes is stored in little-endian order
     * (least significant byte first, most significant byte last). The first sample
     * is obtained from the 10 least significant bits of the 32-bit integer, the
     * second is obtained from the next 10 bits, the third from the next 10 bits,
     * and the two most significant bits are unused. This process is repeated for
     * each successive set of three samples.
     */
    public static int[] toFormat311(byte[] data) {
        throw new IllegalStateException("Not implemeted yet");
    }

    public int time() {
        return (int) (header.headerRecord().numberOfSamples() / header.headerRecord().samplingFrequency());
    }

}
