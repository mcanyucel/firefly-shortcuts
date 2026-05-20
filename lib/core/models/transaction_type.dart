enum TransactionType {
  withdrawal,
  deposit,
  transfer;

  String get apiValue => name;

  static TransactionType fromString(String value) {
    return TransactionType.values.firstWhere(
      (e) => e.name == value.toLowerCase(),
      orElse: () => TransactionType.withdrawal // Default to withdrawal if the value doesn't match any enum
    );
  }
}

