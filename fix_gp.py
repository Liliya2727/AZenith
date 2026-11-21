from pathlib import Path

p = Path('/data/data/com.termux/files/home/AZenith/manager/app/src/main/java/zx/azenith/ui/component/GamepadFocus.kt')
s = p.read_text()
old = '''                if (delta != 0f) {
                    GamepadInput.onDpadKey()
                    val current = GamepadShortcuts.actions
                    current?.onScrollDelta?.invoke(delta)
                    current?.scrollState?.let { runCatching { it.scrollBy(delta) } }
                    current?.shiftableScrollState?.let { runCatching { it.scrollBy(delta) } }
                }'''
new = '''                if (delta != 0f) {
                    GamepadInput.onDpadKey()
                    val current = GamepadShortcuts.actions
                    current?.onScrollDelta?.invoke(delta)
                    try { current?.scrollState?.scrollBy(delta) } catch (_: Throwable) {}
                    try { current?.shiftableScrollState?.scrollBy(delta) } catch (_: Throwable) {}
                }'''
if old not in s:
    print('NOT FOUND')
else:
    s = s.replace(old, new, 1)
    p.write_text(s)
    print('OK')
