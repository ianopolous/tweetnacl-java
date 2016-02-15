CP = `find lib -name "*.jar" -printf %p:`
JAVA_BUILD_OPTS = -g -source 1.8 -target 1.8 -cp .:$(CP)
CP_SPACE = .

.PHONY: def
def: clean compile jni test

.PHONY: clean
clean:
	rm -fr build
	rm -f libtweetnacl.so
	rm -f Test.jar

.PHONY: compile 
compile: 
	mkdir -p build
	javac $(JAVA_BUILD_OPTS) -d build `find src -name \*.java`

.PHONY: test 
test: compile
	echo "Name: TweetNaCl.java Tests" > def.manifest
	echo "Main-Class: test.JSTest" >> def.manifest
	echo "Build-Date: " `date` >> def.manifest
	echo "Class-Path: " $(CP_SPACE)>> def.manifest
	jar -cfm Test.jar def.manifest \
	    -C build org -C build test
	rm -f def.manifest

.PHONY: jni
jni: compile
	javah -jni -classpath build -d jni org.peergos.crypto.JniTweetNacl
	gcc -Wimplicit-function-declaration -fPIC -std=c11 -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux -Inative -Ijni -shared -o libtweetnacl.so jni/org_peergos_crypto_JniTweetNacl.c

.PHONY: jni_tests
jni_tests: def 
	java -Djava.library.path=. -cp "Test.jar:lib/*" test.TestRunner

