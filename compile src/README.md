#The environment to run the source code:

1. OS: Windows 10 or Windows 11
2. JDK: JDK 11
3. Running tool: IntelliJ IDEA(≥2020.1.4)


#The steps of compiling the source code of ANDetect:

*STEP0: Unzip ANDetect.zip. and resources.zip.*

*STEP1: Add apktool-2.7.0.jar to local maven repository.*
</br>mvn install:install-file -Dfile=[the path of apktool-2.7.0.jar] -DgroupId=com.custom -DartifactId=apktool -Dversion=2.7.0 -Dpackaging=jar

*STEP2: Change the output directory in line 33 of pom.xml.*
Change 'E:\target' to suitable path as the result of ANDetect.jar.

*STEP3: Config the JDK and main class of IDEA.*
Add new 'Application' named 'ANDetect'.
Set 'JRE' as the path of JDK 11.
Set 'Main class' as 'iie.group5.ANDMain'.

*STEP4: Build the project and Run.*

*STEP5: Pack the project to jar by Maven.*
Maven-> Lifecycle -> double click 'package' 
Then, the jar will be generated in the path given in STEP2.
Change the name of ANDetectMaven-0.0.1-SNAPSHOT.jar as 'ANDetect.jar'.
Eventually, you can run ANDetect.jar in Ubuntu with the guidance given in 'tool'. Notably, when running ANDetect.jar, the package of 'resources' should be placed in the same directory.
