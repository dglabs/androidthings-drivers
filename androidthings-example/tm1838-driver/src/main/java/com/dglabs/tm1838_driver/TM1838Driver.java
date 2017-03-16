package com.dglabs.tm1838_driver;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.SpiDevice;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by dennis on 11.03.17.
 */

/*
    TM1638 (https://retrocip.cz/files/tm1638.pdf) driver for IC which drives up to 8 7-segment
    indicators with autorefresh and scans attached keyboard.
    Driver may work through GPIO pins (3 pins) or by SPI device
 */
public class TM1838Driver implements Closeable {
    public static final String TAG = TM1838Driver.class.getSimpleName();

    public static final boolean HIGH = true;
    public static final boolean LOW = false;

    // definition for the displayable ASCII chars
    public static final int FONT_DEFAULT[] = {
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // (0) - (31)
        0b00000000, // (32)  <space>
        0b10000110, // (33)	!
        0b00100010, // (34)	"
        0b01111110, // (35)	#
        0b01101101, // (36)	$
        0b00000000, // (37)	%
        0b00000000, // (38)	&
        0b00000010, // (39)	'
        0b00110000, // (40)	(
        0b00000110, // (41)	)
        0b01100011, // (42)	*
        0b00000000, // (43)	+
        0b00000100, // (44)	,
        0b01000000, // (45)	-
        0b10000000, // (46)	.
        0b01010010, // (47)	/
        0b00111111, // (48)	0
        0b00000110, // (49)	1
        0b01011011, // (50)	2
        0b01001111, // (51)	3
        0b01100110, // (52)	4
        0b01101101, // (53)	5
        0b01111101, // (54)	6
        0b00100111, // (55)	7
        0b01111111, // (56)	8
        0b01101111, // (57)	9
        0b00000000, // (58)	:
        0b00000000, // (59)	;
        0b00000000, // (60)	<
        0b01001000, // (61)	=
        0b00000000, // (62)	>
        0b01010011, // (63)	?
        0b01011111, // (64)	@
        0b01110111, // (65)	A
        0b01111111, // (66)	B
        0b00111001, // (67)	C
        0b00111111, // (68)	D
        0b01111001, // (69)	E
        0b01110001, // (70)	F
        0b00111101, // (71)	G
        0b01110110, // (72)	H
        0b00000110, // (73)	I
        0b00011111, // (74)	J
        0b01101001, // (75)	K
        0b00111000, // (76)	L
        0b00010101, // (77)	M
        0b00110111, // (78)	N
        0b00111111, // (79)	O
        0b01110011, // (80)	P
        0b01100111, // (81)	Q
        0b00110001, // (82)	R
        0b01101101, // (83)	S
        0b01111000, // (84)	T
        0b00111110, // (85)	U
        0b00101010, // (86)	V
        0b00011101, // (87)	W
        0b01110110, // (88)	X
        0b01101110, // (89)	Y
        0b01011011, // (90)	Z
        0b00111001, // (91)	[
        0b01100100, // (92)	\ (this can't be the last char on a line, even in comment or it'll concat)
        0b00001111, // (93)	]
        0b00000000, // (94)	^
        0b00001000, // (95)	_
        0b00100000, // (96)	`
        0b01011111, // (97)	a
        0b01111100, // (98)	b
        0b01011000, // (99)	c
        0b01011110, // (100)	d
        0b01111011, // (101)	e
        0b00110001, // (102)	f
        0b01101111, // (103)	g
        0b01110100, // (104)	h
        0b00000100, // (105)	i
        0b00001110, // (106)	j
        0b01110101, // (107)	k
        0b00110000, // (108)	l
        0b01010101, // (109)	m
        0b01010100, // (110)	n
        0b01011100, // (111)	o
        0b01110011, // (112)	p
        0b01100111, // (113)	q
        0b01010000, // (114)	r
        0b01101101, // (115)	s
        0b01111000, // (116)	t
        0b00011100, // (117)	u
        0b00101010, // (118)	v
        0b00011101, // (119)	w
        0b01110110, // (120)	x
        0b01101110, // (121)	y
        0b01000111, // (122)	z
        0b01000110, // (123)	{
        0b00000110, // (124)	|
        0b01110000, // (125)	}
        0b00000001, // (126)	~
        0b00000001, // (127)	~
        0, // (127)	~
    };

