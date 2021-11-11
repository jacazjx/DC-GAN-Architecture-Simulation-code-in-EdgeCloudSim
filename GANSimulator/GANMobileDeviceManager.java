package edu.boun.edgecloudsim.applications.GANSimulator;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_client.CpuUtilizationModel_Custom;
import edu.boun.edgecloudsim.edge_client.MobileDeviceManager;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.network.NetworkModel;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.TaskProperty;
import org.cloudbus.cloudsim.UtilizationModel;

public class GANMobileDeviceManager extends MobileDeviceManager {
    private static final int BASE = 100000; //start from base in order not to conflict cloudsim tag!
    private static final int REQUEST_RECEIVED_BY_CLOUD = BASE + 1;
    private static final int REQUEST_RECEIVED_BY_EDGE_DEVICE = BASE + 2;
    private static final int REQUEST_RECEIVED_BY_MOBILE_DEVICE = BASE + 3;
    private static final int RESPONSE_RECEIVED_BY_MOBILE_DEVICE = BASE + 4;
    private static final int SET_DELAY_LOG = BASE + 5;
    private int taskIdCounter=0;    //任务计数器，主要是为了 Cloudlet 的配置，避免任务的ID重复
    private String orchestratorPolicy;

    public GANMobileDeviceManager(String _orchestratorPolicy) throws Exception {
        orchestratorPolicy = _orchestratorPolicy;

    }

    @Override
    public void initialize() {
    }
/**
 * 整个代码的执行逻辑为
 * SimManager
 * → submitTask (upload) 发送信号量: 1. REQUEST_RECEIVED_BY_CLOUD
 *                                 2. REQUEST_RECEIVED_BY_DEVICE
 *                                 3. REQUEST_RECEIVED_BY_MOBILE_DEVICE
 * → processOtherEvent 接受信号量: 1. REQUEST_RECEIVED_BY_CLOUD     →   发送信号量: 1
 *                               2. REQUEST_RECEIVED_BY_EDGE_DEVICE   →   发送信号量: 1
 *                               3. REQUEST_RECEIVED_BY_MOBILE_DEVICE    →   发送信号量: 1
 *                               4. RESPONSE_RECEIVED_BY_MOBILE_DEVICE   结束任务
 *                     发送信号量: 1. CLOUDLET_SUBMIT
 * → processCloudletReturn 为信号量 CLOUDLET_RETURN 执行函数
 *                         发送信号量: 1. RESPONSE_RECEIVED_BY_MOBILE_DEVICE
 *
 */

    //处理任务的返回事件，即执行任务并返回信息
    @Override
    protected void processCloudletReturn(org.cloudbus.cloudsim.core.SimEvent ev){
        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();  //获取网络模块
        Task task = (Task) ev.getData();
        //记录Log信息
        SimLogger.getInstance().taskExecuted(task.getCloudletId());

        //获取延迟   源，目的，任务
        double Delay = 0;

        if(task.getAssociatedDatacenterId() == SimSettings.CLOUD_DATACENTER_ID){
            Delay = networkModel.getDownloadDelay(SimSettings.CLOUD_DATACENTER_ID , task.getMobileDeviceId(), task);
        }
        else if(task.getAssociatedDatacenterId() == SimSettings.MOBILE_DATACENTER_ID){
            schedule(getId(), 0, RESPONSE_RECEIVED_BY_MOBILE_DEVICE, task);
            return;
        }else{
            Delay = networkModel.getDownloadDelay(task.getAssociatedHostId() , task.getMobileDeviceId(), task);
        }
        if(Delay > 0){
            //获取接收到信息时设备的位置
            schedule(getId(), Delay, RESPONSE_RECEIVED_BY_MOBILE_DEVICE, task);

            if(task.getAssociatedDatacenterId() == SimSettings.CLOUD_DATACENTER_ID){
                SimLogger.getInstance().setDownloadDelay(task.getCloudletId(), Delay, SimSettings.NETWORK_DELAY_TYPES.WAN_DELAY);
            }else{
                SimLogger.getInstance().setDownloadDelay(task.getCloudletId(), Delay, SimSettings.NETWORK_DELAY_TYPES.WLAN_DELAY);
            }
        }else{
            if(task.getAssociatedDatacenterId() == SimSettings.CLOUD_DATACENTER_ID){
                SimLogger.getInstance().failedDueToBandwidth(task.getCloudletId(), org.cloudbus.cloudsim.core.CloudSim.clock(), SimSettings.NETWORK_DELAY_TYPES.WAN_DELAY);
            }else if(task.getAssociatedDatacenterId() == SimSettings.GENERIC_EDGE_DEVICE_ID){
                SimLogger.getInstance().failedDueToBandwidth(task.getCloudletId(), org.cloudbus.cloudsim.core.CloudSim.clock(), SimSettings.NETWORK_DELAY_TYPES.WLAN_DELAY);
            }
        }
    }

