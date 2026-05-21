import 'package:riverpod_annotation/riverpod_annotation.dart';
  import '../../core/providers/core_providers.dart';
  import 'settings_state.dart';

  part 'settings_notifier.g.dart';

  const _defaultRedirectUrl =
      'https://fireflyiiishortcuts.mustafacanyucel.com/oauth2redirect.html';

  @riverpod
  class SettingsNotifier extends _$SettingsNotifier {
    @override
    Future<SettingsState> build() async {
      final repo = ref.watch(settingsRepositoryProvider);
      final serverUrl = await repo.getServerUrl() ?? '';
      final clientId = await repo.getClientId() ?? '';
      final redirectUrl = await repo.getRedirectUrl() ?? _defaultRedirectUrl;
      final personalAccessToken = await repo.getPersonalAccessToken() ?? '';
      return SettingsState(
        serverUrl: serverUrl,
        clientId: clientId,
        redirectUrl: redirectUrl,
        personalAccessToken: personalAccessToken,
      );
    }

    Future<void> save({
      required String serverUrl,
      required String clientId,
      required String redirectUrl,
      required String personalAccessToken,
    }) async {
      final repo = ref.read(settingsRepositoryProvider);
      await repo.saveServerUrl(serverUrl);
      await repo.saveClientId(clientId);
      await repo.saveRedirectUrl(redirectUrl);
      await repo.savePersonalAccessToken(personalAccessToken);
      state = AsyncData(SettingsState(
        serverUrl: serverUrl,
        clientId: clientId,
        redirectUrl: redirectUrl,
        personalAccessToken: personalAccessToken,
      ));
    }
  }