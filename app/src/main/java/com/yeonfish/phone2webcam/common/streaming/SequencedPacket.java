package com.yeonfish.phone2webcam.common.streaming;

import java.util.Arrays;

public class SequencedPacket {

    private int sequenceNumber;
    private byte[] data;

    public SequencedPacket(String serialized) {
        String[] deserialized = serialized.split("\\|");
        this.sequenceNumber = Integer.parseInt(deserialized[0]);
        if (deserialized[1].equals("null"))
            this.data = null;
        else
            this.data = fromString(deserialized[1]);
    }

    public SequencedPacket(int sequenceNumber, byte[] data) {
        this.sequenceNumber = sequenceNumber;
        this.data = data;
    }

    public int getSequenceNumber() {
        return this.sequenceNumber;
    }

    public byte[] getData() {
        return data;
    }

    public String toString() {
        StringBuffer serialized = new StringBuffer();
        serialized.append(this.sequenceNumber);
        serialized.append("|");
        serialized.append(Arrays.toString(this.data));

        return serialized.toString();
    }

    private static byte[] fromString(String byteArrayString) {
        String[] stringValues = byteArrayString.replaceAll("\\[|\\]|\\s", "").split(",");
        byte[] result = new byte[stringValues.length];

        for (int i = 0; i < stringValues.length; i++) {
            result[i] = (byte) Integer.parseInt(stringValues[i].trim(), 10);
        }

        return result;
    }
}
