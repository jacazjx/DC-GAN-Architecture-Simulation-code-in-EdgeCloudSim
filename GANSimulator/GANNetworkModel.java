package edu.boun.edgecloudsim.applications.GANSimulator;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.edge_server.EdgeHost;
import edu.boun.edgecloudsim.network.NetworkModel;
import edu.boun.edgecloudsim.utils.Location;
import org.apache.commons.math3.distribution.ExponentialDistribution;

public class GANNetworkModel extends NetworkModel {
    private int maxNumOfClientsInPlace;
    private Task currentTask;
    private int WLANBandWidth;
    private int WANBandWidth;
    public GANNetworkModel(int _numberOfMobileDevices, String _simScenario) {
        super(_numberOfMobileDevices, _simScenario);
    }


    @Override
    public void initialize() {
        maxNumOfClientsInPlace= SimSettings.getInstance().getGsmBandwidth() ;	//区域最大客户端数量
        currentTask = null;
    }

    /**
     * source device is always mobile device in our simulation scenarios!
     * 获取上传传输延迟
     * sourceDeviceId  发送源
     * destDeviceId 接收源
     * task 任务
     */
    @Override
    public double getUploadDelay(int sourceDeviceId, int destDeviceId, Task task) {
        currentTask = task;
        double delay = 0;
        Location accessPointLocation = SimManager.getInstance().getMobilityModel().getLocation(sourceDeviceId, org.cloudbus.cloudsim.core.CloudSim.clock());
        //mobile device to cloud server    总延迟 = 局域网延迟 + 广域网延迟     云中心
        if(destDeviceId == SimSettings.CLOUD_DATACENTER_ID){
            double wlanDelay = getWlanUploadDelay(accessPointLocation, org.cloudbus.cloudsim.core.CloudSim.clock());
            double wanDelay = getWanUploadDelay(accessPointLocation, org.cloudbus.cloudsim.core.CloudSim.clock() + wlanDelay);
            if(wlanDelay > 0 && wanDelay >0)
                delay = wlanDelay + wanDelay;
        }
        //mobile device to edge orchestrator   总延迟 =  局域网
        else if(destDeviceId == SimSettings.EDGE_ORCHESTRATOR_ID){
            delay = getWlanUploadDelay(accessPointLocation, org.cloudbus.cloudsim.core.CloudSim.clock()) +
                    SimSettings.getInstance().getInternalLanDelay();
        }
        //mobile device to edge device (wifi access point)
        else if (destDeviceId == SimSettings.GENERIC_EDGE_DEVICE_ID) {
            delay = getWlanUploadDelay(accessPointLocation, org.cloudbus.cloudsim.core.CloudSim.clock());
        }

        return delay;
    }

    /**
     * destination device is always mobile device in our simulation scenarios!
     */
    @Override
    public double getDownloadDelay(int sourceDeviceId, int destDeviceId, Task task) {
        currentTask = task;
        //Special Case -> edge orchestrator to edge device   特殊选项：从 边缘协调器 到 边缘设备，直接返回局域网时延（有线局域网）
        if(sourceDeviceId == SimSettings.EDGE_ORCHESTRATOR_ID &&
                destDeviceId == SimSettings.GENERIC_EDGE_DEVICE_ID){
            return SimSettings.getInstance().getInternalLanDelay();
        }

        double delay = 0;
        Location accessPointLocation = SimManager.getInstance().getMobilityModel().getLocation(destDeviceId, org.cloudbus.cloudsim.core.CloudSim.clock());

        //cloud server to mobile device 从 云服务器 到 移动设备（无线局域网 + 广域网）
        if(sourceDeviceId == SimSettings.CLOUD_DATACENTER_ID){
            double wlanDelay = getWlanDownloadDelay(accessPointLocation, org.cloudbus.cloudsim.core.CloudSim.clock());
            double wanDelay = getWanDownloadDelay(accessPointLocation, org.cloudbus.cloudsim.core.CloudSim.clock() + wlanDelay);
            if(wlanDelay > 0 && wanDelay >0)
                delay = wlanDelay + wanDelay;
        }
        //edge device (wifi access point) to mobile device   边缘设备 到 移动设备 （无线局域网）
        else{
            //获取无线局域网的延迟
            delay = getWlanDownloadDelay(accessPointLocation, org.cloudbus.cloudsim.core.CloudSim.clock());

            EdgeHost host = (EdgeHost)(SimManager.
                    getInstance().
                    getEdgeServerManager().
                    getDatacenterList().get(sourceDeviceId).
                    getHostList().get(0));

            //if source device id is the edge server which is located in another location, add internal lan delay
            //in our scenario, serving wlan ID is equal to the host id, because there is only one host in one place
            if(host.getLocation().getServingWlanId() != accessPointLocation.getServingWlanId())
                delay += (SimSettings.getInstance().getInternalLanDelay() * 2);
        }

        return delay;
    }

