package com.yeonfish.phone2webcam.common.streaming;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.yeonfish.phone2webcam.common.streaming.SequencedPacket;

public class ClientManager implements StreamEvent {
    private int port;
    private int pSize;
    private DatagramSocket dgramSocket;
    private List<String> clients;


    public ClientManager(int port, int pSize, List<String> clients) throws SocketException {
        this.port = port;
        this.pSize = pSize;
        this.clients = clients;
        this.dgramSocket = new DatagramSocket(this.port);
    }

    public void startClientRegistering() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        CustomPacket dgramPacketRecv = receivePacket(1024, 0);

                        String data = new String(dgramPacketRecv.data, StandardCharsets.UTF_8);
                        Log.d("Packet received", data);

                        if (data.contains("P2WC-register_")) {
                            String ip = dgramPacketRecv.packet.getAddress().getHostAddress();

                            Log.i("IP", ip);

//                        Pattern p = Pattern.compile("[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}");
//                        Matcher m = p.matcher(ip);
//                        ip = m.group();

                            sendPacket(InetAddress.getByName(ip), ("P2WC-registerResult_SUCCESS|" + String.valueOf(pSize)).getBytes(StandardCharsets.UTF_8));
                        }

                        if (data.contains("P2WC-request")) {
                            String ip = dgramPacketRecv.packet.getAddress().getHostAddress();

                            clients.add(ip);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public void close() {
        this.dgramSocket.close();
    }

    private void sendPacket(InetAddress host, byte[] data) throws IOException {

        int chunkSize = 1024;
        int totalChunks = ((int) Math.ceil((double) data.length / chunkSize));

        Log.d("Total chunks", String.valueOf(totalChunks));

        for (int i = 0; i < totalChunks; i++) {
            int offset = i * chunkSize;
            int length = Math.min(chunkSize, data.length - offset);

            byte[] chunk = new byte[length];
            System.arraycopy(data, offset, chunk, 0, length);

            byte[] sPacket = new SequencedPacket(i, chunk).toString().getBytes(StandardCharsets.UTF_8);
            this.dgramSocket.send(new DatagramPacket(sPacket, sPacket.length, host, this.port));
        }

        byte[] sPacket = new SequencedPacket((totalChunks+1)*-1, null).toString().getBytes(StandardCharsets.UTF_8);
        this.dgramSocket.send(new DatagramPacket(sPacket, sPacket.length, host, this.port));
    }

    private CustomPacket receivePacket(int bytes, int timeout) throws IOException {
        byte[] buffer = new byte[bytes];
        DatagramPacket dgramPacketRecv = new DatagramPacket(buffer, buffer.length);

        int timeoutTmp = this.dgramSocket.getSoTimeout();
        int packetCount = -1;
        int currentPacketCount = 0;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            this.dgramSocket.setSoTimeout(timeout);

            Map<Integer, byte[]> data = new HashMap<>();
            while (true) {
                this.dgramSocket.receive(dgramPacketRecv);
                byte[] receivedData = new byte[dgramPacketRecv.getLength()];
                System.arraycopy(dgramPacketRecv.getData(), 0, receivedData, 0, dgramPacketRecv.getLength());
                currentPacketCount++;

                SequencedPacket sequencedPacket = new SequencedPacket(new String(receivedData, StandardCharsets.UTF_8));
                Log.d("Raw string", new String(receivedData, StandardCharsets.UTF_8));

                Log.d("Sequenced packet", "num: "+sequencedPacket.getSequenceNumber());

                if (sequencedPacket.getSequenceNumber() < 0) {
                    packetCount = sequencedPacket.getSequenceNumber()*-1;
                }else
                    data.put(sequencedPacket.getSequenceNumber(), sequencedPacket.getData());

                if (packetCount == currentPacketCount) break;
            }

            List<Integer> keySet = new ArrayList<>(data.keySet());
            Collections.sort(keySet);

            for (Integer i:keySet) {
                byteArrayOutputStream.write(data.get(i), 0, data.get(i).length);
            }

            this.dgramSocket.setSoTimeout(timeoutTmp);
        } catch (SocketTimeoutException exception) {
            this.dgramSocket.setSoTimeout(timeoutTmp);
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        CustomPacket cPacket = new CustomPacket();
        cPacket.packet = dgramPacketRecv;
        cPacket.data = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();

        return cPacket;
    }

    @Override
    public void OnNewCapture(StreamEvent streamEvent) {

    }

    @Override
    public void SendImg(String host, byte[] byteArray) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.i("Packet size", String.valueOf(byteArray.length));
                    sendPacket(InetAddress.getByName(host), byteArray);
                }catch (IOException exception) {
                    exception.printStackTrace();
                }
            }
        }).start();
    }
}
