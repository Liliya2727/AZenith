if [ -f /data/adb/modules/AZenith/disable ]; then
  cat /data/adb/modules/AZenith/module.prop.orig > /data/adb/modules/AZenith/module.prop
  rm /data/adb/service.d/.azenith_cleanup.sh
fi
