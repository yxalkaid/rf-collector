package com.yxalkaid.rfcollector.recorder;

import com.csvreader.CsvWriter;
import com.yxalkaid.rfcollector.domain.SimpleTag;
import lombok.extern.slf4j.Slf4j;
import org.llrp.ltk.generated.parameters.TagReportData;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * CSV记录器, 将数据记录到csv文件
 */
@Slf4j
public class CsvRecorder extends BaseRecorder {

    /**
     * csv写入
     */
    private CsvWriter csvWriter;

    /**
     * 父目录
     */
    private String parentDir;

    /**
     * 是否正在写入
     */
    private boolean isWriting;

    /**
     * 已记录数
     */
    private int recordCount;

    public CsvRecorder(String parentDir) {
        this.parentDir = parentDir;
        this.initCsvWriter(parentDir);
    }

    /**
     * 初始化csv写入
     * @param parentDir
     */
    private void initCsvWriter(String parentDir) {
        try {

            LocalDateTime localDateTime = LocalDateTime.now();
            DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
            String fileName = "RFID_" + df.format(localDateTime) + ".csv";

            File csvFile = new File(parentDir + File.separator + fileName);

            File parent = csvFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            csvFile.createNewFile();
            log.info("Writing to " + csvFile.getAbsolutePath());

            this.csvWriter=new CsvWriter(csvFile.getAbsolutePath(), ',',Charset.forName("GBK"));
            this.csvWriter.writeRecord(
                Arrays.asList(
                    "time",
                    "id", 
                    "channel", 
                    "phase", 
                    "rssi",
                    "antenna"
                ).toArray(new String[0])
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void processTagReports(List<TagReportData> tdList) {
        if (tdList == null || tdList.isEmpty()) {
            return;
        }

        try {
            for (TagReportData td : tdList) {
                SimpleTag tag = this.buildSimpleTag(td);
                if (tag != null) {
                    this.recordCount+=1;
                    String[] line = this.buildLine(tag);
                    this.csvWriter.writeRecord(line);
                }
            }
        } catch (IOException e) {
            log.error("Error writing to csv", e);
        }
    }

    private String[] buildLine(SimpleTag tag) {
        if (tag == null) {
            return null;
        }
        return new String[]{
                String.valueOf(tag.getFirstSeenTime()),
                tag.getEpc(),
                String.valueOf(tag.getChannelIndex()),
                String.valueOf(tag.getPhaseRaw()),
                String.valueOf(tag.getPeakRssiRaw()),
                String.valueOf(tag.getAntennaId())
        };
    }
}
