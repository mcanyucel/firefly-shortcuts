  import 'package:flutter/material.dart';
  import 'package:flutter_riverpod/flutter_riverpod.dart';
  import 'package:go_router/go_router.dart';
  import '../../core/auth/auth_notifier.dart';

  class HomeScreen extends ConsumerWidget {
    const HomeScreen({super.key});

    @override
    Widget build(BuildContext context, WidgetRef ref) {
      ref.listen(authProvider, (prev, next) {
        if (!next.isAuthenticated) {
          context.go('/settings');
        }
      });

      return Scaffold(
        appBar: AppBar(
          title: const Text('Firefly Shortcuts'),
          backgroundColor: Theme.of(context).colorScheme.inversePrimary,
          actions: [
            IconButton(
              icon: const Icon(Icons.sync),
              onPressed: () => context.push('/sync'),
            ),
            IconButton(
              icon: const Icon(Icons.settings),
              onPressed: () => context.push('/settings'),
            ),
            IconButton(
              icon: const Icon(Icons.logout),
              onPressed: () => ref.read(authProvider.notifier).logout(),
            ),
          ],
        ),
        body: const Center(
          child: Text('No shortcuts configured yet.'),
        ),
      );
    }
  }