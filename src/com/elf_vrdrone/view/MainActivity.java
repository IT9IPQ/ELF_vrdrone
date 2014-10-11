package com.elf_vrdrone.view;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.dd.plist.Base64.OutputStream;
import com.elf_vrdrone.ble.BluetoothHandler;
import com.elf_vrdrone.control.MediaButtonReceiver;
import com.elf_vrdrone.modal.ApplicationSettings;
import com.elf_vrdrone.modal.Channel;
import com.elf_vrdrone.modal.MyArray;
import com.elf_vrdrone.modal.OSDCommon;
import com.elf_vrdrone.modal.Transmitter;
import com.ElecFreaks.ELF_vrdrone.R;
import com.g_zhang.p2pComm.P2PCommDef;
import com.g_zhang.p2pComm.nvcP2PComm;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class MainActivity extends Activity{
	//////////Camera///////////
	public static boolean videoIsInMainActivity = true;
	P2PVideoDisplayer p2pVideoDisplayer = null;
	////////////////////////////
	
	public int screenWidth;
	public int screenHeight;
	// airplane control lock
	private boolean mIsLocked = false;
	private ImageView imageViewLock = null;
	
	public ImageView imageViewLeft = null;
	public ImageView imageViewRight = null;
	public ImageView imageViewJoyBackgroundLeft = null;
	public ImageView imageViewJoyBackgroundRight = null;
	// hold radio button
	private ImageView imageViewKeepAltitudeMode;	
	
	public FrameLayout mainLeftFrameLayout = null;
	public FrameLayout mainRightFrameLayout = null;
	public LinearLayout mainLinearLayout = null;
	public RelativeLayout mainRelativeLayout = null;
	public ImageView mainImageView = null;
	
	public AlertDialog alertDialog = null;
	
	public BluetoothHandler mBluetoothHandler = null;
	public ImageView imageViewScan = null;
	private boolean mConnected = false;
	public ApplicationSettings appSettings = null;
	
	public Channel aileronChannel;
	public Channel elevatorChannel;
	public Channel rudderChannel;
	public Channel throttleChannel;
	public Channel aux1Channel;
	public Channel aux2Channel;
	public Channel aux3Channel;
	public Channel aux4Channel;
    private ArrayList<Channel> channelArrayList;
    public TouchHandler touchHandler;
    
    private FrameLayout mainFramLayoutProgressBar;
    private boolean waitConnectProgressBarIsVisible = false;
    private Transmitter transmitter = null;
    
    private int receivedDataCount = 0;
    private int receivedDataLength = 0;
    private int receivedDataStartIndex = 0;
    private boolean receiveStart = false;
    private byte[] receivedDataBytes = null;
    
    private final static byte MSP_RC_TUNING = 0x6F;
    private final static byte MSP_PID = 0x70;
    //private final static byte MSP_SET_PID = (byte)202;
    private final static byte MSP_SET_RAW_RC_TINY = (byte)150;
    private final static byte MSP_ARM = (byte)151;
    private final static byte MSP_DISARM = (byte)152;
    private final static byte MSP_SET_P = (byte)157;
    private final static byte MSP_SET_I = (byte)158;
    private final static byte MSP_SET_D = (byte)159;
    private final static byte MSP_GET_SENSOR_DATA = (byte)161;
    private final static byte MSP_GET_BARO_P = (byte)162;
    private final static byte MSP_EEPROM_WRITE = (byte)250;
    private final static byte MSP_SET_RC_TUNING = (byte)204;
    private final static byte MSP_RESET_CONF = (byte)208;
    
    private final static int RESULT_RETURN_DATA = 100;
    
    private int settingsPointLeft;
    private int settingsPointRight;
    private int settingsPointBottom;
    private int settingsPointTop;
    private int settingsPointDX;
    private int settingsPointDY;
    
    private float planeHeight = 0;
    
    public int leftRightOffset = 0;	
    public int upDownOffset = 0;	// ��¼AdjustController������Point��ƫ��ֵ
    private MediaButtonReceiver myMediaButtonReceiver;
    
    private int p2p_result = 0;
    
    private static boolean isFirstPressSettings = true;
    
    private static MainActivity ins = null;
    
    public boolean needInitJoyImageViews = true;
	//private TextView textViewTest;
	private boolean MainThreadIsRunning = true;
	
	private PrintStream debugFilePrintStream;
	private FileOutputStream debugFileOutputStream;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.fragment_main);
		/*
		 * �ս���Activityʱ�����onCreate������������Ϊˮƽʱ�ֻ����һ��onCreate�����
		 * ���ѽ�������Ϊˮƽ��onCreate��������Σ���������һЩ��onCreate�г�ʼ���Ĵ���ֻҪ����һ�Σ�
		 * ������仯ʱ�����onConfigurationChanged(Configuration)����ʱ���ǿ������������ʼ��
		 * */
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		ins = this;	// �����ⲿ�����ʹ��MainActivity�����ĳ�Ա
	}
	
	public static MainActivity getInstance(){
		return ins;
	}
	
	/* ��������������Ϊ����������ʱ���������Էɻ����п��ƣ�ֻ�н����ɹ��Ժ���ܶԷɻ����п���
	 * */
	public void lock(View v){
		if(mBluetoothHandler != null && mConnected){
			transmitter = Transmitter.getSharedTransmitter();
			if(mIsLocked){
				transmitter.transmmitSimpleCommand(OSDCommon.MSPCommnand.MSP_DISARM);
			}else{
				transmitter.transmmitSimpleCommand(OSDCommon.MSPCommnand.MSP_ARM);
			}
		}else{
	    	ShowMessage(getResources().getString(R.string.please_connect_bluetooth));
		}
	}
	
	/* ���ð�ť����ʱ������������
	 * �л�����һ������(SettingsActivity)
	 * */
	public void onButtonSettings(View v){
		Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
		if(!isFirstPressSettings){	
			/* ���ڵ�һ�ν���MainActivityʱ��û�еõ�������Щ������
			 * ��˲���Ҫ���ݲ�����SettingsActivity
			 */
			intent.putExtra("PointLeft", settingsPointLeft);
			intent.putExtra("PointRight", settingsPointRight);
			intent.putExtra("PointBottom", settingsPointBottom);
			intent.putExtra("PointTop", settingsPointTop);
		}else{
			isFirstPressSettings = false;
		}
		intent.putExtra("p2p_result", p2p_result);
		// ����SettingsActivity
		startActivityForResult(intent, RESULT_RETURN_DATA);
		// �л�����һ������ȥ��ʾ��Ƶ
		if(videoIsInMainActivity)
			videoIsInMainActivity = false;
	}
	
	private void initChannels() {	// ��ʼ��ͨ��
	    aileronChannel  = appSettings.getChannel(Channel.CHANNEL_NAME_AILERON);
	    elevatorChannel = appSettings.getChannel(Channel.CHANNEL_NAME_ELEVATOR);
	    rudderChannel   = appSettings.getChannel(Channel.CHANNEL_NAME_RUDDER);
	    throttleChannel = appSettings.getChannel(Channel.CHANNEL_NAME_THROTTLE);
	    aux1Channel     = appSettings.getChannel(Channel.CHANNEL_NAME_AUX1);
	    aux2Channel     = appSettings.getChannel(Channel.CHANNEL_NAME_AUX2);
	    aux3Channel     = appSettings.getChannel(Channel.CHANNEL_NAME_AUX3);
	    aux4Channel     = appSettings.getChannel(Channel.CHANNEL_NAME_AUX4);
	    channelArrayList = new ArrayList<Channel>();
	    channelArrayList.add(aileronChannel);
	    channelArrayList.add(elevatorChannel);
	    channelArrayList.add(rudderChannel);
	    channelArrayList.add(throttleChannel);
	    channelArrayList.add(aux1Channel);
	    channelArrayList.add(aux2Channel);
	    channelArrayList.add(aux3Channel);
	    channelArrayList.add(aux4Channel);
	    
	    aileronChannel.setValue(125.0f);	//���� 
	    elevatorChannel.setValue(125.0f);	//����
	    rudderChannel.setValue(125.0f+TouchHandler.rotateOffset);		//��ת
	    //throttleChannel.setValue(1.0f);		//����
	}
	
	/* startActivityForResult()����֮����һ�����棬
	 * Ȼ����Ǹ����淵�ص�ʱ�������������
	 */
	@Override 
    protected void onActivityResult(int requestCode, int resultCode, Intent data) { 
        // TODO Auto-generated method stub  
        super.onActivityResult(requestCode, resultCode, data); 
        if (requestCode == 1) { 			// bluetooth open successful ?
            if (resultCode == RESULT_OK) { 
            	mBluetoothHandler.setEnabled(true);
            } else if (resultCode == RESULT_CANCELED) { 
            	mBluetoothHandler.setEnabled(false);
                finish(); 
            } 
        } 
        if(resultCode == RESULT_RETURN_DATA){	
        	// ��SettingsActivity���ص�ʱ��ᴫһЩ���������������������Խ�����Щ����
        	settingsPointLeft = data.getIntExtra("PointLeft", 0);
    		settingsPointRight = data.getIntExtra("PointRight", 0);
    		settingsPointBottom = data.getIntExtra("PointBottom", 0);
    		settingsPointTop = data.getIntExtra("PointTop", 0);
    		settingsPointDX = data.getIntExtra("PointDX", 0);
    		settingsPointDY = data.getIntExtra("PointDY", 0);
    		// �õ�AdjustController��Point��ƫ��ֵ
    		leftRightOffset = settingsPointDX*WidgetMove.adjustOffset/(WidgetMove.movePixel);
    		upDownOffset = settingsPointDY*WidgetMove.adjustOffset/(WidgetMove.movePixel);
    		
    		// ��p2p��Ƶ��ʾ��MainActivity    P2PVideoDisplayer�������һ������ģʽ
    		p2pVideoDisplayer = P2PVideoDisplayer.getInstance();
    		p2pVideoDisplayer.setParameter(this, getWindow().getDecorView(), p2p_result);
    		
    		// �õ���������Ķ���, ����������п��Ի�ȡ����ģʽ������ģʽ�Ȳ���
    		String filesDir = getFilesDir().toString();	
    		appSettings = new ApplicationSettings(filesDir+"/Settings.plist"); 
            ApplicationSettings.copyDefaultSettingsFileIfNeeded(filesDir, this);
            
            // �����Ƿ�������ģʽ������ͼ��ʹ���������(touchHandler)
    		setHandMode();
        }
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/* ���������豸 */
	public void scan(View v){
		if(!mConnected){	// ������δ���ӣ�����������
			mBluetoothHandler.getDeviceListAdapter().clearDevice();
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			LayoutInflater inflater = LayoutInflater.from(this);
			View view = inflater.inflate(R.layout.device_list, null);
			
			ListView deviceListView = (ListView) view.findViewById(R.id.listViewDevice);
			deviceListView.setAdapter(mBluetoothHandler.getDeviceListAdapter());
			
			deviceListView.setOnItemClickListener(new OnItemClickListener() {
	
					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						BluetoothDevice device = mBluetoothHandler.getDeviceListAdapter().getItem(position);
						alertDialog.cancel();
						showConnectWaitProgressBar();
						// connect
						mBluetoothHandler.connect(device.getAddress());
					}
			});
			
			builder.setView(view);

			alertDialog = builder.create();
			alertDialog.show();
			
			mBluetoothHandler.scanLeDevice(true);	
		}else{			// ��������������Ͽ�����
			transmitter = Transmitter.getSharedTransmitter();
			transmitter.transmmitSimpleCommand(OSDCommon.MSPCommnand.MSP_DISARM);
			//mBluetoothHandler.onPause();
			//mBluetoothHandler.onDestroy();
			showConnectWaitProgressBar();
			connectStatusCallback(false);
		}
	}
	
	
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
    protected void onDestroy() {
        super.onDestroy(); 
        
		try {
			debugFilePrintStream.close();
			debugFileOutputStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        isFirstPressSettings = true;
        MainThreadIsRunning = false;
        
        if(mBluetoothHandler != null){
        	Transmitter.getSharedTransmitter().stop();
        	mBluetoothHandler.onPause();
        	mBluetoothHandler.onDestroy();
        }
        if(videoIsInMainActivity)
        	p2pVideoDisplayer.onDestroy();
        if(p2p_result >= 0){
        	nvcP2PComm.DestoryP2PComm();
        }
        if(myMediaButtonReceiver != null)
        	myMediaButtonReceiver.unRegisterMediaButtonEventReceiver(this);
        MediaButtonReceiver.deleteReceiver();	
    }  
    
    public void connectStatusCallback(boolean isConnected){
    	cancelConnectWatiProgressBar();
    	if(isConnected){
    		transmitter = Transmitter.getSharedTransmitter();
    		transmitter.start();
    		imageViewScan.setImageDrawable(getResources().getDrawable(R.drawable.ble_connected));
    	}else{
    		transmitter = Transmitter.getSharedTransmitter();
    		transmitter.stop();
    		imageViewScan.setImageDrawable(getResources().getDrawable(R.drawable.ble_scan));
    		mIsLocked = false;
    		imageViewLock.setImageDrawable(getResources().getDrawable(R.drawable.btn_lock_image));
    		mBluetoothHandler.onPause();
    		mBluetoothHandler.onDestroy();
    	}
    	aileronChannel.setValue(125.0f);
	    elevatorChannel.setValue(125.0f);
	    rudderChannel.setValue(125.0f);		
	    throttleChannel.setValue(1.0f);		//����

    	mConnected = isConnected;
    	mBluetoothHandler.mConnected = isConnected;
    	if(touchHandler != null){
    		touchHandler.setAccPosition(0);
    	}else{
    		System.out.println("touchHandler = null");
    	}
    	if(appSettings != null){
    		imageViewKeepAltitudeMode.setImageDrawable(getResources().getDrawable(R.drawable.switch_off));
			appSettings.setIsHoldAltitudeMode(false);
			aux2Channel.setValue(-1);
    	}
    }
    
    public void hasRecievedDataCallback(byte[] bytes){
    	//System.out.print("REC:");
    	//for(byte b:bytes)
    	//	System.out.printf("%02X ", b);
    	//System.out.println("");
    	
    	receivedDataBytes = MyArray.arrayCat(receivedDataBytes, bytes);
    	if(receivedDataBytes.length > 2){
	    	for(int i=0; i<receivedDataBytes.length; i++){
	    		if(receiveStart)break;
	    		if(receivedDataBytes[i] == (byte)0x24 && (receivedDataBytes.length-i>3) && receivedDataBytes[i+1] == (byte)0x4D && receivedDataBytes[i+2] == (byte)0x3E){
	    			receivedDataLength = (short) (receivedDataBytes[i+3]&0xff);
	    			receiveStart = true;
	    			receivedDataStartIndex = i;
	    			//System.out.printf("found head, start index=%d\n", i);
	    			break;
	    		}
	    	}
    	}
    	
    	if(receiveStart){
    		receivedDataCount = receivedDataBytes.length-receivedDataStartIndex;
    		if(receivedDataCount == (receivedDataLength+6)){
    			receiveStart = false;
    			receivedDataCount = 0;
    			//System.out.println("receive ok");
    			byte[] dstBytes = new byte[receivedDataLength+6];
        		System.arraycopy(receivedDataBytes, receivedDataStartIndex, dstBytes, 0, receivedDataLength+6);
    			// process data:receivedDataBytes
    			if(processReceivedData(dstBytes) == 0){// process ok
    				receivedDataBytes = null;
    				receivedDataStartIndex = 0;
    			}
    		}
    	}
    	
    }
    
    public int processReceivedData(byte[] bytes){
    	switch(bytes[4]){
    	case (byte)180:
    		System.out.printf("alt=%d\n", bytes[5]);
    		break;
    	case MSP_SET_P:
    	case MSP_SET_I:
    	case MSP_SET_D:
    	case MSP_SET_RC_TUNING:
    	case MSP_PID:
    	case MSP_EEPROM_WRITE:
    	case MSP_RC_TUNING:
    		Intent intent = new Intent("RECEIVED_DATA");
    		intent.putExtra("recievedData", bytes);
    		sendBroadcast(intent);
    		break;
    	case MSP_GET_BARO_P:
    		short p = (short) ((bytes[5]&0xff)+(bytes[6]&0xff)*256);
    		short estAlt = (short) ((bytes[7]&0xff) + (bytes[8]&0xff)*256);
    		short dError = (short) ((bytes[9]&0xff) + (bytes[10]&0xff)*256);
    		short throttle = (short) ((bytes[11]&0xff) + (bytes[12]&0xff)*256);
    		short i = (short) ((bytes[13]&0xff) + (bytes[14]&0xff)*256);
    		short d = (short) ((bytes[15]&0xff) + (bytes[16]&0xff)*256);
    		int pid = p + i + d;
    		if(pid > 123) pid = 123;
    		if(pid < -40) pid = -40;
    		//textViewTest.setText(String.format("p=%d \ti=%d \td=%d \tpid=%d \tcurrent=%d \tdError=%d \tthrottle=%d\n", p, i, d, pid, estAlt, dError, throttle));
    		debugFilePrintStream.printf("p=%d \ti=%d \td=%d \tpid=%d \tcurrent=%d \tdError=%d \tthrottle=%d\n", p, i, d, pid, estAlt, dError, throttle);
    		//System.out.printf("p=%d \ti=%d \td=%d \tpid=%d \tcurrent=%d \tdError=%d \tthrottle=%d\n", p, i, d, pid, estAlt, dError, throttle);
    		break;
    	case MSP_SET_RAW_RC_TINY:

    		break;
    	case MSP_GET_SENSOR_DATA:

    		break;
    	case MSP_RESET_CONF:
    		Transmitter.getSharedTransmitter().transmmitSimpleCommand(OSDCommon.MSPCommnand.MSP_PID);
    		break;
    	case MSP_ARM:
    		if(bytes[2] != '>'){
    			Toast.makeText(MainActivity.this, "failed", Toast.LENGTH_SHORT).show();
    			break;
    		}
    		mIsLocked = true;
    		aileronChannel.setValue(125.0f);
    	    elevatorChannel.setValue(125.0f);
    	    rudderChannel.setValue(125.0f);		
    	    throttleChannel.setValue(1.0f);		//����
    		transmitter.start();
    		imageViewLock.setImageDrawable(getResources().getDrawable(R.drawable.btn_unlock_image));
    		// ��ʼ��������
    		if(touchHandler != null){
        		touchHandler.setAccPosition(0);
        	}
    		if(appSettings != null){
        		imageViewKeepAltitudeMode.setImageDrawable(getResources().getDrawable(R.drawable.switch_off));
    			appSettings.setIsHoldAltitudeMode(false);
    			aux2Channel.setValue(-1);
        	}
    		break;
    	case MSP_DISARM:
    		if(bytes[2] != '>'){
    			Toast.makeText(MainActivity.this, "failed", Toast.LENGTH_SHORT).show();
    			break;
    		}
    		aileronChannel.setValue(125.0f);
    	    elevatorChannel.setValue(125.0f);
    	    rudderChannel.setValue(125.0f);		
    	    throttleChannel.setValue(1.0f);		//����
    		transmitter.stop();
    		mIsLocked = false;
    		imageViewLock.setImageDrawable(getResources().getDrawable(R.drawable.btn_lock_image));
    		// ��ʼ��������
    		if(touchHandler != null){
        		touchHandler.setAccPosition(0);
        	}
    		if(appSettings != null){
        		imageViewKeepAltitudeMode.setImageDrawable(getResources().getDrawable(R.drawable.switch_off));
    			appSettings.setIsHoldAltitudeMode(false);
    			aux2Channel.setValue(-1);
        	}
    		break;
    	}
    	
    	return 0;
    }


	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		//Toast.makeText(this, "onConfigurationChanged", Toast.LENGTH_SHORT).show();
		mBluetoothHandler = new BluetoothHandler(this);
		if(!mBluetoothHandler.isSupportBle()){
			Toast.makeText(this, "your device not support BLE!", Toast.LENGTH_SHORT).show();
			this.finish();
			return ;
		}
		// open bluetooth
        if (!mBluetoothHandler.getBluetoothAdapter().isEnabled()) { 
            Intent mIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE); 
            startActivityForResult(mIntent, 1);   
        }else{
        	mBluetoothHandler.setEnabled(true);
        }    
        
        // you must setBluetoothHandler before new ApplicationSettings()
        // because it will call Transmitter.transmitteData() when you call
        // ApplicationSettings.resetToDefault() or Channel.setValue()
        // mBluetoothHandler is a static member of Transmitter, and this class can only
        // construct one instance, and it depend on BluetoothHandler
        Transmitter.setBluetoothHandler(mBluetoothHandler);
        
        myMediaButtonReceiver = MediaButtonReceiver.getMediaButtonReceiver(this);
        
        imageViewLeft = (ImageView) findViewById(R.id.imageViewDir);
		imageViewRight = (ImageView) findViewById(R.id.imageViewAcc);
		imageViewJoyBackgroundLeft = (ImageView) findViewById(R.id.imageViewJoyBackgroundLeft);
		imageViewJoyBackgroundRight = (ImageView) findViewById(R.id.imageViewJoyBackgroundRight);
		imageViewKeepAltitudeMode = (ImageView) findViewById(R.id.imageViewHoldAltitude);
		
		imageViewLock = (ImageView) findViewById(R.id.imageViewLock);
		imageViewScan = (ImageView) findViewById(R.id.imageViewScan);
		
		mainLeftFrameLayout = (FrameLayout) findViewById(R.id.mainLeftFrameLayout);
		mainRightFrameLayout = (FrameLayout) findViewById(R.id.mainRightFrameLayout);
		mainFramLayoutProgressBar = (FrameLayout) findViewById(R.id.mainFramLayoutProgressBar);
		
		//textViewTest = (TextView) findViewById(R.id.textViewTest);
		
		File debugFileDir = new File("/sdcard/elf_vrdron/debug/");
		if(debugFileDir.exists() == false)
			debugFileDir.mkdirs();
		
		File debugFile = new File("/sdcard/elf_vrdron/debug/debug.txt");
		
		try {
			if(debugFile.exists() == false)
				debugFile.createNewFile();
			debugFileOutputStream = new FileOutputStream(debugFile);
			debugFilePrintStream = new PrintStream(debugFileOutputStream);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		String filesDir = getFilesDir().toString();
        ApplicationSettings.copyDefaultSettingsFileIfNeeded(filesDir, this);
    	appSettings = new ApplicationSettings(filesDir+"/Settings.plist");
    	
    	if(appSettings.isBeginnerMode()){
    		TouchHandler.dirGain = 0.70f;
			TouchHandler.accGain = 0.80f;
    	}else{
    		TouchHandler.dirGain = 0.80f;
			TouchHandler.accGain = 1.00f;
    	}
    	initChannels();

    	imageViewKeepAltitudeMode.setImageDrawable(getResources().getDrawable(R.drawable.switch_off));
		appSettings.setIsHoldAltitudeMode(false);
		aux2Channel.setValue(-1);
    	
        // �����������ȷ�����ҿ����������Ż��Ƿ�����
        setHandMode(); // ����������������һ��TouchHandler����touchHandler
        
		DisplayMetrics dm = this.getResources().getDisplayMetrics();
		screenWidth = dm.widthPixels;
		screenHeight = dm.heightPixels;
		
		
		new Thread(new Runnable() {	// ���¿������λ��
			@Override
			public void run() {
				// TODO Auto-generated method stub
				while(MainThreadIsRunning){
					if(joyImageViewsLayoutFinished() && needInitJoyImageViews){
						//System.out.println("joyImageViewsLayoutFinished");
						touchHandler.initImageRect();			// �õ������ἰ�����ᱳ����λ�ò���
						try {
							Thread.sleep(20);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						touchHandler.initImagePosition();		// �Կ����ἰ�����ᱳ�����в���
						needInitJoyImageViews = false;
					}
				}
			}
		}).start();
		
		Intent intent = getIntent();
		settingsPointLeft = intent.getIntExtra("PointLeft", 0);
		settingsPointRight = intent.getIntExtra("PointRight", 0);
		settingsPointBottom = intent.getIntExtra("PointBottom", 0);
		settingsPointTop = intent.getIntExtra("PointTop", 0);
		
		mainLinearLayout = (LinearLayout) findViewById(R.id.mainLinearLayout);
		mainRelativeLayout = (RelativeLayout) findViewById(R.id.mainRelativeLayout);
		mainImageView = (ImageView) findViewById(R.id.mainImageView); // ������ʾ��Ƶ��ImageView
		p2pVideoDisplayer = P2PVideoDisplayer.getInstance();
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				p2p_result = initP2PComm();
				///////////////////���������������ͷ//////////////////////
				if(videoIsInMainActivity){
					p2pVideoDisplayer.setParameter(MainActivity.getInstance(), getWindow().getDecorView(), p2p_result);
				}
				///////////////////////////////////////////////////////
			}
		}).start();
		
		super.onConfigurationChanged(newConfig);
	}
    
    public void setHandMode(){
    	// ÿ�ε���setHandMode()������������touchHandler����
    	int offset = 0;
    	if(touchHandler != null){		// ����֮ǰ�����accUpOffset
    		offset = touchHandler.getAccUpOffset();
    	}
    	if(appSettings.isLeftHanded()){	//�����Ƿ�������ģʽ�����»��touchHandler
    		touchHandler = new TouchHandler(this, true);	
        }else{
        	touchHandler = new TouchHandler(this, false);	
        }
    	// �ڵ�һ�ν���MainActivityʱ������һ���߳�һֱ�ڼ���Ƿ�Ҫ��������������ͼ���λ��
    	// ���ڽ���տ�ʼ���ػ�ӱ�Ľ�����뵽��������ʱ���Ҫ�������ÿ�����ͼ���λ�ã�
    	// �����������õ���һ�ε�λ�ã�����ͼ�����õ��м�λ��(�ս���ʱ���Ƕ�����͵�λ��)
    	needInitJoyImageViews = true;	
    	touchHandler.setAccUpOffset(offset);
    	touchHandler.setChannelArrayList(channelArrayList);
    	if(touchHandler != null){	// ����ʱ��ʵ���ǰ��¿�����ͼ�꣬���ǰ��¿�����Ĳ��������ƿ������λ�õ�
    		mainLeftFrameLayout.setOnTouchListener(touchHandler);
    		mainRightFrameLayout.setOnTouchListener(touchHandler);
    	}
        
        if(appSettings.isHeadFreeMode())
    		aux1Channel.setValue(1.0f);
    	else
    		aux1Channel.setValue(-1.0f);
    	if(appSettings.isHoldAltitudeMode())
    		aux2Channel.setValue(1.0f);
    	else
    		aux2Channel.setValue(-1.0f);
    }
    
    public int initP2PComm() {
		WifiManager wm=(WifiManager)getSystemService(Context.WIFI_SERVICE);  
		//���Wifi״̬    
		if(!wm.isWifiEnabled()){  
			//wm.setWifiEnabled(true);  
			return -1;
		}
		WifiInfo wi=wm.getConnectionInfo();
		if(wi == null)
			return -1;
		//��ȡ32λ����IP��ַ    
		int ipAdd=wi.getIpAddress(); 
		return nvcP2PComm.InitP2PServer(P2PCommDef.P2PSev_Root1,P2PCommDef.P2PSev_Root2,"","",P2PCommDef.P2P_SEVPORT, ipAdd);
	}  
    
    public void holdAltitudeModeOnClick(View v){
    	if(!mConnected){
    		ShowMessage(getResources().getString(R.string.please_connect_bluetooth));
    		imageViewKeepAltitudeMode.setImageDrawable(getResources().getDrawable(R.drawable.switch_off));
			appSettings.setIsHoldAltitudeMode(false);
			aux2Channel.setValue(-1);
    		return ;
    	}
		if(appSettings.isHoldAltitudeMode()){
			imageViewKeepAltitudeMode.setImageDrawable(getResources().getDrawable(R.drawable.switch_off));
			appSettings.setIsHoldAltitudeMode(false);
			aux2Channel.setValue(-1);
		}else{
			imageViewKeepAltitudeMode.setImageDrawable(getResources().getDrawable(R.drawable.switch_on));
			appSettings.setIsHoldAltitudeMode(true);
			aux2Channel.setValue(1);
		}
		appSettings.save();
	}
    
    public void scanCameraOnClick(View v){
    	if(videoIsInMainActivity && p2pVideoDisplayer != null){
    		p2pVideoDisplayer.scanOnClick(v);
    	}else{
    		ShowMessage(getResources().getString(R.string.offline));
    	}
    }
    
    public void ShowMessage(String str){
    	Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }
    
    public void showConnectWaitProgressBar(){ // �ڽ��沼�ֵ���˷���һ��FrameLayout���������м����һ��ProgressBar
		waitConnectProgressBarIsVisible = true;
		mainFramLayoutProgressBar.setVisibility(FrameLayout.VISIBLE);
	}
	
	public void cancelConnectWatiProgressBar(){
		waitConnectProgressBarIsVisible = false;
		mainFramLayoutProgressBar.setVisibility(FrameLayout.INVISIBLE);
	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		if(waitConnectProgressBarIsVisible || (p2pVideoDisplayer != null && p2pVideoDisplayer.getWaitConnectProgressBarStatus())){
			cancelConnectWatiProgressBar();
			if(p2pVideoDisplayer != null)
				p2pVideoDisplayer.cancelConnectWatiProgressBar();
		}else{
			super.onBackPressed();
		}
	}
	// ����ͼ���͸����
	public void setJoyImageViewsAlpha(float alpha){
		imageViewJoyBackgroundLeft.setAlpha(alpha);
		imageViewJoyBackgroundRight.setAlpha(alpha);
		imageViewLeft.setAlpha(alpha);
		imageViewRight.setAlpha(alpha);
	}
	// �жϲ����Ƿ����
	public boolean joyImageViewsLayoutFinished(){
		if(imageViewJoyBackgroundLeft.getLeft()!=0 && imageViewJoyBackgroundLeft.getRight()!=0 
				&& imageViewJoyBackgroundLeft.getTop()!=0 && imageViewJoyBackgroundLeft.getBottom()!=0 && imageViewJoyBackgroundLeft.getHeight()!=0 && imageViewJoyBackgroundLeft.getWidth()!=0){
			if(imageViewJoyBackgroundRight.getLeft()!=0 && imageViewJoyBackgroundRight.getRight()!=0 
					&& imageViewJoyBackgroundRight.getTop()!=0 && imageViewJoyBackgroundRight.getBottom()!=0 && imageViewJoyBackgroundRight.getHeight()!=0 && imageViewJoyBackgroundRight.getWidth()!=0){
				if(imageViewLeft.getLeft()!=0 && imageViewLeft.getRight()!=0 
						&& imageViewLeft.getTop()!=0 && imageViewLeft.getBottom()!=0 && imageViewLeft.getHeight()!=0 && imageViewLeft.getWidth()!=0){
					if(imageViewRight.getLeft()!=0 && imageViewRight.getRight()!=0 
							&& imageViewRight.getTop()!=0 && imageViewRight.getBottom()!=0 && imageViewRight.getHeight()!=0 && imageViewRight.getWidth()!=0){
						return true;
					}else return false;
				}else return false;
			}else return false;
		}else return false;
	}
	
	public void imageViewCameraOnClick(View v){
		if(p2pVideoDisplayer == null || !videoIsInMainActivity)
			return ;
		p2pVideoDisplayer.startOnClick(v);
	}
}
