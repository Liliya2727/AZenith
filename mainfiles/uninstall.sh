#!/bin/sh

#
# Copyright (C) 2024-2025 Zexshia
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Remove Persistent Properties
setprop persist.sys.azenith.state ""
resetprop --delete persist.sys.azenith.state

setprop persist.sys.azenith.debugmode ""
resetprop --delete persist.sys.azenith.debugmode

setprop persist.sys.azenith.service ""
resetprop --delete persist.sys.azenith.service

setprop persist.sys.azenithconf.logd ""
resetprop --delete persist.sys.azenithconf.logd

setprop persist.sys.azenithconf.DThermal ""
resetprop --delete persist.sys.azenithconf.DThermal

setprop persist.sys.azenithconf.SFL ""
resetprop --delete persist.sys.azenithconf.SFL

setprop persist.sys.azenithconf.malisched ""
resetprop --delete persist.sys.azenithconf.malisched

setprop persist.sys.azenithconf.fpsged ""
resetprop --delete persist.sys.azenithconf.fpsged

setprop persist.sys.azenithconf.schedtunes ""
resetprop --delete persist.sys.azenithconf.schedtunes

setprop persist.sys.azenithconf.clearbg ""
resetprop --delete persist.sys.azenithconf.clearbg

setprop persist.sys.azenithconf.bypasschg ""
resetprop --delete persist.sys.azenithconf.bypasschg

setprop persist.sys.azenithconf.APreload ""
resetprop --delete persist.sys.azenithconf.APreload

setprop persist.sys.azenithconf.iosched ""
resetprop --delete persist.sys.azenithconf.iosched

setprop persist.sys.azenithconf.cpulimit ""
resetprop --delete persist.sys.azenithconf.cpulimit

setprop persist.sys.azenithconf.dnd ""
resetprop --delete persist.sys.azenithconf.dnd

setprop persist.sys.azenithconf.AIenabled ""
resetprop --delete persist.sys.azenithconf.AIenabled


# Uninstall module directories
rm -rf /data/local/tmp/module_icon.png
rm -rf /data/adb/.config/AZenith
rm -rf /data/AZenith

# Uninstaller Script
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
  done <$INFO
  rm -f $INFO
fi
