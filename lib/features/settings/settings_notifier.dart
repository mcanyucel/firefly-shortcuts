  import 'package:riverpod_annotation/riverpod_annotation.dart';
  import '../../core/providers/core_providers.dart';
  import 'settings_state.dart';

  part 'settings_notifier.g.dart';

  const _defaultRedirectUrl = 'com.mustafacanyucel.firefly_shortcuts:/auth2redirect';

@riverpod
class SettingsNotifier extends _$SettingsNotifier {
    @override
    Future<SettingsState> build() async {
        final repo = ref.watch(settingsRepositoryProvider);
        final serverUrl = await repo.getServerUrl() ?? '';
        final clientId = await repo.getClientId() ?? '';
        final redirectUrl = await repo.getRedirectUrl() ?? '';
        return SettingsState(
            serverUrl: serverUrl,
            clientId: clientId,
            redirectUrl: redirectUrl
        );
    }

    Future<void> save({
        required String serverUrl,
        required String clientId,
        required String redirectUrl
    }) async {
        final repo = ref.read(settingsRepositoryProvider);
        await repo.saveServerUrl(serverUrl);
        await repo.saveClientId(clientId);
        await repo.saveRedirectUrl(redirectUrl);
        state = AsyncData(SettingsState(
                serverUrl: serverUrl,
                clientId: clientId,
                redirectUrl: redirectUrl
        ));
    }
}
