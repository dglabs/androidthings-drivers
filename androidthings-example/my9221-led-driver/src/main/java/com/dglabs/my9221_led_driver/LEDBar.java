package com.dglabs.my9221_led_driver;

import android.support.annotation.NonNull;

import java.io.IOException;

/**
 * Created by dennis on 10.03.17.
 */

/**
 *
 */
public class LEDBar extends MY9221LEDDriver {

    private int ledCount = 10;

    /**
     * Create and initialize LED Bar object.
     * @param DI_pin_name DI inpit pin
     * @param DCLK_pin_name DCLK input pin
     * @param ledCount Number of LEDs on this bar (1-12). If 0, then 10 LEDs assumed.
     * @throws IOException
     */
    public LEDBar(@NonNull String DI_pin_name, @NonNull String DCLK_pin_name, int ledCount) throws IOException {
        super(DI_pin_name, DCLK_pin_name);
        if (ledCount > 12) throw new IllegalArgumentException("ledCount should be 1-12");
        if (ledCount > 0) this.ledCount = ledCount;
    }

    public int getLedCount() { return ledCount; }

    /**
     * Highlight value LEDs on the led bar
     * @param value number of LEDs to highlight 0 - ledCount
     * @param brightness bar brightness value (1-255)
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public void write(int value, int brightness) throws IOException, IllegalArgumentException {
        if (value > getLedCount()) throw new IllegalArgumentException("value should be in range 1-" + getLedCount());
        int mask = 0;
        for (int i = 0; i < value; i++) {
            mask <<= 1; mask |= 1;
        }
        lowWrite(mask, brightness);
    }
}
