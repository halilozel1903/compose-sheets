#!/usr/bin/env bash
# Captures README screenshots of the sample app on a running emulator.
# Drags and typing can't be timed reliably through adb, so the sample opens each screen from the `scene` extra with
# the sheet already settled at its detent (it starts there, without an animation), so every capture is the same.
# The system keyboard can't be opened at a reliable moment on CI either: the `keyboard` scene draws a keyboard and
# gives the comment sheet the insets a real keyboard of that height reports, so the sheet lays out exactly as it does
# above a real one.
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
  for scene in peek half full keyboard; do
    fresh_launch --es scene "$scene"
    capture "phone-$scene" "$(expected_text "$scene")"
  done
fi
