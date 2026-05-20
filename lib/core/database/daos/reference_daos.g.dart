// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'reference_daos.dart';

// ignore_for_file: type=lint
mixin _$AccountDaoMixin on DatabaseAccessor<AppDatabase> {
  $AccountsTable get accounts => attachedDatabase.accounts;
  AccountDaoManager get managers => AccountDaoManager(this);
}

class AccountDaoManager {
  final _$AccountDaoMixin _db;
  AccountDaoManager(this._db);
  $$AccountsTableTableManager get accounts =>
      $$AccountsTableTableManager(_db.attachedDatabase, _db.accounts);
}

mixin _$BillDaoMixin on DatabaseAccessor<AppDatabase> {
  $BillsTable get bills => attachedDatabase.bills;
  BillDaoManager get managers => BillDaoManager(this);
}

class BillDaoManager {
  final _$BillDaoMixin _db;
  BillDaoManager(this._db);
  $$BillsTableTableManager get bills =>
      $$BillsTableTableManager(_db.attachedDatabase, _db.bills);
}

mixin _$BudgetDaoMixin on DatabaseAccessor<AppDatabase> {
  $BudgetsTable get budgets => attachedDatabase.budgets;
  BudgetDaoManager get managers => BudgetDaoManager(this);
}

class BudgetDaoManager {
  final _$BudgetDaoMixin _db;
  BudgetDaoManager(this._db);
  $$BudgetsTableTableManager get budgets =>
      $$BudgetsTableTableManager(_db.attachedDatabase, _db.budgets);
}

mixin _$CategoryDaoMixin on DatabaseAccessor<AppDatabase> {
  $CategoriesTable get categories => attachedDatabase.categories;
  CategoryDaoManager get managers => CategoryDaoManager(this);
}

class CategoryDaoManager {
  final _$CategoryDaoMixin _db;
  CategoryDaoManager(this._db);
  $$CategoriesTableTableManager get categories =>
      $$CategoriesTableTableManager(_db.attachedDatabase, _db.categories);
}

mixin _$PiggybankDaoMixin on DatabaseAccessor<AppDatabase> {
  $PiggybanksTable get piggybanks => attachedDatabase.piggybanks;
  PiggybankDaoManager get managers => PiggybankDaoManager(this);
}

class PiggybankDaoManager {
  final _$PiggybankDaoMixin _db;
  PiggybankDaoManager(this._db);
  $$PiggybanksTableTableManager get piggybanks =>
      $$PiggybanksTableTableManager(_db.attachedDatabase, _db.piggybanks);
}

mixin _$TagDaoMixin on DatabaseAccessor<AppDatabase> {
  $TagsTable get tags => attachedDatabase.tags;
  TagDaoManager get managers => TagDaoManager(this);
}

class TagDaoManager {
  final _$TagDaoMixin _db;
  TagDaoManager(this._db);
  $$TagsTableTableManager get tags =>
      $$TagsTableTableManager(_db.attachedDatabase, _db.tags);
}
