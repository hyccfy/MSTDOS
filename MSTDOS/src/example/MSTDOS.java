package example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/*
 * 实验：用户请求数的影响。
 */
public class MSTDOS {

	private static final int USER_NUM = 500;
	private static final int VM_NUM = 200;
	private static final int Net = 8;
	private static final double Relat = 0.3;

	// 改变组件个数，记得改变配置文件（组件之间的依赖关系）
	private static final int COMPENENT_NUM = 4 + 2;

	// 用户数组，下标从1开始, [1, USER_NUM]
	User[] users = new User[USER_NUM + 1];

	// 虚拟机数组，下标从1开始，[1, VM_NUM]
	VM[] vms = new VM[VM_NUM + 1];

	public static void main(String[] args) {
		MSTDOS MSTDOS = new MSTDOS();
		MSTDOS.run();
	}

	/*
	 * 程序入口
	 */
	public void run() {
		initParam();	//初始化User[]，VM[]
		readInputFile();

		
		// 依次处理每个用户
		for (int i = 1; i <= USER_NUM; i++) {
			//System.out.println("-------------------------用户" + i + "-start------------------------");
			
			double RT_min = Double.MAX_VALUE;
			int vm_index = 0;
			for (int j = 1; j <= VM_NUM; j++) {
				if (vms[j].RT < RT_min) {
					RT_min = vms[j].RT; // 记录可云端最早开始执行任务的时间
					vm_index = j; // 记录对应虚拟的编号
				}
			}

			// 使用CRO算法为用户得到最优迁移策略
			Molecule molecule = cro(i, RT_min);
			
			users[i].molecule = molecule;
			
			/*System.out.println("\n用户" + i + "的最终迁移策略:");
			StringBuilder sb = new StringBuilder();
			for(int j = 1; j <= COMPENENT_NUM-2; j++) {
				Map<Integer, Integer> structure = users[i].molecule.getStructure();
				sb.append(structure.get(j));
			}
			System.out.println(sb.toString());*/
			
			
			double makespan = compMakeSpan(i, molecule, RT_min);
			double power = compEnergy(i, molecule, RT_min);
			users[i].makespan = makespan;
			users[i].power = power;

			users[i].power = users[i].power / 1000;
			/*for(int j = 0; j < COMPENENT_NUM; j++ ) {
				System.out.println("组件" + j + ":   ST:" + users[i].component[j].ST + ";   FT:" + users[i].component[j].FT);
			}	
			System.out.println("时间:" + users[i].makespan);
			System.out.println("能耗:" + users[i].power);*/
			
			int size = users[i].cloudList.size();
			if (size != 0) {
				users[i].vm_index = vm_index;
				
				/*System.out.println("所在虚拟机：" + vm_index);
				System.out.println("云端组件链表cloudList长度:" + size);
				System.out.print("云端组件集:");
				for(int j = 0; j < size; j++)
				{
					if (j == size -1) {
						System.out.print(users[i].cloudList.get(j));
					} else {
						System.out.print(users[i].cloudList.get(j) + "-");
					}
				}*/
				
				int cloudlIdx = users[i].cloudList.get(users[i].cloudList.size()-1);
				double cloudFT = users[i].component[cloudlIdx].FT;
				//System.out.println("\n新RT：" + cloudFT);
				// 更新该虚拟机的RT（RT更新的值为当前用户cloudList中最后一个组件的完成时间，即该虚拟机的最新RT）
				updateRT(i, vm_index, cloudFT);
			}
			
			//printVmsRT();
			
			//System.out.println("-------------------------用户" + i + "-end------------------------\n");
		}

		System.out.println("----------------------------------------------------------");
		// 计算所有用户的平均时间和平均能耗
		//printUsersResult();
		System.out.println("\n--------所有用户平均结果---------");
		showResult();
	}
	
	/*
	 * 输出所有用户最终结果
	 */
	public void printUsersResult () {
		for(int i = 1; i <= USER_NUM; i++) {
			System.out.print("\n用户" + i + "的迁移策略:");
			StringBuilder sb = new StringBuilder();
			for(int j = 1; j <= COMPENENT_NUM-2; j++) {
				Map<Integer, Integer> structure = users[i].molecule.getStructure();
				sb.append(structure.get(j));
			}
			System.out.println(sb.toString());
			
			System.out.println("时间:" + Double.valueOf(df.format(users[i].makespan)));
			System.out.println("能耗:" + Double.valueOf(users[i].power));
			
			int size = users[i].cloudList.size();
			if (size != 0) {
				System.out.println("所在虚拟机：" + users[i].vm_index);
				System.out.println("云端组件链表cloudList长度:" + size);
				System.out.print("云端组件集:");
				for(int j = 0; j < size; j++)
				{
					if (j == size -1) {
						System.out.print(users[i].cloudList.get(j));
					} else {
						System.out.print(users[i].cloudList.get(j) + "-");
					}
				}
				
				System.out.println();
			} else {
				System.out.println("用户在本地执行：" + users[i].vm_index);
			}
		}
	}
	
