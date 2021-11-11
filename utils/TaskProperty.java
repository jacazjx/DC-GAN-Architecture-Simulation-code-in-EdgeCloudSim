/*
 * Title:        EdgeCloudSim - EdgeTask
 * 
 * Description: 
 * A custom class used in Load Generator Model to store tasks information
 * 
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.utils;

import org.apache.commons.math3.distribution.ExponentialDistribution;

import edu.boun.edgecloudsim.core.SimSettings;
// 任务执行的数据详情
public class TaskProperty {
	private double startTime;    	//起始时间
	private long length, inputFileSize, outputFileSize;    //
	private int taskType;       	//任务类型
	private int pesNumber;			//需要的CPU核心数
	private int mobileDeviceId;      //执行该任务的设备ID

	public TaskProperty(double _startTime, int _mobileDeviceId, int _taskType, int _pesNumber, long _length, long _inputFileSize, long _outputFileSize) {
		startTime=_startTime;
		mobileDeviceId=_mobileDeviceId;
		taskType=_taskType;
		pesNumber = _pesNumber;
		length = _length;
		outputFileSize = _inputFileSize;
		inputFileSize = _outputFileSize;
	}

	public TaskProperty(int _mobileDeviceId, int _taskType, double _startTime, ExponentialDistribution[][] expRngList) {
		mobileDeviceId=_mobileDeviceId;
		startTime=_startTime;
		taskType=_taskType;
		//随机（指数分布）任务上传下载数据大小以及任务长度
		inputFileSize = (long)expRngList[_taskType][0].sample();  //应用输出的数据大小
		outputFileSize =(long)expRngList[_taskType][1].sample();  //应用接受的数据大小
		length = (long)expRngList[_taskType][2].sample();

		pesNumber = (int)SimSettings.getInstance().getTaskLookUpTable()[_taskType][8];
	}

	public TaskProperty(int mobileDeviceId, double startTime, ExponentialDistribution[] expRngList) {
		this.mobileDeviceId = mobileDeviceId;
		this.startTime = startTime;
		taskType = 0;
		inputFileSize = (long)expRngList[0].sample();
		outputFileSize = (long)expRngList[1].sample();
		length = (long) expRngList[2].sample();
		pesNumber = (int)SimSettings.getInstance().getTaskLookUpTable()[0][8];
	}

	public double getStartTime(){
		return startTime;
	}

	public long getLength(){
		return length;
	}

	public long getInputFileSize(){
		return inputFileSize;
	}

	public long getOutputFileSize(){
		return outputFileSize;
	}

	public int getTaskType(){
		return taskType;
	}

	public int getPesNumber(){
		return pesNumber;
	}

	public int getMobileDeviceId(){
		return mobileDeviceId;
	}
}