    public int getMaxNumOfClientsInPlace(){
        return maxNumOfClientsInPlace;
    }

    //获取设备数量
    private int getDeviceCount(Location deviceLocation, double time){
        int deviceCount = 0;
        //找到同一个区域的设备数
        for(int i=0; i<numberOfMobileDevices; i++) {
            Location location = SimManager.getInstance().getMobilityModel().getLocation(i,time);
            if(location.equals(deviceLocation))
                deviceCount++;
        }

        //record max number of client just for debugging    防止过大
        if(maxNumOfClientsInPlace<deviceCount)
            maxNumOfClientsInPlace = deviceCount;

        return deviceCount;
    }
    //单位 ms
    private double calculateMM1(double propagationDelay, int bandwidth /*Kbps*/, double TaskSize /*KB*/, int deviceCount){
        double Bps=0;
        //排队时延+处理时延
        double QueueDelay = deviceCount * 0.01;
        //平均任务长度  把KB转换成byte，方便计算
        TaskSize = TaskSize * 1000.0; //convert from KB to Byte
        //把带宽Kps转成Bps   1Byte = 8 bit   ->   1Kbps = 1000 / 8 Byteps 这样的好处是方便和任务的长度进行换算
        Bps = bandwidth * 1000.0 / 8.0; //convert from Kbps to Byte per seconds
        //发送时延
        double SendDelay = TaskSize / Bps;
        propagationDelay = new ExponentialDistribution(propagationDelay).sample(); //传播时延取泊松期望
        //总时延=发送时延+传播时延+处理时延+排队时延
        double delay = SendDelay + propagationDelay + QueueDelay;
        if (delay > 8)
            return 0;
        else
            return delay;
    }

    private double getWlanDownloadDelay(Location accessPointLocation, double time) {
        return calculateMM1(
                SimSettings.getInstance().getGsmPropagationDelay(),
                SimSettings.getInstance().getGsmBandwidth(),
                currentTask.getCloudletOutputSize(),
                getDeviceCount(accessPointLocation, time));
    }

    private double getWlanUploadDelay(Location accessPointLocation, double time) {
        return calculateMM1(
                SimSettings.getInstance().getGsmPropagationDelay(),
                SimSettings.getInstance().getGsmBandwidth(),
                currentTask.getCloudletFileSize(),
                getDeviceCount(accessPointLocation, time));
    }

    private double getWanDownloadDelay(Location accessPointLocation, double time) {
        return calculateMM1(
                SimSettings.getInstance().getWanPropagationDelay(),
                SimSettings.getInstance().getWanBandwidth(),
                currentTask.getCloudletOutputSize(),
                getDeviceCount(accessPointLocation, time));
    }

    private double getWanUploadDelay(Location accessPointLocation, double time) {
        return calculateMM1(
                SimSettings.getInstance().getWanPropagationDelay(),
                SimSettings.getInstance().getWanBandwidth(),
                currentTask.getCloudletFileSize(),
                getDeviceCount(accessPointLocation, time));
    }

    @Override
    public void uploadStarted(Location accessPointLocation, int destDeviceId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void uploadFinished(Location accessPointLocation, int destDeviceId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void downloadStarted(Location accessPointLocation, int sourceDeviceId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void downloadFinished(Location accessPointLocation, int sourceDeviceId) {
        // TODO Auto-generated method stub

    }
}