    private static final int CMD_SET_BRIGHTNESS = 0x88;
    private static final int CMD_DISPLAY_OFF = 0x80;
    private static final int CMD_READ_KEYBOARD = 0x42;
    private static final int CMD_WRITE_TO_ADDR = 0x44;
    private static final int CMD_WRITE_BYTES = 0x40;
    private static final int CMD_ADDR_0 = 0xC0;

    public static final int BRIGHTNESS_HIGH = 0x07;
    public static final int BRIGHTNESS_MED = 0x04;
    public static final int BRIGHTNESS_LOW = 0x00;
    private static final int MAX_BUFFER_LENGTH = 32;

    private Gpio CLK, STB, DIO;
    private SpiDevice spiDevice;

    /**
     * Setup and initialize object using SPI device (MOSI pulled up to MISO by 1K resistor)
     * @param spiDeviceName
     * @param brightness desired display brightness 0-7
     * @throws IOException
     */
    public TM1838Driver(@NonNull String spiDeviceName, int brightness) throws IOException {
        PeripheralManagerService service = new PeripheralManagerService();
        spiDevice = service.openSpiDevice(spiDeviceName);
        spiDevice.setMode(SpiDevice.MODE3);
        spiDevice.setFrequency(16000000);     // 16MHz
        spiDevice.setDelay(80);
        spiDevice.setBitsPerWord(8);          // 8 BPW
        spiDevice.setBitJustification(false); // MSB first
        spiDevice.setCsChange(false);
        init(brightness);
    }

    /**
     * Setup and initialize object using GPIO pins
     * @param CLK_pin CLK pin name
     * @param STB_pin STB pin name
     * @param DIO_pin DIO pin name
     * @param brightness desired display brightness 0-7
     * @throws IOException
     */
    public TM1838Driver(@NonNull String CLK_pin, @NonNull String STB_pin, @NonNull String DIO_pin, int brightness) throws IOException {
        PeripheralManagerService service = new PeripheralManagerService();
        CLK = service.openGpio(CLK_pin); CLK.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
        STB = service.openGpio(STB_pin); STB.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
        DIO = service.openGpio(DIO_pin); DIO.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        init(brightness);
    }

    private void init(int brightness) throws IOException {
        // necessary for the TM1640
        if (STB != null && CLK != null) {
            STB.setValue(LOW);
            CLK.setValue(LOW);
            STB.setValue(HIGH);
            CLK.setValue(HIGH);
        }

        // Magic initial command
        sendCommand(CMD_WRITE_BYTES);
        setBrightness(brightness);
        clear();
    }

    @Override
    public void close() throws IOException {
        if (spiDevice != null) {
            try {
                spiDevice.close();
            }
            catch (IOException ex) {
                Log.e(TAG, "Error closing SPI", ex);
                throw ex;
            }
        }

        if (CLK != null && STB != null && DIO != null) {
            try {
                CLK.setValue(HIGH); CLK.close();
                STB.setValue(HIGH); STB.close();
                DIO.setValue(LOW); DIO.close();
            }
            catch (IOException ex) {
                Log.e(TAG, "Error closing GPIO", ex);
                throw ex;
            }
        }
    }

    private byte [] LSBtoMSB(byte [] buf, int length) {
        for (int i = 0; i < length; i++) {
            byte b = 0;
            byte x = buf[i];
            for (int bi = 0; bi < 8; bi++) {
                b<<=1;
                b|=( x &1);
                x>>=1;
            }
            buf[i] = b;
        }
        return buf;
    }

    private void writeByte(int data) throws IOException {
        if (spiDevice != null) {
            final byte [] buffer = new byte[1];
            buffer[0] = (byte)data;
            spiDevice.write(LSBtoMSB(buffer, 1), 1);
        }
        else {
            for (int i = 0; i < 8; i++) {
                CLK.setValue(LOW);
                DIO.setValue(((data >> i) & 0x01) != 0);
                CLK.setValue(HIGH);
            }
        }
    }

    private void writeBytes(byte [] data, int length) throws IOException {
        if (spiDevice != null) {
            spiDevice.write(LSBtoMSB(data, length), length);
        }
        else {
            for (int i = 0; i < length; i++)
                writeByte((int)data[i]);
        }
    }

    private int readByte() throws IOException {
        int result = 0;
        DIO.setDirection(Gpio.DIRECTION_IN);
        DIO.setValue(HIGH);
        for (int i = 0; i < 8; i++) {
            CLK.setValue(LOW);
            result |= (DIO.getValue() ? 1 : 0) << i;
            CLK.setValue(HIGH);
        }
        DIO.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        return result;
    }

