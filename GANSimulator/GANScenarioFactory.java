package edu.boun.edgecloudsim.applications.GANSimulator;

import edu.boun.edgecloudsim.cloud_server.CloudServerManager;
import edu.boun.edgecloudsim.cloud_server.DefaultCloudServerManager;
import edu.boun.edgecloudsim.core.ScenarioFactory;
import edu.boun.edgecloudsim.edge_client.DefaultMobileDeviceManager;
import edu.boun.edgecloudsim.edge_client.MobileDeviceManager;
import edu.boun.edgecloudsim.edge_client.mobile_processing_unit.DefaultMobileServerManager;
import edu.boun.edgecloudsim.edge_client.mobile_processing_unit.MobileServerManager;
import edu.boun.edgecloudsim.edge_orchestrator.BasicEdgeOrchestrator;
import edu.boun.edgecloudsim.edge_orchestrator.EdgeOrchestrator;
import edu.boun.edgecloudsim.edge_server.DefaultEdgeServerManager;
import edu.boun.edgecloudsim.edge_server.EdgeServerManager;
import edu.boun.edgecloudsim.mobility.MobilityModel;
import edu.boun.edgecloudsim.mobility.NomadicMobility;
import edu.boun.edgecloudsim.network.MM1Queue;
import edu.boun.edgecloudsim.network.NetworkModel;
import edu.boun.edgecloudsim.task_generator.LoadGeneratorModel;

public class GANScenarioFactory implements ScenarioFactory {

    private int numOfMobileDevice;		 // 设备数量
    private double simulationTime;		 // 仿真时间（总）
    private String orchestratorPolicy;   // 协调者策略
    private String simScenario;          // 仿真场景

    GANScenarioFactory(int _numOfMobileDevice,
                          double _simulationTime,
                          String _orchestratorPolicy,
                          String _simScenario){
        orchestratorPolicy = _orchestratorPolicy;
        numOfMobileDevice = _numOfMobileDevice;
        simulationTime = _simulationTime;
        simScenario = _simScenario;
    }
    @Override
    public LoadGeneratorModel getLoadGeneratorModel() {
        return new GANLoadGenerator(numOfMobileDevice, simulationTime, simScenario, orchestratorPolicy);
    }

    @Override
    public EdgeOrchestrator getEdgeOrchestrator() {
        return new GANOrchestrator(orchestratorPolicy, simScenario);
    }

    @Override
    public MobilityModel getMobilityModel() {
        return new GANMobilityModel(numOfMobileDevice,simulationTime);
    }

    @Override
    public NetworkModel getNetworkModel() {
        return new GANNetworkModel(numOfMobileDevice, simScenario);
    }

    @Override
    public EdgeServerManager getEdgeServerManager() {
        return new DefaultEdgeServerManager();
    }

    @Override
    public CloudServerManager getCloudServerManager() {
        return new DefaultCloudServerManager();
    }

    @Override
    public MobileDeviceManager getMobileDeviceManager() throws Exception {
        return new GANMobileDeviceManager(orchestratorPolicy);
    }

    @Override
    public MobileServerManager getMobileServerManager() {
        return new GANMobileServerManager(numOfMobileDevice);
    }
}
