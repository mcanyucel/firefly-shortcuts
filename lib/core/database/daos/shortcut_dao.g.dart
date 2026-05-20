// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'shortcut_dao.dart';

// ignore_for_file: type=lint
mixin _$ShortcutDaoMixin on DatabaseAccessor<AppDatabase> {
  $AccountsTable get accounts => attachedDatabase.accounts;
  $CategoriesTable get categories => attachedDatabase.categories;
  $BillsTable get bills => attachedDatabase.bills;
  $BudgetsTable get budgets => attachedDatabase.budgets;
  $PiggybanksTable get piggybanks => attachedDatabase.piggybanks;
  $ShortcutsTable get shortcuts => attachedDatabase.shortcuts;
  $TagsTable get tags => attachedDatabase.tags;
  $ShortcutTagsTable get shortcutTags => attachedDatabase.shortcutTags;
  ShortcutDaoManager get managers => ShortcutDaoManager(this);
}

class ShortcutDaoManager {
  final _$ShortcutDaoMixin _db;
  ShortcutDaoManager(this._db);
  $$AccountsTableTableManager get accounts =>
      $$AccountsTableTableManager(_db.attachedDatabase, _db.accounts);
  $$CategoriesTableTableManager get categories =>
      $$CategoriesTableTableManager(_db.attachedDatabase, _db.categories);
  $$BillsTableTableManager get bills =>
      $$BillsTableTableManager(_db.attachedDatabase, _db.bills);
  $$BudgetsTableTableManager get budgets =>
      $$BudgetsTableTableManager(_db.attachedDatabase, _db.budgets);
  $$PiggybanksTableTableManager get piggybanks =>
      $$PiggybanksTableTableManager(_db.attachedDatabase, _db.piggybanks);
  $$ShortcutsTableTableManager get shortcuts =>
      $$ShortcutsTableTableManager(_db.attachedDatabase, _db.shortcuts);
  $$TagsTableTableManager get tags =>
      $$TagsTableTableManager(_db.attachedDatabase, _db.tags);
  $$ShortcutTagsTableTableManager get shortcutTags =>
      $$ShortcutTagsTableTableManager(_db.attachedDatabase, _db.shortcutTags);
}
