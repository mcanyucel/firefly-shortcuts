  import 'package:flutter/material.dart';
  import 'package:flutter_riverpod/flutter_riverpod.dart';
  import 'core/navigation/app_router.dart';
  import 'core/providers/core_providers.dart';

  void main() {
    WidgetsFlutterBinding.ensureInitialized();
    runApp(const ProviderScope(child: App()));
  }

  class App extends ConsumerWidget {
    const App({super.key});

    @override
    Widget build(BuildContext context, WidgetRef ref) {
      ref.watch(appInitProvider); // fires auth init in the background
      final router = ref.watch(appRouterProvider);
      return MaterialApp.router(
        title: 'Firefly Shortcuts',
        theme: ThemeData(
          colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
          useMaterial3: true,
        ),
        routerConfig: router,
      );
    }
  }