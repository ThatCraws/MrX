@echo off
::javadoc -author -d Doc -sourcepath app\src\main\java com.craws.tree com.craws.mrx.state
javadoc -author -d Doc -classpath C:\android\SDK\platforms\android-29\android.jar -sourcepath app\src\main\java;C:\android\SDK\platforms\android-29\android.jar -subpackages com.craws
pause