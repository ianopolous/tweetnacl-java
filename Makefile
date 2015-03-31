CP = `find lib -name "*.jar" -printf %p:`
JAVA_BUILD_OPTS = -g -source 1.8 -target 1.8 -cp .:$(CP)
CP_SPACE = .

.PHONY: test
test: 
	mkdir -p build
	echo "Name: TweetNaCl.java Tests" > def.manifest
	echo "Main-Class: test.Test" >> def.manifest
	echo "Build-Date: " `date` >> def.manifest
	echo "Class-Path: " $(CP_SPACE)>> def.manifest
	javac $(JAVA_BUILD_OPTS) -d build `find src -name \*.java`
	jar -cfm Test.jar def.manifest \
	    -C build org -C build test
	rm -f def.manifest