    //请求的过程
    @Override
    protected void processOtherEvent(org.cloudbus.cloudsim.core.SimEvent ev){
        //判空
        if (ev == null) {
            SimLogger.printLine(getName() + ".processOtherEvent(): " + "Error - an event is null! Terminating simulation...");
            System.exit(1);
            return;
        }
        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();

        //三种事件， 请求云，请求边缘，请求返回
        switch (ev.getTag()) {
            case REQUEST_RECEIVED_BY_CLOUD:
            {
                Task task = (Task) ev.getData();

                networkModel.uploadFinished(task.getSubmittedLocation(), SimSettings.CLOUD_DATACENTER_ID);

                submitTaskToVm(task,0,SimSettings.CLOUD_DATACENTER_ID);

                break;
            }
            case REQUEST_RECEIVED_BY_EDGE_DEVICE:
            {
                Task task = (Task) ev.getData();

                networkModel.uploadFinished(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID);

                submitTaskToVm(task, 0, SimSettings.GENERIC_EDGE_DEVICE_ID);

                break;
            }
            case REQUEST_RECEIVED_BY_MOBILE_DEVICE:
            {
                Task task = (Task) ev.getData();

                submitTaskToVm(task, 0, SimSettings.MOBILE_DATACENTER_ID);

                break;
            }
            case RESPONSE_RECEIVED_BY_MOBILE_DEVICE:
            {
                Task task = (Task) ev.getData();

                if(task.getAssociatedDatacenterId() == SimSettings.CLOUD_DATACENTER_ID)
                    networkModel.downloadFinished(task.getSubmittedLocation(), SimSettings.CLOUD_DATACENTER_ID);
                else if(task.getAssociatedDatacenterId() != SimSettings.MOBILE_DATACENTER_ID)
                    networkModel.downloadFinished(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID);

                SimLogger.getInstance().taskEnded(task.getCloudletId(), org.cloudbus.cloudsim.core.CloudSim.clock());
                break;
            }
            default:
                SimLogger.printLine(getName() + ".processOtherEvent(): " + "Error - event unknown by this DatacenterBroker. Terminating simulation...");
                System.exit(1);
                break;
        }
    }

