#!/system/bin/sh

#
# Copyright (C) 2026-2027 Zexshia
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

# Wait for boot to Complete
while [ "$(resetprop sys.boot_completed)" != "1" ]; do
	sleep 40
done

# Refresh daemon state
if [ -z "$(resetprop persist.sys.azenith.state)" ] || { [ "$(resetprop persist.sys.azenith.state)" = "running" ] && [ -z "$(/system/bin/toybox pidof sys.azenith-service)" ]; }; then
    resetprop -n persist.sys.azenith.state stopped
    resetprop -n persist.sys.azenith.service ""
fi

# Run Daemon
sys.azenith-service --run
