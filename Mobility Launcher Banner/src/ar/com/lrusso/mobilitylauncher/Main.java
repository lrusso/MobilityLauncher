package ar.com.lrusso.mobilitylauncher;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;

public class Main extends Activity
	{
	@Override protected void onCreate(Bundle savedInstanceState)
		{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		
		Button buttonApp = (Button) findViewById(R.id.buttonApp);
		buttonApp.setOnClickListener(new View.OnClickListener()
			{
		    @Override
		    public void onClick(View view)
		    	{
		    	String url = getResources().getString(R.string.app_download_link);
		    	Intent i = new Intent(Intent.ACTION_VIEW);
		    	i.setData(Uri.parse(url));
		    	startActivity(i);
		    	}
			});
		
		Button buttonSource = (Button) findViewById(R.id.buttonSource);
		buttonSource.setOnClickListener(new View.OnClickListener()
			{
		    @Override
		    public void onClick(View view)
		    	{
		    	String url = getResources().getString(R.string.app_source_link);
		    	Intent i = new Intent(Intent.ACTION_VIEW);
		    	i.setData(Uri.parse(url));
		    	startActivity(i);
		    	}
			});
		}
	}