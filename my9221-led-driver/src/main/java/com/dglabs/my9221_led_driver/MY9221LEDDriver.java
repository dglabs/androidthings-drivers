package com.dglabs.my9221_led_driver;

import android.support.annotation.NonNull;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by dennis on 10.03.17.
 */

/**
 * MY9221 LED driver
 */
public class MY9221LEDDriver implements Closeable {
    private Gpio mDI;
    private Gpio mDCLK;
    private boolean clk_flag = false;

    private static final int CMD_MODE = 0x0000; // Work on 8-bit mode
    private static final int ON = 0x000f;       // 8-bit 1 data
    private static final int SHUT = 0x0000;     // 8-bit 0 data

    /**
     * max brightness
     */
    public static final int BRIGHTNESS_MAX = 0x00ff;
    /**
     * medium brightness
     */
    public static final int BRIGHTNESS_MED = 0x008f;
    /**
     * minimum brightness
     */
    public static final int BRIGHTNESS_MIN = 0x000f;

    /**
     * Create and initialize driver
     * @param DI_pin_name DI data input
     * @param DCLK_pin_name DCLK clock input
     * @throws IOException
     */
    public MY9221LEDDriver(@NonNull String DI_pin_name, @NonNull String DCLK_pin_name) throws IOException {
        PeripheralManagerService service = new PeripheralManagerService();
        mDI = service.openGpio(DI_pin_name);
        mDI.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        mDCLK = service.openGpio(DCLK_pin_name);
        mDCLK.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        clk_flag = false;
    }

    @Override
    public void close() throws IOException {
        if (mDI != null && mDCLK != null) {
            try {
                clear();
                mDI.setValue(false);
                mDCLK.setValue(false);
                mDI.close();
                mDCLK.close();
                clk_flag = false;
            } finally {
                mDI = mDCLK = null;
            }
        }
    }

    public void clear() throws IOException { lowWrite(0, 0); }

    private void low_write16(long mask) throws IOException {
        for (int i = 0; i < 16; i++) {
            mDI.setValue((mask & 1l) != 0);
            mDCLK.setValue(clk_flag = !clk_flag);
        }
        mDI.setValue(false);
    }

    private void sendData(int mask, int greyscale) throws IOException
    {
        if (greyscale == 0) greyscale = ON;
        for(int i=0; i<12; i++)
        {
            if((mask & (1 << i)) != 0)
                low_write16(greyscale);
            else
                low_write16(SHUT);
        }
    }

    private void latchData() throws IOException {
        boolean latchFlag = false;
        try {
            mDI.setValue(latchFlag);
            Thread.sleep(1);
            for (int i = 0; i < 8; i++) {
                mDI.setValue(latchFlag = !latchFlag);
            }
            Thread.sleep(1);
        }
        catch (InterruptedException ex) {}
    }

    /**
     * Write up to 12 bits to driver with given grey scale brightness value
     * @param value 12 bits to highlight (1) or dark (0)
     * @param brightness variable brightness 1-255
     * @throws IOException
     */
    public void lowWrite(int value, int brightness) throws IOException {
        low_write16(CMD_MODE);
        sendData(value, brightness & 0xFF);
        latchData();
    }
}
