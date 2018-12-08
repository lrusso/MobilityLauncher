package ar.com.lrusso.mobilitylauncher.app;

import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import ar.com.lrusso.mobilitylauncher.app.R;
import android.app.Activity;

public class Contacts extends Activity
	{
	private TextView list;
	private TextView create;
	private TextView goback;
	
    @Override protected void onCreate(Bundle savedInstanceState)
    	{
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.contacts);
		GlobalVars.lastActivity = Contacts.class;
		GlobalVars.lastActivityArduino = this;
		list = (TextView) findViewById(R.id.contactslist);
		create = (TextView) findViewById(R.id.contactscreate);
		goback = (TextView) findViewById(R.id.goback);
		GlobalVars.activityItemLocation=0;
		GlobalVars.activityItemLimit=3;
    	}
    
	@Override public void onResume()
		{
		super.onResume();
		try{GlobalVars.alarmVibrator.cancel();}catch(NullPointerException e){}catch(Exception e){}
		GlobalVars.lastActivity = Contacts.class;
		GlobalVars.lastActivityArduino = this;
		GlobalVars.activityItemLocation=0;
		GlobalVars.activityItemLimit=3;
		GlobalVars.selectTextView(list,false);
		GlobalVars.selectTextView(create,false);
		GlobalVars.selectTextView(goback,false);
		if (GlobalVars.contactWasCreated==true)
			{
			GlobalVars.talk(getResources().getString(R.string.layoutContactsOnResumeCreated));
			GlobalVars.contactWasCreated=false;
			}
			else
			{
			GlobalVars.talk(getResources().getString(R.string.layoutContactsOnResume));
			}
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
			case 1: //LIST CONTACTS
			GlobalVars.selectTextView(list,true);
			GlobalVars.selectTextView(create,false);
			GlobalVars.selectTextView(goback,false);
			GlobalVars.talk(getResources().getString(R.string.layoutContactsList));
			break;

			case 2: //CREATE CONTACT
			GlobalVars.selectTextView(create, true);
			GlobalVars.selectTextView(list,false);
			GlobalVars.selectTextView(goback,false);
			GlobalVars.talk(getResources().getString(R.string.layoutContactsCreate));
			break;

			case 3: //GO BACK TO THE MAIN MENU
			GlobalVars.selectTextView(goback,true);
			GlobalVars.selectTextView(list,false);
			GlobalVars.selectTextView(create,false);
			GlobalVars.talk(getResources().getString(R.string.backToMainMenu));
			break;
			}
		}
		
	public void execute()
		{
		switch (GlobalVars.activityItemLocation)
			{
			case 1: //LIST CONTACTS
			GlobalVars.startActivity(ContactsList.class);
			break;

			case 2: //CREATE CONTACT
			GlobalVars.startActivity(ContactsCreate.class);
			break;

			case 3: //GO BACK TO THE MAIN MENU
			this.finish();
			break;
			}
		}
	}