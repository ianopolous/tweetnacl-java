# tweetnacl-java
A Java port of TweetNaCl from the original C. 

It has been extensively tested against the original C version and TweetNaCl.js for messages up to 2^24 bytes. It still needs a professional cryptographic audit. 

To import into your project you only need [org/peergos/crypto/TweetNaCl.java](https://github.com/ianopolous/tweetnacl-java/raw/master/src/org/peergos/crypto/TweetNaCl.java).

If you want to build and test the project via gradle you can run ./gradlew check. Currently the gradle build works on OSX and Linux

If you are using Linux, you can build and test via "make" as well.