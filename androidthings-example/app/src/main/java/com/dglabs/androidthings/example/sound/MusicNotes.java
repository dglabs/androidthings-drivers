package com.dglabs.androidthings.example.sound;

/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class MusicNotes {

    public static final double REST = -1;

    public static final double B4 = 494;
    public static final double B4_FLAT = 466;
    public static final double A4 = 440;
    public static final double A4_FLAT = 415;
    public static final double G4 = 392;
    public static final double G4_FLAT = 370;
    public static final double F4 = 349;
    public static final double E4 = 330;
    public static final double E4_FLAT = 311;
    public static final double D4 = 294;
    public static final double D4_FLAT = 277;
    public static final double C4 = 262;
    public static final double B3 = 247;

    public static final double[] DRAMATIC_THEME = {
            G4, REST, G4, REST, G4, //REST, E4_FLAT, E4_FLAT
    };

    public static final double[] TWINKLE_THEME = {
            C4, REST, C4, REST, D4, REST, D4, REST, F4, REST, F4, REST, C4, REST, G4, REST, G4, REST, C4, REST, C4, REST, B3
    };

    private MusicNotes() {
        //no instance
    }
}