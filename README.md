# presentation-control-app
An example of using [Microsoft Band SDK for Android](https://developer.microsoftband.com/bandsdk) and [Microsoft Band Gestures library for Android](https://bintray.com/osacci/maven/microsoft-band-gestures).
Android app for switching presentation slides through with Microsoft Band 2 through https://github.com/timosadchiy/presentation-control.

# Requirements
[Microsoft Band SDK for Android](https://developer.microsoftband.com/bandsdk)
[Microsoft Band Gestures library for Android](https://bintray.com/osacci/maven/microsoft-band-gestures)

# Getting started
1. Install [Microsoft Health App for Android](https://play.google.com/store/apps/details?id=com.microsoft.kapp).
2. Clone the project.
3. Import the requirement listed above into the project.
4. Compile on the phone.
5. Install and run https://github.com/timosadchiy/presentation-control on your computer.
6. Press Connect to device in the App.

**Note:** Constants in the `BleManager.java` must match `serviceUuid` and `controlCharacteristicUuid` respectively in the `config.js` of the [presentation-control](https://github.com/timosadchiy/presentation-control).
