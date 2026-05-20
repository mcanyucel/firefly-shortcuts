class SettingsState {
    final String serverUrl;
    final String clientId;
    final String redirectUrl;

    const SettingsState({
        required this.serverUrl,
        required this.clientId,
        required this.redirectUrl
    });

    SettingsState copyWith({
        String? serverUrl,
        String? clientId,
        String? redirectUrl
    }) => SettingsState(
        serverUrl: serverUrl ?? this.serverUrl,
        clientId: clientId ?? this.clientId,
        redirectUrl: redirectUrl ?? this.redirectUrl
    );
}
