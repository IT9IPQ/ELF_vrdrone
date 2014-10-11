package com.g_zhang.p2pComm;


import com.elf_vrdrone.view.P2PVideoDisplayer;

import android.util.Log;

public class nvcP2PComm {

	static 
	{
        System.loadLibrary("wels");
		System.loadLibrary("ZGP2PComm");
	}
	public static boolean m_bInitOK = false;
	public static native String getVersion();
	
	//��ʼ�� P2P���ļ� 
	public static native int InitP2PServer(String sev1, String sev2, String sev3, String sev4, short sevPort, int myip);
	// ����ͨѶ��
	public static native void DestoryP2PComm();
	//ע�������豸�Ĳ�����ʶ��ͨ���豸���ʵ��
	
	//����µ��豸��ͨѶ��
	public static native int AddNewP2PDevice(String uid, String accpwd);
	//�޸��豸��Ϣ
	public static native int UpdateP2PDevice(int handle, String uid, String accpwd);
	//��ͨѶ��ɾ���豸
	public static native int DeleteP2PDevice(int handle);
	//�����豸״̬
	public static native int getP2PDeviceStatus(int handle);
	//��ȡ���һ�δ�����Ϣ
	public static native int getP2PDeviceLastError(int handle);
	//�������һ�δ�����Ϣ
	public static native int resetP2PDeviceLastError(int handle);
	//��ȡ����ģʽ
	public static native int getP2PDeviceLinkMode(int handle);
	//����������м����ݣ�����豸������ģʽ��Relay�£���������Ƶ��������Ҫ�����м̣�tmsec���м�ʱ�䳤�ȣ�Ŀǰ������>0
	public static native int reqP2PDeviceRelayData(int handle, int tmsec);
	//����������Ƶ������
	public static native int reqP2PDeviceAVMedia(int handle, int avmode);
	//���ƶԽ������� istalk = 0 �رգ� istalk-1 ����
	public static native int reqP2PDeviceTalk(int handle, int istalk);
	//�ر��豸����Ƶ������
	public static native int StopP2PDeviceAVMedia(int handle);
	//��ȡý��������
	public static native P2PDevMediaType readP2PDeviceMediaParams(int handle, P2PDevMediaType md);
	//���÷���֡��
	public static native int setP2PDevicePlayFps(int handle, int fps);
	//��ȡ�豸�����û���
	public static native int getP2PDeviceOnlineUserCnt(int handle);
	//��ȡ��ǰ�������������ͣ�ͨ��bit λ�����֣� ���� P2PCommDef �ļ����� P2PDATA_BITMK_VIDEO ...
	public static native int getP2PDeviceCurrLiveMode(int handle);
	//���� PCM 8K ������ �Խ����ݣ����ļ��ڲ�ѹ������...
	public static native int sendP2PDeviceTalkAudioData(int handle, byte[] pFrame, int len);
	//���� ý�����ָ��  ���� P2PCommDef �ļ����� IPCP_PPPCTLCMD_VIDEO_RESLU ... ... 
	public static native int sendP2PDeviceMediaCmd(int handle, int cmd, int vlu);
	//����¼���ṩ¼���ļ��� ... ... 
	public static native int recordP2PDeviceStart(int handle, String fl);
	//ֹͣ¼��
	public static native int recordP2PDeviceStop(int handle);
	//��ȡ¼��״̬
	public static native int getRecordStatus(int handle);
	//����lan�����豸
	public static native int StartSehP2PDeviceStatus();
	//���� ������Ϣ֪ͨ almmsg = P2PCommDef �ļ����� IPCP_PUSHMSG_ALM, Ϊ���գ� 0 ������
	public static native int setupP2PDevicePushMsg(int handle, int almmsg);
	//����һ�����ս�ͼ
	public static native int requP2PDeviceSnapshotImg(int handle);
	
	public static native int reConnectDevice(int handle);
	public static native int closeDeviceConnection(int handle);
	
	public static native int ASFPlayFile(int playerIndex, String file);
	public static native int ASFisPlaying(int playerIndex);
	public static native int ASFisRuning(int playerIndex);
	public static native int ASFGetPlayFileTimeLong(int playerIndex);
	public static native int ASFGotoTimeV(int playerIndex, int tmv);
	public static native int ASFPausePlay(int playerIndex, int bPlay);
	public static native int ASFStopPlay(int playerIndex);
	
	public static native int StartWIFIConfig(String ssid, String pwd, String uid, int nauth, int nenc, int ntype);
	public static native int GetWIFIConfigStatus();
	public static native int StopWIFIConfig();
	
