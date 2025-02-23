#syste#!/bin/sh
while [ -z "$(getprop sys.boot_completed)" ]; do
    sleep 15
done



#Run Antares Service
Antares >/dev/null 2>&1
