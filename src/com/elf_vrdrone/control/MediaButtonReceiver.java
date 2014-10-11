package com.elf_vrdrone.control;

import android.content.BroadcastReceiver; 
import android.content.ComponentName;
import android.content.Context; 
import android.content.Intent; 
import android.media.AudioManager;
import android.os.IBinder;
import android.util.Log; 
import android.view.KeyEvent; 
 
public class MediaButtonReceiver extends BroadcastReceiver { 
    private static String TAG = "MediaButtonReceiver:"; 
    
    private static MediaButtonReceiver receiver = null;
    private static ComponentName componentName = null;
    
    
    public static MediaButtonReceiver getMediaButtonReceiver(Context context){
    	if(receiver == null){
    		receiver = new MediaButtonReceiver();
    		componentName = new ComponentName(context.getPackageName(), MediaButtonReceiver.class.getName());
    		((AudioManager)context.getSystemService(Context.AUDIO_SERVICE)).registerMediaButtonEventReceiver(componentName);
    	}
    	return receiver;
    }
    
    public void unRegisterMediaButtonEventReceiver(Context context){
    	((AudioManager)context.getSystemService(Context.AUDIO_SERVICE)).unregisterMediaButtonEventReceiver(componentName);
    }
    
    public static void deleteReceiver(){
    	receiver = null;
    }

	@Override 
    public void onReceive(Context context, Intent intent) { 
		// ���Action 
        String intentAction = intent.getAction(); 
        // ���KeyEvent���� 
        KeyEvent keyEvent = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT); 
 
        if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) { 
            // ��ð����ֽ��� 
            int keyCode = keyEvent.getKeyCode(); 
            // ���� / �ɿ� ��ť 
            int keyAction = keyEvent.getAction(); 
            // ����¼���ʱ�� 
            long downtime = keyEvent.getEventTime(); 
 
            // ��ȡ������ keyCode 
            StringBuilder sb = new StringBuilder(); 
            // ��Щ���ǿ��ܵİ����� �� ��ӡ�����û����µļ� 
            if (KeyEvent.KEYCODE_MEDIA_NEXT == keyCode) { 
            	System.out.println("KEYCODE_MEDIA_NEXT"); 
            } 
            // ˵���������ǰ���MEDIA_BUTTON�м䰴ťʱ��ʵ�ʳ������� KEYCODE_HEADSETHOOK ������ 
            // KEYCODE_MEDIA_PLAY_PAUSE 
            if (KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE == keyCode) { 
            	System.out.println("KEYCODE_MEDIA_PLAY_PAUSE"); 
            } 
            if (KeyEvent.KEYCODE_HEADSETHOOK == keyCode) { 
            	System.out.println("KEYCODE_HEADSETHOOK"); 
            } 
            if (KeyEvent.KEYCODE_MEDIA_PREVIOUS == keyCode) { 
            	System.out.println("KEYCODE_MEDIA_PREVIOUS"); 
            } 
            if (KeyEvent.KEYCODE_MEDIA_STOP == keyCode) { 
            	System.out.println("KEYCODE_MEDIA_STOP"); 
            }
        } 
    } 
}
