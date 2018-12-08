package ar.com.lrusso.mobilitylauncher.app;

import android.app.*;
import android.os.*;
import android.widget.*;
import ar.com.lrusso.mobilitylauncher.app.R;

public class Messages extends Activity
	{
	public static TextView inbox;
	private TextView sent;
	private TextView compose;
	private TextView goback;
	
    @Override protected void onCreate(Bundle savedInstanceState)
    	{
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.messages);
		GlobalVars.lastActivity = Messages.class;
		GlobalVars.lastActivityArduino = this;
		inbox = (TextView) findViewById(R.id.messagesinbox);
		sent = (TextView) findViewById(R.id.messagessent);
		compose = (TextView) findViewById(R.id.messagesnew);
		goback = (TextView) findViewById(R.id.goback);
		GlobalVars.activityItemLocation=0;
		GlobalVars.activityItemLimit=4;
		GlobalVars.setText(inbox, false, GlobalVars.context.getResources().getString(R.string.layoutMessagesInbox) + " (" + String.valueOf(GlobalVars.getMessagesUnreadCount()) + ")");
    	}
		
	@Override public void onResume()
		{
		super.onResume();
		try{GlobalVars.alarmVibrator.cancel();}catch(NullPointerException e){}catch(Exception e){}
		GlobalVars.lastActivity = Messages.class;
		GlobalVars.lastActivityArduino = this;
		GlobalVars.activityItemLocation=0;
		GlobalVars.activityItemLimit=4;
		GlobalVars.selectTextView(inbox,false);
		GlobalVars.selectTextView(sent,false);
		GlobalVars.selectTextView(compose,false);
		GlobalVars.selectTextView(goback,false);
		inbox.setText(GlobalVars.context.getResources().getString(R.string.layoutMessagesInbox) + " (" + String.valueOf(GlobalVars.getMessagesUnreadCount()) + ")");
		if (GlobalVars.messagesWasSent == true)
			{
			GlobalVars.talk(getResources().getString(R.string.layoutMessagesOnResume2));
			GlobalVars.messagesWasSent = false;
			}
			else
			{
			GlobalVars.talk(getResources().getString(R.string.layoutMessagesOnResume));
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
			case 1: //INBOX
			GlobalVars.selectTextView(inbox,true);
			GlobalVars.selectTextView(sent,false);
			GlobalVars.selectTextView(goback,false);
			int smsUnread = GlobalVars.getMessagesUnreadCount();
			if (smsUnread==0)
				{
				GlobalVars.talk(getResources().getString(R.string.layoutMessagesInboxNoNew));
				}
			else if (smsUnread==1)
				{
				GlobalVars.talk(getResources().getString(R.string.layoutMessagesInboxOneNew));
				}
			else
				{
				GlobalVars.talk(getResources().getString(R.string.layoutMessagesInbox) + ". " + smsUnread + " " + getResources().getString(R.string.layoutMessagesInboxNew));
				}
			break;

			case 2: //SENT
			GlobalVars.selectTextView(sent, true);
			GlobalVars.selectTextView(inbox,false);
			GlobalVars.selectTextView(compose,false);
			GlobalVars.talk(getResources().getString(R.string.layoutMessagesSent));
			break;
			
			case 3: //COMPOSE MESSAGE
			GlobalVars.selectTextView(compose, true);
			GlobalVars.selectTextView(sent,false);
			GlobalVars.selectTextView(goback,false);
			GlobalVars.talk(getResources().getString(R.string.layoutMessagesNew2));
			break;

			case 4: //GO BACK TO THE MAIN MENU
			GlobalVars.selectTextView(goback,true);
			GlobalVars.selectTextView(compose,false);
			GlobalVars.selectTextView(inbox,false);
			GlobalVars.talk(getResources().getString(R.string.backToMainMenu));
			break;
			}
		}
	
	public void execute()
		{
		switch (GlobalVars.activityItemLocation)
			{
			case 1: //INBOX
			GlobalVars.startActivity(MessagesInbox.class);
			break;

			case 2: //SENT
			GlobalVars.startActivity(MessagesSent.class);
			break;

			case 3: //COMPOSE MESSAGE
			GlobalVars.startActivity(MessagesCompose.class);
			break;
			
			case 4: //GO BACK TO THE MAIN MENU
			this.finish();
			break;
			}
		}
	}