    private void submitTaskToVm(Task task, double delay, int datacenterId) {
        //从协调器中选择一个虚拟机用于卸载
        org.cloudbus.cloudsim.Vm selectedVM = SimManager.getInstance().getEdgeOrchestrator().getVmToOffload(task, datacenterId);

        int vmType = 0;  //虚拟机类型
        if(datacenterId == SimSettings.CLOUD_DATACENTER_ID) {
            task.setVmType(SimSettings.VM_TYPES.CLOUD_VM);
            vmType = SimSettings.VM_TYPES.CLOUD_VM.ordinal();
        }
        else if(datacenterId == SimSettings.GENERIC_EDGE_DEVICE_ID){
            task.setVmType(SimSettings.VM_TYPES.EDGE_VM);
            vmType = SimSettings.VM_TYPES.EDGE_VM.ordinal();
        }else{
            task.setVmType(SimSettings.VM_TYPES.MOBILE_VM);
            vmType = SimSettings.VM_TYPES.MOBILE_VM.ordinal();
        }

        //把分配给该任务的信息填写进去
        if(selectedVM != null){
            if(datacenterId == SimSettings.GENERIC_EDGE_DEVICE_ID)
                task.setAssociatedDatacenterId(selectedVM.getHost().getDatacenter().getId());
            else
                task.setAssociatedDatacenterId(datacenterId);

            //设置相关主机ID
            task.setAssociatedHostId(selectedVM.getHost().getId());

            //设置相关 VM ID
            task.setAssociatedVmId(selectedVM.getId());

            //bind task to related VM  绑定任务与VM
            getCloudletList().add(task);
            bindCloudletToVm(task.getCloudletId(),selectedVM.getId());

            //提交事件给scheduler，CLoudletSubmit 提交任务
//            SimLogger.printLine(org.cloudbus.cloudsim.core.CloudSim.clock() + ": Cloudlet#" + task.getCloudletId() + " is submitted to VM#" + task.getVmId());
            schedule(getVmsToDatacentersMap().get(task.getVmId()), delay, org.cloudbus.cloudsim.core.CloudSimTags.CLOUDLET_SUBMIT, task);
            //Log  记录日志信息，分配 VM
            SimLogger.getInstance().taskAssigned(
                    task.getCloudletId(),
                    selectedVM.getHost().getDatacenter().getId(),
                    selectedVM.getHost().getId(),
                    selectedVM.getId(),
                    vmType
            );
        }
        else{
            //SimLogger.printLine("Task #" + task.getCloudletId() + " cannot assign to any VM");
            SimLogger.getInstance().rejectedDueToVMCapacity(task.getCloudletId(), org.cloudbus.cloudsim.core.CloudSim.clock(), vmType);
        }

    }


    @Override
    public UtilizationModel getCpuUtilizationModel() {
        return new CpuUtilizationModel_Custom();
    }

