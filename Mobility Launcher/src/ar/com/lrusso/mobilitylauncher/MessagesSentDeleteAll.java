package ar.com.lrusso.mobilitylauncher;

import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.app.Activity;
import android.database.Cursor;
import ar.com.lrusso.mobilitylauncher.R;

public class MessagesSentDeleteAll extends Activity
	{
	private TextView delete;
	private TextView goback;
	
    @Override protected void onCreate(Bundle savedInstanceState)
    	{
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.messagessentdeleteall);
		GlobalVars.lastActivity = MessagesSentDeleteAll.class;
		GlobalVars.lastActivityArduino = this;
		delete = (TextView) findViewById(R.id.messagedelete);
		goback = (TextView) findViewById(R.id.goback);
		GlobalVars.messagesSentWereDeleted = false;
		GlobalVars.activityItemLocation=0;
		GlobalVars.activityItemLimit=2;
    	}
		
	@Override public void onResume()
		{
		super.onResume();
		try{GlobalVars.alarmVibrator.cancel();}catch(NullPointerException e){}catch(Exception e){}
		GlobalVars.lastActivity = MessagesSentDeleteAll.class;
		GlobalVars.lastActivityArduino = this;
		GlobalVars.activityItemLocation=0;
		GlobalVars.activityItemLimit=2;
		GlobalVars.selectTextView(delete,false);
		GlobalVars.selectTextView(goback,false);
		GlobalVars.talk(getResources().getString(R.string.layoutMessagesSentDeleteAllOnResume));
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
			case 1: //CONFIRM DELETE
			GlobalVars.selectTextView(delete, true);
			GlobalVars.selectTextView(goback,false);
			GlobalVars.talk(getResources().getString(R.string.layoutMessagesSentDeleteAllDelete));
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
			case 1: //CONFIRM DELETE
			deleteAllSentMessages();
			GlobalVars.messagesSentWereDeleted = true;
			this.finish();
			break;

			case 2: //GO BACK TO THE PREVIOUS MENU
			this.finish();
			break;
			}
		}

	private void deleteAllSentMessages()
		{
		try
			{
			Uri sentUri = Uri.parse("content://sms/sent");
			Cursor c = getContentResolver().query(sentUri , null, null, null, null);
			while (c.moveToNext())
				{
			    try
			    	{
			        // Delete the SMS
			        String pid = c.getString(0); // Get id;
			        String uri = "content://sms/" + pid;
			        getContentResolver().delete(Uri.parse(uri), null, null);
			    	}
			    	catch (Exception e)
			    	{
			    	}
				}
		    c.close();
			}
			catch(NullPointerException e)
			{
			}
			catch(Exception e)
			{
			}
		}
	}