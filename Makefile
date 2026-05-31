ifeq ($(OS),Windows_NT)
  JAVA_HOME_UNIX := $(shell cygpath -u '$(JAVA_HOME)')
  LIB_NAME = tweetnacl.dll
  JNI_INCLUDE = -I"$(JAVA_HOME_UNIX)/include" -I"$(JAVA_HOME_UNIX)/include/win32"
  SHARED_FLAGS = -shared
  EXTRA_LIBS = -lbcrypt
else
  JAVA_HOME_UNIX := $(JAVA_HOME)
  UNAME_S := $(shell uname -s)
  ifeq ($(UNAME_S),Darwin)
    LIB_NAME = libtweetnacl.dylib
    JNI_INCLUDE = -I$(JAVA_HOME_UNIX)/include -I$(JAVA_HOME_UNIX)/include/darwin
    SHARED_FLAGS = -dynamiclib
    EXTRA_LIBS =
  else
    LIB_NAME = libtweetnacl.so
    JNI_INCLUDE = -I$(JAVA_HOME_UNIX)/include -I$(JAVA_HOME_UNIX)/include/linux
    SHARED_FLAGS = -shared -Wl,-z,max-page-size=16384
    EXTRA_LIBS =
  endif
endif

CP = $(shell find lib -name "*.jar" | tr '\n' ':')
JAVA_BUILD_OPTS = -g -source 1.8 -target 1.8 -cp .:$(CP)
CP_SPACE = .

.PHONY: def
def: clean compile jni test

.PHONY: clean
clean:
	rm -fr build
	rm -f libtweetnacl.so libtweetnacl.dylib tweetnacl.dll
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
jni:
	mkdir -p build
	javac -h jni -d build src/peergos/server/crypto/JniTweetNacl.java
	gcc -O2 -Wimplicit-function-declaration -fPIC -std=c11 \
	  $(JNI_INCLUDE) -Inative -Ijni \
	  $(SHARED_FLAGS) -o $(LIB_NAME) \
	  jni/peergos_server_crypto_JniTweetNacl.c $(EXTRA_LIBS)

.PHONY: jni_tests
jni_tests: def
	java -Djava.library.path=. -cp "Test.jar:lib/*" test.TestRunner
