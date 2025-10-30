# ORB_SLAM3_AR-for-Android
## Introduction
This is an Android Augmented Reality APP based on [ORB-SLAM3](https://github.com/UZ-SLAMLab/ORB_SLAM3) and OpenGL. *(TODO: The demo video can be found in the links below.)*

## Notice
We thank the great contribution of [@muziyongshixin](https://github.com/muziyongshixin/ORB-SLAM2-based-AR-on-Android).

**This project is simple and basic**, and I focus on the SLAM algorithm. You can discuss with [@Abonaventure](https://github.com/Abonaventure/ORB_SLAM3_AR-for-Android) or email bonaventure@163.com if you have any issues or questions.

## Demo Videos
*(TODO)*

## Dependencies, Installation & Usage

1. **Dependencies**
    - Due to the project is based on ORB-SLAM3, OpenCV4Android is needed.
    - Other third-party dependencies like DBow2, g2o, Eigen, boost, openssl, and opencv are included in the project(I have uploaded them all except Eigen source files, boost header files, Sophus, and OpenCV4Android).
    - Recommended IDE: Android Studio 2023.1.1 or higher.
    - You may need to edit some configurations in the `CMakeLists.txt` file at `app/CMakeLists.txt`.

2. **Download Eigen, boost, Sophus and OpenCV4Android**

   **1) Download Eigen 3.4.0**

   Crack its source files `[Eigen and unsupported]` into the following directory:

   ```
   */app/src/main/cpp/Eigen/eigen-3.4.0/
   ```

   **2) Download boost 1.72.0**

   Crack its header files `[boost]` into the following directory:

   ```
   */app/src/main/cpp/boost/boost-1_72_0/
   ```

   **2.1) Fix boost compatibility issue**

   In the file `boost/container_hash/hash.hpp`, around lines 129-130, find this code:

   ```
   template <typename T>
   struct hash_base : std::unary_function<T, std::size_t> {};
   ```

   Replace it with:

   ```
   template <typename T>
   #if defined(__cplusplus) && __cplusplus >= 201703L
       // C++17 and later - std::unary_function was removed
       struct hash_base {
           typedef T argument_type;
           typedef std::size_t result_type;
       };
   #else
       // C++14 and earlier - use std::unary_function
       struct hash_base : std::unary_function<T, std::size_t> {};
   #endif
   ```

   **3) Download OpenCV 4.12.0 Android SDK**

   **3.1) Extract the SDK, go within the `sdk` folder, and copy all its contents directly into:

   ```
   */opencvLibrary/
   ```
   
   **3.2) Copy include files**

   From `opencv-4.12.0-android-sdk/sdk/native/jni/include/` to:

   ```
   */app/src/main/cpp/opencv/opencv-4.12.0/include/
   ```
   *(Basically copy everything except videoio, photo and gapi)*

   **3.3) Copy native libraries**

   **(NOTE: ALREADY ADDED Except for x86 and x86_64)**

   From `opencv-4.12.0-android-sdk/sdk/native/libs/` to:

   ```
   */app/src/main/jniLibs/
   ```
   *(Copy each `libopencv_java4.so` and paste it within their respective directories)*

   **4) Download Sophus 1.24.6**

   Crack its files `[sophus]` into the following directory:

   ```
   */app/src/main/cpp/ORB/Thirdparty/Sophus/
   ```

### 3. Additional Notes
- You may also need to calibrate your phone's camera for better performance. (More details of camera calibration can be found on google.)
- Due to the diversity of Android system version, you may also need to change some configurarions in the ***AndroidManifest.xml*** to make sure that the app have the authority to use the camera and file system.

## Framework & Results
The system is consisted of two parts(https://github.com/Abonaventure/ORB_SLAM3_AR-for-Android), the ORB-SLAM3 part which is used to get the camera's pose matrix. The other part is the OpenGL Rendering module, which use the pose matrix to render the 3D object(the earth in this project).

The ORB-SLAM3 system requires lots of computing resources and depend on Calibration the camera, so this APP can not initilize very quikly(the screen will show red "SLAM NOT INITIALIZED").It's better to choose a rich texture scene to initilize the SLAM(the screen will show green "SLAM ON").

The ORB-SLAM3 system requires significant computing resources and depend on Calibration the camera, so this APP may not initilize very quikly(the screen will show red "SLAM NOT INITIALIZED").It's better to choose a rich texture scene to initilize the SLAM(the screen will show green "SLAM ON").
