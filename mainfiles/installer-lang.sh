#!/system/bin/sh

AZENITH_TRANSLATIONS_DIR="${AZENITH_TRANSLATIONS_DIR:-$MODDIR/translations}"

resolve_locale() {
    local candidate clean_locale short_lang
    candidate="$1"
    [ -n "$candidate" ] || return 1

    clean_locale="$(printf '%s' "$candidate" | tr '_' '-')"

    if [ -f "$AZENITH_TRANSLATIONS_DIR/$clean_locale/strings.sh" ]; then
        lang="$clean_locale"
        return 0
    fi

    case "$clean_locale" in
        zh-CN|zh-Hans-CN|zh-Hans)
            if [ -f "$AZENITH_TRANSLATIONS_DIR/zh-rCN/strings.sh" ]; then
                lang="zh-rCN"
                return 0
            fi
            ;;
        zh-TW|zh-Hant-TW|zh-Hant)
            if [ -f "$AZENITH_TRANSLATIONS_DIR/zh-rTW/strings.sh" ]; then
                lang="zh-rTW"
                return 0
            fi
            ;;
    esac

    short_lang="$(printf '%s' "$clean_locale" | cut -d '-' -f 1 | tr '[:upper:]' '[:lower:]')"
    if [ -f "$AZENITH_TRANSLATIONS_DIR/$short_lang/strings.sh" ]; then
        lang="$short_lang"
        return 0
    fi

    return 1
}

azenith_detect_language() {
    local raw_lang config config_lang config_region
    lang=""

    raw_lang="$(settings get system system_locales 2>/dev/null | cut -d ',' -f 1)"
    resolve_locale "$raw_lang" || :

    if [ -z "$lang" ]; then
        resolve_locale "$(getprop persist.sys.locale 2>/dev/null)" || :
    fi

    if [ -z "$lang" ]; then
        resolve_locale "$(getprop ro.product.locale 2>/dev/null)" || :
    fi

    if [ -z "$lang" ]; then
        config="$(cmd activity get-config 2>/dev/null | grep -m 1 '^config:' | sed 's/^config:[[:space:]]*//' | cut -d ',' -f 1)"
        config_lang="$(printf '%s' "$config" | tr '-' '\n' | grep -m 1 -E '^[a-zA-Z]{2,3}$' | tr '[:upper:]' '[:lower:]')"
        config_region="$(printf '%s' "$config" | tr '-' '\n' | grep -m 1 -E '^r[A-Za-z]{2}$' | cut -c 2- | tr '[:lower:]' '[:upper:]')"
        resolve_locale "${config_lang}${config_region:+-$config_region}" || :
    fi

    printf '%s' "${lang:-en}"
}

AZENITH_LANG="$(azenith_detect_language)"
if [ -f "$AZENITH_TRANSLATIONS_DIR/$AZENITH_LANG/strings.sh" ]; then
    . "$AZENITH_TRANSLATIONS_DIR/$AZENITH_LANG/strings.sh"
elif [ -f "$AZENITH_TRANSLATIONS_DIR/en/strings.sh" ]; then
    AZENITH_LANG="en"
    . "$AZENITH_TRANSLATIONS_DIR/en/strings.sh"
else
    printf '%s\n' "AZenith: English installer translations are missing." >&2
    return 1 2>/dev/null || exit 1
fi

azenith_text() {
    local key
    key="$1"
    shift
    azenith_translate "$key" "$@" && return 0
    printf '%s' "$key"
}

azenith_update_module_description() {
    local module_prop description
    module_prop="$1"
    description="$(azenith_text module_description)"
    sed -i "s/^description=.*/description=$description/" "$module_prop"
}
