#!/bin/env bash

msg="*$TITLE*
\\#ci\\_$VERSION
\`\`\`
$COMMIT_MESSAGE
\`\`\`
[Commit]($COMMIT_URL)
[Workflow run]($RUN_URL)
"

file="$1"
thumbnail="$GITHUB_WORKSPACE/logo.webp"

if [ ! -f "$file" ]; then
    echo "error: File not found" >&2
    exit 1
fi
filename=$(basename -- "$file")
safe_name="${filename/-release/.rel}"

if [ "$safe_name" != "$filename" ]; then
    cp "$file" "/tmp/$safe_name"
    file="/tmp/$safe_name"
    filename="$safe_name"
fi

curl -s -X POST "https://api.telegram.org/bot$BOT_TOKEN/sendDocument" \
    -F "chat_id=$CHAT_ID" \
    -F "document=@$file;filename=$filename" \
    -F "thumb=@$thumbnail" \
    -F "caption=$msg" \
    -F "parse_mode=MarkdownV2" \
    -F "disable_web_page_preview=true"
    