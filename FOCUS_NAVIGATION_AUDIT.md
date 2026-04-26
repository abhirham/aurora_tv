# Aurora TV Focus Navigation Audit

This file tracks the recursive D-pad audit requested by the user:

1. Navigate the app and record every focus/navigation issue before fixing.
2. Stop the audit pass only after reaching a navigation roadblock or covering the full UI surface.
3. Batch-fix all open issues from this file.
4. Rebuild, reinstall, and repeat until no focus-navigation issues remain or a blocker is documented.

## Test Environment

- App: `com.codexlabs.auroratv.debug`
- Device: Google TV Streamer via ADB
- Input method: Android TV D-pad key events (`DPAD_UP`, `DPAD_DOWN`, `DPAD_LEFT`, `DPAD_RIGHT`, `DPAD_CENTER`, `BACK`)
- Screenshot folder: `/tmp/aurora-focus-smoke`

## Issue Log

| ID | Status | Surface | Steps | Expected | Actual | Evidence | Fix Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| FN-001 | Fixed, needs regression pass | Top navigation | Fresh launch on Home, press `DPAD_RIGHT`, then `DPAD_CENTER`. | Initial focus should be on the selected Home tab; Right should move to Live TV; Center should open Live TV. | Initial focus was not assigned to Home, so Right jumped to Search and Center opened the search field. | `/tmp/aurora-focus-smoke/rerun-20260426-123205/01_launch.png`, `/tmp/aurora-focus-smoke/rerun-20260426-123205/02_after_right.png`, `/tmp/aurora-focus-smoke/rerun-20260426-123205/03_after_center.png` | Added stable `FocusRequester`s to top-nav items and request focus for the selected section after composition. |
| FN-002 | Fixed, needs regression pass | Live TV EPG grid | From Live TV, press `DPAD_DOWN` into the channel grid, press `DPAD_DOWN`, then press `DPAD_RIGHT`. | Right should move across the focused row's program cells, or remain predictably within the guide if program cells are not actionable. | Focus leaves the guide row and returns to the Live TV top-nav tab because the program cells are not focusable guide targets. | `/tmp/aurora-focus-smoke/rerun-fixed-20260426-123349/04_live_down.png`, `/tmp/aurora-focus-smoke/rerun-fixed-20260426-123349/05_live_down_second.png`, `/tmp/aurora-focus-smoke/rerun-fixed-20260426-123349/06_live_right_into_content.png` | Program cells are now focusable `FocusCard` targets that keep the row context and play the channel on Center. |
| FN-003 | Fixed, needs regression pass | Live TV EPG grid | In Live TV, focus the first visible channel row, then press `DPAD_DOWN`; press `DPAD_UP` afterward. | Channel rows should keep stable visual order while focus moves up/down through the list. | The newly focused channel is moved to the top of the visible guide, so the row list reorders under focus and Up appears stuck instead of reversing the previous move. | `/tmp/aurora-focus-smoke/rerun-fixed-20260426-123349/04_live_down.png`, `/tmp/aurora-focus-smoke/rerun-fixed-20260426-123349/05_live_down_second.png`, `/tmp/aurora-focus-smoke/pass1-20260426-123904/01_live_up_from_epg.png` | The EPG now renders channels in stable provider order instead of moving the focused channel to the first row. |
| FN-004 | Partially fixed | Live TV EPG grid | From the focused channel row in Live TV, press `DPAD_LEFT` and `DPAD_UP`. | Left should return to the category rail; Up should return to the top navigation or prior row when available. | Focus remains trapped on the same channel row, leaving no reliable arrow-key escape from the guide. | `/tmp/aurora-focus-smoke/pass1-20260426-123904/01_live_up_from_epg.png`, `/tmp/aurora-focus-smoke/pass1-20260426-123904/02_live_left_from_epg.png` | Left escape to the rail is fixed and verified in Pass 2. Up escape is still open as FN-005. |
| FN-005 | Paused per user | Live TV EPG grid | In Pass 2, open Live TV, move down into the first guide row, then press `DPAD_UP`. | Up should leave the guide for the hero Play button or top navigation. | Focus remains in the guide area instead of moving upward. | `/tmp/aurora-focus-smoke/pass2-20260426-124411/09_live_up_hero.png` | Paused because the user asked to ignore the Live TV section for now. |
| FN-006 | Verified | Search results | Open Search, move down to the search field, type `Prison`, dismiss keyboard with Back, then press `DPAD_DOWN` / `DPAD_RIGHT`. | Focus should move from the search field into the first results row and then across results. | Focus stays in the search field; visible result cards are not reachable by D-pad from the field. | `/tmp/aurora-focus-smoke/pass4-20260426-130840/01_search_results_before_down.png`, `/tmp/aurora-focus-smoke/pass4-20260426-130840/02_search_results_after_down.png`, `/tmp/aurora-focus-smoke/pass4-20260426-130840/03_search_results_after_right.png` | Added search-field Down routing to the first non-empty result row, result-card Up routing back to the field, and first-result focus requesters. Verified in Pass 4. |
| FN-007 | Reopened | Series dialog | Open a series, wait for seasons/episodes to load, press `DPAD_DOWN` into the first episode row. | The focused episode row should be fully visible and not obscured or clipped by other controls. | The episode list viewport is still too short; focused episode rows are clipped at the bottom while the `Favorite Series` action stays below them. | `/tmp/aurora-focus-smoke/pass4-20260426-130840/16_series_episode_focus.png`, `/tmp/aurora-focus-smoke/pass4-20260426-130840/18_series_down_toward_favorite.png` | Reopened in Pass 4. Need a more compact, fixed-height episode row and/or a larger usable episode viewport. |
| FN-008 | Fixed, needs regression pass | Series dialog | From the first focused episode row, press `DPAD_DOWN`. | Focus should move to the visible `Favorite Series` action if there are no lower episode rows currently reachable, or the episode list should scroll to another fully visible episode. | Focus remains on the clipped episode row while the `Favorite Series` button remains unreachable. | `/tmp/aurora-focus-smoke/pass3-20260426-124813/26_series_dialog_down_from_season2.png`, `/tmp/aurora-focus-smoke/pass3-20260426-124813/27_series_dialog_down_again.png`, `/tmp/aurora-focus-smoke/pass3-20260426-124813/28_series_dialog_up_from_favorite_or_episode.png` | Added first-episode and favorite focus requesters, with the last episode routed down to `Favorite Series`. |
| FN-009 | Verified | Series dialog | From the first focused episode row, press `DPAD_UP`. | Focus should return to the active season chip or the first season chip predictably. | Focus jumps to `Season 2` because Compose geometry chooses the center-nearest chip instead of the selected/active season. | `/tmp/aurora-focus-smoke/pass4-20260426-130840/16_series_episode_focus.png`, `/tmp/aurora-focus-smoke/pass4-20260426-130840/17_series_episode_up_active_season.png` | Added active-season focus requester and first-episode Up routing to the active season chip. Verified in Pass 4. |
| FN-010 | Verified | Settings form | Open Settings, press `DPAD_DOWN` into Provider URL, dismiss keyboard with Back, then press `DPAD_DOWN` twice. | Focus should move through Username, Password, Connect, and settings rows without trapping in the field. | Focus remains trapped in the Provider URL field after the keyboard is dismissed. | `/tmp/aurora-focus-smoke/pass4-20260426-130840/08_settings_password_no_keyboard.png`, `/tmp/aurora-focus-smoke/pass4-20260426-130840/11_settings_provider_down_username.png`, `/tmp/aurora-focus-smoke/pass4-20260426-130840/12_settings_username_down_password.png` | Settings now starts on `Connect And Sync` and text fields intercept D-pad Up/Down to move through the form instead of trapping focus. Verified in Pass 4. |
| FN-011 | Open | Settings form | Open Settings, move focus among the upper provider fields. | The settings title and provider controls should stay compact and readable while focus is brought into view. | The settings title uses oversized display text for an operational panel and can scroll partially under the persistent top navigation while upper fields are focused. | `/tmp/aurora-focus-smoke/pass4-20260426-130840/09_settings_up_username.png`, `/tmp/aurora-focus-smoke/pass4-20260426-130840/10_settings_up_provider.png` | Reduce the Settings title scale and tighten the top layout so focus movement does not make the panel feel zoomed or clipped. |

