package example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class AllInMobile {

	private static final int USER_NUM = 500;

	// 改变组件个数，记得改变配置文件（组件之间的依赖关系）
	private static final int COMPENENT_NUM = 4 + 2;

	// 用户数组，下标从1开始, [1, USER_NUM]
	User[] users = new User[USER_NUM + 1];


	public static void main(String[] args) {
		AllInMobile allInMobile = new AllInMobile();
		allInMobile.run();
	}
	
	public void run() {
		readInputFile();
		
		mobileResult();
	}
	
	/*
	 * 所有用户在终端执行结果
	 */
	public void mobileResult() {
		for(int i = 1; i <= USER_NUM; i++) {
			
			for(int j = 0; j < COMPENENT_NUM; j++) {
				users[i].component[j].exeloc = 0;
			}
			
			users[i].makespan = compMakeSpan(i);
			users[i].power = compPower(i);
			
			users[i].power = users[i].power/1000;
			
			/*System.out.println("-----------用户" + i + "的结果---------");
			System.out.println("时间：" + users[i].makespan);
			System.out.println("能耗：" +  users[i].power + "\n");*/
		}
		
		double sumTime = 0;
		double sumPower = 0;
		for(int i = 1; i <= USER_NUM; i++) {
			sumTime += users[i].makespan;
			sumPower += users[i].power;
		}
		System.out.println("平均时间：" + sumTime / USER_NUM);
		System.out.println("平均能耗：" + sumPower / USER_NUM + "\n");
	}
	
	/*
	 * 计算完成时间
	 */
	public double compMakeSpan(int i) {
		
		users[i].component[0].ST = 0;
		for(int j = 0 ; j < COMPENENT_NUM; j++) {
			if (j == 0) { 	//第一个组件
				users[i].component[j].FT = users[i].component[j].ST + users[i].component[j].comp_mobile;
			} else {
				users[i].component[j].ST = users[i].component[j-1].FT;
				
				users[i].component[j].FT = users[i].component[j].ST + users[i].component[j].comp_mobile;
			}
		}
		
		double makespan = users[i].component[COMPENENT_NUM-1].FT - users[i].component[0].ST;
		return makespan;
	}
	
	/*
	 * 计算能耗
	 */
	public double compPower(int i) {
		double power = 0;
		power = users[i].makespan * (users[i].CPU_COMP_POWER+users[i].NETWORK_IDLE_POWER);
		return power;
	}
	
	
	/*
	 * 读取配置文件
	 * 1-组件之间的数据传输大小 
	 * 2-组件在本地和云端的执行时间 
	 * 3-用户的cpu功耗和网络接口功耗
	 */
	public void readInputFile() {
		try {
			for (int i = 1; i <= USER_NUM; i++) {

				int filenum = i % 4 + 1;
				
				users[i] = new User();

				URL dir = MSTDOS.class.getResource("");
				// 用户i的配置文件
				String filePath = dir.toString().substring(5) + "User" + filenum + ".txt";
				File file = new File(filePath);
				if (file.exists() && file.isFile()) {
					InputStreamReader isr = new InputStreamReader(new FileInputStream(file), "utf-8");
					@SuppressWarnings("resource")
					BufferedReader br = new BufferedReader(isr);
					String line = null;

					for (int j = 0; j < COMPENENT_NUM; j++) {
						// 用户i的组件之间的传输时间
						if ((line = br.readLine()) != null) {
							// System.out.println(line);
							String[] strs = line.split(" ");
							for (int k = 0; k < COMPENENT_NUM; k++) {
								// 用户i的组件之间的传输时间
								users[i].communication[j][k] = Double.valueOf(strs[k]);
								// System.out.print(users[i].communication[j][k]
								// +" ");
							}
						}
					}

					line = br.readLine();
					
					// 用户i的组件在本地的执行时间
					if ((line = br.readLine()) != null) {
						// System.out.println(line);
						String[] strs = line.split(" ");
						for (int j = 0; j < COMPENENT_NUM; j++) {
							// 用户i的组件之间的传输时间
							users[i].component[j].comp_mobile = Double.valueOf(strs[j]);
							// System.out.print(users[i].component[j].comp_mobile
							// +" ");
						}
					}

					// 用户i的组件在云端执行的时间
					if ((line = br.readLine()) != null) {
						// System.out.println("\n" + line);
						String[] strs = line.split(" ");
						for (int j = 0; j < COMPENENT_NUM; j++) {
							users[i].component[j].comp_cloud = Double.valueOf(strs[j]);
							// System.out.print(users[i].component[j].comp_cloud
							// + " ");
						}
					}

					line = br.readLine();
					
					// 用户i的cpu工作、空闲功率
					if ((line = br.readLine()) != null) {
						// System.out.println("\n" + line);
						String[] strs = line.split(" ");
						users[i].CPU_COMP_POWER = Double.valueOf(strs[0]);
						users[i].CPU_IDLE_POWER = Double.valueOf(strs[1]);
						// System.out.print(users[i].CPU_COMP_POWER + " " +
						// users[i].CPU_IDLE_POWER);
					}

					// 用户i的网络接口发送功率、接收功率、空闲功率
					if ((line = br.readLine()) != null) {
						// System.out.println("\n" + line);
						String[] strs = line.split(" ");
						users[i].NETWORK_SNED_POWER = Double.valueOf(strs[0]);
						users[i].NETWORK_RECV_POWER = Double.valueOf(strs[1]);
						users[i].NETWORK_IDLE_POWER = Double.valueOf(strs[2]);
						// System.out.println(users[i].NETWORK_SNED_POWER);
					}
					
					line = br.readLine();
					
					//用户i的带宽
					if ((line = br.readLine()) != null) {
						String[] strs = line.split(" ");
						users[i].bandWidth = Double.valueOf(strs[0]);
//						System.out.println(users[i].bandWidth);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * 用户类
	 */
	class User {
		Molecule molecule; // 用户的迁移策略——分子结构

		Component[] component; // 用户的组件集合

		double[][] communication; // 组件之间传输时间

		double makespan;
		double power;
		double bandWidth; // 带宽
		

		double CPU_COMP_POWER; // cpu计算、空闲功率
		double CPU_IDLE_POWER;

		double NETWORK_SNED_POWER; // 网络接口发送、接收、空闲功率
		double NETWORK_RECV_POWER;
		double NETWORK_IDLE_POWER;

		public User() {
			component = new Component[COMPENENT_NUM];
			for (int i = 0; i < COMPENENT_NUM; i++) {
				component[i] = new Component();
			}
			communication = new double[COMPENENT_NUM][COMPENENT_NUM];
		}
		
		//清空User内容（计算适应度函数时，会根据不同的策略而改变）
		public void clear() {
			makespan = 0;
			power = 0;
			molecule.clear();
			for(int i = 0; i < COMPENENT_NUM; i++) {
				component[i].clear();
			}
		}
	}
	
	class Molecule {
		Map<Integer, Integer> structure; // 分子结构
		double PE; // 分子势能
		double KE; // 分子动能
		
		double makespan;
		double power;

		public Molecule() {
			structure = new HashMap<>();
			PE = 0;
			KE = 0;
		}
		
		public void clear() {
			structure = new HashMap<>();
			PE = 0;
			KE = 0;
		}

		public void setStructure(Map<Integer, Integer> structure) {
			this.structure = structure;
		}

		public Map<Integer, Integer> getStructure() {
			return structure;
		}

		public double getPE() {
			return PE;
		}

		public void setPE(double pE) {
			PE = pE;
		}

		public double getKE() {
			return KE;
		}

		public void setKE(double kE) {
			KE = kE;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			} else {
				if (this.getClass() == obj.getClass()) {
					Molecule s = (Molecule) obj;
					Map<Integer, Integer> thisStructure = this.getStructure();
					Map<Integer, Integer> objStructure = s.getStructure();
					Set<Integer> keySet = thisStructure.keySet();
					Iterator<Integer> it = keySet.iterator();
					while (it.hasNext()) {
						int name = it.next();
						if (thisStructure.get(name) != objStructure.get(name)) {
							return false;
						}
					}
				}
			}
			return true;
		}
	}
	
	
	/*
	 * 组件类
	 */
	class Component {
		double comp_mobile;	 // 组件在本地执行时间
		double comp_cloud; 	 // 组件在云端执行时间
		int exeloc;			 // 组件执行位置，0-本地，1-云端
		double ST; 			 // 组件开始执行时间
		double FT; 			 // 组件结束时间
		
		public Component() {
			exeloc = -1;
			ST = 0;
			FT = 0;
		}
		
		public void clear() {
			exeloc = -1;
			ST = 0;
			FT = 0;
		}
	}
}
