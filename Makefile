JAVA_BUILD_OPTS = -g -source 1.8 -target 1.8
CP_SPACE = .

.PHONY: def
def: clean compile jni test

.PHONY: clean
clean:
	rm -fr build

.PHONY: compile 
compile: 
	mkdir -p build/classes/jar

	javac $(JAVA_BUILD_OPTS) -cp ".:lib/*" -d build/classes/jar `find src/main -name \*.java`
	javac $(JAVA_BUILD_OPTS) -cp ".:lib/*:build/classes/jar" -d build/classes/jar `find src/test -name \*.java`



.PHONY: test 
test: compile
	echo "Name: TweetNaCl.java Tests" > def.manifest
	echo "Main-Class: org.peergos.crypto.JSTest" >> def.manifest
	echo "Build-Date: " `date` >> def.manifest
	echo "Class-Path: " $(CP_SPACE)>> def.manifest


	jar -cfm build/Test.jar def.manifest -C build/classes/jar org -C src/test/resources nacl.js

	rm -f def.manifest


.PHONY: jni
jni: compile
	javah -jni -classpath build/classes/jar  org.peergos.crypto.JniTweetNacl
	gcc -Wimplicit-function-declaration -fPIC -std=c11 -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux -Isrc/test/resources -Ibuild/jniheaders  -shared -obuild/libtweetnacl.so src/test/resources/org_peergos_crypto_JniTweetNacl.c

.PHONY: jni_test
jni_tests: def
	java -Xmx250m -Xms250m -Djava.library.path=build -cp "build/Test.jar:lib/*" org.peergos.crypto.TestRunner