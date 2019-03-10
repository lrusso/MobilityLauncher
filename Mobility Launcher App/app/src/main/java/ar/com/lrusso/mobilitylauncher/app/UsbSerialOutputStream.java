package ar.com.lrusso.mobilitylauncher.app;

import java.io.OutputStream;

public class UsbSerialOutputStream extends OutputStream
{
    private int timeout = 0;

    protected final UsbSerialInterface device;

    public UsbSerialOutputStream(UsbSerialInterface device)
    {
        this.device = device;
    }

    @Override
    public void write(int b)
    {
        device.syncWrite(new byte[] { (byte)b }, timeout);
    }

    @Override
    public void write(byte[] b)
    {
        device.syncWrite(b, timeout);
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