	public static native int setVideoEncodeInfor(int handle,  int encMode, int encVlu, int EncModeIndex, int reslu, int stu);
	public static native int setVideoCodecLock(int handle,  int frmw, int frmh, int bytepersec);

	public static native int reqDevAlarmConfig(int handle);
	public static native P2PDataAlarmConfig readP2PDevAlarmConfigData(int handle, P2PDataAlarmConfig md);
	public static native int setP2PDevAlarmConfig(int handle, P2PDataAlarmConfig md);
	
	public static native int reqDevWifiInfor(int handle);
	public static native P2PDataWifiInfor readP2PDevWifiInforData(int handle, P2PDataWifiInfor md);
	public static native P2PDataWifiApItem readP2PDevWifiApItem(int handle, int apidx, P2PDataWifiApItem md);
	public static native int startP2PDevWifiScan(int handle);
	public static native int setP2PDevWifiDisconnect(int handle);
	public static native int setP2PDevWifiConnect(int handle, String ssid, String pwd, int nauth, int nenc, int ntype);
	
	public static native int reqP2PDevSDCardRecConfig(int handle);
	public static native P2PDataSDCardRecCfg readP2PDevSDCardRecCfg(int handle, P2PDataSDCardRecCfg md);
	public static native int setP2PDevSDCardRecCfg(int handle, P2PDataSDCardRecCfg md);
	public static native int reqP2PSDCardAllFiles(int handle);
	public static native P2PDataRecFileInfor readP2PDevRecFileData(int handle, P2PDataRecFileInfor md);
	public static native P2PDataRecFileItem readP2PDevRecFileItem(int handle, int flidx, P2PDataRecFileItem md);
	public static native int delDevSDCardFile(int handle, String fl);
	public static native int isDevSDCardPlayerFree(int handle);
	public static native int playDevSDCardRecFile(int handle, String fl, int nodelay);
	public static native P2PDataRecPlayMediaInfor readP2PDevRecPlayMediaInfor(int handle,P2PDataRecPlayMediaInfor md);
	public static native int playDevSDCardRecFileGoto(int handle, int tmv);
	public static native int stopDevSDCardRecFilePlay(int handle);
	
	public static native int setDevAccessPwd(int handle, String newpwd, String oldpwd);
	public static native int doDevReboot(int handle);
	public static native int getDevP2PVersion(int handle);
	
	public static native int reqDevIRLedConfig(int handle);
	public static native P2PDataIRLedConfig readP2PDevIRLedConfigData(int handle, P2PDataIRLedConfig md);
	public static native int setP2PDevIRLedConfig(int handle, P2PDataIRLedConfig md);
	public static native int setDevIRLedOn(int handle, int ison);
	
    public static void Callback_OnP2PDeviceStatusChanged(int handle, int status)
    {
    	//Log.d("nvcP2PComm", String.format("Callback_OnP2PDeviceStatusChanged %d - %d", handle, status));
    	//֪ͨ�豸״̬ ���� P2PCommDef DEVCLTSTU_LOGIN �ȵȶ���
    	if (P2PVideoDisplayer.getInstance() != null )
    		P2PVideoDisplayer.getInstance().UpdateP2PDevLinkStatus(handle, status);
    }
    
    public static void Callback_OnP2PDevMediaParamsChanged(int handle)
    {
    	//Log.d("nvcP2PComm", String.format("Callback_OnP2PDevMediaParamsChanged %d", handle));
    	//֪ͨ�豸ý������仯 readP2PDeviceMediaParams ��ȡ�µ����ݣ���ʽ�����ļ� P2PDevMediaType ����
    	if (P2PVideoDisplayer.getInstance() != null )
    		P2PVideoDisplayer.getInstance().OnRecvMediaInfor(handle);
    }
    
    public static void Callback_OnP2PDevRecvVideoData(int handle, int frmid, int tmv, int wd, int hi, int pkLen, byte[] pFrame)
    {
    	//Log.d("nvcP2PComm", String.format("Callback_VideoFrame Dev:%d Frmid:%d (%dx%d), Tmv %d, Len: %d", handle, frmid, wd, hi, tmv, pFrame.length));
    	//���յ���Ƶ֡���� tmv ʱ����� wd , hi �ֱ��ʣ� pFrame ���ݡ�
    	if (P2PVideoDisplayer.getInstance() != null )
    		P2PVideoDisplayer.getInstance().OnRecvVideoData(handle, frmid, tmv, wd, hi, pFrame);	
    }
    
