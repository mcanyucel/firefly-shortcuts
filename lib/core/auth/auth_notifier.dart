import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../providers/core_providers.dart';
import 'auth_state.dart';

part 'auth_notifier.g.dart';

@Riverpod(keepAlive: true)
class AuthNotifier extends _$AuthNotifier {
  @override
  AuthState build() {
    final initAsync = ref.watch(appInitProvider);
    return initAsync.when(
      data: (_) => ref.read(authManagerProvider).state,
      loading: () => const AuthState.unauthenticated(),
      error: (_, __) => const AuthState.unauthenticated(),
    );
  }

  Future<bool> login() async {
    final success = await ref.read(authManagerProvider).login();
    if (success) {
      state = ref.read(authManagerProvider).state;
    }
    return success;
  }

  Future<void> loginWithPat(String pat) async {
    await ref.read(authManagerProvider).loginWithPat(pat);
    try {
      await ref.read(fireflyApiServiceProvider).testConnection();
      state = ref.read(authManagerProvider).state;
    } catch (e) {
      await ref.read(authManagerProvider).logout();
      rethrow;
    }
  }

  Future<void> logout() async {
    await ref.read(authManagerProvider).logout();
    state = const AuthState.unauthenticated();
  }
}
