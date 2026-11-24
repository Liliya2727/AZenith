# Project
Fork AZenith (Apache-2.0) + ramd (shell daemon from Transsion Optimizer, in src/ramd).

## Rules
- ramd stays shell. Do not rewrite in C.
- ramd is read-only toward zram/vm (owner: separate ZRAM module). Do not add zram writes.
- Phase 1: keep ramd's own game detection. Phase 2: hook to AZenith profile after audit.
- Keep copyright headers; add "Modified by" note on edited files.
- Never invent sysfs paths/props/package names. Verify or guard with [ -e path ].
- POSIX sh only (busybox ash). WebUI: single-file HTML/CSS/JS, no CDN.
- Show a plan before editing. Small commits.
