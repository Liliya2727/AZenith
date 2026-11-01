azenith_translate() {
    local key
    key="$1"
    shift

    case "$key" in
        apk_missing) printf '[!] APK не найден: %s' "$1" ;;
        preparing_apk) printf '%s' '- Подготовка APK к установке...' ;;
        installing) printf 'Установка %s...' "$1" ;;
        installing_spinner) printf '[%s] Установка %s...' "$1" "$2" ;;
        install_timeout_stopping) printf '%s' '[!] Установка выполняется слишком долго. Остановка...' ;;
        install_success) printf '[✓] %s успешно установлен' "$1" ;;
        install_failed) printf '[!] Не удалось установить %s' "$1" ;;
        install_timeout) printf '%s' '  Ошибка: истекло время ожидания установки' ;;
        install_timeout_reason) printf '%s' '  Возможно, прошивка блокирует установку в фоне.' ;;
        install_error_log) printf '  Журнал ошибки: %s' "$1" ;;
        action_install_error_log) printf '  Журнал: %s' "$1" ;;
        run_module_action) printf '%s' '! Запустите действие модуля' ;;
        unzip_module_manual) printf '%s' '! Или распакуйте ZIP модуля и' ;;
        install_module_manual) printf '%s' '! установите его вручную' ;;
        install_continuing) printf '%s' '- Продолжение установки модуля...' ;;
        installation_aborted) printf '%s' '! Установка отменена' ;;
        unsupported_android) printf '%s' '! Версия Android не поддерживается' ;;
        android_required) printf '%s' '! Для AZenith требуется Android 10 (API 29) или новее.' ;;
        android_current) printf '! На устройстве используется API %s.' "$1" ;;
        corrupted_package) printf '%s' '! ZIP-пакет AZenith повреждён или неполон.' ;;
        required_files_missing) printf '%s' '! Не найдены необходимые файлы установки.' ;;
        redownload_module) printf '%s' '! Скачайте модуль заново и повторите попытку.' ;;
        unsupported_arch) printf '%s' '! Архитектура процессора не поддерживается' ;;
        incompatible_arch) printf '%s' '! Эта сборка AZenith несовместима с архитектурой устройства.' ;;
        supported_arch) printf '%s' '! Поддерживаемые архитектуры:' ;;
        install_complete) printf '%s' '- AZenith успешно установлен' ;;
        install_thanks) printf '%s' '- Спасибо, что выбрали AZenith!' ;;
        reboot_device) printf '%s' '- Перезагрузите устройство.' ;;
        open_manager_action) printf '%s' '- Откройте менеджер через раздел «Действие»' ;;
        grant_root) printf '%s' '- Не забудьте предоставить root-доступ.' ;;
        installing_azenith) printf '%s' '- Установка AZenith...' ;;
        create_config) printf '%s' '- Создание конфигурации модуля' ;;
        extracting_verify) printf '%s' '- Извлечение verify.sh' ;;
        extracting_binaries) printf '- Извлечение бинарных файлов для %s...' "$1" ;;
        binaries_installed) printf '%s' '- Все бинарные файлы установлены' ;;
        extracting_service) printf '%s' '- Извлечение service.sh...' ;;
        extracting_post_fs_data) printf '%s' '- Извлечение post-fs-data.sh...' ;;
        extracting_action) printf '%s' '- Извлечение action.sh...' ;;
        extracting_cleanup) printf '%s' '- Извлечение cleanup.sh...' ;;
        extracting_module_prop) printf '%s' '- Извлечение module.prop...' ;;
        extracting_uninstall) printf '%s' '- Извлечение uninstall.sh...' ;;
        extracting_applist) printf '%s' '- Извлечение списка приложений...' ;;
        extracting_banner) printf '%s' '- Извлечение баннера модуля...' ;;
        extracting_language) printf '%s' '- Извлечение языковых данных...' ;;
        hybrid_detected) printf '%s' '- Обнаружен Hybrid Mount, настройка правил...' ;;
        hybrid_cli_found) printf '- Найден CLI Hybrid Mount: %s' "$1" ;;
        hybrid_runtime_updated) printf '%s' "- Политика AZenith изменена на 'ignore'." ;;
        hybrid_cli_missing) printf '%s' '- В стандартных каталогах не найден CLI. Пропуск live-патча.' ;;
        hybrid_rule_added) printf '%s' '- Постоянное правило добавлено в config.toml.' ;;
        hybrid_not_installed) printf '%s' '- Hybrid Mount не установлен. Пропуск настройки.' ;;
        ksu_skip_mount) printf '%s' '- Обнаружен KSU/APatch, монтирование пропущено (skip_mount)' ;;
        creating_symlink) printf '- Создание символической ссылки в %s' "$1" ;;
        checking_soc) printf '%s' '- Проверка SoC устройства' ;;
        applying_tweaks) printf '- Применение настроек для %s' "$1" ;;
        enabling_auto) printf '%s' '- Включение автоматического режима' ;;
        disabling_debug) printf '%s' '- Отключение режима отладки' ;;
        setting_properties) printf '%s' '- Настройка системных свойств...' ;;
        prepare_apk_failed) printf '%s' '[!] Не удалось подготовить APK. Установка менеджера пропущена...' ;;
        enabling_launcher) printf '%s' '- Включение лаунчера...' ;;
        setting_permissions) printf '%s' '- Настройка разрешений...' ;;
        permission_setup_skipped) printf '%s' '[!] Настройка разрешений пропущена: бинарный файл службы отсутствует' ;;
        cleaning_old_files) printf '%s' '- Очистка старых файлов...' ;;
        uninstalling_old_components) printf '%s' '- Удаление старых компонентов' ;;
        launching_manager) printf '%s' '- Запуск менеджера...' ;;
        module_action_apk_missing) printf '%s' '[!] APK не найден в модуле' ;;
        module_action_failed) printf '%s' '[!] Не удалось установить менеджер AZenith' ;;
        module_action_install_manually) printf '%s' '  Установите AZenith.apk вручную.' ;;
        module_action_app_missing) printf '%s' '[*] Приложение не найдено. Подготовка к установке...' ;;
        module_action_launching_manager) printf '%s' '[*] Запуск менеджера AZenith...' ;;
        module_action_binary_missing) printf '[!] Бинарный файл службы не найден: %s' "$1" ;;
        verified_file) printf '%s' '- Файл проверен' ;;
        checksum_missing) printf '%s' 'Отсутствует контрольная сумма файла установки.' ;;
        checksum_mismatch) printf '%s' 'Контрольная сумма файла не совпадает.' ;;
        file_missing) printf 'Файл не найден: %s' "$1" ;;
        update_binary_missing) printf '%s' 'Файл установщика отсутствует.' ;;
        download_from_magisk) printf '%s' '- Установлено из приложения Magisk' ;;
        abort_corrupted) printf '%s' '! Установка отменена. Возможно, модуль повреждён.' ;;
        abort_redownload) printf '%s' '! Скачайте модуль заново и повторите попытку.' ;;
        module_description) printf '%s' 'Раз, два, три! AZenith уже здесь!' ;;
        anti_bootloop_description) printf '%s' 'Защита от цикла загрузки сработала: модуль отключён. Включите его, чтобы продолжить.' ;;
        *) return 1 ;;
    esac
}
