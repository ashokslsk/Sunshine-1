Sunshine (Wearable Watch Face)
===================================
#### Project 6 ("Go Ubiquitous") of the Udacity Android Developer Nanodegree

Synchronizes weather information from OpenWeatherMap on Android Phones, Tablets, and Wearables.

Project 6 specifically focuses on creating the watch face for wearable so users can access Sunshine's weather information at a glance.


Getting Started
-------------
1. Weather information is acquired through openweathermap.org API. An account must be created on this site and an API Key must be requested.
2. A Google developer account is required along with the following actions:
  * A project must be created in your google developer console https://console.developers.google.com
  * Google Cloud Messaging API must be requested for this project
  * Configuration file "google-services.json" must be requested.  Follow instructions here: https://developers.google.com/cloud-messaging/android/client to request file.

Getting Started
---------------
This sample uses the Gradle build system.  To build this project, use the
"gradlew build" command or use "Import Project" in Android Studio.


Installation
------------
1. Clone the GitHub repository 
2. gradle.properties must exist in "Sunshine" and have this line of code, with your own API KEY in quotation marks

 ```MyOpenWeatherMapApiKey = "PUT_MY_API_KEY_HERE"```

3. Place "google-services.json" file in Sunshine/app directory.
4. This project uses the Gradle build system.  To build this project, use the gradlew build" command or open project in Android Studio and build.  

 
Android Wear APIs

1) What steps need to be taken
2) What should the user already have installed or configured
3) What might they have a hard time understanding right away

Pre-requisites
--------------
Android SDK 21 or Higher
Build Tools version 23.0.2
Android Suport Wearable 1.3.0
Play Services Wearable 8.3.0

BumpTech Glide 3.5.2

Android Support AppCompat 23.1.1
Android Support Annotations 23.1.1
Android Support GridLayout 23.1.1
Android Support CardView 23.1.1
Android Support Design 23.1.1
Android Support RecyclerView 23.1.1
Android Support Wearable 1.3.0
Google Play Services GCM 8.3.0
Google Play Services Wearable 8.3.0

Muzei Muzei 2.0

License
-------
Copyright 2015 The Android Open Source Project, Inc.

Licensed to the Apache Software Foundation (ASF) under one or more contributor
license agreements.  See the NOTICE file distributed with this work for
additional information regarding copyright ownership.  The ASF licenses this
file to you under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License.  You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
License for the specific language governing permissions and limitations under
the License.
