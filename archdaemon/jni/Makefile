CC     = clang
TARGET = sys.azenith-service

SRCS = Main.c $(wildcard src/*/*.c)

CFLAGS  = -Iinclude -DNDEBUG -Wall -Wextra -Werror \
          -pedantic-errors -Wpedantic \
          -O2 -std=c23 -fPIC -flto

LDFLAGS = -llog -flto

.PHONY: all clean

all: $(TARGET)

$(TARGET): $(SRCS)
	$(CC) $(CFLAGS) $(SRCS) $(LDFLAGS) -o $@

clean:
	rm -f $(TARGET)