    //任务开始并上传的时候
    @Override
    public void submitTask(TaskProperty edgeTask) {
        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();  // 获取网络模型
        //根据任务信息创建任务
        Task task = createTask(edgeTask);
        Location currentLocation = SimManager.getInstance().getMobilityModel().
                getLocation(task.getMobileDeviceId(), org.cloudbus.cloudsim.core.CloudSim.clock());
        task.setSubmittedLocation(currentLocation);  //设置任务提交时的位置
        SimLogger.getInstance().addLog(task.getMobileDeviceId(),
                task.getCloudletId(),
                task.getTaskType(),
                (int)task.getCloudletLength(),
                (int)task.getCloudletFileSize(),
                (int)task.getCloudletOutputSize());
        // 通过协调者，选择一个 服务层 用于卸载
        int nextHopId = SimManager.getInstance().getEdgeOrchestrator().getDeviceToOffload(task);
        //根据不同的卸载目标，实现卸载功能
        double Delay = networkModel.getUploadDelay(task.getMobileDeviceId(),nextHopId,task); //先获取上传延迟
        networkModel.uploadStarted(currentLocation, nextHopId);  //执行上传过程
        if(Delay > 0){
            if(nextHopId == SimSettings.CLOUD_DATACENTER_ID){  //云
                //日志记录
                //---------------Log----------------
                //删除原任务
                SimLogger.getInstance().deleteTask(task.getCloudletId());
                //生成本地任务
                Task task_device = splitTask(task,SimSettings.VM_TYPES.MOBILE_VM);
                SimLogger.getInstance().addLog(task_device.getMobileDeviceId(),
                        task_device.getCloudletId(),
                        task_device.getTaskType(),
                        (int)task_device.getCloudletLength(),
                        (int)task_device.getCloudletFileSize(),
                        (int)task_device.getCloudletOutputSize());
                SimLogger.getInstance().setFID(task_device.getCloudletId(),task_device.getFID());
                //生成云任务
                Task cloud_edge = splitTask(task,SimSettings.VM_TYPES.CLOUD_VM);
                SimLogger.getInstance().addLog(cloud_edge.getMobileDeviceId(),
                        cloud_edge.getCloudletId(),
                        cloud_edge.getTaskType(),
                        (int)cloud_edge.getCloudletLength(),
                        (int)cloud_edge.getCloudletFileSize(),
                        (int)cloud_edge.getCloudletOutputSize());
                SimLogger.getInstance().setFID(cloud_edge.getCloudletId(),cloud_edge.getFID());
                //************************************************************
                //start in logger
                SimLogger.getInstance().taskStarted(task_device.getCloudletId(), org.cloudbus.cloudsim.core.CloudSim.clock());
                SimLogger.getInstance().taskStarted(cloud_edge.getCloudletId(), org.cloudbus.cloudsim.core.CloudSim.clock());

                SimLogger.getInstance().setUploadDelay(task_device.getCloudletId(), 0, SimSettings.NETWORK_DELAY_TYPES.WLAN_DELAY);
                SimLogger.getInstance().setUploadDelay(cloud_edge.getCloudletId(), Delay, SimSettings.NETWORK_DELAY_TYPES.WAN_DELAY);
                //schedule to run
                schedule(getId(), 0, REQUEST_RECEIVED_BY_MOBILE_DEVICE, task_device);
                schedule(getId(), Delay, REQUEST_RECEIVED_BY_CLOUD, cloud_edge);
            }
            else if(nextHopId == SimSettings.GENERIC_EDGE_DEVICE_ID){ //边缘
                //*************************拆分应用*****************************
                //删除原任务
                SimLogger.getInstance().deleteTask(task.getCloudletId());
                //生成本地任务
                Task task_device = splitTask(task,SimSettings.VM_TYPES.MOBILE_VM);
                SimLogger.getInstance().addLog(task_device.getMobileDeviceId(),
                        task_device.getCloudletId(),
                        task_device.getTaskType(),
                        (int)task_device.getCloudletLength(),
                        (int)task_device.getCloudletFileSize(),
                        (int)task_device.getCloudletOutputSize());
                SimLogger.getInstance().setFID(task_device.getCloudletId(),task_device.getFID());
                //生成边缘任务
                Task task_edge = splitTask(task,SimSettings.VM_TYPES.EDGE_VM);
                SimLogger.getInstance().addLog(task_edge.getMobileDeviceId(),
                        task_edge.getCloudletId(),
                        task_edge.getTaskType(),
                        (int)task_edge.getCloudletLength(),
                        (int)task_edge.getCloudletFileSize(),
                        (int)task_edge.getCloudletOutputSize());
                SimLogger.getInstance().setFID(task_edge.getCloudletId(),task_edge.getFID());
                //************************************************************
                //start in logger
                SimLogger.getInstance().taskStarted(task_device.getCloudletId(), org.cloudbus.cloudsim.core.CloudSim.clock());
                SimLogger.getInstance().taskStarted(task_edge.getCloudletId(), org.cloudbus.cloudsim.core.CloudSim.clock());

                SimLogger.getInstance().setUploadDelay(task_device.getCloudletId(), 0, SimSettings.NETWORK_DELAY_TYPES.WLAN_DELAY);
                SimLogger.getInstance().setUploadDelay(task_edge.getCloudletId(), Delay, SimSettings.NETWORK_DELAY_TYPES.WLAN_DELAY);
                //schedule to run
                schedule(getId(), 0, REQUEST_RECEIVED_BY_MOBILE_DEVICE, task_device);
                schedule(getId(), Delay, REQUEST_RECEIVED_BY_EDGE_DEVICE, task_edge);
            }else{
                SimLogger.printLine("Unknown nextHopId! Terminating simulation...");
                System.exit(1);
            }
        }else{
            SimLogger.getInstance().
                    rejectedDueToBandwidth(
                            task.getCloudletId(),     //任务编号
                            org.cloudbus.cloudsim.core.CloudSim.clock(), //仿真时间
                            SimSettings.VM_TYPES.CLOUD_VM.ordinal(),    //
                            SimSettings.NETWORK_DELAY_TYPES.WAN_DELAY
                    );
        }
    }

