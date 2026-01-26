#include <stddef.h>
#include <stdint.h>
#include <fcntl.h>
#include <unistd.h>
#include "randombytes.h"

int randombytes(uint8_t *out, size_t outlen) {
  int fd = open("/dev/urandom", O_RDONLY);
  if (fd < 0) return -1;
  size_t off = 0;
  while (off < outlen) {
    ssize_t n = read(fd, out + off, outlen - off);
    if (n <= 0) {
      close(fd);
      return -1;
    }
    off += (size_t)n;
  }
  close(fd);
  return 0;
}
