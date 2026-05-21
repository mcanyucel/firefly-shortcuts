  import 'package:flutter/material.dart';
  import 'package:flutter_riverpod/flutter_riverpod.dart';
  import 'sync_notifier.dart';
  import 'sync_state.dart';

  class SyncScreen extends ConsumerWidget {
    const SyncScreen({super.key});

    @override
    Widget build(BuildContext context, WidgetRef ref) {
      final syncState = ref.watch(syncProvider);

      return Scaffold(
        appBar: AppBar(
          title: const Text('Sync'),
          backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        ),
        body: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                syncState.lastSyncTime == null
                    ? 'Never synced'
                    : 'Last synced: ${_formatTime(syncState.lastSyncTime!)}',
                style: Theme.of(context).textTheme.bodyMedium,
              ),
              const SizedBox(height: 16),
              FilledButton.icon(
                onPressed: syncState.isSyncing
                    ? null
                    : () => ref.read(syncProvider.notifier).syncAll(),
                icon: syncState.isSyncing
                    ? const SizedBox(
                        width: 16,
                        height: 16,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.sync),
                label: Text(syncState.isSyncing ? 'Syncing...' : 'Sync All'),
              ),
              const SizedBox(height: 24),
              const Divider(),
              const SizedBox(height: 8),
              _buildRow(context, 'Accounts', syncState.accounts),
              _buildRow(context, 'Bills', syncState.bills),
              _buildRow(context, 'Budgets', syncState.budgets),
              _buildRow(context, 'Categories', syncState.categories),
              _buildRow(context, 'Piggy Banks', syncState.piggybanks),
              _buildRow(context, 'Tags', syncState.tags),
            ],
          ),
        ),
      );
    }

    Widget _buildRow(
      BuildContext context,
      String label,
      EntitySyncState entity,
    ) {
      final (icon, color, detail) = switch (entity.status) {
        SyncStatus.idle => (Icons.remove, Colors.grey, ''),
        SyncStatus.syncing => (Icons.hourglass_top, Colors.blue, 'Syncing...'),
        SyncStatus.done => (
            Icons.check_circle,
            Colors.green,
            '${entity.count} items',
          ),
        SyncStatus.error => (Icons.error, Colors.red, entity.errorMessage ?? ''),
      };

      return Padding(
        padding: const EdgeInsets.symmetric(vertical: 6),
        child: Row(
          children: [
            Icon(icon, color: color, size: 18),
            const SizedBox(width: 12),
            Expanded(
              child: Text(label, style: Theme.of(context).textTheme.bodyLarge),
            ),
            Text(
              detail,
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: color,
                  ),
            ),
          ],
        ),
      );
    }

    String _formatTime(DateTime dt) {
      final now = DateTime.now();
      final diff = now.difference(dt);
      if (diff.inSeconds < 60) return 'just now';
      if (diff.inMinutes < 60) return '${diff.inMinutes}m ago';
      if (diff.inHours < 24) return '${diff.inHours}h ago';
      return '${diff.inDays}d ago';
    }
  }