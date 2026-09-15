#!/bin/bash
set -euo pipefail

if [ $# -lt 1 ]; then
  echo "Usage: normalize-line-endings.sh <file> [file ...]" >&2
  exit 1
fi

for target in "$@"; do
  if [ -f "$target" ]; then
    sed -i 's/\r$//' "$target"
    echo "Normalized $target"
  else
    echo "Skipping $target (not a file)" >&2
  fi
done
