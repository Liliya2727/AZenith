azenith_translate() {
    local key
    key="$1"
    shift

    case "$key" in
        apk_missing) printf '[!] APK not found at %s' "$1" ;;
        preparing_apk) printf '%s' '- Preparing APK for installation...' ;;
        installing) printf 'Installing %s...' "$1" ;;
        installing_spinner) printf '[%s] Installing %s...' "$1" "$2" ;;
        install_timeout_stopping) printf '%s' '[!] Installation took too long. Stopping...' ;;
        install_success) printf '[✓] %s installed successfully' "$1" ;;
        install_failed) printf '[!] Failed to install %s' "$1" ;;
        install_timeout) printf '%s' '  Error: Installation failed due to Timeout' ;;
        install_timeout_reason) printf '%s' '  Possible reason: Your ROM is blocking background installation.' ;;
        install_error_log) printf '  Error log: %s' "$1" ;;
        action_install_error_log) printf '  Log: %s' "$1" ;;
        run_module_action) printf '%s' '! Run module Action' ;;
        unzip_module_manual) printf '%s' '! Or unzip the module file, and' ;;
        install_module_manual) printf '%s' '! install it manually' ;;
        install_continuing) printf '%s' '- Continuing module installation...' ;;
        installation_aborted) printf '%s' '! Installation Aborted' ;;
        unsupported_android) printf '%s' '! Unsupported Android Version Detected' ;;
        android_required) printf '%s' '! AZenith requires Android 10 (API 29) or newer.' ;;
        android_current) printf '! Your device is currently running API %s.' "$1" ;;
        corrupted_package) printf '%s' '! The AZenith package appears to be corrupted or incomplete.' ;;
        required_files_missing) printf '%s' '! Required installation files were not found.' ;;
        redownload_module) printf '%s' '! Please re-download the module and try again.' ;;
        unsupported_arch) printf '%s' '! Unsupported CPU Architecture Detected' ;;
        incompatible_arch) printf '%s' '! Your device architecture is not compatible with this build of AZenith.' ;;
        supported_arch) printf '%s' '! Supported architectures:' ;;
        install_complete) printf '%s' '- AZenith has been successfully installed' ;;
        install_thanks) printf '%s' '- Thank you for choosing AZenith!' ;;
        reboot_device) printf '%s' '- Please reboot your device.' ;;
        open_manager_action) printf '%s' '- Open Manager from Action' ;;
        grant_root) printf '%s' "- Don't forget to grant root access." ;;
        installing_azenith) printf '%s' '- Installing AZenith...' ;;
        create_config) printf '%s' '- Create module config' ;;
        extracting_verify) printf '%s' '- Extracting verify.sh' ;;
        extracting_binaries) printf '%s' '- Extracting binaries for %s...' "$1" ;;
        binaries_installed) printf '%s' '- All binaries installed successfully' ;;
        extracting_service) printf '%s' '- Extracting service.sh...' ;;
        extracting_post_fs_data) printf '%s' '- Extracting post-fs-data.sh...' ;;
        extracting_action) printf '%s' '- Extracting action.sh...' ;;
        extracting_cleanup) printf '%s' '- Extracting cleanup.sh...' ;;
        extracting_module_prop) printf '%s' '- Extracting module.prop...' ;;
        extracting_uninstall) printf '%s' '- Extracting uninstall.sh...' ;;
        extracting_applist) printf '%s' '- Extracting Applist.json...' ;;
        extracting_banner) printf '%s' '- Extracting module banner...' ;;
        extracting_language) printf '%s' '- Extracting installer language data...' ;;
        hybrid_detected) printf '%s' '- Hybrid Mount detected, configuring rules...' ;;
        hybrid_cli_found) printf '%s' '- Found Hybrid Mount CLI at: %s' "$1" ;;
        hybrid_runtime_updated) printf '%s' "- Runtime policy for AZenith updated to 'ignore'." ;;
        hybrid_cli_missing) printf '%s' '- Warning: CLI binary not found in standard paths. Skipping live patch.' ;;
        hybrid_rule_added) printf '%s' '- Permanent rule added to config.toml.' ;;
        hybrid_not_installed) printf '%s' '- Hybrid Mount is not installed. Skipping configuration.' ;;
        ksu_skip_mount) printf '%s' '- KSU/AP detected, skipping module mount (skip_mount)' ;;
        creating_symlink) printf '%s' '- Creating symlink in %s' "$1" ;;
        checking_soc) printf '%s' '- Checking device SoC' ;;
        applying_tweaks) printf '%s' '- Applying tweaks for %s' "$1" ;;
        enabling_auto) printf '%s' '- Enabling Auto Mode' ;;
        disabling_debug) printf '%s' '- Disable Debug mode' ;;
        setting_properties) printf '%s' '- Setting config properties...' ;;
        prepare_apk_failed) printf '%s' '[!] Failed to prepare APK. Continuing without installing manager...' ;;
        enabling_launcher) printf '%s' '- Enabling Launcher...' ;;
        setting_permissions) printf '%s' '- Setting Permissions...' ;;
        permission_setup_skipped) printf '%s' '[!] Permission setup skipped: service binary missing' ;;
        cleaning_old_files) printf '%s' '- Cleaning old files...' ;;
        uninstalling_old_components) printf '%s' '- Uninstalling old components' ;;
        launching_manager) printf '%s' '- Launching manager...' ;;
        module_action_apk_missing) printf '%s' '[!] APK not found in module' ;;
        module_action_failed) printf '%s' '[!] Failed to install AZenith Manager' ;;
        module_action_install_manually) printf '%s' '  Please install AZenith.apk manually.' ;;
        module_action_app_missing) printf '%s' '[*] App not detected. Preparing installation...' ;;
        module_action_launching_manager) printf '%s' '[*] Launching AZenith Manager...' ;;
        module_action_binary_missing) printf '[!] Service binary not found at %s' "$1" ;;
        verified_file) printf '%s' '- Verified file' ;;
        checksum_missing) printf '%s' 'Missing checksum for installation file.' ;;
        checksum_mismatch) printf '%s' 'Checksum mismatch for file.' ;;
        file_missing) printf 'File not found: %s' "$1" ;;
        update_binary_missing) printf '%s' 'Installer file does not exist.' ;;
        download_from_magisk) printf '%s' '- Install from Magisk app' ;;
        abort_corrupted) printf '%s' '! Installation aborted. The module may be corrupted.' ;;
        abort_redownload) printf '%s' '! Please re-download and try again.' ;;
        module_description) printf '%s' 'One, Two, Three! AZenith has arrived!' ;;
        anti_bootloop_description) printf '%s' 'Anti-bootloop triggered. Module disabled; enable it to activate.' ;;
        *) return 1 ;;
    esac
}
