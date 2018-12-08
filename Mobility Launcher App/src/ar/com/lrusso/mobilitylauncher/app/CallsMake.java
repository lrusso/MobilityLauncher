package ar.com.lrusso.mobilitylauncher.app;

import android.os.Bundle;
import android.widget.TextView;
import ar.com.lrusso.mobilitylauncher.app.R;
import android.app.Activity;

public class CallsMake extends Activity
	{
	private TextView phonenumber;
	private TextView makecall;
	private TextView goback;
	private String phoneValue = "";
	
    @Override protected void onCreate(Bundle savedInstanceState)
    	{
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.callsmake);
		GlobalVars.lastActivity = CallsMake.class;
		GlobalVars.lastActivityArduino = this;
		phonenumber = (TextView) findViewById(R.id.phonenumber);
		makecall = (TextView) findViewById(R.id.makecall);
		goback = (TextView) findViewById(R.id.goback);
		GlobalVars.activityItemLocation=0;
		GlobalVars.activityItemLimit=3;
        }
		
	@Override public void onResume()
		{
		super.onResume();
		try{GlobalVars.alarmVibrator.cancel();}catch(NullPointerException e){}catch(Exception e){}
		if (GlobalVars.inputModeResult!=null)
			{
			phoneValue = GlobalVars.inputModeResult;
			GlobalVars.setText(phonenumber, false, phoneValue);
			GlobalVars.inputModeResult = null;
			}
		GlobalVars.lastActivity = CallsMake.class;
		GlobalVars.lastActivityArduino = this;
		GlobalVars.activityItemLocation=0;
		GlobalVars.activityItemLimit=3;
		GlobalVars.selectTextView(phonenumber,false);
		GlobalVars.selectTextView(makecall,false);
		GlobalVars.selectTextView(goback,false);
		GlobalVars.talk(getResources().getString(R.string.layoutCallsMakeCallOnResume));
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
		
	public void select()
		{
		switch (GlobalVars.activityItemLocation)
			{
			case 1: //INPUT PHONE NUMBER
			GlobalVars.selectTextView(phonenumber,true);
			GlobalVars.selectTextView(makecall,false);
			GlobalVars.selectTextView(goback,false);
			if (phoneValue=="")
				{
				GlobalVars.talk(getResources().getString(R.string.layoutCallsMakeCallPhone2));
				}
				else
				{
				GlobalVars.talk(getResources().getString(R.string.layoutCallsMakeCallPhone3) + 
								GlobalVars.divideNumbersWithBlanks(phoneValue));
				}
			break;

			case 2: //MAKE A CALL
			GlobalVars.selectTextView(makecall, true);
			GlobalVars.selectTextView(phonenumber,false);
			GlobalVars.selectTextView(goback,false);
			GlobalVars.talk(getResources().getString(R.string.layoutCallsMakeCallCall));
			break;

			case 3: //GO BACK TO THE PREVIOUS MENU
			GlobalVars.selectTextView(goback,true);
			GlobalVars.selectTextView(phonenumber,false);
			GlobalVars.selectTextView(makecall,false);
			GlobalVars.talk(getResources().getString(R.string.backToPreviousMenu));
			break;
			}
		}
		
	public void execute()
		{
		switch (GlobalVars.activityItemLocation)
			{
			case 1: //INPUT PHONE NUMBER
			GlobalVars.inputModeKeyboardOnlyNumbers = true;
			GlobalVars.startInputActivity();
			break;

			case 2: //MAKE A CALL
			if (phoneValue=="")
				{
				GlobalVars.talk(getResources().getString(R.string.layoutCallsMakeCallCallError));
				}
				else
				{
				GlobalVars.callTo("tel:" + phoneValue);
				}
			break;

			case 3: //GO BACK TO THE PREVIOUS MENU
			this.finish();
			break;
			}
		}
	}