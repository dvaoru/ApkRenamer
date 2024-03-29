# ApkRenamer

Use the renamer program to change an app name, a package name and an icon in an Android app.

Requirement - [JRE 1.8 (64-bit)](https://www.java.com/en/download/manual.jsp)

## Download:

[ApkRenamer.zip](https://github.com/dvaoru/ApkRenamer/releases/latest/download/ApkRenamer.zip)


## Usage:
```
java -jar renamer.jar [-a] <path/to/app.apk> [-o] <path/to/renamed.apk> [-n] <new_name> [-p] <new.package.name> [-i] <new_icon.png>
```
You can place app.apk to \"in\" folder, new_icon.png to \"icon\" folder
and run `java -jar renamer.jar` without arguments. Your renamed_app.apk will be placed in \"out\" folder

Optionally you can add the `[-d]` flag to perform a "deep renaming".

&nbsp;&nbsp;&nbsp;&nbsp;This will search for instances of the old package name in all files and replace them with the new package name.

&nbsp;&nbsp;&nbsp;&nbsp;If you rename an app with the deep renaming you can install the renamed app along with the original app on your device.  

&nbsp;&nbsp;&nbsp;&nbsp;Note that the deep renaming may cause unintended side effects, such as breaking app functionality.

Optionally you can add the `[-t]` flag and the program extract all apk resources at "temp" folder where you can modify it as you want.

&nbsp;&nbsp;&nbsp;&nbsp;After you made the changes you can resume program flow, and it builds and signs the renamed apk

Optionally you can add the `[-m]` flag and the program will not modify the resources of the apk.

&nbsp;&nbsp;&nbsp;&nbsp;It extracts the apk resources to "temp" folder where you can modify what you want manually.

&nbsp;&nbsp;&nbsp;&nbsp;The program will not rename anything. After you made changes resume the program, and it builds and signs the package.

Optionally, you can pass arguments to Apktool.

&nbsp;&nbsp;&nbsp;&nbsp; To implement arguments when Apktool decodes the apk, add the following flag: `-da "-option1 -option2"`. For example, `-da "--keep-broken-res"`. The string with arguments for Apktool should be enclosed in quotation marks.

&nbsp;&nbsp;&nbsp;&nbsp; To implement arguments when Apktool builds the apk, add the following flag: `-ba "-option1 -option2"`. For example, `-ba "--use-aapt2"`. The string with arguments for Apktool should be enclosed in quotation marks.

&nbsp;&nbsp;&nbsp;&nbsp; You can find a list of Apktool's arguments on its [official site](https://ibotpeaches.github.io/Apktool/documentation/).

Optionally add the `[-r] <path/to/dictionary.txt>` flag and the program will replace text in APK files using a dictionary.

&nbsp;&nbsp;&nbsp;&nbsp; Dictionary file format:
```
original text:replacement text
another original text:another replacement text
```
&nbsp;&nbsp;&nbsp;&nbsp; The splitter is ":" symbol. . If you need to include this symbol in the replacement text, you can escape it using "/:".

## Notice:
- You may not use ApkRenamer for any illegal purposes;
- The repacked APKs should not violate the original licenses.

## Third-Party Components

- [Apktool](https://github.com/iBotPeaches/Apktool)
- [Apk Sign](https://github.com/appium/sign)
- [Simple Java Image Tool](https://sjit.sourceforge.io/)
- [Thumbnailator](https://github.com/coobird/thumbnailator)

## Feedback
If you have any question email me [dvaoru@gmail.com](https://mail.google.com/mail/u/0/?view=cm&fs=1&tf=1&source=mailto&to=dvaoru@gmail.com)



