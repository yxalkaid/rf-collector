package com.yxalkaid.rfcollector.controller;

import com.yxalkaid.rfcollector.recorder.BaseRecorder;
import lombok.extern.slf4j.Slf4j;

/**
 * 控制器基类
 */
@Slf4j
public class BaseController implements Runnable {

    /**
     * RFID 记录器
     */
    protected final BaseRecorder recorder;

    /**
     * 是否运行中
     */
    protected volatile boolean isRunning;

    /**
     * 构造方法
     * @param recorder
     */
    public BaseController(BaseRecorder recorder) {
        if (recorder == null){
            throw new NullPointerException("recorder cannot be null");
        }
        if(!recorder.isConnecting()){
            log.warn("Recorder is not connected");
        }
        this.recorder = recorder;
        this.isRunning = false;
    }

    @Override
    public void run() {

        int duration = 10000;
        isRunning = true;
        try {
            log.info("Starting one-shot collection for {}ms", duration);
            recorder.start();
            Thread.sleep(duration);
            recorder.stop();
            log.info("One-shot collection completed");
        } catch (InterruptedException e) {
            log.warn("One-shot collection was interrupted");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Error during one-shot collection", e);
        } finally {
            isRunning = false;
        }
    }
}

