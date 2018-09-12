#include<iostream>
#include<cstring>
#include<cmath>
#include<cstdio>
#include<algorithm>
#include<string.h>
#include<stdlib.h>
using namespace std;

//4类不同手机用户的不同模块在设备端的执行时间ms
double Td[4][4];

//不同模块在云端的执行时间ms
double Tc[4];

//不同手机用户的CPU/网络接口的活跃/空闲能耗mJ
double En[4][4];

//不同模块之间的传输大小（两模块位置不同才有）KB
double Da[5];

//传输时间 = 数据传输量 / 网络带宽
double Tt[5];

#define UserNum 500  //用户数

#define ServerNum 200   //服务器数

#define Bandwidth 8   //虚拟机带宽数Mbps

#define ModuleNum 4+2    //模块数

#define ChannelNum ServerNum //信道数

#define INF -100000
#define exp 0.00001
#define PhoneTypeNum 4
#define N 50000
#define M 100
double T[N];

int part[16][6]= {0};
int TimedotNum;     //划分时间点个数

/*
    模块结构体
    s 表示模块的开始执行时间
    e 表示模块的结束时间
    loc 表示模块的执行位置
*/
struct Module
{
    double s;
    double e;
    int loc;
};

/*
    用户结构体
    module 用来存储用户模块信息
    type 表示用户使用哪一类手机
*/
struct User
{
    Module module[6];
    int type;
} user[UserNum];

/*
    时间片结构体
    s 表示时间片开始时间
    e 表示时间片结束时间
    Snum 表示当前时间片内服务器占有数量
    Nnum 表示当前时间片内网络信道占有数量
    Slink 存储哪个用户的哪个模块在此时间片内占用服务器
    Nlink 存储哪个用户的哪两个模块之间的数据传输在此时间片内占用网络信道
    （0：存储用户     1：存储模块）
*/
struct Slot
{
    double s;
    double e;
    int Snum;
    int Nnum;
    int Slink[UserNum+5][2];
    int Nlink[UserNum+5][2];
} slot[N];

/*
    回报结构体
    value 表示回报值
    index 表示一个标记，用还得到以前的用户和模块
    way 表示采用哪种策略
*/
struct Reward
{
    double value;
    int index;
    int way;
};

void readfile();
void get_initpart();
void divide();
void get_exeTime(int index);
void get_SandN();                   //获取服务器占有链表和网络占有链表函数
double cmp(Reward a, Reward b);
double maxx(double x, double y);
void MDSA();        //多维搜索和调整函数
double Move(int useri,int modulei,int sloti);               //服务器冲突的模块迁移策略
double ModuleDelay(int useri,int modulenumi,int sloti);     //服务器冲突的延迟执行策略
double Change(int useri,int modulei,int sloti);             //网络冲突的位置调整策略
double FlowDelay(int useri,int modulenumi,int sloti);       //网络冲突的延迟执行策略
void output();


int main()
{
    readfile();
    get_initpart();
    get_SandN();
    //output();

    MDSA();
    output();
    return 0;
}

void readfile()
{
    FILE *fp;
    fp = fopen("data.txt","r");
    int i, j, k=0;
    char str[M];
    while(fgets(str, M, fp) != NULL)
    {
        if(k==4 || k==6 || k==11)
        {
            k++;
            continue;
        }
        char sstr[10];
        int l=0;
        j = 0;
        memset(sstr,10,sizeof 0);
        for(i=0;i<strlen(str);i++)
        {

            if(str[i]!=' ')
                sstr[l++]=str[i];
            else
            {
                if(k<4)
                    Td[k][j] = atof(sstr);
                else if(k<6)
                    Tc[j] = atof(sstr);
                else if(k<11)
                    En[k-7][j] = atof(sstr);
                else if(k<13)
                    Da[j] = atof(sstr);
                l = 0;
                j++;
                memset(sstr,10,sizeof 0);
            }
        }
        if(k<4)
            Td[k][j] = atof(sstr);
        else if(k<6)
            Tc[j] = atof(sstr);
        else if(k<11)
            En[k-7][j] = atof(sstr);
        else if(k<13)
            Da[j] = atof(sstr);
        k++;
        j = 0;
        cout<<str;
    }
    cout<<endl;
    fclose(fp);

    /*for(i=0; i<4; i++)
    {
        for(j=0; j<4; j++)
            cout<<Td[i][j]<<" ";
        cout<<endl;
    }
    for(i=0; i<4; i++)
        cout<<Tc[i]<<" ";
    cout<<endl;
    for(i=0; i<4; i++)
    {
        for(j=0; j<4; j++)
            cout<<En[i][j]<<" ";
        cout<<endl;
    }
    for(i=0; i<5; i++)
        cout<<Da[i]<<" ";
    cout<<endl;*/
}

