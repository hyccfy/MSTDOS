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

public class SearchAdjust {

	private static final int USER_NUM = 500;
	private static final int VM_NUM = 200;
	private static final int Net = 6;
	private static final int COMPENENT_NUM = 4 + 2;			// 改变组件个数，记得改变配置文件（组件之间的依赖关系）
	User[] users = new User[USER_NUM + 1];			// 用户数组，下标从1开始, [1, USER_NUM]
	VM[] vms = new VM[VM_NUM + 1];			// 虚拟机数组，下标从1开始，[1, VM_NUM]

	public static void main(String[] args) {
		SearchAdjust wkcro = new SearchAdjust();
		wkcro.run();
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

			// 使用SA算法为用户得到最优迁移策略
			Molecule molecule = SA(i, RT_min);

			users[i].molecule = molecule;
			
			//System.out.println("\n用户" + i + "的最终迁移策略:");
			StringBuilder sb = new StringBuilder();
			for(int j = 1; j <= COMPENENT_NUM-2; j++) {
				Map<Integer, Integer> structure = users[i].molecule.getStructure();
				sb.append(structure.get(j));
			}			
			
			double makespan = compMakeSpan(i, molecule, RT_min);
			double power = compEnergy(i, molecule, RT_min);
			users[i].makespan = makespan;
			users[i].power = power;

			users[i].power = users[i].power / 1000;

			
			int size = users[i].cloudList.size();
			if (size != 0) {
				users[i].vm_index = vm_index;					
				int cloudlIdx = users[i].cloudList.get(users[i].cloudList.size()-1);
				double cloudFT = users[i].component[cloudlIdx].FT;
				updateRT(i, vm_index, cloudFT);
			}
			
			//printVmsRT();
			
			//System.out.println("-------------------------用户" + i + "-end------------------------\n");
		}

		System.out.println("----------------------------------------------------------");
		
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
		System.out.println("所有用户的平均时间：" + Double.valueOf(df.format(avgMakeSpan)));
		System.out.println("所有用户的平均能耗：" + Double.valueOf(df.format(avgPower)));
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

				URL dir = SearchAdjust.class.getResource(""); // file:/E:/workspace/post/bin/com/paper/alg/ch1/
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
						users[i].bandWidth = 1024 * Net / 8.0 ;
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


	DecimalFormat df = new DecimalFormat("#.0000"); // 用户格式化数据，double输出保留四位小数


	List<Molecule> moleculeList = new LinkedList<>();
	
	public Molecule SA(int i, double RT_min) {
		Molecule molecule = new Molecule();
		
		molecule.getStructure().put(0, 0);
		molecule.getStructure().put(COMPENENT_NUM-1, 0);
		for(int j = 1; j < COMPENENT_NUM-1; j++) {
			if (j <= 2) {
				molecule.getStructure().put(j, 0);
			}
			else{
				molecule.getStructure().put(j,1);
			}
		}
		
//		compMakeSpan(i, molecule, RT_min);
		
		return molecule;
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
		double PE = 0.8 * (molecule.makespan - T_min) / (T_max - T_min) + 
				0.2 * (molecule.power - E_min) / (E_max - E_min);
		
//		double PE = (molecule.makespan - T_min) /(T_max - T_min);
				
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

}
