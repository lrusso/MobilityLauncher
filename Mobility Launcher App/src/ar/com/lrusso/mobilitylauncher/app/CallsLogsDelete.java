package ar.com.lrusso.mobilitylauncher.app;

import android.app.*;
import android.os.*;
import android.provider.*;
import android.widget.*;
import ar.com.lrusso.mobilitylauncher.app.R;

public class CallsLogsDelete extends Activity
	{
	private TextView deletecalls;
	private TextView goback;
	   
    @Override protected void onCreate(Bundle savedInstanceState)
    	{
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.callslogsdelete);
		GlobalVars.lastActivity = CallsLogsDelete.class;
		GlobalVars.lastActivityArduino = this;
		deletecalls = (TextView) findViewById(R.id.deletecalllogs);
		goback = (TextView) findViewById(R.id.goback);
		GlobalVars.activityItemLocation=0;
		GlobalVars.activityItemLimit=2;
        }
    
	@Override public void onResume()
		{
		super.onResume();
		try{GlobalVars.alarmVibrator.cancel();}catch(NullPointerException e){}catch(Exception e){}
		GlobalVars.lastActivity = CallsLogsDelete.class;
		GlobalVars.lastActivityArduino = this;
		GlobalVars.activityItemLocation=0;
		GlobalVars.activityItemLimit=2;
		GlobalVars.selectTextView(deletecalls,false);
		GlobalVars.selectTextView(goback,false);
		GlobalVars.talk(getResources().getString(R.string.layoutCallsLogsDeleteOnResume));
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
			case 1: //DELETE ALL CALL LOGS
			GlobalVars.selectTextView(deletecalls,true);
			GlobalVars.selectTextView(goback,false);
			GlobalVars.talk(getResources().getString(R.string.layoutCallsLogsDelete));
			break;

			case 2: //GO BACK TO THE PREVIOUS MENU
			GlobalVars.selectTextView(goback,true);
			GlobalVars.selectTextView(deletecalls,false);
			GlobalVars.talk(getResources().getString(R.string.backToPreviousMenu));
			break;
			}
		}
		
	public void execute()
		{
		switch (GlobalVars.activityItemLocation)
			{
			case 1: //DELETE ALL CALL LOGS
			deleteAllCallLogs();
			GlobalVars.callLogsDeleted=true;
			this.finish();
			break;
			
			case 2: //GO BACK TO THE PREVIOUS MENU
			this.finish();
			break;
			}
		}
	
	public void deleteAllCallLogs()
		{
		try
			{
			getContentResolver().delete(CallLog.Calls.CONTENT_URI, null, null);
			}
			catch(Exception e)
			{
			}
		}
	}