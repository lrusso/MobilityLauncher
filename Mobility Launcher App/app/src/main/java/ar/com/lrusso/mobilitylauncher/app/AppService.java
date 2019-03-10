package ar.com.lrusso.mobilitylauncher.app;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class AppService extends Service
	{
    public static final String ACTION_USB_NOT_SUPPORTED = "ar.com.lrusso.andruino.USB_NOT_SUPPORTED";
    public static final String ACTION_NO_USB = "ar.com.lrusso.andruino.NO_USB";
    public static final String ACTION_USB_PERMISSION_GRANTED = "ar.com.lrusso.andruino.USB_PERMISSION_GRANTED";
    public static final String ACTION_USB_PERMISSION_NOT_GRANTED = "ar.com.lrusso.andruino.USB_PERMISSION_NOT_GRANTED";
    public static final String ACTION_USB_DISCONNECTED = "ar.com.lrusso.andruino.USB_DISCONNECTED";
    public static final String ACTION_SERIAL_CONFIG_CHANGED = "ar.com.lrusso.andruino.SERIAL_CONFIG_CHANGED";

    private static final String ACTION_USB_READY = "ar.com.lrusso.andruino.USB_READY";
    private static final String ACTION_CDC_DRIVER_NOT_WORKING = "ar.com.lrusso.andruino.ACTION_CDC_DRIVER_NOT_WORKING";
    private static final String ACTION_USB_DEVICE_NOT_WORKING = "ar.com.lrusso.andruino.ACTION_USB_DEVICE_NOT_WORKING";
    private static final String ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    private static final String ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
    private static final String ACTION_USB_PERMISSION = "ar.com.lrusso.andruino.USB_PERMISSION";

    public static final int MESSAGE_FROM_SERIAL_PORT = 0;
    public static final int CTS_CHANGE = 1;
    public static final int DSR_CHANGE = 2;

    public static int BAUD_RATE = 9600;

    static boolean SERVICE_CONNECTED = false;

    private final IBinder binder = new UsbBinder();

    private Context context;
    private Handler mHandler;

    private UsbManager usbManager;
    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serialPort;
    private boolean serialPortConnected;

    @Override public void onCreate()
    	{
        this.context = this;
        serialPortConnected = false;
        AppService.SERVICE_CONNECTED = true;
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(ACTION_USB_DETACHED);
        filter.addAction(ACTION_USB_ATTACHED);
        filter.addAction(ACTION_SERIAL_CONFIG_CHANGED);
        registerReceiver(usbReceiver, filter);

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        findSerialPortDevice();
    	}

    @Override public IBinder onBind(Intent intent)
    	{
        return binder;
    	}

    @Override public int onStartCommand(Intent intent, int flags, int startId)
    	{
        return AppService.START_NOT_STICKY;
    	}

    @Override public void onDestroy()
    	{
        super.onDestroy();
        AppService.SERVICE_CONNECTED = false;
    	}

    public void write(byte[] data)
    	{
        if (serialPort != null)
        	{
            serialPort.write(data);
        	}
    	}

    public void setHandler(Handler mHandler)
    	{
        this.mHandler = mHandler;
    	}
    
    private void findSerialPortDevice()
    	{
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();

        if (usbDevices.isEmpty())
        	{
            Intent intent = new Intent(ACTION_NO_USB);
            sendBroadcast(intent);
            return;
        	}

        boolean keep = true;

        for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet())
        	{
            device = entry.getValue();
            int deviceVID = device.getVendorId();
            int devicePID = device.getProductId();


            if (deviceVID != 0x1d6b &&(devicePID != 0x0001 && devicePID != 0x0002 && devicePID != 0x0003))
            	{
                PendingIntent mPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                usbManager.requestPermission(device, mPendingIntent);
                keep = false;
            	}
            	else
            	{
                connection = null;
                device = null;
            	}

            if (!keep)
            	{
                break;
            	}
        	}

        if (!keep)
        	{
            Intent intent = new Intent(ACTION_NO_USB);
            sendBroadcast(intent);
        	}
    	}
    
    public class UsbBinder extends Binder
    	{
        public AppService getService()
        	{
            return AppService.this;
        	}
    	}

    private class ConnectionThread extends Thread
    	{
        @Override public void run()
        	{
            serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
            if (serialPort == null)
            	{
                Intent intent = new Intent(ACTION_USB_NOT_SUPPORTED);
                context.sendBroadcast(intent);
                return;
            	}

            if (serialPort.open())
            	{
                serialPortConnected = true;

                getBaudRateConfig();
                
                serialPort.setBaudRate(BAUD_RATE);
                serialPort.setDataBits(8);
                serialPort.setStopBits(1);
                serialPort.setParity(0);
                serialPort.setFlowControl(0);

                serialPort.read(mCallback);
                serialPort.getCTS(ctsCallback);
                serialPort.getDSR(dsrCallback);

                Intent intent = new Intent(ACTION_USB_READY);
                context.sendBroadcast(intent);
            	}
            	else
            	{
                if (serialPort instanceof UsbSerialCDCSerialDevice)
                	{
                    Intent intent = new Intent(ACTION_CDC_DRIVER_NOT_WORKING);
                    context.sendBroadcast(intent);
                	}
                	else
                	{
                    Intent intent = new Intent(ACTION_USB_DEVICE_NOT_WORKING);
                    context.sendBroadcast(intent);
                	}
            	}
        	}
    	}
    
    private final UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback()
		{
    	@Override public void onReceivedData(byte[] arg)
			{
    		try
        		{
    			String data = new String(arg, "UTF-8");
    			if (mHandler != null)
            		{
    				mHandler.obtainMessage(MESSAGE_FROM_SERIAL_PORT, data).sendToTarget();
            		}
        		}
        		catch (Exception e)
        		{
        		}
			}
		};

	private final UsbSerialInterface.UsbCTSCallback ctsCallback = new UsbSerialInterface.UsbCTSCallback()
		{
		@Override public void onCTSChanged(boolean state)
    		{
			if (mHandler != null)
        		{
				mHandler.obtainMessage(CTS_CHANGE).sendToTarget();
        		}
    		}
		};

	private final UsbSerialInterface.UsbDSRCallback dsrCallback = new UsbSerialInterface.UsbDSRCallback()
		{
		@Override public void onDSRChanged(boolean state)
    		{
			if (mHandler != null)
        		{
				mHandler.obtainMessage(DSR_CHANGE).sendToTarget();
        		}
    		}
		};
	
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver()
    	{
    	@Override public void onReceive(Context context, Intent intent)
    		{
    		switch (intent.getAction())
        		{
        		case ACTION_USB_PERMISSION:
        		boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
        		if (granted)
        			{
        			Intent in = new Intent(ACTION_USB_PERMISSION_GRANTED);
        			context.sendBroadcast(in);
        			connection = usbManager.openDevice(device);
        			new ConnectionThread().start();
        			}
            		else
            		{
            		Intent in = new Intent(ACTION_USB_PERMISSION_NOT_GRANTED);
            		context.sendBroadcast(in);
            		}
        		break;
            
        		case ACTION_USB_ATTACHED:
        		if (!serialPortConnected)
            		{
        			findSerialPortDevice();
            		}
        		break;
            
        		case ACTION_USB_DETACHED:
        		Intent in = new Intent(ACTION_USB_DISCONNECTED);
        		context.sendBroadcast(in);
        		if (serialPortConnected)
            		{
        			serialPort.close();
            		}
        		serialPortConnected = false;
        		break;
            
        		case ACTION_SERIAL_CONFIG_CHANGED:
        		if (serialPortConnected)
            		{
        			serialPort.close();
        			connection = usbManager.openDevice(device);
        			new ConnectionThread().start();
            		}
        		break;
            
        		default:
        		break;
        		}
    		}
    	};
    	
	public void getBaudRateConfig()
    	{
        String value = readFile("baudrate.cfg");
        if (value=="_")
        	{
        	BAUD_RATE = 9600;
        	}
        if (value.contains("9600"))
        	{
        	BAUD_RATE = 9600;
        	}
        	else
        	{
        	if (value.contains("57600"))
        		{
            	BAUD_RATE = 57600;
        		}
        		else
        		{
       	   		if (value.contains("115200"))
            		{
                	BAUD_RATE = 115200;
            		}
       	   			else
       	   			{
                    BAUD_RATE = 9600;
       	    		}
        		}
        	}
    	}
        
	public String readFile(String archivo)
    	{
        String value;
        value = "_";
        try
        	{
        	FileInputStream fis = openFileInput(archivo);
        	InputStreamReader in = new InputStreamReader(fis);
        	BufferedReader br = new BufferedReader(in);
        	value=br.readLine();
        	br.close();
       		}
    		catch (IOException e)
    		{
    		}
        return value;
       	}
	}