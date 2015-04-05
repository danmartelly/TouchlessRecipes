Touchless Recipes

By: Jamar Brooks, Daniel Martelly

In order to get this working you'll need to add some external jars to your project for compilation to work.
- You'll need to download the Leap Motion SDK and add the lib\LeapJava.jar as an external JAR
- You then need to edit the native library location to point to the lib\x64 or lib\x86 folder depening on which architecture you have.
- Add the freetts-1.2\lib\freetts.jar as an external JAR
- Copy the freetts-1.2\speech.properties to C:\Users\USERNAME\ . If that doesn't work, hopefully an error message pops up saying where you do need to copy it to.