	/*
	 * 输出平均时间和能耗
	 */
	public void showResult() {
		double sumMakeSpan =0;
		double sumPower = 0;
		for(int i = 1; i <= USER_NUM; i++) {
			sumMakeSpan += users[i].makespan;
			sumPower += users[i].power;
		}
		double avgMakeSpan = sumMakeSpan / USER_NUM;
		double avgPower = sumPower / USER_NUM;
		double value = Relat*avgMakeSpan + (1-Relat)*avgPower;
		System.out.println("所有用户的平均时间：" + Double.valueOf(df.format(avgMakeSpan)));
		System.out.println("所有用户的平均能耗：" + Double.valueOf(df.format(avgPower)));
		System.out.println(Double.valueOf(df.format(value)));
	}

	/*
	 * 更新vm_index对应虚拟机的RT
	 */
	public void updateRT(int i, int vm_index, double cloudFT) {
		double newRT = vms[vm_index].RT < cloudFT ? cloudFT : vms[vm_index].RT;
		vms[vm_index].RT = newRT;
	}

	/*
	 * 计算响应时间:组件执行时间+组件之间传输时间 （这种计算时间不对，还要考虑在云端的等待时间）
	 */
	public double compMakeSpan(int i, Molecule molecule) {
		
		double makespan = 0;
		double exeTime = compExeTime(i, molecule);
		double netTransTime = compNetTransTime(i, molecule);
		makespan = exeTime + netTransTime;

		// makespan = 最后一个组件的结束时间 - 第一个组件的开始执行时间。（和上面一样的）
		// Map<Integer, Integer> structure = molecule.getStructure();
		// makespan = users[i].component[COMPENENT_NUM - 1].FT -
		// users[i].component[0].ST;
		return makespan;
	}

	/*
	 * 组件执行时间
	 */
	public double compExeTime(int i, Molecule molecule) {
		double exeTime = 0;
		Map<Integer, Integer> structure = molecule.getStructure();
		for (int j = 0; j < COMPENENT_NUM; j++) {
			int pos = structure.get(j);
			if (pos == 0) {
				exeTime += users[i].component[j].comp_mobile; // comp_mobile
			} else {
				exeTime += users[i].component[j].comp_cloud; // comp_cloud
			}
		}
		return exeTime;
	}

	/*
	 * 组件之间传输时间
	 */
	public double compNetTransTime(int i, Molecule molecule) {
		double transTime = 0;

		// 传输时间 = 发送时间 + 接收时间
		double sendTime = compNetSendTime(i, molecule);
		double recvTime = compNetRecvTime(i, molecule);

		transTime = sendTime + recvTime;
		return transTime;
	}

	/*
	 * 网络接口发送数据时间
	 */
	public double compNetSendTime(int i, Molecule molecule) {
		double netSendTime = 0;
		Map<Integer, Integer> structure = molecule.getStructure();
		for (int j = 1; j < COMPENENT_NUM; j++) {
			int pos = structure.get(j);
			int prepos = structure.get(j - 1);
			if (prepos == 0 && pos == 1) {
				netSendTime += users[i].communication[prepos][pos] / users[i].bandWidth; // 发送时间
			}
		}
		return netSendTime;
	}

	/*
	 * 网络接口接收数据时间
	 */
	public double compNetRecvTime(int i, Molecule molecule) {
		double netRecvTime = 0;
		Map<Integer, Integer> structure = molecule.getStructure();
		for (int j = 1; j < COMPENENT_NUM; j++) {
			int pos = structure.get(j);
			int prepos = structure.get(j - 1);
			if (prepos == 1 && pos == 0) {
				netRecvTime += users[i].communication[prepos][pos] / users[i].bandWidth; // 接收时间：数据/带宽
			}
		}
		return netRecvTime;
	}

	/*
	 * 组件在本地执行时间，即cpu处于工作状态的时间
	 */
	public double compMobileExeTime(int i, Molecule molecule) {
		double mobileExeTime = 0;
		Map<Integer, Integer> structure = molecule.getStructure();
		for (int j = 0; j < COMPENENT_NUM; j++) {
			int pos = structure.get(j);
			if (pos == 0) {
				mobileExeTime += users[i].component[j].comp_mobile; // comp_mobile
			}
		}
		return mobileExeTime;
	}

