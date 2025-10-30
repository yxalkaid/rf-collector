package com.yxalkaid.rfcollector;

import java.net.URL;
import java.nio.file.Path;

import com.yxalkaid.rfcollector.controller.CommandController;
import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.PropertyConfigurator;

import com.yxalkaid.rfcollector.recorder.CsvRecorder;

@Slf4j
public class Main {

    public static void main(String[] args) {

        String HOST = "Speedwayr-11-25-ab.local";
        String RESOURCE_PATH = "./src/main/resources";

        try {
            URL resourceUrl = Main.class.getClassLoader().getResource("log4j.xml");
            if (resourceUrl != null) {
                Path resourcePath = Path.of(resourceUrl.toURI());
                RESOURCE_PATH = resourcePath.getParent().toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String configPath = Path.of(RESOURCE_PATH, "SET_READER_CONFIG.xml").toString();
        String roSpecPath = Path.of(RESOURCE_PATH, "ADD_ROSPEC.xml").toString();

        // BasicConfigurator.configure();
        PropertyConfigurator.configure(Path.of(RESOURCE_PATH, "log4j.xml").toString());


        final CsvRecorder recorder = new CsvRecorder("./output");

        /*
         * 特别注意
         * 添加ShutdownHook后，
         * 以下情况会正常调用关闭钩子
         * 1. 程序自然运行结束
         * 2. 主动调用System.exit(int status)
         * 3. 通过 Ctrl+C 发送中断信号
         * 4. 抛出未捕获的运行时异常
         *
         * 以下情况不会触发关闭钩子
         * 1. 使用 kill -9 或任务管理器强制终止进程
         * 2. 通过 IDE 点击“Stop”按钮终止程序（实际上类似使用 kill -9）
         * 3. JVM本身崩溃
         */


        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            if (recorder != null) {
                recorder.close();
            }
            System.out.println("Shutdown complete.");
        }));


        recorder.open(
                HOST,
                configPath,
                roSpecPath
        );
        try {

            // 命令行控制
            Runnable listener = new CommandController(recorder, 1000 * 20);

            Thread listenerThread = new Thread(listener);
            listenerThread.start();
            listenerThread.join();
            // listenerThread.interrupt();
        } catch (InterruptedException e) {
            log.error("Sleep Interrupted");
        }

        System.exit(0);
    }
}
