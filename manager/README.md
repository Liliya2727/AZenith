# System Monitor

Monitors system info for performance tweaks as substitute of `dumpsys`.

## Why System Monitor?

- Fast: Polling system information in milliseconds, closer to real-time data.
- Stable: Interacting with Android system natively via Java.
- Simple: Just run and poll/inotify the file output.

## And dumpsys?

- Slow: Polling limited up to seconds, limiting responsivity on your tweak.
- Complicated: Requires custom parsing which can be complex and slow.
- Bloated: `dumpsys` dumps everything, even ones that you didn't need.

## Usage

```shell
app_process -Djava.class.path=/sdcard/app-release.apk / --nice-name=EncoreSysMon com.rem01gaming.systemmonitor.MainKt /path/to/output/file
```

### Output file, updated each changes

```text
focused_app com.rhmsoft.edit.pro 4720 10292
screen_awake 1
battery_saver 0
zen_mode 0
```

## Lisense

    Copyright 2026 Rem01Gaming

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