/*
    求解初始划分函数
    （忽略资源和带宽的限制）
    从所有迁移策略中选择出一个最优的
*/
void get_initpart()
{
    divide();
    int i,j,k;
    for(i=0;i<5;i++)
        Tt[i]=Da[i]*1000/Bandwidth/1024/8.0;
    memset(T, 0, sizeof T);
    TimedotNum=0;
    for(i=0; i<UserNum; i++)
    {
        //int k=((int)rand())%5;
        //cout<<k<<endl;
        int type=i%PhoneTypeNum;
        double min=10000;
        int flag;
        for(j=0; j<16; j++)
        {
            double s=0;
            for(k=1; k<ModuleNum-1; k++)
            {
                if(part[j][k] != part[j][k-1])
                    s+=(1-part[j][k])*Td[type][k-1] + part[j][k]*Tc[k-1] + Tt[k-1];
                else
                    s+=(1-part[j][k])*Td[type][k-1] + part[j][k]*Tc[k-1];
            }
            if(part[j][k] != part[j][k-1])
                s+=Tt[k-1];

            if(min>s)
            {
                min = s;
                flag=j;
            }
        }

        /*cout<<min<<endl;
        for(j=0;j<6;j++)
            cout<<part[flag][j];
        cout<<endl;*/
        user[i].type=type;
        for(j=1; j<ModuleNum; j++)
        {
            user[i].module[j].s = 0;
            user[i].module[j].e = 0;
            user[i].module[j].loc = part[flag][j];
        }

        get_exeTime(i);
    }
}

/*
    迁移策略划分函数
    用来得到所有可能的模块迁移方案（16）
*/
void divide()
{
    for(int i=0; i<16; i++)
    {
        int l=0;
        int k=i;
        while(k)
        {
            part[i][l+1]=k%2;
            l++;
            k/=2;
        }
    }

    /*for(int i=0;i<16;i++)
    {
        for(int j=0;j<6;j++)
            cout<<part[i][j];
        cout<<endl;
    }*/
}

/*
    求解应用执行时间函数
*/
void get_exeTime(int index)
{
    int j;
    for(j=1; j<ModuleNum; j++)
    {
        if(user[index].module[j].loc != user[index].module[j-1].loc)
            user[index].module[j].s = user[index].module[j-1].e + Tt[j-1];
        else
            user[index].module[j].s = user[index].module[j-1].e;
        T[TimedotNum++] = user[index].module[j].s;

        if(j != ModuleNum-1)
            user[index].module[j].e = user[index].module[j].s +
                                      (1-user[index].module[j].loc)*Td[user[index].type][j-1] + user[index].module[j].loc*Tc[j-1];
        else
            user[index].module[j].e = user[index].module[j].s;
        T[TimedotNum++] = user[index].module[j].e;
    }
}

