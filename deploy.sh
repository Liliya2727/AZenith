#!/system/bin/sh
pids=$(toybox pidof sys.azenith-service)
for p in $pids; do kill -TERM $p 2>/dev/null; done
sleep 1
cp /data/data/com.termux/files/home/AZenith/archdaemon/target/release/azenith-service /data/local/tmp/azenith-service
cp /data/local/tmp/azenith-service /data/adb/modules/AZenith/system/bin/sys.azenith-service
chmod 755 /data/adb/modules/AZenith/system/bin/sys.azenith-service
/data/adb/modules/AZenith/system/bin/sys.azenith-service --run
echo "run rc=$?"
sleep 4
ps -A -o PID,NAME | grep -iE "appmonitoring|sys.azenith-service"