    public static void Callback_OnP2PDevRecvVideoData(int a, int b, int c, int d, int e, byte[] f){
    	
    }
    
    public static void Callback_OnP2PDevRecvAudioData(int handle, int frmid, int tmv, byte[] pFrame)
    {
    	//���յ���Ƶ֡���� tmv ʱ�����pFrame �ѽ����PCM��Ƶ ���ݡ� 
    	//��ʹ�� AudioTrack ����PCM ����.
    	//Log.d("nvcP2PComm", String.format("Callback_AudioFrame Dev:%d Frmid:%d, Tmv %d, Len: %d", handle, frmid, tmv, pFrame.length));
    	if (P2PVideoDisplayer.getInstance() != null )
    		P2PVideoDisplayer.getInstance().OnRecvAudioData(handle, frmid, tmv, pFrame);	
    }

    public static void Callback_OnP2PDevRecvOtherData(int handle, int frmid, byte[] pFrame)
    {
    	//����
    	//Log.d("nvcP2PComm", String.format("Callback_OtherData Dev:%d Frmid:%d, Len: %d", handle, frmid, pFrame.length));
    }
    
    public static void Callback_OnP2PDevRecordStatusChanged(int handle, String flnm, int isRec)
    {
    	//¼��״̬�仯�� Handle �豸����� flnm �ļ����� isRec �Ƿ���¼�� 
    	if (P2PVideoDisplayer.getInstance() != null )
    		P2PVideoDisplayer.getInstance().OnRecordStatusChanged(handle, flnm, isRec);
    }
    
    public static void Callback_OnRecvP2PDevSehResult(String uid, int DevType, int Wanip, int Wanport, int Lanip, int Lanport, int LanAppPort, int isLanSeh)
    {
    	//�������������ע�� IP��ַ �� Port�˿ڶ�������Bit˳�� 
    	if (P2PVideoDisplayer.getInstance() != null )
    		P2PVideoDisplayer.getInstance().OnRecvNewSehItem(uid, DevType, Wanip, Wanport, Lanip, Lanport, LanAppPort, isLanSeh);   	
    	//Log.d("nvcP2PComm", String.format("Callback_OnRecvP2PDevSehResult %s, DevType:%d, %x : %d, %x : %d AppPort %d, ISLan %d \n", uid, DevType, Wanip, Wanport, Lanip, Lanport, LanAppPort, isLanSeh));
    }

    public static void Callback_OnRecvP2PDevImageFrame(int handle, int almtmv, int almtype, int imgtype, int imgcnt, int imgindex, byte[] pImage)
    {
    	Log.d("nvcP2PComm", String.format("Callback_OnRecvP2PDevImageFrame AlmType:%d, Time %d, ImgCnt %d, ImgIndex %d \n", almtype, almtmv, imgcnt, imgindex));
    	if ( almtype == P2PCommDef.IPCP_ALMTYPE_SNAPSHOT ){
    		//��ͼ��������
    		if (P2PVideoDisplayer.getInstance() != null )
    			P2PVideoDisplayer.getInstance().onRecvImageFrame(pImage);
    	}
    	else{
    		//������ͼ�� ÿ��3�ţ����λش���
    	}
    }
    
    public static void CallBack_OnRecvP2PPlayerStatueChanged(int idx, int stu)
    {
    	Log.d("nvcP2PComm", String.format("CallBack_OnRecvP2PPlayerStatueChanged :%d, Status: %d \n", idx, stu));
    }
    
    public static void CallBack_OnRecvP2PPlayerFrameData(int idx, int nStream, byte[] pFrame, int len, int tmv)
    {
    	Log.d("nvcP2PComm", String.format("CallBack_OnRecvP2PPlayerFrameData :%d, Stream: %d, Len %d, Tmv %d \n", idx, nStream, len, tmv));
    }
    
    public static void CallBack_OnRecvP2PDevConfigData(int handle, int dtype)
    {
    	Log.d("nvcP2PComm", String.format("CallBack_OnRecvP2PDevConfigData :%d, Type: %d\n", handle, dtype));
    }
    
    public static void CallBack_OnRecvP2PSDCardPlayFrame(int handle, int plyChn, int nStream, byte[] pFrame, int len, int tmv)
    {
    	Log.d("nvcP2PComm", String.format("CallBack_OnRecvP2PSDCardPlayFrame :%d, Stream: %d, Len %d, Tmv %d \n", plyChn, nStream, len, tmv));
    }   
}
