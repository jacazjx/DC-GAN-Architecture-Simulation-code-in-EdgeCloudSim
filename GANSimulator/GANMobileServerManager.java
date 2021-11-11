package edu.boun.edgecloudsim.applications.GANSimulator;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_client.mobile_processing_unit.MobileHost;
import edu.boun.edgecloudsim.edge_client.mobile_processing_unit.MobileServerManager;
import edu.boun.edgecloudsim.edge_client.mobile_processing_unit.MobileVM;
import edu.boun.edgecloudsim.edge_client.mobile_processing_unit.MobileVmAllocationPolicy_Custom;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.VmAllocationPolicy;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class GANMobileServerManager extends MobileServerManager {
    private int numOfMobileDevices=0;   // 设备数

    public GANMobileServerManager(int _numOfMobileDevices) {
        numOfMobileDevices=_numOfMobileDevices;
    }

    @Override
    public void initialize() {

    }

    @Override
    public VmAllocationPolicy getVmAllocationPolicy(List<? extends Host> list, int dataCenterIndex) {
        return new MobileVmAllocationPolicy_Custom(list, dataCenterIndex);
    }

    @Override
    public void startDatacenters() throws Exception {
        //in the initial version, each mobile device has a separate datacenter
        //however, this approach encounters with out of memory (oom) problem.
        //therefore, we use single datacenter for all mobile devices!
        localDatacenter = createDatacenter(SimSettings.MOBILE_DATACENTER_ID);
    }

    @Override
    public void terminateDatacenters() {
        localDatacenter.shutdownEntity();
    }

    @Override
    public void createVmList(int brokerId) {
        //VMs should have unique IDs, so create Mobile VMs after Edge+Cloud VMs
        int vmCounter= SimSettings.getInstance().getNumOfEdgeVMs() + SimSettings.getInstance().getNumOfCloudVMs();

        //Create VMs for each hosts
        //Note that each mobile device has one host with one VM!
        for (int i = 0; i < numOfMobileDevices; i++) {
            vmList.add(i, new ArrayList<MobileVM>());

            String vmm = "Xen";
            int numOfCores = SimSettings.getInstance().getCoreForMobileVM();
            double mips = SimSettings.getInstance().getMipsForMobileVM();
            int ram = SimSettings.getInstance().getRamForMobileVM();
            long storage = SimSettings.getInstance().getStorageForMobileVM();
            long bandwidth = 0;

            //VM Parameters
            MobileVM vm = new MobileVM(vmCounter, brokerId, mips, numOfCores, ram, bandwidth, storage, vmm, new org.cloudbus.cloudsim.CloudletSchedulerTimeShared());
            vmList.get(i).add(vm);
            vmCounter++;
        }
    }
    //获取平均利用率
    @Override
    public double getAvgUtilization() {
        double totalUtilization = 0;    //全部利用率
        double vmCounter = 0;           //虚拟机编号

        List<? extends Host> list = localDatacenter.getHostList(); //获取主机列表
        // for each host...
        for (int hostIndex=0; hostIndex < list.size(); hostIndex++) {
            List<MobileVM> vmArray = SimManager.getInstance().getMobileServerManager().getVmList(hostIndex);
            //for each vm...
            for(int vmIndex=0; vmIndex<vmArray.size(); vmIndex++){
                totalUtilization += vmArray.get(vmIndex).getCloudletScheduler().getTotalUtilizationOfCpu(org.cloudbus.cloudsim.core.CloudSim.clock());
                vmCounter++;
            }
        }
        //所有虚拟机的负载的平均值
        return totalUtilization / vmCounter;
    }

    //创建数据中心
    private org.cloudbus.cloudsim.Datacenter createDatacenter(int index) throws Exception{
        String arch = "ARM";
        String os = "Harmony OS";
        String vmm = "Xen";
        double costPerBw = 0;
        double costPerSec = 0;
        double costPerMem = 0;
        double costPerStorage = 0;

        List<MobileHost> hostList=createHosts();

        String name = "MobileDatacenter_" + Integer.toString(index);
        double time_zone = 3.0;         // time zone this resource located
        LinkedList<org.cloudbus.cloudsim.Storage> storageList = new LinkedList<org.cloudbus.cloudsim.Storage>();	//we are not adding SAN devices by now

        // 5. Create a DatacenterCharacteristics object that stores the
        //    properties of a data center: architecture, OS, list of
        //    Machines, allocation policy: time- or space-shared, time zone
        //    and its price (G$/Pe time unit).
        org.cloudbus.cloudsim.DatacenterCharacteristics characteristics = new org.cloudbus.cloudsim.DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, costPerSec, costPerMem, costPerStorage, costPerBw);

        // 6. Finally, we need to create a PowerDatacenter object.
        org.cloudbus.cloudsim.Datacenter datacenter = null;

        VmAllocationPolicy vm_policy = getVmAllocationPolicy(hostList,index);
        datacenter = new org.cloudbus.cloudsim.Datacenter(name, characteristics, vm_policy, storageList, 0);

        return datacenter;
    }

    private List<MobileHost> createHosts(){
        // Here are the steps needed to create a PowerDatacenter:
        // 1. We need to create a list to store one or more Machines
        List<MobileHost> hostList = new ArrayList<MobileHost>();

        for (int i = 0; i < numOfMobileDevices; i++) {

            int numOfCores = SimSettings.getInstance().getCoreForMobileVM();
            double mips = SimSettings.getInstance().getMipsForMobileVM();
            int ram = SimSettings.getInstance().getRamForMobileVM();
            long storage = SimSettings.getInstance().getStorageForMobileVM();
            long bandwidth = 0;

            // 2. A Machine contains one or more PEs or CPUs/Cores. Therefore, should
            //    create a list to store these PEs before creating
            //    a Machine.
            List<org.cloudbus.cloudsim.Pe> peList = new ArrayList<org.cloudbus.cloudsim.Pe>();

            // 3. Create PEs and add these into the list.
            //for a quad-core machine, a list of 4 PEs is required:
            for(int j=0; j<numOfCores; j++){
                peList.add(new org.cloudbus.cloudsim.Pe(j, new org.cloudbus.cloudsim.provisioners.PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating
            }

            //4. Create Hosts with its id and list of PEs and add them to the list of machines
            MobileHost host = new MobileHost(
                    //Hosts should have unique IDs, so create Mobile Hosts after Edge+Cloud Hosts
                    i+SimSettings.getInstance().getNumOfEdgeHosts()+SimSettings.getInstance().getNumOfCloudHost(),
                    new org.cloudbus.cloudsim.provisioners.RamProvisionerSimple(ram),
                    new org.cloudbus.cloudsim.provisioners.BwProvisionerSimple(bandwidth), //kbps
                    storage,
                    peList,
                    new org.cloudbus.cloudsim.VmSchedulerSpaceShared(peList)
            );

            host.setMobileDeviceId(i);
            hostList.add(host);
        }

        return hostList;
    }
}
