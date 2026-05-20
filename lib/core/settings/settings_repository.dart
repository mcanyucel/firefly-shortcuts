import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class SettingsRepository {
  final FlutterSecureStorage _storage;

  static const _keyServerUrl = 'server_url';
  static const _keyClientId = 'client_id';
  static const _keyRedirectUrl = 'redirect_url';

  SettingsRepository(this._storage);

  Future<String?> getServerUrl() => _storage.read(key: _keyServerUrl);
  Future<String?> getClientId() => _storage.read(key: _keyClientId);
  Future<String?> getRedirectUrl() => _storage.read(key: _keyRedirectUrl);

  Future<void> saveServerUrl(String url) {
    final trimmed = url.endsWith('/') ? url.substring(0, url.length - 1) : url;
    return _storage.write(key: _keyServerUrl, value: trimmed);
  }

  Future<void> saveClientId(String id) =>
      _storage.write(key: _keyClientId, value: id);

  Future<void> saveRedirectUrl(String url) =>
      _storage.write(key: _keyRedirectUrl, value: url);

  Future<bool> isConfigured() async {
    final url = await getServerUrl();
    final clientId = await getClientId();
    return url != null && url.isNotEmpty && clientId != null && clientId.isNotEmpty;
  }
}