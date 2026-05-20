import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../database/daos/reference_daos.dart';
import '../database/daos/shortcut_dao.dart';
import 'core_providers.dart';

part 'dao_providers.g.dart';

@Riverpod(keepAlive: true)
AccountDao accountDao(Ref ref) =>
    AccountDao(ref.watch(appDatabaseProvider));

@Riverpod(keepAlive: true)
BillDao billDao(Ref ref) => BillDao(ref.watch(appDatabaseProvider));

@Riverpod(keepAlive: true)
BudgetDao budgetDao(Ref ref) =>
    BudgetDao(ref.watch(appDatabaseProvider));

@Riverpod(keepAlive: true)
CategoryDao categoryDao(Ref ref) =>
    CategoryDao(ref.watch(appDatabaseProvider));

@Riverpod(keepAlive: true)
PiggybankDao piggybankDao(Ref ref) =>
    PiggybankDao(ref.watch(appDatabaseProvider));

@Riverpod(keepAlive: true)
TagDao tagDao(Ref ref) => TagDao(ref.watch(appDatabaseProvider));

@Riverpod(keepAlive: true)
ShortcutDao shortcutDao(Ref ref) =>
    ShortcutDao(ref.watch(appDatabaseProvider));
