# DeviceFinder

How can a blind person find its Bluetooth devices in an accessible and smooth way? That's exactly the challenge this project wants to tackle by gamifying the whole process of locating the device. In this sense, the proposed game takes inspiration from the famous children's game, hot and cold, thereby having the user's phone give active feedback about the distance to the device. This feedback must be as tactile or sonorous as possible, making it accessible to users with hearing disabilities.

## Table of Contents
1. [Installation](#installation)
2. [Usage](#usage)
3. [Configuration](#configuration)
4. [Contributing](#contributing)
5. [License](#license)
6. [Contact](#contact)

## Installation
To install the project, follow these steps:

1. Clone the repository to your local machine.
2. Open the project in Android Studio.
3. Build and run the project on your Android device or emulator.

### Prerequisites
- Android Studio
- Android SDK

## Usage
To use the project:

1. Ensure that the necessary permissions are granted for accessing Bluetooth and WiFi-related information, as well as location services.
2. Open the app on your Android device.
3. Press the "Measure Distance" button to start locating the target device.
4. Pay attention to the tactile or sonorous feedback provided by the app, which indicates the distance to the device.
5. When the signal strength indicates that the device is near the target, the app will play a predefined sound (Mario song) and display a message indicating success.

## Configuration
The project's AndroidManifest.xml file includes the necessary permissions for Bluetooth, WiFi, and location services.

- `BLUETOOTH`: Allows the app to perform Bluetooth-related operations.
- `BLUETOOTH_ADMIN`: Allows the app to perform Bluetooth administration tasks.
- `BLUETOOTH_SCAN`: Allows the app to perform Bluetooth scanning.
- `BLUETOOTH_CONNECT`: Allows the app to connect to paired Bluetooth devices.
- `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`: Allows the app to access fine and coarse location information.
- `ACCESS_BACKGROUND_LOCATION`: Allows the app to access location information in the background.
- `ACCESS_WIFI_STATE`: Allows the app to access information about Wi-Fi networks.
- `VIBRATE`: Allows the app to control the device's vibrator.
- `READ_EXTERNAL_STORAGE` and `WRITE_EXTERNAL_STORAGE`: Allows the app to read from and write to external storage.

The layout file (activity.xml) includes UI components for the main activity:

- `TextView`: Displays the app title and version.
- `TextView`: Displays instructions and feedback about the distance to the device.
- `Button`: Allows the user to start or stop searching for the device.
- `ImageView`: Displays the logo of the project.

The project's build.gradle file includes configuration options for the Android application:

- `compileSdk`: Specifies the version of the Android SDK to compile against.
- `minSdk`: Specifies the minimum Android API level required by the application.
- `targetSdk`: Specifies the target Android API level for the application.
- `versionCode` and `versionName`: Specifies the version code and version name of the application.
- `testInstrumentationRunner`: Specifies the test runner for instrumentation tests.

The dependencies section includes dependencies required for the project:

- `implementation("androidx.appcompat:appcompat:1.6.1")`: AndroidX AppCompat library.
- `implementation("com.google.android.material:material:1.9.0")`: Material Components library.
- `implementation("androidx.constraintlayout:constraintlayout:2.1.4")`: ConstraintLayout library.
- `testImplementation("junit:junit:4.13.2")`: JUnit library for unit tests.
- `androidTestImplementation("androidx.test.ext:junit:1.1.5")`: AndroidX JUnit extension for instrumentation tests.
- `androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")`: Espresso library for UI testing.

## Contributing
Contributions to the project are welcome! If you encounter any bugs, have suggestions for enhancements, or want to contribute code improvements, please follow these guidelines:

1. Check for existing issues or create a new one to discuss the proposed changes.
2. Fork the repository and create a new branch for your feature or fix.
3. Make your changes, ensuring adherence to coding conventions and style guidelines.
4. Test your changes thoroughly.
5. Submit a pull request referencing the related issue(s).

## Contact
For questions, support, or further information, please contact the project maintainer at [salvador.santos@nos.pt](salvador.santos@nos.pt) or [salvadorsantos2002@hotmail.com](salvadorsantos2002@hotmail.com).
