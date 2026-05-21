enum SyncStatus { idle, syncing, done, error }

class EntitySyncState {
  final SyncStatus status;
  final int count;
  final String? errorMessage;

  const EntitySyncState({
    this.status = SyncStatus.idle,
    this.count = 0,
    this.errorMessage,
  });
}

class SyncState {
  final EntitySyncState accounts;
  final EntitySyncState bills;
  final EntitySyncState budgets;
  final EntitySyncState categories;
  final EntitySyncState piggybanks;
  final EntitySyncState tags;
  final bool isSyncing;
  final DateTime? lastSyncTime;

  const SyncState({
    this.accounts = const EntitySyncState(),
    this.bills = const EntitySyncState(),
    this.budgets = const EntitySyncState(),
    this.categories = const EntitySyncState(),
    this.piggybanks = const EntitySyncState(),
    this.tags = const EntitySyncState(),
    this.isSyncing = false,
    this.lastSyncTime,
  });

  bool get hasErrors => [
    accounts,
    bills,
    budgets,
    categories,
    piggybanks,
    tags,
  ].any((e) => e.status == SyncStatus.error);

  SyncState copyWith({
    EntitySyncState? accounts,
    EntitySyncState? bills,
    EntitySyncState? budgets,
    EntitySyncState? categories,
    EntitySyncState? piggybanks,
    EntitySyncState? tags,
    bool? isSyncing,
    DateTime? lastSyncTime,
  }) => SyncState(
    accounts: accounts ?? this.accounts,
    bills: bills ?? this.bills,
    budgets: budgets ?? this.budgets,
    categories: categories ?? this.categories,
    piggybanks: piggybanks ?? this.piggybanks,
    tags: tags ?? this.tags,
    isSyncing: isSyncing ?? this.isSyncing,
    lastSyncTime: lastSyncTime ?? this.lastSyncTime,
  );
}
