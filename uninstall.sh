#!/bin/sh
# remove Antares data folder
rm -rf /data/Antares
rm -rf /data/local/tmp/Antares.log
rm -rf /data/local/tmp/AntaresInstall.log
rm -rf /data/local/tmp/Antares.png

if [ -f $INFO ]; then
  while read LINE; do
    if [ "$(echo -n $LINE | tail -c 1)" == "~" ]; then
      continue
    elif [ -f "$LINE~" ]; then
      mv -f $LINE~ $LINE
    else
      rm -f $LINE
      while true; do
        LINE=$(dirname $LINE)
        [ "$(ls -A $LINE 2>/dev/null)" ] && break 1 || rm -rf $LINE
      done
    fi
  done < $INFO
  rm -f $INFO
fi
