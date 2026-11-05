#!/bin/sh
# Renders the Play Store graphics with headless Microsoft Edge (or Chrome via BROWSER=...).
# Run from this folder in Git Bash: sh render.sh
set -e
cd "$(dirname "$0")"
BROWSER="${BROWSER:-/c/Program Files (x86)/Microsoft/Edge/Application/msedge.exe}"
dir="$(pwd -W 2>/dev/null || pwd)"

shot() { # page width height output
  "$BROWSER" --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=1 \
    --default-background-color=00000000 --window-size="$2,$3" \
    --screenshot="$dir/$4" "file:///$dir/$1"
}

shot icon.html 512 512 icon-512.png
# Play wants the icon as a 32-bit PNG; the browser writes RGB.
python png_add_alpha.py icon-512.png icon-512.png
shot feature.html 1024 500 feature-1024x500.png
echo "Rendered icon-512.png and feature-1024x500.png"
