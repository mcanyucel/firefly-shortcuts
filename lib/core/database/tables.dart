import 'package:drift/drift.dart';
import 'converters.dart';

class Accounts extends Table {
  TextColumn get id => text()();
  TextColumn get name => text()();
  TextColumn get accountType => text()();
  TextColumn get accountRole => text().nullable()();
  TextColumn get currencyCode => text().nullable()();
  TextColumn get currencySymbol => text().nullable()();
  BoolColumn get active => boolean()();

  @override
  Set<Column> get primaryKey => {id};
}

class Bills extends Table {
  TextColumn get id => text()();
  TextColumn get name => text()();
  
  @override
  Set<Column> get primaryKey => {id};
}

class Budgets extends Table {
  TextColumn get id => text()();
  TextColumn get name => text()();
  
  @override
  Set<Column> get primaryKey => {id};
}

class Categories extends Table {
  TextColumn get id => text()();
  TextColumn get name => text()();
  
  @override
  Set<Column> get primaryKey => {id};
}

class Piggybanks extends Table {
  TextColumn get id => text()();
  TextColumn get name => text()();
  
  @override
  Set<Column> get primaryKey => {id};
}

class Tags extends Table {
  TextColumn get id => text()();
  TextColumn get name => text()();
  
  @override
  Set<Column> get primaryKey => {id};
}

class Shortcuts extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get name => text()();
  TextColumn get amount => text()();
  // This is a workaround for drift's lack of support for multiple foreign keys to the same table.
    TextColumn get fromAccountId => text()
      .customConstraint('NOT NULL REFERENCES accounts(id) ON DELETE CASCADE')(); 
  // This is a workaround for drift's lack of support for multiple foreign keys to the same table.
  TextColumn get toAccountId => text()
      .customConstraint('NOT NULL REFERENCES accounts(id) ON DELETE CASCADE')();
  TextColumn get categoryId => text().nullable().references(Categories, #id, onDelete: KeyAction.setNull)();
  TextColumn get billId => text().nullable().references(Bills, #id, onDelete: KeyAction.setNull)();
  TextColumn get budgetId => text().nullable().references(Budgets, #id, onDelete: KeyAction.setNull)();
  TextColumn get piggybankId => text().nullable().references(Piggybanks, #id, onDelete: KeyAction.setNull)();
  TextColumn get icon => text().nullable()();
  TextColumn get description => text().nullable()();
  IntColumn get lastUsed => integer().nullable()();
  TextColumn get transactionType => text().map(const TransactionTypeConverter())();
}

class ShortcutTags extends Table {
  IntColumn get shortcutId => integer().references(Shortcuts, #id, onDelete: KeyAction.cascade)();
  TextColumn get tagId => text().references(Tags, #id, onDelete: KeyAction.cascade)();

  @override
  Set<Column> get primaryKey => {shortcutId, tagId};
}