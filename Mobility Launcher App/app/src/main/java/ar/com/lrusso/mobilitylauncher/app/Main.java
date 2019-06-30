package ar.com.lrusso.mobilitylauncher.app;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.media.*;
import android.os.Handler;
import android.net.Uri;
import android.os.*;
import android.Manifest;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.speech.tts.*;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.annotation.TargetApi;
import android.telephony.*;
import android.view.*;
import android.view.View.OnKeyListener;
import android.widget.*;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ar.com.lrusso.mobilitylauncher.app.R;

import java.util.*;

public class Main extends Activity implements TextToSpeech.OnInitListener
	{
	private AppService usbService;
	private MyHandler mHandler;
	private String arduinoValue = "";

	public static int SETTINGS_REQUEST_CODE = 123;
	public static int PERMISSION_REQUEST_CODE = 456;
	private static boolean speakOnResume = false;
	public static TextView messages;
	public static TextView calls;
	private TextView contacts;
	private TextView music;
	private TextView internet;
	public static TextView alarms;
	private TextView voicerecorder;
	private TextView applications;
	private TextView settings;
	private TextView status;
	private Activity activity;
	private boolean okToFinish = false;
	private boolean hasProfileChangerPermission = false;

    @Override protected void onCreate(Bundle savedInstanceState)
    	{
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.main);
    	activity = this;

		GlobalVars.lastActivity = Main.class;
		GlobalVars.lastActivityArduino = this;
		GlobalVars.mainActivity = this;
		speakOnResume=false;
		messages = (TextView) findViewById(R.id.messages);
		calls = (TextView) findViewById(R.id.calls);
		contacts = (TextView) findViewById(R.id.contacts);
		music = (TextView) findViewById(R.id.music);
		internet = (TextView) findViewById(R.id.browser);
		alarms = (TextView) findViewById(R.id.alarms);
		voicerecorder = (TextView) findViewById(R.id.voicerecorder);
		applications = (TextView) findViewById(R.id.apps);
		settings = (TextView) findViewById(R.id.settings);
		status = (TextView) findViewById(R.id.status);
		GlobalVars.activityItemLocation=0;
		GlobalVars.activityItemLimit=10;
		
		GlobalVars.context = this;
		GlobalVars.startTTS(GlobalVars.tts);
		GlobalVars.tts = new TextToSpeech(this,this);
		GlobalVars.tts.setPitch((float) 1.0);

		try
			{
			//SETS THE ALARM VIBRATOR VARIABLE
			GlobalVars.alarmVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
			}
			catch(Exception e)
			{
			}

		try
			{
			//SETS PROFILE TO NORMAL
			AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
			audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
			}
			catch(Exception e)
			{
			}

		try
			{
			GlobalVars.openAndLoadAlarmFile();
			GlobalVars.setText(alarms,false, getResources().getString(R.string.mainAlarms) + " (" + GlobalVars.getPendingAlarmsForTodayCount() + ")");
			}
			catch(Exception e)
			{
			}

		//HIDES THE NAVIGATION BAR
		if (Build.VERSION.SDK_INT>11){try{GlobalVars.hideNavigationBar(this);}catch(Exception e){}}

		//CHECKS IF MARSHMALLOW TO ASK THE USER FOR PERMISSIONS
		if (Build.VERSION.SDK_INT>=23){try{marshmallowPermissions();}catch(Exception e){}}

		//CHECKS IF MARSHMALLOW TO ASK FOR FLOATING VIEWS (FOR ANSWERING AND REJECTING CALLS)
		if (Build.VERSION.SDK_INT>=23){testDrawOverlays();}

		//CHECKS IF MARSHALLOW TO ASK FOR CHANGE THE DEVICE PROFILE
		if (Build.VERSION.SDK_INT>=23){testProfilePermission();}

		//LIST EVERY MUSIC FILE WITH THE MEDIA INFORMATION TO USE IT WITH THE MUSIC PLAYER
		new MusicPlayerThreadRefreshDatabase().execute("");

		//READ WEB BOOKMARKS DATABASE
		GlobalVars.readBookmarksDatabase();

		if (GlobalVars.deviceIsAPhone()==true)
			{
			messages.setText(GlobalVars.context.getResources().getString(R.string.mainMessages) + " (" + String.valueOf(GlobalVars.getMessagesUnreadCount()) + ")");
			}
			else
			{
			messages.setText(GlobalVars.context.getResources().getString(R.string.mainMessages) + " (0)");
			}

		if (GlobalVars.deviceIsAPhone()==true)
			{
			calls.setText(GlobalVars.context.getResources().getString(R.string.mainCalls) + " (" + String.valueOf(GlobalVars.getCallsMissedCount()) + ")");
			}
			else
			{
			calls.setText(GlobalVars.context.getResources().getString(R.string.mainCalls) + " (0)");
			}

		//GETS EVERY ALARM TONE
		try
			{
			RingtoneManager manager = new RingtoneManager(this);
			manager.setType(RingtoneManager.TYPE_ALARM);
			Cursor cursorAlarms = manager.getCursor();
			if (cursorAlarms!=null)
				{
				if(cursorAlarms.moveToFirst())
					{
					while(!cursorAlarms.isAfterLast())
						{
						GlobalVars.settingsToneAlarmTitle.add(cursorAlarms.getString(RingtoneManager.TITLE_COLUMN_INDEX));
						GlobalVars.settingsToneAlarmUri.add(cursorAlarms.getString(RingtoneManager.URI_COLUMN_INDEX));
						GlobalVars.settingsToneAlarmID.add(cursorAlarms.getString(RingtoneManager.ID_COLUMN_INDEX));
						cursorAlarms.moveToNext();
						}
					}
				//cursorAlarms.close();
				}
			}
			catch(NullPointerException e)
			{
			}
			catch(Exception e)
			{
			}

		//GETS EVERY NOTIFICATION TONE
		try
			{
			RingtoneManager manager = new RingtoneManager(this);
			manager.setType(RingtoneManager.TYPE_NOTIFICATION);
			Cursor cursorNotificationTone = manager.getCursor();
			if (cursorNotificationTone!=null)
				{
				if(cursorNotificationTone.moveToFirst())
					{
					while(!cursorNotificationTone.isAfterLast())
						{
						GlobalVars.settingsToneNotificationTitle.add(cursorNotificationTone.getString(RingtoneManager.TITLE_COLUMN_INDEX));
						GlobalVars.settingsToneNotificationUri.add(cursorNotificationTone.getString(RingtoneManager.URI_COLUMN_INDEX));
						GlobalVars.settingsToneNotificationID.add(cursorNotificationTone.getString(RingtoneManager.ID_COLUMN_INDEX));
						cursorNotificationTone.moveToNext();
						}
					}
				//cursorNotificationTone.close();
				}
			}
			catch(NullPointerException e)
			{
			}
			catch(Exception e)
			{
			}

		//GETS EVERY CALL TONE
		try
			{
			RingtoneManager manager = new RingtoneManager(this);
			manager.setType(RingtoneManager.TYPE_RINGTONE);
			Cursor cursorCallTone = manager.getCursor();
			if (cursorCallTone!=null)
				{
				if(cursorCallTone.moveToFirst())
					{
					while(!cursorCallTone.isAfterLast())
						{
						GlobalVars.settingsToneCallTitle.add(cursorCallTone.getString(RingtoneManager.TITLE_COLUMN_INDEX));
						GlobalVars.settingsToneCallUri.add(cursorCallTone.getString(RingtoneManager.URI_COLUMN_INDEX));
						GlobalVars.settingsToneCallID.add(cursorCallTone.getString(RingtoneManager.ID_COLUMN_INDEX));
						cursorCallTone.moveToNext();
						}
					}
				//cursorCallTone.close();
				}
			}
			catch(NullPointerException e)
			{
			}
			catch(Exception e)
			{
			}

		//GETS READING SPEED VALUE
		String readingSpeedString = GlobalVars.readFile("readingspeed.cfg");
		if (readingSpeedString=="")
			{
			GlobalVars.settingsTTSReadingSpeed = 1;
			GlobalVars.tts.setSpeechRate(GlobalVars.settingsTTSReadingSpeed);
			GlobalVars.writeFile("readingspeed.cfg",String.valueOf(GlobalVars.settingsTTSReadingSpeed));
			}
			else
			{
			try
				{
				GlobalVars.settingsTTSReadingSpeed = Integer.valueOf(readingSpeedString);
				GlobalVars.tts.setSpeechRate(GlobalVars.settingsTTSReadingSpeed);
				}
				catch(Exception e)
				{
				GlobalVars.settingsTTSReadingSpeed = 1;
				GlobalVars.tts.setSpeechRate(GlobalVars.settingsTTSReadingSpeed);
				GlobalVars.writeFile("readingspeed.cfg",String.valueOf(GlobalVars.settingsTTSReadingSpeed));
				}
			}

		//GETS INPUT MODE VALUE
		String inputModeString = GlobalVars.readFile("inputmode.cfg");
		if (inputModeString=="")
			{
			GlobalVars.inputMode = GlobalVars.INPUT_KEYBOARD;
			GlobalVars.writeFile("inputmode.cfg",String.valueOf(GlobalVars.INPUT_KEYBOARD));
			}
			else
			{
			try
				{
				GlobalVars.inputMode = Integer.valueOf(inputModeString);
				}
				catch(Exception e)
				{
				GlobalVars.inputMode = GlobalVars.INPUT_KEYBOARD;
				GlobalVars.writeFile("inputmode.cfg",String.valueOf(GlobalVars.INPUT_KEYBOARD));
				}
			}

		//GETS SCREEN TIMEOUT POSSIBLE VALUES
		int[] arr = getResources().getIntArray(R.array.screenTimeOutSeconds);
		for(int i=0;i<arr.length;i++)
			{
			GlobalVars.settingsScreenTimeOutValues.add(String.valueOf(arr[i]));
			}

		//GETS TIME VALUES FOR ALARMS
		String[] arr2 = getResources().getStringArray(R.array.timeHourValues);
		for(int i=0;i<arr2.length;i++)
			{
			GlobalVars.alarmTimeHoursValues.add(String.valueOf(arr2[i]));
			}
		String[] arr3 = getResources().getStringArray(R.array.timeMinutesValues);
		for(int i=0;i<arr3.length;i++)
			{
			GlobalVars.alarmTimeMinutesValues.add(String.valueOf(arr3[i]));
			}

		//GETS SCREEN TIMEOUT VALUE
		String screenTimeOutString = GlobalVars.readFile("screentimeout.cfg");
		if (screenTimeOutString=="")
			{
			GlobalVars.settingsScreenTimeOut = Integer.valueOf(GlobalVars.settingsScreenTimeOutValues.get(GlobalVars.settingsScreenTimeOutValues.size() -1));
			GlobalVars.writeFile("screentimeout.cfg",String.valueOf(GlobalVars.settingsScreenTimeOut));
			}
			else
			{
			try
				{
				GlobalVars.settingsScreenTimeOut = Integer.valueOf(screenTimeOutString);
				}
				catch(Exception e)
				{
				GlobalVars.settingsScreenTimeOut = Integer.valueOf(GlobalVars.settingsScreenTimeOutValues.get(GlobalVars.settingsScreenTimeOutValues.size() -1));
				GlobalVars.writeFile("screentimeout.cfg",String.valueOf(GlobalVars.settingsScreenTimeOut));
				}
			}

		//SETS BLUETOOTH VALUE STATE
		GlobalVars.bluetoothEnabled = GlobalVars.isBluetoothEnabled();

		Handler handler = new Handler();
		handler.postDelayed(new Runnable()
			{
			@Override public void run()
				{
				speakOnResume = true;
				}
			}, 2000);

		GlobalVars.startService(MobilityLauncherService.class);

		mHandler = new MyHandler(this);

		IntentFilter filter = new IntentFilter();
		filter.addAction(AppService.ACTION_USB_PERMISSION_GRANTED);
		filter.addAction(AppService.ACTION_NO_USB);
		filter.addAction(AppService.ACTION_USB_DISCONNECTED);
		filter.addAction(AppService.ACTION_USB_NOT_SUPPORTED);
		filter.addAction(AppService.ACTION_USB_PERMISSION_NOT_GRANTED);
		registerReceiver(mUsbReceiver, filter);

		startService(AppService.class, usbConnection);
    	}
		
    @Override protected void onDestroy()
    	{
    	try
			{
			usbService.setHandler(null);
			}
			catch(Exception e)
			{
			}
		unregisterReceiver(mUsbReceiver);
		unbindService(usbConnection);
    	super.onDestroy();
    	}
		
	@Override public void onResume()
		{
		super.onResume();
		try{GlobalVars.alarmVibrator.cancel();}catch(NullPointerException e){}catch(Exception e){}
		GlobalVars.lastActivity = Main.class;
		GlobalVars.lastActivityArduino = this;
		GlobalVars.activityItemLocation=0;
		GlobalVars.activityItemLimit=10;
		GlobalVars.selectTextView(messages,false);
		GlobalVars.selectTextView(calls,false);
		GlobalVars.selectTextView(contacts,false);
		GlobalVars.selectTextView(music,false);
		GlobalVars.selectTextView(internet,false);
		GlobalVars.selectTextView(alarms,false);
		GlobalVars.selectTextView(voicerecorder,false);
		GlobalVars.selectTextView(applications,false);
		GlobalVars.selectTextView(settings,false);
		GlobalVars.selectTextView(status,false);
		
		//UPDATE ALARM COUNTER
		GlobalVars.setText(alarms,false, getResources().getString(R.string.mainAlarms) + " (" + GlobalVars.getPendingAlarmsForTodayCount() + ")");
		
		if (GlobalVars.deviceIsAPhone()==true)
			{
			messages.setText(GlobalVars.context.getResources().getString(R.string.mainMessages) + " (" + String.valueOf(GlobalVars.getMessagesUnreadCount()) + ")");
			}
		if (speakOnResume==true)
			{
			GlobalVars.talk(getResources().getString(R.string.layoutMainOnResume));
			}
		
		if (GlobalVars.deviceIsAPhone()==true)
			{
			calls.setText(GlobalVars.context.getResources().getString(R.string.mainCalls) + " (" + String.valueOf(GlobalVars.getCallsMissedCount()) + ")");
			}
			else
			{
			calls.setText(GlobalVars.context.getResources().getString(R.string.mainCalls) + " (0)");
			}

		// CHECKS IF THE PERMISSION FOR PROFILE CHANGING IS GRANTED
			if (Build.VERSION.SDK_INT>=23)
			{
			testProfileChanger();
			}

		//HIDES THE NAVIGATION BAR
		if (Build.VERSION.SDK_INT>11){try{GlobalVars.hideNavigationBar(this);}catch(Exception e){}}
		}
	
	@Override public String toString()
		{
		int result = GlobalVars.detectArduinoKeyUp();
		switch (result)
			{
			case GlobalVars.ACTION_SELECT:
			select();
			break;

			case GlobalVars.ACTION_EXECUTE:
			execute();
			break;
			}
		return null;
		}
	
    public void onInit(int status)
    	{
		if (status == TextToSpeech.SUCCESS)
			{
			GlobalVars.talk(getResources().getString(R.string.mainWelcome));
			}
			else
			{
			ContextThemeWrapper themedContext = new ContextThemeWrapper(this, android.R.style.Theme_Holo_Light_Dialog_NoActionBar);
			new AlertDialog.Builder(themedContext).setTitle(getResources().getString(R.string.mainNoTTSInstalledTitle)).setMessage(getResources().getString(R.string.mainNoTTSInstalledMessage)).setPositiveButton(getResources().getString(R.string.mainNoTTSInstalledButton),new DialogInterface.OnClickListener()
				{
				public void onClick(DialogInterface dialog,int which)
					{
					try
						{
						startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.tts")));
						}
						catch (ActivityNotFoundException e)
						{
						try
							{
						    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.tts")));
							}
							catch (ActivityNotFoundException e2)
							{
							}
						}
					}
				}).show();
			}
		}

	public void shutdownEverything()
		{
		GlobalVars.activityItemLocation=0;
		GlobalVars.context = null;
		try
			{
			GlobalVars.cursor.close();
			}
			catch(Exception e)
			{
			}
		try
			{
			GlobalVars.tts.shutdown();
			}
			catch(Exception e)
			{
			}
		try
			{
			if (GlobalVars.musicPlayer!=null)
				{
				GlobalVars.musicPlayer.stop();
				GlobalVars.musicPlayer.reset();
				GlobalVars.musicPlayer.release();
				GlobalVars.musicPlayer = null;
				}
			}
			catch(Exception e)
			{
			}
		GlobalVars.musicPlayerPlayingSongIndex = -1;
		try
			{
			GlobalVars.stopService(MobilityLauncherService.class);
			}
			catch(Exception e)
			{
			}
		}
		
	public void select()
		{
		switch (GlobalVars.activityItemLocation)
			{
			case 1: //MESSAGES
			if (GlobalVars.deviceIsAPhone()==true)
				{
				int smsUnread = GlobalVars.getMessagesUnreadCount();
				if (smsUnread==0)
					{
					GlobalVars.talk(getResources().getString(R.string.mainMessagesNoNew));
					}
				else if (smsUnread==1)
					{
					GlobalVars.talk(getResources().getString(R.string.mainMessagesOneNew));
					}
				else
					{
					GlobalVars.talk(getResources().getString(R.string.mainMessages) + ". " + smsUnread + " " + getResources().getString(R.string.mainMessagesNew));
					}
				}
				else
				{
				GlobalVars.talk(getResources().getString(R.string.mainMessagesNotAvailable));
				}
			GlobalVars.selectTextView(messages,true);
			GlobalVars.selectTextView(calls,false);
			GlobalVars.selectTextView(status,false);
			break;
		
			case 2: //CALLS
			if (GlobalVars.deviceIsAPhone()==true)
				{
				int missedCalls = GlobalVars.getCallsMissedCount();
				if (missedCalls==0)
					{
					GlobalVars.talk(getResources().getString(R.string.mainCallsNoMissed));
					}
				else if (missedCalls==1)
					{
					GlobalVars.talk(getResources().getString(R.string.mainCallsOneMissed));
					}
				else
					{
					GlobalVars.talk(getResources().getString(R.string.mainCalls) + ". " + missedCalls + " " + getResources().getString(R.string.mainCallsMissed));
					}
				}
				else
				{
				GlobalVars.talk(getResources().getString(R.string.mainCallsNotAvailable));
				}
			GlobalVars.selectTextView(calls, true);
			GlobalVars.selectTextView(messages,false);
			GlobalVars.selectTextView(contacts,false);
			break;
		
			case 3: //CONTACTS
			GlobalVars.selectTextView(contacts,true);
			GlobalVars.selectTextView(calls,false);
			GlobalVars.selectTextView(music,false);
			GlobalVars.talk(getResources().getString(R.string.mainContacts));
			break;
		
			case 4: //MUSIC
			GlobalVars.selectTextView(music,true);
			GlobalVars.selectTextView(contacts,false);
			GlobalVars.selectTextView(internet,false);
			GlobalVars.talk(getResources().getString(R.string.mainMusicPlayer));
			break;
		
			case 5: //INTERNET
			GlobalVars.selectTextView(internet,true);
			GlobalVars.selectTextView(music,false);
			GlobalVars.selectTextView(alarms,false);
			GlobalVars.talk(getResources().getString(R.string.mainBrowser));
			break;
		
			case 6: //ALARMS
			GlobalVars.selectTextView(alarms,true);
			GlobalVars.selectTextView(internet,false);
			GlobalVars.selectTextView(voicerecorder,false);
			GlobalVars.talk(GlobalVars.getPendingAlarmsForTodayCountText());
			break;
		
			case 7: //VOICE RECORDER
			GlobalVars.selectTextView(voicerecorder,true);
			GlobalVars.selectTextView(alarms,false);
			GlobalVars.selectTextView(applications,false);
			GlobalVars.talk(getResources().getString(R.string.mainVoiceRecorder));
			break;
		
			case 8: //APPLICATIONS
			GlobalVars.selectTextView(applications,true);
			GlobalVars.selectTextView(voicerecorder,false);
			GlobalVars.selectTextView(settings,false);
			GlobalVars.talk(getResources().getString(R.string.mainApplications));
			break;
		
			case 9: //SETTINGS
			GlobalVars.selectTextView(settings,true);
			GlobalVars.selectTextView(applications,false);
			GlobalVars.selectTextView(status,false);
			GlobalVars.talk(getResources().getString(R.string.mainSettings));
			break;
		
			case 10: //STATUS
			GlobalVars.selectTextView(status,true);
			GlobalVars.selectTextView(messages,false);
			GlobalVars.selectTextView(settings,false);
			GlobalVars.talk(getResources().getString(R.string.mainStatus));
			break;
			}
		}
	
	public void execute()
		{
		switch (GlobalVars.activityItemLocation)
			{
			case 1: //MESSAGES
			if (GlobalVars.deviceIsAPhone()==true)
				{
				GlobalVars.startActivity(Messages.class);
				}
				else
				{
				GlobalVars.talk(getResources().getString(R.string.mainNotAvailable));
				}
			break;
		
			case 2: //CALLS
			//CHECKS THE ANDROID VERSION
			if (Build.VERSION.SDK_INT>=23)
				{
				// CHECKS IF THE APP HAS READ LOGS PERMISSION
				if (hasReadLogsPermission()==true)
					{
					GlobalVars.startActivity(Calls.class);
					}
					else
					{
					GlobalVars.talk(getResources().getString(R.string.mainCallsNotAvailableOS));
					}
				}
				else
				{
				GlobalVars.startActivity(Calls.class);
				}
			break;
		
			case 3: //CONTACTS
			GlobalVars.startActivity(Contacts.class);
			break;
		
			case 4: //MUSIC
			if (GlobalVars.musicPlayerDatabaseReady==true)
				{
				GlobalVars.startActivity(MusicPlayer.class);
				}
				else
				{
				GlobalVars.talk(getResources().getString(R.string.mainMusicPlayerPleaseTryAgain));
				}
			break;
		
			case 5: //INTERNET
			GlobalVars.startActivity(Browser.class);
			break;
		
			case 6: //ALARMS
			GlobalVars.startActivity(Alarms.class);
			break;
		
			case 7: //VOICE RECORDER
			GlobalVars.startActivity(VoiceRecorder.class);
			break;
		
			case 8: //APPLICATIONS
			GlobalVars.startActivity(Applications.class);
			break;
		
			case 9: //SETTINGS
			GlobalVars.startActivity(SettingsApp.class);
			break;
		
			case 10: //STATUS
			GlobalVars.talk(getDeviceStatus());
			break;
			}
		}
	
	public boolean onKeyUp(int keyCode, KeyEvent event)
		{
		if (keyCode==KeyEvent.KEYCODE_BACK)
			{
			if (GlobalVars.isMLTheDefaultLauncher()==false)
				{
				if (okToFinish==false)
					{
					okToFinish=true;
					Handler handler = new Handler();
					handler.postDelayed(new Runnable()
						{
						@Override public void run()
							{
							okToFinish = false;
							}
						}, 3000);
					GlobalVars.talk(getResources().getString(R.string.mainPressBack));
					return false;
					}
					else
					{
					shutdownEverything();
					this.finish();
					}
				}
				else
				{
				return false;
				}
			}
		return false;
		}

	private String getDeviceStatus()
		{
		String year = GlobalVars.getYear();
		String month = GlobalVars.getMonthName(Integer.valueOf(GlobalVars.getMonth()));
		String day = Integer.toString(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
		String dayname = GlobalVars.getDayName(Calendar.getInstance().get(Calendar.DAY_OF_WEEK));
		String hour = Integer.toString(Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
		String minutes = Integer.toString(Calendar.getInstance().get(Calendar.MINUTE));

		String textStatus = "";

		textStatus = textStatus +
					 getResources().getString(R.string.mainBatteryChargedAt) +
					 String.valueOf(batteryLevel() +
					 getResources().getString(R.string.mainPercentAndTime) +
					 hour + getResources().getString(R.string.mainHours) +
					 minutes + getResources().getString(R.string.mainMinutesAndDate) +
					 dayname + " " + day + getResources().getString(R.string.mainOf) +
					 month + getResources().getString(R.string.mainOf) + year);

		if (GlobalVars.batteryAt100==true)
			{
			textStatus = textStatus + getResources().getString(R.string.deviceChargedStatus);
			}
		else if (GlobalVars.batteryIsCharging==true)
			{
			textStatus = textStatus + getResources().getString(R.string.deviceChargingStatus);
			}

		if (GlobalVars.deviceIsAPhone()==true)
			{
			if (GlobalVars.deviceIsConnectedToMobileNetwork()==true)
				{
				textStatus = textStatus + getResources().getString(R.string.mainCarrierIs) + getCarrier();
				}
				else
				{
				textStatus = textStatus + getResources().getString(R.string.mainNoSignal);
				}
			}
		
		AudioManager audioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
		switch(audioManager.getRingerMode())
			{
			case AudioManager.RINGER_MODE_NORMAL:
			textStatus = textStatus + getResources().getString(R.string.mainProfileIsNormal);
			break;

			case AudioManager.RINGER_MODE_SILENT:
			textStatus = textStatus + getResources().getString(R.string.mainProfileIsSilent);
			break;

			case AudioManager.RINGER_MODE_VIBRATE:
			textStatus = textStatus + getResources().getString(R.string.mainProfileIsVibrate);
			break;
			}
			
		if (GlobalVars.isWifiEnabled())
			{
			String name = GlobalVars.getWifiSSID();
			if (name=="")
				{
				textStatus = textStatus + getResources().getString(R.string.mainWifiOnWithoutNetwork);
				}
				else
				{
				textStatus = textStatus + getResources().getString(R.string.mainWifiOnWithNetwork) + name + ".";
				}
			}
			else
			{
			textStatus = textStatus + getResources().getString(R.string.mainWifiOff);
			}
		return textStatus;
		}
		
	private int batteryLevel()
		{
    	Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    	int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
    	int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
    	if(level == -1 || scale == -1)
			{
    		return (int)50.0f;
			}
    	return (int)(((float)level / (float)scale) * 100.0f); 
		}
		
	private String getCarrier()
		{
		try
			{
			TelephonyManager telephonyManager = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE));
			String carrier;
			carrier = telephonyManager.getSimOperatorName();
			if (carrier==null | carrier=="")
				{
				return getResources().getString(R.string.mainCarrierNotAvailable);
				}
				else
				{
				return carrier;
				}
			}
			catch(Exception e)
			{
			return getResources().getString(R.string.mainCarrierNotAvailable);
			}
		}

	@TargetApi(Build.VERSION_CODES.M)
	private boolean hasReadLogsPermission()
		{
		try
			{
			int checkPermission = checkSelfPermission(Manifest.permission.READ_CALL_LOG);
			if (checkPermission == PackageManager.PERMISSION_GRANTED)
				{
				return true;
				}
			}
			catch (Exception e)
			{
			}
		return false;
		}

	@TargetApi(Build.VERSION_CODES.M)
	public void marshmallowPermissions()
		{
		List<String> listPermissionsNeeded = new ArrayList<String>();

		if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED)
			{
			listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
			listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
			}

		if (checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED)
			{
			listPermissionsNeeded.add(Manifest.permission.READ_CONTACTS);
			}

		if (checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED)
			{
			listPermissionsNeeded.add(Manifest.permission.READ_CONTACTS);
			listPermissionsNeeded.add(Manifest.permission.WRITE_CONTACTS);
			}

		if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
			{
			listPermissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
			}

		if (checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED)
			{
			listPermissionsNeeded.add(Manifest.permission.READ_SMS);
			listPermissionsNeeded.add(Manifest.permission.SEND_SMS);
			listPermissionsNeeded.add(Manifest.permission.RECEIVE_SMS);
			}

		if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED)
			{
			listPermissionsNeeded.add(Manifest.permission.CALL_PHONE);
			listPermissionsNeeded.add(Manifest.permission.PROCESS_OUTGOING_CALLS);
			}

		if (checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED)
			{
			listPermissionsNeeded.add(Manifest.permission.READ_CALL_LOG);
			listPermissionsNeeded.add(Manifest.permission.WRITE_CALL_LOG);
			}

		if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)
			{
			listPermissionsNeeded.add(Manifest.permission.READ_PHONE_STATE);
			}

		// ANSWERING CALLS FOR ANDROID 8.0 AND ABOVE
		if(Build.VERSION.SDK_INT >= 26)
			{
			if(checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED)
				{
				listPermissionsNeeded.add(Manifest.permission.ANSWER_PHONE_CALLS);
				}
			}

		if (!listPermissionsNeeded.isEmpty())
			{
			requestPermissions(listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),PERMISSION_REQUEST_CODE);
			}
		}

	@Override protected void onActivityResult(int requestCode, int resultCode, Intent data)
		{
		if (requestCode == SETTINGS_REQUEST_CODE)
			{
			if (Build.VERSION.SDK_INT>=23)
				{
				try
					{
					executeDrawOverlays();
					}
				catch(Exception e)
					{
					}
				}
			}
		}

	@TargetApi(Build.VERSION_CODES.M)
	public void testDrawOverlays()
		{
		if (!Settings.canDrawOverlays(this))
			{
			ContextThemeWrapper themedContext = new ContextThemeWrapper(this, android.R.style.Theme_Holo_Light_Dialog_NoActionBar);
			new AlertDialog.Builder(themedContext).setCancelable(false).setTitle(getResources().getString(R.string.googleRequestTitle)).setMessage(getResources().getString(R.string.googleRequest1)).setPositiveButton(getResources().getString(R.string.googleRequestOk),new DialogInterface.OnClickListener()
				{
				public void onClick(DialogInterface dialog,int which)
					{
					try
						{
						Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + activity.getPackageName()));
						activity.startActivityForResult(intent, SETTINGS_REQUEST_CODE);
						}
						catch(Exception e)
						{
						}
					}
				}).show();
			}
		}

	@TargetApi(Build.VERSION_CODES.M)
	public void executeDrawOverlays()
		{
		try
			{
			if (Settings.canDrawOverlays(this))
				{
				MobilityLauncherService.addFloatingView();
				}
			}
			catch(Exception e)
			{
			}
		}

	@TargetApi(Build.VERSION_CODES.M)
	public void testProfilePermission()
		{
		if (Build.MANUFACTURER.equals("LGE")) // PATCH FOR LG DEVICES
			{
			try
				{
				List<String> listPermissionsNeeded = new ArrayList<String>();
					if (checkSelfPermission(Manifest.permission.ACCESS_NOTIFICATION_POLICY)!=PackageManager.PERMISSION_GRANTED)
					{
					listPermissionsNeeded.add(Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE);
					}
				requestPermissions(listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),PERMISSION_REQUEST_CODE);
				}
				catch(Exception e)
				{
				}
			}
			else
			{
			NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

			if (!notificationManager.isNotificationPolicyAccessGranted())
				{
				ContextThemeWrapper themedContext = new ContextThemeWrapper(this, android.R.style.Theme_Holo_Light_Dialog_NoActionBar);
				new AlertDialog.Builder(themedContext).setCancelable(false).setTitle(getResources().getString(R.string.googleRequestTitle)).setMessage(getResources().getString(R.string.googleRequest2)).setPositiveButton(getResources().getString(R.string.googleRequestOk),new DialogInterface.OnClickListener()
				{
				public void onClick(DialogInterface dialog,int which)
					{
					try
						{
						Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
						startActivity(intent);
						}
						catch(Exception e)
						{
						}
					}
				}).show();
				}
			}
		}

	@TargetApi(Build.VERSION_CODES.M)
	public void testProfileChanger()
		{
		if (hasProfileChangerPermission==false)
			{
			NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			if (notificationManager.isNotificationPolicyAccessGranted())
				{
				hasProfileChangerPermission = true;
				try
					{
					//SETS PROFILE TO NORMAL
					AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
					audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
					}
					catch(Exception e)
					{
					}
				}
			}
		}

	@TargetApi(Build.VERSION_CODES.M)
	@Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
		{
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == PERMISSION_REQUEST_CODE)
			{
			try
				{
				if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED)
					{
					if (GlobalVars.musicPlayerDatabaseReady==true)
						{
						new MusicPlayerThreadRefreshDatabase().execute("");
						}
					}
				}
				catch(Exception e)
				{
				}
			}
		}

	private void startService(Class<?> service, ServiceConnection serviceConnection)
		{
		if (!AppService.SERVICE_CONNECTED)
			{
			Intent startService = new Intent(this, service);
			startService(startService);
			}
		Intent bindingIntent = new Intent(this, service);
		bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
		}

	private void addReceivedData(String data)
		{
		arduinoValue = arduinoValue + data;

		if (arduinoValue.toLowerCase().contains(GlobalVars.ARDUINO_UP_STRING))
			{
			GlobalVars.arduinoKeyPressed = GlobalVars.ARDUINO_UP;
			GlobalVars.lastActivityArduino.toString();
			MobilityLauncherService.acceptCall();
			arduinoValue = "";
			}
		else if (arduinoValue.toLowerCase().contains(GlobalVars.ARDUINO_DOWN_STRING))
			{
			GlobalVars.arduinoKeyPressed = GlobalVars.ARDUINO_DOWN;
			GlobalVars.lastActivityArduino.toString();
			MobilityLauncherService.rejectCall();
			arduinoValue = "";
			}
		else if (arduinoValue.toLowerCase().contains(GlobalVars.ARDUINO_LEFT_STRING))
			{
			GlobalVars.arduinoKeyPressed = GlobalVars.ARDUINO_LEFT;
			GlobalVars.lastActivityArduino.toString();
			arduinoValue = "";
			}
		else if (arduinoValue.toLowerCase().contains(GlobalVars.ARDUINO_RIGHT_STRING))
			{
			GlobalVars.arduinoKeyPressed = GlobalVars.ARDUINO_RIGHT;
			GlobalVars.lastActivityArduino.toString();
			arduinoValue = "";
			}
		else if (arduinoValue.toLowerCase().contains(GlobalVars.ARDUINO_SELECT_STRING))
			{
			GlobalVars.arduinoKeyPressed = GlobalVars.ARDUINO_SELECT;
			GlobalVars.lastActivityArduino.toString();
			arduinoValue = "";
			PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
			boolean isScreenOn = pm.isScreenOn();
			if (isScreenOn==false)
				{
				try
					{
					PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
					PowerManager.ACQUIRE_CAUSES_WAKEUP |
					PowerManager.ON_AFTER_RELEASE, "TurnOnTheScreenTag");
					wl.acquire();
					wl.release();
					}
					catch(NullPointerException e)
					{
					}
					catch(Exception e)
					{
					}
				}
				else
				{
				try
					{
					PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TurnOffTheScreenTag");
					wl.acquire();
					}
					catch(NullPointerException e)
					{
					}
					catch(Exception e)
					{
					}
				}
			}
		}

	private static class MyHandler extends Handler
		{
		private final WeakReference<Main> mActivity;

		public MyHandler(Main activity)
			{
			mActivity = new WeakReference<>(activity);
			}

		@Override public void handleMessage(Message msg)
			{
			switch (msg.what)
				{
				case AppService.MESSAGE_FROM_SERIAL_PORT:
				String data = (String) msg.obj;
				if (data != null)
					{
					mActivity.get().addReceivedData(data);
					}
				break;

				default:
				break;
				}
			}
		}

	private final ServiceConnection usbConnection = new ServiceConnection()
		{
		@Override public void onServiceConnected(ComponentName arg0, IBinder arg1)
			{
			usbService = ((AppService.UsbBinder) arg1).getService();
			}

		@Override public void onServiceDisconnected(ComponentName arg0)
			{
			usbService = null;
			}
		};

	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver()
		{
		@Override public void onReceive(Context context, Intent intent)
			{
			switch (intent.getAction())
				{
				case AppService.ACTION_USB_PERMISSION_GRANTED:
				try
					{
					usbService.setHandler(mHandler);
					}
					catch(Exception e)
					{
					}
				break;

				case AppService.ACTION_USB_DISCONNECTED:
				try
					{
					usbService.setHandler(null);
					}
					catch(Exception e)
					{
					}
				break;

				default:
				break;
				}
			}
		};
	}