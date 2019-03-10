package ar.com.lrusso.mobilitylauncher.app;

import android.os.Bundle;
import android.widget.TextView;
import ar.com.lrusso.mobilitylauncher.app.R;
import android.view.*;
import java.util.*;
import android.app.Activity;

public class InputKeyboard extends Activity
	{
	private float x1,x2,y1;
	private float y2 = -1;
	public int location = -1;
	private int limit;
	private TextView message;
	private List<String> keyList;
	
	@Override protected void onCreate(Bundle savedInstanceState)
		{
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.inputkeyboard);
		GlobalVars.lastActivity = InputKeyboard.class;
		GlobalVars.lastActivityArduino = this;
		message = (TextView) findViewById(R.id.message);
		String[] original = null;
		if (GlobalVars.inputModeKeyboardOnlyNumbers==true)
			{
			original = getResources().getStringArray(R.array.keysOnlyNumbers);
			keyList = new ArrayList<String>(Arrays.asList(original));
			limit = keyList.size() -1;
			location = -1;
			}
			else
			{
			original = getResources().getStringArray(R.array.keysAll);
			keyList = new ArrayList<String>(Arrays.asList(original));
			limit = keyList.size() -1;
			location = limit;
			}

		//HIDES THE NAVIGATION BAR
		if (android.os.Build.VERSION.SDK_INT>11){try{GlobalVars.hideNavigationBar(this);}catch(Exception e){}}
		}
		
	@Override public void onResume()
		{
		super.onResume();
		try{GlobalVars.alarmVibrator.cancel();}catch(NullPointerException e){}catch(Exception e){}
		GlobalVars.lastActivity = InputKeyboard.class;
		GlobalVars.talk(getResources().getString(R.string.layoutInputKeysOnResume));

		//HIDES THE NAVIGATION BAR
		if (android.os.Build.VERSION.SDK_INT>11){try{GlobalVars.hideNavigationBar(this);}catch(Exception e){}}
		}
	
	@Override public String toString()
		{
		//int result = GlobalVars.detectArduinoKeyUp();
		if (GlobalVars.arduinoKeyPressed==GlobalVars.ARDUINO_UP)
			{
			if (location-1<0)
				{
				location=limit;
				}
				else
				{
				location=location-1;
				}
			changeCharacter();
			}
		else if (GlobalVars.arduinoKeyPressed==GlobalVars.ARDUINO_DOWN)
			{
			if (location+1>limit)
				{
				location=0;
				}
				else
				{
				location=location+1;
				}
			changeCharacter();
			}
		else if (GlobalVars.arduinoKeyPressed==GlobalVars.ARDUINO_RIGHT)
			{
			nextCharacter();
			}
		else if (GlobalVars.arduinoKeyPressed==GlobalVars.ARDUINO_LEFT)
			{
			deleteLastCharacter();
			}
		return null;
		}


	public boolean onKeyDown(int keyCode, KeyEvent event)
		{ 
		if (keyCode == KeyEvent.KEYCODE_BACK)
			{
			return true;
			}
			else
			{
			return super.onKeyDown(keyCode, event);
			}
		}
		
	public void changeCharacter()
		{
		String value = message.getText().toString();
		if (value.endsWith(getResources().getString(R.string.layoutInputKeysKeyEnter)))
			{
			message.setText(value.substring(0,value.length()-getResources().getString(R.string.layoutInputKeysKeyEnter).length()) + keyList.get(location));
			}
		else if (value.endsWith(getResources().getString(R.string.layoutInputKeysKeyRead)))
			{
			message.setText(value.substring(0,value.length()-getResources().getString(R.string.layoutInputKeysKeyRead).length()) + keyList.get(location));
			}
		else
			{
			message.setText(value.substring(0,value.length()-1) + keyList.get(location));
			}
		if (keyList.get(location).contains(getResources().getString(R.string.layoutInputKeysKeyEnter)))
			{
			GlobalVars.talk(getResources().getString(R.string.layoutInputKeysKeyEnter2));
			}
		else if (keyList.get(location).contains(getResources().getString(R.string.layoutInputKeysKeyRead)))
			{
			if (GlobalVars.inputModeKeyboardOnlyNumbers==true)
				{
				GlobalVars.talk(getResources().getString(R.string.layoutInputKeysKeyReadNumber));
				}
				else
				{
				GlobalVars.talk(getResources().getString(R.string.layoutInputKeysKeyReadText));
				}
			}
		else if (keyList.get(location).contains("_"))
			{
			GlobalVars.talk(getResources().getString(R.string.layoutInputKeysKeySpace));
			}
		else
			{
			GlobalVars.talk(keyList.get(location));	
			}
		}
		
	public void deleteLastCharacter()
		{
		removeSpecialKeyText();
		String value = message.getText().toString();
		if (value.endsWith("_"))
			{
			value = value.substring(0,value.length()-1);
			}
		if (value.length()==0)
			{
			GlobalVars.talk(getResources().getString(R.string.layoutInputKeysEmptyField));
			}
			else
			{
			value = value.substring(0,value.length()-1);
			location=limit;
			if (value.length()==0)
				{
				GlobalVars.talk(getResources().getString(R.string.layoutInputKeysEmptyField));
				}
				else
				{
				GlobalVars.talk(value);
				}
			}
		message.setText(value + "_");
		}

	public void nextCharacter()
		{
		String value = message.getText().toString();
		if (value.endsWith("_"))
			{
			if (GlobalVars.inputModeKeyboardOnlyNumbers==false)
				{
				if (value.substring(0,value.length()-1).length()>0)
					{
					GlobalVars.talk(value.substring(0,value.length()-1));
					}
				message.setText(value.substring(0,value.length()-1) + " _");
				}
				else
				{
				message.setText(value.substring(0,value.length()-1) + "_");
				}
			
			}
		else if (value.endsWith(getResources().getString(R.string.layoutInputKeysKeyEnter)))
			{
			executeEnterKey(value);
			this.finish();
			}
		else if (value.endsWith(getResources().getString(R.string.layoutInputKeysKeyRead)))
			{
			if (value.length()==getResources().getString(R.string.layoutInputKeysKeyRead).length())
				{
				GlobalVars.talk(getResources().getString(R.string.layoutInputKeysNothingToRead));
				}
				else
				{
				if (GlobalVars.inputModeKeyboardOnlyNumbers==true)
					{
					GlobalVars.talk(GlobalVars.divideNumbersWithBlanks(value.substring(0,value.length()-getResources().getString(R.string.layoutInputKeysKeyRead).length())));
					}
					else
					{
					GlobalVars.talk(value.substring(0,value.length()-getResources().getString(R.string.layoutInputKeysKeyRead).length()));
					}
				}
			}
		else
			{
			GlobalVars.talk(value.substring(value.length()-1,value.length()) + getResources().getString(R.string.layoutInputKeysEntered));
			message.setText(value + "_");
			if (GlobalVars.inputModeKeyboardOnlyNumbers==true)
				{
				location=-1;
				}
				else
				{
				location=limit;
				}
			}
		}
		
	public void removeSpecialKeyText()
		{
		String value = message.getText().toString();
		if (value.endsWith("_"))
			{
			message.setText(value.substring(0,value.length()-1));
			}
		else if (value.endsWith(getResources().getString(R.string.layoutInputKeysKeyRead)))
			{
			message.setText(value.substring(0,value.length()-getResources().getString(R.string.layoutInputKeysKeyRead).length()));
			}
		else if (value.endsWith(getResources().getString(R.string.layoutInputKeysKeyEnter)))
			{
			message.setText(value.substring(0,value.length()-getResources().getString(R.string.layoutInputKeysKeyEnter).length()));
			}
		}
		
	public void executeEnterKey(String value)
		{
		try
			{
			if (value.substring(0,value.length()-getResources().getString(R.string.layoutInputKeysKeyEnter).length()).length()>0)
				{
				GlobalVars.inputModeResult = value.substring(0,value.length()-getResources().getString(R.string.layoutInputKeysKeyEnter).length());
				}
				else
				{
				GlobalVars.inputModeResult = null;
				}
			GlobalVars.inputModeKeyboardOnlyNumbers = false;
			}
			catch(StringIndexOutOfBoundsException e)
			{
			GlobalVars.inputModeKeyboardOnlyNumbers = false;
			}
			catch(Exception e)
			{
			GlobalVars.inputModeKeyboardOnlyNumbers = false;
			}	
		}
	}