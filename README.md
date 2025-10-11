# **Maid Management App**

A native Android application designed to help households efficiently manage domestic help. This app streamlines task assignment, tracks biometric attendance, and automates monthly payroll calculations, making the entire process transparent and hassle-free.

## **✨ Key Features**

* **👤 User & Maid Profiles:** Secure registration for employers and comprehensive profile management for maids, including personal details and photos.  
* **👆 Biometric Attendance:** Secure and accurate attendance logging for each task using the maid's fingerprint. No more manual tracking\!  
* **📋 Task & Payroll Management:**  
  * Create custom tasks (cleaning, cooking, etc.).  
  * Assign a price and a specific frequency (daily, alternate days, weekly) to each task.  
  * The app automatically calculates the monthly salary based on completed tasks, deducting amounts for missed days.  
* **📞 Direct Communication:** Instantly call a maid directly from their profile within the app.  
* **☁️ Cloud-Powered:** All data is securely stored on a cloud server, ensuring it's always available and backed up.

## **🛠️ Tech Stack & Architecture**

This project is built with a modern, scalable, and maintainable tech stack, following SOLID principles and an MVVM architecture for the Android client.

### **Client-Side (Android App)**

* **Language:** [Kotlin](https://kotlinlang.org/)  
* **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetpack/compose)  
* **Architecture:** MVVM (Model-View-ViewModel)  
* **Biometrics:** Android BiometricPrompt API

### **Server-Side (Backend)**

* **Framework:** [Node.js](https://nodejs.org/) with [Express.js](https://expressjs.com/)  
* **Database:** [MongoDB](https://www.mongodb.com/) (hosted on MongoDB Atlas or AWS DocumentDB)  
* **Authentication:** JSON Web Tokens (JWT)  
* **File Storage:** [Amazon S3](https://aws.amazon.com/s3/) for profile pictures  
* **Hosting:** [AWS EC2](https://aws.amazon.com/ec2/)

## **🚀 Getting Started**

To get a local copy up and running, follow these simple steps.

### **Prerequisites**

* Android Studio (latest version)  
* Node.js & npm  
* MongoDB instance (local or cloud)  
* AWS Account for S3

### **Installation**

1. **Clone the repo**  
   git clone \[https://github.com/your-username/maid-management-app.git\](https://github.com/your-username/maid-management-app.git)

2. **Backend Setup**  
   \# Navigate to the server directory  
   cd server/

   \# Install NPM packages  
   npm install

   \# Create a .env file and add your configuration (DB\_URI, JWT\_SECRET, AWS\_KEYS)  
   cp .env.example .env

   \# Start the server  
   npm start

3. **Android App Setup**  
   * Open the /android folder in Android Studio.  
   * Let Gradle sync the dependencies.  
   * Update the API base URL in the network configuration file to point to your local server's address.  
   * Build and run the app on an emulator or a physical device.

## **📄 License**

Distributed under the MIT License. See LICENSE.txt for more information.

## **📧 Contact**

Your Name \- [@your\_twitter](https://www.google.com/search?q=https://twitter.com/your_twitter) \- email@example.com

Project Link: [https://github.com/your-username/maid-management-app](https://www.google.com/search?q=https://github.com/your-username/maid-management-app)