package com.dglabs.androidthings.example;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.dglabs.my9221_led_driver.LEDBar;
import com.dglabs.androidthings.example.driver.LEDMatrix;
import com.dglabs.androidthings.example.sound.MusicNotes;
import com.google.android.things.contrib.driver.pwmspeaker.Speaker;
import com.google.android.things.contrib.driver.tm1637.NumericDisplay;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int INTERVAL_BETWEEN_BLINKS_MS = 1000;
    private static final long PLAYBACK_NOTE_DELAY = 400L;

    private static final String GPIO_LED_PIN_NAME = "BCM17";
    private static final String GPIO__BUTTON_PIN_NAME = "BCM23";
    private static final String BUZZER_PIN_NAME = "PWM1";

    private static final String LED_BAR_DI_PIN_NAME = "BCM22";
    private static final String LED_BAR_DCLK_PIN_NAME = "BCM27";

    private static final String DISPLAY7_SELECT_PIN_NAME = "BCM27";
    private static final String DISPLAY7_CLK_PIN_NAME = "BCM22";
    private static final String DISPLAY7_DATA_PIN_NAME = "BCM5";

    private static final String LED_MATRIX_DI_PIN_NAME = "BCM27";
    private static final String LED_MATRIX_RCLK_PIN_NAME = "BCM5";
    private static final String LED_MATRIX_SRCLK_PIN_NAME = "BCM22";

    private Handler mHandler = new Handler();

    private Gpio mLedGpio;
    private Gpio mButtonGpio;
    private Gpio mDisplaySelect;

    private Speaker mSpeaker;
    private LEDBar mLedBar;
    private LEDMatrix mLedMatrix;

    NumericDisplay mDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PeripheralManagerService service = new PeripheralManagerService();
        Log.d(TAG, "Available GPIO: " + service.getGpioList());
        Log.d(TAG, "Available I2C: " + service.getI2cBusList());
        Log.d(TAG, "Available SPI: " + service.getSpiBusList());
        Log.d(TAG, "Available PWM: " + service.getPwmList());
        Log.d(TAG, "Available UART: " + service.getUartDeviceList());

        try {
            mLedGpio = service.openGpio(GPIO_LED_PIN_NAME);
            // Step 2. Configure as an output.
            mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

            // Step 1. Create GPIO connection.
            mButtonGpio = service.openGpio(GPIO__BUTTON_PIN_NAME);
            // Step 2. Configure as an input.
            mButtonGpio.setDirection(Gpio.DIRECTION_IN);
            // Step 3. Enable edge trigger events.
            mButtonGpio.setEdgeTriggerType(Gpio.EDGE_FALLING);
            // Step 4. Register an event callback.
            mButtonGpio.registerGpioCallback(mCallback);

            mSpeaker = new Speaker(BUZZER_PIN_NAME);
            mSpeaker.stop(); // in case the PWM pin was enabled already

            mLedMatrix = new LEDMatrix(LED_MATRIX_RCLK_PIN_NAME, LED_MATRIX_SRCLK_PIN_NAME, LED_MATRIX_DI_PIN_NAME);

            /*mLedBar = new LEDBar(LED_BAR_DI_PIN_NAME, LED_BAR_DCLK_PIN_NAME, 10);
            mLedBar.clear();*/

            /*mDisplay = new NumericDisplay(DISPLAY7_DATA_PIN_NAME, DISPLAY7_CLK_PIN_NAME);
            mDisplaySelect = service.openGpio(DISPLAY7_SELECT_PIN_NAME);
            mDisplaySelect .setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);*/
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        }

        THEME = MusicNotes.DRAMATIC_THEME;
    }

    int bitCount = 0;

    // Step 4. Register an event callback.
    private GpioCallback mCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            Log.i(TAG, "GPIO changed, button pressed");

            mHandler.removeCallbacks(mBlinkRunnable);
            mHandler.removeCallbacks(mPlaybackRunnable);

            //mHandler.post(mPlaybackRunnable);
            // Step 4. Repeat using a handler.
            mHandler.post(mBlinkRunnable);

            if (mLedBar != null) {
                try {
                    mLedBar.write(bitCount++, LEDBar.BRIGHTNESS_MED);
                    bitCount %= mLedBar.getLedCount();
                } catch (IOException ex) {
                    Log.e(TAG, "Error write to LED", ex);
                }
            }

            if (mLedMatrix != null) {
                mLedMatrix.display(LEDMatrix.data[bitCount++]);
                bitCount %= LEDMatrix.data.length;
            }

            /*try {
                // displays "42"
                mDisplay.display(42);

                // displays "12:00"
                mDisplay.setColonEnabled(true);
                mDisplay.display("1200");
            } catch (IOException e) {
                // error setting display
            }*/

            return true;
        }
    };
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Step 4. Remove handler events on close.
        mHandler.removeCallbacks(mBlinkRunnable);
        mHandler.removeCallbacks(mPlaybackRunnable);

        // Step 5. Close the resource.
        if (mLedGpio != null) {
            try {
                mLedGpio.setValue(false);
                mLedGpio.close();
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
        }

        if (mButtonGpio != null) {
            mButtonGpio.unregisterGpioCallback(mCallback);
            try {
                mButtonGpio.close();
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
        }

        if (mSpeaker != null) {
            try {
                mSpeaker.stop();
                mSpeaker.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing speaker", e);
            } finally {
                mSpeaker = null;
            }
        }

        if (mLedBar != null) {
            try {
                mLedBar.clear();
                mLedBar.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing LEDBar", e);
            } finally {
                mLedBar = null;
            }
        }

        if (mDisplay != null) {
            try {
                mDisplaySelect.setValue(false);
                mDisplaySelect.close();
                mDisplay.close();
            } catch (IOException e) {
                mDisplay = null;
                // error closing display
            }
        }

        if (mLedMatrix != null) {
            try {
                mLedMatrix.clear();
                mLedMatrix.close();
            } catch (IOException e) {
                mLedMatrix = null;
            }

        }
    }

    private Runnable mBlinkRunnable = new Runnable() {
        @Override
        public void run() {
            // Exit if the GPIO is already closed
            if (mLedGpio == null) {
                return;
            }

            try {
                // Step 3. Toggle the LED state
                mLedGpio.setValue(!mLedGpio.getValue());

                // Step 4. Schedule another event after delay.
                mHandler.postDelayed(mBlinkRunnable, INTERVAL_BETWEEN_BLINKS_MS);
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
        }
    };

    private double[] THEME;

    private Runnable mPlaybackRunnable = new Runnable() {

        private int index = 0;

        @Override
        public void run() {
            if (mSpeaker == null || THEME == null) {
                return;
            }

            try {
                if (index == THEME.length) {
                    // reached the end
                    index = 0;
                    mSpeaker.stop();
                } else {
                    double note = THEME[index++];
                    if (note > 0) {
                        mSpeaker.play(note);
                    } else {
                        mSpeaker.stop();
                    }
                    mHandler.postDelayed(this, note > 0 ? PLAYBACK_NOTE_DELAY : PLAYBACK_NOTE_DELAY / 2);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error playing speaker", e);
            }
        }
    };

}