/*
    获取服务器占有链表和网络占有链表函数
*/
void get_SandN()
{
    int i, j, k;
    /*cout<<endl<<endl;
    for(i=0; i<TimedotNum; i++)
    {
    cout<<T[i]<<"--";
    }
    cout<<endl<<endl<<endl;*/
    sort(T, T+TimedotNum);
    j = 1;
    for(i=1; i<TimedotNum; i++)
    {
        //cout<<T[i]<<" ";
        T[j++] = T[i];
        if(fabs(T[j-2]-T[j-1]) < exp) j--;
    }
    //cout<<endl;
    TimedotNum = j;
    for(i=0; i<TimedotNum-1; i++)
    {
        slot[i].s = T[i];
        slot[i].e = T[i+1];
        slot[i].Snum = 0;
        slot[i].Nnum = 0;
    }

    for(i=0; i<UserNum; i++)
    {
        for(j=0; j<ModuleNum; j++)
        {
            if(user[i].module[j].loc==1)
            {
                for(k=0; k<TimedotNum-1; k++)
                {
                    if(user[i].module[j].s - slot[k].s <= exp && slot[k].e - user[i].module[j].e <= exp)
                    {
                        slot[k].Slink[slot[k].Snum][0] = i;
                        slot[k].Slink[slot[k].Snum][1] = j;
                        slot[k].Snum++;
                    }
                    if(user[i].module[j].e<slot[k].s)
                        break;
                }
            }

            if(j>0 && user[i].module[j-1].loc != user[i].module[j].loc)
            {
                for(k=0; k<TimedotNum-1; k++)
                {
                    if(user[i].module[j-1].e - slot[k].s <= exp && slot[k].e - user[i].module[j].s <= exp)
                    {
                        slot[k].Nlink[slot[k].Nnum][0] = i;
                        slot[k].Nlink[slot[k].Nnum][1] = j;
                        slot[k].Nnum++;
                    }
                    if(user[i].module[j].s<slot[k].s)
                        break;
                }
            }
        }
    }

}

