#!/usr/bin/env bash
set -euo pipefail

root="${2:-data/place-images}"
source_image="${1:-$root/thumbnail_ex_1.jpg}"
sanitized_image="$root/thumbnail_ex_1_sanitized.jpg"

if ! command -v jpegtran >/dev/null; then
    echo "jpegtran is required to remove EXIF metadata." >&2
    exit 1
fi
if [[ ! -f "$source_image" ]]; then
    echo "Source image not found: $source_image" >&2
    exit 1
fi

mkdir -p "$root"
jpegtran -copy none -optimize -outfile "$sanitized_image" "$source_image"

for number in {1..100}; do
    (( number % 5 == 0 )) && continue
    printf -v storage_key '00000000-0000-4000-8000-%012d.jpg' "$number"
    target="$root/$storage_key"
    if [[ -e "$target" ]]; then
        cmp -s "$sanitized_image" "$target" || {
            echo "Existing dummy image differs: $target" >&2
            exit 1
        }
    else
        ln "$sanitized_image" "$target"
    fi
done

echo "Prepared 80 dummy place thumbnails in $root"
