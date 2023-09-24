# ANDetect
ANDetect.jar is a tool for detecting the third-party advertising libraries of an APK. To help testers apply our tools, we provide demo videos. 

# Dev Environment
JDK version: 11.0.16.1
IntelliJ IDEA: 2020.1.4 x64

# Test Environmen
OS Name: Ubuntu 20.04.2 LTS
Processor: 12th Gen Intel® Core™ i9-12900K × 8 
Memory: 31.3 GiB

# Command
$ java -jar ANDetect.jar -a [APK name] -o [output file name]
where [APK name] refers to the target APK and the result will be written to [output file name] 

# Running Step
1. Download all files in this folder. Unzip resources.zip and then place the folder 'resources' in the same directory with 'ANDetect.jar' and 'demo_apks'.
2. Run ANDetect.jar with the given demo apks in demo_apks. Choose an apk in demo_apks and name an output file you like, for example 'result.txt', and the file will record the ad libraries detected in the demo apk.
$ java -jar ANDetect.jar -a demo_apks/encrypted_ad.apk -o result.txt

# Result
1. [Encrypted APK] presents the APK is encrypted or not.
2. If encrypted, ANDetect analyzes APK by resource features and gives the name of Ad networks in [Res Detect Result].
3. If not-encrypted, ANDetect analyzes APK by structural and semantic features and gives the name as well as version of Ad networks in [SC Detect Result].
