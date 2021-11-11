package edu.boun.edgecloudsim.applications.GANSimulator;

import edu.boun.edgecloudsim.core.ScenarioFactory;
import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class GANMainAppForMDGAN {

    public static void main(String[] args) {
        Log.disable();  //关闭控制台输出日志，避免干扰仿真的日志输出
        SimLogger.enablePrintLog(); //启用仿真的日志输出
        int iterationNumber = 3;
        SimLogger.printLine("Simulation setting file, output folder and iteration number are not provided! Using default ones...");
        String configFile = "scripts/GANSimulator/config/config.properties";
        String applicationsFile = "scripts/GANSimulator/config/GAN_config.xml";
        String edgeDevicesFile = "scripts/GANSimulator/config/edge_devices.xml";
        String outputFolder = "sim_results/GAN_sim" + iterationNumber;

        //load settings from configuration file
        //加载配置文件，读取参数  SS为配置文件实体
        SimSettings SS = SimSettings.getInstance();  //获取实例
        if(SS.initialize(configFile, edgeDevicesFile, applicationsFile) == false){
            SimLogger.printLine("cannot initialize simulation settings!");
            System.exit(0);
        }
        // 本地日志开关，若允许日志则初始化
        if(SS.getFileLoggingEnabled()){
            SimLogger.enableFileLog();
            SimUtils.cleanOutputFolder(outputFolder);    //清理旧文件夹
        }

        DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date SimulationStartDate = Calendar.getInstance().getTime();
        String now = df.format(SimulationStartDate);
        SimLogger.printLine("Simulation started at " + now);
        SimLogger.printLine("----------------------------------------------------------------------");

        // 循环初始值为设备数量的最小值，结束值为最大值，累加值为counter size  实际上的循环次数为（MAX-MIN）/ Size，为测试次数
        // 本实验模拟1000~10000，每次累加值1000
        for(int j=SS.getMinNumOfMobileDev(); j<=SS.getMaxNumOfMobileDev(); j+=SS.getMobileDevCounterSize())
        {
            //场景的数量 1
            for(int k=0; k<SS.getSimulationScenarios().length; k++)
            {
                String simScenario = SS.getSimulationScenarios()[k];           // 定义当前仿真场景
                String orchestratorPolicy = SS.getOrchestratorPolicies()[1];   // 定义当前仿真策略
                Date ScenarioStartDate = Calendar.getInstance().getTime();
                now = df.format(ScenarioStartDate);  // 仿真启动时间记录
                // 仿真参数  simScenario仿真场景
                SimLogger.printLine("Scenario started at " + now);
                SimLogger.printLine("Scenario: " + simScenario + " - Policy: " + orchestratorPolicy + " - #iteration: " + iterationNumber);
                SimLogger.printLine("Duration: " + SS.getSimulationTime()/3600 + " hour(s) - Poisson: " + SS.getTaskLookUpTable()[0][2] + " - #devices: " + j);
                SimLogger.getInstance().simStarted(outputFolder,"SIMRESULT_" + simScenario + "_"  + orchestratorPolicy + "_" + j + "DEVICES");

                try
                {
                    // 初始化CloudSim仿真库
                    int num_user = 2;   // number of grid users   TODO 网格用户数量 ?
                    Calendar calendar = Calendar.getInstance();  // 获取日期
                    boolean trace_flag = false;  // mean trace events 追踪标志位

                    // Initialize the CloudSim library   初始化云仿真库
                    CloudSim.init(num_user, calendar, trace_flag, 0.01);

                    // Generate EdgeCloudsim Scenario Factory     边缘场景初始化
                    ScenarioFactory sampleFactory = new GANScenarioFactory(j,SS.getSimulationTime(), orchestratorPolicy, simScenario);

                    // Generate EdgeCloudSim Simulation Manager     边缘仿真管理器初始化
                    SimManager manager = new SimManager(sampleFactory, j, simScenario, orchestratorPolicy);  // orchestratorPolicy在sampleFactory中已经定义以此，为何再次传入

                    // Start simulation
                    manager.startSimulation();
                }
                catch (Exception e)
                {
                    SimLogger.printLine("The simulation has been terminated due to an unexpected error");
                    e.printStackTrace();
                    System.exit(0);
                }

                Date ScenarioEndDate = Calendar.getInstance().getTime();
                now = df.format(ScenarioEndDate);
                SimLogger.printLine("Scenario finished at " + now +  ". It took " + SimUtils.getTimeDifference(ScenarioStartDate,ScenarioEndDate));
                SimLogger.printLine("----------------------------------------------------------------------");
            }//End of scenarios loop
        }//End of mobile devices loop

        Date SimulationEndDate = Calendar.getInstance().getTime();
        now = df.format(SimulationEndDate);
        SimLogger.printLine("Simulation finished at " + now +  ". It took " + SimUtils.getTimeDifference(SimulationStartDate,SimulationEndDate));
    }
}
