package ar.com.lrusso.mobilitylauncher.app;

import android.app.*;
import android.os.*;
import android.widget.*;
import ar.com.lrusso.mobilitylauncher.app.R;

public class Browser extends Activity
	{
	private TextView browsergoogle;
	private TextView bookmarks;
	private TextView goback;
	
    @Override protected void onCreate(Bundle savedInstanceState)
    	{
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.browser);
		GlobalVars.lastActivity = Browser.class;
		GlobalVars.lastActivityArduino = this;
		browsergoogle = (TextView) findViewById(R.id.browsergoogle);
		bookmarks = (TextView) findViewById(R.id.bookmarks);
		goback = (TextView) findViewById(R.id.goback);
		GlobalVars.activityItemLocation=0;
		GlobalVars.activityItemLimit=3;

		//HIDES THE NAVIGATION BAR
		if (android.os.Build.VERSION.SDK_INT>11){try{GlobalVars.hideNavigationBar(this);}catch(Exception e){}}
    	}
    
	@Override public void onResume()
		{
		super.onResume();
		try{GlobalVars.alarmVibrator.cancel();}catch(NullPointerException e){}catch(Exception e){}
		GlobalVars.lastActivity = Browser.class;
		GlobalVars.lastActivityArduino = this;
		GlobalVars.activityItemLocation=0;
		GlobalVars.activityItemLimit=3;
		GlobalVars.selectTextView(browsergoogle,false);
		GlobalVars.selectTextView(bookmarks,false);
		GlobalVars.selectTextView(goback,false);
		if (GlobalVars.inputModeResult!=null)
			{
			if (GlobalVars.browserRequestInProgress==false)
				{
				GlobalVars.browserRequestInProgress=true;
				new BrowserThreadGoTo().execute("http://www.google.com/custom?q=" + GlobalVars.inputModeResult);
				}
				else
				{
				GlobalVars.talk(getResources().getString(R.string.layoutBrowserErrorPendingRequest));
				}
			GlobalVars.inputModeResult = null;
			}
			else
			{
			GlobalVars.talk(getResources().getString(R.string.layoutBrowserOnResume));
			}

		//HIDES THE NAVIGATION BAR
		if (android.os.Build.VERSION.SDK_INT>11){try{GlobalVars.hideNavigationBar(this);}catch(Exception e){}}
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
			case 1: //SEARCH IN GOOGLE
			GlobalVars.selectTextView(browsergoogle,true);
			GlobalVars.selectTextView(bookmarks,false);
			GlobalVars.selectTextView(goback,false);
			if (GlobalVars.browserRequestInProgress==true)
				{
				GlobalVars.talk(getResources().getString(R.string.layoutBrowserSearchInGoogle) + 
								getResources().getString(R.string.layoutBrowserAWebPageItsBeenLoading));
				}
				else
				{
				GlobalVars.talk(getResources().getString(R.string.layoutBrowserSearchInGoogle));
				}
			break;

			case 2: //LIST BOOKMARKS
			GlobalVars.selectTextView(bookmarks, true);
			GlobalVars.selectTextView(browsergoogle,false);
			GlobalVars.selectTextView(goback,false);
			GlobalVars.talk(getResources().getString(R.string.layoutBrowserListBookmarks));
			break;

			case 3: //GO BACK TO THE MAIN MENU
			GlobalVars.selectTextView(goback,true);
			GlobalVars.selectTextView(bookmarks,false);
			GlobalVars.selectTextView(browsergoogle,false);
			GlobalVars.talk(getResources().getString(R.string.backToMainMenu));
			break;
			}
		}

	public void execute()
		{
		switch (GlobalVars.activityItemLocation)
			{
			case 1: //SEARCH IN GOOGLE
			if (GlobalVars.browserRequestInProgress==false)
				{
				GlobalVars.startInputActivity();
				}
				else
				{
				GlobalVars.talk(getResources().getString(R.string.layoutBrowserErrorPendingRequest));
				}
			break;

			case 2: //LIST BOOKMARKS
			GlobalVars.startActivity(BookmarksList.class);
			break;

			case 3: //GO BACK TO THE MAIN MENU
			this.finish();
			break;
			}
		}
	}