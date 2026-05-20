class AuthState{
  final String? accessToken;
  final String? refreshToken;
  final DateTime? accessTokenExpiry;

  AuthState({
    this.accessToken,
    this.refreshToken,
    this.accessTokenExpiry,
  });

  const AuthState.unauthenticated()
      : accessToken = null,
        refreshToken = null,
        accessTokenExpiry = null;

  bool get isAuthenticated =>
      accessToken != null && refreshToken != null;

      bool get isAccessTokenExpired =>
      accessTokenExpiry != null && DateTime.now().isAfter(accessTokenExpiry!.subtract(const Duration(minutes: 2)));
}