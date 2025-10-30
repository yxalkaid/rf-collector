package com.yxalkaid.rfcollector.controller;

import com.yxalkaid.rfcollector.recorder.BaseRecorder;
import lombok.extern.slf4j.Slf4j;
import java.util.Scanner;

/**
 * 命令行监听控制器
 */
@Slf4j
public class CommandController extends BaseController {

    // 单次采集时长
    private final long duration;

    public CommandController(BaseRecorder recorder, long duration) {
        super(recorder);
        this.duration = duration;
    }

    @Override
    public void run() {
        try (Scanner input = new Scanner(System.in)) {
            while (isRunning && !Thread.interrupted()) {
                isRunning = false;
                System.out.println("Please enter 'P' to start collection");
                String line = input.nextLine();
                if (line.equalsIgnoreCase("P")) {
                    isRunning = true;
                }

                if (isRunning) {
                    log.info(String.format("Start collecting, expected to last for %ds", duration / 1000));
                    recorder.start();
                    Thread.sleep(duration);
                    recorder.stop();
                    log.info("End collection");
                }
            }
        } catch (Exception e) {
            log.error("Error during command collection", e);
        }
    }

    public void stop() {
        isRunning = false;
    }
}
