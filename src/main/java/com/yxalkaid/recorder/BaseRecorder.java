package com.yxalkaid.recorder;

import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import org.jdom.JDOMException;
import org.llrp.ltk.exceptions.InvalidLLRPMessageException;
import org.llrp.ltk.generated.custom.messages.IMPINJ_ENABLE_EXTENSIONS;
import org.llrp.ltk.generated.custom.messages.IMPINJ_ENABLE_EXTENSIONS_RESPONSE;
import org.llrp.ltk.generated.enumerations.GetReaderCapabilitiesRequestedData;
import org.llrp.ltk.generated.enumerations.GetReaderConfigRequestedData;
import org.llrp.ltk.generated.enumerations.StatusCode;
import org.llrp.ltk.generated.messages.*;
import org.llrp.ltk.generated.parameters.*;
import org.llrp.ltk.net.LLRPConnection;
import org.llrp.ltk.net.LLRPConnectionAttemptFailedException;
import org.llrp.ltk.net.LLRPConnector;
import org.llrp.ltk.net.LLRPEndpoint;
import org.llrp.ltk.types.*;
import org.llrp.ltk.util.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * 基础记录器
 */
@Slf4j
public class BaseRecorder implements LLRPEndpoint {

    /**
     * 消息超时时长（毫秒）
     */
    private final int messageTimeout = 10000;

    /**
     * 读写器连接
     */
    private LLRPConnection connection;

    /**
     * 消息ID
     */
    private int MessageID = 0;

    /**
     * RoSpec
     */
    private ROSpec rospec;

    /**
     * 打开连接
     *
     * @param host
     * @param configPath
     * @param RoSpecPath
     */
    public void open(String host, String configPath, String RoSpecPath) {
        if (this.connection != null){
            return;
        }

        this.connect(host); // 连接读写器
        this.enableImpinjExtensions(); // 启用Impinj扩展功能
        this.resetToFactoryDefaults(); // 恢复出厂设置
        this.deleteRoSpecs();// 删除所有RoSpec

        this.getReaderCapabilities(); // 获取读写器能力信息
        this.getReaderConfiguration(); // 获取读写器配置信息

        this.setReaderConfiguration(configPath); // 进行读写器配置
        this.addRoSpec(RoSpecPath); // 添加RoSpec任务
        this.enableRoSpec(); // 启用RoSpec
    }

    /**
     * 连接读写器
     *
     * @param host
     */
    private void connect(String host) {
        if (this.connection != null) {
            return;
        }

        log.info("Initiate LLRP connection to {}", host);

        this.connection = new LLRPConnector(this, host);
        try {
            ((LLRPConnector) connection).connect();
            log.info("Connecting to reader already");
        } catch (LLRPConnectionAttemptFailedException ex) {
            log.error("Connection to reader failed", ex);
            this.connection = null;
            System.exit(1);
        }
    }

    /**
     * 关闭连接
     */
    public void close() {
        if (this.connection == null) {
            return;
        }

        ((LLRPConnector) this.connection).disconnect();
        this.connection = null;

        log.info("Disconnected from reader");
    }

    public void start(){
        if (this.connection == null) {
            return;
        }

        this.enableRoSpec();
        this.startRoSpec();
    }

    public void stop(){
        if (this.connection == null) {
            return;
        }

        this.stopRoSpec();
        this.disableRoSpec();
    }

    @Override
    public void messageReceived(LLRPMessage message) {

        if (message.getTypeNum() == RO_ACCESS_REPORT.TYPENUM) {
            RO_ACCESS_REPORT report = (RO_ACCESS_REPORT) message;

            List<TagReportData> tdlist = report.getTagReportDataList();

            for (TagReportData tr : tdlist) {
                logOneTagReport(tr);
            }

            List<Custom> clist = report.getCustomList();
            for (Custom custom : clist) {
                logOneCustom(custom);
            }


        } else if (message.getTypeNum() == READER_EVENT_NOTIFICATION.TYPENUM) {
            // TODO
        }
    }

    @Override
    public void errorOccured(String s) {
        log.error(s);
    }

    /**
     * 处理Custom信息
     *
     * @param custom
     */
    protected void logOneCustom(Custom custom) {

        if (!custom.getVendorIdentifier().equals(25882)) {
            log.error("Non Impinj Extension Found in message");
            return;
        }
    }