	/*
	 * 计算终端能耗：cpu能耗 + 网络接口能耗
	 */
	public double compEnergy(int i, Molecule molecule) {
		double totalEnergy = 0;
		double cpuEnergy = compCpuEnergy(i, molecule);
		double netEnergy = compNetEnergy(i, molecule);
		totalEnergy = cpuEnergy + netEnergy;
		return totalEnergy;
	}

	/*
	 * cpu能耗：cpu工作能耗+cpu空闲能耗
	 */
	public double compCpuEnergy(int i, Molecule molecule) {
		double cpuEnergy;
		double cpuWorkEnergy = compCpuWorkEnergy(i, molecule);
		double cpuIdleEnergy = compCpuIdleEnergy(i, molecule);
		cpuEnergy = cpuWorkEnergy + cpuIdleEnergy;
		return cpuEnergy;
	}

	/*
	 * 计算cpu处于工作状态下的能耗
	 */
	public double compCpuWorkEnergy(int i, Molecule molecule) {
		double cpuWorkEnergy = 0;
		double mobileExeTime = compMobileExeTime(i, molecule);
		cpuWorkEnergy = mobileExeTime * users[i].CPU_COMP_POWER;
		return cpuWorkEnergy;
	}

	/*
	 * 计算cpu处于空闲状态时的能耗
	 */
	public double compCpuIdleEnergy(int i, Molecule molecule) {
		double cpuIdleEnergy = 0;

		// 应用执行总时间
		double makespan = compMakeSpan(i, molecule);

		// 本地执行时间
		double mobileExeTime = compMobileExeTime(i, molecule);

		// 差值即为cpu空闲时间
		double idleTime = makespan - mobileExeTime;
		cpuIdleEnergy = idleTime * users[i].CPU_IDLE_POWER;
		return cpuIdleEnergy;
	}

	/*
	 * 网络接口能耗：发送功耗+接收功耗+空闲功耗
	 */
	public double compNetEnergy(int i, Molecule molecule) {
		double netEnergy = 0;
		double netSendEnergy = compNetSendEnergy(i, molecule);
		double netRecvEnergy = compNetRecvEnergy(i, molecule);
		double netIdleEnergy = compNetIdleEnergy(i, molecule);
		netEnergy = netSendEnergy + netRecvEnergy + netIdleEnergy;
		return netEnergy;
	}

	/*
	 * 计算网络接口处于发送状态的能耗
	 */
	public double compNetSendEnergy(int i, Molecule molecule) {
		double netSendEnergy = 0;
		double netSendTime = compNetSendTime(i, molecule);
		netSendEnergy = netSendTime * users[i].NETWORK_SNED_POWER;
		return netSendEnergy;
	}

	/*
	 * 计算网络接口处于接收状态的能耗
	 */
	public double compNetRecvEnergy(int i, Molecule molecule) {
		double netRecvEnergy = 0;
		double netRecvTime = compNetRecvTime(i, molecule);
		netRecvEnergy = netRecvTime * users[i].NETWORK_RECV_POWER;
		return netRecvEnergy;
	}

	/*
	 * 计算网络接口处于空闲时的能耗
	 */
	public double compNetIdleEnergy(int i, Molecule molecule) {
		double netIdleEnergy = 0;
		double netIdleTime = 0;

		// 网络接口空闲时间 = 总时间 - 发送时间 - 接收时间
		double makespan = compMakeSpan(i, molecule);
		double netSendTime = compNetSendTime(i, molecule);
		double netRecvTime = compNetRecvTime(i, molecule);

		netIdleTime = makespan - netSendTime - netRecvTime;
		netIdleEnergy = netIdleTime * users[i].NETWORK_IDLE_POWER;
		return netIdleEnergy;
	}

	/*
	 * 初始化相关参数
	 */
	public void initParam() {
		// 初始化用户对象数组users
		for (int i = 1; i <= USER_NUM; i++) {
			users[i] = new User();
		}
		// 初始胡虚拟机对象数组vms
		for (int i = 1; i <= VM_NUM; i++) {
			vms[i] = new VM();
		}
	}

