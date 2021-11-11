package edu.boun.edgecloudsim.applications.GANSimulator;


import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.task_generator.LoadGeneratorModel;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;
import edu.boun.edgecloudsim.utils.TaskProperty;
import org.apache.commons.math3.distribution.ExponentialDistribution;

import java.util.ArrayList;

public class GANLoadGenerator extends LoadGeneratorModel {
    int taskTypeOfDevices[];   // 任务分配表，里面存放了设备i所被分配的任务类型编号
    String policy;

    // 本实验的任务分为只有两种，一种是可卸载到边缘的GAN，一种是只在云端使用的GAN
    public GANLoadGenerator(int _numberOfMobileDevices, double _simulationTime, String _simScenario,String _orchestratorPolicy) {
        super(_numberOfMobileDevices, _simulationTime, _simScenario);
        policy = _orchestratorPolicy;
    }

    @Override
    public void initializeModel() {
        taskList = new ArrayList<TaskProperty>();   // 任务列表初始化
        double ratio = 1;
        //exponential number generator for file input size, file output size and task length   指数生成器，用于控制文件输入输出大小，和任务长度   二维【应用种类数量】【3】
        ExponentialDistribution[][] expRngList = new ExponentialDistribution[SimSettings.getInstance().getTaskLookUpTable().length][3];

        //create random number generator for each place  为每个应用创建随机数
        for(int i=0; i<SimSettings.getInstance().getTaskLookUpTable().length; i++) {
            if(SimSettings.getInstance().getTaskLookUpTable()[i][0] ==0)   // TaskLookUpTable[0] 为 使用率 ，这里是为了跳过使用率为0的软件，实际上没有意义，使用率为0的软件可以不在xml中声明
                continue;
            if(policy.equals("FeGAN") != true){
                ratio = 0.1;
            }
            expRngList[i][0] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][5]  *  ratio);   //平均上传数据大小
            expRngList[i][1] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][6]);   //平均下载数据大小
            expRngList[i][2] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][7]);   //平均任务大小
        }

        //Each mobile device utilizes an app type (task type)  为每个设备分配一个应用
        taskTypeOfDevices = new int[numberOfMobileDevices];
        for(int i=0; i<numberOfMobileDevices; i++) {
            int randomTaskType = -1;
            double taskTypeSelector = SimUtils.getRandomDoubleNumber(0,100);    //获得一个随机数
            double taskTypePercentage = 0;
            //遍历所有类型的任务，比较使用率和随机数的大小，分配第一个使用率大于随机数的应用给设备，使用率随着遍历的应用使用率增大，直到100。注，一定会有应用被分配。
            for (int j=0; j<SimSettings.getInstance().getTaskLookUpTable().length; j++) {
                taskTypePercentage += SimSettings.getInstance().getTaskLookUpTable()[j][0];
                if(taskTypeSelector <= taskTypePercentage){
                    randomTaskType = j;   ///
                    break;
                }
            }
            if(randomTaskType == -1){
                SimLogger.printLine("Impossible is occurred! no random task type!");
                continue;
            }
            // 将选择的任务分配给设备，放入设备分配表
            taskTypeOfDevices[i] = randomTaskType;
            //获取其他参数
            double poissonMean = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][2];   //泊松均值
            double activePeriod = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][3];  //活动期
            double idlePeriod = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][4];    //空闲期
            double activePeriodStartTime = SimUtils.getRandomDoubleNumber(
                    SimSettings.CLIENT_ACTIVITY_START_TIME,
                    SimSettings.CLIENT_ACTIVITY_START_TIME + activePeriod);
            //active period starts shortly after the simulation started (e.g. 10 seconds)以随机值设置用户使用该软件的活跃期起始时间
            double virtualTime = activePeriodStartTime;  //虚拟时间？
            ExponentialDistribution rng = new ExponentialDistribution(poissonMean);
            //当 虚拟时间 < 仿真时间 时，该仿真时间为整个仿真时间，即当仿真时间未结束时，这里是为了模拟运行情况
            while(virtualTime < simulationTime) {
                double interval = rng.sample();    //根据泊松分布获取一个间隔值

                if(interval <= 0){
                    SimLogger.printLine("Impossible is occurred! interval is " + interval + " for device " + i + " time " + virtualTime);
                    continue;
                }
                //SimLogger.printLine(virtualTime + " -> " + interval + " for device " + i + " time ");
                // 虚拟时钟增加间隔时间
                virtualTime += interval;
                //如果虚拟时钟大于有效期 ，说明虚拟时钟跑快了，调整时间与
                if(virtualTime > activePeriodStartTime + activePeriod){
                    activePeriodStartTime = activePeriodStartTime + activePeriod + idlePeriod;
                    virtualTime = activePeriodStartTime;
                    continue;
                }
                //生成任务属性，并存入任务列表，属性包括设备ID，任务类型编号，虚拟时间，任务指数分布图
                taskList.add(new TaskProperty(i,randomTaskType, virtualTime, expRngList));

            }
        }

    }

    @Override
    public int getTaskTypeOfDevice(int deviceId) {
        return taskTypeOfDevices[deviceId];
    }
}