    private void sendCommand(int command) throws IOException{
        if (STB != null) STB.setValue(LOW);
        writeByte(command);
        if (STB != null) STB.setValue(HIGH);
    }

    /**
     * Set display brightness. -1 turns display off
     * @param brightness 0-7 the brightness level. -1 turns display off
     */
    public void setBrightness(int brightness) throws  IOException {
        if (brightness < 0) sendCommand(CMD_DISPLAY_OFF);
        else sendCommand(CMD_SET_BRIGHTNESS | (brightness & 0x07));
    }

    /**
     * Read keyboard state and return 32 bits of keys
     * @return  long bitmap of 32 key states
     * @throws IOException
     */
    public long readKeyboard() throws IOException {
        long result = 0;
        sendCommand(CMD_READ_KEYBOARD);
        if (spiDevice != null) {
            final byte [] buffer = new byte[4];
            spiDevice.read(buffer, 4);
            result = (buffer[3] << 24) | (buffer[2] << 16) | (buffer[1] << 8) | buffer[0];
        }
        else {
            STB.setValue(LOW);
            for (int i = 0; i < 4; i++) {
                result |= (((long) readByte()) << i * 8);
            }
            STB.setValue(HIGH);
        }
        return result;
    }

    /**
     * Display a single character in given position (0-7)
     * @param c character to display. use c = 0 to empty this position
     * @param address position 0-7
     * @param period true if period should be displayed
     * @throws IOException
     */
    public void display(char c, int address, boolean period) throws IOException {
        sendCommand(CMD_WRITE_TO_ADDR);
        if (STB != null) STB.setValue(LOW);
        writeByte(CMD_ADDR_0 | address & 0x7);
        writeByte(FONT_DEFAULT[(int)c & 0xFF] | (period ? 0x80 : 0x00));
        if (STB != null) STB.setValue(HIGH);
    }

    private void prepareBufferSwap(byte [] buffer, int start, int length) {
        for (int i = start; i < length; i+=2) {
            byte b = buffer[i];
            buffer[i] = buffer[i + 1];
            buffer[i + 1] = b;
        }
    }

    /**
     * Fill display with up to 8 bytes of data. Show period into given position
     * @param data  up to 8 bytes to send to display starting from 0 position
     * @param periodMask display periods in bitmap position(s). 0 to hide period
     * @throws IOException
     */
    public void display(int [] data, int periodMask) throws IOException {
        sendCommand(CMD_WRITE_BYTES);
        if (STB != null) STB.setValue(LOW);
        int length = data.length > MAX_BUFFER_LENGTH - 1 ? MAX_BUFFER_LENGTH - 1 : data.length;
        final byte [] buffer = new byte [MAX_BUFFER_LENGTH + 2];
        buffer[0] = (byte)CMD_ADDR_0;
        for (int i = 0; i < length; i++)
            buffer[(i * 2) +1] = (byte)(data[i] | ((periodMask & i) != 0 ? 0x80 : 0x00));
        //prepareBufferSwap(buffer, 2, length + 1);
        writeBytes(buffer, (length * 2) + 1);
        if (STB != null) STB.setValue(HIGH);
    }

    /**
     * Display up to 8 characters on the display with period in given position
     * @param chars characters to display
     * @param periodPosition position of the period to display 0-7. -1 to hide a period
     * @throws IOException
     */
    public void display(String chars, int periodPosition) throws IOException {
        sendCommand(CMD_WRITE_BYTES);
        if (STB != null) STB.setValue(LOW);

        Log.d(TAG, "Display: " + chars);

        int length = chars.length() > MAX_BUFFER_LENGTH - 1 ? MAX_BUFFER_LENGTH - 1 : chars.length();
        final byte [] buffer = new byte [MAX_BUFFER_LENGTH + 1];
        buffer[0] = (byte)CMD_ADDR_0;
        for (int i = 0; i < length; i++)
            buffer[(i * 2) +1] = (byte)(FONT_DEFAULT[(int) chars.charAt(i) & 0xFF] | (periodPosition == i ? 0x80 : 0x00));
        //prepareBufferSwap(buffer, 2, length + 1);
        writeBytes(buffer, (length * 2) + 1);
        if (STB != null) STB.setValue(HIGH);
    }

    private static final int EMPTY[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    public void clear() throws IOException {
        display(EMPTY, 0);
    }
}
