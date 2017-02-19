package ar.com.lrusso.mobilitylauncher;

import android.os.Bundle;
import android.widget.TextView;
import android.app.Activity;
import ar.com.lrusso.mobilitylauncher.R;

public class AlarmsDeleteAll extends Activity
	{
	private TextView delete;
	private TextView goback;
	
    @Override protected void onCreate(Bundle savedInstanceState)
    	{
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.alarmsdeleteall);
		GlobalVars.lastActivity = AlarmsDeleteAll.class;
		GlobalVars.lastActivityArduino = this;
		delete = (TextView) findViewById(R.id.alarmsdelete);
		goback = (TextView) findViewById(R.id.goback);
		GlobalVars.activityItemLocation=0;
		GlobalVars.activityItemLimit=2;
		GlobalVars.alarmWereDeleted = false;
    	}
    
	@Override public void onResume()
		{
		super.onResume();
		try{GlobalVars.alarmVibrator.cancel();}catch(NullPointerException e){}catch(Exception e){}
		GlobalVars.lastActivity = AlarmsDeleteAll.class;
		GlobalVars.lastActivityArduino = this;
		GlobalVars.activityItemLocation=0;
		GlobalVars.activityItemLimit=2;
		GlobalVars.selectTextView(delete,false);
		GlobalVars.selectTextView(goback,false);
		GlobalVars.talk(getResources().getString(R.string.layoutAlarmsDeleteAllOnResume));
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
			case 1: //DELETE ALL ALARMS
			GlobalVars.selectTextView(delete, true);
			GlobalVars.selectTextView(goback,false);
			GlobalVars.talk(getResources().getString(R.string.layoutAlarmsDeleteAllDelete));
			break;

			case 2: //GO BACK TO THE PREVIOUS MENU
			GlobalVars.selectTextView(goback,true);
			GlobalVars.selectTextView(delete,false);
			GlobalVars.talk(getResources().getString(R.string.backToPreviousMenu));
			break;
			}
		}
		
	public void execute()
		{
		switch (GlobalVars.activityItemLocation)
			{
			case 1: //DELETE ALL ALARMS
			GlobalVars.deleteAllAlarms();
			GlobalVars.alarmWereDeleted=true;
			this.finish();
			break;

			case 2: //GO BACK TO THE PREVIOUS MENU
			GlobalVars.alarmToDeleteIndex = -1;
			this.finish();
			break;
			}
		}
	}