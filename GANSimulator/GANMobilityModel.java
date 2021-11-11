package edu.boun.edgecloudsim.applications.GANSimulator;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.mobility.MobilityModel;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class GANMobilityModel extends MobilityModel {
    private List<TreeMap<Double, Location>> treeMapArray;

    public GANMobilityModel(int _numberOfMobileDevices, double _simulationTime) {
        super(_numberOfMobileDevices, _simulationTime);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void initialize() {
        /**
         * TreeMap是一个常用的集合对象，会对传入的值进行排序
         */
        treeMapArray = new ArrayList<TreeMap<Double, Location>>();

        //按照数据中心的数量创建
        ExponentialDistribution[] expRngList = new ExponentialDistribution[SimSettings.getInstance().getNumOfEdgeDatacenters()];

        //create random number generator for each place   为每个地点创建随机数生成器
        /**
         * 这里使用了 XML DOM 来读取 XML配置文件的信息
         */
        Document doc = SimSettings.getInstance().getEdgeDevicesDocument();
        NodeList datacenterList = doc.getElementsByTagName("datacenter");
        for (int i = 0; i < datacenterList.getLength(); i++) {
            Node datacenterNode = datacenterList.item(i);
            Element datacenterElement = (Element) datacenterNode;
            Element location = (Element)datacenterElement.getElementsByTagName("location").item(0);
            String attractiveness = location.getElementsByTagName("attractiveness").item(0).getTextContent();
            int placeTypeIndex = Integer.parseInt(attractiveness);
            //这里是将每个基站的网络延迟存储下来
            expRngList[i] = new ExponentialDistribution(SimSettings.getInstance().getMobilityLookUpTable()[placeTypeIndex]);
        }

        // 初始化树图和移动设备的位置，即将移动基站的位置信息以及wlanID存入TreeMap集合，
        // TODO 其实是为每个设备随机分配一个datacenter基站
        for(int i=0; i<numberOfMobileDevices; i++) {
            treeMapArray.add(i, new TreeMap<Double, Location>());

            int randDatacenterId = SimUtils.getRandomNumber(0, SimSettings.getInstance().getNumOfEdgeDatacenters()-1);
            Node datacenterNode = datacenterList.item(randDatacenterId);
            Element datacenterElement = (Element) datacenterNode;
            Element location = (Element)datacenterElement.getElementsByTagName("location").item(0);
            String attractiveness = location.getElementsByTagName("attractiveness").item(0).getTextContent();
            int placeTypeIndex = Integer.parseInt(attractiveness);
            int wlan_id = Integer.parseInt(location.getElementsByTagName("wlan_id").item(0).getTextContent());
            int x_pos = Integer.parseInt(location.getElementsByTagName("x_pos").item(0).getTextContent());
            int y_pos = Integer.parseInt(location.getElementsByTagName("y_pos").item(0).getTextContent());

            //start locating user shortly after the simulation started (e.g. 10 seconds)TreeMap<>
            treeMapArray.get(i).put(SimSettings.CLIENT_ACTIVITY_START_TIME, new Location(placeTypeIndex, wlan_id, x_pos, y_pos));
        }

        // 遍历设备，把仿真过程记录在 treeMapArray里，这个数组里面存储了设备 i 的基站选择记录
        for(int i=0; i<numberOfMobileDevices; i++) {
            TreeMap<Double, Location> treeMap = treeMapArray.get(i);    //获取设备 i 所分配的基站
            // 对设备 i 进行仿真 lastkey 存储的是上次设备 i 的活动时间，即记录在仿真时间里设备 i 所连接的基站信息
            while(treeMap.lastKey() < SimSettings.getInstance().getSimulationTime()) {
                boolean placeFound = false;    //标志位，记录是否找到新基站
                int currentLocationId = treeMap.lastEntry().getValue().getServingWlanId();
                double waitingTime = expRngList[currentLocationId].sample();  //根据平均延迟按指数分布生成等待时间
                // 随机分配一个不同的基站给设备
                while(placeFound == false){
                    int newDatacenterId = SimUtils.getRandomNumber(0,SimSettings.getInstance().getNumOfEdgeDatacenters()-1); //生成一个随机数,即分配一个新基站
                    if(newDatacenterId != currentLocationId){ //新基站和原基站不同时分配
                        placeFound = true;
                        Node datacenterNode = datacenterList.item(newDatacenterId); //获取基站信息
                        Element datacenterElement = (Element) datacenterNode;
                        Element location = (Element)datacenterElement.getElementsByTagName("location").item(0);
                        String attractiveness = location.getElementsByTagName("attractiveness").item(0).getTextContent();
                        int placeTypeIndex = Integer.parseInt(attractiveness);
                        int wlan_id = Integer.parseInt(location.getElementsByTagName("wlan_id").item(0).getTextContent());
                        int x_pos = Integer.parseInt(location.getElementsByTagName("x_pos").item(0).getTextContent());
                        int y_pos = Integer.parseInt(location.getElementsByTagName("y_pos").item(0).getTextContent());

                        treeMap.put(treeMap.lastKey()+waitingTime, new Location(placeTypeIndex, wlan_id, x_pos, y_pos));
                    }
                }
                if(!placeFound){
                    SimLogger.printLine("impossible is occurred! location cannot be assigned to the device!");
                    System.exit(1);
                }
            }
        }
    }

    @Override
    public Location getLocation(int deviceId, double time) {
        TreeMap<Double, Location> treeMap = treeMapArray.get(deviceId);  //得到设备 i 全部的轨迹和基站连接记录

        Map.Entry<Double, Location> e = treeMap.floorEntry(time); //根据时间获取当时的位置

        if(e == null){
            SimLogger.printLine("impossible is occurred! no location is found for the device '" + deviceId + "' at " + time);
            System.exit(1);
        }

        return e.getValue();
    }

}