	/*
	 * 读取配置文件 1-组件之间的数据传输大小 2-组件在本地和云端的执行时间 3-用户的cpu功耗和网络接口功耗
	 */
	public void readInputFile() {
		try {
			for (int i = 1; i <= USER_NUM; i++) {

				int filenum = i % 4 + 1;
				
				users[i] = new User();

				URL dir = MSTDOS.class.getResource(""); // file:/E:/workspace/post/bin/com/paper/alg/ch1/
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
						users[i].bandWidth = 1024 * Net / 8.0 /1000;
//						System.out.println(users[i].bandWidth);
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 用户请求相关参数，用户类
	 *
	 */
	class User {
		Molecule molecule; // 用户的迁移策略——分子结构

		Component[] component; // 用户的组件集合

		double[][] communication; // 组件之间传输时间

		double waitTime; // 等待时间

		List<Integer> cloudList = new LinkedList<>(); // 用户在云端执行的组件的链表

		double makespan;
		double power;
		double bandWidth; // 带宽
		
		int vm_index;	//若用户有组件在云端执行，记录其所在的虚拟机

		double CPU_COMP_POWER; // cpu计算、空闲功率
		double CPU_IDLE_POWER;

		double NETWORK_SNED_POWER; // 网络接口发送、接收、空闲功率
		double NETWORK_RECV_POWER;
		double NETWORK_IDLE_POWER;

		public User() {
			molecule = new Molecule();
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
			cloudList.clear();
		}
	}

	/*
	 * 组件类
	 */
	class Component {
		double comp_mobile; // 组件在本地执行时间
		double comp_cloud; // 组件在云端执行时间
		int exeloc; // 组件执行位置，0-本地，1-云端
		double ST; // 组件开始执行时间
		double FT; // 组件结束时间
		
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

	/*
	 * 虚拟机类
	 */
	class VM {
		double RT;

		public VM() {
		}
	}

	/**
	 * 分子类
	 */
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

	// CRO相关参数
	int popSize = 16; // 种群大小
	int maxIter = 100; // 最大迭代次数
	double KELossRate = 0.3; // 能量损失率
	double moleColl = 0.2; // 决策分子反应的参数

	double buffer = 0;
	double iKE = 0;
	double decomp, synth; // 分解和合成判断标准

	DecimalFormat df = new DecimalFormat("#.0000"); // 用户格式化数据，double输出保留四位小数

	//分子种群中分子（策略）的集合

	List<Molecule> moleculeList = new LinkedList<>();
	/*
	 * 化学反应
	 * 
	 * @param i: 用户编号
	 * @parma RT_min: 用户i可最早开始执行任务的时间
	 */
	public Molecule cro(int i, double RT_min) {
		Random rand = new Random();
		initCROParam();

		moleculeList = new LinkedList<>();
		
		//1.初始化popSize个分子种群，并计算E_max,E_min,T_max,T_min
		initMoleculePopulation(popSize, RT_min);
		
		//printMoleculeList();

		double[] ET = getEAndT(i, RT_min);
		
		//2.初始化分子的势能和动能
		initKEAndPE(i, RT_min, ET);

		//输出T_max,T_min,E_max,E_min
//		printEAndT(ET);

//		System.out.println("---------初始分子种群-------");
//		printMoleculeList();
		Molecule minStructure = new Molecule();
		
		int currMoleculeSize = 0;
		
		//CRO迭代过程，计算适应度函数也要考虑RT_min
		int iter = 1;
		while (iter <= maxIter) {
			
			currMoleculeSize = moleculeList.size();
			
			boolean STATUS = false; // 判断化学反应是否发生成功
			double t = rand.nextDouble();
			if (t > moleColl || moleculeList.size()==1) { // 发生单分子反应
				// 选择一个分子进行单分子反应。[0,moleculeList.size()-1]
				Molecule s = moleculeList.get(randAToB(0, moleculeList.size()-1));
				
				if (checkDecomp(s) ) { // 单分子分解反应
					STATUS = decompose(i, s, ET, RT_min);
					if (!STATUS) { // 分解失败
						continue;
					}
				} else { // 单分子碰撞反应
					ineff_coll_on_wall(i, s, ET, RT_min);
				}
			} else { // 分子间反应
				// 选择一个分子对<S1,S2>
				Molecule s1 = moleculeList.get(randAToB(0, moleculeList.size()-1));
				Molecule s2 = moleculeList.get(randAToB(0, moleculeList.size()-1));
				
				int flag = 0;
				while (s1.equals(s2)) {
					s2 = moleculeList.get(randAToB(0, moleculeList.size()-1));
					if(flag++ == 20)
						break;
				}
				
				if (checkSynth(s1, s2)) { // 分子合成
					STATUS = synthesis(i, s1, s2, ET, RT_min);
					if (!STATUS) {
						continue;
					}
				} else { // 分之间碰撞
					inter_ineff_coll(i, s1, s2, ET, RT_min);
				}
			}
			
			// 找到本代中势能最小的分子结构，更新全局最小
			minStructure = getMinStructure(moleculeList);

			Map<Integer, Integer> map = minStructure.getStructure();
			StringBuilder sb = new StringBuilder();
			for (int j = 0; j < COMPENENT_NUM; j++) {
				sb.append(map.get(j));
			}
//			System.out.println("-------第" + iter + "次迭代-------");
//			System.out.println("种群：" + moleculeList.size());
			
			
//			printMoleculeList();
			
//			System.out.println("本代最优分子结构：" + sb.toString());
//			System.out.println("本代中最小的势能：" + Double.valueOf(df.format(minStructure.PE)));
			minStructure.PE = Double.MAX_VALUE;
			
			//ET = getEAndT(i, RT_min);
			
			iter++;
		} // 迭代结束
		
		
		return minStructure;
	}

	/*
	 * 输出分子集合
	 */
	public void printMoleculeList() {
		for (int i = 0; i < moleculeList.size(); i++) {
			Map<Integer, Integer> map = moleculeList.get(i).getStructure();
			StringBuilder sb = new StringBuilder();
			for (int j = 0; j < COMPENENT_NUM; j++) {
				sb.append(map.get(j));
			}
			System.out.println(sb.toString() + ":()" + Double.valueOf(df.format(moleculeList.get(i).PE)));
			printMolecule(moleculeList.get(i));
		}
	}
	
	/*
	 * 打印分子
	 */
	public void printMolecule(Molecule molecule){
		System.out.println();
		Map<Integer, Integer> structure = molecule.getStructure();
		System.out.print("迁移策略：");
		for(int k = 0; k < COMPENENT_NUM; k++) {
			System.out.print(structure.get(k) + " ");
		}
		
		System.out.println();
		System.out.println("KE:" + Double.valueOf(df.format(molecule.KE)) + ";    " + "PE:" + Double.valueOf(df.format(molecule.PE)));
		System.out.println("makespan:" + Double.valueOf(df.format(molecule.makespan)) +";    " + "power:" + Double.valueOf(df.format(molecule.power)));
		System.out.println();
	}
	
	public void printVmsRT() {
		for(int j = 1; j <= VM_NUM; j++) {
			VM vm = vms[j];
			System.out.println("VM" + j + ":" + vm.RT);
		}
	}

	/*
	 * 初始化CRO的相关参数
	 * （这两个数值可以设置得更小一下，依据ZHM算法）
	 */
	public void initCROParam() {
		Random rand = new Random();
		decomp = rand.nextDouble(); // 单分子分解常数
		synth = rand.nextDouble(); // 分子间合成常数
	}

	/*
	 * check单分子分解
	 */
	public boolean checkDecomp(Molecule s) {
		return s.PE >= decomp;
	}

	/*
	 * check分子合成
	 */
	public boolean checkSynth(Molecule s1, Molecule s2) {

		return (s1.KE <= synth && s2.KE <= synth);
	}

	/*
	 * 初始化popSize个分子种群
	 */
	public void initMoleculePopulation(int popSize, double RT_min) {
		for (int j = 1; j <= popSize; j++) {
			// 随机产生一个分子
			Molecule molecule = randomGenMolecule();

			if (!moleculeList.contains(molecule)) {
				moleculeList.add(molecule);
			} else {
				j--;
			}
		}
	}
	
	/*
	 * 获得E_max,E_min,T_max,T_min
	 */
	public double[] getEAndT(int i, double RT_min) {
		double E_max = Double.MIN_VALUE;
		double E_min = Double.MAX_VALUE;
		double T_max = Double.MIN_VALUE;
		double T_min = Double.MAX_VALUE;
		double[] ET = new double[4];
		for(int j = 0; j < moleculeList.size(); j++) {
			Molecule molecule = moleculeList.get(j);
			
//			Map<Integer, Integer> map = molecule.getStructure();
//			System.out.println(map);
			
			double makespan = compMakeSpan(i, molecule, RT_min);
			double  power = compEnergy(i, molecule, RT_min);
			
//			System.out.println("makespan:" + makespan + " ;;;;" + "power:" + power);
			
			molecule.makespan = makespan;
			molecule.power = power;
			
			T_max = makespan > T_max ? makespan : T_max;
			T_min = makespan < T_min ? makespan : T_min;

			E_max = power > E_max ? power : E_max;
			E_min = power < E_min ? power : E_min;
		}
		ET[0] = T_max;
		ET[1] = T_min;
		ET[2] = E_max;
		ET[3] = E_min;
		return ET;
	}
	
	/*
	 * 打印当前代E和T
	 */
	public void printEAndT(double[] ET) {
		double T_max = ET[0];
		double T_min = ET[1];
		double E_max = ET[2];
		double E_min = ET[3];
		System.out.println("T_max:" + T_max + "————" + "T_min:" + T_min + "————" + ""
				+ "E_max:" + E_max + "————" + "E_min:" + E_min);
	}
	
	/*
	 * 初始化动能和势能
	 */
	public void initKEAndPE(int i, double RT_min, double[] ET) {
		double sumPE = 0;
		for(int j = 0; j < moleculeList.size(); j++) {
			Molecule molecule =moleculeList.get(j);
			
			double makespan = compMakeSpan(i, molecule, RT_min);
			double power = compEnergy(i, molecule, RT_min);
			
//			System.out.println(molecule.getStructure());
//			System.out.println("makespan:" + makespan + ";;;;"  + "power:" + power);
			
			double PE = compPE(i, molecule, ET, RT_min);
			
			molecule.makespan = makespan;
			molecule.power = power;
			molecule.PE = PE;
			
			sumPE += PE;
		}
		
		Random rand = new Random();
		double initKE = sumPE / moleculeList.size();
		
		//初始化动能为势能的均值一个[0,1]区间的随机数
		for(int j = 0; j < moleculeList.size(); j++) {
			Molecule molecule = moleculeList.get(j);
			molecule.setKE(rand.nextDouble() * initKE);

			//输出分子信息
//			printMolecule(molecule);
		}
	}
	
	/*
	 * 计算适应度函数——势能
	 */
	public double compPE(int i, Molecule molecule, double[] ET, double RT_min) {
		double T_max = ET[0];
		double T_min = ET[1];
		double E_max = ET[2];
		double E_min = ET[3];
		
		double makespan = compMakeSpan(i, molecule, RT_min);
		double  power = compEnergy(i, molecule, RT_min);
		
		
		molecule.makespan = makespan;
		molecule.power = power;
		
		//还要传入两个权重
		double PE = Relat * (molecule.makespan - T_min) / (T_max - T_min) + 
				(1-Relat) * (molecule.power - E_min) / (E_max - E_min);
		
		//只考虑时间，只考虑能耗
//		double PE = 0 * (molecule.makespan - T_min) / (T_max - T_min) + 
//				1 * (molecule.power - E_min) / (E_max - E_min);
		
		
				
		//清空暂时存放数据的user[i]
		users[i].clear();
		return PE;
	}
	
	/*
	 * 计算完成时间，考虑RT_min
	 */
	public double compMakeSpan(int i, Molecule molecule, double RT_min) {
		
		//重要，users[i].cloudList是通用的，前面的迁移策略会对后面产生影响
		users[i].cloudList = new LinkedList<>();
		
		users[i].component[0].ST = 0;
		Map<Integer, Integer> structure = molecule.getStructure();
		for(int j = 0 ; j < COMPENENT_NUM; j++) {
			int pos = structure.get(j);
			int prepos = -1;
			if (j == 0) { 	//第一个组件
				users[i].component[j].FT = users[i].component[j].ST + users[i].component[j].comp_mobile;
			} else {
				if (pos == 0) {		
					//当前组件在本地执行
					prepos = structure.get(j-1);
					
					if (prepos == 1) {	//其前驱在云端执行
						users[i].component[j].ST = users[i].component[j-1].FT +
								users[i].communication[j-1][j] / users[i].bandWidth;
					} else {  	//前驱在本地执行
						users[i].component[j].ST = users[i].component[j-1].FT;
					}
					users[i].component[j].FT = users[i].component[j].ST + users[i].component[j].comp_mobile;
				} else {	
					//组件在云端执行
					
					if (users[i].cloudList.size() == 0) {	
						//没有前驱组件在云端执行,组件的开始时间需要 与RT_min比较
						
						double pTime = users[i].component[j-1].FT + users[i].communication[j-1][j] / users[i].bandWidth;
						users[i].component[j].ST = max(pTime, RT_min);
					} else {	//云端链表中有组件
						prepos = structure.get(j-1);
						if (prepos == 0) {		//前驱在本地
							users[i].component[j].ST = users[i].component[j-1].FT + users[i].communication[j-1][j] / users[i].bandWidth;
						} else {
							users[i].component[j].ST = users[i].component[j-1].FT;
						}
					}
					//完成时间
					users[i].component[j].FT = users[i].component[j].ST + users[i].component[j].comp_cloud;
					
					//更新用户i的云端组件链表
					users[i].cloudList.add(j);		//将组件j添加到云端执行链表
				}
			}
		}
		
		double makespan = users[i].component[COMPENENT_NUM-1].FT - users[i].component[0].ST;
		users[i].makespan = makespan;
		return makespan;
	}
	
	/*
	 * 计算能耗，考虑RT_min
	 */
	public double compEnergy(int i, Molecule molecule, double RT_min) {
		
		//应用执行总时间
		double makespan = users[i].makespan;
		Map<Integer, Integer> structure = molecule.getStructure();
		double exeTime = 0;
		double sendTime = 0, recvTime = 0;
		for(int j = 0; j < structure.size(); j++) {
			int pos = structure.get(j);
			if(pos == 0) {
				exeTime += users[i].component[j].FT - users[i].component[j].ST;
			}
			if (j > 0 ) {
				int prepos = structure.get(j-1);
				if (prepos == 0 && pos == 1) {
					sendTime += users[i].component[j].ST - users[i].component[j-1].FT;
				}
				if (prepos == 1 && pos == 0) {
					recvTime += users[i].component[j].ST - users[i].component[j-1].FT;
				}
			}
		}
		double cpuWorkEnergy = exeTime * users[i].CPU_COMP_POWER;
		double cpuIdleEnergy = (makespan - exeTime) * users[i].CPU_IDLE_POWER;
		double netSendEnergy = sendTime * users[i].NETWORK_SNED_POWER;
		double netRecvEnergy = recvTime * users[i].NETWORK_RECV_POWER;
		double netIdleEnergy = (makespan - sendTime - recvTime) * users[i].NETWORK_IDLE_POWER;
		double power = cpuWorkEnergy + cpuIdleEnergy + 
				netSendEnergy + netRecvEnergy + netIdleEnergy;
		users[i].power = power;
		return power;
	}
	
	/*
	 * 返回两者中较小值
	 */
	public double max(double a, double b) {
		return a > b ? a : b;
	}

	/*
	 * 随机生成一种策略
	 */
	public Molecule randomGenMolecule() {
		Molecule molecule = new Molecule();
		
		Map<Integer, Integer> structure = new HashMap<>();
		
		//组件0和COMPONENT_NUM-1默认在本地执行
		structure.put(0, 0);
		structure.put(COMPENENT_NUM-1, 0);
		
		for (int i = 1; i < COMPENENT_NUM-1; i++) {
			int pos = rand0And1();
			structure.put(i, pos);
		}
		molecule.setStructure(structure);
		return molecule;
	}
	
	/*
	 * 随机产生0或1
	 */
	public int rand0And1() {
		Random rand = new Random();
		
		return rand.nextDouble() > 0.5 ? 1 : 0;
	}
	
	/*
	 * 随机产生[a,b]之间的int值
	 */
	public int randAToB(int a, int b) {
		int num = (int)( Math.random() * ( b - a + 1 )) + a;
		return num;
	}

	/*
	 * 单分子碰撞
	 */
	public void ineff_coll_on_wall(int i, Molecule s, double[] ET, double RT_min) {
		Random rand = new Random();
		Molecule newS = new Molecule();
		
		int point = randAToB(1, COMPENENT_NUM-2);	//[1,COMPENENT_NUM-2]
		
		newS.getStructure().put(0, 0);
		newS.getStructure().put(COMPENENT_NUM-1, 0);
		
		// 执行单分子碰撞操作
		for (int j = 1; j <= COMPENENT_NUM-2; j++) {
			if (j == point) {
				newS.getStructure().put(j, 1 - s.getStructure().get(j));
			} else {
				newS.getStructure().put(j, s.getStructure().get(j));
			}
		}
		
		
		double newSPE = compPE(i, newS, ET, RT_min);
		
		double tempBuff = s.KE + s.PE - newSPE;
		if (tempBuff >= 0) {
			newS.PE = newSPE;

			// 产生随机数q属于[KELossRate,1]
			double q = rand.nextDouble() * (1 - KELossRate) + KELossRate;
			newS.KE = tempBuff * q;

			// 更新buffer
			buffer = buffer + tempBuff * (1 - q);

			moleculeList.remove(s);
			moleculeList.add(newS);
		}
	}

	/*
	 * 单分子分解
	 */
	public boolean decompose(int i, Molecule s, double[] ET, double RT_min) {
		boolean status = false;
		Random rand = new Random();

		// 分解后两个新分子
		Molecule newS1 = new Molecule();
		Molecule newS2 = new Molecule();
		
		int point = randAToB(1, COMPENENT_NUM-2); //[0,COMPENENT_NUM]-->[1,COMPENENT_NUM-2]
		
		newS1.getStructure().put(0, 0);
		newS1.getStructure().put(COMPENENT_NUM-1, 0);
		
		newS2.getStructure().put(0, 0);
		newS2.getStructure().put(COMPENENT_NUM-1, 0);
		
		for (int j = 1; j <= COMPENENT_NUM-2; j++) {
			if (j < point) {
				newS1.getStructure().put(j, s.getStructure().get(j)); // s1获得s的point之前的部分，后半部分随机生成
				newS2.getStructure().put(j, rand.nextInt(2));
			} else {
				newS2.getStructure().put(j, s.getStructure().get(j)); // s2获得s的point之后的部分，前部分随机生成
				newS1.getStructure().put(j, rand.nextInt(2));
			}
		}

		double newS1PE = compPE(i, newS1, ET, RT_min);
		double newS2PE = compPE(i, newS2, ET, RT_min);
		
		double tempBuff = s.KE + s.PE - newS1.PE - newS2.PE;
		if (tempBuff >= 0) {
			status = true;
			newS1.PE = newS1PE;
			newS2.PE = newS2PE;
			double p = rand.nextDouble();
			newS1.KE = tempBuff * p;
			newS2.KE = tempBuff * (1 - p);
			moleculeList.remove(s);
			moleculeList.add(newS1);
			moleculeList.add(newS2);
		} else if (tempBuff + buffer > 0) {
			status = true;
			newS1.PE = newS1PE;
			newS2.PE = newS2PE;
			double m1 = rand.nextDouble();
			double m2 = rand.nextDouble();
			double m3 = rand.nextDouble();
			double m4 = rand.nextDouble();
			newS1.KE = (tempBuff + buffer) * m1 * m2;
			newS2.KE = (tempBuff + buffer) * m3 * m4 * (1 - m1 * m2);
			buffer = tempBuff + buffer - newS1.KE - newS2.KE;
			moleculeList.remove(s);
			moleculeList.add(newS1);
			moleculeList.add(newS2);
		} else {
			status = false;
		}
		return status;
	}

	/*
	 * 分子间碰撞
	 */
	public void inter_ineff_coll(int i, Molecule s1, Molecule s2, double[] ET, double RT_min) {
		Random rand = new Random();
		Molecule newS1 = new Molecule();
		Molecule newS2 = new Molecule();

		// 执行碰撞操作
		// 选择两个不同的点进行分子间的碰撞操作
		int point1 = randAToB(1, COMPENENT_NUM-2); // [1,COMPENENT_NUM-2]
		int point2 = randAToB(1, COMPENENT_NUM-2);
		
		while (point1 == point2) {
			point2 = randAToB(1, COMPENENT_NUM-2);
		}
		
		newS1.getStructure().put(0, 0);
		newS1.getStructure().put(COMPENENT_NUM-1, 0);
		
		newS2.getStructure().put(0, 0);
		newS2.getStructure().put(COMPENENT_NUM-1, 0);
		
		for (int j = 1; j <= COMPENENT_NUM-2; j++) {
			if (j == point1 || j == point2) {
				newS1.getStructure().put(j, s2.getStructure().get(j));
				newS2.getStructure().put(j, s1.getStructure().get(j));
			} else {
				newS1.getStructure().put(j, s1.getStructure().get(j));
				newS2.getStructure().put(j, s2.getStructure().get(j));
			}
		}

		double newS1PE = compPE(i, newS1, ET, RT_min);
		double newS2PE = compPE(i, newS2, ET, RT_min);

		// 能耗守恒check
		double tempBuff = s1.KE + s1.PE + s2.KE + s2.PE - newS1PE - newS2PE;

		// 满足碰撞条件
		if (tempBuff >= 0) {

			double p = rand.nextDouble();
			newS1.PE = newS1PE;
			newS1.KE = tempBuff * p;

			newS2.PE = newS2PE;
			newS2.KE = tempBuff * (1 - p);

			moleculeList.remove(s1);
			moleculeList.remove(s2);
			moleculeList.add(newS1);
			moleculeList.add(newS2);
		}
	}

	/*
	 * 分子合成
	 */
	public boolean synthesis(int i, Molecule s1, Molecule s2, double[] ET, double RT_min) {

		Molecule newS = new Molecule();

		int point = randAToB(1, COMPENENT_NUM-2); 	//[1,COMPONENT-2]
		
		newS.getStructure().put(0, 0);
		newS.getStructure().put(COMPENENT_NUM-1, 0);
		
		// 合成操作
		for (int j = 1; j <= COMPENENT_NUM-2; j++) {
			if (j < point) {
				newS.getStructure().put(j, s1.getStructure().get(j));
			} else {
				newS.getStructure().put(j, s2.getStructure().get(j));
			}
		}

		double newSPE = compPE(i, newS, ET, RT_min);

		double tempBuff = s1.KE + s1.PE + s2.KE + s2.PE - newSPE;
		if (tempBuff >= 0) {
			newS.PE = newSPE;
			newS.KE = tempBuff;
			moleculeList.remove(s1);
			moleculeList.remove(s2);
			moleculeList.add(newS);
			return true;
		}
		return false;
	}

	/*
	 * 分子种群中选择具有最小势能的分子
	 */
	public Molecule getMinStructure(List<Molecule> moleculeList) {
		Molecule minStructure = new Molecule();
		int minIndex = Integer.MAX_VALUE;
		double minPE = Double.MAX_VALUE;
		for (int i = 0; i < moleculeList.size(); i++) {
			Molecule m = moleculeList.get(i);
			if (m.PE < minPE) {
				minPE = m.PE;
				minIndex = i;
			}
		}
		minStructure.setStructure(moleculeList.get(minIndex).getStructure());
		minStructure.setKE(moleculeList.get(minIndex).getKE());
		minStructure.setPE(moleculeList.get(minIndex).getPE());

		return minStructure;
	}
}
