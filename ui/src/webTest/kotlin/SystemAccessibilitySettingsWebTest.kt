package top.ltfan.m3x.compose.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.browser.document
import kotlinx.browser.window
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.compose.ui.accessibility.SystemAccessibilitySettings
import top.ltfan.m3x.compose.ui.accessibility.rememberSystemAccessibilitySettings

class SystemAccessibilitySettingsWebTest {
    @Test
    fun mediaQueryChangesRecomposeAndDisposeTheirListeners() = runInitializedComposeUiTest {
        val script = document.createElement("script")
        script.textContent =
            """
            (function() {
              const original = window.matchMedia;
              const state = { transparency: false, contrast: false, motion: false };
              const queries = new Map();
              const staleCallbacks = [];
              const controlHandlers = [];
              let listenerCount = 0;
              function onControl(name, listener) {
                window.addEventListener(name, listener);
                controlHandlers.push([name, listener]);
              }
              function key(query) {
                if (query.indexOf('prefers-reduced-transparency') >= 0) return 'transparency';
                if (query.indexOf('prefers-contrast') >= 0) return 'contrast';
                if (query.indexOf('prefers-reduced-motion') >= 0) return 'motion';
                return null;
              }
              window.matchMedia = function(query) {
                const name = key(query);
                if (name === null) return original.call(window, query);
                if (!queries.has(query)) {
                  const listeners = new Set();
                  const item = original.call(window, query);
                  Object.defineProperty(item, 'matches', { value: state[name], writable: true, configurable: true });
                  Object.assign(item, {
                    addEventListener(type, listener) {
                      if (type === 'change') {
                        if (!listeners.has(listener)) {
                          listeners.add(listener);
                          staleCallbacks.push([item, listener]);
                          listenerCount += 1;
                          document.documentElement.setAttribute('data-m3x-listeners', String(listenerCount));
                        }
                      }
                    },
                    removeEventListener(type, listener) {
                      if (type === 'change' && listeners.delete(listener)) {
                        listenerCount -= 1;
                        document.documentElement.setAttribute('data-m3x-listeners', String(listenerCount));
                      }
                    },
                    update() {
                      this.matches = state[name];
                      const event = new Event('change');
                      listeners.forEach(listener => listener.call(this, event));
                    }
                  });
                  queries.set(query, item);
                }
                return queries.get(query);
              };
              [
                ['m3x-transparency-on', 'transparency', true],
                ['m3x-transparency-off', 'transparency', false],
                ['m3x-contrast-on', 'contrast', true],
                ['m3x-motion-on', 'motion', true]
              ].forEach(([eventName, name, value]) => onControl(eventName, () => {
                state[name] = value;
                queries.forEach(item => item.update());
              }));
              onControl('m3x-run-stale', () => {
                staleCallbacks.forEach(([target, listener]) => listener.call(target, new Event('change')));
              });
              onControl('m3x-accessibility-cleanup', () => {
                window.matchMedia = original;
                controlHandlers.forEach(([name, listener]) => window.removeEventListener(name, listener));
                document.documentElement.removeAttribute('data-m3x-listeners');
                delete window.__m3xAccessibilityCount;
              });
              window.__m3xAccessibilityCount = () => listenerCount;
            })();
            """
                .trimIndent()
        document.head?.appendChild(script)
        try {
            var active by mutableStateOf(true)
            var observed = SystemAccessibilitySettings()
            setContent {
                if (active) observed = rememberSystemAccessibilitySettings()
            }
            waitForIdle()
            assertEquals(
                SystemAccessibilitySettings(
                    reduceTransparency = false,
                    increaseContrast = false,
                    reduceMotion = false,
                ),
                observed,
            )
            dispatchAccessibilityEvent("m3x-transparency-on")
            waitForIdle()
            assertEquals(
                SystemAccessibilitySettings(
                    reduceTransparency = true,
                    increaseContrast = false,
                    reduceMotion = false,
                ),
                observed,
            )
            dispatchAccessibilityEvent("m3x-contrast-on")
            dispatchAccessibilityEvent("m3x-motion-on")
            waitForIdle()
            assertEquals(
                SystemAccessibilitySettings(
                    reduceTransparency = true,
                    increaseContrast = true,
                    reduceMotion = true,
                ),
                observed,
            )

            runOnIdle { active = false }
            waitForIdle()
            assertEquals(0, listenerCount())
            dispatchAccessibilityEvent("m3x-transparency-off")
            dispatchAccessibilityEvent("m3x-run-stale")
            waitForIdle()
            assertEquals(
                SystemAccessibilitySettings(
                    reduceTransparency = true,
                    increaseContrast = true,
                    reduceMotion = true,
                ),
                observed,
            )
            assertEquals(0, listenerCount())
        } finally {
            dispatchAccessibilityCleanup()
            script.remove()
        }
    }

    private fun dispatchAccessibilityEvent(name: String) {
        val event =
            document.createEvent("Event").apply {
                initEvent(name, bubbles = false, cancelable = false)
            }
        window.dispatchEvent(event)
    }

    private fun dispatchAccessibilityCleanup() =
        dispatchAccessibilityEvent("m3x-accessibility-cleanup")

    private fun listenerCount(): Int =
        document.documentElement?.getAttribute("data-m3x-listeners")?.toIntOrNull() ?: 0
}
