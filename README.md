# **My Maid Manager (Android App) 📱**

This is the native Android application for My Maid Manager, designed to help users manage household staff, track tasks, verify attendance via OTP, and calculate monthly payroll.

## **✨ Features**

* **👤 User Authentication:** Secure user registration and login using JWT. Session persistence ensures users stay logged in.  
* **🧹 Maid Management:** Add new maids with their contact details. View a list of all maids.  
* **📄 Maid Details:** View detailed information for a specific maid.  
* **📝 Task Management:** Add tasks with price and frequency for each maid. Delete tasks.  
* **📲 OTP Attendance Verification:** Securely mark attendance for specific tasks using Twilio Verify for OTP generation and verification sent via SMS.  
* **📅 Attendance History:** View the recent attendance records for each maid.  
* **💰 Payroll Calculation:** View the calculated salary for the current month based on assigned tasks and attendance records.  
* **🎨 Modern UI:** Built with Jetpack Compose using Material 3 design principles.

## **🛠️ Tech Stack**

* **Language:** Kotlin  
* **UI Toolkit:** Jetpack Compose (Material 3\)  
* **Architecture:** MVVM (Model-View-ViewModel)  
* **Networking:** Retrofit & OkHttp  
* **Navigation:** Jetpack Navigation Compose  
* **Security:** EncryptedSharedPreferences (for session token)

## **🚀 Setup**

1. **Clone the repository:**  
   git clone https://github.com/adish450/MyMaidManager

2. **Open in Android Studio:** Open the cloned project folder in the latest stable version of Android Studio.  
3. **Configure Backend URL:**  
   * Open the file app/src/main/java/com/laundrypro/mymaidmanager/network/RetrofitClient.kt.  
   * Find the BASE\_URL constant.  
   * Replace the placeholder IP address with the actual Public IPv4 address of your running backend server (e.g., http://YOUR\_EC2\_IP:5000/).  
4. **Build and Run:** Build the project and run it on an Android emulator or a physical device.

## **🔗 Backend**

This application requires the corresponding backend API to function. The backend repository can be found here:

https://github.com/adish450/MyMaidManagerBackend