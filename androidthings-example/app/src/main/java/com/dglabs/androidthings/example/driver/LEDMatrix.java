package com.dglabs.androidthings.example.driver;

import android.os.Process;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by dennis on 10.03.17.
 */

/**
 * 8*8 LED Matrix by cascading two 74HC595
 */
public class LEDMatrix implements Closeable {

    private static final String TAG = LEDMatrix.class.getSimpleName();

    public static final int data[][] = {
            { 0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00 }, //NULL
            { 0x00,0x00,0x3C,0x42,0x42,0x3C,0x00,0x00 }, //0
            { 0x00,0x00,0x00,0x44,0x7E,0x40,0x00,0x00 }, //1
            { 0x00,0x00,0x44,0x62,0x52,0x4C,0x00,0x00 }, //2
            { 0x00,0x00,0x78,0x14,0x12,0x14,0x78,0x00 }, //A
            { 0x00,0x00,0x60,0x90,0x90,0xFE,0x00,0x00 }, //d
            { 0x00,0x00,0x1C,0x2A,0x2A,0x2A,0x24,0x00 }, //e
            { 0x00,0x00,0x1C,0x2A,0x2A,0x2A,0x24,0x00 }, //e
            { 0x00,0x00,0x7E,0x12,0x12,0x0C,0x00,0x00 }, //p
            { 0x00,0x00,0x08,0x7E,0x88,0x40,0x00,0x00 }, //t
            { 0x3C,0x42,0x95,0xB1,0xB1,0x95,0x42,0x3C }, //:)
            { 0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00 } //NULL
    };

    private static final int tab [] = {0xfe,0xfd,0xfb,0xf7,0xef,0xdf,0xbf,0x7f};

    private Gpio RCLK, SRCLK, DI;

    private boolean stopping = false;
    private final int [] display_buffer = new int [tab.length];

    private Thread refreshThread = new Thread() {
        @Override
        public void run() {
            final int [] db = new int [tab.length];
            while (!stopping) {
                synchronized (display_buffer) {
                    for (int i = 0; i < display_buffer.length; i++) db[i] = display_buffer[i];
                }
                try {
                    write(db);
                }
                catch (IOException ex) { break; }
                /*if (!stopping)
                    try {
                        Thread.sleep(1);
                    }
                    catch (InterruptedException ex) {
                        break;
                    }*/
            }
        }
    };

    /**
     * Create and initialize LED matrix
     * @param RCLK_pin memory clock input(STCP)
     * @param SRCLK_pin shift register clock input(SHCP)
     * @param DI_pin serial data input
     * @throws IOException
     */
    public LEDMatrix(@NonNull String RCLK_pin, @NonNull String SRCLK_pin, @NonNull String DI_pin) throws IOException {
        PeripheralManagerService service = new PeripheralManagerService();
        RCLK = service.openGpio(RCLK_pin); RCLK.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        SRCLK = service.openGpio(SRCLK_pin); SRCLK.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        DI = service.openGpio(DI_pin); DI.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

        refreshThread.setPriority(Process.THREAD_PRIORITY_LESS_FAVORABLE);
        refreshThread.start();
    }

    @Override
    public void close() throws IOException {
        stopping = true;
        refreshThread.interrupt();
        if (RCLK != null && SRCLK != null && DI != null) {
            try {
                RCLK.close();
                SRCLK.close();
                DI.close();
            }
            catch (IOException ex) {
                Log.e(TAG, "Error closing GPIO", ex);
            }
        }
    }

    private void hc595WriteByte(int dat) throws IOException
    {
        for(int i=0; i < 8; i++) {
            DI.setValue(((dat << i) & 0x80) != 0);
            SRCLK.setValue(true);
            // delay(1);
            SRCLK.setValue(false);
        }
    }

    private void hc595SetAddress(int address) throws IOException {
        hc595WriteByte(address);
        RCLK.setValue(true);
        // delay(1);
        RCLK.setValue(false);
    }

    private static final int [] EMPTY = {0, 0, 0, 0, 0, 0, 0, 0};
    public void clear() throws IOException {
        write(EMPTY);
    }

    /**
     * Write a byte of data to the desired
     * @param data byte of data to display
     * @param row the row number 0-7
     * @throws IOException
     */
    private void write(int data, int row) throws IOException {
        if (row < 0 || row >= tab.length) throw new IllegalArgumentException("row should be in 0-" + (tab.length - 1));
        hc595WriteByte(data);
        hc595SetAddress(tab[row]);
    }

    /**
     * Write 8 bytes of image to the LED matrix
     * @param data bitmap array
     * @throws IOException
     */
    private void write(@NonNull int [] data) throws IOException {
        try {
            for (int i = 0; i < tab.length && i < data.length; i++) {
                write(data[i], i);
                Thread.sleep(2);
            }
        }
        catch (InterruptedException ex) {}
    }

    /**
     * Write 8 bytes of image to the LED matrix
     * @param data bitmap array
     */
    public void display(@NonNull int [] data) {
        synchronized (display_buffer) {
            for (int i = 0; i < display_buffer.length && i < data.length; i++) display_buffer[i] = data[i];
        }
    }
}
