package com.yxalkaid.rfcollector.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SimpleTag {

    /**
     * EPC，十六进制字符串
     */
    private String epc;

    /**
     * 天线端口号，通常为1-4（取决于读写器天线数量）
     */
    private Short antennaId;

    /**
     * 信道索引
     */
    private Integer channelIndex;

    /**
     * 首次检测到标签的时间戳（微秒）
     */
    private Long firstSeenTime;

    /**
     * 最后检测到标签的时间戳（微秒）
     */
    private Long lastSeenTime;

    /**
     * 标签被检测到的次数
     */
    private Short tagSeenCount;

    /**
     * 峰值RSSI（原始值），
     * 乘以 1/100 得到实际的dBm值
     */
    private Short peakRssiRaw;

    /**
     * 多普勒频移（原始值），
     * 乘以 1/16 得到实际的Hz值
     */
    private Short dopplerFrequencyRaw;

    /**
     * 相位（原始值），
     * 值范围为0至4096，乘以 π/2048 得到实际的相位角弧度值
     */
    private Short phaseRaw;
}
