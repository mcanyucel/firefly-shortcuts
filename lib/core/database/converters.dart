import 'package:drift/drift.dart';
import '../models/models.dart';

class TransactionTypeConverter extends TypeConverter<TransactionType, String> {
  const TransactionTypeConverter();

  @override
  TransactionType fromSql(String fromDb) => TransactionType.fromString(fromDb);

  @override
  String toSql(TransactionType value) => value.apiValue;
}