/*
    多维搜索和调整函数
*/
void MDSA()
{
    int i,j,k;
    Reward reward[UserNum+5];
    int useri, modulei;
    double rewardmove, rewarddelay;
    for(i = 0; i < TimedotNum-1 ; i++)
    {
        //getchar();
        //cout<<"pan duan\n";
	int front = 0;
        if(slot[i].Snum > ServerNum)	//判断是否发生服务器冲突
        {
            //cout<<"get Reward\n";
            for(j=0; j < slot[i].Snum; j++)	    //对发生冲突的模块求解回报值
            {
                useri = slot[i].Slink[j][0];
                modulei = slot[i].Slink[j][1];
                rewardmove = Move(useri, modulei, i);   //将模块迁移到本地执行
                for(k=0; k<i; k++)      //判断迁移后是否产生网络冲突
                {
                    if(slot[i].Nnum > ChannelNum)
                        rewardmove = INF;
                }
                rewarddelay = ModuleDelay(useri, modulei, i);   //将模块延迟执行
                //cout<<rewardmove<<"   "<<rewarddelay<<endl;

                //选择回报值较大的，并记录采用的是哪种调整策略
                reward[j].value = maxx(rewardmove, rewarddelay);
                reward[j].index = j;
                reward[j].way = reward[j].value==rewardmove ? 0 : 1;
            }

            //cout<<"sou suo\n";
            sort(reward, reward+slot[i].Snum, cmp);	    //对获得的回报值进行从大到小排序
            /*for(j=0; j<slot[i].Snum; j++)
            cout<<reward[j].value<<" ";
            cout<<endl;*/

            front = slot[i].Snum - ServerNum;       //需要调整的模块数
            //cout<<front<<"  kaishi\n";
            for(j=0; j < front; j++)	    //对前front个模块进行调整
            {
                useri = slot[i].Slink[j][0];
                modulei = slot[i].Slink[j][1];
                //cout<<useri<<"  "<<modulei;
                if(reward[j].way == 0)	    //迁移回本地执行的策略
                {
                    //cout<<" Move\n";
                    user[useri].module[modulei].loc = 1 - user[useri].module[modulei].loc;
                    get_exeTime(useri);
                }
                else	    //延迟执行的策略
                {
                    //cout<<" ModuleDelay\n";
		    int t;
		    for(t=i; t<TimedotNum-1; t++)
 		    {
      		   	if(slot[t].Snum < ServerNum)
         		   break;
   		    }
		    double tt = slot[t].s - user[useri].module[modulei].s;
                    for(k=modulei; k<ModuleNum; k++)
                    {
                        user[useri].module[k].s +=  tt;
                        T[TimedotNum++] = user[useri].module[k].s;
                        user[useri].module[k].e +=  tt;
                        T[TimedotNum++] = user[useri].module[k].e;
                    }
                }
            }

            get_SandN();
            //cout<<"Server tiao zheng "<<slot[i].Snum<<" "<<slot[i].Nnum<<endl;
            //output();
        }

        if(slot[i].Nnum > ChannelNum)		//判断是否发生网络冲突
        {
            for(j=0; j < slot[i].Nnum; j++)	    //对发生冲突的模块求解回报值
            {
                useri = slot[i].Nlink[j][0];
                modulei = slot[i].Nlink[j][1];
                rewardmove = Change(useri, modulei, i);      //改变相邻模块的执行位置
                for(k=0; k<i; k++)          //判断迁移后是否产生服务器冲突
                {
                    if(slot[i].Snum > ServerNum)
                        rewardmove = INF;
                }
                rewarddelay = FlowDelay(useri, modulei, i);     //将模块延迟执行

                //选择回报值较大的，并记录采用的是哪种调整策略
                reward[j].value = maxx(rewardmove, rewarddelay);
                reward[j].index = j;
                reward[j].way = reward[j].value==rewardmove ? 0 : 1;
            }

            sort(reward, reward+slot[i].Nnum, cmp);     //对获得的回报值进行从大到小排序
            front = slot[i].Nnum - ChannelNum;      //需要调整的模块数
            for(j=0; j<front; j++)      //对前front个模块进行调整
            {
                useri = slot[i].Nlink[j][0];
                modulei = slot[i].Nlink[j][1];
                //cout<<useri<<"  "<<modulei;
                if(reward[j].way == 0)	        //迁移回本地执行的策略
                {
                    //cout<<" Move\n";
                    user[useri].module[modulei].loc = 1 - user[useri].module[modulei].loc;
                    get_exeTime(useri);
                }
                else	        //延迟执行的策略
                {
                    //cout<<" ModuleDelay\n";
		    int t;
		    for(t=i; t<TimedotNum-1; t++)
 		    {
      		   	if(slot[t].Nnum < ChannelNum)
         		   break;
   		    }
		    double tt = slot[t].s - user[useri].module[modulei].s;
                    for(k=modulei; k<ModuleNum; k++)
                    {
                        user[useri].module[k].s += tt;
                        T[TimedotNum++] = user[useri].module[k].s;
			user[useri].module[k].e += tt;
                        T[TimedotNum++] = user[useri].module[k].e;
                    }
                }

            }

            get_SandN();
            //cout<<"Network tiao zheng "<<slot[i].Snum<<" "<<slot[i].Nnum<<endl;
        }
	get_SandN();

	/*cout<<slot[i].s<<"++++++++++"<<slot[i].e<<endl;
	if(i != TimedotNum-2)
	    cout<<front<<"   "<<slot[i].Snum<<"   "<<slot[i].Nnum<<"   "<<slot[i+1].Snum<<
		"   "<<slot[i+1].Nnum<<"   "<<reward[0].way<<"   "<<reward[0].value<<endl;
 	if(slot[i].Snum > ServerNum)
	{
	    int x = slot[i].Slink[0][0];
	    int y = slot[i].Slink[0][1];
	    cout<<x<<"---"<<y<<"---"<<user[x].module[y].s<<"---"<<user[x].module[y].e<<endl<<endl;
	    for(j=0; j<slot[i].Snum; j++)
		cout<<slot[i].Slink[j][0]<<"+"<<slot[i].Slink[j][1]<<"  ";
	    cout<<endl;
	}*/
	/*if(slot[i].Snum > ServerNum)
	{
	    for(k=0; k<front; k++)
	        cout<<slot[i].Slink[k][0]<<" "<<slot[i].Slink[k][1]<<endl<<endl;
	    for(j=0; j<slot[i].Snum; j++)
		cout<<slot[i].Slink[j][0]<<"+"<<slot[i].Slink[j][1]<<"  ";
	    cout<<endl;
	}*/
    }
}

