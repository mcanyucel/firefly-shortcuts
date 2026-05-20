import 'package:drift/drift.dart';
import '../app_database.dart';
import '../tables.dart';

part 'reference_daos.g.dart';

@DriftAccessor(tables: [Accounts])
class AccountDao extends DatabaseAccessor<AppDatabase> with _$AccountDaoMixin {
  AccountDao(super.db);

  Future<List<Account>> getAll() => select(accounts).get();

  Future<void> upsert(AccountsCompanion entry) =>
      into(accounts).insertOnConflictUpdate(entry);

  Future<void> deleteById(String id) =>
      (delete(accounts)..where((tbl) => tbl.id.equals(id))).go();
}

@DriftAccessor(tables: [Bills])
class BillDao extends DatabaseAccessor<AppDatabase> with _$BillDaoMixin {
  BillDao(super.db);

  Future<List<Bill>> getAll() => select(bills).get();

  Future<void> upsert(BillsCompanion entry) =>
      into(bills).insertOnConflictUpdate(entry);

  Future<void> deleteById(String id) =>
      (delete(bills)..where((tbl) => tbl.id.equals(id))).go();
}

@DriftAccessor(tables: [Budgets])
class BudgetDao extends DatabaseAccessor<AppDatabase> with _$BudgetDaoMixin {
  BudgetDao(super.db);

  Future<List<Budget>> getAll() => select(budgets).get();

  Future<void> upsert(BudgetsCompanion entry) =>
      into(budgets).insertOnConflictUpdate(entry);

  Future<void> deleteById(String id) =>
      (delete(budgets)..where((t) => t.id.equals(id))).go();
}

@DriftAccessor(tables: [Categories])
class CategoryDao extends DatabaseAccessor<AppDatabase>
    with _$CategoryDaoMixin {
  CategoryDao(super.db);

  Future<List<Category>> getAll() => select(categories).get();

  Future<void> upsert(CategoriesCompanion entry) =>
      into(categories).insertOnConflictUpdate(entry);

  Future<void> deleteById(String id) =>
      (delete(categories)..where((t) => t.id.equals(id))).go();
}

@DriftAccessor(tables: [Piggybanks])
class PiggybankDao extends DatabaseAccessor<AppDatabase>
    with _$PiggybankDaoMixin {
  PiggybankDao(super.db);

  Future<List<Piggybank>> getAll() => select(piggybanks).get();

  Future<void> upsert(PiggybanksCompanion entry) =>
      into(piggybanks).insertOnConflictUpdate(entry);

  Future<void> deleteById(String id) =>
      (delete(piggybanks)..where((t) => t.id.equals(id))).go();
}

@DriftAccessor(tables: [Tags])
class TagDao extends DatabaseAccessor<AppDatabase> with _$TagDaoMixin {
  TagDao(super.db);

  Future<List<Tag>> getAll() => select(tags).get();

  Future<void> upsert(TagsCompanion entry) =>
      into(tags).insertOnConflictUpdate(entry);

  Future<void> deleteById(String id) =>
      (delete(tags)..where((t) => t.id.equals(id))).go();
}