    /**
     * 处理TagReportData信息
     *
     * @param tr
     */
    protected void logOneTagReport(TagReportData tr) {
    }


    /**
     * 获取一个唯一消息ID
     *
     * @return
     */
    private UnsignedInteger getUniqueMessageID() {
        return new UnsignedInteger(MessageID++);
    }

    // 处理StatusCode
    private void logStatusCode(StatusCode status, String msgType) {
        if (status.equals(new StatusCode("M_Success"))) {
            log.info("{} was successful", msgType);
        } else {
            log.error("{} failed", msgType);
            System.exit(1);
        }
    }

    /**
     * 启用扩展功能
     */
    private void enableImpinjExtensions() {
        LLRPMessage response;

        String msgType = "Enable Impinj Extensions";
        log.info("{} ...", msgType);

        try {
            IMPINJ_ENABLE_EXTENSIONS message = new IMPINJ_ENABLE_EXTENSIONS();
            message.setMessageID(getUniqueMessageID());

            response = connection.transact(message, messageTimeout);

            StatusCode status = ((IMPINJ_ENABLE_EXTENSIONS_RESPONSE) response).getLLRPStatus().getStatusCode();
            this.logStatusCode(status, msgType);
        } catch (TimeoutException ex) {
            log.error("Timeout waiting for response to {}", msgType, ex);
            System.exit(1);
        }
    }

    /**
     * 恢复出厂设置
     */
    private void resetToFactoryDefaults() {
        LLRPMessage response;

        String msgType = "Reset To Factory Defaults";
        log.info("{} ...", msgType);

        try {
            SET_READER_CONFIG set = new SET_READER_CONFIG();
            set.setMessageID(getUniqueMessageID());
            set.setResetToFactoryDefault(new Bit(true)); // 设置为恢复出厂设置

            response = connection.transact(set, messageTimeout);

            StatusCode status = ((SET_READER_CONFIG_RESPONSE) response).getLLRPStatus().getStatusCode();
            this.logStatusCode(status, msgType);
        } catch (TimeoutException ex) {
            log.error("Timeout waiting for response to {}", msgType, ex);
            System.exit(1);
        }
    }


    /**
     * 删除所有RoSpec
     */
    private void deleteRoSpecs() {
        LLRPMessage response;
        String msgType = "Delete RoSpecs";

        try {
            log.info("{} ...", msgType);

            DELETE_ROSPEC delete_rospec = new DELETE_ROSPEC();
            delete_rospec.setMessageID(this.getUniqueMessageID());
            delete_rospec.setROSpecID(new UnsignedInteger(0)); // 设置为0，表示删除所有RoSpec

            response = connection.transact(delete_rospec, messageTimeout);

            StatusCode status = ((DELETE_ROSPEC_RESPONSE) response).getLLRPStatus().getStatusCode();
            this.logStatusCode(status, msgType);
        } catch (TimeoutException ex) {
            log.error("Timeout waiting for response to {}", msgType, ex);
            System.exit(1);
        }
    }

    /**
     * 获取读写器能力信息
     */
    private void getReaderCapabilities() {
        LLRPMessage response;

        String msgType = "Get Reader Capabilities";
        log.info("{} ...", msgType);

        try {
            GET_READER_CAPABILITIES message = new GET_READER_CAPABILITIES();
            GetReaderCapabilitiesRequestedData data = new
                    GetReaderCapabilitiesRequestedData(
                    GetReaderCapabilitiesRequestedData.All);
            message.setRequestedData(data);
            message.setMessageID(getUniqueMessageID());

            response = connection.transact(message, messageTimeout);

            GET_READER_CAPABILITIES_RESPONSE resp = (GET_READER_CAPABILITIES_RESPONSE) response;
            StatusCode status = resp.getLLRPStatus().getStatusCode();
            if (status.equals(new StatusCode("M_Success"))) {
                log.info("{} was successful", msgType);

                // get the info we need
                GeneralDeviceCapabilities dev_cap = resp.getGeneralDeviceCapabilities();
                if ((dev_cap == null) ||
                        (!dev_cap.getDeviceManufacturerName().equals(new UnsignedInteger(25882)))) {
                    log.error("Must use Impinj model Reader, not " +
                            dev_cap.getDeviceManufacturerName().toString());
                    System.exit(1);
                }

                UnsignedInteger modelName = dev_cap.getModelName();
                log.info("Found Impinj reader model " + modelName.toString());

                // get the max power level
                if (resp.getRegulatoryCapabilities() != null) {
                    UHFBandCapabilities band_cap =
                            resp.getRegulatoryCapabilities().getUHFBandCapabilities();

                    List<TransmitPowerLevelTableEntry> pwr_list =
                            band_cap.getTransmitPowerLevelTableEntryList();

                    TransmitPowerLevelTableEntry entry =
                            pwr_list.get(pwr_list.size() - 1);

                    UnsignedShort maxPowerIndex = entry.getIndex();
                    SignedShort maxPower = entry.getTransmitPowerValue();
                    // LLRP sends power in dBm * 100
                    double d = ((double) maxPower.intValue()) / 100;

                    log.info("Max power " + d +
                            " dBm at index " + maxPowerIndex.toString());
                }
            } else {
                log.info("{} faild", msgType);
                System.exit(1);
            }
        } catch (TimeoutException ex) {
            log.error("Timeout waiting for response to {}", msgType, ex);
            System.exit(1);
        }
    }