## Audit Passes

### Pass 1

- Status: In progress
- Started after applying the FN-001 top-nav fix because the issue was found immediately before the requested logging workflow.
- Goal: Navigate every main surface using only D-pad and log all additional issues before making the next batch of fixes.
- FN-002 logged from recovered screenshots created by the interrupted Live TV test.
- FN-003 logged from the same Live TV path after confirming Up does not naturally reverse the Down move because the guide row order changes.
- FN-004 is the Pass 1 roadblock: arrow navigation could not leave the Live TV guide row with Left or Up.
- Batch fix applied for FN-002 through FN-004; build passed with `./gradlew :app:assembleDebug`.

### Pass 2

- Status: Paused for Live TV, continuing elsewhere
- Goal: Regress FN-001 through FN-004 on-device, then continue auditing the remaining UI surfaces.
- FN-001 verified: Home owns launch focus and Right moves to Live TV.
- FN-002 verified: Right from a guide channel row enters a program cell.
- FN-003 verified: guide rows no longer reorder when moving into the first visible row.
- FN-004 partially verified: Left escape to the rail works; Up escape still fails and is tracked as FN-005.
- User asked to ignore Live TV for now and audit the rest of the app.

### Pass 3

- Status: In progress
- Scope: Home, Search, Movies, Series, Settings, and reachable non-Live-TV dialogs/player flows.
- FN-006 logged: Search results are visible but not reachable from the search field via D-pad.
- Movies category/grid/dialog path verified: categories, poster grid, Play/Favorite dialog actions, and Back restoration are navigable.
- FN-007 through FN-009 logged from Series dialog episode/season navigation.
- FN-010 logged as the Pass 3 roadblock: Settings text-field navigation traps D-pad focus.
- Batch fix applied for FN-006 through FN-010; build passed with `./gradlew :app:assembleDebug`.

### Pass 4

- Status: In progress
- Scope: Regression test FN-006 through FN-010, then continue non-Live-TV audit.
- FN-006 verified: Search field Down enters results and Right moves across the result row.
- FN-009 verified: Up from the first focused episode returns to the active season chip.
- FN-010 verified: Settings text fields no longer trap D-pad movement after the keyboard is dismissed.
- FN-007 reopened because focused episode rows are still clipped in the Series dialog.
- FN-011 logged from Settings regression screenshots; the panel title/upper scroll state feels oversized and can clip under the top nav.

## Open Questions / Blockers

- None yet.
