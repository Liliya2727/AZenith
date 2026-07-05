CC     = clang
TARGET = sys.azenith-service
TARGET_TRIPLE = aarch64-unknown-linux-android26

SRCS = Main.c $(wildcard src/*/*.c)

CFLAGS  = -Iinclude -DNDEBUG -Wall -Wextra -Werror \
          -pedantic-errors -Wpedantic \
          --target=$(TARGET_TRIPLE) \
          -O2 -std=c23 -fPIC -flto

LDFLAGS = -llog -flto --target=$(TARGET_TRIPLE)

.PHONY: all clean

all: $(TARGET)

$(TARGET): $(SRCS)
	$(CC) $(CFLAGS) $(SRCS) $(LDFLAGS) -o $@

clean:
	rm -f $(TARGET)