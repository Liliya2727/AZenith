#!/system/bin/sh

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

# Bypass Charging Path
MTK_BYPASS_CHARGER="/sys/devices/platform/charger/bypass_charger"
MTK_BYPASS_CHARGER_ON="1"
MTK_BYPASS_CHARGER_OFF="0"

MTK_CURRENT_CMD="/proc/mtk_battery_cmd/current_cmd"
MTK_CURRENT_CMD_ON="0 1"
MTK_CURRENT_CMD_OFF="0 0"

TRAN_AICHG="/sys/devices/platform/charger/tran_aichg_disable_charger"
TRAN_AICHG_ON="1"
TRAN_AICHG_OFF="0"

MTK_DISABLE_CHARGER="/sys/devices/platform/mt-battery/disable_charger"
MTK_DISABLE_CHARGER_ON="1"
MTK_DISABLE_CHARGER_OFF="0"