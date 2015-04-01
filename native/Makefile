

.PHONY: compile
compile: 
	#gcc -shared -c -fPIC tweetnacl.c -o tweetnacl.so
	gcc -c -Wall -fPIC tweetnacl.c -o tweetnacl.o
	gcc -shared -o libtweetnacl.so tweetnacl.o