    /**
     * 获取读写器配置信息
     */
    private void getReaderConfiguration() {
        LLRPMessage response;

        String msgType = "Get Reader Configuration";
        log.info("{} ...", msgType);

        try {
            GET_READER_CONFIG message = new GET_READER_CONFIG();
            GetReaderConfigRequestedData data =
                    new GetReaderConfigRequestedData(
                            GetReaderConfigRequestedData.All);
            message.setRequestedData(data);
            message.setMessageID(getUniqueMessageID());
            message.setAntennaID(new UnsignedShort(0));
            message.setGPIPortNum(new UnsignedShort(0));
            message.setGPOPortNum(new UnsignedShort(0));

            response = connection.transact(message, messageTimeout);

            GET_READER_CONFIG_RESPONSE resp = (GET_READER_CONFIG_RESPONSE) response;
            StatusCode status = resp.getLLRPStatus().getStatusCode();
            if (status.equals(new StatusCode("M_Success"))) {
                log.info("{} was successful", msgType);

                List<AntennaConfiguration> alist = resp.getAntennaConfigurationList();

                if (!alist.isEmpty()) {
                    AntennaConfiguration a_cfg = alist.get(0);
                    UnsignedShort channelIndex = a_cfg.getRFTransmitter().getChannelIndex();
                    UnsignedShort hopTableID = a_cfg.getRFTransmitter().getHopTableID();
                    //                    UnsignedShort p =  a_cfg.getRFTransmitter().getTransmitPower();
                    log.info("ChannelIndex " + channelIndex.toString() +
                            " hopTableID " + hopTableID.toString());
                } else {
                    log.error("Could not find antenna configuration");
                    System.exit(1);
                }
            } else {
                log.info("{} faild", msgType);
                System.exit(1);
            }
        } catch (TimeoutException ex) {
            log.error("Timeout waiting for response to {}", msgType, ex);
            System.exit(1);
        }
    }

    /**
     * 从XML文件构建LLRPMessage
     *
     * @param path
     * @return
     */
    private LLRPMessage buildMessageFromXML(String path) {
        LLRPMessage message = null;
        try {
            message = Util.loadXMLLLRPMessage(new File(path));
            return message;
        } catch (FileNotFoundException ex) {
            log.error("Could not find file");
        } catch (IOException ex) {
            log.error("IO Exception on file");
        } catch (JDOMException ex) {
            log.error("Unable to convert XML to DOM");
        } catch (InvalidLLRPMessageException ex) {
            log.error("Unable to convert XML to Internal Object");
        }
        return null;
    }

    /**
     * 进行Reader配置
     */
    private void setReaderConfiguration(String path) {
        LLRPMessage response;

        String msgType = "Set Reader Configuration";
        log.info("{} ...", msgType);

        try {
            LLRPMessage msg = this.buildMessageFromXML(path);
            if (msg == null || msg.getTypeNum() != SET_READER_CONFIG.TYPENUM) {
                log.error("Could not build LLRPMessage from XML: {}", path);
                System.exit(1);
            }
            SET_READER_CONFIG message = (SET_READER_CONFIG) msg;
            message.setMessageID(getUniqueMessageID());

            response = connection.transact(message, messageTimeout);

            StatusCode status = ((SET_READER_CONFIG_RESPONSE) response).getLLRPStatus().getStatusCode();
            this.logStatusCode(status, msgType);
        } catch (TimeoutException ex) {
            log.error("Timeout waiting for response to {}", msgType, ex);
            System.exit(1);
        }
    }