/*
    sort中的比较函数
    关键值大的排在前面（回报函数值）
*/
double cmp(Reward a, Reward b)
{
    return a.value > b.value;
}

/*
    最大值函数
    返回两者之间的较大值
*/
double maxx(double x, double y)
{
    return x > y? x: y;
}

/*
    服务器冲突的模块迁移策略
    将发生冲突的模块的执行位置进行改变
    求得相应的服务器和数据传输减少的占有时间以及应用延迟增加时间
    返回回报函数值
*/
double Move(int useri,int modulei,int sloti)
{
    double Tserver,Tnet,Tdelay;
    int type = user[useri].type;
    if(user[useri].module[modulei-1].loc==0)
    {
        Tserver = user[useri].module[modulei].e - slot[sloti].s;

        if(user[useri].module[modulei+1].loc==1)
        {
            Tnet = 0 - Tt[modulei];
            Tdelay = Td[type][modulei] + Tt[modulei] - Tt[modulei-1] - Tc[modulei];
        }
        else
        {
            Tnet = Tt[modulei];
            Tdelay = Td[type][modulei] - Tt[modulei] - Tt[modulei-1] - Tc[modulei];
        }
    }
    else
    {
        Tserver = user[useri].module[modulei].e - slot[sloti].s;

        if(user[useri].module[modulei+1].loc==1)
        {
            Tnet = 0 - Tt[modulei] - Tt[modulei-1] + (slot[sloti].s - user[useri].module[modulei-1].e);
            Tdelay = Td[type][modulei] + Tt[modulei] - Tt[modulei-1] - Tc[modulei];
        }
        else
        {
            Tnet = Tt[modulei] - Tt[modulei-1] + (slot[sloti].s - user[useri].module[modulei-1].e);
            Tdelay = Td[type][modulei] + Tt[modulei] - Tt[modulei] - Tc[modulei];
        }
    }

    //cout<<Tserver<<"  "<<Tnet<<"  "<<Tdelay<<endl;
    return Tserver + Tnet - Tdelay;
}

/*
    服务器冲突的延迟执行策略
    服务器执行时间和网络传输时间的增量为0
    延迟时间 = 最近一个没有发生服务器冲突的时间点 - 冲突开始时间点
    返回回报函数值
*/
double ModuleDelay(int useri,int modulenumi,int sloti)
{
    double Tserver,Tnet,Tdelay;
    Tserver = 0;
    Tnet = 0;
    //Tdelay = slot[sloti].e - slot[sloti].s;
    int i;
    for(i=sloti; i<TimedotNum-1; i++)
    {
        if(slot[i].Snum < ServerNum)
            break;
    }
    Tdelay = slot[i].s - slot[sloti].s;

    return Tserver + Tnet - Tdelay;
}