    // 通过 TaskProperty 创建 Task
    private Task createTask(TaskProperty edgeTask){
        UtilizationModel utilizationModel = new org.cloudbus.cloudsim.UtilizationModelFull(); /*UtilizationModelStochastic*/ //静态利用率
        UtilizationModel utilizationModelCPU = getCpuUtilizationModel();       //CPU利用率，TODO 算法重构，里面的实现是直接返回应用配置文件的信息
        //创建Task
        Task task = new Task(
                edgeTask.getMobileDeviceId(),   //执行该任务的设备ID 云/边缘
                ++taskIdCounter,                //任务编号
                edgeTask.getLength(),           //任务长度
                edgeTask.getPesNumber(),        //任务所需的CPU核心数
                edgeTask.getInputFileSize(),    //任务输入大小
                edgeTask.getOutputFileSize(),   //输出大小
                utilizationModelCPU,            //CPU利用率模型
                utilizationModel,               //Ram
                utilizationModel                //Bw
        );

        //set the owner of this task  设置该任务的所有者
        task.setUserId(this.getId());
        task.setTaskType(edgeTask.getTaskType());
        //CPU模型算法必须是 utilizationModelCPU 类的子类
        if (utilizationModelCPU instanceof CpuUtilizationModel_Custom) {
            ((CpuUtilizationModel_Custom)utilizationModelCPU).setTask(task);
        }
        return task;
    }

    private Task splitTask(Task edgeTask,SimSettings.VM_TYPES deviceType){
        UtilizationModel utilizationModel = new org.cloudbus.cloudsim.UtilizationModelFull(); /*UtilizationModelStochastic*/ //静态利用率
        UtilizationModel utilizationModelCPU = getCpuUtilizationModel();       //CPU利用率，TODO 算法重构，里面的实现是直接返回应用配置文件的信息
        int flag = deviceType == SimSettings.VM_TYPES.MOBILE_VM ? 0 : 1;
        double ratio = 1.0;
        if(orchestratorPolicy.equals("GD-GAN") || orchestratorPolicy.equals("MD-GAN")){
            ratio = deviceType == SimSettings.VM_TYPES.MOBILE_VM ? 0.3 : 0.7;
        }

        //创建Task
        Task task = new Task(
                edgeTask.getMobileDeviceId(),   //执行该任务的设备ID 云/边缘
                ++taskIdCounter,                //任务编号
                (long) (edgeTask.getCloudletLength() * ratio),   //任务长度
                edgeTask.getNumberOfPes(),      //任务所需的CPU核心数
                edgeTask.getCloudletFileSize() * flag, //任务输入大小
                edgeTask.getCloudletOutputSize() * flag,//输出大小
                utilizationModelCPU,            //CPU利用率模型
                utilizationModel,               //Ram
                utilizationModel                //Bw
        );

        //set the owner of this task  设置该任务的所有者
        task.setFID(edgeTask.getCloudletId());
        task.setUserId(this.getId());
        task.setTaskType(edgeTask.getTaskType());
        task.setSubmittedLocation(edgeTask.getSubmittedLocation());
        //CPU模型算法必须是 utilizationModelCPU 类的子类
        if (utilizationModelCPU instanceof CpuUtilizationModel_Custom) {
            ((CpuUtilizationModel_Custom)utilizationModelCPU).setTask(task);
        }
        return task;
    }
}
