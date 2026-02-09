CC = gcc
CFLAGS = -Wall -Wextra -O3 -fomit-frame-pointer

# 目标文件列表
SOURCES = reduce.c ntt.c poly.c polyvec.c fips202.c symmetric.c cbd.c randombytes.c indcpa.c kem.c verify.c test_kyber.c
HEADERS = params.h reduce.h ntt.h poly.h polyvec.h fips202.h symmetric.h cbd.h randombytes.h indcpa.h kem.h verify.h
OBJECTS = reduce.o ntt.o poly.o polyvec.o fips202.o symmetric.o cbd.o randombytes.o indcpa.o kem.o verify.o test_kyber.o

# 默认目标
all: test_kyber.exe libpkyber.dll

# 链接规则
test_kyber.exe: $(OBJECTS)
	$(CC) $(CFLAGS) -o test_kyber.exe $(OBJECTS)

libpkyber.dll: $(OBJECTS)
	$(CC) $(CFLAGS) -shared -o libpkyber.dll $(OBJECTS)

# 编译规则 (.c -> .o)
%.o: %.c $(HEADERS)
	$(CC) $(CFLAGS) -c $< -o $@

# 清理规则
clean:
	del /Q *.o test_kyber.exe 2>nul || echo Clean done