    /**
     * 添加RoSpec
     *
     * @param path
     */
    private void addRoSpec(String path) {
        LLRPMessage response;

        String msgType = "Add RoSpec";
        log.info("{} ...", msgType);
        try {
            LLRPMessage msg = this.buildMessageFromXML(path);
            if (msg == null || msg.getTypeNum() != ADD_ROSPEC.TYPENUM) {
                log.error("Could not build LLRPMessage from XML: {}", path);
                System.exit(1);
            }
            ADD_ROSPEC message = (ADD_ROSPEC) msg;
            message.setMessageID(getUniqueMessageID());

            rospec = message.getROSpec();

            response = connection.transact(message, messageTimeout);

            StatusCode status = ((ADD_ROSPEC_RESPONSE) response).getLLRPStatus().getStatusCode();
            this.logStatusCode(status, msgType);
        } catch (TimeoutException ex) {
            log.error("Timeout waiting for response to {}", msgType, ex);
            System.exit(1);
        }
    }

    /**
     * 启用RoSpec
     */
    private void enableRoSpec() {
        LLRPMessage response;

        String msgType = "Enable RoSpec";
        log.info("{} ...", msgType);

        try {
            ENABLE_ROSPEC message = new ENABLE_ROSPEC();
            message.setMessageID(getUniqueMessageID());
            message.setROSpecID(rospec.getROSpecID());

            response = connection.transact(message, messageTimeout);

            StatusCode status = ((ENABLE_ROSPEC_RESPONSE) response).getLLRPStatus().getStatusCode();
            this.logStatusCode(status, msgType);
        } catch (TimeoutException ex) {
            log.error("Timeout waiting for response to {}", msgType, ex);
            System.exit(1);
        }
    }

    /**
     * 禁用RoSpec
     */
    private void disableRoSpec() {
        LLRPMessage response;

        String msgType = "Disable RoSpec";
        log.info("{} ...", msgType);

        try {
            DISABLE_ROSPEC message = new DISABLE_ROSPEC();
            message.setMessageID(getUniqueMessageID());
            message.setROSpecID(rospec.getROSpecID());

            response = connection.transact(message, messageTimeout);

            StatusCode status = ((DISABLE_ROSPEC_RESPONSE) response).getLLRPStatus().getStatusCode();
            if (status.equals(new StatusCode("M_Success"))) {
                log.info("{} was successful", msgType);
            } else {
                log.warn("{} failed", msgType);
            }
        } catch (TimeoutException ex) {
            log.error("Timeout waiting for response to {}", msgType, ex);
            System.exit(1);
        }
    }


    /**
     * 启动RoSpec
     */
    private void startRoSpec() {
        LLRPMessage response;

        String msgType = "Start RoSpec";
        log.info("{} ...", msgType);

        try {
            START_ROSPEC message = new START_ROSPEC();
            message.setMessageID(getUniqueMessageID());
            message.setROSpecID(rospec.getROSpecID());

            response = connection.transact(message, messageTimeout);

            StatusCode status = ((START_ROSPEC_RESPONSE) response).getLLRPStatus().getStatusCode();
            this.logStatusCode(status, msgType);
        } catch (TimeoutException ex) {
            log.error("Timeout waiting for response to {}", msgType, ex);
            System.exit(1);
        }
    }

    /**
     * 停止RoSpec
     */
    private void stopRoSpec() {
        LLRPMessage response;

        String msgType = "Stop RoSpec";
        log.info("{} ...", msgType);

        try {
            STOP_ROSPEC message = new STOP_ROSPEC();
            message.setMessageID(getUniqueMessageID());
            message.setROSpecID(rospec.getROSpecID());

            response = connection.transact(message, messageTimeout);

            StatusCode status = ((STOP_ROSPEC_RESPONSE) response).getLLRPStatus().getStatusCode();
            this.logStatusCode(status, msgType);
        } catch (TimeoutException ex) {
            log.error("Timeout waiting for response to {}", msgType, ex);
            System.exit(1);
        }
    }
}
