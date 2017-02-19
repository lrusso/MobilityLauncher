package ar.com.lrusso.mobilitylauncher;

import android.app.*;
import android.content.*;
import android.hardware.usb.UsbManager;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.preference.PreferenceManager;
import android.speech.tts.*;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.telephony.*;
import android.view.*;
import android.view.View.OnKeyListener;
import android.widget.*;
import java.util.*;

public class Main extends Activity implements TextToSpeech.OnInitListener
	{
	private static final int DISP_CHAR  = 0;
	private static final int DISP_DEC   = 1;
	private static final int DISP_HEX   = 2;
	private static final int LINEFEED_CODE_CR   = 0;
	private static final int LINEFEED_CODE_CRLF = 1;
	private static final int LINEFEED_CODE_LF   = 2;
	private int mWriteLinefeedCode = LINEFEED_CODE_LF;
	private static String arduinoValue = "";
	FTDriver mSerial;
	private StringBuilder mText = new StringBuilder();
	private boolean mStop = false;
	boolean lastDataIs0x0D = false;
	Handler mHandler = new Handler();
	private int mDisplayType = DISP_CHAR;
	private int mReadLinefeedCode = LINEFEED_CODE_LF;
	private int mBaudrate = FTDriver.BAUD9600;
	private int mDataBits = FTDriver.FTDI_SET_DATA_BITS_8;
	private int mParity = FTDriver.FTDI_SET_DATA_PARITY_NONE;
	private int mStopBits = FTDriver.FTDI_SET_DATA_STOP_BITS_1;
	private int mFlowControl = FTDriver.FTDI_SET_FLOW_CTRL_NONE;
	private int mBreak = FTDriver.FTDI_SET_NOBREAK;
	private boolean mRunningMainLoop = false;
	private static final String ACTION_USB_PERMISSION = "ar.com.lrusso.mobilitylauncher.USB_PERMISSION";
	private final static String BR = System.getProperty("line.separator");
	
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
	private boolean okToFinish = false;
	
    @Override protected void onCreate(Bundle savedInstanceState)
    	{
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.main);
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
		GlobalVars.tts.setOnUtteranceCompletedListener(new OnUtteranceCompletedListener()
			{
			@Override public void onUtteranceCompleted(String utteranceId)
				{
				try
					{
					GlobalVars.musicPlayer.setVolume(1f,1f);
					}
					catch(NullPointerException e)
					{
					}
					catch(Exception e)
					{
					}
				}
			});

		//SETS THE ALARM VIBRATOR VARIABLE
		GlobalVars.alarmVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		
		//SETS PROFILE TO NORMAL
		AudioManager audioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
		audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);

		GlobalVars.alarmAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		GlobalVars.openAndLoadAlarmFile();
		GlobalVars.setText(alarms,false, getResources().getString(R.string.mainAlarms) + " (" + GlobalVars.getPendingAlarmsForTodayCount() + ")");
		
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
			GlobalVars.cursor = manager.getCursor();
			while (GlobalVars.cursor.moveToNext())
				{
				GlobalVars.settingsToneAlarmTitle.add(GlobalVars.cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX));
				GlobalVars.settingsToneAlarmUri.add(GlobalVars.cursor.getString(RingtoneManager.URI_COLUMN_INDEX));
				GlobalVars.settingsToneAlarmID.add(GlobalVars.cursor.getString(RingtoneManager.ID_COLUMN_INDEX));
				}
			}
			catch(Exception e)
			{
			}
			
		//GETS EVERY NOTIFICATION TONE
		try
			{
			RingtoneManager manager = new RingtoneManager(this);
			manager.setType(RingtoneManager.TYPE_NOTIFICATION);
			GlobalVars.cursor = manager.getCursor();
			while (GlobalVars.cursor.moveToNext())
				{
				GlobalVars.settingsToneNotificationTitle.add(GlobalVars.cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX));
				GlobalVars.settingsToneNotificationUri.add(GlobalVars.cursor.getString(RingtoneManager.URI_COLUMN_INDEX));
				GlobalVars.settingsToneNotificationID.add(GlobalVars.cursor.getString(RingtoneManager.ID_COLUMN_INDEX));
				}
			}
			catch(Exception e)
			{
			}
		
		//GETS EVERY CALL TONE
		try
			{
			RingtoneManager manager = new RingtoneManager(this);
			manager.setType(RingtoneManager.TYPE_RINGTONE);
			GlobalVars.cursor = manager.getCursor();
			while (GlobalVars.cursor.moveToNext())
				{
			    GlobalVars.settingsToneCallTitle.add(GlobalVars.cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX));
			    GlobalVars.settingsToneCallUri.add(GlobalVars.cursor.getString(RingtoneManager.URI_COLUMN_INDEX));
			    GlobalVars.settingsToneCallID.add(GlobalVars.cursor.getString(RingtoneManager.ID_COLUMN_INDEX));
				}
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
		
		//STARTS ARDUINO DETECTING CODE
		mBaudrate = FTDriver.BAUD9600;
		mSerial = new FTDriver((UsbManager) getSystemService(Context.USB_SERVICE));
		IntentFilter filter = new IntentFilter();
		filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		registerReceiver(mUsbReceiver, filter);
		mBaudrate = loadDefaultBaudrate();
		PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		mSerial.setPermissionIntent(permissionIntent);
		if (mSerial.begin(mBaudrate))
			{
			loadDefaultSettingValues();
			mainloop();
			}
			else
			{
			}
    	}
		
    @Override protected void onDestroy()
    	{
		shutdownEverything();
		mSerial.end();
		mStop = true;
		unregisterReceiver(mUsbReceiver);
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
		}
	
	@Override public String toString()
		{
		if (GlobalVars.arduinoStringToSend!="")
			{
			try
				{
				String strWrite = GlobalVars.arduinoStringToSend;
				strWrite = changeLinefeedcode(strWrite);
				mSerial.write(strWrite.getBytes(), strWrite.length()-1);
				}
				catch(NullPointerException e)
				{
				}
				catch(Exception e)
				{
				}
			GlobalVars.arduinoStringToSend="";
			}
			else
			{
			if (MobilityLauncherService.myView!=null)
				{
				if (MobilityLauncherService.myView.getVisibility()==View.VISIBLE)
					{
					int result = GlobalVars.detectArduinoKeyUpWhileCalling();
					switch (result)
						{
						case GlobalVars.ARDUINO_UP:
						MobilityLauncherService.acceptCall();
						break;
						
						case GlobalVars.ARDUINO_DOWN:
						MobilityLauncherService.rejectCall();
						break;
						}
					GlobalVars.arduinoKeyPressed=-1;
					}
					else
					{
					normalKeyBehavior();
					}
				}
				else
				{
				normalKeyBehavior();
				}
			}
		GlobalVars.arduinoKeyPressed = -1;
		return null;
		}
	
	public void normalKeyBehavior()
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
		}
	
    public void onInit(int status)
    	{
		if (status == TextToSpeech.SUCCESS)
			{
			GlobalVars.talk(getResources().getString(R.string.mainWelcome));
			}
			else
			{
			new AlertDialog.Builder(this).setTitle(getResources().getString(R.string.mainNoTTSInstalledTitle)).setMessage(getResources().getString(R.string.mainNoTTSInstalledMessage)).setPositiveButton(getResources().getString(R.string.mainNoTTSInstalledButton),new DialogInterface.OnClickListener()
				{
				public void onClick(DialogInterface dialog,int which)
					{
					try
						{
					    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=id=com.google.android.tts")));
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
			if (GlobalVars.deviceIsAPhone()==true)
				{
				GlobalVars.startActivity(Calls.class);
				}
				else
				{
				GlobalVars.talk(getResources().getString(R.string.mainNotAvailable));
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
			GlobalVars.startActivity(Settings.class);
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
	
	private void mainloop()
		{
		mStop = false;
		mRunningMainLoop = true;
		new Thread(mLoop).start();
		}

	private Runnable mLoop = new Runnable()
		{
		@Override public void run()
			{
			int len;
			byte[] rbuf = new byte[4096];
			for (;;)
				{
				len = mSerial.read(rbuf);
				rbuf[len] = 0;
				if (len > 0)
					{
					switch (mDisplayType)
						{
						case DISP_CHAR:
						setSerialDataToTextView(mDisplayType, rbuf, len, "", "");
						break;
					
						case DISP_DEC:
						setSerialDataToTextView(mDisplayType, rbuf, len, "013", "010");
						break;
					
						case DISP_HEX:
						setSerialDataToTextView(mDisplayType, rbuf, len, "0d", "0a");
						break;
						}
					mHandler.post(new Runnable()
						{
						public void run()
							{
							arduinoValue = arduinoValue + mText;
							if (arduinoValue.toLowerCase().contains(GlobalVars.ARDUINO_UP_STRING))
								{
								GlobalVars.arduinoKeyPressed = GlobalVars.ARDUINO_UP;
								GlobalVars.lastActivityArduino.toString();
								arduinoValue = "";
								}
							else if (arduinoValue.toLowerCase().contains(GlobalVars.ARDUINO_DOWN_STRING))
								{
								GlobalVars.arduinoKeyPressed = GlobalVars.ARDUINO_DOWN;
								GlobalVars.lastActivityArduino.toString();
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
							mText.setLength(0);
							}
						});
					}
				try
					{
					Thread.sleep(50);
					}
					catch (InterruptedException e)
					{
					}
				if (mStop)
					{
					mRunningMainLoop = false;
					return;
					}
				}
			}
		};

	private String IntToHex2(int Value)
		{
		char HEX2[] = {Character.forDigit((Value >> 4) & 0x0F, 16),Character.forDigit(Value & 0x0F, 16)};
		String Hex2Str = new String(HEX2);
		return Hex2Str;
		}

	void setSerialDataToTextView(int disp, byte[] rbuf, int len, String sCr, String sLf)
		{
		int tmpbuf;
		for (int i = 0; i < len; ++i)
			{
			if ((mReadLinefeedCode == LINEFEED_CODE_CR) && (rbuf[i] == 0x0D))
				{
				mText.append(sCr);
				mText.append(BR);
				}
			else if ((mReadLinefeedCode == LINEFEED_CODE_LF) && (rbuf[i] == 0x0A))
				{
				mText.append(sLf);
				mText.append(BR);
				}
			else if ((mReadLinefeedCode == LINEFEED_CODE_CRLF) && (rbuf[i] == 0x0D) && (rbuf[i + 1] == 0x0A))
				{
				mText.append(sCr);
				if (disp != DISP_CHAR)
					{
					mText.append(" ");
					}
				mText.append(sLf);
				mText.append(BR);
				++i;
				}
			else if ((mReadLinefeedCode == LINEFEED_CODE_CRLF) && (rbuf[i] == 0x0D))
				{
				mText.append(sCr);
				lastDataIs0x0D = true;
				}
			else if (lastDataIs0x0D && (rbuf[0] == 0x0A))
				{
				if (disp != DISP_CHAR)
					{
					mText.append(" ");
					}
				mText.append(sLf);
				mText.append(BR);
				lastDataIs0x0D = false;
				}
			else if (lastDataIs0x0D && (i != 0))
				{
				lastDataIs0x0D = false;
				--i;
				}
			else
				{
				switch (disp)
					{
					case DISP_CHAR:
					mText.append((char) rbuf[i]);
					break;
									
					case DISP_DEC:
					tmpbuf = rbuf[i];
					if (tmpbuf < 0)
						{
						tmpbuf += 256;
						}
					mText.append(String.format("%1$03d", tmpbuf));
					mText.append(" ");
					break;
									
					case DISP_HEX:
					mText.append(IntToHex2((int) rbuf[i]));
					mText.append(" ");
					break;
							
					default:
					break;
					}
				}
			}
		}

	void loadDefaultSettingValues()
		{
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		String res = pref.getString("display_list", Integer.toString(DISP_CHAR));
		mDisplayType = Integer.valueOf(res);
		res = pref.getString("fontsize_list", Integer.toString(12));
		res = pref.getString("readlinefeedcode_list", Integer.toString(LINEFEED_CODE_CRLF));
		mReadLinefeedCode = Integer.valueOf(res);
		res = pref.getString("databits_list", Integer.toString(FTDriver.FTDI_SET_DATA_BITS_8));
		mDataBits = Integer.valueOf(res);
		mSerial.setSerialPropertyDataBit(mDataBits, FTDriver.CH_A);
		res = pref.getString("parity_list", Integer.toString(FTDriver.FTDI_SET_DATA_PARITY_NONE));
		mParity = Integer.valueOf(res) << 8;
		mSerial.setSerialPropertyParity(mParity, FTDriver.CH_A);
		res = pref.getString("stopbits_list", Integer.toString(FTDriver.FTDI_SET_DATA_STOP_BITS_1));
		mStopBits = Integer.valueOf(res) << 11;
		mSerial.setSerialPropertyStopBits(mStopBits, FTDriver.CH_A);
		res = pref.getString("flowcontrol_list", Integer.toString(FTDriver.FTDI_SET_FLOW_CTRL_NONE));
		mFlowControl = Integer.valueOf(res) << 8;
		mSerial.setFlowControl(FTDriver.CH_A, mFlowControl);
		res = pref.getString("break_list", Integer.toString(FTDriver.FTDI_SET_NOBREAK));
		mBreak = Integer.valueOf(res) << 14;
		mSerial.setSerialPropertyBreak(mBreak, FTDriver.CH_A);
		mSerial.setSerialPropertyToChip(FTDriver.CH_A);
		}

	int loadDefaultBaudrate()
		{
		int res = FTDriver.BAUD9600;
		mBaudrate = FTDriver.BAUD9600;
		return res;
		}

	private void openUsbSerial()
		{
		if (!mSerial.isConnected())
			{
			mBaudrate = loadDefaultBaudrate();
			if (!mSerial.begin(mBaudrate))
				{
				//CONNECTION NOT ESTABLISHED
				return;
				}
				else
				{
				//CONNECTION ESTABLISHED
				//mensajeEstado(getResources().getString(R.string.textoConexionEstablecida));
				}
			}
		if (!mRunningMainLoop)
			{
			mainloop();
			}
		}

	protected void onNewIntent(Intent intent)
		{
		openUsbSerial();
		};
	
	BroadcastReceiver mUsbReceiver = new BroadcastReceiver()
		{
		public void onReceive(Context context, Intent intent)
			{
			String action = intent.getAction();
			if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action))
				{
				if (!mSerial.isConnected())
					{
					mBaudrate = loadDefaultBaudrate();
					mSerial.begin(mBaudrate);
					loadDefaultSettingValues();
					}
				if (!mRunningMainLoop)
					{
					mainloop();
					}
				}
				else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action))
				{
				mStop = true;
				mSerial.usbDetached(intent);
				mSerial.end();
				}
			else if (ACTION_USB_PERMISSION.equals(action))
				{
				synchronized (this)
					{
					if (!mSerial.isConnected())
						{
						mBaudrate = loadDefaultBaudrate();
						mSerial.begin(mBaudrate);
						loadDefaultSettingValues();
						}
					}
				if (!mRunningMainLoop)
					{
					mainloop();
					}
				}
			}
		};
		
	private String changeLinefeedcode(String str)
		{
		str = str.replace("\\r", "\r");
		str = str.replace("\\n", "\n");
		switch (mWriteLinefeedCode)
			{
			case LINEFEED_CODE_LF:
			str = str + "\n";
			break;
			
			default:
			}
		return str;
		}
	}