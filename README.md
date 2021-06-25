# SimplyBLE

## About

**SimplyBLE** is an Android application that displays a list of discoverable BLE devices.

Developed on Pixel 2XL running Android 11.

APK: [SimplyBLE v1.0](https://github.com/jekkogray/SimplyBLE/releases/tag/v1.0)

## Important Notes
Device Minimum API is 26.

## Technical Test Requirements
### Simple BLE Scanner App
- [x] Build a simple BLE scanner app the displays a list of discovered BLE devices 
- [x] Each device entry show in the list should include the device name, RSSI, and whether the device is connectable or not (this is part of the advertisement data)
- [x] Include a button to enable/disable sorting the list by RSSI
- [x] When one of the devices in the list is selected by the user, the app should connect to it.  Doesn’t need to do anything else
- [x] You may build the app for iOS or Android 
- [x] During the follow-up interview, we will have you walk us through the code you wrote and explain how it works 


## Building release apk to run on Android devices
To build your own release apk, in Android Studio navigate to the menu bar and select <b>Build > Build Bundle(s) / APK(s) > Build APK(s)</b>.

The release apk will start building.
When the application finish installing an indicator on the lower right *Event Log* will appear
indicating that the APK(s) generated succesfully for 1 module. 

Click on *locate* and it will direct you to the .apk file. 



