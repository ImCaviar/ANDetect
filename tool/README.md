# ANDetect

ANDetect.jar is a tool for detecting the third-party advertising libraries of an APK.

## Dev Environment
JDK version: 11.0.16.1
IntelliJ IDEA: 2020.1.4 x64

## Test Environmen
OS Name: Ubuntu 20.04.2 LTS
Processor: 12th Gen Intel® Core™ i9-12900K × 8 
Memory: 31.3 GiB

## Command
$ java -jar ANDetect.jar -a [APK name] -o [output file name]
where [APK name] refers to the target APK and the result will be written to [output file name] 

## Result
1. The APK is encrypted or not.
2. If encrypted, ANDetect analyzes APK by resource features and gives the name of Ad networks.
3. If not-encrypted, ANDetect analyzes APK by structural and semantic features and gives the name as well as version of Ad networks.
