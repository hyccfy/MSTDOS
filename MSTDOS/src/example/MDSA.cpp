#include<iostream>
#include<cstring>
#include<cmath>
#include<cstdio>
#include<algorithm>
#include<string.h>
#include<stdlib.h>
using namespace std;

//4�಻ͬ�ֻ��û��Ĳ�ͬģ�����豸�˵�ִ��ʱ��ms
double Td[4][4];

//��ͬģ�����ƶ˵�ִ��ʱ��ms
double Tc[4];

//��ͬ�ֻ��û���CPU/����ӿڵĻ�Ծ/�����ܺ�mJ
double En[4][4];

//��ͬģ��֮��Ĵ����С����ģ��λ�ò�ͬ���У�KB
double Da[5];

//����ʱ�� = ���ݴ����� / �������
double Tt[5];

#define UserNum 500  //�û���

#define ServerNum 200   //��������

#define Bandwidth 8   //�����������Mbps

#define ModuleNum 4+2    //ģ����

#define ChannelNum ServerNum //�ŵ���

#define INF -100000
#define exp 0.00001
#define PhoneTypeNum 4
#define N 50000
#define M 100
double T[N];

int part[16][6]= {0};
int TimedotNum;     //����ʱ������

/*
    ģ��ṹ��
    s ��ʾģ��Ŀ�ʼִ��ʱ��
    e ��ʾģ��Ľ���ʱ��
    loc ��ʾģ���ִ��λ��
*/
struct Module
{
    double s;
    double e;
    int loc;
};

/*
    �û��ṹ��
    module �����洢�û�ģ����Ϣ
    type ��ʾ�û�ʹ����һ���ֻ�
*/
struct User
{
    Module module[6];
    int type;
} user[UserNum];

