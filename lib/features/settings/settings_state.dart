  class SettingsState {
    final String serverUrl;
    final String clientId;
    final String redirectUrl;
    final String personalAccessToken;

    const SettingsState({
      required this.serverUrl,
      required this.clientId,
      required this.redirectUrl,
      this.personalAccessToken = '',
    });

    SettingsState copyWith({
      String? serverUrl,
      String? clientId,
      String? redirectUrl,
      String? personalAccessToken,
    }) => SettingsState(
      serverUrl: serverUrl ?? this.serverUrl,
      clientId: clientId ?? this.clientId,
      redirectUrl: redirectUrl ?? this.redirectUrl,
      personalAccessToken: personalAccessToken ?? this.personalAccessToken,
    );
  }