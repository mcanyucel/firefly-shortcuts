 import 'package:flutter/material.dart';
  import 'package:flutter_riverpod/flutter_riverpod.dart';
  import 'core/providers/core_providers.dart';

  void main() async {
    WidgetsFlutterBinding.ensureInitialized();
    runApp(const ProviderScope(child: App()));
  }

  class App extends ConsumerWidget {
    const App({super.key});

    @override
    Widget build(BuildContext context, WidgetRef ref) {
      final init = ref.watch(appInitProvider);
      return MaterialApp(
        title: 'Firefly Shortcuts',
        theme: ThemeData(
          colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
          useMaterial3: true,
        ),
        home: init.when(
          data: (_) => const Scaffold(
            body: Center(child: Text('Firefly Shortcuts')),
          ),
          loading: () => const Scaffold(
            body: Center(child: CircularProgressIndicator()),
          ),
          error: (e, _) => Scaffold(
            body: Center(child: Text('Init failed: $e')),
          ),
        ),
      );
    }
  }