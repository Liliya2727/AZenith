#!/bin/sh

SKIPMOUNT=false
PROPFILE=true
POSTFSDATA=false
LATESTARTSERVICE=true
REPLACE="

"

sleep 2
ui_print
ui_print
ui_print "          ANTARES!            "
ui_print 
ui_print
ui_print "- Releases : 26/02/2025"
ui_print "- Author : @Zexshia"
ui_print "- Version : 1.5"
sleep 1
ui_print "- Device : $(getprop ro.product.board) "
sleep 2
ui_print "- Installing Antares!"
sleep 2
ui_print "- Extracting module files.."
sleep 2
ui_print "- Extracting system.prop"
sleep 1
mkdir /data/Antares
unzip -o "$ZIPFILE" 'common/system.prop' -d $MODPATH >&2
unzip -o "$ZIPFILE" 'system/*' -d $MODPATH >&2
unzip -o "$ZIPFILE" 'service.sh' -d "$MODPATH" >&2
unzip -o "$ZIPFILE" 'gamelist.txt' -d "/data/Antares" >&2
unzip -o "$ZIPFILE" 'antares.png' -d "/data/local/tmp" >&2
ui_print "- Installing bellavita toast"
unzip -o "$ZIPFILE" 'toast.apk' -d $MODPATH >&2
pm install $MODPATH/toast.apk
rm $MODPATH/toast.apk
set_perm_recursive $MODPATH 0 0 0777 0777
