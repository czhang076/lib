CC = /usr/bin/gcc
CFLAGS = -Wall -Wextra -O3 -fomit-frame-pointer

# 目标文件列表
SOURCES = reduce.c ntt.c poly.c polyvec.c fips202.c symmetric.c cbd.c randombytes.c indcpa.c kem.c test_kyber.c
HEADERS = params.h reduce.h ntt.h poly.h polyvec.h fips202.h symmetric.h cbd.h randombytes.h indcpa.h kem.h
OBJECTS = reduce.o ntt.o poly.o polyvec.o fips202.o symmetric.o cbd.o randombytes.o indcpa.o kem.o test_kyber.o

# 默认目标
all: test_kyber

# 链接规则
test_kyber: $(OBJECTS)
	$(CC) $(CFLAGS) -o test_kyber $(OBJECTS)

# 编译规则 (.c -> .o)
%.o: %.c $(HEADERS)
	$(CC) $(CFLAGS) -c $< -o $@

# 清理规则
clean:
	rm -f *.o test_kyber
