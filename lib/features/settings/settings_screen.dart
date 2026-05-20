import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/auth/auth_notifier.dart';
import '../../core/auth/auth_state.dart';
import 'settings_notifier.dart';
import 'settings_state.dart';

class SettingsScreen extends ConsumerStatefulWidget {
  const SettingsScreen({super.key});

  @override
  ConsumerState<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends ConsumerState<SettingsScreen> {
  late final TextEditingController _serverUrlController;
  late final TextEditingController _clientIdController;
  late final TextEditingController _redirectUrlController;
  bool _isConnecting = false;

  @override
  void initState() {
    super.initState();
    _serverUrlController = TextEditingController();
    _clientIdController = TextEditingController();
    _redirectUrlController = TextEditingController();
  }

  @override
  void dispose() {
    _serverUrlController.dispose();
    _clientIdController.dispose();
    _redirectUrlController.dispose();
    super.dispose();
  }

  void _populateControllers(SettingsState state) {
    _serverUrlController.text = state.serverUrl;
    _clientIdController.text = state.clientId;
    _redirectUrlController.text = state.redirectUrl;
  }

  Future<void> _save() async {
    try {
      await ref
          .read(settingsProvider.notifier)
          .save(
            serverUrl: _serverUrlController.text.trim(),
            clientId: _clientIdController.text.trim(),
            redirectUrl: _redirectUrlController.text.trim(),
          );
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('Settings saved')));
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Failed to save: $e')));
      }
    }
  }

  Future<void> _connect() async {
    setState(() => _isConnecting = true);
    try {
      final success = await ref.read(authProvider.notifier).login();
      if (!success && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Login failed or was cancelled')),
        );
      }
    } finally {
      if (mounted) setState(() => _isConnecting = false);
    }
  }

  Future<void> _disconnect() async {
    await ref.read(authProvider.notifier).logout();
  }

  @override
  Widget build(BuildContext context) {
    ref.listen(settingsProvider, (previous, next) {
      if (previous?.value == null && next.value != null) {
        _populateControllers(next.value!);
      }
    });

    // Navigate to home when authentication succeeds
    ref.listen(authProvider, (prev, next) {
      if (next.isAuthenticated && !(prev?.isAuthenticated ?? false)) {
        context.go('/home');
      }
    });

    final settingsAsync = ref.watch(settingsProvider);
    final authState = ref.watch(authProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Settings'),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
      ),
      body: settingsAsync.when(
        data: (_) => _buildBody(context, authState),
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('Error loading settings: $e')),
      ),
    );
  }

  Widget _buildBody(BuildContext context, AuthState authState) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            'Firefly III Connection',
            style: Theme.of(context).textTheme.titleMedium,
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _serverUrlController,
            decoration: const InputDecoration(
              labelText: 'Server URL',
              hintText: 'https://your-firefly.example.com',
              border: OutlineInputBorder(),
            ),
            keyboardType: TextInputType.url,
            autocorrect: false,
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _clientIdController,
            decoration: const InputDecoration(
              labelText: 'Client ID',
              hintText: 'Numeric OAuth2 client ID from Firefly III',
              border: OutlineInputBorder(),
            ),
            keyboardType: TextInputType.number,
            autocorrect: false,
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _redirectUrlController,
            decoration: const InputDecoration(
              labelText: 'Redirect URL',
              hintText: 'com.mustafacanyucel.firefly_shortcuts:/oauth2redirect',
              border: OutlineInputBorder(),
            ),
            autocorrect: false,
          ),
          const SizedBox(height: 24),
          FilledButton(onPressed: _save, child: const Text('Save')),
          const SizedBox(height: 32),
          const Divider(),
          const SizedBox(height: 16),
          _buildAuthSection(context, authState),
        ],
      ),
    );
  }

  Widget _buildAuthSection(BuildContext context, AuthState authState) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text('Authentication', style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 12),
        Row(
          children: [
            Icon(
              authState.isAuthenticated ? Icons.check_circle : Icons.cancel,
              color: authState.isAuthenticated ? Colors.green : Colors.red,
              size: 16,
            ),
            const SizedBox(width: 8),
            Text(
              authState.isAuthenticated ? 'Connected' : 'Not connected',
              style: Theme.of(context).textTheme.bodyMedium,
            ),
          ],
        ),
        const SizedBox(height: 16),
        if (authState.isAuthenticated)
          OutlinedButton(
            onPressed: _disconnect,
            child: const Text('Disconnect'),
          )
        else
          FilledButton(
            onPressed: _isConnecting ? null : _connect,
            child: _isConnecting
                ? const SizedBox(
                    height: 20,
                    width: 20,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Text('Connect to Firefly III'),
          ),
      ],
    );
  }
}
