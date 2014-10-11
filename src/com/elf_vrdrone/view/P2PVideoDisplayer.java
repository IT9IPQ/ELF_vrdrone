package com.elf_vrdrone.view;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.ElecFreaks.ELF_vrdrone.R;
import com.elf_vrdrone.control.AudioParam;
import com.elf_vrdrone.control.AudioPlayer;
import com.g_zhang.p2pComm.P2PCommDef;
import com.g_zhang.p2pComm.P2PDevMediaType;
import com.g_zhang.p2pComm.nvcP2PComm;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioFormat;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class P2PVideoDisplayer {
	ImageView imageViewLeftFrame;
	ImageView imageViewRightFrame;
	ImageView imageViewLeftCursor;
	ImageView imageViewRightCursor;
	ImageView imageViewLeftGraduation;
	ImageView imageViewRightGraduation;
	ImageView imageViewCamera;
	ImageView imageViewAudio;
	ImageView imageViewLiveRec;
	ImageView imageViewButtonScan;
	TextView textViewCamStatus;
	FrameLayout framLayoutProgressBar;
	SlidingDrawer slidingDrawerMenus;
	AudioPlayer mAudioPlayer;				// ������
	
	private Matrix imageViewGraduationMatrix;
	
	private String p2p_uid = ""; //"ZGCS_AAAA0_xxxx"; 
	private final String p2p_pwd = "8888"; //"xxxx";
	
	public static final int MSG_LINK_CHANGED = 0;
	public static final int MSG_VIDEO_DATA = 1;
	public static final int MSG_AUDIO_DATA = 2;
	public static final int MSG_TIMER = 3;
	public static final int MSG_UPDATEMEDIA = 4;
	public static final int MSG_RECEVT = 5;
	public static final int MSG_SNAPSHOT_DATA = 6;
	
	public static final int REQUEST_NON = 0;
	public static final int REQUEST_CONNECT = 1;
	public static final int REQUEST_START_VIDEO = 2;
	public static final int REQUEST_RECV_VIDEO = 3;
	
	private Handler m_MsgHandler;
	private byte[] m_pVideoFrame = null;
	private int m_nVideoFrameLen = 0;
	private P2PDevMediaType m_MediaType = null;
	private AlertDialog alertDialog = null;
	private AlertDialog progressBarLoginInDialog = null;
	private AlertDialog progressBarLoginOutDialog = null;
	
	private int p2p_nDevHandle = 0;
	private int p2p_nDevStatus = 0;
	private int p2p_requestStatus = REQUEST_NON;
	private boolean p2p_bStart = false;
	private boolean p2p_needaudio = false;
	private boolean m_bRecing = false;
	private boolean m_isConnected = false;
	private boolean waitConnectProgressBarIsVisible = false;
	
	private int p2p_result;
	
	private Context context;
	private View view;
	private static P2PVideoDisplayer ins = null;
	
	private P2PCameraListAdapter m_cameraAdapter;
	
	public boolean isConnected(){
		return m_isConnected;
	}
	
	public boolean isStarted(){
		return p2p_bStart;
	}
	
	private P2PVideoDisplayer() {
		// TODO Auto-generated constructor stub
		m_MsgHandler = new Handler(){
		    public void handleMessage(Message msg){
		    	switch (msg.what){
				  	case MSG_LINK_CHANGED:
				  		ProcessLinkStatus(msg);
				  	break;
				  	case MSG_VIDEO_DATA:
				  		ProcessVideoMsg(msg);
				    break;
				  	case MSG_AUDIO_DATA:
				  		ProcessAudioMsg(msg);
				    break;
				  	case MSG_TIMER:
					  //ProcessOnTimer();
				    break;
				  	case MSG_UPDATEMEDIA:
				  		UpdateCurrentMediaInfor();
				    break;
				  	case MSG_RECEVT:
				  		DoRecStatusChangedEvt();
				  	break;
				  	case MSG_SNAPSHOT_DATA:
				  		saveSnapshot(msg);
				    break;
				  	default:
				  		return;
			    }
		    }
		};
		// �õ�һ����Ƶ������
		mAudioPlayer = new AudioPlayer(8000, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
		mAudioPlayer.init();
	}
	// ����ģʽ
	public static P2PVideoDisplayer getInstance(){
		if(ins == null){
			ins = new P2PVideoDisplayer();
		}
		return ins;
	}
	
	public void onDestroy(){
		ins = null;
		if (p2p_nDevHandle != 0){ // ɾ���豸
			nvcP2PComm.DeleteP2PDevice(p2p_nDevHandle);
			m_isConnected = false;
			mAudioPlayer.release();
		}
	}
	
	public void startOnClick(View v){	// ������ֹͣ��Ƶ
		if (!ISDeviceOnline()){
			ShowMsg(context.getResources().getString(R.string.offline));
			return ;
		}
		if ( !p2p_bStart ){
			showConnectWaitProgressBar();
			p2p_requestStatus = REQUEST_START_VIDEO; // ���ΪҪ������Ƶ
			StartP2PLiveVideo();
		}
		else{
			StopP2PLiveVideo();
			if(!MainActivity.videoIsInMainActivity){
				if(imageViewLeftFrame != null && imageViewRightFrame != null){
					imageViewLeftFrame.setImageDrawable(context.getResources().getDrawable(R.drawable.nothing));
					imageViewRightFrame.setImageDrawable(context.getResources().getDrawable(R.drawable.nothing));
					imageViewCamera.setImageDrawable(context.getResources().getDrawable(R.drawable.camera_close));
				}
			}else{
				MainActivity.getInstance().mainImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.nothing));
				MainActivity.getInstance().setJoyImageViewsAlpha(1.0f);
				imageViewCamera.setImageDrawable(context.getResources().getDrawable(R.drawable.btn_play_image));
			}
		}
	}
	
	public boolean slidingDrawerIsOpened(){	// �����Ƿ��
		return slidingDrawerMenus.isOpened();
	}
	
	public void openSlidingDrawer(){		// �򿪳���
		slidingDrawerMenus.open();
	}
	
	public void closeSlidingDrawer(){		// �رճ���
		slidingDrawerMenus.close();
	}
	
	public boolean getWaitConnectProgressBarStatus(){	// �õ���ת��������״̬
		return waitConnectProgressBarIsVisible;
	}
	
	public void showConnectWaitProgressBar(){			// ��ʾ��ת������
		waitConnectProgressBarIsVisible = true;
		framLayoutProgressBar.setVisibility(FrameLayout.VISIBLE);
	}
	
	public void cancelConnectWatiProgressBar(){			// ������ת������
		waitConnectProgressBarIsVisible = false;
		framLayoutProgressBar.setVisibility(FrameLayout.INVISIBLE);
	}
	
	public void scanOnClick(View v){					// ɨ���豸
		if(p2p_result < 0){
			ShowMsg(context.getResources().getString(R.string.please_connect_camera));
			return ;
		}
		if(!m_isConnected){								// ����һ���Ի���
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			LayoutInflater inflater = LayoutInflater.from(context);
			View view = inflater.inflate(R.layout.device_list, null);	// �õ�listView����
			
			m_cameraAdapter.clearP2PCamera();			// Ҫ��ʼɨ���ˣ������һ��ɨ��ĺۼ�
			StartSearch();								// ��ʼɨ��
			ListView deviceListView = (ListView) view.findViewById(R.id.listViewDevice);
			deviceListView.setAdapter(m_cameraAdapter);
			deviceListView.setOnItemClickListener(new OnItemClickListener() {
														// ����listView������
				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					alertDialog.cancel();
					p2p_uid = ((P2PCameraListAdapter.P2PCameraInfo)m_cameraAdapter.getItem(position)).uid;
					showConnectWaitProgressBar();		// ��ѡ��һ���豸ʱ��ʾ��ת��������ʾ��������
					// ���һ����Ҫ���ӵĶ���, �����ӳɹ�ʱ������
					p2p_requestStatus = REQUEST_CONNECT;
					addNewP2PDevice();	// ��ʼ�����豸
				}
			});
			builder.setView(view);
			alertDialog = builder.create();
			alertDialog.show();
		}else{			// ���Ѿ���������ʱҪȡ������
			new Thread(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					deleteP2PDevice(); // ɾ���豸
					p2p_nDevHandle = 0;
				}
			}).start();
			p2p_bStart = false;
			if(!MainActivity.videoIsInMainActivity){
				if(imageViewLeftFrame != null && imageViewRightFrame != null){
					textViewCamStatus.setText("---");
					imageViewCamera.setImageDrawable(context.getResources().getDrawable(R.drawable.camera_close));
					imageViewLeftFrame.setImageDrawable(context.getResources().getDrawable(R.drawable.nothing));
					imageViewRightFrame.setImageDrawable(context.getResources().getDrawable(R.drawable.nothing));
					imageViewAudio.setImageDrawable(context.getResources().getDrawable(R.drawable.audio_close));
				}
			}else{
				imageViewCamera.setImageDrawable(context.getResources().getDrawable(R.drawable.btn_play_image));
				MainActivity.getInstance().mainImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.nothing));
				MainActivity.getInstance().setJoyImageViewsAlpha(1.0f);
			}
			imageViewButtonScan.setImageDrawable(context.getResources().getDrawable(R.drawable.btn_camera_close));
		}
	}
	
	public void setParameter(Context context, View v, int p2p_result){
		this.context = context;
		this.view = v;
		this.p2p_result = p2p_result;
		
		m_cameraAdapter = new P2PCameraListAdapter(context);
		
		if(!MainActivity.videoIsInMainActivity){
			imageViewLeftFrame = (ImageView) view.findViewById(R.id.imageViewLeftFrame);
			imageViewRightFrame = (ImageView) view.findViewById(R.id.imageViewRightFrame);
			imageViewLeftCursor = (ImageView) view.findViewById(R.id.imageViewLeftCursor);
			imageViewRightCursor = (ImageView) view.findViewById(R.id.imageViewRightCursor);
			imageViewLeftGraduation = (ImageView) view.findViewById(R.id.imageViewLeftGraduation);
			imageViewRightGraduation = (ImageView) view.findViewById(R.id.imageViewRightGraduation);
			imageViewCamera = (ImageView) view.findViewById(R.id.imageViewCamera);
			imageViewAudio = (ImageView) view.findViewById(R.id.imageViewAudio);
			imageViewLiveRec = (ImageView) view.findViewById(R.id.imageViewLiveRec);
			
			imageViewButtonScan = (ImageView) view.findViewById(R.id.imageViewScanCamera);
			textViewCamStatus = (TextView) view.findViewById(R.id.textViewCamStatus);
			framLayoutProgressBar = (FrameLayout) view.findViewById(R.id.framLayoutProgressBar);
			
			if(p2p_bStart)
				imageViewCamera.setImageDrawable(context.getResources().getDrawable(R.drawable.camera_open));
			else{
				imageViewCamera.setImageDrawable(context.getResources().getDrawable(R.drawable.camera_close));
				imageViewLeftFrame.setImageDrawable(context.getResources().getDrawable(R.drawable.nothing));
				imageViewRightFrame.setImageDrawable(context.getResources().getDrawable(R.drawable.nothing));
			}
		}else{
			imageViewCamera = (ImageView) view.findViewById(R.id.mainImageViewCamera);
			imageViewButtonScan = (ImageView) view.findViewById(R.id.mainImageViewScanCamera);
			framLayoutProgressBar = (FrameLayout) view.findViewById(R.id.mainFramLayoutProgressBar);
			if(p2p_bStart)
				imageViewCamera.setImageDrawable(context.getResources().getDrawable(R.drawable.btn_pause_image));
			else{
				imageViewCamera.setImageDrawable(context.getResources().getDrawable(R.drawable.btn_play_image));
				MainActivity.getInstance().mainImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.nothing));
				MainActivity.getInstance().setJoyImageViewsAlpha(1.0f);
			}
		}
		if(m_isConnected)
			imageViewButtonScan.setImageDrawable(context.getResources().getDrawable(R.drawable.btn_camera_open));
		else
			imageViewButtonScan.setImageDrawable(context.getResources().getDrawable(R.drawable.btn_camera_close));
		
		slidingDrawerMenus = (SlidingDrawer) view.findViewById(R.id.slidingDrawerMenus);
		
		imageViewGraduationMatrix = new Matrix();
		
		if(!MainActivity.videoIsInMainActivity){
			imageViewLeftGraduation.setImageMatrix(imageViewGraduationMatrix);
			imageViewRightGraduation.setImageMatrix(imageViewGraduationMatrix);
		}
	}
	
	public void setGraduationRotation(float r){
		imageViewLeftCursor.setRotation(r);
		imageViewRightCursor.setRotation(r);
		imageViewLeftGraduation.setRotation(r);
		imageViewRightGraduation.setRotation(r);
	}
	
	public void setGraduationMatrix(Matrix m){
		imageViewGraduationMatrix = m;
		imageViewLeftGraduation.setImageMatrix(m);
		imageViewRightGraduation.setImageMatrix(m);
	}
	
	public void snapshotImage(){
		if (!ISDeviceOnline()){
			ShowMsg(context.getResources().getString(R.string.offline));
			return ;
		}
		if(p2p_nDevHandle != 0)
			nvcP2PComm.requP2PDeviceSnapshotImg(p2p_nDevHandle);
	}
	
	public Matrix getGraduationMatrix(){
		return imageViewGraduationMatrix;
	}
	
	private boolean hasSdcard(){
		// �ж�sdcard�Ƿ����
		String status = Environment.getExternalStorageState();
		if (!status.equals(Environment.MEDIA_MOUNTED)) {
			ShowMsg(context.getResources().getString(R.string.sdcard_not_exist));
			return false;
		}
		return true;
	}
	
	private void saveSnapshot(Message msg) {
		// TODO Auto-generated method stub
		byte[] pImage = (byte[])msg.obj;
		
		if(!hasSdcard()){
			return ;
		}
		
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
		Date curDate = new Date(System.currentTimeMillis());//��ȡ��ǰʱ�� 
		String strDate = formatter.format(curDate);
		String picName = strDate + ".jpg";
		String picPath = "/sdcard/" + p2p_uid + "/picture/";
		String picWholePath = picPath + picName;
		try {
			File file = new File(picPath);
			if(!file.exists()){
				file.mkdirs();
			}
			FileOutputStream fos = new FileOutputStream(picWholePath);
			fos.write(pImage);
			fos.close();
			ShowMsg(picWholePath);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.out.printf("��ͼ:len=%d\n", pImage.length);
	}
	
	public void addNewP2PDevice(){
		p2p_nDevHandle = nvcP2PComm.AddNewP2PDevice(p2p_uid, p2p_pwd);
	}
	
	public void ProcessAudioMsg(Message msg) {
		// TODO Auto-generated method stub
		byte[] pFrame = (byte[]) msg.obj;
		
		mAudioPlayer.playAudioTrack(pFrame, 0, pFrame.length);
		//System.out.printf("ProcessAudioMsg:dataLen = %d\n", pFrame.length);
	}

	public void deleteP2PDevice(){
		if(p2p_nDevHandle != 0){
			nvcP2PComm.DeleteP2PDevice(p2p_nDevHandle);
			m_isConnected = false;
			p2p_uid = "";
		}
	}
  
	public void ShowMsg(String msg){
		Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
	}
  
	public void StartP2PLiveVideo(){
		if (!this.ISDeviceOnline()){
			this.ShowMsg(context.getResources().getString(R.string.offline));
			return ;
		}
		int lnk = nvcP2PComm.getP2PDeviceLinkMode(this.p2p_nDevHandle);
		if ( lnk == P2PCommDef.DEVCLTLLNK_RELAY ){
			int rs = nvcP2PComm.reqP2PDeviceRelayData(this.p2p_nDevHandle, 120) ;
			if ( rs < 1 ){
				Log.d("P2PCam", String.format("nvcP2PComm.reqP2PDeviceRelayData(%d) : Error %d", p2p_nDevHandle, rs));
			}
		}

		int avmode = 0;
		avmode |= P2PCommDef.P2PDATA_BITMK_VIDEO;
		avmode |= P2PCommDef.P2PDATA_BITMK_AUDIO;

		int rs  = nvcP2PComm.reqP2PDeviceAVMedia(this.p2p_nDevHandle, avmode);
		if (rs != 0 ){
			Log.d("P2PCam", String.format("nvcP2PComm.reqP2PDeviceAVMedia(%d) : Error %d", p2p_nDevHandle, rs));
			return ;
		}
		// �����ڿ�����Ƶ��ʱ����ʱ���� reqP2PDeviceAVMedia(this.p2p_nDevHandle, avmode); ��������Ƶ������Ҫ��ֹͣ��Ƶ
		Log.d("P2PCam", String.format("RequLiveData(%d) : Mode 0x%x,  Error %d", p2p_nDevHandle, avmode, rs));
		p2p_bStart = true;
		p2p_needaudio = false;
		if(!MainActivity.videoIsInMainActivity)
			imageViewAudio.setImageDrawable(context.getResources().getDrawable(R.drawable.audio_open));
		
		SetVideoBrightness(90);
	}
	
	public void StopP2PLiveVideo(){
		this.StopRecord();
  	
		if ( !this.ISDeviceOnline()) return ;
		nvcP2PComm.reqP2PDeviceAVMedia(this.p2p_nDevHandle, 0);
		nvcP2PComm.reqP2PDeviceTalk(this.p2p_nDevHandle, 0);	//�رնԽ�
		
		p2p_bStart = false;
		p2p_needaudio = true;
		if(!MainActivity.videoIsInMainActivity)
			imageViewAudio.setImageDrawable(context.getResources().getDrawable(R.drawable.audio_close));
	}
  
	public void needAudio(View v){
		int avmode = 0;
		
		if (!ISDeviceOnline()){
			ShowMsg(context.getResources().getString(R.string.offline));
			return ;
		}
		if (p2p_needaudio){
			avmode |= P2PCommDef.P2PDATA_BITMK_AUDIO;
		}
		avmode |= P2PCommDef.P2PDATA_BITMK_VIDEO;

		int rs  = nvcP2PComm.reqP2PDeviceAVMedia(this.p2p_nDevHandle, avmode);
		if (rs != 0 ){
			Log.d("P2PCam", String.format("nvcP2PComm.reqP2PDeviceAVMedia(%d) : Error %d", p2p_nDevHandle, rs));
		}else{
			((ImageView)v).setImageDrawable(context.getResources().getDrawable(p2p_needaudio?R.drawable.audio_open:R.drawable.audio_close));
			p2p_needaudio = !p2p_needaudio;
		}
	}
	
	public boolean ISDeviceOnline(){
		return (this.p2p_nDevStatus == P2PCommDef.DEVCLTSTU_ONLINE) && (this.p2p_nDevHandle != 0);
	}
	
	public int GetDevOnlineUserCount(){
		if ( !this.ISDeviceOnline()) return 0;
		return nvcP2PComm.getP2PDeviceOnlineUserCnt(this.p2p_nDevHandle);
	}
	    
	public String getCamStatusMsg(int stu){	// ״̬�ı���
		int lsterr = nvcP2PComm.getP2PDeviceLastError(this.p2p_nDevHandle);
		p2p_nDevStatus = stu;
		if ( p2p_nDevStatus == P2PCommDef.DEVCLTSTU_UNCFG )
			p2p_nDevStatus = nvcP2PComm.getP2PDeviceStatus(this.p2p_nDevHandle);
		
		if ( p2p_nDevStatus == P2PCommDef.DEVCLTSTU_UNCFG || 
				p2p_nDevStatus == P2PCommDef.DEVCLTSTU_ONLINE ||
				lsterr == P2PCommDef.DEVCLTERR_OK )
		{
			String msg =  P2PCommDef.getP2PDevClientStatusMsg(p2p_nDevStatus);
			if ( p2p_nDevStatus == P2PCommDef.DEVCLTSTU_ONLINE ){
				// ���ӳɹ�
				m_isConnected = true;
				if(p2p_requestStatus == REQUEST_CONNECT){
					cancelConnectWatiProgressBar();
					imageViewButtonScan.setImageDrawable(context.getResources().getDrawable(R.drawable.btn_camera_open));
					if(MainActivity.videoIsInMainActivity){
						imageViewCamera.setImageDrawable(context.getResources().getDrawable(R.drawable.btn_play_image));
					}else{
						imageViewCamera.setImageDrawable(context.getResources().getDrawable(R.drawable.camera_close));
					}
					p2p_requestStatus = REQUEST_NON;
				}
				
				msg += context.getResources().getString(R.string.online);
			}else if(p2p_nDevStatus == P2PCommDef.DEVCLTSTU_LOGIN){
				msg += "---";
			}else{
				msg += "---";
			}
			
			
			return msg;
		}else{
			return P2PCommDef.getP2PDevClientErrorMsg(lsterr);
		}
	}
  
	public void ProcessLinkStatus(Message msg){	// ���ӱ仯��
		if (msg.arg1 != this.p2p_nDevHandle) return ;
		if(!MainActivity.videoIsInMainActivity){
			if(textViewCamStatus != null)
				textViewCamStatus.setText(p2p_uid + ":" + this.getCamStatusMsg(msg.arg2));
		}else{
			getCamStatusMsg(msg.arg2);
		}
	}
	
	public void UpdateP2PDevLinkStatus(int handle, int stu){
		Message msg = Message.obtain();
		msg.what = MSG_LINK_CHANGED;
		msg.arg1 = handle;
		msg.arg2 = stu;
		m_MsgHandler.sendMessage(msg);	
	}
  
	public boolean SaveByteArrayFile(String fname, byte[] pData, int len){
		File fl = new File(fname);
		if(!fl.exists()){
			try{
				if (!fl.createNewFile() ){
					Log.e("SDCardTool", "SaveByteArrayFile Create File Error");
					return false;
				}
			}
			catch(Exception ex){
				Log.e("SDCardTool", "SaveByteArrayFile Create File Error: " + ex.getLocalizedMessage());
				return false;				
			}
		}
		try{
			FileOutputStream stm = new FileOutputStream(fl);
			stm.write(pData, 0, len);
			stm.flush();
			stm.close();
			Log.d("SDCardTool", "SaveByteArrayFile OK : " + fl);
			return true;
		}
		catch(Exception ex){
			return false;
		}
	}
	public void SaveSnaptshot(){
		if (m_pVideoFrame == null || m_nVideoFrameLen == 0) return ;
		SaveByteArrayFile("�ļ�·������",m_pVideoFrame, m_nVideoFrameLen);
	}
  
	public void ProcessVideoMsg(Message msg){	// �յ�����
		this.m_pVideoFrame = (byte[])msg.obj;
		this.m_nVideoFrameLen = m_pVideoFrame.length;

		Bitmap bitmap = BitmapFactory.decodeByteArray(m_pVideoFrame, 0, m_nVideoFrameLen);
		if ( bitmap != null ){
			if(!p2p_bStart){	// ��Ƶ��û������
				if(!MainActivity.videoIsInMainActivity){
					if(imageViewLeftFrame != null && imageViewRightFrame != null){// �����������ʾ�Ļ���
						imageViewLeftFrame.setImageDrawable(context.getResources().getDrawable(R.drawable.nothing));
						imageViewRightFrame.setImageDrawable(context.getResources().getDrawable(R.drawable.nothing));
						imageViewCamera.setImageDrawable(context.getResources().getDrawable(R.drawable.camera_close));
					}
				}else{
					MainActivity.getInstance().mainImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.nothing));
					MainActivity.getInstance().setJoyImageViewsAlpha(1.0f);
					imageViewCamera.setImageDrawable(context.getResources().getDrawable(R.drawable.btn_play_image));
				}
			}else{
				if(waitConnectProgressBarIsVisible){
					if(p2p_requestStatus == REQUEST_START_VIDEO){ // ������Ƶ��һֱ�ڽ��գ�������ֻ��Ҫ����һ��ͼ��
						cancelConnectWatiProgressBar();
						if(!MainActivity.videoIsInMainActivity) 
							imageViewCamera.setImageDrawable(context.getResources().getDrawable(R.drawable.camera_open));
						else{
							imageViewCamera.setImageDrawable(context.getResources().getDrawable(R.drawable.btn_pause_image));
							MainActivity.getInstance().setJoyImageViewsAlpha(0.6f);
						}
						p2p_requestStatus = REQUEST_NON;
					}
				}
				if(!MainActivity.videoIsInMainActivity){	// ��ʾ����
					if(imageViewLeftFrame != null && imageViewRightFrame != null){
						imageViewLeftFrame.setImageBitmap(bitmap);
						imageViewRightFrame.setImageBitmap(bitmap);
					}
				}else{
					Drawable drawable =new BitmapDrawable(bitmap);
					MainActivity.getInstance().mainImageView.setImageDrawable(drawable);
				}
			}
		}
		else{
			Log.d("MainActivity", String.format("Decode Frame Error, FrmID %d, Len: %d", msg.arg1, m_nVideoFrameLen));
		}
	}
	
	public void OnRecvVideoData(int handle, int frmid, int tmv, int wd, int hi, byte[] pFrame){
		//System.out.println("OnRecvVideoData");
		if ( this.p2p_nDevHandle  != handle ) return ;
		// �����ڴ�ͳ�ƴ����֡��
		if ( pFrame.length > 1){
			//++ this.m_nFpsCnt;
			Message msg = Message.obtain();
			msg.what = MSG_VIDEO_DATA;
			msg.arg1 = frmid;
			msg.arg2 = tmv;
			msg.obj = pFrame;
			m_MsgHandler.sendMessage(msg);			
		}
		//System.out.printf("pFrame.len=%d\n", pFrame.length);
	}
	
	public void UpdateCurrentMediaInfor(){
		if ( !this.ISDeviceOnline()) return ;
		P2PDevMediaType mt = nvcP2PComm.readP2PDeviceMediaParams(this.p2p_nDevHandle, this.m_MediaType);
		if ( mt == null) return ;
		this.m_MediaType = mt;
		//�� �ڴ˸�����ʾ��Ƶ����Ƶ����Ϣ
		Log.d("P2PCam", String.format("DeviceMediaType(%d) : vdWidth %d,  vdHeight %d", p2p_nDevHandle, this.m_MediaType.vdFrame_Width, this.m_MediaType.vdFrame_Height));
	}
	
	public void OnRecvMediaInfor(int handle){
		if ( handle != this.p2p_nDevHandle ) return ;
		Message msg = Message.obtain();
		msg.what = MSG_UPDATEMEDIA;
		m_MsgHandler.sendMessage(msg);	
	}
	
	void StartRecord(){
		if ( !this.ISDeviceOnline()) {
			ShowMsg(context.getResources().getString(R.string.offline));
			return ;
		}
		if ( !this.p2p_bStart){ 
			ShowMsg(context.getResources().getString(R.string.video_not_start));
			return ;
		}
		if ( this.m_bRecing ) return ;
			
		if(!hasSdcard()){
			return ;
		}
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
		Date curDate = new Date(System.currentTimeMillis());//��ȡ��ǰʱ�� 
		String strDate = formatter.format(curDate);
		String videoName = strDate + ".asf";
		String videoPath = "/sdcard/" + p2p_uid + "/video/";
		String videoWholePath = videoPath + videoName;
		File file = new File(videoPath);
		if(!file.exists())
			file.mkdirs();
		
		if ( nvcP2PComm.recordP2PDeviceStart(p2p_nDevHandle, videoWholePath) != 0){
			this.ShowMsg(context.getResources().getString(R.string.record_open_failed));
		}
	}
	
	void StopRecord(){
		if ( !this.m_bRecing ) return ;
		if ( nvcP2PComm.recordP2PDeviceStop(this.p2p_nDevHandle) != 0){
			this.ShowMsg(context.getResources().getString(R.string.record_close_failed));
		}
	}
	
	public void DoRecStatusChangedEvt(){
		if ( !this.ISDeviceOnline()) return ;
		if ( nvcP2PComm.getRecordStatus(p2p_nDevHandle) == 0 )
			this.m_bRecing = false;
		else
			this.m_bRecing = true;

		if (this.m_bRecing ){
			imageViewLiveRec.setImageDrawable(context.getResources().getDrawable(R.drawable.live_recing));
			this.ShowMsg(context.getResources().getString(R.string.record_is_opened));
		}
		else{
			imageViewLiveRec.setImageDrawable(context.getResources().getDrawable(R.drawable.live_rec));
			this.ShowMsg(context.getResources().getString(R.string.record_is_closed));
		}
	}
	
	public void OnRecordStatusChanged(int hnd, String flnm, int isRec){
		if (hnd != this.p2p_nDevHandle ) return ;

		//"Error For Stop Record";
		Message msg = Message.obtain();
		msg.what = MSG_RECEVT;
		m_MsgHandler.sendMessage(msg);		
	}


	public String DWORDtoIPAddr(int ip){
		int p1 = ((ip >> 24) & 0xff);
		int p2 = ((ip >> 16) & 0xff);
		int p3 = ((ip >> 8) & 0xff);
		int p4 = (ip & 0xff);
		return String.format("%d.%d.%d.%d", p4, p3, p2, p1);
	}
	
	public int HostToNetS(int v){
		int p = (v >> 8 ) & 0xff;
		p |= ((v & 0xff) << 8);
		return p;
	}
	
	//��ʼ�����������豸
	public void StartSearch(){
		nvcP2PComm.StartSehP2PDeviceStatus();
	}
	
	public void OnRecvNewSehItem(String uid, int DevType, int Wanip, int Wanport, int Lanip, int Lanport, int LanAppPort, int isLanSeh){
		Log.d("P2PCam", String.format("�ѵ��豸 UID:%s, DevType:%d, ������ַ��%s:%d, ��������ַ��%s:%d \n", uid, DevType, 
				DWORDtoIPAddr(Wanip), HostToNetS(Wanport), DWORDtoIPAddr(Lanip), HostToNetS(Lanport)) );
		P2PCameraListAdapter.P2PCameraInfo camInfo = null;
		if(m_cameraAdapter == null || uid == null)
			return ;
		if(m_cameraAdapter.isContainP2PCamera(uid))
			return ;
		camInfo = m_cameraAdapter.new P2PCameraInfo();
		camInfo.ipAddr = DWORDtoIPAddr(Lanip);
		camInfo.uid = uid;
		m_cameraAdapter.addP2PCamera(camInfo);
	}
	
	//���������ָ��     ���÷ֱ��� -�� ��ȣ��߶�
	public boolean SetVideoResultion(int wd, int hi){
		if ( !this.ISDeviceOnline()) return false;
		if (this.m_MediaType.vdFrame_Width == wd && this.m_MediaType.vdFrame_Height == hi)
			return true;
		
		int res = (wd << 16) | hi;
		return nvcP2PComm.sendP2PDeviceMediaCmd(this.p2p_nDevHandle, P2PCommDef.IPCP_PPPCTLCMD_VIDEO_RESLU, res) == 0; 
	}
	
	public boolean SetVideoFps(int fps){	//����֡�� -�� ֡��
		if ( !this.ISDeviceOnline()) return false;

		return nvcP2PComm.sendP2PDeviceMediaCmd(this.p2p_nDevHandle, P2PCommDef.IPCP_PPPCTLCMD_VIDEO_FPS, fps) == 0; 
	}	

	public boolean SetVideoContrast(int cst){	//�Աȶ� 0--100 
		if ( !this.ISDeviceOnline()) return false;
		if ( cst < 0 || cst > 100) return false;
		
		return nvcP2PComm.sendP2PDeviceMediaCmd(this.p2p_nDevHandle, P2PCommDef.IPCP_PPPCTLCMD_VIDEO_CONSTRAST, cst) == 0; 
	}
	
	public boolean SetVideoBrightness(int brt){	//���� 0--100 
		if ( !this.ISDeviceOnline()) return false;
		if ( brt < 0 || brt > 100) return false;
		
		return nvcP2PComm.sendP2PDeviceMediaCmd(this.p2p_nDevHandle, P2PCommDef.IPCP_PPPCTLCMD_VIDEO_BIRGHTNESS, brt) == 0; 
	}
	
	public boolean SetVideoFlipSwitch(boolean vflp, boolean hflp){	//ͼ��ת �� ��ֱ��ת��ˮƽ��ת
		if ( !this.ISDeviceOnline()) return false;
		
		int rs = 0;
		if (vflp)
			rs |= P2PCommDef.IPCP_IMGPRM_FLIP_V;
		if (hflp)
			rs |= P2PCommDef.IPCP_IMGPRM_FLIP_H;
		
		return nvcP2PComm.sendP2PDeviceMediaCmd(this.p2p_nDevHandle, P2PCommDef.IPCP_PPPCTLCMD_VIDEO_FLIP, rs) == 0; 
	}
	
	public boolean PTZ_LEFT(){
		return this.SetPTZCmd(P2PCommDef.IPCP_PTZCODE_MTH_LEFT, 500);
	}
	
	public boolean SetPTZCmd(int cmd, int tmv){	//��ָ̨�� cmd cmd�������P2PCommDef.IPCP_PTZCODE_STOPV�� �� ��̨ת������ʱ�� tmv  ����
		if ( !this.ISDeviceOnline()) return false;
		
		Log.d("P2PCam", String.format("PTZCMD Cmd %d, Tmv %d", cmd, tmv));
		cmd &= 0xffff;
		cmd |= (tmv << 16);
		
		return nvcP2PComm.sendP2PDeviceMediaCmd(this.p2p_nDevHandle, P2PCommDef.IPCP_PPPCTLCMD_PTZCMD, cmd) == 0; 
	}

	public void onRecvImageFrame(byte[] pImage) {	// �յ���ͼ����
		// TODO Auto-generated method stub
		if ( pImage.length > 1){
			Message msg = Message.obtain();
			msg.what = MSG_SNAPSHOT_DATA;
			msg.obj = pImage;
			m_MsgHandler.sendMessage(msg);			
		}
	}

	public void OnRecvAudioData(int handle, int frmid, int tmv, byte[] pFrame) {
		// TODO Auto-generated method stub
		if ( this.p2p_nDevHandle  != handle ) return ;
		if ( pFrame.length > 1){
			Message msg = Message.obtain();
			msg.what = MSG_AUDIO_DATA;
			msg.arg1 = frmid;
			msg.arg2 = tmv;
			msg.obj = pFrame;
			m_MsgHandler.sendMessage(msg);			
		}
	}

	public void liveRecOnClick(View v) {  // ¼��
		// TODO Auto-generated method stub
		if(m_bRecing)
			StopRecord();
		else
			StartRecord();
	}
	
}

