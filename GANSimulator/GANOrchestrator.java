package edu.boun.edgecloudsim.applications.GANSimulator;

import edu.boun.edgecloudsim.cloud_server.CloudVM;
import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_client.CpuUtilizationModel_Custom;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.edge_client.mobile_processing_unit.MobileVM;
import edu.boun.edgecloudsim.edge_orchestrator.EdgeOrchestrator;
import edu.boun.edgecloudsim.edge_server.EdgeHost;
import edu.boun.edgecloudsim.edge_server.EdgeVM;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimUtils;

import java.util.List;

public class GANOrchestrator  extends EdgeOrchestrator {
    private int numberOfHost; //used by load balancer 负载均衡器使用该参数，主机的数量
    private int lastSelectedHostIndex; //used by load balancer 负载均衡器使用，上次选择的主机编号
    private int[] lastSelectedVmIndexes; //used by each host individually  每个主机单独使用，上次选择的虚拟机序号

    public GANOrchestrator(String _policy, String _simScenario) {
        super(_policy, _simScenario);
    }
    // 重构，初始化
    @Override
    public void initialize() {
        numberOfHost= SimSettings.getInstance().getNumOfEdgeHosts();  //获取host的数量

        lastSelectedHostIndex = -1;  //最近选择的主机号
        lastSelectedVmIndexes = new int[numberOfHost];  //初始化，让每个host都能存储最近选择的虚拟机编号
        for(int i=0; i<numberOfHost; i++)  //初始化index
            lastSelectedVmIndexes[i] = -1;
    }
    //获取一个Cloud 还是 Edge用于卸载
    @Override
    public int getDeviceToOffload(Task task) {
        int result = SimSettings.GENERIC_EDGE_DEVICE_ID;
        if(!policy.equals("Edge_Mobile")){ //如果不是边缘——设备，则使用云卸载
            result = SimSettings.CLOUD_DATACENTER_ID;
        }
        return result;
    }

    //选择一个虚拟机用于卸载
    @Override
    public org.cloudbus.cloudsim.Vm getVmToOffload(Task task, int deviceId) {
        org.cloudbus.cloudsim.Vm selectedVM = null;

        if(deviceId == SimSettings.CLOUD_DATACENTER_ID){ //不需要改
            //最小负载算法选择VM
            double selectedVmCapacity = 0; //start with min value
            List<org.cloudbus.cloudsim.Host> list = SimManager.getInstance().getCloudServerManager().getDatacenter().getHostList();
            //遍历主机列表
            for (int hostIndex=0; hostIndex < list.size(); hostIndex++) {
                List<CloudVM> vmArray = SimManager.getInstance().getCloudServerManager().getVmList(hostIndex);
                for(int vmIndex=0; vmIndex<vmArray.size(); vmIndex++){
                    double requiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(vmArray.get(vmIndex).getVmType());
                    double targetVmCapacity = (double)100 - vmArray.get(vmIndex).getCloudletScheduler().getTotalUtilizationOfCpu(org.cloudbus.cloudsim.core.CloudSim.clock());
                    if(requiredCapacity <= targetVmCapacity && targetVmCapacity > selectedVmCapacity){
                        selectedVM = vmArray.get(vmIndex);
                        selectedVmCapacity = targetVmCapacity;
                    }
                }
            }
        }
        else if(deviceId == SimSettings.MOBILE_DATACENTER_ID){
            List<MobileVM> vmArray = SimManager.getInstance().getMobileServerManager().getVmList(task.getMobileDeviceId());
            double requiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(vmArray.get(0).getVmType());
            double targetVmCapacity = (double) 100 - vmArray.get(0).getCloudletScheduler().getTotalUtilizationOfCpu(org.cloudbus.cloudsim.core.CloudSim.clock());
            if (requiredCapacity <= targetVmCapacity)
                selectedVM = vmArray.get(0);
        }
        else
            selectedVM = selectVmOnLoadBalancer(task);   //负载均衡器

        return selectedVM;
    }

    //负载均衡器的选择，选择与设备连接的基站进行卸载
    public EdgeVM selectVmOnLoadBalancer(Task task){
        EdgeVM selectedVM = null;
        //获取所连基站ID
        int baseStationId = task.getSubmittedLocation().getServingWlanId();
        //获取基站的虚拟机列表
        List<EdgeVM> vms = SimManager.getInstance().getEdgeServerManager().getDatacenterList().get(baseStationId).getHostList().get(0).getVmList();
        //选择空闲的分配
        int maxIndex = 0;
        //获取实时CPU利用率 = 实时上传数据大小 / 原始数据大小
        double ratio = task.getCloudletFileSize() / SimSettings.getInstance().getTaskLookUpTable()[task.getTaskType()][5];

        double originalCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(vms.get(0).getVmType());
        double requiredCapacity = ratio * originalCapacity;
        double maxVmCapacity = (double)100 - vms.get(0).getCloudletScheduler().getTotalUtilizationOfCpu(org.cloudbus.cloudsim.core.CloudSim.clock());
        for(int i = 1; i < vms.size(); i++){
           double targetVmCapacity = (double)100 - vms.get(i).getCloudletScheduler().getTotalUtilizationOfCpu(org.cloudbus.cloudsim.core.CloudSim.clock());
            if(maxVmCapacity < targetVmCapacity){
                maxVmCapacity = targetVmCapacity;
                maxIndex = i;
            }
        }
        //判断是否可以分配
        if(maxVmCapacity >= requiredCapacity){
            selectedVM = vms.get(maxIndex);
        }

        return selectedVM;
    }

    @Override
    public void processEvent(org.cloudbus.cloudsim.core.SimEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void shutdownEntity() {
        // TODO Auto-generated method stub

    }

    @Override
    public void startEntity() {
        // TODO Auto-generated method stub

    }
}
