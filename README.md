# PharmacIST

PharmacIST is a mobile application developed for the Android operating system as part of the Mobile and Ubiquitous Computing master's course @ IST. It serves as a platform to help users find pharmacies and medicines in times of need, providing a comprehensive and user-friendly interface for discovering and accessing pharmacy information.
Built using Kotlin and Jetpack Compose.

Authors | Github
--------|--------
Yassir Yassin | https://github.com/yassirscreed
Luis Marques | https://github.com/Pacten15
Tiago Santos | https://github.com/xDraGonGamer

**Project Grade:** 20 / 20

## Overview

In this project, we have developed a non-trivial mobile application for the Android Operating System called PharmacIST. This application explores various key aspects of mobile development, including location awareness, resource management, and user interaction. PharmacIST aims to provide mobile support for finding pharmacies and medicines, helping users locate nearby pharmacies, discover available medicines, and manage pharmacy information.

## Mandatory Features

The PharmacIST app includes the following core functionalities:

### Map Screen
- Draggable and centerable map displaying pharmacy locations
- Address search functionality
- Pharmacy markers with different colors for favorites
- Information popups for pharmacies
- Option to add new pharmacies

### Medicine Search
- Search for medicines with sub-string functionality
- Display of closest pharmacies with the searched medicine

### Pharmacy Information Panel
- Detailed information about each pharmacy
- List of available medicines
- Options to add/remove medicines and manage stock
- Favorite toggle and flagging system

### Medicine Information Panel
- Detailed information about each medicine
- Option to set notifications for availability in favorite pharmacies

## Backend Service

The implementation of the back-end was done using Firebase, a set of backend cloud computing services and application development platforms provided by Google. It hosts databases, services, authentication, and integration for a variety of applications. Specifically, we used:

- Firestore database to store all associated data for users, pharmacies, and medicines
- Firebase Authentication for user login
- Firebase Storage to store pharmacy and medicine photos in the backend

This backend solution provides robust data management, real-time synchronization, and scalable storage for our application.

## Resource Frugality and Caching

PharmacIST implements several strategies to optimize resource usage and enhance performance:

- Efficient caching system using Room for local database storage
- Optimized map population and data retrieval
- Handling of metered connections with selective data loading
- Photo caching to reduce redundant fetches
- Real-time synchronization between devices and back-end

## Notification Handling

The app provides a robust notification system for medicine availability and nearby pharmacies, enhancing user engagement and information accessibility.

## Additional Components

We have implemented the following additional features:

- Securing Communication
- Meta Moderation
- User Accounts
- Social Sharing To Other Apps
- UI Adaptability: Light/Dark Theme

## Execution Instructions

### Using an Emulator

1. Open the folder "PharmacIST" in Android Studio. The folder will be recognized as an Android project by the Android icon that appears in the file manager provided by the Android Studio IDE.
2. Wait for Gradle to build the project.
3. Go to Device Manager and click the plus icon. Then, click "Create Virtual Device," which will lead to a menu to create a virtual emulator of a specific model and Android version. The preferable model and version of the emulated device is a Pixel 6 using the VanillaIceCream Google API, which is essentially Android 14.
4. Select the new device emulator created, which will appear next to the app text, and click the play button.
5. Register a user for yourself.
6. Log in with the user created.
7. Use the app as you please. The way the project works is presented in the report.

**Side Note:** The barcode reader does not work on the emulator; it only works with non-emulated smartphones. So, when the button to scan a barcode is clicked, it will do nothing.
And also the APIs will most likely not work since the keys has been removed.

### Using a Real Android Smartphone

1. Open the folder "PharmacIST" in Android Studio. The folder will be recognized as an Android project by the Android icon that appears in the file manager provided by the Android Studio IDE.
2. Wait for Gradle to build the project.
3. Verify if your device is being recognized by the IDE.
4. If your device is not recognized, activate the programmer mode of your smartphone. This process depends on the model, so if you do not know how to do it, please search for instructions specific to your model.
5. Now, with Android Studio recognizing your device, just press the play button and accept the installation of the app on your smartphone, assuming you will receive a pop-up to do so.
6. The app will be executed right after and can be used.
7. Register a user for yourself.
8. Log in with the user created.
9. Use the app as you please. The way the project works is presented in the report.

## Conclusion

PharmacIST offers a comprehensive, user-friendly mobile application that enables users to discover, search, and access pharmacies and medicines around them. With its modern interface, efficient approach, and rich functionalities, PharmacIST aims to assist people in finding pharmacies and medicines efficiently, utilizing the mobile device's available resources and toolsets to provide an optimized experience.