/*
    网络冲突的位置调整策略
    将flow邻接模块的执行位置进行改变
    求得相应的服务器和数据传输减少的占有时间以及应用延迟增加时间
    返回回报函数值
*/
double Change(int useri,int modulei,int sloti)
{
    double Tserver,Tnet,Tdelay;
    int type = user[useri].type;
    if(user[useri].module[modulei].loc==1)
    {
        Tserver = user[useri].module[modulei].e;

        if(user[useri].module[modulei+1].loc==1)
        {
            Tnet = Tt[modulei-1] - Tt[modulei] - (slot[sloti].s - user[useri].module[modulei-1].e);
            Tdelay = Td[type][modulei] + Tt[modulei] - Tt[modulei-1] - Tc[modulei];
        }
        else
        {
            Tnet = Tt[modulei-1] + Tt[modulei] - (slot[sloti].s - user[useri].module[modulei-1].e);
            Tdelay = Td[type][modulei] - Tt[modulei] - Tt[modulei-1] - Tc[modulei];
        }
    }
    else
    {
        Tserver = Tc[modulei] - (slot[sloti].s - user[useri].module[modulei-1].e);

        if(user[useri].module[modulei+1].loc==1)
        {
            Tnet = Tt[modulei] + Tt[modulei-1] - (slot[sloti].s - user[useri].module[modulei-1].e);
            Tdelay = Tc[modulei] - Tt[modulei] - Tt[modulei-1] - Td[type][modulei];
        }
        else
        {
            Tnet = Tt[modulei] - Tt[modulei-1] + (slot[sloti].s - user[useri].module[modulei-1].e);
            Tdelay = Tc[modulei] + Tt[modulei] - Tt[modulei-1] - Td[type][modulei];
        }
    }

    return Tserver + Tnet - Tdelay;
}

/*
    网络冲突的延迟执行策略
    服务器执行时间和数据传输时间的增量为0
    延迟时间 = 最近一个没有发生网络冲突的时间点 - 冲突开始时间点
    返回回报函数值
*/
double FlowDelay(int useri,int modulenumi,int sloti)
{
    double Tserver,Tnet,Tdelay;
    Tserver = 0;
    Tnet = 0;
    //Tdelay = slot[sloti].e - slot[sloti].s;
    int i;
    for(i=sloti; i<TimedotNum-1; i++)
    {
        if(slot[i].Nnum < ChannelNum)
            break;
    }
    Tdelay = slot[i].s - slot[sloti].s;

    return Tserver + Tnet - Tdelay;
}

/*
    输出函数
*/
void output()
{
    int i,j;
    double sum = 0;
    double SEn = 0;
    for(i=0; i<UserNum; i++)
    {
        /*cout<<"用户 "<<i<<" 的模块划分情况：\n";
        for(j=0; j<ModuleNum; j++)
        {
            cout<<"第 "<<j<<" 个模块的位置，开始时间，结束时间： ";
            cout<<user[i].module[j].loc<<"  "<<user[i].module[j].s<<"  "<<user[i].module[j].e<<endl;
        }
        cout<<endl<<endl;*/

        int T[4]={0};
        int type = user[i].type;
        for(j=1; j<ModuleNum; j++)
        {
            if(user[i].module[j].loc == 0)
            {
                T[0] += user[i].module[j].e - user[i].module[j].s;
                T[3] += user[i].module[j].e - user[i].module[j].s;
            }
            if(user[i].module[j].loc != user[i].module[j-1].loc)
            {
                T[2] += user[i].module[j].s - user[i].module[j-1].e;
                T[1] += user[i].module[j].s - user[i].module[j-1].e;
            }
            if(user[i].module[j].loc == 1)
            {
                T[1] += user[i].module[j].e - user[i].module[j].s;
                T[3] += user[i].module[j].e - user[i].module[j].s;
            }
        }
        SEn += T[0]*En[type][0] + T[1]*En[type][1] + T[2]*En[type][2] + T[3]*En[type][3];

        sum += user[i].module[ModuleNum-1].e - user[i].module[0].s;
    }
    printf("%.3lf\n\n", sum/UserNum);
    printf("%.3lf\n\n", SEn/UserNum/1000);

    /*for(i=0;i<TimedotNum;i++)
        cout<<T[i]<<" ";
    cout<<endl;*/

    /*cout<<"开始时间 结束时间 服务器占有数 信道占有数\n";
    for(i=0; i<TimedotNum-1; i++)
    {
        printf("%8.2lf %8.2lf %12d %10d\n",slot[i].s,slot[i].e,slot[i].Snum,slot[i].Nnum);
    }*/
}















