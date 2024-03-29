/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.deskclock.worldclock;

import android.content.SharedPreferences;
import android.util.Log;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

public class Cities {

    public static final String WORLDCLOCK_UPDATE_INTENT = "com.android.deskclock.worldclock.update";
    public static final String NUMBER_OF_CITIES = "number_of_cities";

    public static void saveCitiesToSharedPrefs(
            SharedPreferences prefs, HashMap<String, CityObj> cities) {
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(NUMBER_OF_CITIES, cities.size());
        final Collection<CityObj> col = cities.values();
        final Iterator<CityObj> i = col.iterator();
        int count = 0;
        while (i.hasNext()) {
            final CityObj c = i.next();
            c.saveCityToSharedPrefs(editor, count);
            count++;
        }
        editor.apply();
    }

    public static  HashMap<String, CityObj> readCitiesFromSharedPrefs(SharedPreferences prefs) {
        final int size = prefs.getInt(NUMBER_OF_CITIES, -1);
        final HashMap<String, CityObj> c = new HashMap<String, CityObj>();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                final CityObj o = new CityObj(prefs, i);
                if (o.mCityName != null && o.mTimeZone != null) {
                    c.put(o.mCityId, o);
                } 
            }
        }
        return c;
    }


    public static final String CITY_NAME = "city_name_";
    public static final String CITY_TIME_ZONE = "city_tz_";

    public static void dumpCities(SharedPreferences prefs, String title) {
        final int size = prefs.getInt(NUMBER_OF_CITIES, -1);

        Log.d("Cities", "Selected Cities List " + title);
        Log.d("Cities", "Number of cities " + size);
        //Old Code:
        //HashMap<String, CityObj> c = new HashMap<String, CityObj>();
        // Yuqi's Change: removed this line of code.
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                /* old code 
                CityObj o = new CityObj(prefs, i);

                if (o.mCityName != null && o.mTimeZone != null) {
                    Log.d("Cities", "Name " + o.mCityName + " tz " + o.mTimeZone);
                }
                */

                /*Yuqi's Change start*/
                final String mCityName = prefs.getString(CITY_NAME + i, null);
                final String mTimeZone = prefs.getString(CITY_TIME_ZONE + i, null);
                if (mCityName != null && mTimeZone != null) {
                    Log.d("Cities", "Name " + mCityName + " tz " + mTimeZone);
                }
                /*Yuqi's Change end*/
            }
        }
    }
}