/*
    ʱ��Ƭ�ṹ��
    s ��ʾʱ��Ƭ��ʼʱ��
    e ��ʾʱ��Ƭ����ʱ��
    Snum ��ʾ��ǰʱ��Ƭ�ڷ�����ռ������
    Nnum ��ʾ��ǰʱ��Ƭ�������ŵ�ռ������
    Slink �洢�ĸ��û����ĸ�ģ���ڴ�ʱ��Ƭ��ռ�÷�����
    Nlink �洢�ĸ��û���������ģ��֮������ݴ����ڴ�ʱ��Ƭ��ռ�������ŵ�
    ��0���洢�û�     1���洢ģ�飩
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
    �ر��ṹ��
    value ��ʾ�ر�ֵ
    index ��ʾһ����ǣ��û��õ���ǰ���û���ģ��
    way ��ʾ�������ֲ���
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
void get_SandN();                   //��ȡ������ռ�����������ռ��������
double cmp(Reward a, Reward b);
double maxx(double x, double y);
void MDSA();        //��ά�����͵�������
double Move(int useri,int modulei,int sloti);               //��������ͻ��ģ��Ǩ�Ʋ���
double ModuleDelay(int useri,int modulenumi,int sloti);     //��������ͻ���ӳ�ִ�в���
double Change(int useri,int modulei,int sloti);             //�����ͻ��λ�õ�������
double FlowDelay(int useri,int modulenumi,int sloti);       //�����ͻ���ӳ�ִ�в���
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
    ����ʼ���ֺ���
    ��������Դ�ʹ�������ƣ�
    ������Ǩ�Ʋ�����ѡ���һ�����ŵ�
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
    Ǩ�Ʋ��Ի��ֺ���
    �����õ����п��ܵ�ģ��Ǩ�Ʒ�����16��
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
    ���Ӧ��ִ��ʱ�亯��
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
    ��ȡ������ռ�����������ռ��������
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
    ��ά�����͵�������
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
        if(slot[i].Snum > ServerNum)	//�ж��Ƿ�����������ͻ
        {
            //cout<<"get Reward\n";
            for(j=0; j < slot[i].Snum; j++)	    //�Է�����ͻ��ģ�����ر�ֵ
            {
                useri = slot[i].Slink[j][0];
                modulei = slot[i].Slink[j][1];
                rewardmove = Move(useri, modulei, i);   //��ģ��Ǩ�Ƶ�����ִ��
                for(k=0; k<i; k++)      //�ж�Ǩ�ƺ��Ƿ���������ͻ
                {
                    if(slot[i].Nnum > ChannelNum)
                        rewardmove = INF;
                }
                rewarddelay = ModuleDelay(useri, modulei, i);   //��ģ���ӳ�ִ��
                //cout<<rewardmove<<"   "<<rewarddelay<<endl;

                //ѡ��ر�ֵ�ϴ�ģ�����¼���õ������ֵ�������
                reward[j].value = maxx(rewardmove, rewarddelay);
                reward[j].index = j;
                reward[j].way = reward[j].value==rewardmove ? 0 : 1;
            }

            //cout<<"sou suo\n";
            sort(reward, reward+slot[i].Snum, cmp);	    //�Ի�õĻر�ֵ���дӴ�С����
            /*for(j=0; j<slot[i].Snum; j++)
            cout<<reward[j].value<<" ";
            cout<<endl;*/

            front = slot[i].Snum - ServerNum;       //��Ҫ������ģ����
            //cout<<front<<"  kaishi\n";
            for(j=0; j < front; j++)	    //��ǰfront��ģ����е���
            {
                useri = slot[i].Slink[j][0];
                modulei = slot[i].Slink[j][1];
                //cout<<useri<<"  "<<modulei;
                if(reward[j].way == 0)	    //Ǩ�ƻر���ִ�еĲ���
                {
                    //cout<<" Move\n";
                    user[useri].module[modulei].loc = 1 - user[useri].module[modulei].loc;
                    get_exeTime(useri);
                }
                else	    //�ӳ�ִ�еĲ���
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

        if(slot[i].Nnum > ChannelNum)		//�ж��Ƿ��������ͻ
        {
            for(j=0; j < slot[i].Nnum; j++)	    //�Է�����ͻ��ģ�����ر�ֵ
            {
                useri = slot[i].Nlink[j][0];
                modulei = slot[i].Nlink[j][1];
                rewardmove = Change(useri, modulei, i);      //�ı�����ģ���ִ��λ��
                for(k=0; k<i; k++)          //�ж�Ǩ�ƺ��Ƿ������������ͻ
                {
                    if(slot[i].Snum > ServerNum)
                        rewardmove = INF;
                }
                rewarddelay = FlowDelay(useri, modulei, i);     //��ģ���ӳ�ִ��

                //ѡ��ر�ֵ�ϴ�ģ�����¼���õ������ֵ�������
                reward[j].value = maxx(rewardmove, rewarddelay);
                reward[j].index = j;
                reward[j].way = reward[j].value==rewardmove ? 0 : 1;
            }

            sort(reward, reward+slot[i].Nnum, cmp);     //�Ի�õĻر�ֵ���дӴ�С����
            front = slot[i].Nnum - ChannelNum;      //��Ҫ������ģ����
            for(j=0; j<front; j++)      //��ǰfront��ģ����е���
            {
                useri = slot[i].Nlink[j][0];
                modulei = slot[i].Nlink[j][1];
                //cout<<useri<<"  "<<modulei;
                if(reward[j].way == 0)	        //Ǩ�ƻر���ִ�еĲ���
                {
                    //cout<<" Move\n";
                    user[useri].module[modulei].loc = 1 - user[useri].module[modulei].loc;
                    get_exeTime(useri);
                }
                else	        //�ӳ�ִ�еĲ���
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
    sort�еıȽϺ���
    �ؼ�ֵ�������ǰ�棨�ر�����ֵ��
*/
double cmp(Reward a, Reward b)
{
    return a.value > b.value;
}

/*
    ���ֵ����
    ��������֮��Ľϴ�ֵ
*/
double maxx(double x, double y)
{
    return x > y? x: y;
}

/*
    ��������ͻ��ģ��Ǩ�Ʋ���
    ��������ͻ��ģ���ִ��λ�ý��иı�
    �����Ӧ�ķ����������ݴ�����ٵ�ռ��ʱ���Լ�Ӧ���ӳ�����ʱ��
    ���ػر�����ֵ
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
    ��������ͻ���ӳ�ִ�в���
    ������ִ��ʱ������紫��ʱ�������Ϊ0
    �ӳ�ʱ�� = ���һ��û�з�����������ͻ��ʱ��� - ��ͻ��ʼʱ���
    ���ػر�����ֵ
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
    �����ͻ��λ�õ�������
    ��flow�ڽ�ģ���ִ��λ�ý��иı�
    �����Ӧ�ķ����������ݴ�����ٵ�ռ��ʱ���Լ�Ӧ���ӳ�����ʱ��
    ���ػر�����ֵ
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
    �����ͻ���ӳ�ִ�в���
    ������ִ��ʱ������ݴ���ʱ�������Ϊ0
    �ӳ�ʱ�� = ���һ��û�з��������ͻ��ʱ��� - ��ͻ��ʼʱ���
    ���ػر�����ֵ
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
    �������
*/
void output()
{
    int i,j;
    double sum = 0;
    double SEn = 0;
    for(i=0; i<UserNum; i++)
    {
        /*cout<<"�û� "<<i<<" ��ģ�黮�������\n";
        for(j=0; j<ModuleNum; j++)
        {
            cout<<"�� "<<j<<" ��ģ���λ�ã���ʼʱ�䣬����ʱ�䣺 ";
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

    /*cout<<"��ʼʱ�� ����ʱ�� ������ռ���� �ŵ�ռ����\n";
    for(i=0; i<TimedotNum-1; i++)
    {
        printf("%8.2lf %8.2lf %12d %10d\n",slot[i].s,slot[i].e,slot[i].Snum,slot[i].Nnum);
    }*/
}















