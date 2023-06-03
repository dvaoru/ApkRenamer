# ApkRenamer

Use the renamer program to change an app name, a package name and an icon in an Android app.

Requirement - [JRE 1.8 (64-bit)](https://www.java.com/en/download/manual.jsp)


## Usage:

java -jar renamer.jar [-a] <path/to/app.apk> [-o] <path/to/renamed.apk> [-n] <new_name> [-p] <new.package.name> [-i] <new_icon.png>

You can place app.apk to \"in\" folder, new_icon.png to \"icon\" folder
and run \"java -jar renamer.jar\" without arguments. Your renamed_app.apk will be placed in \"out\" folder

Optionally you can add the [-d] flag to perform a "deep renaming".

&nbsp;&nbsp;&nbsp;&nbsp;This will search for instances of the old package name in all files and replace them with the new package name.

&nbsp;&nbsp;&nbsp;&nbsp;If you rename an app with the deep renaming you can install the renamed app along with the original app on your device.  

&nbsp;&nbsp;&nbsp;&nbsp;Note that the deep renaming may cause unintended side effects, such as breaking app functionality.

Optionally you can add the [-t] flag and the program extract all apk resources at "temp" folder where you can modify it as you want.

&nbsp;&nbsp;&nbsp;&nbsp;After you made the changes you can resume program flow, and it builds and signs the renamed apk

Optionally you can add the [-m] flag and the program will not modify the resources of the apk.

&nbsp;&nbsp;&nbsp;&nbsp;It extracts the apk resources to "temp" folder where you can modify what you want manually.

&nbsp;&nbsp;&nbsp;&nbsp;The program will not rename anything. After you made changes resume the program, and it builds and signs the package.

## Download:

[ApkRenamer.zip](https://github.com/dvaoru/ApkRenamer/releases/latest/download/ApkRenamer.zip)


## Notice:
- You may not use ApkRenamer for any illegal purposes;
- The repacked APKs should not violate the original licenses.

## Third-Party Components

- [Apktool](https://github.com/iBotPeaches/Apktool)
- [Apk Sign](https://github.com/appium/sign)
- [Simple Java Image Tool](https://sjit.sourceforge.io/)

## Feedback
If you have any question email me [dvaoru@gmail.com](https://mail.google.com/mail/u/0/?view=cm&fs=1&tf=1&source=mailto&to=dvaoru@gmail.com)



