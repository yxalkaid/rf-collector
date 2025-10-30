package com.yxalkaid.rfcollector.controller;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import com.yxalkaid.rfcollector.recorder.BaseRecorder;

import lombok.extern.slf4j.Slf4j;

/**
 * UDP监听控制器
 */
@Slf4j
public class UdpController extends BaseController {

    /**
     * 监听端口
     */
    private final int port;

    public UdpController(BaseRecorder recorder, int port) {
        super(recorder);
        this.port = port;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(this.port)) {
            log.info("UDP listener started on port {}", this.port);

            byte[] buffer = new byte[1024];
            while (isRunning && !Thread.interrupted()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String command = new String(packet.getData(), 0, packet.getLength()).trim();
                log.info("Received UDP command: {}", command);

                // 处理命令
                if ("START".equalsIgnoreCase(command)) {
                    recorder.start();
                } else if ("STOP".equalsIgnoreCase(command)) {
                    recorder.stop();
                } else if ("CLOSE".equalsIgnoreCase(command)) {
                    recorder.stop();
                    isRunning = false;
                }
            }
        } catch (Exception e) {
            log.error("Error during UDP command processing", e);
        }
    }

    public void stop() {
        isRunning = false;
    }
}