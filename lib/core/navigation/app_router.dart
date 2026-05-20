  import 'package:firefly_shortcuts/features/home/home_screen.dart';
import 'package:go_router/go_router.dart';
  import 'package:riverpod_annotation/riverpod_annotation.dart';
  import 'package:flutter_riverpod/flutter_riverpod.dart';
  import '../../features/settings/settings_screen.dart';

  part 'app_router.g.dart';

  @Riverpod(keepAlive: true)
  GoRouter appRouter(Ref ref) {
    return GoRouter(
      initialLocation: '/settings',
      routes: [
        GoRoute(
          path: '/settings',
          builder: (context, state) => const SettingsScreen(),
        ),
        GoRoute(
          path: '/home',
          builder: (context, state) => const HomeScreen()
        )
      ],
    );
  }