#!/usr/bin/env bash
# Captures README screenshots of the sample app on a running emulator.
# Drags and typing can't be timed reliably through adb, so the sample opens each screen from the `scene` extra with
# the sheet already settled at its detent (it starts there, without an animation), so every capture is the same.
# The `keyboard` scene focuses the comment field and asks for the real system keyboard; the capture waits until the
# system reports the keyboard as shown (and tapping the field once more if it is not), and fails otherwise.
# Every capture is checked for the expected text and for a blank image.
#
#   bash scripts/screenshots.sh tablet   # pixel_tablet in landscape: the places list as a side sheet
#   bash scripts/screenshots.sh phone    # pixel_7: the places sheet at peek, half and full, the comment sheet
set -euo pipefail
source "$(dirname "$0")/screenshot-lib.sh"

device="${1:-phone}"

# The text each scene must show; the capture fails without it.
# "Show map" only appears at the full detent, "Post comment" only with the comment sheet open.
expected_text() {
  case "$1" in
    peek) echo "Coffee near Harbor Square" ;;
    half) echo "Lighthouse Roasters" ;;
    full) echo "Show map" ;;
    keyboard) echo "Post comment" ;;
    tablet) echo "Lighthouse Roasters" ;;
  esac
}

install_sample
set_night_mode light
if [ "$device" = tablet ]; then
  ensure_landscape
  fresh_launch --es scene tablet
  capture "tablet-side-sheet" "$(expected_text tablet)"
else
  for scene in peek half full; do
    fresh_launch --es scene "$scene"
    capture "phone-$scene" "$(expected_text "$scene")"
  done
  # The emulator has a hardware keyboard, which hides the soft keyboard unless this is on.
  adb shell settings put secure show_ime_with_hard_keyboard 1
  fresh_launch --es scene keyboard
  wait_for_ime
  capture "phone-keyboard" "$(expected_text keyboard)"
fi
