/*
 * Title:        EdgeCloudSim - Simulation Utils
 * 
 * Description:  Utility class providing helper functions
 * 
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.utils;

import java.io.File;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class SimUtils {

	public static final Random RNG = new Random(System.currentTimeMillis());

	public static int getRandomNumber(int start, int end) {
		//return pd.sample();
		long range = (long)end - (long)start + 1;
		long fraction = (long)(range * RNG.nextDouble());
		return (int)(fraction + start);
	}

	public static double getRandomDoubleNumber(double start, double end) {
		//return pd.sample();
		double range = end - start;
		double fraction = (range * RNG.nextDouble());  //获取伪随机数
		return (fraction + start); 
	}

	public static long getRandomLongNumber(long start, long end) {
		//return pd.sample();
		long range = (long)end - (long)start + 1;
		long fraction = (long)(range * RNG.nextDouble());
		return (fraction + start); 
	}

	public static void cleanOutputFolder(String outputFolder){
		//clean the folder where the result files will be saved
		File dir = new File(outputFolder);
		// ****  Add by Jacazjx  2021-04-06  ****
		if(dir.exists()){
			SimLogger.printLine("文件夹存在，已清空: " + outputFolder);
			File[] files = dir.listFiles();
			for (File f: files){
				//打印文件名
				String name = dir.getName();
				f.delete();
			}
			//删除空文件夹  for循环已经把上一层节点的目录清空。
			dir.delete();
		}
		// **************************************
		if(!dir.mkdirs()){
			SimLogger.printLine("创建文件夹失败: " + outputFolder);
			System.exit(1);
		}
		for (File f: dir.listFiles())
		{
			if (f.exists() && f.isFile())
			{
				if(!f.delete())
				{
					SimLogger.printLine("file cannot be cleared: " + f.getAbsolutePath());
					System.exit(1);
				}
			}
		}
	}

	public static String getTimeDifference(Date startDate, Date endDate){
		String result = "";
		long duration  = endDate.getTime() - startDate.getTime();

		long diffInMilli = TimeUnit.MILLISECONDS.toMillis(duration);
		long diffInSeconds = TimeUnit.MILLISECONDS.toSeconds(duration);
		long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(duration);
		long diffInHours = TimeUnit.MILLISECONDS.toHours(duration);
		long diffInDays = TimeUnit.MILLISECONDS.toDays(duration);

		if(diffInDays>0)
			result += diffInDays + ((diffInDays>1 == true) ? " Days " : " Day ");
		if(diffInHours>0)
			result += diffInHours % 24 + ((diffInHours>1 == true) ? " Hours " : " Hour ");
		if(diffInMinutes>0)
			result += diffInMinutes % 60 + ((diffInMinutes>1 == true) ? " Minutes " : " Minute ");
		if(diffInSeconds>0)
			result += diffInSeconds % 60 + ((diffInSeconds>1 == true) ? " Seconds" : " Second");
		if(diffInMilli>0 && result.isEmpty())
			result += diffInMilli + ((diffInMilli>1 == true) ? " Milli Seconds" : " Milli Second");

		return result;
	}
}
